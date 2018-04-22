package com.example.bluetoothsensor;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.achartengine.GraphicalView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


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

	String[] menuItems = { "Home","Dashboard", "Profile", "Connect", "Habit report", "Logout" };

	// User information
	protected User user = new User();

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

		// getActionBar().setDisplayHomeAsUpEnabled(true);
		// getActionBar().setHomeButtonEnabled(true);

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
					// rfduinoService.close();
					// onStop();

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


		text = (TextView) rootView.findViewById(R.id.txtLabel);
		text.setText("Profile");
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		welcome = (TextView) rootView.findViewById(R.id.welcome);
		welcome.setTypeface(tf);
		welcome.setText(welcomeString.toString());
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
		return rootView;
	}

}

class ConnectFragment extends Fragment implements BluetoothAdapter.LeScanCallback{
	// constructor
	public ConnectFragment() {
	}
	
	/*
	 * Structures used for saving data received via BT
	 */
    public static ArrayList<SensorData> VibrationDataList = new ArrayList<SensorData>();
    public static ArrayList<Double> VibrationDeviationList = new ArrayList<Double>();
    
    String ReceiveBuffer = "";
    static TextView TextThreshold;
    static View MainView;
    static ImageButton connectingButton;
    static ImageButton startButton;
    static TextView TextStdDev;
    static TextView LabelOnOff;
    static ProgressBar MainProgressBar;
    static ProgressBar progressBarConnecting;
  
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
		View rootView = inflater.inflate(R.layout.fragment_connect, container,false);
		TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
		
		TextStdDev = (TextView) rootView.findViewById(R.id.stdDevData);
    	connectingButton = (ImageButton) rootView.findViewById(R.id.connectingButton);
    	LabelOnOff = (TextView) rootView.findViewById(R.id.labelOnOff);
    	MainProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar1);
    	progressBarConnecting = (ProgressBar) rootView.findViewById(R.id.progressBarConnecting);
    	final LinearLayout scrollLayout = (LinearLayout) rootView.findViewById(R.id.scrollLayout);
        final LinearLayout sensLabel = (LinearLayout) rootView.findViewById(R.id.sensLabel);
        final TextView textStd = (TextView) rootView.findViewById(R.id.stdDevData);
        
    	
    	/*
    	 * This timer helps us detect when the device has been turned off.
    	 * Ie. if the last received data does not change for some time
    	 */
    	Timer timer = new Timer();
    	timer.scheduleAtFixedRate(new TimerTask()
    	{
    		  @Override
    		  public void run() 
    		  {
    			  if((LastData == ReceivedData) && (ReceivedData > 2))
    			  {
    				  ReceivedData = 0;
    				  LastData = 0;
    				      				  
    				  /*
    				   * Here we detect when the device has been turned off after being turned on previously
    				   * But we do nothing....for now!
    				   */
    	
    				  
    				  Connected = false;
    				  
    				  try
    				  {
    					  /*
    					   * Close the connections
    					   */
    					  getActivity().unbindService(rfduinoServiceConnection);
    				 	  rfduinoService.disconnect();
    				  }
    				  catch(IllegalArgumentException ex)
    				  {

    				  }

    				  getActivity().runOnUiThread(new Runnable()
          				{
	          	            @Override
	          	            public void run()
	          	            {
	    						connectingButton.setVisibility(ImageButton.INVISIBLE);
	          	            }
          				});


    			  }
    	   		   LastData = ReceivedData;
    		  }
    		}, 2000,2000); //2 seconds is the interval of the timer

    	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        /*
         * A user moving the seekbar will change the value of the threshold for detecting swallows.
         * This is necessary for calibration
         */
		SeekBar pg1 = (SeekBar) rootView.findViewById(R.id.seekBar1);
		pg1.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2)
			{
				Algorithms.THRESHOLD = (((double)arg1) / ((double)1000));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0)
			{
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0)
			{
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

                if(Connected == false)
                {
                	onStart();
                	progressBarConnecting.setVisibility(ProgressBar.VISIBLE);
                	bluetoothAdapter.startLeScan(new UUID[]{ RFduinoService.UUID_SERVICE },ConnectFragment.this);
                }
            }
        });


    	/*
		 * If the user presses the "connect" button
		 */
        ImageButton startButton = (ImageButton) rootView.findViewById(R.id.profile);
        startButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

               RegisterSwallows = !RegisterSwallows;
            }
        });



        /*
         * Exit the app
         */
        ImageButton QuitButton = (ImageButton) rootView.findViewById(R.id.quitButton);
        QuitButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
            	//finish();
            }
        });

        /*
         * Toggles the calibration controls
         */
        ImageButton SetupButton = (ImageButton) rootView.findViewById(R.id.setupButton);
        SetupButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v) 
            {

            	int visibility = scrollLayout.getVisibility(); 
            	
            	if(visibility == 4)
            		visibility = 0;
            	else
            		visibility = 4;
            	
            	scrollLayout.setVisibility(visibility);
            	sensLabel.setVisibility(visibility);
            	textStd.setVisibility(visibility);
            		
            }
        });
        
        return rootView;
    }
    

    public static void SetStdDev(double val)
    {
		TextStdDev.setText(String.valueOf(val));

    }
    
    
    
    public void SetProgressBarVisibility(boolean val){
    	if(val)
    	{
			progressBarConnecting.setVisibility(ProgressBar.VISIBLE);
    	}
    	else
    	{
			progressBarConnecting.setVisibility(ProgressBar.INVISIBLE);
    	}
    }
    
    /*
     * Read from the buffer the received data.
     * Then we parse it (its in a special format)
     */
    public void ProcessReceivedData(String data)
    {
        if(Connected == false)
    	{
        	Connected = true; //we received the data, its a "heartbeat"

    		ReceiveBuffer = "";
    		VibrationDataList.clear();
    		VibrationDeviationList.clear();
    		connectingButton.setVisibility(ProgressBar.VISIBLE);  
    		SetProgressBarVisibility(false);
			connectingButton.setVisibility(ImageButton.VISIBLE);

    	}
    	
    	ReceivedData++;
    	
		ReceiveBuffer = ReceiveBuffer + data;
		
		int begin = ReceiveBuffer.indexOf("B");
		int end = ReceiveBuffer.indexOf("E");
		if(end > begin)
		{
			String newString = ReceiveBuffer.substring(begin,end+1);
			ReceiveBuffer = ReceiveBuffer.replace(newString,"");
			newString = newString.replace(" ","");
			newString = newString.replace("B","");
			newString = newString.replace("E","");
			
			if(newString.contains(":"))
			{
				String [] data_split = newString.split(":");
				if(data_split.length == 2)
				{
					SensorData NewData = new SensorData(data_split[0],data_split[1]);
		
					VibrationDataList.add(NewData);
					if(VibrationDataList.size() > 50)
					{
						VibrationDataList.remove(0);
					}
					Algorithms.DetectSwallows();
				}
			}
		}          		
    }
	
    /*
     * 
     * 
     * Some helper functions for Bluetooth
     */
	    @Override
	    public void onStart()
	    {
	        super.onStart();

	        getActivity().registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	        getActivity().registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());
	        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
	    }


	    private void upgradeState(int newState) 
	    {
	        if (newState > state)
	        {
	            updateState(newState);
	        }
	    }

	    private void downgradeState(int newState)
	    {
	        if (newState < state)
	        {
	            updateState(newState);
	        }
	    }

	    private void updateState(int newState)
	    {
	        state = newState;
	    }



	    private void addData(byte[] data)
	    {
	        String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
	        if (ascii != null) 
	        {
	        	ProcessReceivedData(ascii);
	        }
	    }

	    
	    @Override
	    public void onLeScan(BluetoothDevice device, final int rssi, final byte[] scanRecord) 
	    {
	        bluetoothAdapter.stopLeScan(this);
	        bluetoothDevice = device;

	        getActivity().runOnUiThread(new Runnable()
	        {
	            @Override
	            public void run() 
	            {
	                    
      	                if(bluetoothDevice.getName().contains("RF"))
      	                {
      	                	/*
      	                	 * We have scanned and found the RFDuino
      	                	 */
      	               				
      	                	Intent rfduinoIntent = new Intent(getActivity(), RFduinoService.class);
   	                		//bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
      	                	getActivity().bindService(rfduinoIntent, rfduinoServiceConnection, 1);
        					
      	                }
	            }
	        });  
	    }
	    

	    
	    
	    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() 
	    {
	        @Override
	        public void onReceive(Context context, Intent intent)
	        {
	            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
	            if (state == BluetoothAdapter.STATE_ON) 
	            {
	                upgradeState(STATE_DISCONNECTED);
	            }
	            else if (state == BluetoothAdapter.STATE_OFF)
	            {
	                downgradeState(STATE_BLUETOOTH_OFF);
	            }
	        }
	    };
	    
	    
	 

	    private ServiceConnection rfduinoServiceConnection = new ServiceConnection() 
	    {
	        @Override
	        public void onServiceConnected(ComponentName name, IBinder service) 
	        {
	            rfduinoService = ((RFduinoService.LocalBinder) service).getService();
	            if (rfduinoService.initialize()) 
	            {
	            	boolean result = rfduinoService.connect(bluetoothDevice.getAddress());
	            	
	                if (result == true) 
	                {
	                    upgradeState(STATE_CONNECTING);
	                }
	            }
	        }

	        @Override
	        
	        public void onServiceDisconnected(ComponentName name)
	        {
	            rfduinoService = null;
	            downgradeState(STATE_DISCONNECTED);
	        }
	    };
	    
	   

	    
	    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() 
	    {    	
	        @Override
	        public void onReceive(Context context, Intent intent) 
	        {
	            final String action = intent.getAction();
	            
	            if (RFduinoService.ACTION_CONNECTED.equals(action))
	            {
					connectingButton.setVisibility(ImageButton.VISIBLE);

	                upgradeState(STATE_CONNECTED);
	            } 
	            else if (RFduinoService.ACTION_DISCONNECTED.equals(action))
	            {
	                downgradeState(STATE_DISCONNECTED);
	            } 
	            else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action))
	            {
	                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
	            }
	        } 
	    };	    
	
};

class SummaryFragment extends Fragment{
	public SummaryFragment() {}

	// TODO: fixme (to use real user
	// User user = ((MainUI) getActivity()).user;
	User user = new User();

	LinearLayout layout;
	GraphicalView view;
	CaloriesGraph today = new CaloriesGraph("#FFFB00", user.getHbe(), CaloriesGraph.Timescale.DAILY );
	CaloriesGraph week = new CaloriesGraph("#FF4400", user.getHbe(), CaloriesGraph.Timescale.WEEKLY);
	CaloriesGraph month = new CaloriesGraph("#3399FF", user.getHbe(), CaloriesGraph.Timescale.MONTHLY);
	static int current = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_summary, container,false);
		
		TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
		final TextView day = (TextView) rootView.findViewById(R.id.day);
		TextView bmi = (TextView) rootView.findViewById(R.id.bmi);
		TextView bmi1 = (TextView) rootView.findViewById(R.id.bmi1);
		
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		day.setTypeface(tf);
		bmi.setTypeface(tf);
		bmi1.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);
		layout = (LinearLayout) rootView.findViewById(R.id.layout1);
		
		view = today.getView(getActivity());
		layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		today.setDefaultDailyGraph();
		view.repaint();
		
		ImageButton prev = (ImageButton) rootView.findViewById(R.id.prev);
		ImageButton next = (ImageButton) rootView.findViewById(R.id.next);
		
		prev.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				if(current == 0){
					current = 2;
					day.setText("Last Month");
					view = null;
					view = month.getView(getActivity()); 
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					month.setDefaultMonthlyGraph();
					view.repaint();
				}else if(current == 1){
					current = 0;
					day.setText("Today");
					view = null;
					view = today.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));


					today.setDefaultDailyGraph();
					view.repaint();
				}else if(current == 2){
					current = 1;
					day.setText("Last Week");
					view = null;
					view = week.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					week.setDefaultWeeklyGraph();
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
					day.setText("Last Week");
					view = null;
					view = week.getView(getActivity()); 
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					week.setDefaultWeeklyGraph();
					view.repaint();
				}else if(current == 1){
					current = 2;
					day.setText("Last Month");
					view = null;
					view = month.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					month.setDefaultMonthlyGraph();
					view.repaint();
				}else if(current == 2){
					current = 0;
					day.setText("Today");
					view = null;
					view = today.getView(getActivity());
					layout.removeAllViews();
					layout.addView(view, new LayoutParams(LayoutParams.FILL_PARENT,
							LayoutParams.FILL_PARENT));

					today.setDefaultDailyGraph();
					view.repaint();
				}
				
			}
		});
		
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
		TextView text = (TextView) rootView.findViewById(R.id.txtLabel);
		Typeface tf = Typeface.createFromAsset(getActivity().getAssets(), "Aaargh.ttf");
		text.setTypeface(tf);
		Drawable bg = rootView.findViewById(R.id.txtLabel).getBackground();
		bg.setAlpha(127);

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
