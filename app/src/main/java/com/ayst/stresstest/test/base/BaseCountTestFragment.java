package com.ayst.stresstest.test.base;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.ayst.stresstest.R;
import com.orhanobut.logger.Logger;

public abstract class BaseCountTestFragment extends BaseTestFragment {

    // The target test count
    protected int mTargetCount = 0;

    // The current test count
    protected int mCurrentCount = 0;

    @Override
    public void start() {
        Logger.t(TAG).d("Start %s times", mTargetCount);

        mCurrentCount = 0;

        super.start();
    }

    @Override
    public void stop() {
        Logger.t(TAG).d("Stop %s %s", mCurrentCount + "/" + mTargetCount, sResultStringMap.get(mResult));

        super.stop();
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (mTargetCount > 0) {
            mCountTv.setText(mCurrentCount + "/" + mTargetCount);
            mCountTv.setVisibility(View.VISIBLE);

            mFailureCountTv.setText(mFailureCount + "/" + mCurrentCount);
            mFailureCountTv.setVisibility(View.VISIBLE);
        } else {
            mCountTv.setVisibility(View.GONE);
            mFailureCountTv.setVisibility(View.GONE);
        }

        mProgressbar.setProgress(mCurrentCount > 0 ? (mCurrentCount * 100) / mTargetCount : 0);
    }

    protected boolean next() {
        mCurrentCount++;
        if (!isRunning() || mCurrentCount >= mTargetCount) {
            stop();

            return false;
        } else {
            update();

            Logger.t(TAG).d("Testing %d/%d", mCurrentCount, mTargetCount);

            return true;
        }
    }

    @Override
    protected void showSetTargetDialog() {
        final EditText editText = new EditText(mActivity);
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this.getActivity())
                .setTitle(getString(R.string.set_time_tips))
                .setView(editText)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editText.getText().toString();
                        if (!TextUtils.isEmpty(text)) {
                            mTargetCount = Integer.valueOf(text);
                            if (mTargetCount > 0) {
                                start();
                                return;
                            }
                        }
                        showToast(getString(R.string.set_time_tips));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }
}
