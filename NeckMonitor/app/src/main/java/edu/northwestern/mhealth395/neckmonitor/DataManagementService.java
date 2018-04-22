package edu.northwestern.mhealth395.neckmonitor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DataManagementService extends Service {
    public DataManagementService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        return START_STICKY;
    }
}
