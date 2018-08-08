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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiTestFragment extends BaseTestFragment {
    private final static int SCAN_PERIOD = 3000; //扫描wifi周期 <3s>

    private LinearLayout mSettingsContainer;
    private FrameLayout mRunningContainer;
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
    private int mConnectCount = 0;

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
        mWifiLv = (ListView) view.findViewById(R.id.lv_wifi);
        mTipsTv = (TextView) view.findViewById(R.id.tv_tips);
        mCheckAPCheckbox = (CheckBox) view.findViewById(R.id.chbox_check_ap);

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
            mWifiManager.startScan();
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
                    Log.d(TAG, "run, WIFI test finish!");
                    mResult = RESULT_SUCCESS;
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
                                if (++mConnectCount > 2) {
                                    mResult = RESULT_FAIL;
                                    stop();
                                    return;
                                }
                            } else {
                                mConnectCount = 0;
                            }
                        }

                        mWifiManager.setWifiEnabled(false);
                        Log.d(TAG, "run, WIFI is opened, try close WIFI now!");
                        IncCurrentCount();
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
                mWifiManager.startScan();
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
                mWifiManager.startScan();
                mTipsTv.setVisibility(View.INVISIBLE);
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                mTipsTv.setText(R.string.wifi_test_opening);
                mTipsTv.setVisibility(View.VISIBLE);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                mWifiAdapter.updateList(null);
                mWifiAdapter.notifyDataSetChanged();
                mTipsTv.setText(R.string.wifi_test_closing);
                mTipsTv.setVisibility(View.VISIBLE);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                mTipsTv.setText(R.string.wifi_test_closed);
                mTipsTv.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateAccessPoints() {
        mTipsTv.setVisibility(View.INVISIBLE);
        mWifiList = mWifiManager.getScanResults();
        mWifiAdapter.updateList(mWifiList);
        mWifiAdapter.notifyDataSetChanged();
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

    public boolean isWifiConnected(Context context) {
        if (null == mConnManager) {
            mConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
        NetworkInfo wifiNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return ((wifiNetworkInfo != null) && wifiNetworkInfo.isConnected());
    }
}
