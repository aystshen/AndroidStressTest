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

package com.ayst.stresstest.test.audio;

import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseTimingTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.AudioManager;
import com.ayst.stresstest.util.FileUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;

public class AudioTestFragment extends BaseTimingTestFragment {

    @BindView(R.id.container_content)
    LinearLayout mContentContainer;
    @BindView(R.id.tv_path)
    TextView mPathTv;
    @BindView(R.id.tv_error)
    TextView mErrorTv;
    @BindView(R.id.audio_rl_cover)
    RelativeLayout mRlCover;
    @BindView(R.id.container_audio)
    LinearLayout mAudioContainer;
    @BindView(R.id.iv_center)
    ImageView mIvCenter;
    @BindView(R.id.iv_status)
    ImageView mIvStatus;
    @BindView(R.id.iv_pre)
    ImageView mIvPre;
    @BindView(R.id.iv_next)
    ImageView mIvNext;
    @BindView(R.id.pb_load)
    ProgressBar mPbLoad;
    Unbinder unbinder;

    private String mPath;
    private ValueAnimator mCoverAnimator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.audio_test);
        setType(TestType.TYPE_AUDIO_TEST);

        View contentView = inflater.inflate(R.layout.fragment_audio_test, container, false);
        setFullContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initCDAnimation();

        if (TextUtils.isEmpty(mPath)) {
            mPathTv.setVisibility(View.GONE);
        } else {
            mPathTv.setVisibility(View.VISIBLE);
            mPathTv.setText(mPath);
        }

        AudioManager.getDefault().setOnPlayStatusListener(new AudioManager.
                OnPlayStatusListener() {
            @Override
            public void onReady() {
                resumeCoverAnimation();
                mIvCenter.setSelected(true);
            }

            @Override
            public void onComplete() {
                pauseCoverAnimation();
                mIvCenter.setSelected(false);
            }

            @Override
            public void onError(String message) {
                mIvCenter.setSelected(false);
                mErrorTv.setText(message);
                pauseCoverAnimation();
                markFailure();
                playAudio();
            }
        });

        mIvCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mIvPre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mIvNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    /**
     * 初始化CD动画
     */
    private void initCDAnimation() {
        mCoverAnimator = ValueAnimator.ofFloat(mRlCover.getRotation(), 360f + mRlCover.getRotation());
        mCoverAnimator.setTarget(mRlCover);
        mCoverAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mCoverAnimator.setDuration(15000);
        mCoverAnimator.setInterpolator(new LinearInterpolator());
        mCoverAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float current = (Float) animation.getAnimatedValue();
                mRlCover.setRotation(current);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        pauseCoverAnimation();
        mCoverAnimator = null;
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mContentContainer.setVisibility(View.GONE);
            mAudioContainer.setVisibility(View.VISIBLE);
        } else {
            mAudioContainer.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
            mPathTv.setVisibility(View.VISIBLE);
            mPathTv.setText(mPath);
            if (mResult == Result.POOR || mResult == Result.FAIL) {
                mErrorTv.setVisibility(View.VISIBLE);
            } else {
                mErrorTv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStartClicked() {
        if (!isRunning()) {
            showFileChooser();
        } else {
            super.onStartClicked();
        }
    }

    @Override
    public void start() {
        playAudio();

        super.start();
    }

    @Override
    public void stop() {
        stopAudio();

        super.stop();
    }

    /**
     * 开始cd动画
     */
    private void resumeCoverAnimation() {
        if (mCoverAnimator != null && !mCoverAnimator.isRunning()) {
            mCoverAnimator.setFloatValues(mRlCover.getRotation(), 360f + mRlCover.getRotation());
            mCoverAnimator.start();
        }
    }

    /**
     * 暂停CD动画
     */
    private void pauseCoverAnimation() {
        if (mCoverAnimator != null && mCoverAnimator.isRunning()) {
            mCoverAnimator.cancel();
        }
    }

    @Override
    public boolean isSupport() {
        return true;
    }

    private boolean playAudio() {
        if (mPath == null) {
            showToast(R.string.audio_test_select_file_tips);
            return false;
        }
        AudioManager.getDefault()
                .setLoop(true)
                .playMusic(mPath);

        return true;
    }

    private void stopAudio() {
        AudioManager.getDefault().stopMusic();
    }

    private static final int FILE_SELECT_CODE = 1;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.audio_test_select_file)), FILE_SELECT_CODE);
        } catch (ActivityNotFoundException ex) {
            showToast("Please install a File Manager.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "onActivityResult, File Uri: " + uri.toString());
                    // Get the path
                    mPath = FileUtils.getFilePathByUri(mActivity, uri);
                    Log.d(TAG, "onActivityResult, File Path: " + mPath);
                    if (!TextUtils.isEmpty(mPath)) {
                        super.onStartClicked();
                    } else {
                        showToast(R.string.video_test_invalid_file);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
