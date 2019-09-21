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

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ayst.stresstest.R;
import com.ayst.stresstest.util.FileUtils;

import static android.app.Activity.RESULT_OK;

public class VideoTestFragment extends BaseTestFragment {
    private RelativeLayout mVideoContainer;
    private LinearLayout mContentContainer;
    private VideoView mVideoView;
    private TextView mPathTv;
    private TextView mErrorTv;

    private String mPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.video_test);
        setCountType(COUNT_TYPE_TIME);
        setType(TestType.TYPE_VIDEO_TEST);

        View contentView = inflater.inflate(R.layout.fragment_video_test, container, false);
        setFullContentView(contentView);

        initView(contentView);


        return view;
    }

    private void initView(View view) {
        mVideoContainer = (RelativeLayout) view.findViewById(R.id.container_video);
        mContentContainer = (LinearLayout) view.findViewById(R.id.container_content);
        mVideoView = (VideoView) view.findViewById(R.id.video_view);
        mPathTv = (TextView) view.findViewById(R.id.tv_path);
        mErrorTv = (TextView) view.findViewById(R.id.tv_error);

        mPathTv.setText(mPath);
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mContentContainer.setVisibility(View.GONE);
            mVideoContainer.setVisibility(View.VISIBLE);
        } else {
            mVideoContainer.setVisibility(View.INVISIBLE);
            mPathTv.setText(mPath);
            mContentContainer.setVisibility(View.VISIBLE);
            if (mResult == RESULT.POOR || mResult == RESULT.FAIL) {
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
        super.start();

        playVideo();
    }

    @Override
    public void stop() {
        super.stop();

        stopVideo();
    }

    private boolean playVideo() {
        if (mPath == null) {
            Toast.makeText(mActivity, R.string.video_test_select_file_tips, Toast.LENGTH_LONG).show();
            return false;
        }
        MediaController mc = new MediaController(mActivity);
        mVideoView.setMediaController(mc);
        //videoView.setVideoURI(Uri.parse(""));
        mVideoView.setVideoPath(mPath);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "onCompletion, restart");
                mVideoView.start();
            }
        });
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mErrorTv.setText(getString(R.string.video_test_play_error) + "（" + what + "," + extra + ")");
                mResult = RESULT.POOR;
                stop();
                return false;
            }
        });
        mVideoView.requestFocus();
        mVideoView.start();

        return true;
    }

    private void stopVideo() {
        mVideoView.stopPlayback();
    }

    private static final int FILE_SELECT_CODE = 0;
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult( Intent.createChooser(intent, getString(R.string.video_test_select_file)), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(mActivity, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
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
                    mPath = FileUtils.getPath(mActivity, uri);
                    Log.d(TAG, "onActivityResult, File Path: " + mPath);

                    if (!TextUtils.isEmpty(mPath)) {
                        super.onStartClicked();
                    } else {
                        Toast.makeText(mActivity, R.string.video_test_invalid_file, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
