package com.example.letsdoit;

import static com.example.letsdoit.duplicate_main.findDuplicates;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileActionService extends Service {
    public static final String ACTION_SCAN       = "com.example.letsdoit.ACTION_SCAN";
    public static final String ACTION_TYPE       = "action_type";
    public static final String ACTION_SCAN_COMPLETE = "com.example.letsdoit.SCAN_COMPLETE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getStringExtra(ACTION_TYPE);

        // Build a simple notification
        Notification notification = new NotificationCompat.Builder(this, "fileActionChannel")
                .setContentTitle("Duplicate-Scan")
                .setContentText("Scanning for duplicates…")
                .setSmallIcon(R.drawable.pdf1)
                .build();
        startForeground(1, notification);

        new Thread(() -> {
            if (ACTION_SCAN.equals(action)) {
                try {
                    // 1. Run your duplicate-finder
                    File root = new File("/storage/emulated/0/");
                    List<List<File>> groups = findDuplicates(root);

                    // 2. Flatten to a list of paths
                    ArrayList<String> paths = new ArrayList<>();
                    for (List<File> grp : groups) {
                        // Skip the first file, keep the rest
                        for (int i = 1; i < grp.size(); i++) {
                            paths.add(grp.get(i).getAbsolutePath());
                        }
                    }

                    // 3. Broadcast completion
                    Intent done = new Intent(ACTION_SCAN_COMPLETE);
                    done.putStringArrayListExtra("duplicate_paths", paths);
                    sendBroadcast(done);

                } catch (IOException | NoSuchAlgorithmException |
                         ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }

            stopForeground(true);
            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    "fileActionChannel",
                    "Duplicate Scan Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
    }
}
