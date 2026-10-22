package com.daka.assist;

/**
 * 围栏判定：进入 20 m、离开 35 m（滞回），连续 3 次采样才改变状态。
 * 精度差于 15 m 的采样直接丢弃，避免室内漂移造成误判。
 */
public class GeofenceEngine {

    public static final int UNKNOWN = 0;
    public static final int IN = 1;
    public static final int OUT = 2;

    private static final float MAX_ACCURACY = 15f;
    private static final int CONFIRM_COUNT = 3;

    private final float enterRadius;
    private final float exitRadius;

    private int state = UNKNOWN;
    private int inRun = 0;
    private int outRun = 0;

    public GeofenceEngine(float enterRadius, float exitRadius) {
        this.enterRadius = enterRadius;
        this.exitRadius = exitRadius;
    }

    public int getState() {
        return state;
    }

    public int update(float distanceMeters, float accuracyMeters) {
        if (accuracyMeters > MAX_ACCURACY) {
            return state;
        }
        if (distanceMeters <= enterRadius) {
            inRun++;
            outRun = 0;
            if (inRun >= CONFIRM_COUNT) {
                state = IN;
            }
        } else if (distanceMeters >= exitRadius) {
            outRun++;
            inRun = 0;
            if (outRun >= CONFIRM_COUNT) {
                state = OUT;
            }
        } else {
            inRun = 0;
            outRun = 0;
        }
        return state;
    }

    public void reset() {
        state = UNKNOWN;
        inRun = 0;
        outRun = 0;
    }
}
