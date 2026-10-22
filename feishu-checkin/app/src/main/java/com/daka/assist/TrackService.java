package com.daka.assist;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.Locale;

/** 前台服务：持续定位、围栏判定、进出提醒、夜间任务调度。 */
public class TrackService extends Service implements LocationListener {

    private static final String TAG = "DakaTrack";

    private LocationManager locationManager;
    private GeofenceEngine fence;
    private DayStateStore store;
    private int lastState = GeofenceEngine.UNKNOWN;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new DayStateStore(this);
        SharedPreferences sp = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        float radius = sp.getFloat(Constants.KEY_RADIUS, 20f);
        fence = new GeofenceEngine(radius, radius + 15f);
        lastState = sp.getInt(Constants.KEY_LAST_STATE, GeofenceEngine.UNKNOWN);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 5f, this);
            }
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000L, 10f, this);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "location permission missing", e);
        }

        TriggerScheduler.scheduleAll(this);
        Log.i(TAG, "service created, radius=" + radius);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(Notifier.ID_FOREGROUND, Notifier.foreground(this, "正在监测打卡范围"));
        getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(Constants.KEY_SERVICE_ON, true).apply();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(Constants.KEY_SERVICE_ON, false).apply();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location loc) {
        SharedPreferences sp = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        String latText = sp.getString(Constants.KEY_LAT, null);
        String lngText = sp.getString(Constants.KEY_LNG, null);
        if (latText == null || lngText == null) {
            return;
        }
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(latText);
            lng = Double.parseDouble(lngText);
        } catch (NumberFormatException e) {
            return;
        }

        float[] result = new float[1];
        Location.distanceBetween(lat, lng, loc.getLatitude(), loc.getLongitude(), result);
        float accuracy = loc.hasAccuracy() ? loc.getAccuracy() : 999f;
        int state = fence.update(result[0], accuracy);

        sp.edit()
                .putInt(Constants.KEY_LAST_STATE, state)
                .putString(Constants.KEY_LAST_DISTANCE,
                        String.format(Locale.CHINA, "距离 %.1f 米，精度 %.0f 米", result[0], accuracy))
                .apply();

        if (state != lastState) {
            Log.i(TAG, "state " + lastState + " -> " + state);
            lastState = state;
            if (state == GeofenceEngine.IN) {
                if (!store.isDone("on")) {
                    Notifier.notify(this, Notifier.ID_IN_REMIND, "已到工位",
                            "请打开飞书打上班卡。");
                }
            } else if (state == GeofenceEngine.OUT) {
                if (store.isDone("on") && !store.isDone("off")) {
                    Notifier.notify(this, Notifier.ID_OUT_REMIND, "已离开打卡范围",
                            "如已下班，请打下班卡。");
                }
            }
        }
    }
}
