package com.fowler.vehiclemaintenance.util;

import java.util.Calendar;

public final class TimeUtil {

    public static final long ONE_DAY = 24 * 60 * 60 * 1000;

    public static Calendar midnightToday() {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, 0);
        time.set(Calendar.MINUTE, 0);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);
        return time;
    }

    private TimeUtil() {}
}
