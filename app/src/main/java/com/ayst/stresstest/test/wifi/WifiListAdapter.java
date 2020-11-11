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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ayst.stresstest.R;

import java.util.List;

/**
 * Created by ayst on 2014/11/27.
 */
public class WifiListAdapter extends BaseAdapter {
    private final static String TAG = "WifiListAdapter";
    private LayoutInflater inflater;
    private List<ScanResult> list;
    private WifiInfo mInfo = null;
    private NetworkInfo.DetailedState mState = null;
    private AnimationDrawable mWaitAnim;
    private final int mWaitImg[] = {R.drawable.ic_wait_0, R.drawable.ic_wait_1, R.drawable.ic_wait_2, R.drawable.ic_wait_3,
            R.drawable.ic_wait_4, R.drawable.ic_wait_5, R.drawable.ic_wait_6, R.drawable.ic_wait_7, R.drawable.ic_wait_8};

    public WifiListAdapter(Context context, List<ScanResult> list, WifiInfo info) {
        // TODO Auto-generated constructor stub
        super();
        this.inflater = LayoutInflater.from(context);
        this.list = list;
        this.mInfo = info;
        mWaitAnim = new AnimationDrawable();
        for (int i = 0; i < mWaitImg.length; i++) {
            mWaitAnim.addFrame(context.getResources().getDrawable(mWaitImg[i]), 200);
        }
        mWaitAnim.setOneShot(false);
    }

    public void updateList(List<ScanResult> list) {
        this.list = list;
    }

    public void updateStatus(WifiInfo info, NetworkInfo.DetailedState state) {
        mInfo = info;
        mState = state;
    }

    @Override
    public int getCount() {
        if (list != null) {
            return list.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        if (list != null) {
            view = inflater.inflate(R.layout.wifi_list_item, null);
            ScanResult scanResult = list.get(position);
            TextView text = (TextView) view.findViewById(R.id.text);
            text.setText(scanResult.SSID);

            //加密性
            ImageView imgPwd = (ImageView) view.findViewById(R.id.img_pwd);
            if (scanResult.capabilities.contains("WEP")
                    || scanResult.capabilities.contains("PSK")
                    || scanResult.capabilities.contains("EAP")) {
                imgPwd.setImageResource(R.drawable.ic_wifi_pwd);
            }

            //性号强度
            ImageView imgSignal = (ImageView) view.findViewById(R.id.img_signal);
            int level = getLevel(scanResult.level);
            if (level == 3) {
                imgSignal.setImageResource(R.drawable.ic_wifi_3);
            } else if (level == 2) {
                imgSignal.setImageResource(R.drawable.ic_wifi_2);
            } else if (level == 1) {
                imgSignal.setImageResource(R.drawable.ic_wifi_1);
            } else {
                imgSignal.setImageResource(R.drawable.ic_wifi_0);
            }

            //连接状态
            if (mInfo != null && mInfo.getSSID() != null && mState != null) {
                ImageView imgConnected = (ImageView) view.findViewById(R.id.img_connected);
                TextView subText = (TextView) view.findViewById(R.id.sub_text);
                if (TextUtils.equals(mInfo.getBSSID(), scanResult.BSSID)) {
                    subText.setVisibility(View.VISIBLE);
                    if (mState == NetworkInfo.DetailedState.CONNECTED) {
                        imgConnected.setImageResource(R.drawable.ic_wifi_connected);
                        subText.setText(R.string.wifi_test_connected);
                    } else if (mState == NetworkInfo.DetailedState.CONNECTING) {
                        imgConnected.setImageDrawable(mWaitAnim);
                        mWaitAnim.start();
                        subText.setText(R.string.wifi_test_connecting);
                    } else if (mState == NetworkInfo.DetailedState.AUTHENTICATING) {
                        imgConnected.setImageDrawable(mWaitAnim);
                        mWaitAnim.start();
                        subText.setText(R.string.wifi_test_authenticating);
                    } else if (mState == NetworkInfo.DetailedState.DISCONNECTED) {
                        subText.setText(R.string.wifi_test_disconnected);
                    } else if (mState == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                        imgConnected.setImageDrawable(mWaitAnim);
                        mWaitAnim.start();
                        subText.setText(R.string.wifi_test_obtaining_ipaddr);
                    } else {
//                        subText.setText(R.string.wifi_test_disconnected);
                    }
                } else {
                    subText.setVisibility(View.GONE);
                }
            }
        }

        return view;
    }

    private static int getLevel(int level) {
        if (level == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(level, 4);
    }
}
