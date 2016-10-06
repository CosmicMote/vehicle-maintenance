package com.fowler.vehiclemaintenance.datamodel;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Date;

// todo implement Parcelable
public class MaintenanceItem implements Serializable, Comparable<MaintenanceItem> {

    private static final int SOON_DUE_THRESHOLD = 500;

    private Vehicle vehicle;
    private Integer id;
    private MaintenanceType type;
    private int mileageInterval;
    private Integer lastMileageDone;
    private Date lastNotification;

    public MaintenanceItem() {

    }

    public MaintenanceItem(Vehicle vehicle, Integer id, MaintenanceType type, int mileageInterval,
                           Integer lastMileageDone, Date lastNotification) {
        this.vehicle = vehicle;
        this.id = id;
        this.type = type;
        this.mileageInterval = mileageInterval;
        this.lastMileageDone = lastMileageDone;
        this.lastNotification = lastNotification;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Integer getId() {
        return id;
    }

    public MaintenanceType getType() {
        return type;
    }

    public void setType(MaintenanceType type) {
        this.type = type;
    }

    public int getMileageInterval() {
        return mileageInterval;
    }

    public void setMileageInterval(int mileageInterval) {
        this.mileageInterval = mileageInterval;
    }

    public Integer getLastMileageDone() {
        return lastMileageDone;
    }

    public void setLastMileageDone(int lastMileageDone) {
        this.lastMileageDone = lastMileageDone;
    }

    public int getMileageDue() {
        return (lastMileageDone == null ? 0 : lastMileageDone) + mileageInterval;
    }

    public MaintenanceStatus getMaintenanceStatus() {
        int vehicleMileage = vehicle.getEstimatedCurrentMileage();
        int mileageDue = getMileageDue();
        if(mileageDue < vehicleMileage)
            return MaintenanceStatus.PAST_DUE;
        else if(mileageDue - vehicleMileage <= SOON_DUE_THRESHOLD)
            return MaintenanceStatus.SOON_DUE;
        else
            return MaintenanceStatus.CURRENT;
    }

    public Date getLastNotification() {
        return lastNotification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MaintenanceItem that = (MaintenanceItem) o;

        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return vehicle != null ? vehicle.getName() + " " + type : type.toString();
    }

    @Override
    public int compareTo(@NonNull MaintenanceItem that) {
        int compare = this.mileageInterval - that.mileageInterval;
        if(compare != 0)
            return compare;

        return this.type.compareTo(that.type);
    }
}
