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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ayst.stresstest.R;
import com.ayst.stresstest.util.AppUtils;
import com.ayst.stresstest.util.SPUtils;

import java.util.Timer;

public class RebootTestFragment extends BaseTestFragment {
    public static final String SP_REBOOT_FLAG = "reboot_flag";
    private static final String SP_REBOOT_COUNT = "reboot_count";
    private static final String SP_REBOOT_MAX = "reboot_max";
    private static final String SP_REBOOT_DELAY = "reboot_delay";
    private static final String SP_REBOOT_SD = "reboot_sd";
    private static final String SP_REBOOT_WIFI = "reboot_wifi";

    private final static int MSG_REBOOT_COUNTDOWN = 1001;

    private CheckBox mSdcardCheckbox;
    private TextView mSdStateTv;
    private CheckBox mWifiCheckbox;
    private TextView mWifiStateTv;
    private EditText mDelayEdt;
    private TextView mCountdownTv;

    private int mDelayTime;
    private int mCountDownTime;
    private boolean mIsCheckSD = false;
    private boolean mIsCheckWifi = false;
    private Timer mTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mState = SPUtils.getInstance(mActivity).getData(SP_REBOOT_FLAG, STATE_STOP);
        mCurrentCount = SPUtils.getInstance(mActivity).getData(SP_REBOOT_COUNT, 0);
        mMaxTestCount = SPUtils.getInstance(mActivity).getData(SP_REBOOT_MAX, 0);
        mDelayTime = SPUtils.getInstance(mActivity).getData(SP_REBOOT_DELAY, 5);
        mIsCheckSD = SPUtils.getInstance(mActivity).getData(SP_REBOOT_SD, false);
        mIsCheckWifi = SPUtils.getInstance(mActivity).getData(SP_REBOOT_WIFI, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.reboot_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_REBOOT_TEST);

        View contentView = inflater.inflate(R.layout.fragment_reboot_test, container, false);
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
        mSdcardCheckbox = (CheckBox) view.findViewById(R.id.chbox_sdcard);
        mWifiCheckbox = (CheckBox) view.findViewById(R.id.chbox_wifi);
        mSdStateTv = (TextView) view.findViewById(R.id.tv_sd_state);
        mWifiStateTv = (TextView) view.findViewById(R.id.tv_wifi_state);
        mDelayEdt = (EditText) view.findViewById(R.id.edt_delay);
        mCountdownTv = (TextView) view.findViewById(R.id.tv_countdown);

        mSdcardCheckbox.setChecked(mIsCheckSD);
        mWifiCheckbox.setChecked(mIsCheckWifi);
        mDelayEdt.setText(mDelayTime + "");

        mSdcardCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppUtils.isExternalStorageMounted()) {
                        Toast.makeText(mActivity, R.string.reboot_test_insert_sdcard_tips, Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                }
            }
        });

        mWifiCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppUtils.isConnNetWork(mActivity)) {
                        Toast.makeText(mActivity, R.string.reboot_test_connect_wifi_tips, Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                }
            }
        });
    }

    private void check() {
        if (isRunning()) {
            if (mIsCheckSD) {
                if (!AppUtils.isExternalStorageMounted()) {
                    mSdStateTv.setText("Check SD: Unmount");
                    mSdStateTv.setVisibility(View.VISIBLE);
                    stop();
                    return;
                }
            }

            if (mIsCheckWifi) {
                if (!AppUtils.isConnNetWork(mActivity)) {
                    mWifiStateTv.setText("Check WIFI: Disconnect");
                    mWifiStateTv.setVisibility(View.VISIBLE);
                    stop();
                    return;
                }
            }

            if (mMaxTestCount != 0 && mMaxTestCount <= mCurrentCount) {
                Log.d(TAG, "run, Bluetooth test finish!");
                stop();
            } else {
                mCountDownTime = mDelayTime; // DELAY_TIME/1000;
                mHandler.sendEmptyMessage(MSG_REBOOT_COUNTDOWN);
            }
        }
    }

    @Override
    public void start() {
        super.start();

        mDelayTime = Integer.valueOf(mDelayEdt.getText().toString());
        new AlertDialog.Builder(mActivity)
                .setMessage(String.format(getString(R.string.reboot_test_reboot_tips), mDelayTime))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                mState = STATE_RUNNING;
                                mIsCheckSD = mSdcardCheckbox.isChecked();
                                mIsCheckWifi = mWifiCheckbox.isChecked();
                                mCountDownTime = mDelayTime;
                                mHandler.sendEmptyMessage(MSG_REBOOT_COUNTDOWN);
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

        mHandler.removeMessages(MSG_REBOOT_COUNTDOWN);
        mCountdownTv.setVisibility(View.GONE);
        saveState();
    }

    @Override
    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case MSG_REBOOT_COUNTDOWN:
                if (!isRunning()) {
                    return;
                }

                if (mCountDownTime > 0) {
                    mCountdownTv.setText(mCountDownTime + "");
                    mCountdownTv.setVisibility(View.VISIBLE);
                    mCountDownTime--;
                    mHandler.sendEmptyMessageDelayed(MSG_REBOOT_COUNTDOWN, 1000);
                } else {
                    mCountdownTv.setVisibility(View.GONE);
                    reboot();
                }

                break;
        }
    }

    private void reboot() {
        // save state
        incCurrentCount();
        saveState();

        // 重启
        PowerManager pManager = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        pManager.reboot(null);
        Log.d(TAG, "reboot");
    }

    private void saveState() {
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_FLAG, mState);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_COUNT, mCurrentCount);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_MAX, mMaxTestCount);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_DELAY, mDelayTime);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_SD, mIsCheckSD);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_WIFI, mIsCheckWifi);
    }
}
