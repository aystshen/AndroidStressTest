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

package com.ayst.stresstest.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ayst.stresstest.R;
import com.ayst.stresstest.test.airplanemode.AirplaneModeTestFragment;
import com.ayst.stresstest.test.audio.AudioTestFragment;
import com.ayst.stresstest.test.bluetooth.BluetoothTestFragment;
import com.ayst.stresstest.test.cpu.CPUTestFragment;
import com.ayst.stresstest.test.camera.CameraTestFragment;
import com.ayst.stresstest.test.ddr.DDRTestFragment;
import com.ayst.stresstest.test.memory.MemoryTestFragment;
import com.ayst.stresstest.test.network.NetworkTestFragment;
import com.ayst.stresstest.test.reboot.RebootTestFragment;
import com.ayst.stresstest.test.recovery.RecoveryTestFragment;
import com.ayst.stresstest.test.sleep.SleepTestFragment;
import com.ayst.stresstest.test.timingboot.TimingBootTestFragment;
import com.ayst.stresstest.test.video.VideoTestFragment;
import com.ayst.stresstest.test.wifi.WifiTestFragment;
import com.ayst.stresstest.test.base.BaseTestFragment;
import com.ayst.stresstest.test.base.TestType;
import com.ayst.stresstest.test.uvccamera.UVCCameraTestFragment;
import com.ayst.stresstest.util.AppUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import kr.co.namee.permissiongen.PermissionGen;

public class MainActivity extends AppCompatActivity implements BaseTestFragment.OnFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    @BindView(R.id.tv_intro)
    TextView mAppIntro;

    private FragmentManager mFragmentManager;

    private ArrayList<BaseTestFragment> mTestFragments = new ArrayList<>();
    private int mContainerIds[] = {R.id.container1, R.id.container2, R.id.container3,
            R.id.container4, R.id.container5, R.id.container6,
            R.id.container7, R.id.container8, R.id.container9,
            R.id.container10, R.id.container11, R.id.container12,
            R.id.container13, R.id.container14, R.id.container15};
    private ArrayList<TestType[]> mMutexTests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mFragmentManager = getFragmentManager();

        initView();

        PermissionGen.with(MainActivity.this)
                .addRequestCode(100)
                .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA)
                .request();
    }

    private void initView() {
        mAppIntro.setText(String.format(getString(R.string.app_intro),
                AppUtils.getVersionName(this)));

        mTestFragments.add(TestType.TYPE_CPU_TEST.ordinal(), new CPUTestFragment());
        mTestFragments.add(TestType.TYPE_MEMORY_TEST.ordinal(), new MemoryTestFragment());
        mTestFragments.add(TestType.TYPE_DDR_TEST.ordinal(), new DDRTestFragment());
        mTestFragments.add(TestType.TYPE_VIDEO_TEST.ordinal(), new VideoTestFragment());
        mTestFragments.add(TestType.TYPE_AUDIO_TEST.ordinal(), new AudioTestFragment());
        mTestFragments.add(TestType.TYPE_WIFI_TEST.ordinal(), new WifiTestFragment());
        mTestFragments.add(TestType.TYPE_BT_TEST.ordinal(), new BluetoothTestFragment());
        mTestFragments.add(TestType.TYPE_AIRPLANE_MODE_TEST.ordinal(), new AirplaneModeTestFragment());
        mTestFragments.add(TestType.TYPE_REBOOT_TEST.ordinal(), new RebootTestFragment());
        mTestFragments.add(TestType.TYPE_SLEEP_TEST.ordinal(), new SleepTestFragment());
        mTestFragments.add(TestType.TYPE_RECOVERY_TEST.ordinal(), new RecoveryTestFragment());
        mTestFragments.add(TestType.TYPE_TIMING_BOOT_TEST.ordinal(), new TimingBootTestFragment());
        mTestFragments.add(TestType.TYPE_NETWORK_TEST.ordinal(), new NetworkTestFragment());
        mTestFragments.add(TestType.TYPE_CAMERA_TEST.ordinal(), new CameraTestFragment());
        mTestFragments.add(TestType.TYPE_UVCCAMERA_TEST.ordinal(), new UVCCameraTestFragment());

        for (int i = 0; i < mTestFragments.size(); i++) {
            mFragmentManager.beginTransaction().add(mContainerIds[i], mTestFragments.get(i)).commit();
        }

        // 建立互斥表
        mMutexTests.add(TestType.TYPE_CPU_TEST.ordinal(), createRebootMutex(null));
        mMutexTests.add(TestType.TYPE_MEMORY_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_DDR_TEST}));
        mMutexTests.add(TestType.TYPE_DDR_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_MEMORY_TEST}));
        mMutexTests.add(TestType.TYPE_VIDEO_TEST.ordinal(), createRebootMutex(null));
        mMutexTests.add(TestType.TYPE_AUDIO_TEST.ordinal(), createRebootMutex(null));
        mMutexTests.add(TestType.TYPE_WIFI_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_AIRPLANE_MODE_TEST}));
        mMutexTests.add(TestType.TYPE_BT_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_AIRPLANE_MODE_TEST}));
        mMutexTests.add(TestType.TYPE_AIRPLANE_MODE_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_WIFI_TEST, TestType.TYPE_BT_TEST}));
        mMutexTests.add(TestType.TYPE_REBOOT_TEST.ordinal(), createSingleMutex(TestType.TYPE_REBOOT_TEST));
        mMutexTests.add(TestType.TYPE_SLEEP_TEST.ordinal(), createSingleMutex(TestType.TYPE_SLEEP_TEST));
        mMutexTests.add(TestType.TYPE_RECOVERY_TEST.ordinal(), createSingleMutex(TestType.TYPE_RECOVERY_TEST));
        mMutexTests.add(TestType.TYPE_TIMING_BOOT_TEST.ordinal(), createSingleMutex(TestType.TYPE_TIMING_BOOT_TEST));
        mMutexTests.add(TestType.TYPE_NETWORK_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_AIRPLANE_MODE_TEST, TestType.TYPE_WIFI_TEST}));
        mMutexTests.add(TestType.TYPE_CAMERA_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_UVCCAMERA_TEST}));
        mMutexTests.add(TestType.TYPE_UVCCAMERA_TEST.ordinal(), createRebootMutex(new TestType[]{TestType.TYPE_CAMERA_TEST}));
    }

    @Override
    protected void onStart() {
        super.onStart();

        for (int i = 0; i < mTestFragments.size(); i++) {
            if (mTestFragments.get(i).isRunning()) {
                TestType[] mutexTests = mMutexTests.get(i);
                for (TestType mutexTest : mutexTests) {
                    mTestFragments.get(mutexTest.ordinal()).setAvailable(false);
                }
            }
        }
    }

    @Override
    public void onFragmentInteraction(TestType testType, int state) {
        Log.d(TAG, "onFragmentInteraction, testType=" + testType + ", state=" + state);
        if (BaseTestFragment.State.RUNNING == state) {
            TestType[] mutexTests = mMutexTests.get(testType.ordinal());
            for (TestType mutexTest : mutexTests) {
                mTestFragments.get(mutexTest.ordinal()).setAvailable(false);
            }
        } else {
            for (int i = 0; i < mTestFragments.size(); i++) {
                TestType[] mutexTests = mMutexTests.get(i);
                int j;
                for (j = 0; j < mutexTests.length; j++) {
                    if (mTestFragments.get(mutexTests[j].ordinal()).isRunning()) {
                        break;
                    }
                }
                if (j == mutexTests.length) {
                    mTestFragments.get(i).setAvailable(true);
                }
            }
        }
    }

    private boolean isHaveRunningTest() {
        for (int i = 0; i < mTestFragments.size(); i++) {
            if (mTestFragments.get(i).isRunning()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (isHaveRunningTest()) {
            showFinishDialogWithRunning();
        } else {
            showFinishDialog();
        }
    }

    public void showFinishDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit)
                .setMessage(R.string.exit_test_tips)
                .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    public void showFinishDialogWithRunning() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.exit_app_tips)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * Create a table that can only be run as a singleton.
     *
     * @param self Self test type
     * @return Mutex table
     */
    private TestType[] createSingleMutex(TestType self) {
        int index = 0;
        TestType[] allValues = TestType.values();
        TestType[] newValues = new TestType[allValues.length - 1];

        for (TestType value : allValues) {
            if (value != self) {
                newValues[index++] = value;
            }
        }

        return newValues;
    }

    /**
     * Create a table containing reboot the system.
     *
     * @param sub Attached table
     * @return Mutex table
     */
    private TestType[] createRebootMutex(TestType[] sub) {
        TestType[] baseValues = new TestType[]{TestType.TYPE_REBOOT_TEST, TestType.TYPE_SLEEP_TEST,
                TestType.TYPE_RECOVERY_TEST, TestType.TYPE_TIMING_BOOT_TEST};

        if (sub != null && sub.length > 0) {
            TestType[] newValues = new TestType[baseValues.length + sub.length];
            System.arraycopy(baseValues, 0, newValues, 0, baseValues.length);
            System.arraycopy(sub, 0, newValues, baseValues.length, sub.length);
            return newValues;
        }

        return baseValues;
    }
}
