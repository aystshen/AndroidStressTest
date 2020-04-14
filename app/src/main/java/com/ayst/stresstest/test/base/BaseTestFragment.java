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

package com.ayst.stresstest.test.base;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.StringRes;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ayst.stresstest.R;
import com.ayst.stresstest.service.WorkService;

import java.util.HashMap;

public abstract class BaseTestFragment extends Fragment {
    protected String TAG;

    // Notify Handler Message for update ui
    protected static final int MSG_UPDATE = 1;

    // Progress bar
    protected ProgressBar mProgressbar;

    // The title view
    protected TextView mTitleTv;

    // Tested count view
    protected TextView mCountTv;

    // Failure count view
    protected TextView mFailureCountTv;

    // Title container
    protected FrameLayout mTitleContainer;

    // Content container
    protected RelativeLayout mContentContainer;

    // Start button
    protected Button mStartBtn;

    // Logo view
    protected ImageView mLogoIv;

    // Full container
    protected RelativeLayout mFullContainer;

    // Context
    protected Activity mActivity;

    // Test item types
    protected TestType mType = TestType.TYPE_CPU_TEST;

    // Failure count, Record the number of failure
    protected int mFailureCount = 0;

    // Fail threshold, Slight errors show yellow
    protected int mFailThreshold = 1;

    // Poor threshold, Serious errors show red
    protected int mPoorThreshold = 3;

    // Is available?
    private boolean isAvailable = true;

    // Notice the Activity state
    protected OnFragmentInteractionListener mListener;

    protected Handler mThreadHandler;

    // Running state
    public class State {
        public static final int RUNNING = 1;
        public static final int STOP = 2;
    }

    // Current running state
    protected int mState = State.STOP;

    // The test results
    protected enum Result {
        GOOD,
        FAIL,
        POOR,
        CANCEL
    }

    // Current test result
    protected Result mResult = Result.GOOD;

    // The test results corresponding to the progress of color
    protected HashMap<Result, ClipDrawable> mResultDrawable = new HashMap<>();

    // The test results character
    protected static HashMap<Result, String> sResultStringMap = new HashMap<>();

    static {
        sResultStringMap.put(Result.GOOD, "GOOD");
        sResultStringMap.put(Result.FAIL, "FAIL");
        sResultStringMap.put(Result.POOR, "POOR");
        sResultStringMap.put(Result.CANCEL, "CANCEL");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this.getActivity();
        TAG = this.getClass().getSimpleName();

        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        mThreadHandler = new Handler(handlerThread.getLooper());

        mResultDrawable.put(Result.GOOD, new ClipDrawable(new ColorDrawable(Color.GREEN), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(Result.FAIL, new ClipDrawable(new ColorDrawable(Color.YELLOW), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(Result.POOR, new ClipDrawable(new ColorDrawable(Color.RED), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(Result.CANCEL, new ClipDrawable(new ColorDrawable(Color.GRAY), Gravity.LEFT, ClipDrawable.HORIZONTAL));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base_test, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mProgressbar = (ProgressBar) view.findViewById(R.id.progressbar);
        mTitleTv = (TextView) view.findViewById(R.id.tv_title);
        mCountTv = (TextView) view.findViewById(R.id.tv_count);
        mFailureCountTv = (TextView) view.findViewById(R.id.tv_failure_count);
        mTitleContainer = (FrameLayout) view.findViewById(R.id.container_title);
        mContentContainer = (RelativeLayout) view.findViewById(R.id.container);
        mStartBtn = (Button) view.findViewById(R.id.btn_start);
        mLogoIv = (ImageView) view.findViewById(R.id.iv_logo);
        mFullContainer = (RelativeLayout) view.findViewById(R.id.container_full);

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartClicked();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        updateImpl();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isRunning()) {
            stop();
        }
    }

    /**
     * Update ui, can only be executed in the main thread
     */
    protected void updateImpl() {
        // Updating progressbar.
        mProgressbar.setVisibility(isAvailable() ? View.VISIBLE : View.INVISIBLE);
        if (mProgressbar.getProgressDrawable() != mResultDrawable.get(mResult)) {
            mProgressbar.setProgressDrawable(mResultDrawable.get(mResult));
        }

        // Updating other.
        mStartBtn.setEnabled(isAvailable());
        if (isRunning()) {
            mStartBtn.setText(R.string.stop);
            mStartBtn.setSelected(true);
            mLogoIv.setVisibility(View.VISIBLE);
        } else {
            mStartBtn.setText(R.string.start);
            mStartBtn.setSelected(false);
            mLogoIv.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * To update the UI in the main thread
     */
    protected void update() {
        mHandler.sendEmptyMessage(MSG_UPDATE);
    }

    /**
     * Click the start button
     */
    protected void onStartClicked() {
        if (isRunning()) {
            showStopDialog();
        } else {
            showSetTargetDialog();
        }
    }

    /**
     * Set the title
     *
     * @param text String
     */
    protected void setTitle(String text) {
        mTitleTv.setText(text);
    }

    /**
     * Set the title by resource id
     *
     * @param textId Resource id
     */
    protected void setTitle(@StringRes int textId) {
        mTitleTv.setText(textId);
    }

    /**
     * Set the content
     *
     * @param contentView
     */
    protected void setContentView(View contentView) {
        mContentContainer.addView(contentView);
    }

    /**
     * Set the full content
     *
     * @param contentView
     */
    protected void setFullContentView(View contentView) {
        mFullContainer.addView(contentView);
    }

    /**
     * Set test type
     *
     * @param type TestType
     */
    protected void setType(TestType type) {
        mType = type;
    }

    /**
     * Set failure threshold
     *
     * @param fail
     * @param poor
     */
    protected void setThreshold(int fail, int poor) {
        if (poor > fail) {
            mFailThreshold = fail;
            mPoorThreshold = poor;
        } else {
            Log.w(TAG, "setThreshold, Poor threshold must be greater than the Fail threshold.");
        }
    }

    /**
     * To start testing
     */
    public void start() {
        mState = State.RUNNING;
        mResult = Result.GOOD;
        mFailureCount = 0;

        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, State.RUNNING);
                }
            });
        }

        update();
    }

    /**
     * To stop testing
     */
    public void stop() {
        mState = State.STOP;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, State.STOP);
                }
            });
        }

        update();
    }

    /**
     * Mark a failure, the failure count
     */
    protected void markFailure() {
        markFailure(-1);
    }

    /**
     * Mark a failure, the failure count
     */
    protected void markFailure(int count) {
        if (count > 0) {
            mFailureCount = count;
        } else {
            mFailureCount++;
        }

        if (mFailureCount >= mPoorThreshold) {
            mResult = Result.POOR;
        } else if (mFailureCount >= mFailThreshold) {
            mResult = Result.FAIL;
        } else {
            mResult = Result.GOOD;
        }

        update();
    }

    /**
     * Is running?
     *
     * @return
     */
    public boolean isRunning() {
        return mState == State.RUNNING;
    }

    /**
     * Set Available or unavailable
     *
     * @param available
     */
    public void setAvailable(boolean available) {
        isAvailable = available;
        update();
    }

    /**
     * Available or unavailable?
     *
     * @return
     */
    public boolean isAvailable() {
        return isAvailable && isSupport();
    }

    /**
     * Is support?
     *
     * @return
     */
    public abstract boolean isSupport();

    @SuppressLint("HandlerLeak")
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE:
                    updateImpl();
                    break;

                default:
                    handleMsg(msg);
                    break;
            }
        }
    };

    protected void handleMsg(Message msg) {

    }


    /**
     * Pop-up set test time or number of dialog
     */
    protected abstract void showSetTargetDialog();

    /**
     * Pop-up the stop dialog
     */
    protected void showStopDialog() {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.stop_test_tips)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stop();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * A {@link Handler} for showing {@link Toast}s on the UI thread.
     */
    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mActivity, (String) msg.obj, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param resId The message to show.
     */
    protected void showToast(int resId) {
        showToast(getString(resId));
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    protected void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    /**
     * Pop-up the error dialog
     *
     * @param strId
     */
    protected void showErrorDialog(int strId) {
        showErrorDialog(getString(strId));
    }

    /**
     * Pop-up the error dialog
     *
     * @param msg
     */
    protected void showErrorDialog(String msg) {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(msg)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mResult = Result.POOR;
                        stop();
                    }
                }).show();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(TestType testType, int state);
    }
}
