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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SPUtils {
    public static final String SP = "stress_test";

    public static final String VERSION_KEY = "version_code";
    public static final String KEY_DEBUG = "debug";

    private static SPUtils instance;
    private static SharedPreferences mSp = null;

    private SPUtils(Context context) {
        mSp = context.getSharedPreferences(SP, Context.MODE_PRIVATE);
    }

    public static SPUtils getInstance(Context context) {
        if (instance == null)
            instance = new SPUtils(context);
        return instance;
    }

    public void saveData(String key, String value) {
        Editor editor = mSp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void saveData(String key, boolean value) {
        Editor editor = mSp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void saveData(String key, int value) {
        Editor editor = mSp.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void saveData(String key, long value) {
        Editor editor = mSp.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public void saveData(String key, float value) {
        Editor editor = mSp.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public String getData(String key, String defValue) {
        return mSp.getString(key, defValue);
    }

    public boolean getData(String key, boolean defValue) {
        return mSp.getBoolean(key, defValue);
    }

    public int getData(String key, int defValue) {
        return mSp.getInt(key, defValue);
    }

    public long getData(String key, long defValue) {
        return mSp.getLong(key, defValue);
    }

    public float getData(String key, float defValue) {
        return mSp.getFloat(key, defValue);
    }

}
