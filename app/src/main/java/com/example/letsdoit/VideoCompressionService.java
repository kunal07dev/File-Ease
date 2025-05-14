package com.example.letsdoit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.arthenica.ffmpegkit.FFmpegKit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class VideoCompressionService extends Service {

    private static final String CHANNEL_ID = "VideoCompressionChannel";
    private static final int NOTIFICATION_ID = 101;

    public static final String ACTION_START_COMPRESSION = "START_COMPRESSION";
    public static final String ACTION_STOP_COMPRESSION = "STOP_COMPRESSION";
    public static final String EXTRA_VIDEO_URIS = "video_uris";
    public static final String EXTRA_REPLACE_ORIGINAL = "replace_original";

    private final Queue<CompressionTask> taskQueue = new LinkedList<>();
    private boolean isRunning = false;
    private boolean isStopped = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void startCompression(Context context, List<Uri> uris, boolean replaceOriginal) {
        Intent intent = new Intent(context, VideoCompressionService.class);
        intent.setAction(ACTION_START_COMPRESSION);
        intent.putParcelableArrayListExtra(EXTRA_VIDEO_URIS, new ArrayList<>(uris));
        intent.putExtra(EXTRA_REPLACE_ORIGINAL, replaceOriginal);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        MainActivity obj=new MainActivity();


    }

    public static void stopCompression(Context context) {
        Intent intent = new Intent(context, VideoCompressionService.class);
        intent.setAction(ACTION_STOP_COMPRESSION);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (ACTION_STOP_COMPRESSION.equals(intent.getAction())) {
                isStopped = true;
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_START_COMPRESSION.equals(intent.getAction())) {
                List<Uri> uris = intent.getParcelableArrayListExtra(EXTRA_VIDEO_URIS);
                boolean replaceOriginal = intent.getBooleanExtra(EXTRA_REPLACE_ORIGINAL, false);
                if (uris != null) {
                    for (Uri uri : uris) {
                        taskQueue.add(new CompressionTask(uri, replaceOriginal));
                    }
                    if (!isRunning) {
                        isRunning = true;
                        startForeground(NOTIFICATION_ID, buildNotification("Starting compression..."));
                        processNextTask();
                    }
                }
            }
        }
        return START_STICKY;
    }

    private void processNextTask() {
        if (isStopped || taskQueue.isEmpty()) {
            stopForeground(true);
            stopSelf();
            return;
        }

        CompressionTask task = taskQueue.poll();
        if (task == null) return;

        updateNotification("Compressing video...");

        new Thread(() -> {
            try {
                Uri uri = task.uri;
                boolean replaceOriginal = task.replaceOriginal;

                File inputFile = FileUtils.copyToInternal(this, uri);
                if (inputFile == null) {
                    Log.e("CompressionService", "Failed to copy input file");
                    processNextTask();
                    return;
                }

                File outputFile = new File(getCacheDir(), "compressed_" + UUID.randomUUID() + ".mp4");
                String outputPath = outputFile.getAbsolutePath();

                String cmd = "-y -i \"" + inputFile.getAbsolutePath() + "\" " +
                        "-c:v libvpx-vp9 -crf 32 -b:v 0 -c:a libopus \"" + outputPath + "\"";

                FFmpegKit.execute(cmd);

                if (replaceOriginal) {
                    FileUtils.replaceOriginalFile(this, uri, outputPath);
                    Log.i("CompressionService", "Replaced original video: " + uri);
                } else {
                    FileUtils.saveToMediaStore(this, outputPath);
                    Log.i("CompressionService", "Saved compressed video to gallery");
                }


            } catch (Exception e) {
                Log.e("CompressionService", "Compression failed", e);
            }

            processNextTask();
        }).start();
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Video Compression")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Video Compression", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private static class CompressionTask {
        Uri uri;
        boolean replaceOriginal;

        CompressionTask(Uri uri, boolean replaceOriginal) {
            this.uri = uri;
            this.replaceOriginal = replaceOriginal;
        }
    }

    public static class FileUtils {

        public static File copyToInternal(Context context, Uri uri) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                File outFile = new File(context.getCacheDir(), "temp_" + System.currentTimeMillis() + ".mp4");
                OutputStream outputStream = new FileOutputStream(outFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.close();
                inputStream.close();
                return outFile;
            } catch (Exception e) {
                Log.e("FileUtils", "copyToInternal failed", e);
                return null;
            }
        }

        public static void replaceOriginalFile(Context context, Uri originalUri, String compressedPath) {
            try {
                InputStream inputStream = new FileInputStream(compressedPath);
                OutputStream outputStream = context.getContentResolver().openOutputStream(originalUri, "wt");
                if (outputStream == null) throw new IOException("OutputStream is null");

                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
                inputStream.close();
                outputStream.close();

                // 🔁 Get the original file's last modified time
                String originalPath = getRealPathFromUri(context, originalUri);
                if (originalPath != null) {
                    File originalFile = new File(originalPath);
                    long lastModified = originalFile.lastModified();

                    // Set compressed file's last modified to match original
                    File compressedFile = new File(compressedPath);
                    boolean success = compressedFile.setLastModified(lastModified);
                    Log.i("FileReplace", "SetLastModified success: " + success);
                }

                // Optional: scan to update MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                context.getContentResolver().update(originalUri, values, null, null);

                // Scan both original and compressed paths
                String originalPat = getRealPathFromUri(context, originalUri);
                MediaScannerConnection.scanFile(context,
                        new String[]{originalPat, compressedPath},
                        null,
                        null
                );


            } catch (Exception e) {
                Log.e("FileReplace", "Failed to replace original file", e);
            }
        }


        public static void saveToMediaStore(Context context, String compressedPath) {
            try {
                File sourceFile = new File(compressedPath);
                String fileName = "compressed_" + System.currentTimeMillis() + ".mp4";

                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CompressedVideos");
                values.put(MediaStore.Video.Media.IS_PENDING, 1);

                ContentResolver resolver = context.getContentResolver();
                Uri videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                if (videoUri != null) {
                    try (OutputStream out = resolver.openOutputStream(videoUri);
                         InputStream in = new FileInputStream(sourceFile)) {

                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }

                        values.clear();
                        values.put(MediaStore.Video.Media.IS_PENDING, 0);
                        resolver.update(videoUri, values, null, null);

                        Log.i("MediaStore", "Video saved to gallery: " + videoUri.toString());
                    }
                }
            } catch (Exception e) {
                Log.e("MediaStore", "Failed to save to MediaStore", e);
            }
        }

    }
    @Nullable
    public static String getRealPathFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Video.Media.DATA};
            cursor = context.getContentResolver().query(uri, proj, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e("FileUtils", "getRealPathFromUri failed", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

}
