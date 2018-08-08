package com.ayst.stresstest.util;

/**
 * Created by Habo Shen on 17-11-7.
 */

public class MemOpUtils {

    static {
        System.loadLibrary("MemFillTool");
    }

    public static native int malloc(int size);
    public static native int free();

}
