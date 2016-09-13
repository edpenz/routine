package nz.edpe.routine.date;

import java.util.Calendar;
import java.util.GregorianCalendar;

// From http://en.wikipedia.org/wiki/Julian_day
public class JulianDay {
    public static int fromDate(Calendar date) {
        final int year = date.get(Calendar.YEAR);
        final int month = date.get(Calendar.MONTH) + 1; // Calendar months are 0-indexed.
        final int day = date.get(Calendar.DAY_OF_MONTH);

        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    public static Calendar toDate(int julianDay) {
        int f = julianDay + 1401 + (((4 * julianDay + 274277) / 146097) * 3) / 4 - 38;
        int e = 4 * f + 3;
        int g = e % 1461 / 4;
        int h = 5 * g + 2;

        int day = (h % 153) / 5 + 1;
        int month = (h / 153 + 2) % 12 + 1;
        int year = (e / 1461) - 4716 + (12 + 2 - month) / 12;

        return new GregorianCalendar(year, month - 1, day); // Calendar months are 0-indexed.
    }

    public static int today() {
        Calendar today = Calendar.getInstance();
        return fromDate(today);
    }

    private JulianDay() {
        throw new IllegalAccessError("Static-only class");
    }
}
