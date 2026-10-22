package com.daka.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** 23:55 与 00:00 的到点执行。 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        SharedPreferences sp = c.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        int state = sp.getInt(Constants.KEY_LAST_STATE, GeofenceEngine.UNKNOWN);
        boolean inRange = state == GeofenceEngine.IN;
        DayStateStore store = new DayStateStore(c);

        if (Constants.ACTION_NIGHT_OFF.equals(action)) {
            if (!inRange) {
                Notifier.notify(c, 41, "23:55 下班卡未自动执行",
                        "当前不在打卡范围内，请手动确认是否已打下班卡。");
            } else if (store.isDone("off")) {
                Notifier.notify(c, 42, "下班卡已完成", "今日下班卡已有记录，跳过自动打卡。");
            } else {
                PunchExecutor.execute(c, "off");
            }
        } else if (Constants.ACTION_NIGHT_ON.equals(action)) {
            if (!inRange) {
                Notifier.notify(c, 43, "00:00 上班卡未自动执行",
                        "当前不在打卡范围内，按约定已跳过（不自动打卡）。");
            } else if (store.isDone("on")) {
                Notifier.notify(c, 44, "上班卡已完成", "新一天上班卡已有记录，跳过自动打卡。");
            } else {
                PunchExecutor.execute(c, "on");
            }
        }
        // 重排下一次闹钟，保证连续运行
        TriggerScheduler.scheduleAll(c);
    }
}
