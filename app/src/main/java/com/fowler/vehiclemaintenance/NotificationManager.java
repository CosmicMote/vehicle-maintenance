package com.fowler.vehiclemaintenance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public final class NotificationManager {

    private static final String TAG = NotificationManager.class.getSimpleName();

    public static void registerPeriodicNotifications(Context context) {
        Intent alarmIntent = new Intent(context, MaintenanceNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        // With setInexactRepeating, you must use one of the AlarmManager constants for the interval.
        // If you need a custom interval, use setRepeating instead.
//        manager.setRepeating(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis(),
//                60000, pendingIntent);
        long triggerAt = SystemClock.elapsedRealtime() + 60*1000; // one minute from now
        manager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAt,
                AlarmManager.INTERVAL_HOUR, pendingIntent);
        Log.i(TAG, "Scheduled hourly repeating alarms for vehicle maintenance status checks. " +
                   "First check (using inexact timing) in one minute.");
    }

    private NotificationManager() {  }
}
