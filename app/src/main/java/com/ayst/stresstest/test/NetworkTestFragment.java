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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.ayst.stresstest.R;
import com.ayst.stresstest.util.NetworkUtils;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class NetworkTestFragment extends BaseTestFragment {

    private static final int RETRY_INTERVAL = 3000; // 1s

    @BindView(R.id.edt_url)
    EditText mUrlEdt;
    @BindView(R.id.edt_retry_count)
    EditText mRetryCountEdt;
    Unbinder unbinder;

    private String mTestUrl = "www.baidu.com";
    private int mRetryCount = 3;

    private Timer mTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.network_test);
        setCountType(COUNT_TYPE_COUNT);
        setType(TestType.TYPE_NETWORK_TEST);

        View contentView = inflater.inflate(R.layout.fragment_network_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        initView();

        return view;
    }

    private void initView() {
        mUrlEdt.setText(mTestUrl);
        mRetryCountEdt.setText(mRetryCount + "");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();
    }

    @Override
    public void onStartClicked() {
        mTestUrl = mUrlEdt.getText().toString();
        if (TextUtils.isEmpty(mTestUrl)) {
            Toast.makeText(mActivity, R.string.network_test_url_empty_tips, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning() || (mMaxTestCount != 0 && mCurrentCount >= mMaxTestCount)) {
                    stop();
                } else {
                    int retry = 0;
                    int lostCnt = 0;
                    while (retry++ < mRetryCount) {
                        boolean available = NetworkUtils.isAvailableByDns(mTestUrl);
                        Log.i(TAG, "run, Check the network is "
                                + (available ? "working" : "no working")
                                + ", retry " + retry);
                        if (available) {
                            lostCnt = 0;
                        } else {
                            lostCnt++;
                        }
                        try {
                            Thread.sleep(RETRY_INTERVAL);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    if (lostCnt >= mRetryCount) {
                        incFailureCount();
                    }

                    incCurrentCount();
                    mHandler.sendEmptyMessage(MSG_UPDATE);
                }
            }
        }, 10000, 10000);
    }

    @Override
    public void stop() {
        super.stop();

        if (mTimer != null) {
            mTimer.cancel();
        }
    }
}
