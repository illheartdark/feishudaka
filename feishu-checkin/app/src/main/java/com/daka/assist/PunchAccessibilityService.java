package com.daka.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Locale;

/**
 * 无障碍服务：打开飞书并点击「打卡」控件，然后读取页面结果。
 * 说明：控件选择器做了容错（文本包含「打卡」且排除记录/设置等），但飞书改版后仍可能失效。
 */
public class PunchAccessibilityService extends AccessibilityService {

    private static final String TAG = "DakaA11y";
    private static final long TIMEOUT_MS = 30000L;
    private static final long POLL_MS = 800L;

    public static PunchAccessibilityService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Context appContext;
    private String pendingType;
    private long deadline;
    private boolean clicked;
    private int scanCount;

    private final Runnable scanTask = new Runnable() {
        @Override
        public void run() {
            scan();
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        appContext = getApplicationContext();
        Log.i(TAG, "accessibility service connected");
    }

    @Override
    public void onDestroy() {
        instance = null;
        handler.removeCallbacks(scanTask);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 轮询为主，事件仅用于唤醒一次扫描
        if (pendingType != null && !clicked) {
            handler.removeCallbacks(scanTask);
            handler.postDelayed(scanTask, 400L);
        }
    }

    @Override
    public void onInterrupt() {
        // 不需要处理
    }

    /** 入口：由 PunchExecutor 调用。 */
    public void performPunch(Context c, String type) {
        if (pendingType != null) {
            Log.w(TAG, "already running for " + pendingType);
            return;
        }
        pendingType = type;
        clicked = false;
        scanCount = 0;
        deadline = System.currentTimeMillis() + TIMEOUT_MS;

        Intent launch = c.getPackageManager().getLaunchIntentForPackage(Constants.FEISHU_PKG);
        if (launch == null) {
            finish(false, "未安装飞书，无法自动打卡");
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            c.startActivity(launch);
        } catch (Exception e) {
            finish(false, "拉起飞书失败：" + e.getMessage());
            return;
        }
        handler.postDelayed(scanTask, 1500L);
    }

    private void scan() {
        if (pendingType == null) {
            return;
        }
        if (System.currentTimeMillis() > deadline) {
            finish(false, "超时未完成打卡，请手动处理");
            return;
        }
        scanCount++;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            handler.postDelayed(scanTask, POLL_MS);
            return;
        }

        if (clicked) {
            boolean ok = findText(root, "打卡成功") || findText(root, "已打卡") || findText(root, "更新打卡");
            if (ok) {
                finish(true, null);
                return;
            }
        } else {
            AccessibilityNodeInfo button = findPunchButton(root);
            if (button != null && clickNode(button)) {
                clicked = true;
                Log.i(TAG, "punch button clicked, attempt=" + scanCount);
            }
        }
        handler.postDelayed(scanTask, POLL_MS);
    }

    private void finish(boolean success, String message) {
        String type = pendingType;
        pendingType = null;
        handler.removeCallbacks(scanTask);

        if (appContext == null) {
            appContext = getApplicationContext();
        }
        if (appContext == null) {
            return;
        }

        String label = PunchExecutor.label(type);
        String time = new java.text.SimpleDateFormat("HH:mm", Locale.CHINA)
                .format(new java.util.Date());

        DayStateStore store = new DayStateStore(appContext);
        if (success) {
            store.setDone(type);
            store.saveReport("自动打" + label + "成功 " + time);
            Notifier.notify(appContext, Notifier.ID_RESULT, "自动打" + label + "成功",
                    "已于 " + time + " 完成" + label + "。");
            // 返回上一层，尽量减少对用户的影响
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
            }, 1200L);
        } else {
            store.saveReport("自动打" + label + "失败：" + message);
            Notifier.notify(appContext, Notifier.ID_RESULT + 1, "自动打" + label + "失败",
                    message + "。请手动打开飞书完成打卡。");
        }
    }

    private static boolean clickNode(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = node;
        int depth = 0;
        while (cur != null && depth < 4) {
            if (cur.isClickable() && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            cur = cur.getParent();
            depth++;
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private static AccessibilityNodeInfo findPunchButton(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<AccessibilityNodeInfo>();
        queue.add(root);
        AccessibilityNodeInfo fallback = null;

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) {
                continue;
            }
            String text = textOf(n);
            if (text.contains("打卡") && !text.contains("记录") && !text.contains("设置")
                    && !text.contains("统计") && !text.contains("补卡") && !text.contains("规则")
                    && !text.contains("帮助") && !text.contains("详情")) {
                if (n.isClickable()) {
                    return n;
                }
                if (fallback == null) {
                    fallback = n;
                }
            }
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return fallback;
    }

    private static boolean findText(AccessibilityNodeInfo root, String needle) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<AccessibilityNodeInfo>();
        queue.add(root);
        while (!queue.isEmpty()) {
            AccessibilityNodeInfo n = queue.poll();
            if (n == null) {
                continue;
            }
            if (textOf(n).contains(needle)) {
                return true;
            }
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return false;
    }

    private static String textOf(AccessibilityNodeInfo n) {
        String a = n.getText() == null ? "" : n.getText().toString();
        String b = n.getContentDescription() == null ? "" : n.getContentDescription().toString();
        return a + "|" + b;
    }
}
