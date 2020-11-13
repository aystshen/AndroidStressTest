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

package com.ayst.stresstest.test.ddr;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.base.BaseTimingTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.util.AppUtils;
import com.ayst.stresstest.util.ShellUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DDRTestFragment extends BaseTimingTestFragment {

    private static final String STRESS_APP_TEST = "stressapptest";
    private static final String ARM32_SUFFIX = "32bit";
    private static final String ARM64_SUFFIX = "64bit";

    private static final int COMMAND_UPDATE_LOG = 1;

    @BindView(R.id.container_content)
    LinearLayout mContentContainer;
    @BindView(R.id.tv_log)
    TextView mLogTv;
    @BindView(R.id.tv_result)
    TextView mResultTv;
    Unbinder unbinder;

    private static boolean isRooted = false;
    private static String sSuffix = ARM32_SUFFIX;
    private static int sMemory = 256;
    private Thread mStressAppTestThread = null;
    private Handler mHandler = new LogHandler(Looper.getMainLooper());

    private class LogHandler extends Handler {
        LogHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COMMAND_UPDATE_LOG:
                    String line = (String) msg.obj;
                    String pretty = line.substring(line.indexOf("(UTC)") + 6);
                    if (pretty.startsWith("Status: PASS")) {
                        mResultTv.setText(pretty);
                        mResultTv.setTextColor(getResources().getColor(R.color.green));
                    } else if (pretty.startsWith("Status: FAIL")) {
                        mResultTv.setText(pretty);
                        mResultTv.setTextColor(getResources().getColor(R.color.red));
                    }
                    if (pretty.contains("Error:")) {
                        pretty = "<font color=\"#ff0000\">" + pretty + "</font>";
                        markFailure();
                    }
                    mLogTv.append(Html.fromHtml(pretty));
                    mLogTv.append("\n");
                    int offset = mLogTv.getLineCount() * mLogTv.getLineHeight();
                    if (offset > mLogTv.getHeight()) {
                        mLogTv.scrollTo(0, offset - mLogTv.getHeight());
                    }
                    break;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ShellUtils.CommandResult result = ShellUtils.execCmd("ls -l", true);
        isRooted = (result.result >= 0 && TextUtils.isEmpty(result.errorMsg));

        if (TextUtils.isEmpty(AppUtils.getProperty(
                "ro.product.cpu.abilist64", ""))) {
            sSuffix = ARM32_SUFFIX;
        } else {
            sSuffix = ARM64_SUFFIX;
        }

        switch (getTotalRam()) {
            case 1:
                sMemory = 128;
                break;
            case 2:
                sMemory = 256;
                break;
            case 3:
                sMemory = 256;
                break;
            case 4:
                sMemory = 512;
                break;
            default:
                sMemory = 256;
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        setTitle(R.string.ddr_test);
        setType(TestType.TYPE_DDR_TEST);

        View contentView = inflater.inflate(R.layout.fragment_ddr_test, container, false);
        setContentView(contentView);

        unbinder = ButterKnife.bind(this, contentView);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    protected void updateImpl() {
        super.updateImpl();

        if (isRunning()) {
            mContentContainer.setVisibility(View.INVISIBLE);
            mLogTv.setVisibility(View.VISIBLE);
        } else {
            mContentContainer.setVisibility(View.VISIBLE);
            mLogTv.setVisibility(View.INVISIBLE);
            if (mResult == Result.POOR || mResult == Result.FAIL) {
                mLogTv.setTextColor(getResources().getColor(R.color.red));
            }
        }
    }

    @Override
    protected void onStartClicked() {
        if (!binExist(STRESS_APP_TEST)) {
            if (!copyBin(STRESS_APP_TEST)) {
                Log.e(TAG, "start, install stressapptest error");
                Toast.makeText(mActivity, R.string.ddr_test_install_error,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
        super.onStartClicked();
    }

    @Override
    public void start() {
        super.start();

        mStressAppTestThread = new Thread(mStressAppTestRunnable);
        mStressAppTestThread.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (mStressAppTestThread != null) {
            mStressAppTestThread.interrupt();
        }
    }

    @Override
    public boolean isSupport() {
        return isRooted;
    }

    private boolean binExist(String name) {
        File bin = new File(AppUtils.getRootDir(mActivity)
                + File.separator + name);
        return bin.exists();
    }

    private boolean copyBin(String name) {
        File desFile = new File(AppUtils.getRootDir(mActivity)
                + File.separator + name);
        if (copyAssetFile("bin/" + name + "_" + sSuffix, desFile)) {
            ShellUtils.CommandResult result1 = ShellUtils.execCmd(
                    "cp " + desFile.getAbsolutePath() + " /data/" + name,
                    true);
            Log.i(TAG, "copyBin, cp result: " + result1.toString());

            ShellUtils.CommandResult result2 = ShellUtils.execCmd(
                    "chmod 777 /data/" + name,
                    true);
            Log.i(TAG, "copyBin, chmod result: " + result2.toString());

            return result1.result >= 0 && TextUtils.isEmpty(result1.errorMsg)
                    && result2.result >= 0 && TextUtils.isEmpty(result2.errorMsg);
        }
        return false;
    }

    private boolean copyAssetFile(String from, File to) {
        if (!TextUtils.isEmpty(from) && to != null) {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = getResources().getAssets().open(from);
                os = new FileOutputStream(to);

                int count;
                byte[] bytes = new byte[1024];
                while ((count = is.read(bytes)) != -1) {
                    os.write(bytes, 0, count);
                }
                return true;
            } catch (IOException e) {
                Log.e(TAG, "copyFile, copy file error: " + e.getMessage());
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "copyFile, close file error: " + e.getMessage());
                }
            }
        }
        return false;
    }

    public int getTotalRam() {
        String path = "/proc/meminfo";
        String firstLine = "";
        int totalRam = 2; // default: 2GB
        try {
            FileReader fileReader = new FileReader(path);
            BufferedReader br = new BufferedReader(fileReader, 8192);
            firstLine = br.readLine().split("\\s+")[1];
            br.close();
        } catch (Exception e) {
            Log.e(TAG, "getTotalRam, error: " + e.getMessage());
        }
        if (!TextUtils.isEmpty(firstLine)) {
            totalRam = (int) Math.ceil((Float.valueOf(Float.parseFloat(firstLine) / (1024 * 1024))
                    .doubleValue()));
        }

        Log.i(TAG, "getTotalRam, total ram: " + totalRam + "GB");

        return totalRam;
    }

    private Runnable mStressAppTestRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "stressapptest run...");

            DataOutputStream dos = null;
            try {
                Process process = Runtime.getRuntime().exec("su");
                dos = new DataOutputStream(process.getOutputStream());

                String command = "/data/" + STRESS_APP_TEST + "  -s " + (mTargetTime - 10)
                        + " -i 4 -C 4 -W --stop_on_errors -M " + sMemory + "\n";
                dos.write(command.getBytes(Charset.forName("utf-8")));
                dos.flush();

                String line;
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                while (isRunning() && (line = bufferedReader.readLine()) != null) {
                    Log.i(TAG, "stressapptest: " + line);
                    Message msg = new Message();
                    msg.what = COMMAND_UPDATE_LOG;
                    msg.obj = line;
                    mHandler.sendMessage(msg);
                }

                dos.writeBytes("exit\n");
                dos.flush();
                Log.w(TAG, "stressapptest exit.");
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                try {
                    if (dos != null) {
                        dos.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    };
}
