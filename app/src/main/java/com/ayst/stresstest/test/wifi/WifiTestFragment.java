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

package com.ayst.stresstest.test.wifi;

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

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestWithTimerFragment;
import com.ayst.stresstest.test.base.TestType;
import com.github.ybq.android.spinkit.SpinKitView;

import java.util.List;

import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class WifiTestFragment extends BaseCountTestWithTimerFragment {

    @BindView(R.id.chbox_check_connect)
    CheckBox mCheckConnectCheckbox;
    @BindView(R.id.container_settings)
    LinearLayout mSettingsContainer;
    @BindView(R.id.lv_wifi)
    ListView mWifiLv;
    @BindView(R.id.spin_kit)
    SpinKitView mSpinKitView;
    @BindView(R.id.tv_tips)
    TextView mTipsTv;
    @BindView(R.id.container_running)
    FrameLayout mRunningContainer;
    Unbinder unbinder;

    private static final int STATE_ENABLED = 1;
    private static final int STATE_DISABLED = 2;
    private int mCurState = STATE_DISABLED;
    private int mPreState = STATE_DISABLED;

    private WifiInfo mWifiInfo;
    private List<ScanResult> mWifiList;
    private WifiReceiver mWifiReceiver;
    private WifiListAdapter mWifiAdapter;
    private NetworkInfo.DetailedState mLastState = null;

    private boolean isCheckConnect = false;

    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;

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
        setType(TestType.TYPE_WIFI_TEST);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCheckConnectCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!mWifiManager.isWifiEnabled() || !isWifiConnected(mActivity)) {
                        showToast(R.string.wifi_test_connect_tips);
                        buttonView.setChecked(false);
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
        isCheckConnect = mCheckConnectCheckbox.isChecked();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mActivity.registerReceiver(mWifiReceiver, filter);

        if (mWifiManager.isWifiEnabled()) {
            scan();
            mWifiList = mWifiManager.getScanResults();
            mWifiInfo = mWifiManager.getConnectionInfo();
            mPreState = STATE_DISABLED;
        } else {
            mPreState = STATE_ENABLED;
        }
        mWifiAdapter = new WifiListAdapter(mActivity, mWifiList, mWifiInfo);
        mWifiLv.setAdapter(mWifiAdapter);

        super.start();
    }

    @Override
    public void stop() {
        if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
            mWifiManager.setWifiEnabled(true);
        }

        mActivity.unregisterReceiver(mWifiReceiver);

        super.stop();
    }

    @Override
    public boolean isSupport() {
        return (null != mWifiManager);
    }

    @Override
    protected boolean testOnce() {
        if (mWifiManager.isWifiEnabled()) {
            mCurState = STATE_ENABLED;
            if (isCheckConnect) {
                if (!isWifiConnected(mActivity)) {
                    markFailure();
                }
            }

            mWifiManager.setWifiEnabled(false);
            Log.d(TAG, "run, WIFI is opened, try close WIFI now.");

        } else {
            mCurState = STATE_DISABLED;
            mWifiManager.setWifiEnabled(true);
            Log.d(TAG, "run, WIFI is closed, try open WIFI now.");
        }

        if (mCurState == mPreState) {
            markFailure();
        }
        mPreState = mCurState;

        return true;
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            handleEvent(context, intent);
        }
    }

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
