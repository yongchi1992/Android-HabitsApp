package edu.northwestern.mhealth395.neckmonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LabelActivity extends Activity {

    private int swallowCount = 0;

    private static final String TAG = "LabelActivity";

    private Button chewButton;
    private Button drinkButton;
    private Button nothingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label);

        nothingButton = (Button) findViewById(R.id.nothingBUtton);
        nothingButton.setEnabled(false);
        chewButton = (Button) findViewById(R.id.chewButton);
        chewButton.setEnabled(true);
        drinkButton = (Button) findViewById(R.id.drinkButton);
        drinkButton.setEnabled(true);

        // Register broadcast listener for disconnect
        IntentFilter filter = new IntentFilter();
        filter.addAction(StreamActivity.BROADCAST_DISCONNECTED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "Disconnected!");

                chewButton.getRootView().setBackgroundColor(Color.RED);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        context).setTitle("Warning: necklace disconnected")
                        .setMessage("Please make sure the necklace is on and in range")
                        .setCancelable(false)
                        .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                chewButton.getRootView().setBackgroundColor(Color.WHITE);
                            }
                        });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();

            }
        }, filter);

        findViewById(R.id.swallowButton).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (v.getId() == R.id.swallowButton) {
                            // Start
                            Intent intent = new Intent(DataHandlerService.B_TYPE_LABEL);
                            intent.putExtra(DataHandlerService.LABEL_EXTRA,
                                    DataStorageContract.LabelTable.VALUE_SWALLOW);
                            sendBroadcast(intent);
                            swallowCount++;
                            ((TextView) findViewById(R.id.swallowLabel)).setText("Swallows: " + Integer.toString(swallowCount));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (v.getId() == R.id.swallowButton) {
                            // Whatever is disabled should be the next label
                            if (!chewButton.isEnabled()) {
                                // Eating
                                Intent reIntent = new Intent(DataHandlerService.B_TYPE_LABEL);
                                reIntent.putExtra(DataHandlerService.LABEL_EXTRA,
                                        DataStorageContract.LabelTable.VALUE_EATING);
                                sendBroadcast(reIntent);

                            } else if (!drinkButton.isEnabled()) {
                                // Drinking
                                Intent reIntent = new Intent(DataHandlerService.B_TYPE_LABEL);
                                reIntent.putExtra(DataHandlerService.LABEL_EXTRA,
                                        DataStorageContract.LabelTable.VALUE_DRINKING);
                                sendBroadcast(reIntent);

                            } else if (!nothingButton.isEnabled()) {
                                // Nothing label

                                Intent reIntent = new Intent(DataHandlerService.B_TYPE_LABEL);
                                reIntent.putExtra(DataHandlerService.LABEL_EXTRA,
                                        DataStorageContract.LabelTable.VALUE_NOTHING);
                                sendBroadcast(reIntent);
                            }

                        }
                        break;
                }
                return false;
            }
        });
    }


    public void onNothing(View view) {
        // broadcast nothing to service
        Intent intent = new Intent(DataHandlerService.B_TYPE_LABEL);
        intent.putExtra(DataHandlerService.LABEL_EXTRA,
                DataStorageContract.LabelTable.VALUE_NOTHING);
        sendBroadcast(intent);

        chewButton.setEnabled(true);
        drinkButton.setEnabled(true);
        nothingButton.setEnabled(false);

    }

    public void onChew(View view) {
        // broadcast chew to service
        Intent intent = new Intent(DataHandlerService.B_TYPE_LABEL);
        intent.putExtra(DataHandlerService.LABEL_EXTRA,
                DataStorageContract.LabelTable.VALUE_EATING);
        sendBroadcast(intent);
        chewButton.setEnabled(false);
        drinkButton.setEnabled(true);
        nothingButton.setEnabled(true);
    }

    public void onDrinking(View view) {
        // broadcast swallow to service
        Intent intent = new Intent(DataHandlerService.B_TYPE_LABEL);
        intent.putExtra(DataHandlerService.LABEL_EXTRA,
                DataStorageContract.LabelTable.VALUE_DRINKING);
        sendBroadcast(intent);
        chewButton.setEnabled(true);
        drinkButton.setEnabled(false);
        nothingButton.setEnabled(true);

    }
}
