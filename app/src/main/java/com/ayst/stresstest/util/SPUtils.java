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

    /**
     * 保存数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public void saveData(String key, String value) {
        Editor editor = mSp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * 保存数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public void saveData(String key, boolean value) {
        Editor editor = mSp.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    /**
     * 保存数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public void saveData(String key, int value) {
        Editor editor = mSp.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * 保存数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public void saveData(String key, float value) {
        Editor editor = mSp.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    /**
     * 取出数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public String getData(String key, String defValue) {
        return mSp.getString(key, defValue);
    }

    /**
     * 取出数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public boolean getData(String key, boolean defValue) {
        return mSp.getBoolean(key, defValue);
    }

    /**
     * 取出数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public int getData(String key, int defValue) {
        return mSp.getInt(key, defValue);
    }

    /**
     * 取出数据
     *
     * @param context
     * @param key
     * @param defValue
     * @return
     */
    public float getData(String key, float defValue) {
        return mSp.getFloat(key, defValue);
    }

}
