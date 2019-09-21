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
import android.content.DialogInterface;
import android.os.Build;
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
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.ayst.stresstest.R;
import com.ayst.stresstest.util.ArmFreqUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CPUTestFragment extends BaseTestFragment {
    private static final int MSG_RANDOM_SET_FREQ = 1001;

    private LinearLayout mSettingsContainer;
    private LinearLayout mRunningContainer;
    private CheckBox mFixFreqCheckbox;
    private CheckBox mRandomFreqCheckbox;
    private CheckBox mCpuRateCheckbox;
    private Spinner mFreqSpinner;
    private Spinner mIntervalSpinner;
    private Spinner mCpuRateSpinner;
    private TextView mCurPercentTv;
    private ArcProgress mCurPercentPgr;
    private TextView mCurFreqTv;
    private ArcProgress mCurFreqPgr;
    private TextView mMaxFreqTv;

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
    private static int[] mIntervalTimeList = null;
    private static int[] mCpuRateList = null;
    private Timer mTimer;
    private Timer mRandomFreqTimer;
    private ReaderThread mCpuReaderThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntervalTimeList = this.getResources().getIntArray(R.array.cpu_random_freq_interval);
        mCpuRateList = this.getResources().getIntArray(R.array.cpu_rate);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.cpu_test);
        setCountType(COUNT_TYPE_TIME);
        setType(TestType.TYPE_CPU_TEST);

        View contentView = inflater.inflate(R.layout.fragment_cpu_test, container, false);
        setContentView(contentView);

        initView(contentView);

        return view;
    }

    private void initView(View view) {
        mSettingsContainer = (LinearLayout) view.findViewById(R.id.container_settings);
        mRunningContainer = (LinearLayout) view.findViewById(R.id.container_running);
        mFixFreqCheckbox = (CheckBox) view.findViewById(R.id.chbox_fix_freq);
        mRandomFreqCheckbox = (CheckBox) view.findViewById(R.id.chbox_random_freq);
        mCpuRateCheckbox = (CheckBox) view.findViewById(R.id.chbox_cpu_rate);
        mFreqSpinner = (Spinner) view.findViewById(R.id.spinner_freq);
        mIntervalSpinner = (Spinner) view.findViewById(R.id.spinner_interval);
        mCpuRateSpinner = (Spinner) view.findViewById(R.id.spinner_cpu_rate);
        mCurFreqTv = (TextView) view.findViewById(R.id.tv_freq);
        mCurFreqPgr = (ArcProgress) view.findViewById(R.id.pgr_freq);
        mMaxFreqTv = (TextView) view.findViewById(R.id.tv_freq_max);
        mCurPercentTv = (TextView) view.findViewById(R.id.tv_percent);
        mCurPercentPgr = (ArcProgress) view.findViewById(R.id.pgr_percent);

        mFreqs = ArmFreqUtils.getCpuAvailableFreqs();
        if (!mFreqs.isEmpty()) {
            String str = mFreqs.get(mFreqs.size() - 1);
            mMaxFreq = Integer.valueOf(str.split("M")[0]);
        } else {
            mStartBtn.setEnabled(false);
        }
        mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_spinner_item, mFreqs);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mFreqSpinner.setAdapter(mAdapter);

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
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mSettingsContainer.setVisibility(View.INVISIBLE);
            mRunningContainer.setVisibility(View.VISIBLE);

            mMaxFreqTv.setText(mMaxFreq+"");

            mCurPercentTv.setText(mCurPercent+"");
            mCurPercentPgr.setProgress(mCurPercent);
            if (mCurPercent >= 70) {
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.red));
            } else if (mCurPercent >= 50){
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.orange));
            } else {
                mCurPercentPgr.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
            }

            if (mCurPercent > (mCpuRate+5)) {
                mDelayCnt -= DELAY_CNT_STEP;
            } else if (mCurPercent < (mCpuRate-5)) {
                mDelayCnt += DELAY_CNT_STEP;
            }

            int progress = mCurFreq*100/mMaxFreq;
            mCurFreqTv.setText(mCurFreq+"");
            mCurFreqPgr.setProgress(progress);
            if (progress >= 70) {
                mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.red));
            } else if (progress >= 50){
                mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.orange));
            } else {
                mCurFreqPgr.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
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
                Toast.makeText(mActivity, R.string.at_least_one_test_case, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        super.onStartClicked();
    }

    @Override
    public void start() {
        if (mFreqs.isEmpty()) {
            Toast.makeText(mActivity, R.string.cpu_test_not_support, Toast.LENGTH_SHORT).show();
            return;
        }

        super.start();

        try {
            ArmFreqUtils.setCpuGovernorMode(ArmFreqUtils.USERSPACE_MODE);
        } catch (Exception e) {
            Log.e(TAG, "start, setCpuGovernorMode failed: " + e.getMessage());
            showErrorDialog(R.string.cpu_test_userspace_mode_not_support);
            return;
        }

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
        } else if (mRandomFreqCheckbox.isChecked()) {
            int index = mIntervalSpinner.getSelectedItemPosition();
            if (index < 0 || index >= mIntervalTimeList.length) {
                Log.e(TAG, "start, index invalid");
                showErrorDialog(R.string.cpu_test_invalid_interval);
                return;
            }

            if (!setRandomFreq()) {
                showErrorDialog(R.string.cpu_test_set_random_freq_fail);
                return;
            }

            int intervalTime = mIntervalTimeList[index];

            mRandomFreqTimer = new Timer();
            mRandomFreqTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isRunning()) {
                        mHandler.sendEmptyMessage(MSG_RANDOM_SET_FREQ);
                    }
                }
            }, intervalTime, intervalTime);
        } else {
            String str = mFreqs.get(mFreqs.size() - 1);
            mCurFreq = Integer.valueOf(str.split("M")[0]);
            int value = mCurFreq * 1000;
            try {
                ArmFreqUtils.setCpuFreq(value);
            } catch (Exception e) {
                Log.e(TAG, "start, setCpuFreq failed: " + e.getMessage());
                Toast.makeText(mActivity, R.string.cpu_test_set_freq_fail, Toast.LENGTH_SHORT).show();
            }

            int index = mCpuRateSpinner.getSelectedItemPosition();
            if (index < 0 || index >= mCpuRateList.length) {
                Log.e(TAG, "start, mCpuRateList index invalid");
                showErrorDialog(R.string.cpu_test_invalid_percent);
                return;
            }
            int cores = ArmFreqUtils.getNumberOfCPUCores();
            mCpuRate = mCpuRateList[index];
            Log.d(TAG, "start, CPU cores: " + cores + ", CPU rate: " + mCpuRate);

            for (int i=0; i<cores; i++) {
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

        // 启动读CPU占用率线程
//        if(Build.VERSION.SDK_INT >= 26) {
//            Log.w(TAG, "start, Read CPU state is not supported on Android O and above and will be no-op.");
//        } else {
            if(this.mCpuReaderThread == null) {
                this.mCpuReaderThread = new ReaderThread();
                this.mCpuReaderThread.start();
            }
//        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isRunning()) {
                    update();
                } else {
                    stop();
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void stop() {
        super.stop();

        if (mTimer != null) {
            mTimer.cancel();
        }

        if(this.mCpuReaderThread != null) {
            this.mCpuReaderThread.cancel();

            try {
                this.mCpuReaderThread.join();
            } catch (InterruptedException var2) {
                ;
            }

            this.mCpuReaderThread = null;
        }
    }

    private boolean setRandomFreq() {
        int size = mFreqs.size();
        int randomIndex = new Random().nextInt(size);
        String str = mFreqs.get(randomIndex);
        mCurFreq = Integer.valueOf(str.split("M")[0]);
        int value = mCurFreq * 1000;
        Log.d(TAG, "setRandomFreq, set freq value: " + value);
        try {
            ArmFreqUtils.setCpuFreq(value);
        } catch (Exception e) {
            Log.e(TAG, "setRandomFreq, set random freq failed: " + e.getMessage());
            return false;
        }
        mCurFreqTv.setText(mCurFreq+"");
        return true;
    }

    public void showErrorDialog(int strId) {
        showErrorDialog(getString(strId));
    }

    public void showErrorDialog(String msg) {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(msg)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mResult = RESULT.POOR;
                        stop();
                    }
                }).show();
    }

    @Override
    protected void handleMsg(Message msg) {
        super.handleMsg(msg);

        switch (msg.what) {
            case MSG_RANDOM_SET_FREQ:
                if (isRunning()) {
                    if (!setRandomFreq()) {
                        mResult = RESULT.POOR;
                        stop();
                    }
                }

                break;
        }
    }

    private static double getPercentInRange(double percent) {
        return percent > 100.0D?100.0D:(percent < 0.0D?0.0D:percent);
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
            while(true) {
                if(!Thread.currentThread().isInterrupted()) {
                    this.openCpuReaders();
                    this.read();
                    this.closeCpuReaders();

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
            this.interrupt();
        }

        private void openCpuReaders() {
            if(this.totalCpuReader == null) {
                try {
                    this.totalCpuReader = new BufferedReader(new FileReader("/proc/stat"));
                } catch (FileNotFoundException var3) {
                    Log.w(TAG, "Could not open '/proc/stat' - " + var3.getMessage());
                }
            }

            if(this.myPidCpuReader == null) {
                try {
                    this.myPidCpuReader = new BufferedReader(new FileReader("/proc/" + Process.myPid() + "/stat"));
                } catch (FileNotFoundException var2) {
                    Log.w(TAG, "Could not open '/proc/" + Process.myPid() + "/stat' - " + var2.getMessage());
                }
            }

        }

        private void read() {
            String[] cpuData;
            if(this.totalCpuReader != null) {
                try {
                    cpuData = this.totalCpuReader.readLine().split("[ ]+", 9);
                    this.jiffies = Long.parseLong(cpuData[1]) + Long.parseLong(cpuData[2]) + Long.parseLong(cpuData[3]);
                    this.totalJiffies = this.jiffies + Long.parseLong(cpuData[4]) + Long.parseLong(cpuData[6]) + Long.parseLong(cpuData[7]);
                } catch (IOException var8) {
                    Log.w("CpuUsageDataModule", "Failed reading total cpu data - " + var8.getMessage());
                }
            }

            if(this.myPidCpuReader != null) {
                try {
                    cpuData = this.myPidCpuReader.readLine().split("[ ]+", 18);
                    this.jiffiesMyPid = Long.parseLong(cpuData[13]) + Long.parseLong(cpuData[14]) + Long.parseLong(cpuData[15]) + Long.parseLong(cpuData[16]);
                } catch (IOException var7) {
                    Log.w("CpuUsageDataModule", "Failed reading my pid cpu data - " + var7.getMessage());
                }
            }

            if(this.totalJiffiesBefore > 0L) {
                long totalDiff = this.totalJiffies - this.totalJiffiesBefore;
                long jiffiesDiff = this.jiffies - this.jiffiesBefore;
                long jiffiesMyPidDiff = this.jiffiesMyPid - this.jiffiesMyPidBefore;
                mCurPercent = (int) getPercentInRange((double)(100.0F * (float)jiffiesDiff / (float)totalDiff));
                mCurMyPidPercent = (int) getPercentInRange((double)(100.0F * (float)jiffiesMyPidDiff / (float)totalDiff));
            }

            this.totalJiffiesBefore = this.totalJiffies;
            this.jiffiesBefore = this.jiffies;
            this.jiffiesMyPidBefore = this.jiffiesMyPid;
        }

        private void closeCpuReaders() {
            try {
                if(this.totalCpuReader != null) {
                    this.totalCpuReader.close();
                    this.totalCpuReader = null;
                }

                if(this.myPidCpuReader != null) {
                    this.myPidCpuReader.close();
                    this.myPidCpuReader = null;
                }
            } catch (IOException var2) {
                ;
            }

        }
    }
}
