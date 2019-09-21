/*
 * Copyright(c) 2018 Bob Shen <ayst.shen@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayst.stresstest.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ayst.stresstest.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SleepTestFragment extends BaseTestFragment {
    private static final String ACTION_TEST_CASE_SLEEP = "com.topband.stresstest.sleep.ACTION_TEST_CASE_SLEEP";

    public static String DATE_TO_STRING_PATTERN = "MM-dd HH:mm:ss";

    private EditText mWakeupEdt;
    private EditText mSleepEdt;
    private LinearLayout mTimeContainer;
    private TextView mSleepTimeTv;
    private TextView mWakeupTimeTv;

    private PowerManager mPowerManager;
    private AlarmManager mAlarmManager;
    private long mWakeTime = 5000L;
    private long mSleepTime = 10000L;
    private int mDefaultScreenOffTime = -1;

    private PowerManager mPm;
    private PowerManager.WakeLock mWakeLock;

    private long mEnterSleepTime;
    private SimpleDateFormat mSimpleDateFormat;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPm = ((PowerManager) mActivity.getSystemService(Context.POWER_SERVICE));
        mWakeLock = mPm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();

        mActivity.registerReceiver(mSleepTestReceiver, new IntentFilter(ACTION_TEST_CASE_SLEEP));

        mSimpleDateFormat = new SimpleDateFormat(DATE_TO_STRING_PATTERN);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.sleep_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_SLEEP_TEST);

        View contentView = inflater.inflate(R.layout.fragment_sleep_test, container, false);
        setContentView(contentView);

        initView(contentView);

        return view;
    }

    private void initView(View view) {
        mWakeupEdt = (EditText) view.findViewById(R.id.edt_wakeup_time);
        mSleepEdt = (EditText) view.findViewById(R.id.edt_sleep_time);
        mTimeContainer = (LinearLayout) view.findViewById(R.id.container_time);
        mSleepTimeTv = (TextView) view.findViewById(R.id.tv_sleep_time);
        mWakeupTimeTv = (TextView) view.findViewById(R.id.tv_wakeup_time);

        mWakeupEdt.setText(mWakeTime / 1000 + "");
        mSleepEdt.setText(mSleepTime / 1000 + "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mActivity.unregisterReceiver(mSleepTestReceiver);

        if(null != mWakeLock){
            mWakeLock.release();
        }
    }

    private void setAlarm(Context context, long sleepTime, boolean repeat) {
        if (null == mAlarmManager) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_TEST_CASE_SLEEP), 0);

        if (repeat) {
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, sleepTime + System.currentTimeMillis(), sleepTime, intent);
        } else {
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, sleepTime + System.currentTimeMillis(), intent);
        }
    }

    private void stopAlarm(Context context) {
        PendingIntent intent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_TEST_CASE_SLEEP), 0);

        if (null == mAlarmManager) {
            mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        }
        mAlarmManager.cancel(intent);
    }

    @Override
    public void onStartClicked() {
        String wakeupStr = mWakeupEdt.getText().toString();
        if (TextUtils.isEmpty(wakeupStr)) {
            Toast.makeText(mActivity, R.string.sleep_test_set_wakeup_duration_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        String sleepStr = mSleepEdt.getText().toString();
        if (TextUtils.isEmpty(sleepStr)) {
            Toast.makeText(mActivity, R.string.sleep_test_set_sleep_duration_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        mWakeTime = Integer.parseInt(wakeupStr)*1000;
        mSleepTime = Integer.parseInt(sleepStr)*1000;

        Log.d(TAG, "onStartClicked, mWakeTime=" + mWakeTime + " mSleepTime=" + mSleepTime);

        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();

        stopAlarm(mActivity);

        mDefaultScreenOffTime = Settings.System.getInt(mActivity.getContentResolver(), "screen_off_timeout", -1);
        Settings.System.putInt(mActivity.getContentResolver(), "screen_off_timeout", (int)mWakeTime);
        setAlarm(mActivity, mWakeTime+mSleepTime, false);

        if(null != mWakeLock){
            mWakeLock.release();
        }

        mEnterSleepTime = System.currentTimeMillis() + mWakeTime;
        mSleepTimeTv.setText(mSimpleDateFormat.format(new Date(mEnterSleepTime)));
        mWakeupTimeTv.setText("--:--:--");
        mTimeContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void stop() {
        super.stop();

        Settings.System.putInt(mActivity.getContentResolver(), "screen_off_timeout", mDefaultScreenOffTime);
        stopAlarm(mActivity);

        mWakeLock = mPm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG);
        mWakeLock.acquire();

        mTimeContainer.setVisibility(View.GONE);
    }

    private BroadcastReceiver mSleepTestReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "SleepTestReceiver onReceive...");

            mSleepTimeTv.setText(mSimpleDateFormat.format(new Date(mEnterSleepTime)));
            mWakeupTimeTv.setText(mSimpleDateFormat.format(new Date(System.currentTimeMillis())));
            mEnterSleepTime = System.currentTimeMillis() + mWakeTime;

            if (null == mPowerManager) {
                mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            }
            mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "ScreenOnTimer").acquire(mWakeTime);

            incCurrentCount();
            if (mMaxTestCount != 0 && mCurrentCount >= mMaxTestCount) {
                stop();
            } else {
                setAlarm(mActivity, mWakeTime+mSleepTime, false);
            }
        }

    };
}
