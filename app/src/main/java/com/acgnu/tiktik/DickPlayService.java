package com.acgnu.tiktik;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DickPlayService extends Service {
    public static volatile boolean isPlaying;
    public static volatile int currentBpm = Tempo.DEFAULT;
    private MetronomePlayer player;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        int bpm = intent.getIntExtra("bpm", Tempo.DEFAULT);
        if (bpm < Tempo.MIN || bpm > Tempo.MAX) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (player == null) {
            player = new MetronomePlayer(bpm);
        }
        player.setBpm(bpm);
        currentBpm = bpm;
        isPlaying = true;
        player.start();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        isPlaying = false;
        if (player != null) {
            player.stop();
            player = null;
        }
        super.onDestroy();
    }
}
