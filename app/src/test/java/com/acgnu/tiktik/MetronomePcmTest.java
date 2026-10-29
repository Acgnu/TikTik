package com.acgnu.tiktik;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetronomePcmTest {
    @Test
    public void bpmTimingRetainsFractionalIntervalsOverTenMinutes() {
        int rate = 48000;
        int bpm = 140;
        MetronomePcm pcm = new MetronomePcm(rate);
        short[] block = new short[480];
        long position = 0;
        int beat = 0;
        int silence = rate;
        while (position < rate * 600L) {
            pcm.renderBpm(block, bpm);
            for (short sample : block) {
                if (sample != 0) {
                    if (silence > rate / 50) {
                        double expected = beat * (double) rate * 60 / bpm + 1;
                        assertTrue(Math.abs(position - expected) <= 1.001);
                        beat++;
                    }
                    silence = 0;
                } else silence++;
                position++;
            }
        }
        assertEquals(1400, beat);
    }

    @Test
    public void allBeatsHaveIdenticalSoundAndSilenceBetweenThem() {
        short[] samples = new short[48000];
        new MetronomePcm(48000).render(samples, 500);
        boolean audible = false;
        for (int i = 0; i < 24000; i++) {
            assertEquals(samples[i], samples[i + 24000]);
            audible |= samples[i] != 0;
            if (i >= 720) {
                assertEquals(0, samples[i]);
            }
        }
        assertTrue(audible);
    }

    @Test
    public void fractionalFramesDoNotAccumulateDriftOverTenMinutes() {
        int rate = 44100;
        int interval = 241;
        MetronomePcm pcm = new MetronomePcm(rate);
        short[] block = new short[997];
        long position = 0;
        int beat = 0;
        int silentFrames = rate;
        while (position < rate * 600L) {
            pcm.render(block, interval);
            for (short sample : block) {
                if (sample != 0) {
                    if (silentFrames > rate / 50) {
                        // The first sample of the sine click is zero.
                        long expected = (beat * (long) rate * interval + 999) / 1000 + 1;
                        assertEquals(expected, position);
                        beat++;
                    }
                    silentFrames = 0;
                } else {
                    silentFrames++;
                }
                position++;
            }
        }
        assertTrue(beat > 2400);
    }

    @Test
    public void chunkBoundariesDoNotChangeWaveform() {
        short[] expected = new short[100003];
        new MetronomePcm(44100).render(expected, 249);
        MetronomePcm pcm = new MetronomePcm(44100);
        int offset = 0;
        while (offset < expected.length) {
            short[] block = new short[Math.min(317, expected.length - offset)];
            pcm.render(block, 249);
            for (short sample : block) {
                assertEquals(expected[offset++], sample);
            }
        }
    }

    @Test
    public void tempoChangeIsAnchoredToPreviousBeat() {
        MetronomePcm pcm = new MetronomePcm(48000);
        pcm.render(new short[9600], 500); // 200 ms after first beat.
        short[] next = new short[14400];
        pcm.render(next, 300);
        assertEquals(4801, firstNonzero(next)); // Next beat at 300 ms.
    }

    @Test
    public void overdueTempoChangeEmitsOnlyOneBeatThenResumesSpacing() {
        MetronomePcm pcm = new MetronomePcm(48000);
        pcm.render(new short[19200], 900);
        short[] next = new short[24000];
        pcm.render(next, 240);
        assertEquals(1, firstNonzero(next));
        for (int i = 720; i <= 11520; i++) {
            assertEquals(0, next[i]);
        }
        assertTrue(next[11521] != 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroInterval() {
        new MetronomePcm(48000).render(new short[100], 0);
    }

    private int firstNonzero(short[] samples) {
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] != 0) {
                return i;
            }
        }
        return -1;
    }
}
