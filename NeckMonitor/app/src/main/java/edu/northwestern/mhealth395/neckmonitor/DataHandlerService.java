package edu.northwestern.mhealth395.neckmonitor;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class DataHandlerService extends Service {
    public DataHandlerService() {
    }

    public static final String DEVICE_EXTRA = "device";
    public static final String START_EXTRA = "start";
    public static final String USER_ID_EXTRA = "USERid";
    public static final String USER_NAME_EXTRA = "Username";
    public static final String LABEL_EXTRA = "label";
    public static final String B_TYPE_LABEL = "broadcastLabel";
    public static final String B_TYPE_CSV = "broadcastCsv";
    public static final String CSV_STREAM_EXTRA = "csvStream";
    public static final String STUDY_EXTRA = "study";

    private static final String T_NECKLACE = "necklace";

    private String userID;
    private String userName;
    private String studyId;
    private int label = 3;
    private boolean isCsvStreaming = true;

    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";
    public final static UUID UUID_SERVICE = sixteenBitUuid(0x2220);
    public final static UUID UUID_RECEIVE = sixteenBitUuid(0x2221);
    public final static UUID UUID_CLIENT_CONFIGURATION = sixteenBitUuid(0x2902);
    private final static String ACTION_DATA_AVAILABLE =
            "com.rfduino.ACTION_DATA_AVAILABLE";

    public static UUID sixteenBitUuid(long shortUuid) {
        assert shortUuid >= 0 && shortUuid <= 0xFFFF;
        return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
    }

    private final String TAG = "DataHandlerService";
    private HashMap<BluetoothDevice, BluetoothGatt> devices = new HashMap();
    private SQLiteDatabase db;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Bundle extras = intent.getExtras();

        BluetoothDevice device = null;
        boolean startStream = true;
        db = DataStorageContract.NecklaceDbHelper.getInstance(this).getWritableDatabase();

        if (extras != null) {
            userID = extras.getString(USER_ID_EXTRA);
            userName = extras.getString(USER_NAME_EXTRA);
            studyId = extras.getString(STUDY_EXTRA);
            label = extras.getInt(LABEL_EXTRA);
            device = (BluetoothDevice) extras.get(DEVICE_EXTRA);
            startStream = extras.getBoolean(START_EXTRA);
        }

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    label = extras.getInt(LABEL_EXTRA);
                }
            }
        }, new IntentFilter(B_TYPE_LABEL));


        if (device != null) {
            if (!devices.containsKey(device) && startStream) {
                Log.v(TAG, "Starting stream...");
                devices.put(device, device.connectGatt(this, true, gattCallback));
                doDetection = true;
                Intent bIntent = new Intent(StreamActivity.BROADCAST_NAME);
                bIntent.putExtra(StreamActivity.BROADCAST_CONNECTED, "");
                sendBroadcast(bIntent);

            } else if (devices.containsKey(device) && !startStream) {
                Log.v(TAG, "Stopping stream...");
                doDetection = false;
                ((BluetoothGatt) devices.get(device)).disconnect();
                devices.remove(device);
            } else {
                Log.w(TAG, "Device is already streaming and startstream called (or opposite)");
            }
        }

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intent) {
                isCsvStreaming = intent.getExtras().getBoolean(CSV_STREAM_EXTRA);
                Handler mHandler = new Handler();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isCsvStreaming) {
                            Toast.makeText(context, "Streaming to CSV", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "No longer streaming to CSV", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }, new IntentFilter(B_TYPE_CSV));


        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.v("BluetoothLE ", "Status: " + status);
            Intent broadcastIntent = new Intent("asdf");
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    broadcastIntent.setAction(StreamActivity.BROADCAST_CONNECTED);
                    gatt.discoverServices();
                    sendBroadcast(broadcastIntent);
                    //new DetectSwallow().doInBackground();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.w("gattCallback", "STATE_DISCONNECTED");
                    broadcastIntent.setAction(StreamActivity.BROADCAST_DISCONNECTED);
                    sendBroadcast(broadcastIntent);
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService mBluetoothGattService = gatt.getService(UUID_SERVICE);
                if (mBluetoothGattService == null) {
                    Log.e(TAG, "RFduino GATT service not found!");
                    return;
                }

                BluetoothGattCharacteristic receiveCharacteristic =
                        mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
                if (receiveCharacteristic != null) {
                    BluetoothGattDescriptor receiveConfigDescriptor =
                            receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                    if (receiveConfigDescriptor != null) {
                        gatt.setCharacteristicNotification(receiveCharacteristic, true);

                        receiveConfigDescriptor.setValue(
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(receiveConfigDescriptor);
                    } else {
                        Log.e(TAG, "RFduino receive config descriptor not found!");
                    }

                } else {
                    Log.e(TAG, "RFduino receive characteristic not found!");
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.v(TAG, "Characteristic read!!!");

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.v(TAG, "Characteristic changed!!!");
            if (label == 0) {
                label = 3;
            }
            NecklaceEvent event = new NecklaceEvent(characteristic.getValue());
            new StoreEventTask().doInBackground(event);
        }
    };

    protected static int count = 0;

    public class StoreEventTask extends AsyncTask<NecklaceEvent, Void, Void> {
        @Override
        protected Void doInBackground(NecklaceEvent... params) {
            NecklaceEvent event = params[0];
            count ++;

            if (event != null) {
                Log.v(TAG, "Task Started");

                Log.v(TAG, "Accx: " + Float.toString(event.getFloat0()));
                Log.v(TAG, "Accy: " + Float.toString(event.getFloat2()));
                Log.v(TAG, "Accz: " + Float.toString(event.getFloat3()));
                Log.v(TAG, "Audio: " + Float.toString(event.getFloat1()));
//                Log.v(TAG, "COUNTER: " + Float.toString(event.getFloat1()));
                Log.v(TAG, "Vib: " + Float.toString(event.getFloat4()));

                if (count >= 5) {
                    count = 0;
                    Intent bIntent = new Intent(StreamActivity.BROADCAST_DATA);
                    bIntent.putExtra(StreamActivity.B_DATA_ACC_X, event.getFloat0());
                    bIntent.putExtra(StreamActivity.B_DATA_ACC_Y, event.getFloat2());
                    bIntent.putExtra(StreamActivity.B_DATA_ACC_Z, event.getFloat3());
                    bIntent.putExtra(StreamActivity.B_DATA_AUDIO, event.getFloat1());
                    bIntent.putExtra(StreamActivity.B_DATA_PEIZO, event.getFloat4());
                    sendBroadcast(bIntent);
                }
                ContentValues values = new ContentValues();

                try {

                    values.put(DataStorageContract.DataTable.COLUMN_NAME_STUDY, studyId);
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_USER, userID);
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_ACC_X, event.getFloat0());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_ACC_Y, event.getFloat2());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_ACC_Z, event.getFloat3());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_AUDIO, event.getFloat1());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_PIEZO, event.getFloat4());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_TIME, event.getTimeStamp());
                    values.put(DataStorageContract.DataTable.COLUMN_NAME_LABLE, label);

                    db.insert(DataStorageContract.DataTable.TABLE_NAME,
                            null,
                            values);

                    values.clear();

                    /* OLD IMPLEMENTATION OF STORING INTO TABLES
                    int devNo = getDeviceIdOrInsert(userID);
                    Log.v(TAG, "DevNo is " + Integer.toString(devNo));
                    devId = devNo;

                    // Insert accelerometer data
                    values.clear();
                    values.put(DataStorageContract.AccTable.COLUMN_NAME_DEV_ID, devNo);
                    values.put(DataStorageContract.AccTable.COLUMN_NAME_X, event.getFloat0());
                    values.put(DataStorageContract.AccTable.COLUMN_NAME_Y, event.getFloat2());
                    values.put(DataStorageContract.AccTable.COLUMN_NAME_Z, event.getFloat3());
                    values.put(DataStorageContract.AccTable.COLUMN_NAME_TIME, event.getTimeStamp());

                    db.insert(DataStorageContract.AccTable.TABLE_NAME,
                            null,
                            values);

                    // Insert audio data
                    values.clear();
                    values.put(DataStorageContract.AudioTable.COLUMN_NAME_DEV_ID, devNo);
                    values.put(DataStorageContract.AudioTable.COLUMN_NAME_DATA, event.getFloat1());
                    values.put(DataStorageContract.AudioTable.COLUMN_NAME_TIME, event.getTimeStamp());

                    db.insert(DataStorageContract.AudioTable.TABLE_NAME,
                            null,
                            values);
//                    values.put(DataStorageContract.CounterTable.COLUMN_NAME_COUNTER, event.getFloat1());
//                    values.put(DataStorageContract.CounterTable.COLUMN_NAME_TIME, event.getTimeStamp());
//                    db.insert(DataStorageContract.CounterTable.TABLE_NAME,
//                            null,
//                            values);

                    // Insert piezo data
                    values.clear();
                    values.put(DataStorageContract.PiezoTable.COLUMN_NAME_DEV_ID, devNo);
                    values.put(DataStorageContract.PiezoTable.COLUMN_NAME_DATA, event.getFloat4());
                    values.put(DataStorageContract.PiezoTable.COLUMN_NAME_TIME, event.getTimeStamp());//Long.toString(event.getTimeStamp()));

                    db.insert(DataStorageContract.PiezoTable.TABLE_NAME,
                            null,
                            values);


                    values.clear();
                    values.put(DataStorageContract.LabelTable.COLUMN_NAME_TIME, event.getTimeStamp());
                    values.put(DataStorageContract.LabelTable.COLUMN_NAME_LABEL, label);

                    db.insert(DataStorageContract.LabelTable.TABLE_NAME,
                            null,
                            values);
                            */

                    Log.v(TAG, "SUCCESSFULLY ENTERED DATA");
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e(TAG, "No event passed to StoreEventTask");
            }
            // Write the event to the csv
            if (isCsvStreaming)
                postToCSV(event);
            return null;
        }

    }

    private int getDeviceIdOrInsert(int userId) {
        int devNo;

        Log.v(TAG, "Adding device for userid " + Integer.toString(userId));
        // Check to see if the device already exists in this study
        Cursor c = db.query(DataStorageContract.DeviceTable.TABLE_NAME,
                new String[]{DataStorageContract.DeviceTable._ID},
                DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID + "=? AND " +
                        DataStorageContract.DeviceTable.COLUMN_NAME_TYPE + "=?",
                new String[]{Integer.toString(userId), T_NECKLACE},
                null,
                null,
                null);

        // If the name does not exist...
        if (c.getCount() == 0) {
            ContentValues vals = new ContentValues();
            vals.put(DataStorageContract.DeviceTable.COLUMN_NAME_TYPE, T_NECKLACE);
            vals.put(DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID, userId);
            vals.put(DataStorageContract.DeviceTable._ID, 0);

            try {
                db.insertOrThrow(DataStorageContract.DeviceTable.TABLE_NAME,
                        null,
                        vals);
            } catch (SQLException e) {
                Log.e(TAG, "Failed to insert into device table");
                e.printStackTrace();
            }

            // Get id
            c = db.query(DataStorageContract.DeviceTable.TABLE_NAME,
                    new String[]{DataStorageContract.DeviceTable._ID},
                    DataStorageContract.DeviceTable.COLUMN_NAME_TYPE + "=? AND " +
                            DataStorageContract.DeviceTable.COLUMN_NAME_USER_ID + "=?",
                    new String[]{T_NECKLACE, Integer.toString(userId)},
                    null,
                    null,
                    null);

            c.moveToFirst();
            devNo = c.getInt(c.getColumnIndex(DataStorageContract.DeviceTable._ID));
        } else {
            c.moveToFirst();
            devNo = c.getInt(c.getColumnIndexOrThrow(DataStorageContract.StudyTable._ID));
        }
        c.close();
        return devNo;
    }


    private boolean doDetection = false;
    private final long WAIT_TIME = 1500;
    public int devId;

    private class DetectSwallow extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Cursor cursor;
                    while (doDetection) {
                        try {
                            Thread.sleep(WAIT_TIME);
                            String MY_QUERY =
                                    "SELECT " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_X
                                            + ", " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_Y
                                            + ", " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_Z
                                            + ", " + DataStorageContract.AudioTable.TABLE_NAME + "." + DataStorageContract.AudioTable.COLUMN_NAME_DATA
                                            + ", " + DataStorageContract.PiezoTable.TABLE_NAME + "." + DataStorageContract.PiezoTable.COLUMN_NAME_DATA
                                            + " FROM " + DataStorageContract.AccTable.TABLE_NAME
                                            + " LEFT JOIN " + DataStorageContract.AudioTable.TABLE_NAME
                                            + " ON " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_TIME
                                            + "=" + DataStorageContract.AudioTable.TABLE_NAME + "." + DataStorageContract.AudioTable.COLUMN_NAME_TIME
                                            + " LEFT JOIN " + DataStorageContract.PiezoTable.TABLE_NAME
                                            + " ON " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_TIME
                                            + "=" + DataStorageContract.PiezoTable.TABLE_NAME + "." + DataStorageContract.PiezoTable.COLUMN_NAME_TIME
                                            + " WHERE " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_DEV_ID
                                            + "=" + DataStorageContract.AccTable.COLUMN_NAME_DEV_ID
                                            + " ORDER BY " + DataStorageContract.AccTable.TABLE_NAME + "." + DataStorageContract.AccTable.COLUMN_NAME_TIME
                                            + " DESC "
                                            + " LIMIT 50";
//                            Log.i(TAG, MY_QUERY);
                            cursor = db.rawQuery(MY_QUERY, new String[]{});
                            // Detect a swallow
                            if (detectSwallow(cursor)) {
                                //Broadcast a swallow
                                Log.d(TAG, "Swallow!");
                                Intent bIntent = new Intent(StreamActivity.BROADCAST_DATA);
                                bIntent.putExtra(StreamActivity.B_DATA_SWALLOW, true);
                                sendBroadcast(bIntent);
                            } else {
                                Log.v(TAG, "No Swallow");
                            }


                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.e(TAG, "Detecting swallow failed to sleep");
                        }
                    }
                }
            });
            thread.start();
            return null;
        }
    }

    private boolean detectSwallow(Cursor c) {
        // Indices for getting data from the cursor
        int accxIndex = c.getColumnIndex(DataStorageContract.AccTable.COLUMN_NAME_X);
        int accyIndex = c.getColumnIndex(DataStorageContract.AccTable.COLUMN_NAME_Y);
        int acczIndex = c.getColumnIndex(DataStorageContract.AccTable.COLUMN_NAME_Z);
        int audioIndex = c.getColumnIndex(DataStorageContract.AudioTable.COLUMN_NAME_DATA);
        int piezoIndex = c.getColumnIndex(DataStorageContract.PiezoTable.COLUMN_NAME_DATA);

        float sumAccX = 0;
        float sumAccY = 0;
        float sumAccZ = 0;
        float count = 0;

        float avgAccX;
        float avgAccY;
        float avgAccZ;

        float sumPiezo = 0;
        float maxPiezo = 0;
        float avgPiezo;
        int piezoThresh = 400;

        c.moveToFirst();
        while (!c.isAfterLast()) {
            sumAccX += c.getFloat(accxIndex);
            sumAccY += c.getFloat(accyIndex);
            sumAccZ += c.getFloat(acczIndex);
            sumPiezo += c.getFloat(piezoIndex);
            if (c.getFloat(piezoIndex) > maxPiezo)
                maxPiezo = c.getFloat(piezoIndex);
            count += 1;
            c.moveToNext();
        }

//        Log.e(TAG, "Count = " + Float.toString(count));

        avgAccX = sumAccX / count;
        avgAccY = sumAccY / count;
        avgAccZ = sumAccZ / count;

        avgPiezo = sumPiezo / count;

        Log.e(TAG, Float.toString(maxPiezo));
        if (avgPiezo > piezoThresh) {
            return true;
        }
//        if (!(avgAccX > 1.5 && (avgAccZ < 1.0 || avgAccZ > 2.0))) {
//        } else {
//            if (avgPiezo > piezoThresh) {
//                return true;
//            }
//        }


        return false;
    }

    public void postToCSV(NecklaceEvent event) {
        {

            File folder = new File(Environment.getExternalStorageDirectory()
                    + "/NeckMonitorData");

            boolean var = false;
            if (!folder.exists())
                var = folder.mkdir();

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy kk");
            String formattedDate = df.format(cal.getTime());
            final String filename = folder.toString() + "/" + studyId + " " + formattedDate + ".csv";

            File file = new File(filename);

// If file does not exists, then create it
            boolean fpExists = true;
            if (!file.exists()) {
                try {
                    boolean fC = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fpExists = false;
            }


            // Post data to the csv
            FileWriter fw;

            try {
                fw = new FileWriter(filename, true);
                if (!fpExists) {
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    sendBroadcast(intent);
                    fw.append("Study,User,Sensor0,Sensor1,Sensor2,Sensor3,Sensor4,time,label\n");
                }
                fw.append(studyId);
                fw.append(',');
                fw.append(userName);
                fw.append(',');
                fw.append(Float.toString(event.getFloat0()));
                fw.append(',');
                fw.append(Float.toString(event.getFloat1()));
                fw.append(',');
                fw.append(Float.toString(event.getFloat2()));
                fw.append(',');
                fw.append(Float.toString(event.getFloat3()));
                fw.append(',');
                fw.append(Float.toString(event.getFloat4()));
                fw.append(',');
                fw.append(Long.toString(event.getTimeStamp()));
                fw.append(',');
                fw.append(Integer.toString(label));
                fw.append('\n');
                fw.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to write to csv");
            }
        }
    }
}
