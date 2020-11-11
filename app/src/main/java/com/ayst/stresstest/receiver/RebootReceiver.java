
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

package com.ayst.stresstest.receiver;

import com.ayst.stresstest.service.WorkService;
import com.ayst.stresstest.test.base.BaseTestFragment;
import com.ayst.stresstest.test.reboot.RebootTestFragment;
import com.ayst.stresstest.test.timingboot.TimingBootTestFragment;
import com.ayst.stresstest.ui.MainActivity;
import com.ayst.stresstest.util.SPUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootReceiver extends BroadcastReceiver {
    private static final String TAG = "RebootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            int rebootFlag = SPUtils.getInstance(context).getData(RebootTestFragment.SP_REBOOT_FLAG, BaseTestFragment.State.STOP);
            int timingBootFlag = SPUtils.getInstance(context).getData(TimingBootTestFragment.SP_TIMING_BOOT_FLAG, BaseTestFragment.State.STOP);
            Log.d(TAG, "RebootReceiver, rebootFlag:" + rebootFlag);
            if (rebootFlag == BaseTestFragment.State.RUNNING
                    || timingBootFlag == BaseTestFragment.State.RUNNING) {
                Intent intent1 = new Intent(context, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } else {
                Intent serviceIntent = new Intent(context, WorkService.class);
                serviceIntent.putExtra("command", WorkService.COMMAND_CHECK_RECOVERY_STATE);
                serviceIntent.putExtra("delay", 5000);
                //AppUtils.startService(context, serviceIntent);
                context.startService(intent);
            }
        }
    }

}
