package org.nodpi.hello.service;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.Manifest;

import org.nodpi.hello.MainActivity;
import org.nodpi.hello.R;

import dpi.DPIS;

public class DpiService extends Service {

    private final int notificationIcon = android.R.color.transparent;
    private final String notificationChannelName = "NotificationChannelName";
    private final String notificationChannelId = "NotificationChannelId";

    private final long TIMER_TASK_PERIOD = 30;

    private PowerManager.WakeLock wakeLock;
    private Thread myThread;
    private Timer timer = new Timer();

    private BroadcastReceiver updateNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            updateNotification(message);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        createNotificationChannel();
        startForeground(2765, getNotification());
        IntentFilter filter = new IntentFilter("UPDATE_NOTIFICATION");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(updateNotificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(2765, getNotification());
        checkImportantThread();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        setRestartAlarmAndWork();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        setRestartAlarmAndWork();
        super.onDestroy();
        releaseWakeLock();
        unregisterReceiver(updateNotificationReceiver);
    }

    private void checkImportantThread() {
        claimWakeLock();
        launchTimerTask();

        if (myThread == null) {
            startImportantThread();
        } else if (!myThread.isAlive()) {
            restartMyThread();
        }

    }

    private void updateNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId);
        String appName = getString(R.string.app_name);
        builder.setOngoing(true)
                .setSmallIcon(notificationIcon)
                .setContentTitle(appName)
                .setContentText(message)
                .setContentIntent(getContentIntent());
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(2765, builder.build());
            } else {
                Log.d("Notification", "POST_NOTIFICATIONS permission not granted");
            }
        } else {
            notificationManager.notify(2765, builder.build());
        }
    }

    private void restartVPN() {
        SharedPreferences sharedPreferences = this.getSharedPreferences("app_state", Context.MODE_PRIVATE);
        String status = sharedPreferences.getString("status", "inactive");
        Log.d("VPNServiceStatus:", status);
        if (status.equals("active")) {
            Intent vpnIntent = new Intent(this, DpiVpnService.class);
            if (!isServiceRunning(vpnIntent)) {
                Log.d("VPNService", "Service not running running");
                startService(vpnIntent);
            } else {
                Log.d("VPNService", "Already running");
            }
        }
    }

    private boolean isServiceRunning(Intent serviceIntent) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().equals(Objects.requireNonNull(serviceIntent.getComponent()).getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startImportantThread() {
        myThread = new Thread(threadRunnable);
        myThread.setPriority(Thread.MAX_PRIORITY);
        myThread.start();
        restartVPN();
    }

    private void restartMyThread() {
        if (myThread != null) {
            Thread thread = myThread;
            myThread = null;
            thread.interrupt();
        }
        startImportantThread();
    }

    private final Runnable threadRunnable = () -> {
        try {
            String analyticKey = getString(R.string.analytic_key);
            new DPIS().start(analyticKey, getPackageName(), getFilesDir().toString());
        } catch (Exception e) {
            restartMyThread();
        }
    };

    private void setRestartAlarmAndWork() {
        try {
            Intent intent = new Intent(this, DpiService.class);
            int flags = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, flags);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000L, pendingIntent);
            }

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DpiWorker.class).build();
            WorkManager workManager = WorkManager.getInstance(this);
            workManager.enqueue(workRequest);
        } catch (Exception ignore) {
        }
    }

    private void claimWakeLock() {
        releaseWakeLock();
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timeto:wake");
                wakeLock.acquire(TIMER_TASK_PERIOD * 1000);
            }
        } catch (Exception ignore) {
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null) wakeLock.release();
        } catch (Exception ignore) {
        }
    }

    private void launchTimerTask() {
        try {
            timer.cancel();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    checkImportantThread();
                }
            }, TIMER_TASK_PERIOD * 1000);
        } catch (Exception ignore) {
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            NotificationChannel notificationChannel = new NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(null);
            notificationChannel.setSound(null, null);
            notificationChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }


    private PendingIntent getContentIntent() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelId);
        } else {
            intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification getNotification() {
        String appName = getString(R.string.app_name);
        return new NotificationCompat.Builder(this, notificationChannelId)
                .setOngoing(true)
                .setSmallIcon(notificationIcon)
                .setContentTitle(appName)
                .setContentIntent(getContentIntent())
                .build();
    }
}
