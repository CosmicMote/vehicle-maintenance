package com.fowler.vehiclemaintenance.datamodel;

import android.support.annotation.NonNull;

import com.fowler.vehiclemaintenance.util.TimeUtil;

import java.io.Serializable;
import java.util.Date;

// todo implement Parcelable
public class Mileage implements Serializable, Comparable<Mileage> {

    private Date date;
    private int miles;

    public Mileage(int miles) {
        this.date = TimeUtil.midnightToday().getTime();
        this.miles = miles;
    }

    Mileage(Date date, int miles) {
        this.date = date;
        this.miles = miles;
    }

    public boolean isToday() {
        return this.date.equals(TimeUtil.midnightToday().getTime());
    }

    public Date getDate() {
        return date;
    }

    public int getMiles() {
        return miles;
    }

    public float getAvgMilesPerDay(Mileage other) {
        int mileageDiff = Math.abs(this.miles - other.miles);
        float daysDiff = Math.abs((this.date.getTime() - other.date.getTime()) / (float) TimeUtil.ONE_DAY);
        return mileageDiff / daysDiff;
    }

    public int getEstimatedCurrentMileage(float milesPerDay) {
        float daysDiff = Math.abs((this.date.getTime() - System.currentTimeMillis()) / (float) TimeUtil.ONE_DAY);
        return Math.round(this.miles + daysDiff * milesPerDay);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mileage mileage = (Mileage) o;

        return date.equals(mileage.date);
    }

    @Override
    public int hashCode() {
        return date.hashCode();
    }

    @Override
    public int compareTo(@NonNull Mileage that) {
        return this.date.compareTo(that.date);
    }
}
