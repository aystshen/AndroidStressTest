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

package com.ayst.stresstest.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

public class RingVibrateHelper {
    private static String TAG = "RingVibrateHelper";
    private static Context sContext = null;
    private static RingVibrateHelper sInstance = null;

    private MediaPlayer mMediaPlayer = null;
    private AudioManager mAudioManager = null;
    private Vibrator mVibrator = null;

    private boolean isStartRing = false;

    private RingVibrateHelper(Context context) {
        sContext = context;
        mAudioManager = (AudioManager) sContext.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) sContext.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static RingVibrateHelper instance(Context context) {
        if (sInstance == null) {
            sInstance = new RingVibrateHelper(context);
        }

        return sInstance;
    }

    public void startRing(int resId, boolean isRepeat) {
        stopRing();
        if (!isStartRing) {
            Log.i(TAG, "startRing");
            isStartRing = true;

            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            mMediaPlayer = MediaPlayer.create(sContext, resId);
            if (mMediaPlayer != null) {
                mMediaPlayer.setLooping(isRepeat);
                mMediaPlayer.start();
                mMediaPlayer.setVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        Log.i(TAG, "onCompletion");
                        stopRing();
                    }
                });
                mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.i(TAG, "onError, what=" + what + ", extra=" + extra);
                        stopRing();
                        return false;
                    }
                });
            }
        }
    }

    public void stopRing() {
        if (isStartRing) {
            Log.i(TAG, "stopRing");
            isStartRing = false;

            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
                mAudioManager.setMode(AudioManager.MODE_NORMAL);
            }
        }
    }

    public void startVibrate(boolean isRepeat) {
        mVibrator.vibrate(new long[]{1000, 1000, 1000, 1000, 1000}, isRepeat ? 1 : -1);
    }

    public void stopVibrate() {
        mVibrator.cancel();
    }
}
