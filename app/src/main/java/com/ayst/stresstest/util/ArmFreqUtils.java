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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class ArmFreqUtils {
    private static final String TAG = "ArmFreqUtils";

    public static final String USERSPACE_MODE = "userspace";
    public static final String INTERACTIVE_MODE = "interactive";
    public static final int GPU_AVAILABLE_FREQ_COUNT = 6;

    private static File sCpuFreqsFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies");
    private static File sCpuCurFreqFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
    private static File sCpuMaxFreqFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
    private static File sCpuMinFreqFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
    private static File sCpuGovernorFreqFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
    private static File sCpuSetFreqFile = new File(
            "/sys/devices/system/cpu/cpu0/cpufreq/scaling_setspeed");

    private static File sGpuFreqsFile = new File("/sys/mali400_utility/param");
    private static File sGpuFreqFile_3188 = new File("/sys/mali400_utility/utility");
    private static File sGpuFreqFile = new File("/proc/pvr/freq");

    private static File sDdrFreqFile = new File("/proc/driver/ddr_ts");

    public static void setDDRFreq(String value) throws IOException {
        WriteFile(sDdrFreqFile, value);
    }

    public static void openGpuEcho() throws IOException {
        WriteFile(sGpuFreqFile, "debug_lo");
    }

    public static void setGpuFreq(String value) throws IOException {
        WriteFile(sGpuFreqFile, value);
    }

    public static void setGpuFreqFor3188(String value) throws IOException {
        WriteFile(sGpuFreqFile_3188, value);
    }

    public static void setCpuGovernorMode(String mode) throws IOException {
        WriteFile(sCpuGovernorFreqFile, mode);
    }

    public static void setCpuFreq(int value) throws IOException {
        WriteFile(sCpuSetFreqFile, String.valueOf(value));
    }

    public static List<String> getCpuAvailableFreqs() {
        List<String> result = new ArrayList<String>();
        if (sCpuFreqsFile.exists()) {
            try {
                String str = ReadFile(sCpuFreqsFile);
                String freqs[] = str.split(" ");
                if (freqs.length > 0) {
                    for (String freq : freqs) {
                        result.add(Integer.valueOf(freq) / 1000 + "M");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (result.isEmpty()) {
            if (sCpuMinFreqFile.exists()) {
                try {
                    String freq = ReadFile(sCpuMinFreqFile);
                    result.add(Integer.valueOf(freq) / 1000 + "M");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sCpuMaxFreqFile.exists()) {
                try {
                    String freq = ReadFile(sCpuMaxFreqFile);
                    result.add(Integer.valueOf(freq) / 1000 + "M");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public static Integer getCpuCurFreq() {
        try {
            return Integer.valueOf(ReadFile(sCpuCurFreqFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static int getDDRCurFreq() {
        try {
            return Integer.valueOf(ReadFile(sDdrFreqFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static List<String> getGpuAvailableFreqs() {
        List<String> result = new ArrayList<String>();
        if (sGpuFreqsFile.exists()) {
            try {
                String str = ReadFile(sGpuFreqsFile);
                String freqs[] = str.split(",");
                if (freqs.length > 0) {
                    int count = Integer.valueOf(freqs[0]);
                    for (int i = 0; i < count; i++) {
                        result.add(Integer.valueOf(freqs[i + 1]) + "");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static int getNumberOfCPUCores() {
        int cores;
        try {
            cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
        } catch (SecurityException | NullPointerException e) {
            cores = 1;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    private static boolean WriteFile(File file, String message)
            throws IOException {

        if (!file.exists()) {
            throw new IOException();
        }

        if (file.canWrite()) {
            FileOutputStream fWrite = new FileOutputStream(file);
            byte[] bytes = message.getBytes();
            fWrite.write(bytes);
            fWrite.close();
        } else {
            Log.e(TAG, file.toString() + " can't write");
            throw new IOException();
        }
        return true;

    }

    private static String ReadFile(File file)
            throws IOException {

        if (!file.exists()) {
            throw new IOException();
        }

        try {
            FileReader fRead = new FileReader(file);
            BufferedReader buffer = new BufferedReader(fRead);
            StringBuilder sb = new StringBuilder();
            String str;
            while ((str = buffer.readLine()) != null) {
                sb.append(str);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, file.toString() + " can't read");
            throw new IOException();
        }
    }
}
