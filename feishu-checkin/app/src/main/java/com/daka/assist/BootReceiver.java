package com.daka.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** 开机或应用更新后恢复监测服务与闹钟。 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        SharedPreferences sp = c.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        TriggerScheduler.scheduleAll(c);
        if (!sp.getBoolean(Constants.KEY_SERVICE_ON, false)) {
            return;
        }
        Intent svc = new Intent(c, TrackService.class);
        try {
            c.startForegroundService(svc);
        } catch (Exception e) {
            c.startService(svc);
        }
    }
}
