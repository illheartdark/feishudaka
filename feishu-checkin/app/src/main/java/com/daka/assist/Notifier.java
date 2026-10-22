package com.daka.assist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 通知渠道与各类提醒。 */
public final class Notifier {

    public static final int ID_FOREGROUND = 1;
    public static final int ID_IN_REMIND = 11;
    public static final int ID_OUT_REMIND = 12;
    public static final int ID_RESULT = 21;

    private Notifier() {
    }

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                return;
            }
            NotificationChannel ch = new NotificationChannel(
                    Constants.CHANNEL, "打卡提醒", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("打卡提醒与执行结果");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
    }

    public static Notification foreground(Context c, String text) {
        ensureChannel(c);
        PendingIntent pi = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(c, Constants.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("打卡助手运行中")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    public static void notify(Context c, int id, String title, String text) {
        ensureChannel(c);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        PendingIntent pi = PendingIntent.getActivity(c, id, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(c, Constants.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build();
        nm.notify(id, n);
    }
}
