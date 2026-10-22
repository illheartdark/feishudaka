package com.daka.assist;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 按「考勤日」保存今日上班卡/下班卡是否已完成。 */
public class DayStateStore {

    private final SharedPreferences sp;

    public DayStateStore(Context c) {
        sp = c.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
    }

    public boolean isDone(String type) {
        return sp.getBoolean(today() + "_" + type, false);
    }

    public void setDone(String type) {
        sp.edit().putBoolean(today() + "_" + type, true).apply();
    }

    public void clearDone(String type) {
        sp.edit().remove(today() + "_" + type).apply();
    }

    public void saveReport(String text) {
        sp.edit().putString(Constants.KEY_LAST_REPORT, text).apply();
    }

    public String lastReport() {
        return sp.getString(Constants.KEY_LAST_REPORT, "无");
    }
}
