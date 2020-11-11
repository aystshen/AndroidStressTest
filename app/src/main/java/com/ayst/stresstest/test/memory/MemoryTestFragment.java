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

package com.ayst.stresstest.test.memory;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseTimingTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.MemOpUtils;
import com.ayst.stresstest.view.DincondFontTextView;
import com.github.lzyzsd.circleprogress.ArcProgress;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MemoryTestFragment extends BaseTimingTestFragment {

    @BindView(R.id.arc_progress)
    ArcProgress mArcProgress;
    @BindView(R.id.tv_free_memory)
    DincondFontTextView mFreeMemoryTv;
    @BindView(R.id.tv_total_memory)
    TextView mTotalMemoryTv;
    @BindView(R.id.spinner_percent)
    Spinner mFillPercentSpinner;
    Unbinder unbinder;

    private static int[] sMemoryFillPercentList = null;

    private long mFreeMemory;
    private long mUsedMemory;
    private long mTotalMemory;
    private int mFillPercent;
    private int mCountDown = 30;

    private Timer mMallocMemoryTimer;
    private ActivityManager mAm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAm = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        sMemoryFillPercentList = this.getResources().getIntArray(R.array.memory_fill_percent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.memory_test);
        setType(TestType.TYPE_MEMORY_TEST);

        View contentView = inflater.inflate(R.layout.fragment_memory_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFillPercentSpinner.setSelection(2); // Default 80%
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onStart() {
        super.onStart();

        mCountDown = 30;
        mMallocMemoryTimer = new Timer();
        mMallocMemoryTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isRunning()) {
                    if ((mUsedMemory * 100) / mTotalMemory < mFillPercent) {
                        MemOpUtils.malloc(10); // Malloc 10M memory
                    } else {
                        if (mCountDown > 0) {
                            mCountDown--;
                        } else {
                            if (mMallocMemoryTimer != null) {
                                mMallocMemoryTimer.cancel();
                                mMallocMemoryTimer = null;
                            }
                        }
                    }
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mMallocMemoryTimer != null) {
            mMallocMemoryTimer.cancel();
            mMallocMemoryTimer = null;
        }
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        MemoryInfo systemMemInfo = new MemoryInfo();
        mAm.getMemoryInfo(systemMemInfo);
        mFreeMemory = systemMemInfo.availMem / 1024 / 1024;
        mTotalMemory = systemMemInfo.totalMem / 1024 / 1024;

        mUsedMemory = mTotalMemory - mFreeMemory;
        int progress = (int)((mUsedMemory * 100) / mTotalMemory);
        mArcProgress.setProgress(progress);
        if (progress >= 70) {
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.red));
        } else if (progress >= 50) {
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.orange));
        } else {
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
        }

        mFreeMemoryTv.setText(mFreeMemory + "");
        mTotalMemoryTv.setText(mTotalMemory + "");
    }

    @Override
    public void start() {
        mFillPercent = sMemoryFillPercentList[mFillPercentSpinner.getSelectedItemPosition()];

        super.start();
    }

    @Override
    public void stop() {
        MemOpUtils.free();

        super.stop();
    }

    @Override
    public boolean isSupport() {
        return true;
    }
}
