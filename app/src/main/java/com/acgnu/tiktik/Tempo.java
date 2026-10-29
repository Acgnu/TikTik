package com.acgnu.tiktik;

final class Tempo {
    static final int MIN = 1;
    static final int MAX = 999;
    static final int DEFAULT = 120;

    static int clamp(int bpm) {
        return Math.max(MIN, Math.min(MAX, bpm));
    }

    static float angleDelta(float previous, float current) {
        float delta = (current - previous) % 360f;
        if (delta > 180f) delta -= 360f;
        if (delta < -180f) delta += 360f;
        return delta;
    }
}
