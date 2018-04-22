package edu.northwestern.mhealth395.neckmonitor;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class DeviceManagement extends AppCompatActivity {

    private final String TAG = "Device Management";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public static final String USER_ID_EXTRA = "userId";
    public static final String USER_NAME_EXTRA = "username";
    public static final String STUDY_EXTRA = "study";

    private int userID;
    private String userName;
    private String study;
    SQLiteDatabase db;

    BluetoothDeviceAdapter devListAdapter;
    ArrayList<BluetoothDevice> devList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_management);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userID = extras.getInt(USER_ID_EXTRA);
            userName = extras.getString(USER_NAME_EXTRA);
            study = extras.getString(STUDY_EXTRA);
        }

        db = DataStorageContract.NecklaceDbHelper.getInstance(this).getWritableDatabase();

        devListAdapter = new BluetoothDeviceAdapter(this, devList);

        ((ListView) findViewById(R.id.deviceListView)).setAdapter(devListAdapter);
        devListAdapter.clear();
        devListAdapter.notifyDataSetChanged();



        // Click hoabeo listener
        ((ListView) findViewById(R.id.deviceListView)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Log.v(TAG, devList.get(position) + " clicked.");

                Intent streamIntent = new Intent(DeviceManagement.this, StreamActivity.class);
                streamIntent.putExtra(StreamActivity.DEVICE_EXTRA, (BluetoothDevice) devListAdapter.getItem(position));
                streamIntent.putExtra(StreamActivity.USER_ID_EXTR, userID);
                streamIntent.putExtra(StreamActivity.USER_NAME_EXTRA, userName);
                streamIntent.putExtra(StreamActivity.STUDY_EXTRA, study);

                if (devListAdapter.streaming.contains(position)) {
                    devListAdapter.streaming.remove(position);
                    streamIntent.putExtra(StreamActivity.START_STREAM_EXTRA, false);
                } else {
                    //devListAdapter.streaming.add(position);
                    streamIntent.putExtra(StreamActivity.START_STREAM_EXTRA, true);
                }
                startActivity(streamIntent);

            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        }
                    }
                });
                builder.show();
            }
        }
    }


    public void onRefreshClicked(View view) {
        Log.v(TAG, "Refresh clicked!");
        devListAdapter.clear();
        devListAdapter.notifyDataSetChanged();
        scanLeDevice(true);
    }


    /* ************************** SCANNING FOR BT LE DEVICES **************************** */
    private BluetoothLeScanner mLeScanner;
    private boolean mScanning = false;
    private Handler mHandler = new Handler();

    private boolean bluetoothLe;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private void scanLeDevice(boolean enable) {
        if (BluetoothAdapter.getDefaultAdapter() != null) {
            mLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                if (BluetoothAdapter.getDefaultAdapter().isOffloadedScanBatchingSupported() ) {
                if (enable) {
                    new ScanTask().execute(enable);
                    Log.v(TAG, "executed scan task");
                } else {
                    Log.e(TAG, "LE scan was not enabled");
                }
//                } else
                Log.e(TAG, "Batch not supported");
            }
        }
    }


    private class ScanTask extends AsyncTask<Boolean, Void, Void> {
        @Override
        protected Void doInBackground(Boolean... params) {
            if (!mScanning) {

                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mScanning = false;
                        mLeScanner.stopScan(mLeScanCallback);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((Button) findViewById(R.id.refreshButton)).setText("Refresh");
                                findViewById(R.id.refreshButton).setEnabled(true);
                                devListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                if (mLeScanner != null)
                    mLeScanner.startScan(mLeScanCallback);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            ((Button) findViewById(R.id.refreshButton)).setText("Refreshing...");
            (findViewById(R.id.refreshButton)).setEnabled(false);
        }
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.v(TAG, "Got batch results from scan for LE devices.");
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = result.getDevice();
                    if (device.getName() != null) {
                        Log.v(TAG, "Got single result from scan for LE devices.");
                        Log.v(TAG, device.getName());
                        if (!devList.contains(device)) {
                            devListAdapter.add(device);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                devListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
        }


        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan for LE bluetooth devices failed with error code " + errorCode);
        }
    };
}
