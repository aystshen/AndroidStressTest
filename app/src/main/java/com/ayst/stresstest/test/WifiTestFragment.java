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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ayst.stresstest.R;
import com.github.ybq.android.spinkit.SpinKitView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiTestFragment extends BaseTestFragment {
    private final static int SCAN_PERIOD = 3000; //扫描wifi周期 <3s>

    private LinearLayout mSettingsContainer;
    private FrameLayout mRunningContainer;
    private SpinKitView mSpinKitView;
    private TextView mTipsTv;
    private CheckBox mCheckAPCheckbox;
    private ListView mWifiLv;

    private WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private WifiReceiver mWifiReceiver;
    private WifiListAdapter mWifiAdapter;
    private NetworkInfo.DetailedState mLastState = null;
    private boolean mIsScanPause = false;
    private boolean isCheckConnect = false;

    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;
    private Timer mTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiReceiver = new WifiReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        View contentView = inflater.inflate(R.layout.fragment_wifi_test, container, false);
        setContentView(contentView);

        setTitle(R.string.wifi_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_WIFI_TEST);

        initView(contentView);

        return view;
    }

    private void initView(View view) {
        mSettingsContainer = (LinearLayout) view.findViewById(R.id.container_settings);
        mRunningContainer = (FrameLayout) view.findViewById(R.id.container_running);
        mSpinKitView = (SpinKitView) view.findViewById(R.id.spin_kit);
        mWifiLv = (ListView) view.findViewById(R.id.lv_wifi);
        mTipsTv = (TextView) view.findViewById(R.id.tv_tips);
        mCheckAPCheckbox = (CheckBox) view.findViewById(R.id.chbox_check_connect);

        mCheckAPCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mWifiManager.isWifiEnabled() || !isWifiConnected(mActivity)) {
                        Toast.makeText(mActivity, R.string.wifi_test_connect_tips, Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                }
            }
        });
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mSettingsContainer.setVisibility(View.INVISIBLE);
            mRunningContainer.setVisibility(View.VISIBLE);
        } else {
            mSettingsContainer.setVisibility(View.VISIBLE);
            mRunningContainer.setVisibility(View.INVISIBLE);
        }
    }

    private void scan() {
        if (mWifiManager.isWifiEnabled()) {
            mSpinKitView.setVisibility(View.VISIBLE);
            mWifiManager.startScan();
        }
    }

    @Override
    public void start() {
        super.start();

        isCheckConnect = mCheckAPCheckbox.isChecked();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mActivity.registerReceiver(mWifiReceiver, filter);

        //mHandler.postDelayed(mRunnable, SCAN_PERIOD);

        if (mWifiManager.isWifiEnabled()) {
            scan();
            mWifiList = mWifiManager.getScanResults();
            mWifiInfo = mWifiManager.getConnectionInfo();
        }
        mWifiAdapter = new WifiListAdapter(mActivity, mWifiList, mWifiInfo);
        mWifiLv.setAdapter(mWifiAdapter);

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning() || (mMaxTestCount != 0 && mCurrentCount >= mMaxTestCount)) {
                    if (mWifiManager.getWifiState() == mWifiManager.WIFI_STATE_DISABLED) {
                        mWifiManager.setWifiEnabled(true);
                    }
                    stop();
                } else {
                    if (!mWifiManager.isWifiEnabled()) {
                        mWifiManager.setWifiEnabled(true);
                        Log.d(TAG, "run, WIFI is closed, try open WIFI now!");
                    } else if (mWifiManager.isWifiEnabled()) {
                        if (isCheckConnect) {
                            if (!isWifiConnected(mActivity)) {
                                incFailureCount();
                            }
                        }

                        mWifiManager.setWifiEnabled(false);
                        Log.d(TAG, "run, WIFI is opened, try close WIFI now!");
                        incCurrentCount();
                    }
                    mHandler.sendEmptyMessage(MSG_UPDATE);
                }
            }
        }, 15000, 15000);
    }

    @Override
    public void stop() {
        super.stop();

        if (mTimer != null) {
            mTimer.cancel();
        }

        mHandler.removeCallbacks(mRunnable);
        mActivity.unregisterReceiver(mWifiReceiver);
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            handleEvent(context, intent);
        }
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWifiManager.isWifiEnabled() && !mIsScanPause) {
                scan();
            }
            mHandler.postDelayed(this, SCAN_PERIOD);
        }
    };

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "handleEvent, action:" + action);
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
//                WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
//                WifiManager.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(
                    WifiManager.EXTRA_NEW_STATE);
            updateConnectionState(WifiInfo.getDetailedStateOf(state));
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            updateConnectionState(info.getDetailedState());
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            updateConnectionState(null);
        }
    }

    private void updateWifiState(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
                scan();
                hideTips();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                showTips(R.string.wifi_test_opening);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                mWifiAdapter.updateList(null);
                mWifiAdapter.notifyDataSetChanged();
                showTips(R.string.wifi_test_closing);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                showTips(R.string.wifi_test_closed);
                break;
        }
    }

    private void updateAccessPoints() {
        hideTips();
        mWifiList = mWifiManager.getScanResults();
        mWifiAdapter.updateList(mWifiList);
        mWifiAdapter.notifyDataSetChanged();

        if (!mWifiList.isEmpty()) {
            mSpinKitView.setVisibility(View.INVISIBLE);
        }
    }

    private void updateConnectionState(NetworkInfo.DetailedState state) {
        if (state != null) {
            Log.d(TAG, "updateConnectionState, state=" + state.toString());
            mLastState = state;
            mWifiInfo = mWifiManager.getConnectionInfo();
            mWifiAdapter.updateStatus(mWifiInfo, mLastState);
            mWifiAdapter.notifyDataSetChanged();

            if (state == NetworkInfo.DetailedState.CONNECTED) {
                mWifiManager.saveConfiguration();
            }

            if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR
                    || state == NetworkInfo.DetailedState.CONNECTING) {
                mIsScanPause = true;
            } else {
                mIsScanPause = false;
            }
        }
    }

    private boolean isWifiConnected(Context context) {
        if (null == mConnManager) {
            mConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo wifiNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ((wifiNetworkInfo != null) && wifiNetworkInfo.isConnected());
    }

    private void showTips(int strId) {
        mTipsTv.setText(strId);
        mTipsTv.setVisibility(View.VISIBLE);
        mSpinKitView.setVisibility(View.INVISIBLE);
    }

    private void hideTips() {
        mTipsTv.setVisibility(View.INVISIBLE);
    }
}
