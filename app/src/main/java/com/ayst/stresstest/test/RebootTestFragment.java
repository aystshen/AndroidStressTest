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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.AppUtils;
import com.ayst.stresstest.util.SPUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RebootTestFragment extends BaseCountTestFragment {

    public static final String SP_REBOOT_FLAG = "reboot_flag";
    private static final String SP_REBOOT_COUNT = "reboot_count";
    private static final String SP_REBOOT_MAX = "reboot_max";
    private static final String SP_REBOOT_DELAY = "reboot_delay";
    private static final String SP_REBOOT_SD = "reboot_sd";
    private static final String SP_REBOOT_WIFI = "reboot_wifi";

    private final static int MSG_REBOOT_COUNTDOWN = 1001;
    private final static int DELAY_DEFAULT = 10; // Default 10s

    @BindView(R.id.chbox_check_wifi)
    CheckBox mWiFiCheckbox;
    @BindView(R.id.tv_wifi_state)
    TextView mWifiStateTv;
    @BindView(R.id.chbox_sdcard)
    CheckBox mSdcardCheckbox;
    @BindView(R.id.tv_sd_state)
    TextView mSdStateTv;
    @BindView(R.id.edt_delay)
    EditText mDelayEdt;
    @BindView(R.id.tv_countdown)
    TextView mCountdownTv;
    Unbinder unbinder;

    private int mDelayTime;
    private int mCountDownTime;
    private boolean isCheckSD = false;
    private boolean isCheckWifi = false;

    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mState = SPUtils.getInstance(mActivity).getData(SP_REBOOT_FLAG, State.STOP);
        mCurrentCount = SPUtils.getInstance(mActivity).getData(SP_REBOOT_COUNT, 0);
        mTargetCount = SPUtils.getInstance(mActivity).getData(SP_REBOOT_MAX, 0);
        mDelayTime = SPUtils.getInstance(mActivity).getData(SP_REBOOT_DELAY, DELAY_DEFAULT);
        isCheckSD = SPUtils.getInstance(mActivity).getData(SP_REBOOT_SD, false);
        isCheckWifi = SPUtils.getInstance(mActivity).getData(SP_REBOOT_WIFI, false);

        mWifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.reboot_test);
        setType(TestType.TYPE_REBOOT_TEST);

        View contentView = inflater.inflate(R.layout.fragment_reboot_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mWiFiCheckbox.setChecked(isCheckWifi);
        mSdcardCheckbox.setChecked(isCheckSD);
        mDelayEdt.setText(mDelayTime + "");

        mSdcardCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!AppUtils.isExternalStorageMounted()) {
                        showToast(R.string.reboot_test_insert_sdcard_tips);
                        buttonView.setChecked(false);
                    }
                }
            }
        });

        mWiFiCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton view, boolean isChecked) {
                if (isChecked) {
                    if (!mWifiManager.isWifiEnabled() || !isWifiConnected(mActivity)) {
                        showToast(R.string.wifi_test_connect_tips);
                        view.setChecked(false);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onStart() {
        super.onStart();

        check();
    }

    private void check() {
        if (isRunning()) {
            if (isCheckSD) {
                if (!AppUtils.isExternalStorageMounted()) {
                    mSdStateTv.setText("FAIL");
                    mSdStateTv.setVisibility(View.VISIBLE);
                    markFailure();
                }
            } else if (isCheckWifi) {
                if (!isWifiConnected(mActivity)) {
                    mWifiStateTv.setText("FAIL");
                    mWifiStateTv.setVisibility(View.VISIBLE);
                    markFailure();
                }
            }

            if (next()) {
                mCountDownTime = mDelayTime;
                mHandler.sendEmptyMessage(MSG_REBOOT_COUNTDOWN);
            }
        }
    }

    @Override
    protected void onStartClicked() {
        String delayStr = mDelayEdt.getText().toString();
        if (TextUtils.isEmpty(delayStr)) {
            showToast(R.string.reboot_test_delay_empty);
            return;
        }
        mDelayTime = Integer.valueOf(mDelayEdt.getText().toString());

        super.onStartClicked();
    }

    @Override
    public void start() {
        new AlertDialog.Builder(mActivity)
                .setMessage(String.format(getString(R.string.reboot_test_reboot_tips), mDelayTime))
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                isCheckWifi = mWiFiCheckbox.isChecked();
                                isCheckSD = mSdcardCheckbox.isChecked();
                                mCountDownTime = mDelayTime;
                                mHandler.sendEmptyMessage(MSG_REBOOT_COUNTDOWN);
                            }
                        })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                stop();
                                dialog.cancel();
                            }
                        }).show();

        super.start();
    }

    @Override
    public void stop() {
        mHandler.removeMessages(MSG_REBOOT_COUNTDOWN);
        mCountdownTv.setVisibility(View.GONE);
        cleanState();

        super.stop();
    }

    @Override
    public boolean isSupport() {
        return true;
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
        saveState();

        PowerManager pManager = (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        pManager.reboot(null);
        Log.d(TAG, "reboot");
    }

    private void saveState() {
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_FLAG, mState);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_COUNT, mCurrentCount);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_MAX, mTargetCount);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_DELAY, mDelayTime);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_SD, isCheckSD);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_WIFI, isCheckWifi);
    }

    private void cleanState() {
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_FLAG, State.STOP);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_COUNT, 0);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_MAX, 0);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_DELAY, DELAY_DEFAULT);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_SD, false);
        SPUtils.getInstance(mActivity).saveData(SP_REBOOT_WIFI, false);
    }

    public boolean isWifiConnected(Context context) {
        if (null == mConnManager) {
            mConnManager = (ConnectivityManager) context.
                    getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo wifiNetworkInfo = mConnManager.
                getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ((wifiNetworkInfo != null) && wifiNetworkInfo.isConnected());
    }
}
