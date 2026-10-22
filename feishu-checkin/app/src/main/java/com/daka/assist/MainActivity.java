package com.daka.assist;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** 主界面：状态、设工位、启停服务、测试打卡、跳转权限设置。 */
public class MainActivity extends Activity {

    private SharedPreferences sp;
    private TextView statusView;
    private EditText radiusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        Notifier.ensureChannel(this);
        requestPerms();
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void requestPerms() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] perms;
            if (Build.VERSION.SDK_INT >= 33) {
                perms = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.POST_NOTIFICATIONS};
            } else {
                perms = new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
            }
            requestPermissions(perms, 100);
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("打卡助手");
        title.setTextSize(22f);
        box.addView(title);

        statusView = new TextView(this);
        statusView.setTextSize(14f);
        statusView.setPadding(0, dp(12), 0, dp(12));
        box.addView(statusView);

        box.addView(label("打卡半径（米）"));
        radiusView = new EditText(this);
        radiusView.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        radiusView.setText(String.valueOf(sp.getFloat(Constants.KEY_RADIUS, 20f)));
        box.addView(radiusView);

        addButton(box, "保存半径", new Runnable() {
            @Override
            public void run() {
                saveRadius();
            }
        });
        addButton(box, "把当前位置设为工位", new Runnable() {
            @Override
            public void run() {
                useCurrentLocation();
            }
        });
        addButton(box, "开启监测服务", new Runnable() {
            @Override
            public void run() {
                startTrack();
            }
        });
        addButton(box, "停止监测服务", new Runnable() {
            @Override
            public void run() {
                stopService(new Intent(MainActivity.this, TrackService.class));
                sp.edit().putBoolean(Constants.KEY_SERVICE_ON, false).apply();
                toast("已停止");
                refresh();
            }
        });
        addButton(box, "测试：自动打上班卡", new Runnable() {
            @Override
            public void run() {
                toast("测试模式：立即执行，不受影子模式限制");
                PunchExecutor.executeForTest(MainActivity.this, PunchExecutor.TYPE_ON);
            }
        });
        addButton(box, "测试：自动打下班卡", new Runnable() {
            @Override
            public void run() {
                toast("测试模式：立即执行，不受影子模式限制");
                PunchExecutor.executeForTest(MainActivity.this, PunchExecutor.TYPE_OFF);
            }
        });
        addButton(box, "无障碍设置", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                toast("请找到「打卡助手」并开启");
            }
        });
        addButton(box, "应用设置（自启动 / 电池）", new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())));
            }
        });
        addButton(box, "切换影子模式", new Runnable() {
            @Override
            public void run() {
                boolean shadow = !sp.getBoolean(Constants.KEY_SHADOW, true);
                sp.edit().putBoolean(Constants.KEY_SHADOW, shadow).apply();
                toast(shadow ? "影子模式已开启（只记录不点击）" : "影子模式已关闭（会真实点击）");
                refresh();
            }
        });
        addButton(box, "标记今日上班卡已完成", new Runnable() {
            @Override
            public void run() {
                new DayStateStore(MainActivity.this).setDone(PunchExecutor.TYPE_ON);
                toast("已标记");
                refresh();
            }
        });
        addButton(box, "标记今日下班卡已完成", new Runnable() {
            @Override
            public void run() {
                new DayStateStore(MainActivity.this).setDone(PunchExecutor.TYPE_OFF);
                toast("已标记");
                refresh();
            }
        });

        scroll.addView(box);
        return scroll;
    }

    private void saveRadius() {
        String text = radiusView.getText().toString().trim();
        float radius;
        try {
            radius = Float.parseFloat(text);
        } catch (NumberFormatException e) {
            toast("半径必须是数字");
            return;
        }
        if (radius < 5f || radius > 5000f) {
            toast("半径需在 5–5000 米之间");
            return;
        }
        sp.edit().putFloat(Constants.KEY_RADIUS, radius).apply();
        toast("已保存：" + radius + " 米");
        refresh();
    }

    @SuppressWarnings("MissingPermission")
    private void useCurrentLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            toast("定位服务不可用");
            return;
        }
        Location best = null;
        try {
            for (String provider : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (best == null || l.getTime() > best.getTime()) {
                    best = l;
                }
            }
        } catch (SecurityException e) {
            toast("缺少定位权限");
            return;
        }
        if (best == null) {
            toast("还没有定位结果，请先打开定位并等待几秒，或先用手机地图定位一次");
            return;
        }
        sp.edit()
                .putString(Constants.KEY_LAT, String.valueOf(best.getLatitude()))
                .putString(Constants.KEY_LNG, String.valueOf(best.getLongitude()))
                .apply();
        toast("已设为工位：" + String.format(Locale.CHINA, "%.5f, %.5f", best.getLatitude(), best.getLongitude())
                + "（精度 " + Math.round(best.getAccuracy()) + " 米）");
        refresh();
    }

    private void startTrack() {
        Intent i = new Intent(this, TrackService.class);
        try {
            startForegroundService(i);
        } catch (Exception e) {
            startService(i);
        }
        toast("监测已开启，建议在最近任务里加锁，防止被系统清理");
        refresh();
    }

    private void refresh() {
        if (statusView == null) {
            return;
        }
        String lat = sp.getString(Constants.KEY_LAT, null);
        String lng = sp.getString(Constants.KEY_LNG, null);
        boolean running = sp.getBoolean(Constants.KEY_SERVICE_ON, false);
        boolean shadow = sp.getBoolean(Constants.KEY_SHADOW, true);
        int state = sp.getInt(Constants.KEY_LAST_STATE, GeofenceEngine.UNKNOWN);
        String distance = sp.getString(Constants.KEY_LAST_DISTANCE, "暂无");

        String stateText;
        if (state == GeofenceEngine.IN) {
            stateText = "在范围内";
        } else if (state == GeofenceEngine.OUT) {
            stateText = "在范围外";
        } else {
            stateText = "未知（等待定位）";
        }

        DayStateStore store = new DayStateStore(this);
        StringBuilder sb = new StringBuilder();
        sb.append("服务：").append(running ? "运行中" : "已停止").append('\n');
        sb.append("工位：").append(lat == null || lng == null ? "未设置（点下方按钮设置）"
                : String.format(Locale.CHINA, "%s, %s", lat, lng)).append('\n');
        sb.append("半径：").append(sp.getFloat(Constants.KEY_RADIUS, 20f)).append(" 米\n");
        sb.append("围栏：").append(stateText).append('\n');
        sb.append("最近测距：").append(distance).append('\n');
        sb.append("今日上班卡：").append(store.isDone("on") ? "已完成" : "未完成").append('\n');
        sb.append("今日下班卡：").append(store.isDone("off") ? "已完成" : "未完成").append('\n');
        sb.append("影子模式：").append(shadow ? "开启（只记录不点击）" : "关闭（会真实点击）").append('\n');
        sb.append("无障碍：").append(isAccessibilityOn() ? "已开启" : "未开启").append('\n');
        sb.append("最近结果：").append(store.lastReport());
        statusView.setText(sb.toString());
    }

    private boolean isAccessibilityOn() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) {
            return false;
        }
        String me = getPackageName() + "/" + PunchAccessibilityService.class.getName();
        return enabled.contains(me);
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setPadding(0, dp(12), 0, 0);
        return v;
    }

    private void addButton(LinearLayout parent, String text, final Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        parent.addView(b);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
