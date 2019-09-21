/*
 * Copyright(c) 2019 Bob Shen <ayst.shen@foxmail.com>
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IMcuService;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ayst.stresstest.R;
import com.ayst.stresstest.util.SPUtils;

import java.lang.reflect.Method;

public class TimingBootTestFragment extends BaseTestFragment {
    public static final String SP_TIMING_BOOT_FLAG = "timing_boot_flag";
    private static final String SP_TIMING_BOOT_COUNT = "timing_boot_count";
    private static final String SP_TIMING_BOOT_MAX = "timing_boot_max";
    private static final String SP_TIMING_BOOT_SHUTDOWN_DELAY = "timing_boot_shutdown_delay";
    private static final String SP_TIMING_BOOT_STARTUP_DELAY = "timing_boot_startup_delay";
    private static final String SP_TIMING_BOOT_STARTUP_TIME = "timing_boot_startup_time";

    private final static int MSG_TIMING_BOOT_COUNTDOWN = 1001;

    private final static int TIME_DEVIATION = 300; // unit: seconds

    private EditText mShutdownDelayEdt;
    private EditText mStartupDelayEdt;
    private TextView mCountdownTv;

    private int mShutdownDelayTime;
    private int mStartupDelayTime;
    private long mStartupTime;
    private int mCountDownTime;

    private IMcuService mMcuService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Method method = null;
        try {
            method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            IBinder binder = (IBinder) method.invoke(null, new Object[]{"mcu"});
            mMcuService = IMcuService.Stub.asInterface(binder);
        } catch (Exception e) {
            e.printStackTrace();
            setEnable(false);
        }

        mState = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_FLAG, STATE_STOP);
        mCurrentCount = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_COUNT, 0);
        mMaxTestCount = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_MAX, 0);
        mShutdownDelayTime = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_SHUTDOWN_DELAY, 60);
        mStartupDelayTime = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_STARTUP_DELAY, 60);
        mStartupTime = SPUtils.getInstance(mActivity).getData(SP_TIMING_BOOT_STARTUP_TIME, (long)0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.timing_boot_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_TIMING_BOOT_TEST);

        View contentView = inflater.inflate(R.layout.fragment_timing_boot_test, container, false);
        setContentView(contentView);

        initView(contentView);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        check();
    }

    private void initView(View view){
        mShutdownDelayEdt = (EditText) view.findViewById(R.id.edt_shutdown_delay);
        mStartupDelayEdt = (EditText) view.findViewById(R.id.edt_startup_delay);
        mCountdownTv = (TextView) view.findViewById(R.id.tv_countdown);

        mShutdownDelayEdt.setText(mShutdownDelayTime + "");
        mStartupDelayEdt.setText(mStartupDelayTime + "");
    }

    private void check() {
        if (isRunning()) {
            long diffTime = (System.currentTimeMillis() - mStartupTime)/1000;
            Log.d(TAG, "check, diffTime=" + diffTime);
            if (Math.abs(diffTime) > TIME_DEVIATION) {
                Log.d(TAG, "check, Timing boot test failed!");
                incFailureCount();
            }

            if (mMaxTestCount != 0 && mMaxTestCount <= mCurrentCount) {
                stop();
            } else {
                mCountDownTime = mShutdownDelayTime; // DELAY_TIME/1000;
                mHandler.sendEmptyMessage(MSG_TIMING_BOOT_COUNTDOWN);
            }
        }
    }

    @Override
    public void onStartClicked() {
        String shutdownStr = mShutdownDelayEdt.getText().toString();
        if (TextUtils.isEmpty(shutdownStr)) {
            Toast.makeText(mActivity, R.string.timing_boot_test_set_shutdown_delay_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        String startupStr = mStartupDelayEdt.getText().toString();
        if (TextUtils.isEmpty(startupStr)) {
            Toast.makeText(mActivity, R.string.timing_boot_test_set_startup_delay_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        mShutdownDelayTime = Integer.parseInt(shutdownStr);
        mStartupDelayTime = Integer.parseInt(startupStr);

        Log.d(TAG, "onStartClicked, mShutdownDelayTime=" + mShutdownDelayTime + " mStartupDelayTime=" + mStartupDelayTime);

        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();

        new AlertDialog.Builder(mActivity)
                .setMessage(String.format(getString(R.string.timing_boot_test_shutdown_tips), mShutdownDelayTime))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mState = STATE_RUNNING;
                                mCountDownTime = mShutdownDelayTime;
                                mHandler.sendEmptyMessage(MSG_TIMING_BOOT_COUNTDOWN);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mState = STATE_STOP;
                                dialog.cancel();
                            }
                        }).show();
    }

    @Override
    public void stop() {
        super.stop();

        mHandler.removeMessages(MSG_TIMING_BOOT_COUNTDOWN);
        mCountdownTv.setVisibility(View.GONE);
        saveState();
    }

    @Override
    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case MSG_TIMING_BOOT_COUNTDOWN:
                if (!isRunning()) {
                    return;
                }

                if (mCountDownTime > 0) {
                    mCountdownTv.setText(mCountDownTime + "");
                    mCountdownTv.setVisibility(View.VISIBLE);
                    mCountDownTime--;
                    mHandler.sendEmptyMessageDelayed(MSG_TIMING_BOOT_COUNTDOWN, 1000);
                } else {
                    mCountdownTv.setVisibility(View.GONE);
                    powerOff();
                }

                break;
        }
    }

    private void powerOff() {
        // save state
        incCurrentCount();
        saveState();
        setUptime(mStartupDelayTime);

        // 关机
        Intent intent = new Intent("com.android.internal.intent.action.REQUEST_SHUTDOWN");
        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(intent);
        Log.d(TAG, "shutdown");
    }

    public int setUptime(int time) {
        if (null != mMcuService) {
            try {
                return mMcuService.setUptime(time);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private void saveState() {
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_FLAG, mState);
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_COUNT, mCurrentCount);
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_MAX, mMaxTestCount);
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_SHUTDOWN_DELAY, mShutdownDelayTime);
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_STARTUP_DELAY, mStartupDelayTime);
        SPUtils.getInstance(mActivity).saveData(SP_TIMING_BOOT_STARTUP_TIME, System.currentTimeMillis() + mStartupDelayTime*1000);
    }
}
