package edu.northwestern.mhealth395.neckmonitor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class StreamActivity extends AppCompatActivity {

    public static final String DEVICE_EXTRA = "DEVICE";
    public static final String START_STREAM_EXTRA = "start";
    public static final String STUDY_EXTRA = "study";

    public static final String BROADCAST_NAME = "broadcast";
    public static final String BROADCAST_DISCONNECTED = "disconnected";
    public static final String BROADCAST_CONNECTED = "connected";
    public static final String BROADCAST_DATA = "data";
    public static final String B_DATA_ACC_X = "accx";
    public static final String B_DATA_ACC_Y = "accy";
    public static final String B_DATA_ACC_Z = "accz";
    public static final String B_DATA_AUDIO = "audio";
    public static final String B_DATA_PEIZO = "peizo";
    public static final String USER_ID_EXTR = "usid";
    public static final String USER_NAME_EXTRA = "username";
    public static final String B_DATA_SWALLOW = "Swallow";


    private GraphCollectionPagerAdapter mGraphCollectionPagerAdapter;
    private ViewPager mViewPager;
    private static LineGraphSeries<DataPoint> accXSeries = new LineGraphSeries<>();
    private static LineGraphSeries<DataPoint> accYSeries = new LineGraphSeries<>();
    private static LineGraphSeries<DataPoint> accZSeries = new LineGraphSeries<>();
    private static LineGraphSeries<DataPoint> audioSeries = new LineGraphSeries<>();
    private static LineGraphSeries<DataPoint> piezoSeries = new LineGraphSeries<>();
    private static LineGraphSeries<DataPoint> indicator = new LineGraphSeries<>();

    private final int MAX_DATA = 100;
    private CustomCircularBuffer accXBuf = new CustomCircularBuffer(MAX_DATA);
    private CustomCircularBuffer accYBuf = new CustomCircularBuffer(MAX_DATA);
    private CustomCircularBuffer accZBuf = new CustomCircularBuffer(MAX_DATA);
    private CustomCircularBuffer audioBuf = new CustomCircularBuffer(MAX_DATA);
    private CustomCircularBuffer piezoBuf = new CustomCircularBuffer(MAX_DATA);

    private static Button accXButton;
    private static Button accZButton;
    private static Button accYButton;
    private static Button audioButton;
    private static Button piezoButton;
    private static TextView swallowText;
    private int swallowCount = 0;


    private final String TAG = "StreamActivity:";
    private BluetoothDevice device;
    private IntentFilter filter;

    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        swallowText = (TextView) findViewById(R.id.swallowLabel);




        // Get the ViewPager and set it's PagerAdapter so that it can display items
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(new GraphCollectionPagerAdapter(getSupportFragmentManager()));
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int oldPage = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position != oldPage) {
                    // Filter for only page switches
//                    Log.v(TAG, "Page changed; position = " + Integer.toString(position));
                    switch (position) {
                        case 0:
                            accXButton.setEnabled(false);
                            accYButton.setEnabled(true);
                            accZButton.setEnabled(true);
                            audioButton.setEnabled(true);
                            piezoButton.setEnabled(true);
                            break;
                        case 1:
                            accXButton.setEnabled(true);
                            accYButton.setEnabled(false);
                            accZButton.setEnabled(true);
                            audioButton.setEnabled(true);
                            piezoButton.setEnabled(true);
                            break;
                        case 2:
                            accXButton.setEnabled(true);
                            accYButton.setEnabled(true);
                            accZButton.setEnabled(false);
                            audioButton.setEnabled(true);
                            piezoButton.setEnabled(true);
                            break;
                        case 3:
                            accXButton.setEnabled(true);
                            accYButton.setEnabled(true);
                            accZButton.setEnabled(true);
                            audioButton.setEnabled(false);
                            piezoButton.setEnabled(true);
                            break;
                        case 4:
                            accXButton.setEnabled(true);
                            accYButton.setEnabled(true);
                            accZButton.setEnabled(true);
                            audioButton.setEnabled(true);
                            piezoButton.setEnabled(false);
                            break;
                        default:
                            Log.e(TAG, "WARNING: unimplemented page reached");
                    }

                    // Update oldPage
                    oldPage = position;
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        accXButton = (Button) findViewById(R.id.accXButton);
        accXButton.setEnabled(false);
        accYButton = (Button) findViewById(R.id.accYButton);
        accZButton = (Button) findViewById(R.id.accZButton);
        audioButton = (Button) findViewById(R.id.audioButton);
        piezoButton = (Button) findViewById(R.id.piezoButton);

        accXButton.setVisibility(View.GONE);
        accYButton.setVisibility(View.GONE);
        accZButton.setVisibility(View.GONE);
        audioButton.setVisibility(View.GONE);


        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            device = (BluetoothDevice) extras.get(DEVICE_EXTRA);

            if (device != null) {
                Intent intent = new Intent(StreamActivity.this, DataHandlerService.class);
                intent.putExtra(DataHandlerService.DEVICE_EXTRA, device);
                intent.putExtra(DataHandlerService.START_EXTRA, true);// extras.getBoolean(START_STREAM_EXTRA));
                intent.putExtra(DataHandlerService.USER_ID_EXTRA, extras.getString(USER_ID_EXTR));
                intent.putExtra(DataHandlerService.USER_NAME_EXTRA, extras.getString(USER_NAME_EXTRA));
                intent.putExtra(DataHandlerService.STUDY_EXTRA, extras.getString(STUDY_EXTRA));
                Log.v(TAG, "Study received is " + extras.getString(STUDY_EXTRA));
                startService(intent);
            } else {
                ((TextView) findViewById(R.id.connectionText)).setText("No device given");
            }
        } else {
            Log.e(TAG, "StreamActivity called but no device was passed");
        }

        // Start the service using the device
        Log.v(TAG, "Started activity...");
        filter = new IntentFilter(BROADCAST_CONNECTED);
        filter.addAction(BROADCAST_DISCONNECTED);
        filter.addAction(BROADCAST_NAME);
        filter.addAction(BROADCAST_DATA);
        registerReceiver(receiver, filter);


        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mGraphCollectionPagerAdapter =
                new GraphCollectionPagerAdapter(
                        getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mGraphCollectionPagerAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, DataHandlerService.class);
        intent.putExtra(DataHandlerService.DEVICE_EXTRA, device);
        intent.putExtra(DataHandlerService.START_EXTRA, false);
        startService(intent);
        unregisterReceiver(receiver);
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "RECEIVED BROADCAST " + intent.getAction());
            switch (intent.getAction()) {
                case BROADCAST_CONNECTED:
                    Log.v(TAG, "Connected!");
                    ((TextView) findViewById(R.id.connectionText)).setText("Connected");
                    break;
                case BROADCAST_DISCONNECTED:
                    Log.e(TAG, "Disconnected!");
                    ((TextView) findViewById(R.id.connectionText)).setText("Disconnected");
                    swallowText.getRootView().setBackgroundColor(Color.RED);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                            context).setTitle("Warning: necklace disconnected")
                            .setMessage("Please make sure the necklace is on and in range")
                            .setCancelable(false)
                            .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int id) {
                                    swallowText.getRootView().setBackgroundColor(Color.WHITE);
                                }
                            });

                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();

                    // show it
                    alertDialog.show();

                    break;
                case BROADCAST_DATA:
                    StringBuilder message = new StringBuilder("Data received...");
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        float datum;
//                        if (extras.containsKey(B_DATA_ACC_X)) {
//                            message.append("\naccelerometer x: ");
//                            datum = extras.getFloat(B_DATA_ACC_X);
//                            message.append(Float.toString(datum));
//                            // Add to the accx data set
//                            new UpdateAccx().doInBackground(extras);
//
//
//                        }
//                        if (extras.containsKey(B_DATA_ACC_Y)) {
//                            message.append("\naccelerometer y: ");
//                            datum = extras.getFloat(B_DATA_ACC_Y);
//                            message.append(Float.toString(datum));
//                            new UpdateAccy().doInBackground(extras);
////
////                            // Add to data series
////                            accYBuf.add(datum);
////                            accYSeries.resetData(accYBuf.getBuffer());
//                        }
//                        if (extras.containsKey(B_DATA_ACC_Z)) {
//                            message.append("\naccelerometer z: ");
//                            datum = extras.getFloat(B_DATA_ACC_Z);
//                            message.append(Float.toString(datum));
////                            new UpdateAccz().doInBackground(extras);
////                            // Add to series
////                            accZBuf.add(datum);
////                            accZSeries.resetData(accZBuf.getBuffer());
//                        }
//                        if (extras.containsKey(B_DATA_AUDIO)) {
//                            message.append("\nAudio: ");
//                            datum = extras.getFloat(B_DATA_AUDIO);
//                            message.append(Float.toString(datum));
//                            //Add to series
////                            audioBuf.add(datum);
////                            audioSeries.resetData(audioBuf.getBuffer());
//                            new UpdateAudio().doInBackground(extras);
//                        }
                        if (extras.containsKey(B_DATA_ACC_Z)) {
                            message.append("\nProximity: ");
                            datum = extras.getFloat(B_DATA_ACC_Z);
                            message.append(Float.toString(datum));
                            new UpdatePiezo().doInBackground(extras);

//                            // Add to series
//                            piezoBuf.add(datum);
//                            piezoSeries.resetData(piezoBuf.getBuffer());
//                               message.append("\nSwallow count: " + Integer.toString(swallowCount));
                               ((TextView) findViewById(R.id.connectionText)).setText(message);
                        }
                        if (extras.containsKey(B_DATA_SWALLOW)) {
                            Log.e(TAG, "REceived swallow");
//                            swallowCount++;
                        }
                    }
                    break;
                default:
                    Log.e(TAG, "Unknown broadcast with correct action");
            }
        }
    };

    private class UpdateAccx extends AsyncTask<Bundle, Void,Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle extras = params[0];
            if (extras != null) {
                float datum;
                if (extras.containsKey(B_DATA_ACC_X)) {
                    datum = extras.getFloat(B_DATA_ACC_X);

                    //Add to series
                    accXBuf.add(datum);
                    accXSeries.resetData(accXBuf.getBuffer());
                }
            }
            return null;
        }
    }
    private class UpdateAccy extends AsyncTask<Bundle, Void,Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle extras = params[0];
            if (extras != null) {
                float datum;
                if (extras.containsKey(B_DATA_ACC_Y)) {
                    datum = extras.getFloat(B_DATA_ACC_Y);

                    //Add to series
                    accYBuf.add(datum);
                    accYSeries.resetData(accYBuf.getBuffer());
                }
            }
            return null;
        }
    }


    private class UpdateAccz extends AsyncTask<Bundle, Void,Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle extras = params[0];
            if (extras != null) {
                float datum;
                if (extras.containsKey(B_DATA_ACC_Z)) {
                    datum = extras.getFloat(B_DATA_ACC_Z);

                    //Add to series
                    accZBuf.add(datum);
                    accZSeries.resetData(accZBuf.getBuffer());

                }
            }
            return null;
        }
    }


    private class UpdateAudio extends AsyncTask<Bundle, Void,Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle extras = params[0];
            if (extras != null) {
                float datum;
                if (extras.containsKey(B_DATA_AUDIO)) {
                    datum = extras.getFloat(B_DATA_AUDIO);

                    //Add to series
                    audioBuf.add(datum);
                    audioSeries.resetData(audioBuf.getBuffer());
                }
            }
            return null;
        }
    }

    private class UpdatePiezo extends AsyncTask<Bundle, Void,Void> {
        @Override
        protected Void doInBackground(Bundle... params) {
            Bundle extras = params[0];
            if (extras != null) {
                float datum;
                if (extras.containsKey(B_DATA_ACC_Z)) {
                    datum = extras.getFloat(B_DATA_ACC_Z);

                    //Add to series
                    piezoBuf.add(datum);
                    piezoSeries.resetData(piezoBuf.getBuffer());

                    DataPoint[] indicate = new DataPoint[2];
                    count = (count + 1) % 100;
                    indicate[0] = new DataPoint(count, 0);
                    indicate[1] = new DataPoint(count, 66000);
                    indicator.resetData(indicate);
                }
            }
            return null;
        }
    }

    @Override

    public void onBackPressed() {
        Log.v(TAG, "Back pressed.. stopping stream");
        super.onBackPressed();
    }


    /************
     * Fragment for tabbed view
     ************/
    // Since this is an object collection, use a FragmentStatePagerAdapter,
    // and NOT a FragmentPagerAdapter.
    public class GraphCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public GraphCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        private GraphFragment fragment;
        public void updateGraphs() {
//            fragment.updateGraphs();
        }

        @Override
        public Fragment getItem(int i) {
            fragment = new GraphFragment();
            Bundle args = new Bundle();
            // Our object is just an integer
            args.putInt(GraphFragment.ARG_OBJECT, i);
            fragment.setArguments(args);
            Log.v(TAG, "GetItem was called");
            return fragment;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Log.v(TAG, "getPageTitle called with position " + Integer.toString(position));
            String title;
            switch (position) {
                case 0:
                    title = "Accelerometer X";
                    accXButton.setEnabled(false);
                    accYButton.setEnabled(true);
                    accZButton.setEnabled(true);
                    audioButton.setEnabled(true);
                    piezoButton.setEnabled(true);
                    break;
                case 1:
                    title = "Accelerometer Y";
                    accXButton.setEnabled(true);
                    accYButton.setEnabled(false);
                    accZButton.setEnabled(true);
                    audioButton.setEnabled(true);
                    piezoButton.setEnabled(true);
                    break;
                case 2:
                    title = "Accelerometer Z";
                    accXButton.setEnabled(true);
                    accYButton.setEnabled(true);
                    accZButton.setEnabled(false);
                    audioButton.setEnabled(true);
                    piezoButton.setEnabled(true);
                    break;
                case 3:
                    title = "Audio";
                    accXButton.setEnabled(true);
                    accYButton.setEnabled(true);
                    accZButton.setEnabled(true);
                    audioButton.setEnabled(false);
                    piezoButton.setEnabled(true);
                    break;
                case 4:
                    title = "Piezo";
                    accXButton.setEnabled(true);
                    accYButton.setEnabled(true);
                    accZButton.setEnabled(true);
                    audioButton.setEnabled(true);
                    piezoButton.setEnabled(false);
                    break;
                default:
                    return "Unimplemented position " + Integer.toString(position);
            }
            return title;
        }
    }

    // Instances of this class are fragments representing a single
    // object in our collection.
    public static class GraphFragment extends Fragment {
        public static final String ARG_OBJECT = "POSITION";
        private static final String TAG = "GraphFragment";
        private GraphView acXGraph;
        private GraphView acYGraph;
        private GraphView acZGraph;
        private GraphView audioGraph;
        private GraphView piezoGraph;


        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            // The last two arguments ensure LayoutParams are inflated
            // properly.
            View rootView = inflater.inflate(
                    R.layout.fragment_graph, container, false);
            Bundle args = getArguments();


            GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
            graph.removeAllSeries();

            int position = args.getInt(ARG_OBJECT);
            Log.v(TAG, "Fragment position is " + Integer.toString(position));

            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(66000);
//            switch (position) {
//                case 0:
//                    // Set accx graph
//                    graph.addSeries(accXSeries);
//                    acXGraph = graph;
//                    graph.setTitle("Accelerometer X");
//                    break;
//                case 1:
//                    // Set accy graph
//                    graph.addSeries(accYSeries);
//                    acYGraph = graph;
//                    graph.setTitle("Accelerometer Y");
//                    break;
//                case 2:
//                    // Set accZ graph
//                    graph.addSeries(accZSeries);
//                    acZGraph = graph;
//                    graph.setTitle("Accelerometer X");
//                    break;
//                case 3:
//                    // Set audio graph
//                    graph.addSeries(audioSeries);
//                    audioGraph = graph;
//                    graph.setTitle("Audio Signal");
//                    break;
//                case 4:
//                default:
//                    Log.e(TAG, "ERROR: create called for unhandled graph");
//            };
            // Set piezo graph
            graph.addSeries(piezoSeries);

            DataPoint[] indicate = new DataPoint[2];

            indicate[0] = new DataPoint(40, 0);
            indicate[1] = new DataPoint(40, 66000);

            indicator.resetData(indicate);
            indicator.setColor(Color.RED);
            graph.addSeries(indicator);


            piezoGraph = graph;
            graph.setTitle("New necklace data flow");


//            accXSeries.appendData(new DataPoint(accXSeries.getHighestValueX() + 1,
//                    args.getInt(ARG_OBJECT)), true, 5);

            Viewport vp = graph.getViewport();
            vp.setXAxisBoundsManual(true);
            vp.setMinX(0);
            vp.setMaxX(100);
            return rootView;
        }

        public void updateGraphs() {
            // Set accx graph
            acXGraph.removeAllSeries();
            acXGraph.addSeries(accXSeries);
            // Set accy graph
            acYGraph.removeAllSeries();
            acYGraph.addSeries(accYSeries);
            // Set accZ graph
            acZGraph.removeAllSeries();
            acZGraph.addSeries(accZSeries);
            // Set audio graph
            audioGraph.removeAllSeries();
            audioGraph.addSeries(audioSeries);
            // Set piezo graph
            piezoGraph.removeAllSeries();
            piezoGraph.addSeries(piezoSeries);
            piezoGraph.addSeries(indicator);
        }
    }

    // Button listeners
    public void onAccXClick(View view) {
        mViewPager.setCurrentItem(0);
    }
    public void onAccYClick(View view) {
        mViewPager.setCurrentItem(1);
    }
    public void onAccZClick(View view) {
        mViewPager.setCurrentItem(2);
    }
    public void onAudioClicked(View view) {
        mViewPager.setCurrentItem(3);
    }
    public void onPiezoClicked(View view) {
        mViewPager.setCurrentItem(4);
    }

    public class CustomCircularBuffer {

        private DataPoint[] buffer;

        private int tail;

        private int head;

        public DataPoint[] getBuffer() {return buffer;}

        public int getHead(){
            return head;
        }

        public CustomCircularBuffer(int n) {
            buffer = new DataPoint[n];
            tail = 0;
            head = 0;

            // Initialize to datapoints of (x,0)
            for (int i = 0; i < n; i++) {
                add(0);
            }
        }

        public void add(double toAdd) {
            if (head != (tail - 1)) {
                buffer[head++] = new DataPoint((double) head, toAdd);

            } else {
                throw new BufferOverflowException();
            }
            head = head % buffer.length;
        }

        public DataPoint get() {
            DataPoint t = null;
            int adjTail = tail > head ? tail - buffer.length : tail;
            if (adjTail < head) {
                t = buffer[tail++];
                tail = tail % buffer.length;
            } else {
                throw new BufferUnderflowException();
            }
            return t;
        }

        public String toString() {
            return "CustomCircularBuffer(size=" + buffer.length + ", head=" + head + ", tail=" + tail + ")";
        }
    }

    public void onStartCsvClicked(View view) {
        // send broadcast to start writing to csv
        Intent i = new Intent(DataHandlerService.B_TYPE_CSV);
        i.putExtra(DataHandlerService.CSV_STREAM_EXTRA, true);
        sendBroadcast(i);
    }

    public void onStopCsvClicked(View view) {
        // send broadcast to stop writing to csv
        Intent i = new Intent(DataHandlerService.B_TYPE_CSV);
        i.putExtra(DataHandlerService.CSV_STREAM_EXTRA, false);
        sendBroadcast(i);
    }

    public void onStartLabelClicked(View view) {
        // Start label activity
        Intent i = new Intent(this, LabelActivity.class);
        unregisterReceiver(receiver);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, filter);
    }
}
