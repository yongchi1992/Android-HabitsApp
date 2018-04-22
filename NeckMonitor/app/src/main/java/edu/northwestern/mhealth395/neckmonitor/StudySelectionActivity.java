package edu.northwestern.mhealth395.neckmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.LinkedList;
import java.util.List;

public class StudySelectionActivity extends Activity {

    public static final String USER_EXTRA = "userID";

    private String uName;
    private final String TAG = "StudySelectionActivity:";
    private ListAdapter adapter;
    private List<String> studies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_selection);

        // To add a study add to studies list
        studies = new LinkedList();
        adapter = new StringAdapter(this, studies);
        ((ListView) findViewById(R.id.studyListView)).setAdapter(adapter);

        ((ListView) findViewById(R.id.studyListView)).setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Get the text from the view
                        onStudyClick(studies.get(position));
                    }
                }
        );

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        boolean ok = true;
        if (extras != null) {
            uName = extras.getString(USER_EXTRA);
            if (uName == null) {
                ok = false;
            } else {
               new getUserStudiesTAsk().execute(uName);
            }

        } else {
            Log.e(TAG, "No extras were passed");
            ok = false;
        }

        if (!ok) {
            Intent i = new Intent(StudySelectionActivity.this, LoginActivity.class);
            startActivity(i);
        }
    }


    private class getUserStudiesTAsk extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            getStudiesForUser(params[0]);
            return null;
        }
    }


    private void getStudiesForUser(String userName) {
        SQLiteOpenHelper dbHelper = DataStorageContract.NecklaceDbHelper.getInstance(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if (db == null) {
            dbHelper.onCreate(db);
        }

        Cursor c = null, cur = null;
        try {
            // SELECT study_id FROM User_Table WHERE name="userName"
            if (db != null) {
                c = db.query(DataStorageContract.UserTable.TABLE_NAME,
                        new String[]{DataStorageContract.UserTable.COLUMN_NAME_STUDY_ID},
                        DataStorageContract.StudyTable.COLUMN_NAME_NAME + "=?",
                        new String[]{userName},
                        null,
                        null,
                        null);
            }
            else {
                throw new NullPointerException();
            }


            c.moveToFirst();
            int studyNameIndex = c.getColumnIndexOrThrow(DataStorageContract.UserTable.COLUMN_NAME_STUDY_ID);
            while (!c.isAfterLast()) {


                // SELECT name FROM Study_Table WHERE _ID="id"
                // foreach entry in Cursor c, add name to listAdapter
                // Update ListView
                cur = db.query(DataStorageContract.StudyTable.TABLE_NAME,
                        new String[] {DataStorageContract.StudyTable.COLUMN_NAME_NAME,
                                DataStorageContract.StudyTable._ID},
                        DataStorageContract.StudyTable._ID + "=?",
                        new String[] {Integer.toString(c.getInt(studyNameIndex))},
                        null,
                        null,
                        null);
                int studyNameColumn = cur.getColumnIndex(DataStorageContract.StudyTable.COLUMN_NAME_NAME);

                cur.moveToFirst();
                Log.v(TAG, cur.getString(studyNameColumn));
                studies.add(cur.getString(studyNameColumn));


                c.moveToNext();
            }




        } catch (SQLException e1) {
            e1.printStackTrace();
        } catch (NullPointerException e2) {

        } finally {
            if (c != null) {
                c.close();
                if (cur != null)
                    cur.close();
            }
        }
    }

    public void onStartStudyClicked(View view) {
        String studyName = ((EditText) findViewById(R.id.studyNameField)).getText().toString();

        // Create new study if it does't exist in studies
        if (!studies.contains(studyName)) {
            // Insert a study into the table
            Cursor c = null;
            SQLiteOpenHelper dbHelper = DataStorageContract.NecklaceDbHelper.getInstance(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                ContentValues vals = new ContentValues();
                vals.put(DataStorageContract.StudyTable.COLUMN_NAME_NAME, studyName);

                db.insert(DataStorageContract.StudyTable.TABLE_NAME,
                        null,
                        vals);

                c = db.query(DataStorageContract.StudyTable.TABLE_NAME,
                        new String[] {DataStorageContract.StudyTable._ID},
                        DataStorageContract.StudyTable.COLUMN_NAME_NAME + "=?",
                        new String[] {studyName},
                        null,
                        null,
                        null
                );

                c.moveToFirst();
                int studyId = c.getInt(c.getColumnIndex(DataStorageContract.StudyTable._ID));

                vals.clear();
                vals.put(DataStorageContract.UserTable.COLUMN_NAME_NAME, uName);
                vals.put(DataStorageContract.UserTable.COLUMN_NAME_STUDY_ID, studyId);
                db.insert(DataStorageContract.UserTable.TABLE_NAME,
                        null,
                        vals);

                c = db.query(DataStorageContract.UserTable.TABLE_NAME,
                        new String[] {DataStorageContract.UserTable._ID},
                        DataStorageContract.UserTable.COLUMN_NAME_NAME + "=? AND " +
                                DataStorageContract.UserTable.COLUMN_NAME_STUDY_ID + "=?",
                        new String[] {uName, Integer.toString(studyId)},
                        null,
                        null,
                        null);

                c.moveToFirst();
                int userId = c.getInt(c.getColumnIndex(DataStorageContract.UserTable._ID));

                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    // Device does not support Bluetooth
                } else {
                    if (mBluetoothAdapter.isEnabled()) {
                        // Bluetooth is enabled
                        Intent intent = new Intent(this, DeviceManagement.class);
                        intent.putExtra(DeviceManagement.USER_NAME_EXTRA, uName);
                        intent.putExtra(DeviceManagement.USER_ID_EXTRA, userId);
                        intent.putExtra(StreamActivity.STUDY_EXTRA, studyName);
                        startActivity(intent);
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Bluetooth")
                                .setMessage("Warning: bluetooth is not enabled")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
            } finally {
                if (c!=null)
                    c.close();
            }


        } else {
            Log.e(TAG, "study already exists");
        }
    }

    public void onStudyClick(String sName) {
        // Get the study id with sName
        Cursor c = null;
        try {

            // Add study to the database
            SQLiteDatabase db = DataStorageContract.NecklaceDbHelper.getInstance(this).getWritableDatabase();

            c = db.query(DataStorageContract.StudyTable.TABLE_NAME,
                    new String[] {DataStorageContract.StudyTable._ID},
                    DataStorageContract.StudyTable.COLUMN_NAME_NAME + "=?",
                    new String[] {sName},
                    null,
                    null,
                    null);
            Log.v(TAG, "Study name: " + sName);

            c.moveToFirst();
            int studyId = c.getInt(c.getColumnIndex(DataStorageContract.StudyTable._ID));

            c = db.query(DataStorageContract.UserTable.TABLE_NAME,
                    new String[] {DataStorageContract.UserTable._ID},
                    DataStorageContract.UserTable.COLUMN_NAME_NAME + "=? AND " +
                            DataStorageContract.UserTable.COLUMN_NAME_STUDY_ID + "=?",
                    new String[] {uName, Integer.toString(studyId)},
                    null,
                    null,
                    null);

            c.moveToFirst();
            int userId = c.getInt(c.getColumnIndex(DataStorageContract.UserTable._ID));

            Intent intent = new Intent(this, DeviceManagement.class);
            intent.putExtra(DeviceManagement.USER_ID_EXTRA, userId);
            intent.putExtra(DeviceManagement.STUDY_EXTRA, sName);
            startActivity(intent);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (c != null)
                c.close();
        }
    }


}
