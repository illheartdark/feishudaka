package com.daka.assist;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** 精确闹钟：下一个 23:55（当天最后一张下班卡）与下一个 00:00（新一天上班卡）。 */
public final class TriggerScheduler {

    public static final int REQ_OFF = 101;
    public static final int REQ_ON = 102;

    private TriggerScheduler() {
    }

    public static long nextOccurrence(long now, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= now) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }

    public static void scheduleAll(Context c) {
        long now = System.currentTimeMillis();
        set(c, nextOccurrence(now, 23, 55), Constants.ACTION_NIGHT_OFF, REQ_OFF);
        set(c, nextOccurrence(now, 0, 0), Constants.ACTION_NIGHT_ON, REQ_ON);
    }

    private static void set(Context c, long at, String action, int req) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        Intent i = new Intent(c, AlarmReceiver.class).setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
    }
}
