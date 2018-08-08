package com.ayst.stresstest.test;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.ayst.stresstest.R;
import com.ayst.stresstest.util.MemOpUtils;

import java.util.Timer;
import java.util.TimerTask;

public class MemoryTestFragment extends BaseTestFragment {

    private ArcProgress mArcProgress;
    private TextView mFreeMemoryTv;
    private TextView mTotalMemoryTv;
    private Spinner mFillPercentSpinner;

    private int mFreeMemory;
    private int mUsedMemory;
    private int mTotalMemory;
    private int mFillPercent;
    private static int[] sMemoryFillPercentList = null;
    private int mCountDown = 30;

    private Timer mTimer;
    private ActivityManager mAm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAm = (ActivityManager)mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        sMemoryFillPercentList = this.getResources().getIntArray(R.array.memory_fill_percent);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.memory_test);
        setCountType(COUNT_TYPE_TIME);
        setType(TestType.TYPE_MEMORY_TEST);

        View contentView = inflater.inflate(R.layout.fragment_memory_test, container, false);
        setContentView(contentView);

        initView(view);

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        mCountDown = 30;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();

                if (isRunning()) {
                    if ((mUsedMemory*100)/mTotalMemory < mFillPercent) {
                        MemOpUtils.malloc(10);
                    } else {
                        if (mCountDown > 0) {
                            mCountDown--;
                        } else {
                            if (mTimer != null) {
                                mTimer.cancel();
                                mTimer = null;
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

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private void initView(View view) {
        mArcProgress = (ArcProgress) view.findViewById(R.id.arc_progress);
        mFreeMemoryTv = (TextView) view.findViewById(R.id.tv_free_memory);
        mTotalMemoryTv = (TextView) view.findViewById(R.id.tv_total_memory);
        mFillPercentSpinner = (Spinner) view.findViewById(R.id.spinner_percent);
        mFillPercentSpinner.setSelection(2); // 默认80%
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        MemoryInfo systemMemInfo = new MemoryInfo();
        mAm.getMemoryInfo(systemMemInfo);
        mFreeMemory = (int) systemMemInfo.availMem/1024/1024;
        mTotalMemory = (int) systemMemInfo.totalMem/1024/1024;
        mUsedMemory = mTotalMemory - mFreeMemory;
        int progress = (mUsedMemory*100)/mTotalMemory;
        mArcProgress.setProgress(progress);
        if (progress >= 70) {
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.red));
        } else if (progress >= 50){
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.orange));
        } else {
            mArcProgress.setFinishedStrokeColor(getResources().getColor(R.color.colorAccent));
        }
        mFreeMemoryTv.setText(mFreeMemory+"");
        mTotalMemoryTv.setText(mTotalMemory+"");
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
}
