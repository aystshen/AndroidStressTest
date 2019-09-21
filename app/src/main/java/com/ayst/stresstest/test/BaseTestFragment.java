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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.StringRes;

import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.ayst.stresstest.R;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BaseTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BaseTestFragment extends Fragment {
    protected String TAG = "BaseTestFragment";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    protected static final int MSG_UPDATE = 1;

    protected ProgressBar mProgressbar;
    protected TextView mTitleTv;
    protected TextView mCountTv;
    protected TextView mFailureCountTv;
    protected FrameLayout mTitleContainer;
    protected RelativeLayout mContentContainer;
    protected Button mStartBtn;
    protected ImageView mLogoIv;
    protected RelativeLayout mFullContainer;

    protected Activity mActivity;

    protected TestType mType = TestType.TYPE_CPU_TEST;

    public static final int STATE_RUNNING = 1;
    public static final int STATE_STOP = 2;
    protected int mState = STATE_STOP;

    protected static final int RESULT_SUCCESS = 1;
    protected static final int RESULT_FAIL = 2;
    protected static final int RESULT_CANCEL = 3;
    protected int mResult = RESULT_CANCEL;

    protected static final int COUNT_TYPE_COUNT = 1;
    protected static final int COUNT_TYPE_TIME = 2;
    protected static final int COUNT_TYPE_NONE = 3;
    protected int mCountType = COUNT_TYPE_COUNT;

    protected int mMaxTestCount = 0;
    protected int mMaxTestTime = 0;
    protected int mCurrentCount = 0;
    protected int mFailureCount = 0;
    protected int mCurrentTime = 0;
    private Timer mCountTimer;

    private boolean isEnable = true;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    protected OnFragmentInteractionListener mListener;

    public BaseTestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BaseTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BaseTestFragment newInstance(String param1, String param2) {
        BaseTestFragment fragment = new BaseTestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mActivity = this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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

    protected void updateImpl() {
        if (mCountType == COUNT_TYPE_COUNT) {
            if (mMaxTestCount > 0) {
                mCountTv.setText(mCurrentCount + "/" + mMaxTestCount);
                mCountTv.setVisibility(View.VISIBLE);
                mFailureCountTv.setVisibility(View.VISIBLE);
            } else {
                mCountTv.setVisibility(View.GONE);
                mFailureCountTv.setVisibility(View.GONE);
            }
            mFailureCountTv.setText(mFailureCount + "/" + mCurrentCount);
        } else if (mCountType == COUNT_TYPE_TIME) {
            if (mMaxTestTime > 0) {
                int curHour = mCurrentTime / 3600;
                int curMin = (mCurrentTime % 3600) / 60;
                int curSec = (mCurrentTime % 3600) % 60;
                mCountTv.setText(curHour + ":" + curMin + ":" + curSec + "/" + mMaxTestTime + ":0:0");
                mCountTv.setVisibility(View.VISIBLE);
            } else {
                mCountTv.setVisibility(View.GONE);
            }
        } else {
            mCountTv.setVisibility(View.GONE);
        }

        if (isRunning()) {
            mStartBtn.setText(R.string.stop);
            mStartBtn.setSelected(true);
            if (mCountType == COUNT_TYPE_NONE) {
                mProgressbar.setVisibility(View.GONE);
            } else {
                mProgressbar.setProgress((mCountType == COUNT_TYPE_COUNT) ? (mCurrentCount * 100) / mMaxTestCount : (mCurrentTime * 100) / (mMaxTestTime * 3600));
                mProgressbar.setVisibility(View.VISIBLE);
            }
            mLogoIv.setVisibility(View.VISIBLE);
        } else {
            mStartBtn.setText(R.string.start);
            mStartBtn.setSelected(false);
            mProgressbar.setVisibility(View.INVISIBLE);
            updateTitleBg();
            mLogoIv.setVisibility(View.INVISIBLE);
        }
    }

    protected void update() {
        mHandler.sendEmptyMessage(MSG_UPDATE);
    }

    protected void onStartClicked() {
        if (isRunning()) {
            showStopDialog();
        } else {
            if (mCountType == COUNT_TYPE_NONE) {
                start();
            } else {
                showSetMaxDialog();
            }
        }
    }

    private void updateTitleBg() {
        if (!isEnable()) {
            mTitleContainer.setBackgroundColor(getResources().getColor(R.color.black_50));
        } else if (mResult == RESULT_SUCCESS) {
            mTitleContainer.setBackgroundColor(getResources().getColor(R.color.green));
        } else if (mResult == RESULT_FAIL) {
            mTitleContainer.setBackgroundColor(getResources().getColor(R.color.red));
        } else {
            mTitleContainer.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
        }
    }

    protected void setTitle(String text) {
        mTitleTv.setText(text);
    }

    protected void setTitle(@StringRes int textId) {
        mTitleTv.setText(textId);
    }

    protected void setContentView(View contentView) {
        mContentContainer.addView(contentView);
    }

    protected void setFullContentView(View contentView) {
        mFullContainer.addView(contentView);
    }

    protected void setCountType(int type) {
        mCountType = type;
    }

    protected void setType(TestType type) {
        mType = type;
    }

    public void start() {
        Logger.t(TAG).d("Start %s", mCountType == COUNT_TYPE_COUNT ? mMaxTestCount : mMaxTestTime + "h");

        mState = STATE_RUNNING;
        mResult = RESULT_CANCEL;
        mCurrentCount = 0;
        mCurrentTime = 0;

        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, STATE_RUNNING);
                }
            });
        }

        if (mCountType == COUNT_TYPE_TIME) {
            mCountTimer = new Timer();
            mCountTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isRunning() || (mMaxTestTime != 0 && mCurrentTime >= mMaxTestTime * 3600)) {
                        Log.d(TAG, "CountTimer, " + TAG + " test finish!");
                        mResult = RESULT_SUCCESS;
                        stop();
                    } else {
                        mCurrentTime++;
                        update();
                    }
                }
            }, 1000, 1000);
        }

        update();
    }

    public void stop() {
        String message;
        if (mCountType == COUNT_TYPE_COUNT) {
            message = mCurrentCount + "/" + mMaxTestCount;
            if (mFailureCount > 0) {
                mResult = RESULT_FAIL;
            } else {
                mResult = RESULT_SUCCESS;
            }
        } else {
            int curHour = mCurrentTime / 3600;
            int curMin = (mCurrentTime % 3600) / 60;
            int curSec = (mCurrentTime % 3600) % 60;
            message = curHour + ":" + curMin + ":" + curSec + "/" + mMaxTestTime + ":0:0";
        }
        Logger.t(TAG).d("Stop %s %s", message, mResult == RESULT_SUCCESS ? "SUCCESS" : (mResult == RESULT_CANCEL ? "CANCEL" : "FAIL"));

        mState = STATE_STOP;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, STATE_STOP);
                }
            });
        }
        if (mCountTimer != null) {
            mCountTimer.cancel();
        }
        update();
    }

    protected void IncCurrentCount() {
        mCurrentCount++;
        Logger.t(TAG).d("Testing %d/%d", mCurrentCount, mMaxTestCount);
    }

    protected void incFailureCount() {
        mFailureCount++;
    }

    public boolean isRunning() {
        return mState == STATE_RUNNING;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
        mStartBtn.setEnabled(enable);
        updateTitleBg();
    }

    public boolean isEnable() {
        return isEnable;
    }

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

    public void showSetMaxDialog() {
        final String title = mCountType == COUNT_TYPE_COUNT ? getString(R.string.set_time_tips) : getString(R.string.set_duration_tips);
        final EditText editText = new EditText(this.getActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this.getActivity())
                .setTitle(title)
                .setView(editText)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!editText.getText().toString().trim().equals("")) {
                            if (mCountType == COUNT_TYPE_COUNT) {
                                mMaxTestCount = Integer.valueOf(editText.getText().toString());
                            } else {
                                mMaxTestTime = Integer.valueOf(editText.getText().toString());
                            }
                            start();
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

    public void showStopDialog() {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.stop_test_tips)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mResult = RESULT_CANCEL;
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
        // TODO: Update argument type and name
        void onFragmentInteraction(TestType testType, int state);
    }
}
