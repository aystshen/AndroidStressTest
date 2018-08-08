package com.ayst.stresstest.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LogService extends Service {
    private static final String TAG = "LogService";

    private boolean mCanceled = false;

    public LogService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    class LogThread extends Thread {

        public LogThread() {

        }

        @Override
        public void run() {
            super.run();

            try {
                Process process = Runtime.getRuntime().exec("logcat -d -v time");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {

                }
            } catch (IOException e) {
                Log.e(TAG, "LogThread.run, " + e.getMessage());
                return;
            }

            while (!mCanceled) {


            }
        }
    }
}
