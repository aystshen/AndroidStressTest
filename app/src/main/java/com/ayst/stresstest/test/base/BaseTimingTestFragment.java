package com.ayst.stresstest.test.base;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.ayst.stresstest.R;
import com.orhanobut.logger.Logger;

import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseTimingTestFragment extends BaseTestFragment {

    // The target test time
    protected int mTargetTime = 0;

    // The current test time
    protected int mCurrentTime = 0;

    // Timing timer
    private Timer mTimer;

    @Override
    public void start() {
        Logger.t(TAG).d("Start %s", time2String(mTargetTime));

        mCurrentTime = 0;

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning() || mCurrentTime >= mTargetTime) {
                    stop();
                } else {
                    mCurrentTime++;
                    update();
                }
            }
        }, 1000, 1000);

        super.start();
    }

    @Override
    public void stop() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        Logger.t(TAG).d("Stop %s %s", formatTimeString(), sResultStringMap.get(mResult));

        super.stop();
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (mTargetTime > 0) {
            mCountTv.setText(formatTimeString());
            mCountTv.setVisibility(View.VISIBLE);

            mFailureCountTv.setText(mFailureCount + "");
            mFailureCountTv.setVisibility(View.VISIBLE);
        } else {
            mCountTv.setVisibility(View.GONE);
            mFailureCountTv.setVisibility(View.GONE);
        }

        mProgressbar.setProgress(mCurrentTime > 0 ? (mCurrentTime * 100) / mTargetTime : 0);
    }

    @Override
    public void showSetTargetDialog() {
        final EditText editText = new EditText(mActivity);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this.getActivity())
                .setTitle(getString(R.string.set_duration_tips))
                .setView(editText)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            mTargetTime = Integer.valueOf(text) * 3600;
                            start();
                        } else {
                            showToast(getString(R.string.set_duration_tips));
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    private String time2String(int time) {
        int curHour = time / 3600;
        int curMin = (time % 3600) / 60;
        int curSec = (time % 3600) % 60;
        return curHour + ":" + curMin + ":" + curSec;
    }

    private String formatTimeString() {
        return time2String(mCurrentTime) + "/" + time2String(mTargetTime);
    }
}
