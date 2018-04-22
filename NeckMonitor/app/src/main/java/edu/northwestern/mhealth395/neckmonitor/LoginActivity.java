package edu.northwestern.mhealth395.neckmonitor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends Activity {

    private final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        storagePermitted(this);
    }

    private static boolean storagePermitted(Activity activity) {

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED)

            return true;

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

        return false;

    }

    public void onLoginClicked(View view) {

//        SQLiteOpenHelper dbHelper = DataStorageContract.NecklaceDbHelper.getInstance(this);
//        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // User name
        String name = ((EditText) findViewById(R.id.userIdField)).getText().toString();

        Intent intent = new Intent(LoginActivity.this, StudySelectionActivity.class);
        intent.putExtra(StudySelectionActivity.USER_EXTRA, name);
        startActivity(intent);


//        // Query the db for user
//        if (db == null) {
//            Log.w(TAG, "Database was null");
//            dbHelper.onCreate(db);
//        }
//
//        Cursor c;
//        try {
//
//            if (db != null) {
//                c = db.query(DataStorageContract.UserTable.TABLE_NAME,
//                        new String[] {DataStorageContract.UserTable._ID,
//                                DataStorageContract.UserTable.COLUMN_NAME_NAME},
//                        DataStorageContract.StudyTable.COLUMN_NAME_NAME + "=?",
//                        new String[] {name},
//                        null,
//                        null,
//                        null);
//
//                if (c.getCount() == 0) {
//                    Log.v(TAG, "Creating a new user");
//
//                    ContentValues values = new ContentValues();
//                    values.put(DataStorageContract.UserTable.COLUMN_NAME_NAME, name);
//
//                    // Get current highest id
//                    c = db.query(DataStorageContract.UserTable.TABLE_NAME,
//                            new String[] { DataStorageContract.UserTable._ID })
//
//
//
//
//
//
//                }
//            } else {
//                throw new NullPointerException();
//            }
//        } catch (NullPointerException e) {
//            Log.e(TAG, "db was null");
//            e.printStackTrace();
//        } catch (SQLException e1) {
//            Log.e(TAG, "Failed to query database");
//            e1.printStackTrace();
//        } finally {
//            c.close();
//        }

    }
}
