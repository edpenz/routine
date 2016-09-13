package nz.edpe.routine.date;

import android.location.Location;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.lang.Math.floor;

public class Astronomy {
    public static double ZENITH_OFFICIAL = 90.833;
    public static double ZENITH_CIVIL = 96.0;
    public static double ZENITH_NAUTICAL = 102.0;
    public static double ZENITH_ASTRONOMICAL = 108.0;

    public static Calendar getTimeOfSolarEvent(Calendar date, SolarEvent event, Location location) {
        return getTimeOfSolarEvent(date, event, location, ZENITH_OFFICIAL);
    }

    public static Calendar getTimeOfSolarEvent(Calendar date, SolarEvent event, Location location, double zenith) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);

        double longitude = location.getLongitude();
        double latitude = location.getLatitude();

        double localOffset = date.getTimeZone().getOffset(date.getTimeInMillis()) / 1000.0 / 60.0 / 60.0;

        // 1. first calculate the day of the year
        double N1 = floor(275 * month / 9.0);
        double N2 = floor((month + 9) / 12.0);
        double N3 = (1 + floor((year - 4 * floor(year / 4.0) + 2) / 3.0));
        double N = N1 - (N2 * N3) + day - 30;

        // 2. convert the longitude to hour value and calculate an approximate time
        double lngHour = longitude / 15.0;

        double t = N + ((event.a - lngHour) / 24.0);

        // 3. calculate the Sun's mean anomaly
        double M = (0.9856 * t) - 3.289;

        // 4. calculate the Sun's true longitude
        double L = M + (1.916 * sind(M)) + (0.020 * sind(2.0 * M)) + 282.634;
        L = (L + 360.0) % 360.0;
        // NOTE: L potentially needs to be adjusted into the range [0,360) by adding/subtracting 360

        // 5a. calculate the Sun's right ascension
        double RA = atand(0.91764 * tand(L));
        RA = (RA + 360.0) % 360.0;
        // NOTE: RA potentially needs to be adjusted into the range [0,360) by adding/subtracting 360

        // 5b. right ascension value needs to be in the same quadrant as L
        double Lquadrant = floor(L / 90.0) * 90.0;
        double RAquadrant = floor(RA / 90.0) * 90.0;
        RA = RA + (Lquadrant - RAquadrant);

        // 5c. right ascension value needs to be converted into hours
        RA = RA / 15.0;

        // 6. calculate the Sun's declination
        double sinDec = 0.39782 * sind(L);
        double cosDec = cosd(asind(sinDec));

        // 7a. calculate the Sun's local hour angle
        double cosH = (cosd(zenith) - (sinDec * sind(latitude))) / (cosDec * cosd(latitude));

        //if (cosH >  1)
        //    the sun never rises on this location (on the specified date)
        //if (cosH < -1)
        //    the sun never sets on this location (on the specified date)

        // 7b. finish calculating H and convert into hours
        double H = event.b +  event.c * acosd(cosH);

        H = H / 15;

        // 8. calculate local mean time of rising/setting
        double T = H + RA - (0.06571 * t) - 6.622;

        // 9. adjust back to UTC
        double UT = T - lngHour;
        // NOTE: UT potentially needs to be adjusted into the range [0,24) by adding/subtracting 24

        // 10. convert UT value to local time zone of latitude/longitude
        double localT = UT + localOffset;
        localT = (localT + 24.0) % 24.0;

        Calendar eventTime = new GregorianCalendar(year, month - 1, day);
        eventTime.add(Calendar.MILLISECOND, (int) (localT * 60.0 * 60.0 * 1000.0));
        return eventTime;
    }

    public static Location estimateLocation(TimeZone timezone) {
        double offsetHours = timezone.getRawOffset() / 1000.0 / 60.0 / 60.0;
        double longitude = Math.min(Math.max(-180.0, 180.0 * offsetHours / 12.0), 180.0);

        Location estimatedLocation = new Location("");
        estimatedLocation.setLatitude(0.0);
        estimatedLocation.setLongitude(longitude);

        return estimatedLocation;
    }

    private static double sind(double degrees) {
        return Math.sin(Math.toRadians(degrees));
    }

    private static double cosd(double degrees) {
        return Math.cos(Math.toRadians(degrees));
    }

    private static double tand(double degrees) {
        return Math.tan(Math.toRadians(degrees));
    }

    private static double asind(double value) {
        return Math.toDegrees(Math.asin(value));
    }

    private static double acosd(double value) {
        return Math.toDegrees(Math.acos(value));
    }

    private static double atand(double value) {
        return Math.toDegrees(Math.atan(value));
    }

    public enum SolarEvent {
        SUNRISE(6, 360, -1), SUNSET(18, 0, 1);

        final double a;
        final double b;
        final double c;

        SolarEvent(double a, double b, double c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }
}
