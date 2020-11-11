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

package com.ayst.stresstest.test.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseCountTestWithTimerFragment;
import com.ayst.stresstest.test.base.TestType;
import com.github.ybq.android.spinkit.SpinKitView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class BluetoothTestFragment extends BaseCountTestWithTimerFragment {

    @BindView(R.id.chbox_check_devices)
    CheckBox mCheckDevicesCheckbox;
    @BindView(R.id.chbox_ble)
    CheckBox mBleCheckbox;
    @BindView(R.id.container_settings)
    LinearLayout mSettingsContainer;
    @BindView(R.id.lv_device)
    ListView mDeviceLv;
    @BindView(R.id.spin_kit)
    SpinKitView mSpinKitView;
    @BindView(R.id.tv_tips)
    TextView mTipsTv;
    @BindView(R.id.container_running)
    FrameLayout mRunningContainer;
    Unbinder unbinder;

    private boolean isCheckConnect = false;
    private boolean isScanning = true;

    private DeviceListAdapter mDeviceListAdapter = null;
    private ArrayList<ScanResult> mData = new ArrayList<ScanResult>();
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        View contentView = inflater.inflate(R.layout.fragment_bt_test, container, false);
        setContentView(contentView);

        setTitle(R.string.bluetooth_test);
        setType(TestType.TYPE_BT_TEST);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mCheckDevicesCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    showToast(R.string.bt_test_valid_device_tips);
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
    public void start() {
        isCheckConnect = mCheckDevicesCheckbox.isChecked();

        mActivity.registerReceiver(mBTStateChangeReceiver, makeFilter());

        mDeviceListAdapter = new DeviceListAdapter();
        mDeviceLv.setAdapter(mDeviceListAdapter);

        if (mBluetoothAdapter.isEnabled()) {
            scan(true);
        }

        super.start();
    }

    @Override
    public void stop() {
        scan(false);
        mActivity.unregisterReceiver(mBTStateChangeReceiver);

        super.stop();
    }

    @Override
    public boolean isSupport() {
        return (null != mBluetoothAdapter);
    }

    @Override
    protected boolean testOnce() {
        if (!mBluetoothAdapter.isEnabled()) {
            return mBluetoothAdapter.enable();
        } else {
            if (isCheckConnect) {
                if (mData.isEmpty()) {
                    markFailure();
                }
            }
            return mBluetoothAdapter.disable();
        }
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mSettingsContainer.setVisibility(View.INVISIBLE);
            mRunningContainer.setVisibility(View.VISIBLE);

            if (isScanning) {
                mSpinKitView.setVisibility(View.VISIBLE);
            } else {
                mSpinKitView.setVisibility(View.INVISIBLE);
            }
        } else {
            mSettingsContainer.setVisibility(View.VISIBLE);
            mRunningContainer.setVisibility(View.INVISIBLE);
        }
    }

    private void scan(final boolean enable) {
        if (mBluetoothAdapter.isEnabled()) {
            if (mBleCheckbox.isChecked()) {
                scanLeDevice(enable);
            } else {
                scanClassicalDevice(enable);
            }
        }
        isScanning = enable;
        update();
    }

    private void scanClassicalDevice(final boolean enable) {
        if (enable) {
            if (!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
            }
        } else {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    private void scanLeDevice(final boolean enable) {
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            scanner.startScan(mLeScanCallback);
        } else {
            scanner.stopScan(mLeScanCallback);
        }
    }

    private void showTips(int strId) {
        mTipsTv.setText(strId);
        mTipsTv.setVisibility(View.VISIBLE);
        mSpinKitView.setVisibility(View.INVISIBLE);
    }

    private void hideTips() {
        mTipsTv.setVisibility(View.INVISIBLE);
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final android.bluetooth.le.ScanResult result) {
            super.onScanResult(callbackType, result);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BluetoothDevice device = result.getDevice();
                    String name = device.getName();
                    Log.i(TAG, "onLeScan, found: " + name);

                    for (int i = 0; i < mData.size(); i++) {
                        ScanResult tmp = mData.get(i);
                        if (tmp.getDevice().getAddress().equals(device.getAddress())) {
                            mData.set(i, new ScanResult(tmp.getDevice(), result.getRssi()));
                            mDeviceListAdapter.notifyDataSetChanged();
                            return;
                        }
                    }
                    mData.add(new ScanResult(device, result.getRssi()));
                    mDeviceListAdapter.notifyDataSetChanged();

                    if (!mData.isEmpty()) {
                        mSpinKitView.setVisibility(View.INVISIBLE);
                    }
                }
            });
        }
    };

    class ScanResult {
        private BluetoothDevice mDevice = null;
        private int mRssi = 0;

        ScanResult(BluetoothDevice device, int rssi) {
            mDevice = device;
            mRssi = rssi;
        }

        BluetoothDevice getDevice() {
            return mDevice;
        }

        int getRssi() {
            return mRssi;
        }
    }

    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        return filter;
    }

    private BroadcastReceiver mBTStateChangeReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive, ACTION:" + action);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (btState) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        showTips(R.string.bt_test_bt_opening);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        scan(true);
                        hideTips();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        scan(false);
                        mData.clear();
                        mDeviceListAdapter.notifyDataSetChanged();
                        showTips(R.string.bt_test_bt_closing);
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        showTips(R.string.bt_test_bt_closed);
                        break;
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                String name = device.getName();
                Log.i(TAG, "scan, found: " + name);

                if (TextUtils.isEmpty(device.getName())) {
                    return;
                }

                for (int i = 0; i < mData.size(); i++) {
                    ScanResult tmp = mData.get(i);
                    if (tmp.getDevice().getAddress().equals(device.getAddress())) {
                        mData.set(i, new ScanResult(tmp.getDevice(), rssi));
                        mDeviceListAdapter.notifyDataSetChanged();
                        return;
                    }
                }
                mData.add(new ScanResult(device, rssi));
                mDeviceListAdapter.notifyDataSetChanged();

                if (!mData.isEmpty()) {
                    mSpinKitView.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    private class DeviceListAdapter extends BaseAdapter {
        private LayoutInflater mInflater = null;

        DeviceListAdapter() {
            mInflater = LayoutInflater.from(mActivity);
        }

        @Override
        public int getCount() {
            if (mData != null) {
                return mData.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int i) {
            if (mData != null) {
                return mData.get(i);
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder holder;
            if (view == null) {
                view = mInflater.inflate(R.layout.device_item, null);
                holder = new ViewHolder();
                holder.mMainTv = (TextView) view.findViewById(R.id.tv_main);
                holder.mSubOneTv = (TextView) view.findViewById(R.id.tv_sub_one);
                holder.mSubTwoTv = (TextView) view.findViewById(R.id.tv_sub_two);
                holder.mIconIv = (ImageView) view.findViewById(R.id.iv_icon);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.mMainTv.setText(mData.get(i).getDevice().getName());
            holder.mSubOneTv.setText(mData.get(i).getDevice().getAddress());
            holder.mSubTwoTv.setText("rssi: " + mData.get(i).getRssi());
            holder.mIconIv.setImageResource(R.drawable.ic_device);
            return view;
        }

        final class ViewHolder {
            private TextView mMainTv = null;
            private TextView mSubOneTv = null;
            private TextView mSubTwoTv = null;
            private ImageView mIconIv = null;

        }
    }
}
