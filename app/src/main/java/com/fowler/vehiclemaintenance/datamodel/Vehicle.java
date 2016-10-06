package com.fowler.vehiclemaintenance.datamodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo: implement Parcelable
public class Vehicle implements Serializable {

    private static final int DEFAULT_MILES_PER_MONTH = 1000;

    private Integer id;
    private String name;
    private Integer milesPerMonth;
    private List<MaintenanceItem> maintenanceItems;
    private Mileage initialMileage;
    private Mileage latestMileage;

    public Vehicle(String name, int currentMileage, Integer milesPerMonth) {
        this.name = name;
        this.initialMileage = new Mileage(currentMileage);
        this.milesPerMonth = milesPerMonth;
    }

    public Vehicle(Integer id, String name, Integer milesPerMonth) {
        this.id = id;
        this.name = name;
        this.milesPerMonth = milesPerMonth;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMilesPerMonth() {
        return milesPerMonth;
    }

    public void setMilesPerMonth(Integer milesPerMonth) {
        this.milesPerMonth = milesPerMonth;
    }

    public List<MaintenanceItem> getMaintenanceItems() {
        if(maintenanceItems == null)
            maintenanceItems = new ArrayList<>();
        return maintenanceItems;
    }

    public MaintenanceItem getMaintenanceItemById(int maintenanceItemId) {
        for(MaintenanceItem maintenanceItem : getMaintenanceItems()) {
            if(maintenanceItem.getId() == maintenanceItemId)
                return maintenanceItem;
        }
        return null;
    }

    public List<MaintenanceType> getUnusedMaintenanceTypes() {
        List<MaintenanceType> unusedMaintenanceTypes = new ArrayList<>(Arrays.asList(MaintenanceType.values()));
        for(MaintenanceItem maintenanceItem : getMaintenanceItems()) {
            unusedMaintenanceTypes.remove(maintenanceItem.getType());
        }
        return unusedMaintenanceTypes;
    }

    public Mileage getInitialMileage() {
        return initialMileage;
    }

    public void setInitialMileage(Mileage initialMileage) {
        this.initialMileage = initialMileage;
    }

    public Mileage getLatestMileage() {
        return latestMileage;
    }

    public void setLatestMileage(Mileage latestMileage) {
        this.latestMileage = latestMileage;
    }

    public void setCurrentMileage(int mileage) {
        // If the initial mileage was just set today (i.e. vehicle was created today), then
        // assume we're just updating that mileage.  Otherwise update the latest mileage.
        if(initialMileage.isToday())
            initialMileage = new Mileage(mileage);
        else
            this.latestMileage = new Mileage(mileage);
    }

    public int getEstimatedCurrentMileage() {

        // Handle special cases where we have recorded mileage for today
        if(initialMileage.isToday())
            return initialMileage.getMiles();
        else if(latestMileage != null && latestMileage.isToday())
            return latestMileage.getMiles();

        // Handle normal cases where we estimate mileage
        if(latestMileage != null) {
            float avgMilesPerDay = initialMileage.getAvgMilesPerDay(latestMileage);
            return latestMileage.getEstimatedCurrentMileage(avgMilesPerDay);
        } else {
            float avgMilesPerDay = (milesPerMonth != null ? milesPerMonth : DEFAULT_MILES_PER_MONTH) / 30f;
            return initialMileage.getEstimatedCurrentMileage(avgMilesPerDay);
        }
    }

    public MaintenanceStatus getMaintenanceStatus() {
        boolean anySoonDue = false;
        for(MaintenanceItem maintenanceItem : getMaintenanceItems()) {
            switch(maintenanceItem.getMaintenanceStatus()) {
                case SOON_DUE: anySoonDue = true; break;
                case PAST_DUE: return MaintenanceStatus.PAST_DUE;
            }
        }

        return anySoonDue ? MaintenanceStatus.SOON_DUE : MaintenanceStatus.CURRENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vehicle vehicle = (Vehicle) o;

        return id.equals(vehicle.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
