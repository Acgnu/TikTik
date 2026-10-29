package com.acgnu.tiktik;

/** Single-threaded PCM renderer. Timing is measured in audio frames, not wall time. */
final class MetronomePcm {
    private final int sampleRate;
    private final short[] click;
    private int clickPosition;
    private double intervalMs;
    // One frame is 1000 units, retaining fractional frames at rates such as 44100 Hz.
    private double remainingUnits;
    private boolean started;

    MetronomePcm(int sampleRate) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive");
        }
        this.sampleRate = sampleRate;
        click = new short[Math.max(2, sampleRate * 15 / 1000)];
        for (int i = 0; i < click.length; i++) {
            double time = (double) i / sampleRate;
            double attack = Math.min(1.0, time / 0.001);
            double release = 1.0 - (double) i / (click.length - 1);
            double envelope = attack * release * Math.exp(-time * 250.0);
            click[i] = (short) (Math.sin(2.0 * Math.PI * 1800.0 * time)
                    * envelope * Short.MAX_VALUE * 0.6);
        }
        clickPosition = click.length;
    }

    void renderBpm(short[] output, int bpm) {
        if (bpm <= 0) throw new IllegalArgumentException("BPM must be positive");
        renderInterval(output, 60000.0 / bpm);
    }

    void render(short[] output, int newIntervalMs) {
        renderInterval(output, newIntervalMs);
    }

    private void renderInterval(short[] output, double newIntervalMs) {
        if (newIntervalMs <= 0) {
            throw new IllegalArgumentException("Beat interval must be positive");
        }
        if (started && newIntervalMs != intervalMs) {
            // Anchor to the previous beat; don't emit a burst of overdue beats.
            remainingUnits = Math.max(0L, remainingUnits
                    + (newIntervalMs - intervalMs) * sampleRate);
        }
        intervalMs = newIntervalMs;
        for (int i = 0; i < output.length; i++) {
            if (remainingUnits <= 0) {
                clickPosition = 0;
                remainingUnits += intervalMs * sampleRate;
                started = true;
            }
            output[i] = clickPosition < click.length ? click[clickPosition++] : 0;
            remainingUnits -= 1000;
        }
    }
}
