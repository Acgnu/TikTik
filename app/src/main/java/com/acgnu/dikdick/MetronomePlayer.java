package com.acgnu.dikdick;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.util.Log;

/** Streams continuous mono PCM; AudioTrack provides the playback clock. */
final class MetronomePlayer {
    private static final String TAG = "MetronomePlayer";
    private final Object lock = new Object();
    private volatile int bpm;
    private volatile boolean running;
    private Thread worker;
    private AudioTrack track;

    MetronomePlayer(int bpm) {
        setBpm(bpm);
    }

    void setBpm(int bpm) {
        if (bpm <= 0) {
            throw new IllegalArgumentException("Beat interval must be positive");
        }
        this.bpm = bpm;
    }

    void start() {
        synchronized (lock) {
            if (worker != null) {
                return;
            }
            running = true;
            worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    streamAudio();
                }
            }, "MetronomeAudio");
            worker.start();
        }
    }

    void stop() {
        synchronized (lock) {
            running = false;
            if (track != null) {
                // Pause unblocks a blocking write; flush discards queued beats.
                track.pause();
                track.flush();
            }
        }
        // The worker owns release(), so the UI never waits for audio teardown.
    }

    private void streamAudio() {
        AudioTrack output = null;
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            if (sampleRate <= 0) {
                sampleRate = 48000;
            }
            int minBytes = AudioTrack.getMinBufferSize(sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBytes <= 0) {
                throw new IllegalStateException("Unsupported audio output: " + minBytes);
            }
            // Small blocks keep speed changes responsive without queuing a whole beat.
            short[] block = new short[Math.max(1, sampleRate / 100)];
            int bufferBytes = Math.max(minBytes, block.length * 2 * 2);
            AudioTrack.Builder builder = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufferBytes);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
            }
            output = builder.build();
            if (output.getState() != AudioTrack.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioTrack initialization failed");
            }
            MetronomePcm pcm = new MetronomePcm(sampleRate);
            synchronized (lock) {
                if (!running) {
                    return;
                }
                track = output;
                output.play();
            }
            while (running) {
                pcm.renderBpm(block, bpm);
                int offset = 0;
                while (running && offset < block.length) {
                    int written = output.write(block, offset, block.length - offset,
                            AudioTrack.WRITE_BLOCKING);
                    if (!running) {
                        break;
                    }
                    if (written <= 0) {
                        throw new IllegalStateException("Audio write failed: " + written);
                    }
                    offset += written;
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to play metronome", e);
        } finally {
            synchronized (lock) {
                track = null;
                if (output != null) {
                    output.release();
                }
                running = false;
                worker = null;
            }
        }
    }
}
