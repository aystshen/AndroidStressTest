package com.ayst.stresstest.receiver;

import com.ayst.stresstest.service.WorkService;
import com.ayst.stresstest.test.BaseTestFragment;
import com.ayst.stresstest.test.RebootTestFragment;
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
            int rebootFlag = SPUtils.getInstance(context).getData(RebootTestFragment.SP_REBOOT_FLAG, BaseTestFragment.STATE_STOP);
            Log.d(TAG, "RebootReceiver, rebootFlag:" + rebootFlag);
            if (rebootFlag == BaseTestFragment.STATE_RUNNING) {
                Intent intent1 = new Intent(context, MainActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } else {
                Intent serviceIntent = new Intent(context, WorkService.class);
                serviceIntent.putExtra("command", WorkService.COMMAND_CHECK_RECOVERY_STATE);
                serviceIntent.putExtra("delay", 5000);
                context.startService(serviceIntent);
            }
        }
    }

}
