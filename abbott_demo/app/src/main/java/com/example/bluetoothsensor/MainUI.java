package com.example.bluetoothsensor;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.GraphicalView;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainUI extends Activity
		implements BluetoothAdapter.LeScanCallback, IntroFragment.IntroInterractionListener {
	/*
	 * Structures used for saving data received via BT
	 */
	public static ArrayList<SensorData> VibrationDataList = new ArrayList<SensorData>();
	public static ArrayList<Double> VibrationDeviationList = new ArrayList<Double>();

	String ReceiveBuffer = "";
	static TextView TextThreshold;
	static View MainView;
	static ImageButton connectingButton;

	// For logging
	private final static String TAG = "MainUI";

	/*
	 * Parameters for the swallow detection algorithm
	 */
	static int DISABLE_COUNTER = -50;
	final static int WINDOW_SIZE = 6;
	final static int DISABLE_LENGTH = 25; // based on 20 samples per second!!
	static int SWALLOW_COUNT = 0;
	static double THRESHOLD = 2;

	/*
	 * Estimate if they are wearing the necklace
	 */
	int ConsecutiveZero = 0;
	double LastReading = 0;

	int ReceivedData = 0;
	int LastData = 0;

	// State machine
	final private static int STATE_BLUETOOTH_OFF = 1;
	final private static int STATE_DISCONNECTED = 2;
	final private static int STATE_CONNECTING = 3;
	final private static int STATE_CONNECTED = 4;

	private static int state;

	public static BluetoothAdapter bluetoothAdapter;
	public static BluetoothDevice bluetoothDevice;
	public static RFduinoService rfduinoService;

	boolean Connected = false;

	// navigation drawer variables
	DrawerLayout drawer;
	ListView drawerList;
	ActionBarDrawerToggle drawerToggle;

	String[] menuItems = { "Home","Dashboard", "Profile", "Connect", "Habit report", "Logout", "Nutrition" };

	// User information
	protected User user = new User();
	protected boolean didEatFruit = false;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) && !didEatFruit){
//			didEatFruit = true;
//			Toast.makeText(this, "You just ate a fruit cup.", Toast.LENGTH_SHORT).show();

			// Update UI as necessary
			Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
			FoodData data = FoodData.getInstance(this);
			Calendar c = Calendar.getInstance();
			FoodDatum fruitCup = new FoodDatum("1/10 Fruit cup", "10", "0", "0", "0",
					Integer.toString(c.get(Calendar.HOUR_OF_DAY)) + ":" + Integer.toString(c.get(Calendar.MINUTE)),
					Integer.toString(c.get(Calendar.MONTH)+1) + "/" + Integer.toString(c.get(Calendar.DAY_OF_MONTH)) + "/" +
						Integer.toString(c.get(Calendar.YEAR)));

			data.put(fruitCup);

			if (currentFragment instanceof SummaryFragment) {
				// Update graph
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.content_frame, new SummaryFragment()).commit();
			} else if (currentFragment instanceof NutritionFragment) {
				((NutritionFragment) currentFragment).adapter.addDatum(fruitCup);
				((NutritionFragment) currentFragment).adapter.notifyDataSetChanged();
			}

		} else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
			Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
			Calendar c = Calendar.getInstance();
			FoodData.getInstance(this).remove(-1, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH)+1, c.get(Calendar.YEAR));

			if (currentFragment instanceof SummaryFragment) {
				// Update graph
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.content_frame, new SummaryFragment()).commit();
			} else if (currentFragment instanceof NutritionFragment) {
				((NutritionFragment) currentFragment).adapter.notifyDataSetChanged();
			}
		}
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_ACTION_BAR);
		setContentView(R.layout.activity_main);
		
		
		Fragment fragment = new IntroFragment();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.content_frame, fragment).commit();

		drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawerList = (ListView) findViewById(R.id.left_drawer);
		drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		drawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, menuItems));

		drawerList.setOnItemClickListener(new DrawerItemClickListener());

		drawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		drawer, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				// getActionBar().setTitle("WearSens");
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				// getActionBar().setTitle("WearSens");
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		drawer.setDrawerListener(drawerToggle);

		// ------------------------------------------------------------------------------------
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if ((LastData == ReceivedData) && (ReceivedData > 2)) {
					ReceivedData = 0;
					LastData = 0;

					/*
					 * Here we detect when the device has been turned off after
					 * being turned on previously But we do nothing....for now!
					 */

					// This code will always run on the UI thread, therefore is
					// safe to modify UI elements.

					Connected = false;

					try {
						unbindService(rfduinoServiceConnection);
						rfduinoService.disconnect();
					} catch (IllegalArgumentException ex) {

					}

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// connectingButton.setVisibility(ImageButton.INVISIBLE);

						}
					});

				}
				LastData = ReceivedData;
			}
		}, 2000, 2000); // 3 seconds

		// ------------------------------------------------------------------------------------

		
		//ConnectFragment.MyUI = this;
		
		// connectingButton = (ImageButton) findViewById(R.id.connectingButton);
		// ------------------------------------------------------------------------------------
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// ------------------------------------------------------------------------------------
		/*
		 * SeekBar pg1 = (SeekBar) findViewById(R.id.seekBar1);
		 * 
		 * pg1.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
		 * 
		 * @Override public void onProgressChanged(SeekBar arg0, int arg1,
		 * boolean arg2) { MainUI.THRESHOLD = (((double)arg1) / ((double)1000));
		 * }
		 * 
		 * @Override public void onStartTrackingTouch(SeekBar arg0) { }
		 * 
		 * @Override public void onStopTrackingTouch(SeekBar arg0) { }
		 * 
		 * });
		 * 
		 * ImageButton scanButton = (ImageButton) findViewById(R.id.scan);
		 * scanButton.setOnClickListener(new ImageButton.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) {
		 * 
		 * if(Connected == false) { onStart();
		 * findViewById(R.id.progressBarConnecting
		 * ).setVisibility(ProgressBar.VISIBLE);
		 * bluetoothAdapter.startLeScan(new UUID[]{ RFduinoService.UUID_SERVICE
		 * },MainUI.this);
		 * 
		 * }
		 * 
		 * 
		 * 
		 * 
		 * } });
		 * 
		 * 
		 * ImageButton QuitButton = (ImageButton) findViewById(R.id.quitButton);
		 * QuitButton.setOnClickListener(new ImageButton.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { finish(); } });
		 * 
		 * 
		 * ImageButton SetupButton = (ImageButton)
		 * findViewById(R.id.setupButton); SetupButton.setOnClickListener(new
		 * ImageButton.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) {
		 * 
		 * LinearLayout scrollLayout = (LinearLayout)
		 * findViewById(R.id.scrollLayout); LinearLayout sensLabel =
		 * (LinearLayout)findViewById(R.id.sensLabel); TextView textStd =
		 * (TextView)findViewById(R.id.stdDevData);
		 * 
		 * int visibility = scrollLayout.getVisibility();
		 * 
		 * if(visibility == 4) visibility = 0; else visibility = 4;
		 * 
		 * scrollLayout.setVisibility(visibility);
		 * sensLabel.setVisibility(visibility);
		 * textStd.setVisibility(visibility);
		 * 
		 * } });
		 */
	}

	@Override
	public void updateName(String name) { this.user.setName(name); }

	@Override
	public void updateAge(int age) { this.user.setAge(age);	}

	@Override
	public void updateWeight(float weight) { this.user.setWeight(weight); }

	@Override
	public void updateHeight(float height) { this.user.setHeight(height); }

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position);
		}
	}

	private void selectItem(int position) {
		// update the main content by replacing fragments
		Fragment fragment = null;
		switch (position) {
			case 0:
				fragment = new IntroFragment();
				break;
			case 1:
				fragment = new SummaryFragment();
				break;
			
			case 2:
				fragment = new ProfileFragment();
				break;
			case 3:
				fragment = new ConnectFragment();
				break;

			case 4:
				fragment = new ReportFragment();
				break;

			case 5:
				fragment = new LogoutFragment();
				break;
			case 6:
				Calendar c = Calendar.getInstance();
				fragment = NutritionFragment.newInstance(
						c.get(Calendar.YEAR),
						c.get(Calendar.MONTH)+1,
						c.get(Calendar.DAY_OF_MONTH),
						-1);
				break;
			default:
				break;
		}

		if (fragment != null) {
			FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();

			// update selected item and title, then close the drawer
			drawerList.setSelection(position);
			drawerList.setItemChecked(position, true);
			setTitle(menuItems[position]);
			drawer.closeDrawer(drawerList);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		drawerToggle.onConfigurationChanged(newConfig);
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		boolean drawerOpen = drawer.isDrawerOpen(drawerList);

		return super.onPrepareOptionsMenu(menu);
	}

	
	private static void upgradeState(int newState) {
		if (newState > state) {
			updateState(newState);
		}
	}

	private static void downgradeState(int newState) {
		if (newState < state) {
			updateState(newState);
		}
	}

	private static void updateState(int newState) {
		state = newState;
	}

	private void addData(byte[] data) {

		String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
		if (ascii != null) {
			//ProcessReceivedData(ascii);
		}
	}

	@Override
	public void onLeScan(BluetoothDevice device, final int rssi,
			final byte[] scanRecord) {
		bluetoothAdapter.stopLeScan(this);
		bluetoothDevice = device;

		MainUI.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (bluetoothDevice.getName().contains("RF")) {

					Intent rfduinoIntent = new Intent(MainUI.this,
							RFduinoService.class);
					bindService(rfduinoIntent, rfduinoServiceConnection,
							BIND_AUTO_CREATE);

				}
			}
		});
	}

	private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				upgradeState(STATE_DISCONNECTED);
			} else if (state == BluetoothAdapter.STATE_OFF) {
				downgradeState(STATE_BLUETOOTH_OFF);
			}
		}
	};

	private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// = (bluetoothAdapter.getScanMode() !=
			// BluetoothAdapter.SCAN_MODE_NONE);
		}
	};

	final static ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			rfduinoService = ((RFduinoService.LocalBinder) service)
					.getService();
			if (rfduinoService.initialize()) {
				boolean result = rfduinoService.connect(bluetoothDevice
						.getAddress());

				if (result == true) {
					upgradeState(STATE_CONNECTING);
				}
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			rfduinoService = null;
			downgradeState(STATE_DISCONNECTED);
		}
	};

	private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (RFduinoService.ACTION_CONNECTED.equals(action)) {
				connectingButton.setVisibility(ImageButton.VISIBLE);

				upgradeState(STATE_CONNECTED);
			} else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
				downgradeState(STATE_DISCONNECTED);
			} else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
				addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
			}
		}
	};
};

class SensorData {
	public String iIndex, iValue;

	public SensorData(String index, String value) {
		iIndex = index;
		iValue = value;
	}
}

class IntroFragment extends Fragment{
	Button getStarted;
	
	public IntroFragment(){}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		final View rootView = inflater.inflate(R.layout.fragment_intro, container,false);
		TextView label = (TextView) rootView.findViewById(R.id.intro);
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		label.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.intro).getBackground();
		bg.setAlpha(127);
		
		getStarted = (Button) rootView.findViewById(R.id.button1);
		getStarted.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MainUI ui = (MainUI) getActivity();
				ui.updateAge(Integer.valueOf(
						((TextView) rootView.findViewById(R.id.editTextAge)).getText().toString()));
				ui.updateHeight(Float.valueOf(
						((TextView) rootView.findViewById(R.id.editTextHeight)).getText().toString()));
				ui.updateName(((TextView) rootView.findViewById(R.id.editTextName)).getText().toString());
				ui.updateWeight(Float.valueOf(
						((TextView) rootView.findViewById(R.id.editTextWeight)).getText().toString()));

				Fragment fragment = new SummaryFragment();
				FragmentManager fragmentManager = getFragmentManager();
				fragmentManager.beginTransaction()
						.replace(R.id.content_frame, fragment).commit();
			}
		});
		return rootView;
	}

	public interface IntroInterractionListener {
		void updateName(String name);
		void updateAge(int age);
		void updateWeight(float weight);
		void updateHeight(float height);
	}
	
}

class ProfileFragment extends Fragment {
	public ProfileFragment() {
	}
	
	TextView text, welcome;
	User user;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.fragment_profile, container,false);

		MainUI activity = (MainUI) getActivity();
		this.user = activity.user;

		StringBuilder welcomeString = new StringBuilder();
		welcomeString.append("Welcome ")
				.append(user.getName())
				.append("!\nYour current weight is ")
				.append(user.getWeight())
				.append("\nYour current height is ")
				.append(user.getHeight())
				.append("\nYour current age is ")
				.append(user.getAge());


		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		welcome = (TextView) rootView.findViewById(R.id.welcome);
		welcome.setTypeface(tf);
		welcome.setText(welcomeString.toString());
		return rootView;
	}

}

class ConnectFragment extends Fragment{
	// constructor
	public ConnectFragment() {
	}
	
	/*
	 * Structures used for saving data received via BT
	 */

    String ReceiveBuffer = "";
    static TextView TextThreshold;
    static View MainView;
    static ImageButton connectingButton;
    static ImageButton startButton;
	static ProgressBar progressBarConnecting;


	BluetoothDeviceAdapter devListAdapter;
	ArrayList<BluetoothDevice> devList = new ArrayList<BluetoothDevice>();
  
    /*
     * For detecting when the necklace has been taken off
     */
    int ReceivedData = 0;
    int LastData = 0;
    /*
     * State machine for Bluetooth
     */
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
	final private  String TAG = "Connect Fragment";

    private int state;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private RFduinoService rfduinoService;
   
    boolean Connected = false;
    public static boolean RegisterSwallows = false;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.fragment_connect, container,false);    // rootView is the fragment
		TextView text = (TextView) rootView.findViewById(R.id.txtLabel);     //text is "Connect"
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);


		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);

		progressBarConnecting = (ProgressBar) rootView.findViewById(R.id.progressBarConnecting);

		devListAdapter = new BluetoothDeviceAdapter(inflater, devList);


		ListView deviceListView = (ListView) rootView.findViewById(R.id.deviceListView);
		deviceListView.setAdapter(devListAdapter);
		devListAdapter.clear();
		devListAdapter.notifyDataSetChanged();

		// Click hoabeo listener
		deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Log.v(TAG, devList.get(position) + " clicked.");

				Intent streamIntent = new Intent(getActivity(), StreamActivity.class);
				streamIntent.putExtra(StreamActivity.DEVICE_EXTRA, (BluetoothDevice) devListAdapter.getItem(position));

				//not relevant data
				streamIntent.putExtra(StreamActivity.USER_ID_EXTR, "1");
				streamIntent.putExtra(StreamActivity.USER_NAME_EXTRA, "1");
				streamIntent.putExtra(StreamActivity.STUDY_EXTRA, "1");

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



		/*
		 * If the user presses the "connect" button
		 */
        ImageButton scanButton = (ImageButton) rootView.findViewById(R.id.scan);
        scanButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
				Log.v(TAG, "Refresh clicked!");
				devListAdapter.clear();
				devListAdapter.notifyDataSetChanged();
				scanLeDevice(true);
				progressBarConnecting.setVisibility(ProgressBar.VISIBLE);
            }
        });

		devListAdapter.clear();
		devListAdapter.notifyDataSetChanged();
		scanLeDevice(true);

        
        return rootView;
    }


	/* ************************** SCANNING FOR BT LE DEVICES **************************** */
	private BluetoothLeScanner mLeScanner;
	private boolean mScanning = false;
	private Handler mHandler = new Handler();


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
						progressBarConnecting.setVisibility(ProgressBar.INVISIBLE);
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

			BluetoothDevice device = result.getDevice();
			if (device.getName() != null) {
				Log.v(TAG, "Got single result from scan for LE devices.");
				Log.v(TAG, device.getName());
				if (!devList.contains(device)) {
					devListAdapter.add(device);
				}
				devListAdapter.notifyDataSetChanged();

			}

		}


		@Override
		public void onScanFailed(int errorCode) {
			Log.e(TAG, "Scan for LE bluetooth devices failed with error code " + errorCode);
		}
	};
    


    

	
    /*
     * 
     * 
     * Some helper functions for Bluetooth
     */
	    @Override
	    public void onStart()
	    {
	        super.onStart();
	    }


//
//
//	    @Override
//	    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord)
//	    {
//	        bluetoothAdapter.stopLeScan(this);
//	        bluetoothDevice = device;
//
//	        getActivity().runOnUiThread(new Runnable()
//	        {
//	            @Override
//	            public void run()
//	            {
//
//      	                if(bluetoothDevice.getName().contains("RF"))
//      	                {
//      	                	/*
//      	                	 * We have scanned and found the RFDuino
//      	                	 */
//
//      	                	Intent rfduinoIntent = new Intent(getActivity(), RFduinoService.class);
//   	                		//bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
//      	                	getActivity().bindService(rfduinoIntent, rfduinoServiceConnection, 1);
//
//      	                }
//	            }
//	        });
//	    }
//
//
//
//
//	    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver()
//	    {
//	        @Override
//	        public void onReceive(Context context, Intent intent)
//	        {
//	            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
//	            if (state == BluetoothAdapter.STATE_ON)
//	            {
//	                upgradeState(STATE_DISCONNECTED);
//	            }
//	            else if (state == BluetoothAdapter.STATE_OFF)
//	            {
//	                downgradeState(STATE_BLUETOOTH_OFF);
//	            }
//	        }
//	    };
//
//
//
//
//	    private ServiceConnection rfduinoServiceConnection = new ServiceConnection()
//	    {
//	        @Override
//	        public void onServiceConnected(ComponentName name, IBinder service)
//	        {
//	            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
//	            if (rfduinoService.initialize())
//	            {
//	            	boolean result = rfduinoService.connect(bluetoothDevice.getAddress());
//
//	                if (result == true)
//	                {
//	                    upgradeState(STATE_CONNECTING);
//	                }
//	            }
//	        }
//
//	        @Override
//
//	        public void onServiceDisconnected(ComponentName name)
//	        {
//	            rfduinoService = null;
//	            downgradeState(STATE_DISCONNECTED);
//	        }
//	    };
//
//
//
//
//	    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver()
//	    {
//	        @Override
//	        public void onReceive(Context context, Intent intent)
//	        {
//	            final String action = intent.getAction();
//
//	            if (RFduinoService.ACTION_CONNECTED.equals(action))
//	            {
//					connectingButton.setVisibility(ImageButton.VISIBLE);
//
//	                upgradeState(STATE_CONNECTED);
//	            }
//	            else if (RFduinoService.ACTION_DISCONNECTED.equals(action))
//	            {
//	                downgradeState(STATE_DISCONNECTED);
//	            }
//	            else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action))
//	            {
//	                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
//	            }
//	        }
//	    };
//
};

class SummaryFragment extends Fragment{
	public SummaryFragment() {}

	// TODO: fixme (to use real user
	// User user = ((MainUI) getActivity()).user;
	User user = new User();

	LinearLayout layout;
	GraphicalView view;
	CaloriesGraph today;// = new CaloriesGraph("#FFFB00", user.getHbe(), CaloriesGraph.Timescale.DAILY, con);
	CaloriesGraph week;// = new CaloriesGraph("#FF4400", user.getHbe(), CaloriesGraph.Timescale.WEEKLY);
	CaloriesGraph month;// = new CaloriesGraph("#3399FF", user.getHbe(), CaloriesGraph.Timescale.MONTHLY);
	static int current = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		today = CaloriesGraph.getDefaultDailyGraph(user, container.getContext());
		week = new CaloriesGraph("#FF4400", user.getHbe(), CaloriesGraph.Timescale.WEEKLY, container.getContext());
		week.getView(getActivity()).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e("SummaryFragment", "view clicked.");
			}
		});
		month = new CaloriesGraph("#3399FF", user.getHbe(), CaloriesGraph.Timescale.MONTHLY, container.getContext());
		View rootView = inflater.inflate(R.layout.fragment_summary, container,false);

		final TextView day = (TextView) rootView.findViewById(R.id.day);
		
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		day.setTypeface(tf);
		layout = (LinearLayout) rootView.findViewById(R.id.layout1);

		switch (current) {
			case 0:
				day.setText("Today");
				view = today.getView(getActivity());
				break;
			case 1:
				day.setText("This Week");
				view = week.getView(getActivity());
				break;
			case 2:
				day.setText("This Month");
				view = month.getView(getActivity());
				break;
		}
		layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		view.repaint();

		ImageButton prev = (ImageButton) rootView.findViewById(R.id.prev);
		ImageButton next = (ImageButton) rootView.findViewById(R.id.next);
		Button nutritionButton = (Button) rootView.findViewById(R.id.nutrition_button);

		nutritionButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// Go to nutrition page depending on what graph is being displayed.
				Fragment fragment = null;
				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				switch (current) {
					case 0:
						// Today
						fragment = NutritionFragment.newInstance(year, month, day, -1);

						break;
					case 1:
						// This week
						fragment = NutritionFragment.newInstance(year, month, day, -1);
						break;
					case 2:
						// Last month
						fragment = NutritionFragment.newInstance(year, month, -1, -1);
						break;
					default:
						Log.e("NutritionButton", "Unknown value for current");
				}

				// update selected item and title, then close the drawer
				((MainUI) getActivity()).drawerList.setSelection(6);
				((MainUI) getActivity()).drawerList.setItemChecked(6, true);
				getActivity().setTitle(((MainUI) getActivity()).menuItems[6]);

				if (fragment != null) {
					FragmentManager fragmentManager = getFragmentManager();
					fragmentManager.beginTransaction()
							.replace(R.id.content_frame, fragment).commit();
				}
			}
		});
		
		prev.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				if(current == 0){
					current = 2;
					day.setText("This Month");
					view = null;
					view = month.getView(getActivity()); 
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					view.repaint();
				}else if(current == 1){
					current = 0;
					day.setText("Today");
					view = null;
					view = today.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					view.repaint();
				}else if(current == 2){
					current = 1;
					day.setText("This Week");
					view = null;
					view = week.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					view.repaint();
				}
				
			}
		});
		
		next.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(current == 0){
					current = 1;
					day.setText("This Week");
					view = null;
					view = week.getView(getActivity()); 
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

//					week.setDefaultWeeklyGraph();
					view.repaint();
				}else if(current == 1){
					current = 2;
					day.setText("This Month");
					view = null;
					view = month.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

//					month.setDefaultMonthlyGraph();
					view.repaint();
				}else if(current == 2){
					current = 0;
					day.setText("Today");
					view = null;
					view = today.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

//					today.setDefaultDailyGraph();
					view.repaint();
				}
				
			}
		});


		// Update the equation
		float goal = 2359;
		float soFar = 0;
		Calendar c = Calendar.getInstance();
		List<FoodDatum> l = FoodData.getInstance(getActivity()).getAll(-1,
				c.get(Calendar.MONTH)+1,
				c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.YEAR));

		for (FoodDatum d : l) {
			soFar += d.energy;
		}

		((TextView) rootView.findViewById(R.id.goal)).setText(Integer.toString(Math.round(goal)));
		((TextView) rootView.findViewById(R.id.food)).setText(Integer.toString(Math.round(soFar)));
		((TextView) rootView.findViewById(R.id.remaining)).setText(Integer.toString(Math.round(goal - soFar)));


		return rootView;
	}
	
}

class ReportFragment extends Fragment {
	public ReportFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.fragment_report, container, false);

		return rootView;
	}
}

class LogoutFragment extends Fragment{
	public LogoutFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View rootView = inflater.inflate(R.layout.fragment_logout, container,false);
		TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);

		Button logout = (Button) rootView.findViewById(R.id.logout);
		logout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				getActivity().finish();
			}
		});


		return rootView;
	}



}
