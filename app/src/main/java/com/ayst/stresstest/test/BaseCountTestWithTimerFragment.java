package com.ayst.stresstest.test;

import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseCountTestWithTimerFragment extends BaseCountTestFragment {

    // Default time period
    private static final long DEFAULT_PERIOD = 10*1000; // Default 10s

    // Timer period
    private long mPeriod = DEFAULT_PERIOD;

    // Timer
    private Timer mTimer;

    @Override
    public void start() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (next()) {
                    if (!testOnce()) {
                        incFailureCount();
                    }
                }
            }
        }, 1000, mPeriod);

        super.start();
    }

    @Override
    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        super.stop();
    }

    protected abstract boolean testOnce();

    protected void setPeriod(long period) {
        if (period > 1000) {
            mPeriod = period;
        } else {
            Log.w(TAG, "setPeriod, period is too short.");
        }
    }
}
