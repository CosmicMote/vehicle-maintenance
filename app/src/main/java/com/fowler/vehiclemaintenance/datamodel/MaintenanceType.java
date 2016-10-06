package com.fowler.vehiclemaintenance.datamodel;

public enum MaintenanceType {
//    NONE("Select Maintenance Type", 0),
    OIL_CHANGE("Oil Change", 3000),
    TIRE_ROTATION("Tire Rotation", 10000),
    AIR_FILTER("Air Filter", 50000),
    FUEL_FILTER("Fuel Filter", 50000),
    SPARK_PLUGS("Spark Plugs", 100000),
    TRANSMISSION_FLUID("Transmission Fluid", 50000),
    COOLANT_FLUSH("Coolant Flush", 50000);

    MaintenanceType(String text, int defaultMileageInterval) {
        this.text = text;
        this.defaultMileageInterval = defaultMileageInterval;
    }

    private String text;
    private int defaultMileageInterval;

    public int getDefaultMileageInterval() {
        return defaultMileageInterval;
    }

    @Override
    public String toString() {
        return text;
    }
}
