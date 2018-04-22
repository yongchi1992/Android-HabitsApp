package edu.northwestern.mhealth395.neckmonitor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by William on 2/8/2016
 */
public class DataStorageContract {

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String PRIMARY = " INTEGER PRIMARY KEY";
    private static final String FLOAT_TYPE = " REAL";
    private static final String DATETIME_TYPE = " DATETIME";
    private static final String COMMA_SEP = ",";
    private static final String AUTOINC = " AUTOINCREMENT";

    // Deprecated
    public static abstract class NecklaceTable implements BaseColumns {
        public static final String TABLE_NAME = "Necklace_Table";
        public static final String COLUMN_NAME_ACCX = "acc_x";
        public static final String COLUMN_NAME_ACCY = "acc_y";
        public static final String COLUMN_NAME_ACCZ = "acc_z";
        public static final String COLUMN_NAME_AUDIO = "audio";
        public static final String COLUMN_NAME_VIBRATION = "vibration";
        public static final String COLUMN_NAME_TIMESTAMP = "time_stamp";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_ACCX + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACCY + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACCZ + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_AUDIO + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_VIBRATION + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIMESTAMP + DATETIME_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class StudyTable implements BaseColumns {
        public static final String TABLE_NAME = "Study_Table";
        public static final String COLUMN_NAME_NAME = "name";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + PRIMARY + AUTOINC + COMMA_SEP +
                        COLUMN_NAME_NAME + TEXT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class UserTable implements BaseColumns {
        public static final String TABLE_NAME = "User_Table";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_STUDY_ID = "study_id";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + PRIMARY + AUTOINC + COMMA_SEP +
                        COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_STUDY_ID + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS "+ TABLE_NAME;

    }

    public static abstract class DeviceTable implements BaseColumns {
        public static final String TABLE_NAME = "Device_Table";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_USER_ID = "user_id";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + PRIMARY + AUTOINC + COMMA_SEP +
                        COLUMN_NAME_TYPE + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_USER_ID + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class AccTable implements BaseColumns {
        public static final String TABLE_NAME = "Accelerometer_Table";
        public static final String COLUMN_NAME_X = "x";
        public static final String COLUMN_NAME_Y = "y";
        public static final String COLUMN_NAME_Z = "z";
        public static final String COLUMN_NAME_DEV_ID = "acc_device_id";
        public static final String COLUMN_NAME_TIME = "time_stamp";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_DEV_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_X + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_Y + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_Z + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + DATETIME_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class AudioTable implements BaseColumns {
        public static final String TABLE_NAME = "Audio_Table";
        public static final String COLUMN_NAME_DATA = "audio_data";
        public static final String COLUMN_NAME_DEV_ID = "audio_device_id";
        public static final String COLUMN_NAME_TIME = "time_stamp";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_DEV_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DATA + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + DATETIME_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class CounterTable implements BaseColumns {
        public static final String TABLE_NAME = "Counter_table";
        public static final String COLUMN_NAME_COUNTER = "counter";
        public static final String COLUMN_NAME_TIME = "time";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_COUNTER + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + INT_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

    public static abstract class LabelTable implements BaseColumns {
        public static final String TABLE_NAME = "Label_table";
        public static final String COLUMN_NAME_LABEL = "label";
        public static final String COLUMN_NAME_TIME = "time_stamp";
        public static final int VALUE_NOTHING = 3;
        public static final int VALUE_EATING = 1;
        public static final int VALUE_DRINKING = 2;
        public static final int VALUE_SWALLOW = 4;

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_LABEL + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + DATETIME_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

    public static abstract class PiezoTable implements BaseColumns {
        public static final String TABLE_NAME = "Piezo_Table";
        public static final String COLUMN_NAME_DATA = "piezo_data";
        public static final String COLUMN_NAME_DEV_ID = "piezo_device_id";
        public static final String COLUMN_NAME_TIME = "time_stamp";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_DATA + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_DEV_ID + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + DATETIME_TYPE +
                        " )";
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static abstract class DataTable implements BaseColumns {
        public static final String TABLE_NAME = "Data_Table";
        public static final String COLUMN_NAME_STUDY = "Study";
        public static final String COLUMN_NAME_USER = "User";
//        public static final String COLUMN_NAME_DEVICE =
        public static final String COLUMN_NAME_ACC_X = "accx";
        public static final String COLUMN_NAME_ACC_Y = "accy";
        public static final String COLUMN_NAME_ACC_Z = "accz";
        public static final String COLUMN_NAME_AUDIO = "audio";
        public static final String COLUMN_NAME_PIEZO = "piezo";
        public static final String COLUMN_NAME_TIME = "timestamp";
        public static final String COLUMN_NAME_LABLE = "lable";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        COLUMN_NAME_STUDY + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_USER + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACC_X + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACC_Y + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ACC_Z + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_AUDIO + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_PIEZO + FLOAT_TYPE + COMMA_SEP +
                        COLUMN_NAME_TIME + INT_TYPE + COMMA_SEP +
                        COLUMN_NAME_LABLE + INT_TYPE +
                        " )";
        private  static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }

    public static class NecklaceDbHelper extends SQLiteOpenHelper {

        private static NecklaceDbHelper sInstance;

        public static synchronized NecklaceDbHelper getInstance(Context context) {
            if (sInstance == null) {
                sInstance = new NecklaceDbHelper(context.getApplicationContext());
            }
            return sInstance;
        }

        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 19;
        public static final String DATABASE_NAME = "Necklace.db";

        private NecklaceDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(StudyTable.SQL_CREATE_ENTRIES);
            db.execSQL(UserTable.SQL_CREATE_ENTRIES);
            db.execSQL(DeviceTable.SQL_CREATE_ENTRIES);
            db.execSQL(AccTable.SQL_CREATE_ENTRIES);
            db.execSQL(AudioTable.SQL_CREATE_ENTRIES);
            db.execSQL(CounterTable.SQL_DELETE_ENTRIES);
            db.execSQL(LabelTable.SQL_CREATE_ENTRIES);
            db.execSQL(PiezoTable.SQL_CREATE_ENTRIES);
            db.execSQL(DataTable.SQL_CREATE_ENTRIES);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(StudyTable.SQL_DELETE_ENTRIES);
            db.execSQL(UserTable.SQL_DELETE_ENTRIES);
            db.execSQL(DeviceTable.SQL_DELETE_ENTRIES);
            db.execSQL(AccTable.SQL_DELETE_ENTRIES);
            db.execSQL(AudioTable.SQL_DELETE_ENTRIES);
            db.execSQL(CounterTable.SQL_CREATE_ENTRIES);
            db.execSQL(LabelTable.SQL_DELETE_ENTRIES);
            db.execSQL(PiezoTable.SQL_DELETE_ENTRIES);
            db.execSQL(NecklaceTable.SQL_DELETE_ENTRIES);
            db.execSQL(DataTable.SQL_DELETE_ENTRIES);
            Log.v("Db", "Deleted tables");
            onCreate(db);
            Log.v("DB", "created new database");
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
}
