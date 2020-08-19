package com.ayst.stresstest.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.util.Log;

public class AudioManager implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
    private static final String TAG = "AudioManager";

    private MediaPlayer mPlayer;
    private boolean isLoop;

    private AudioManager() {
    }

    private static final AudioManager sInstance = new AudioManager();

    public static AudioManager getDefault() {
        return sInstance;
    }

    /**
     * 播放音乐
     */
    public void playMusic(Context context, int resId) {
        Log.d(TAG, "playMusic");
        stopMusic();
        AssetFileDescriptor fd = context.getResources().openRawResourceFd(resId);
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setLooping(isLoop);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            postError(e.getMessage());
        }
    }

    public AudioManager setLoop(boolean loop) {
        isLoop = loop;
        return this;
    }

    /**
     * 播放音乐
     */
    public void playMusic(String path) {
        Log.d(TAG, "playMusic");
        stopMusic();
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(path);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setLooping(isLoop);
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            postError(e.getMessage());
        }
    }

    public void pause() {
        try {
            if (mPlayer != null && mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            postError(e.getMessage());
        }
    }

    public void resume() {
        try {
            if (mPlayer != null && !mPlayer.isPlaying()) {
                mPlayer.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            postError(e.getMessage());
        }
    }

    /**
     * 停止播放
     */
    public void stopMusic() {
        Log.d(TAG, "stopMusic");
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        if (mPlayer == null)
            return;
        if (mOnPlayStatusListener != null) {
            mOnPlayStatusListener.onReady();
        }
        try {
            mp.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            postError(e.getMessage());
        }
    }

    private void postError(String message) {
        if (mOnPlayStatusListener != null) {
            mOnPlayStatusListener.onError(message);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mOnPlayStatusListener != null) {
            mOnPlayStatusListener.onComplete();
        }
    }

    private OnPlayStatusListener mOnPlayStatusListener;

    public void setOnPlayStatusListener(OnPlayStatusListener onPlayStatusListener) {
        mOnPlayStatusListener = onPlayStatusListener;
    }

    public interface OnPlayStatusListener {
        void onReady();

        void onComplete();

        void onError(String message);
    }
}
