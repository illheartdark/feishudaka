package com.daka.assist;

import android.content.Context;
import android.content.SharedPreferences;

/** 打卡编排：影子模式判断 → 无障碍检查 → 交给无障碍服务执行。 */
public final class PunchExecutor {

    public static final String TYPE_ON = "on";
    public static final String TYPE_OFF = "off";

    private PunchExecutor() {
    }

    public static String label(String type) {
        return TYPE_OFF.equals(type) ? "下班卡" : "上班卡";
    }

    /** 正常路径：遵守影子模式开关。 */
    public static void execute(Context c, String type) {
        SharedPreferences sp = c.getSharedPreferences(Constants.PREFS, Context.MODE_PRIVATE);
        if (sp.getBoolean(Constants.KEY_SHADOW, true)) {
            new DayStateStore(c).saveReport("影子模式：本该打" + label(type) + "，未执行");
            Notifier.notify(c, 51, "影子模式：本该打" + label(type),
                    "已判定应打" + label(type) + "，但当前是影子模式，没有实际点击。"
                            + "确认判定准确后，请在首页关闭影子模式。");
            return;
        }
        run(c, type);
    }

    /** 测试路径：忽略影子模式，需要用户明确点击测试按钮。 */
    public static void executeForTest(Context c, String type) {
        run(c, type);
    }

    private static void run(Context c, String type) {
        PunchAccessibilityService svc = PunchAccessibilityService.instance;
        if (svc == null) {
            new DayStateStore(c).saveReport("无障碍服务未开启，自动打" + label(type) + "失败");
            Notifier.notify(c, 52, "自动打" + label(type) + "失败",
                    "无障碍服务未开启。请到「设置 → 辅助功能 → 无障碍 → 打卡助手」开启后重试，本次请手动打卡。");
            return;
        }
        svc.performPunch(c, type);
    }
}
