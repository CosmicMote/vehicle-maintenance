package com.fowler.vehiclemaintenance.datamodel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SimpleDateFormat")
public class VehicleDataService {

    private static final String TAG = VehicleDataService.class.getName();
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private File dbFile;

    public VehicleDataService(Context context) {
        Log.d(TAG, "Creating vehicles db if it does not exist");
        dbFile = new File(context.getFilesDir(), "vehicles.db");
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            db.execSQL("create table if not exists mileage " +
                       "( " +
                       "  id integer primary key not null, " +
                       "  date text not null, " +
                       "  miles integer not null " +
                       ");");

            db.execSQL("create table if not exists vehicle " +
                       "( " +
                       "  id integer primary key not null, " +
                       "  name text not null, " +
                       "  miles_per_month integer null, " +
                       "  initial_mileage_id integer not null, " +
                       "  latest_mileage_id integer null, " +
                       "  foreign key(initial_mileage_id) references mileage_time(id), " +
                       "  foreign key(latest_mileage_id) references mileage_time(id) " +
                       ");");

            db.execSQL("create table if not exists maintenance_item " +
                       "( " +
                       "  id integer primary key not null, " +
                       "  vehicle_id integer not null, " +
                       "  type text not null, " +
                       "  mileage_interval integer not null, " +
                       "  last_mileage_done integer null, " +
                       "  last_notification integer null, " +
                       "  foreign key(vehicle_id) references vehicle(id) " +
                       ");");

        } finally {
            if(db != null)
                db.close();
        }
        Log.d(TAG, "Created vehicles db if it did not exist");
    }

    public List<Vehicle> getVehicles() {
        Log.d(TAG, "Getting vehicles...");
        Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            Map<Integer, Mileage> mileageMap = getMileages(db, "select * from mileage");

            cursor = db.rawQuery("select * from vehicle order by name", null);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                Integer milesPerMonth = null;
                if(!cursor.isNull(2))
                    milesPerMonth = cursor.getInt(2);
                int initialMileageId = cursor.getInt(3);
                Integer latestMileageId = null;
                if(!cursor.isNull(4))
                    latestMileageId = cursor.getInt(4);

                Vehicle vehicle = new Vehicle(id, name, milesPerMonth);
                vehicle.setInitialMileage(mileageMap.get(initialMileageId));
                if(latestMileageId != null)
                    vehicle.setLatestMileage(mileageMap.get(latestMileageId));
                vehicles.put(id, vehicle);
            }
            Log.d(TAG, String.format("Successfully retrieved %d vehicles", vehicles.size()));

            cursor = db.rawQuery("select * from maintenance_item order by mileage_interval", null);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(0);
                int vehicleId = cursor.getInt(1);
                MaintenanceType type = MaintenanceType.valueOf(cursor.getString(2));
                int mileageInterval = cursor.getInt(3);
                Integer lastMileageDone = null;
                if(!cursor.isNull(4))
                    lastMileageDone = cursor.getInt(4);
                Date lastNotification = null;
                if(!cursor.isNull(5))
                    lastNotification = new Date(cursor.getInt(5));

                Vehicle vehicle = vehicles.get(vehicleId);
                if(vehicle != null) {
                    MaintenanceItem item =
                            new MaintenanceItem(vehicle, id, type, mileageInterval, lastMileageDone, lastNotification);
                    vehicle.getMaintenanceItems().add(item);
                } else {
                    Log.w(TAG, "Vehicle does not exist: " + vehicleId);
                }
            }

        } catch(Exception e) {
            Log.e(TAG, "Failed to get vehicles", e);
        } finally {
            if(cursor != null)
                cursor.close();
            if(db != null)
                db.close();
        }
        return new ArrayList<>(vehicles.values());
    }

    private Map<Integer, Mileage> getMileages(SQLiteDatabase db, String query, Object... params)
            throws ParseException {

        String[] selectionArgs = null;
        if(params.length > 0) {
            selectionArgs = new String[params.length];
            for(int i = 0; i < params.length; i++)
                selectionArgs[i] = params[i].toString();
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        Cursor cursor = null;
        try {
            Map<Integer, Mileage> mileages = new HashMap<>();
            cursor = db.rawQuery(query, selectionArgs);
            while(cursor.moveToNext()) {
                int id = cursor.getInt(0);
                Date date = sdf.parse(cursor.getString(1));
                int miles = cursor.getInt(2);
                mileages.put(id, new Mileage(date, miles));
            }
            return mileages;
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    public Vehicle getVehicle(int id) {
        Vehicle vehicle = null;
        Log.d(TAG, "Getting vehicle by id " + id);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            String[] params = {"" + id};

            cursor = db.rawQuery("select * from vehicle where id = ?", params);
            if (cursor.moveToNext()) {
                String name = cursor.getString(1);
                Integer milesPerMonth = null;
                if(!cursor.isNull(2))
                    milesPerMonth = cursor.getInt(2);
                int initialMileageId = cursor.getInt(3);
                Integer latestMileageId = null;
                if(!cursor.isNull(4))
                    latestMileageId = cursor.getInt(4);

                vehicle = new Vehicle(id, name, milesPerMonth);

                Map<Integer, Mileage> mileageMap;
                if(latestMileageId != null) {
                    mileageMap = getMileages(db, "select * from mileage where id in (?, ?)",
                            initialMileageId, latestMileageId);
                }
                else {
                    mileageMap = getMileages(db, "select * from mileage where id = ?", initialMileageId);
                }

                vehicle.setInitialMileage(mileageMap.get(initialMileageId));
                if(latestMileageId != null)
                    vehicle.setLatestMileage(mileageMap.get(latestMileageId));

            } else {
                Log.w(TAG, "Vehicle not found: " + id);
                return null;
            }
            Log.d(TAG, "Successfully retrieved vehicle by id: " + id);

            cursor = db.rawQuery("select * from maintenance_item where vehicle_id = ? order by mileage_interval", params);
            while(cursor.moveToNext()) {
                int maintenanceItemId = cursor.getInt(0);
                @SuppressWarnings("unused")
                int vehicleId = cursor.getInt(1);
                MaintenanceType type = MaintenanceType.valueOf(cursor.getString(2));
                int mileageInterval = cursor.getInt(3);
                Integer lastMileageDone = null;
                if(!cursor.isNull(4))
                    lastMileageDone = cursor.getInt(4);
                Date lastNotification = null;
                if(!cursor.isNull(5))
                    lastNotification = new Date(cursor.getInt(5));

                MaintenanceItem item =
                  new MaintenanceItem(vehicle, maintenanceItemId, type, mileageInterval, lastMileageDone,
                          lastNotification);
                vehicle.getMaintenanceItems().add(item);
            }

        } catch(Exception e) {
            Log.e(TAG, "Failed to get vehicle by id: " + id, e);
        } finally {
            if(cursor != null)
                cursor.close();
            if(db != null)
                db.close();
        }

        return vehicle;
    }

    public MaintenanceItem getMaintenanceItem(int id) {
        Log.d(TAG, "Getting maintenance by id " + id);
        // First get the vehicle id associated with the maintenance item
        Integer vehicleId = null;
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

            cursor = db.rawQuery(
                    "select v.id from vehicle v " +
                    "  join maintenance_item mi on (v.id = mi.vehicle_id) " +
                    "where mi.id = ?", new String[]{"" + id});
            if(cursor.moveToNext())
                vehicleId = cursor.getInt(0);
        } catch(Exception e) {
            Log.e(TAG, "Failed to get vehicle id by maintenance item id: " + id, e);
        } finally {
            if(cursor != null)
                cursor.close();
            if(db != null)
                db.close();
        }

        if(vehicleId == null) {
            Log.d(TAG, "Vehicle id not found for maintenance item id: " + id);
            return null;
        }

        // Since we want the object graph to be complete, and MaintenanceItem references Vehicle,
        // just fetch the vehicle itself.
        Vehicle vehicle = getVehicle(vehicleId);
        return vehicle.getMaintenanceItemById(id);
    }

    public int persist(Vehicle vehicle) {

        Integer vehicleId = vehicle.getId();
        if(vehicleId != null)
            deleteVehicle(vehicleId);

        Log.d(TAG, "Adding vehicle: " + vehicle.getName());

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();

            int initialMileageId = persist(db, vehicle.getInitialMileage());
            Integer latestMileageId = null;
            if(vehicle.getLatestMileage() != null)
                latestMileageId = persist(db, vehicle.getLatestMileage());

            Object[] params = new Object[]{ vehicle.getName(), vehicle.getMilesPerMonth(),
                                            initialMileageId, latestMileageId };
            db.execSQL("insert into vehicle (name, miles_per_month, initial_mileage_id, latest_mileage_id) " +
                       "values (?, ?, ?, ?)", params);

            // retrieve the id of the vehicle just inserted
            cursor = db.rawQuery("select max(id) from vehicle", null);
            if(cursor.moveToNext())
                vehicleId = cursor.getInt(0);
            else
                throw new IllegalStateException("No id found for vehicle just inserted");

            for(MaintenanceItem maintenanceItem : vehicle.getMaintenanceItems()) {
                params = new Object[]{ vehicleId, maintenanceItem.getType().name(),
                        maintenanceItem.getMileageInterval(), maintenanceItem.getLastMileageDone() };
                db.execSQL("insert into maintenance_item (vehicle_id, type, mileage_interval, last_mileage_done) " +
                           "values (?, ?, ?, ?)", params);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully added vehicle: " + vehicle.getName());
            return vehicleId;
        } catch(Exception e) {
            Log.e(TAG, "Failed to add vehicle", e);
            throw e;
        } finally {
            if(cursor != null) {
                cursor.close();
            }
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public int persist(SQLiteDatabase db, Mileage mileage) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        Cursor cursor = null;
        try {
            Object[] params = new Object[]{ sdf.format(mileage.getDate()), mileage.getMiles() };
            db.execSQL("insert into mileage (date, miles) values (?, ?)", params);
            // Retrieve the id of the mileage just inserted
            cursor = db.rawQuery("select max(id) from mileage", null);
            if(cursor.moveToNext())
                return cursor.getInt(0);
            else
                throw new IllegalStateException("No id found for mileage just inserted");
        } finally {
            if(cursor != null)
                cursor.close();
        }
    }

    public int persist(int vehicleId, MaintenanceItem maintenanceItem) {
        Integer maintenanceItemId = maintenanceItem.getId();
        if(maintenanceItemId != null)
            deleteMaintenanceItem(maintenanceItemId);

        Log.d(TAG, "Adding maintenance item: " + maintenanceItem);

        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();

            Object[] params = new Object[]{ vehicleId, maintenanceItem.getType().name(),
                                            maintenanceItem.getMileageInterval(), maintenanceItem.getLastMileageDone(),
                    maintenanceItem.getLastNotification() != null ? maintenanceItem.getLastNotification().getTime() : null };
            db.execSQL("insert into maintenance_item (vehicle_id, type, mileage_interval, last_mileage_done, last_notification) " +
                       "values (?, ?, ?, ?, ?)", params);
            if(maintenanceItemId == null) {
                // retrieve the id of the vehicle just inserted
                cursor = db.rawQuery("select max(id) from maintenance_item", null);
                if(cursor.moveToNext())
                    maintenanceItemId = cursor.getInt(0);
                else
                    throw new IllegalStateException("No id found for maintenance item just inserted");
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully added maintenance item: " + maintenanceItem);
            return maintenanceItemId;
        } catch(Exception e) {
            Log.e(TAG, "Failed to add maintenance item", e);
            throw e;
        } finally {
            if(cursor != null) {
                cursor.close();
            }
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void markAsNotified(int maintenanceItemId) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();

            Object[] params = new Object[]{ System.currentTimeMillis(), maintenanceItemId };
            db.execSQL("update maintenance_item set last_notification = ? where id = ?", params);

            db.setTransactionSuccessful();
            Log.d(TAG, String.format("Successfully marked maintenance item %d as notified", maintenanceItemId));
        } catch(Exception e) {
            Log.e(TAG, String.format("Failed to mark maintenance item %d as notified", maintenanceItemId), e);
            throw e;
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void persist(int maintenanceItemId, int mileageDone) {

        Log.d(TAG, String.format("Setting mileage done %d for maintenance item %d", mileageDone, maintenanceItemId));

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();
            Object[] params = new Object[]{ mileageDone, maintenanceItemId };
            db.execSQL("update maintenance_item set last_mileage_done = ? where id = ?", params);
            db.setTransactionSuccessful();
            Log.d(TAG, String.format("Successfully set mileage done %d for maintenance item %d",
                    mileageDone, maintenanceItemId));
        } catch(Exception e) {
            Log.e(TAG, "Failed to set mileage done for maintenance item", e);
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void deleteVehicle(int id) {
        Log.d(TAG, "Deleting vehicle: " + id);

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();
            Object[] params = { id };
            db.execSQL("delete from mileage where id = (select initial_mileage_id from vehicle where id = ?)", params);
            db.execSQL("delete from mileage where id = (select latest_mileage_id from vehicle where id = ?)", params);
            db.execSQL("delete from maintenance_item where vehicle_id = ?", params);
            db.execSQL("delete from vehicle where id = ?", params);
            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully deleted vehicle: " + id);
        } catch(Exception e) {
            Log.e(TAG, "Failed to delete vehicle", e);
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void deleteMaintenanceItem(int id) {
        Log.d(TAG, "Deleting maintenance item: " + id);

        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            db.beginTransaction();
            Object[] params = { id };
            db.execSQL("delete from maintenance_item where id = ?", params);
            db.setTransactionSuccessful();
            Log.d(TAG, "Successfully deleted maintenance item: " + id);
        } catch(Exception e) {
            Log.e(TAG, "Failed to delete maintenance item", e);
        } finally {
            if(db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }
}
