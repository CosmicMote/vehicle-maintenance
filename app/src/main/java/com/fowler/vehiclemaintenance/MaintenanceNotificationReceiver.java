package com.fowler.vehiclemaintenance;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.fowler.vehiclemaintenance.datamodel.MaintenanceItem;
import com.fowler.vehiclemaintenance.datamodel.MaintenanceStatus;
import com.fowler.vehiclemaintenance.datamodel.Vehicle;
import com.fowler.vehiclemaintenance.datamodel.VehicleDataService;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MaintenanceNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = MaintenanceNotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Checking vehicle status for potential notifications...");

        Calendar notificationCutoff = Calendar.getInstance();
        notificationCutoff.add(Calendar.DAY_OF_YEAR, -1);

        VehicleDataService vehiclesDataSvc = new VehicleDataService(context);
        for(Vehicle vehicle : vehiclesDataSvc.getVehicles()) {
            for(MaintenanceItem maintenanceItem : vehicle.getMaintenanceItems()) {
                MaintenanceStatus status = maintenanceItem.getMaintenanceStatus();
                if(status != MaintenanceStatus.CURRENT) {
                    Date lastNotification = maintenanceItem.getLastNotification();
                    boolean notify = lastNotification == null || lastNotification.before(notificationCutoff.getTime());
                    String title = vehicle.getName() + " " + maintenanceItem.getType();
                    if(notify) {
                        Log.d(TAG, "Creating notification for " + title);
                        String msg = status.toString(maintenanceItem.getMileageDue());
                        addNotification(context, maintenanceItem.getId(), title, Collections.singletonList(msg));
                        vehiclesDataSvc.markAsNotified(maintenanceItem.getId());
                    } else {
                        Log.d(TAG, String.format("Skipping notification for %s (last notification at %s)",
                                title, lastNotification));
                    }
                }
            }
        }
    }

    private void addNotification(Context context, int id, String title, List<String> messages) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.vehicle_icon);
        builder.setContentTitle(title);
        if(messages.size() == 1) {
            builder.setContentText(messages.get(0));
        } else {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("Maintenance soon due or past due:");
            for(String message : messages)
                inboxStyle.addLine(message);
            builder.setStyle(inboxStyle);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(VehicleListActivity.class);
        Intent intent = new Intent(context, VehicleListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }
}
