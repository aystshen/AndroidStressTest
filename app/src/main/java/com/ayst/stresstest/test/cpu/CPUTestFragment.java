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

package com.ayst.stresstest.test.cpu;

import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseTimingTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.ArmFreqUtils;
import com.ayst.stresstest.view.DincondFontTextView;
import com.github.lzyzsd.circleprogress.ArcProgress;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class CPUTestFragment extends BaseTimingTestFragment {

    private static final int MSG_RANDOM_SET_FREQ = 1001;

    @BindView(R.id.chbox_fix_freq)
    CheckBox mFixFreqCheckbox;
    @BindView(R.id.spinner_freq)
    Spinner mFreqSpinner;
    @BindView(R.id.chbox_random_freq)
    CheckBox mRandomFreqCheckbox;
    @BindView(R.id.spinner_interval)
    Spinner mIntervalSpinner;
    @BindView(R.id.chbox_cpu_rate)
    CheckBox mCpuRateCheckbox;
    @BindView(R.id.spinner_cpu_rate)
    Spinner mCpuRateSpinner;
    @BindView(R.id.container_settings)
    LinearLayout mSettingsContainer;
    @BindView(R.id.pgr_percent)
    ArcProgress mCurPercentPgr;
    @BindView(R.id.tv_percent)
    DincondFontTextView mCurPercentTv;
    @BindView(R.id.pgr_freq)
    ArcProgress mCurFreqPgr;
    @BindView(R.id.tv_freq)
    DincondFontTextView mCurFreqTv;
    @BindView(R.id.tv_freq_max)
    TextView mMaxFreqTv;
    @BindView(R.id.container_running)
    LinearLayout mRunningContainer;
    Unbinder unbinder;

    private static int[] sIntervalTimeList = null;
    private static int[] sCpuRateList = null;

    private static final int DELAY_CNT_BASE = 10000;
    private static final int DELAY_CNT_STEP = 500;
    private int mDelayCnt = DELAY_CNT_BASE;
    private int mCpuRate = 100;

    private int mCurFreq;
    private int mMaxFreq;
    private int mCurPercent;
    private int mCurMyPidPercent;

    protected ArrayAdapter<String> mAdapter;
    private List<String> mFreqs = null;

    private Timer mRandomFreqTimer;
    private ReaderThread mCpuReaderThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sIntervalTimeList = getResources().getIntArray(R.array.cpu_random_freq_interval);
        sCpuRateList = getResources().getIntArray(R.array.cpu_rate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.cpu_test);
        setType(TestType.TYPE_CPU_TEST);

        View contentView = inflater.inflate(R.layout.fragment_cpu_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mFreqs = ArmFreqUtils.getCpuAvailableFreqs();
        if (!mFreqs.isEmpty()) {
            String str = mFreqs.get(mFreqs.size() - 1);
            mMaxFreq = Integer.valueOf(str.split("M")[0]);

            mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, mFreqs);
            mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mFreqSpinner.setAdapter(mAdapter);
        } else {
            mRandomFreqCheckbox.setEnabled(false);
            mFixFreqCheckbox.setEnabled(false);
        }

        mFixFreqCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mRandomFreqCheckbox.setChecked(false);
                    mCpuRateCheckbox.setChecked(false);
                }
            }
        });

        mRandomFreqCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mFixFreqCheckbox.setChecked(false);
                    mCpuRateCheckbox.setChecked(false);
                }
            }
        });

        mCpuRateCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mFixFreqCheckbox.setChecked(false);
                    mRandomFreqCheckbox.setChecked(false);
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

            mMaxFreqTv.setText(mMaxFreq + "");

            mCurPercentTv.setText(mCurPercent + "");
            mCurPercentPgr.setProgress(mCurPercent);
            if (mCurPercent >= 70) {
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.red));
            } else if (mCurPercent >= 50) {
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.orange));
            } else {
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
            }

            if (mCurPercent > (mCpuRate + 5)) {
                mDelayCnt -= DELAY_CNT_STEP;
            } else if (mCurPercent < (mCpuRate - 5)) {
                mDelayCnt += DELAY_CNT_STEP;
            }

            if (mCurFreq > 0 && mMaxFreq > 0) {
                int progress = mCurFreq * 100 / mMaxFreq;
                mCurFreqTv.setText(mCurFreq + "");
                mCurFreqPgr.setProgress(progress);
                if (progress >= 70) {
                    mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.red));
                } else if (progress >= 50) {
                    mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.orange));
                } else {
                    mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
                }
            }
        } else {
            mSettingsContainer.setVisibility(View.VISIBLE);
            mRunningContainer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onStartClicked() {
        if (!isRunning()) {
            if (!mFixFreqCheckbox.isChecked()
                    && !mRandomFreqCheckbox.isChecked()
                    && !mCpuRateCheckbox.isChecked()) {
                showToast(R.string.at_least_one_test_case);
                return;
            }
        }
        super.onStartClicked();
    }

    @Override
    public void start() {
        // Check Governor mode when CPU frequency test
        if (mFixFreqCheckbox.isChecked() || mRandomFreqCheckbox.isChecked()) {
            try {
                ArmFreqUtils.setCpuGovernorMode(ArmFreqUtils.USERSPACE_MODE);
            } catch (Exception e) {
                Log.e(TAG, "start, setCpuGovernorMode failed: " + e.getMessage());
                showErrorDialog(R.string.cpu_test_userspace_mode_not_support);
                return;
            }
        }

        // Fixed frequency test
        if (mFixFreqCheckbox.isChecked()) {
            String str = mFreqs.get(mFreqSpinner.getSelectedItemPosition());
            mCurFreq = Integer.valueOf(str.split("M")[0]);
            int value = mCurFreq * 1000;
            try {
                ArmFreqUtils.setCpuFreq(value);
            } catch (Exception e) {
                Log.e(TAG, "start, setCpuFreq failed: " + e.getMessage());
                showErrorDialog(R.string.cpu_test_set_freq_fail);
                return;
            }

            // Frequency conversion test
        } else if (mRandomFreqCheckbox.isChecked()) {
            int index = mIntervalSpinner.getSelectedItemPosition();
            if (index < 0 || index >= sIntervalTimeList.length) {
                Log.e(TAG, "start, index invalid");
                showErrorDialog(R.string.cpu_test_invalid_interval);
                return;
            }

            if (!setRandomFreq()) {
                showErrorDialog(R.string.cpu_test_set_random_freq_fail);
                return;
            }

            int intervalTime = sIntervalTimeList[index];

            mRandomFreqTimer = new Timer();
            mRandomFreqTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isRunning()) {
                        mHandler.sendEmptyMessage(MSG_RANDOM_SET_FREQ);
                    }
                }
            }, intervalTime, intervalTime);

            // CPU usage test
        } else {
            if (null != mFreqs && !mFreqs.isEmpty()) {
                String str = mFreqs.get(mFreqs.size() - 1);
                mCurFreq = Integer.valueOf(str.split("M")[0]);
                int value = mCurFreq * 1000;
                try {
                    ArmFreqUtils.setCpuFreq(value);
                } catch (Exception e) {
                    Log.e(TAG, "start, setCpuFreq failed: " + e.getMessage());
                    showToast(R.string.cpu_test_set_freq_fail);
                }
            }

            int index = mCpuRateSpinner.getSelectedItemPosition();
            if (index < 0 || index >= sCpuRateList.length) {
                Log.e(TAG, "start, sCpuRateList index invalid");
                showErrorDialog(R.string.cpu_test_invalid_percent);
                return;
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    int cores = ArmFreqUtils.getNumberOfCPUCores();
                    mCpuRate = sCpuRateList[index];
                    Log.d(TAG, "start, CPU cores: " + cores + ", CPU rate: " + mCpuRate);

                    for (int i = 0; i < cores; i++) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                int dec;
                                while (isRunning()) {
                                    dec = mDelayCnt;
                                    while (dec > 0) {
                                        dec--;
                                    }
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }).start();
                    }
                }
            }, 1000);
        }

        if (mCpuReaderThread == null) {
            mCpuReaderThread = new ReaderThread();
            mCpuReaderThread.start();
        }

        super.start();
    }

    @Override
    public void stop() {
        if (mRandomFreqTimer != null) {
            mRandomFreqTimer.cancel();
            mRandomFreqTimer = null;
        }

        if (mCpuReaderThread != null) {
            mCpuReaderThread.cancel();

            try {
                mCpuReaderThread.join();
            } catch (InterruptedException ignored) {
            }

            mCpuReaderThread = null;
        }

        super.stop();
    }

    @Override
    public boolean isSupport() {
        return true;
    }

    private boolean setRandomFreq() {
        int size = mFreqs.size();
        int randomIndex = new Random().nextInt(size);
        String str = mFreqs.get(randomIndex);
        mCurFreq = Integer.valueOf(str.split("M")[0]);
        int value = mCurFreq * 1000;

        try {
            ArmFreqUtils.setCpuFreq(value);
        } catch (Exception e) {
            Log.e(TAG, "setRandomFreq, set random freq failed: " + e.getMessage());
            return false;
        }

        mCurFreqTv.setText(mCurFreq + "");

        return true;
    }

    @Override
    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case MSG_RANDOM_SET_FREQ:
                if (isRunning()) {
                    if (!setRandomFreq()) {
                        markFailure();
                    }
                }
                break;
        }
    }

    private static double getPercentInRange(double percent) {
        return percent > 100.0D ? 100.0D : (percent < 0.0D ? 0.0D : percent);
    }

    private class ReaderThread extends Thread {
        private BufferedReader totalCpuReader;
        private BufferedReader myPidCpuReader;
        private long totalJiffies;
        private long totalJiffiesBefore;
        private long jiffies;
        private long jiffiesBefore;
        private long jiffiesMyPid;
        private long jiffiesMyPidBefore;

        private ReaderThread() {
        }

        public void run() {
            while (true) {
                if (!Thread.currentThread().isInterrupted()) {
                    openCpuReaders();
                    read();
                    closeCpuReaders();

                    try {
                        Thread.currentThread();
                        Thread.sleep(1000);
                        continue;
                    } catch (InterruptedException var2) {
                        ;
                    }
                }

                return;
            }
        }

        public void cancel() {
            interrupt();
        }

        private void openCpuReaders() {
            if (totalCpuReader == null) {
                try {
                    totalCpuReader = new BufferedReader(new FileReader("/proc/stat"));
                } catch (FileNotFoundException var3) {
                    Log.w(TAG, "Could not open '/proc/stat' - " + var3.getMessage());
                }
            }

            if (myPidCpuReader == null) {
                try {
                    myPidCpuReader = new BufferedReader(new FileReader("/proc/"
                            + Process.myPid() + "/stat"));
                } catch (FileNotFoundException var2) {
                    Log.w(TAG, "Could not open '/proc/" + Process.myPid() + "/stat' - "
                            + var2.getMessage());
                }
            }
        }

        private void read() {
            String[] cpuData;
            if (totalCpuReader != null) {
                try {
                    cpuData = totalCpuReader.readLine().split("[ ]+", 9);
                    jiffies = Long.parseLong(cpuData[1]) + Long.parseLong(cpuData[2])
                            + Long.parseLong(cpuData[3]);
                    totalJiffies = jiffies + Long.parseLong(cpuData[4])
                            + Long.parseLong(cpuData[6]) + Long.parseLong(cpuData[7]);
                } catch (IOException var8) {
                    Log.w("CpuUsageDataModule", "Failed reading total cpu data - "
                            + var8.getMessage());
                }
            }

            if (myPidCpuReader != null) {
                try {
                    cpuData = myPidCpuReader.readLine().split("[ ]+", 18);
                    jiffiesMyPid = Long.parseLong(cpuData[13]) + Long.parseLong(cpuData[14])
                            + Long.parseLong(cpuData[15]) + Long.parseLong(cpuData[16]);
                } catch (IOException var7) {
                    Log.w("CpuUsageDataModule", "Failed reading my pid cpu data - "
                            + var7.getMessage());
                }
            }

            if (totalJiffiesBefore > 0L) {
                long totalDiff = totalJiffies - totalJiffiesBefore;
                long jiffiesDiff = jiffies - jiffiesBefore;
                long jiffiesMyPidDiff = jiffiesMyPid - jiffiesMyPidBefore;
                mCurPercent = (int) getPercentInRange((double) (100.0F * (float) jiffiesDiff
                        / (float) totalDiff));
                mCurMyPidPercent = (int) getPercentInRange((double) (100.0F * (float) jiffiesMyPidDiff
                        / (float) totalDiff));
            }

            totalJiffiesBefore = totalJiffies;
            jiffiesBefore = jiffies;
            jiffiesMyPidBefore = jiffiesMyPid;
        }

        private void closeCpuReaders() {
            try {
                if (totalCpuReader != null) {
                    totalCpuReader.close();
                    totalCpuReader = null;
                }

                if (myPidCpuReader != null) {
                    myPidCpuReader.close();
                    myPidCpuReader = null;
                }
            } catch (IOException ignored) {
            }

        }
    }
}
