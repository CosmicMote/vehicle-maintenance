package com.fowler.vehiclemaintenance.datamodel;

import android.graphics.Color;

import com.fowler.vehiclemaintenance.R;

public enum MaintenanceStatus {
    CURRENT("Current", R.drawable.ic_done_green_24dp, null),
    SOON_DUE("Soon Due", R.drawable.ic_warning_yellow_24dp, null),
    PAST_DUE("Past Due", R.drawable.ic_error_red_24dp, Color.RED);

    private String value;
    private int imageResource;
    private Integer color;

    MaintenanceStatus(String value, int imageResource, Integer color) {
        this.value = value;
        this.imageResource = imageResource;
        this.color = color;
    }

    public int getImageResource() {
        return imageResource;
    }

    public Integer getColor() {
        return color;
    }

    @Override
    public String toString() {
        return value;
    }

    public String toString(int mileage) {
        switch(this) {
            case CURRENT: return String.format("%s (next due at %,d miles)", toString(), mileage);
            case SOON_DUE:
            case PAST_DUE: return String.format("%s at %,d miles", toString(), mileage);
            default:
                throw new IllegalStateException("Unknown maintenance status value: " + this);
        }
    }
}
