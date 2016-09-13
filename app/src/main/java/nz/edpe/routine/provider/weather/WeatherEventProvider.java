package nz.edpe.routine.provider.weather;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import nz.edpe.routine.date.Astronomy;
import nz.edpe.routine.date.Astronomy.SolarEvent;
import nz.edpe.routine.schedule.Event;
import nz.edpe.routine.schedule.EventScheduleProvider;
import nz.edpe.routine.schedule.EventType;
import nz.edpe.routine.date.JulianDay;

public class WeatherEventProvider extends EventScheduleProvider {
    private static final String API_KEY = "8de99c4e7372a072dd06b22d402feb40";
    private static final String API_TEMPLATE = "http://api.openweathermap.org/data/2.5/forecast?q=&mode=xml&appid=";

    private final GoogleApiClient mGoogleApiClient;

    public WeatherEventProvider(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void close() throws IOException {
        mGoogleApiClient.disconnect();
    }

    @Override
    public Collection<Event> getEventsForDay(int julianDay) {
        Collection<Event> sunlightEvent = getSunlightEvents(julianDay);
        Collection<Event> weatherEvents = getWeatherEvents(julianDay);

        List<Event> events = new ArrayList<>();
        events.addAll(sunlightEvent);
        events.addAll(weatherEvents);
        return events;
    }

    private Collection<Event> getSunlightEvents(int julianDay) {
        Calendar date = JulianDay.toDate(julianDay);
        long dayStartMillis = date.getTimeInMillis();
        Location userLocation = getUserLocation();

        Calendar astronomicalDawn = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNRISE, userLocation, Astronomy.ZENITH_ASTRONOMICAL);
        Calendar nauticalDawn = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNRISE, userLocation, Astronomy.ZENITH_NAUTICAL);
        Calendar civilDawn = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNRISE, userLocation, Astronomy.ZENITH_CIVIL);
        Calendar sunrise = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNRISE, userLocation, Astronomy.ZENITH_OFFICIAL);
        Calendar sunset = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNSET, userLocation, Astronomy.ZENITH_OFFICIAL);
        Calendar civilDusk = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNSET, userLocation, Astronomy.ZENITH_CIVIL);
        Calendar nauticalDusk = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNSET, userLocation, Astronomy.ZENITH_NAUTICAL);
        Calendar astronomicalDusk = Astronomy.getTimeOfSolarEvent(date, SolarEvent.SUNSET, userLocation, Astronomy.ZENITH_ASTRONOMICAL);

        long astronomicalDawnMillis = astronomicalDawn.getTimeInMillis() - dayStartMillis;
        long nauticalDawnMillis = nauticalDawn.getTimeInMillis() - dayStartMillis;
        long civilDawnMillis = civilDawn.getTimeInMillis() - dayStartMillis;
        long sunriseMillis = sunrise.getTimeInMillis() - dayStartMillis;
        long sunsetMillis = sunset.getTimeInMillis() - dayStartMillis;
        long civilDuskMillis = civilDusk.getTimeInMillis() - dayStartMillis;
        long nauticalDuskMillis = nauticalDusk.getTimeInMillis() - dayStartMillis;
        long astronomicalDuskMillis = astronomicalDusk.getTimeInMillis() - dayStartMillis;

        return Arrays.asList(
                new Event(EventType.ASTRONOMY, 0x22, astronomicalDawnMillis, nauticalDawnMillis),
                new Event(EventType.ASTRONOMY, 0x21, nauticalDawnMillis, civilDawnMillis),
                new Event(EventType.ASTRONOMY, 0x20, civilDawnMillis, sunriseMillis),
                new Event(EventType.ASTRONOMY, 0x10, sunriseMillis, sunsetMillis),
                new Event(EventType.ASTRONOMY, 0x20, sunsetMillis, civilDuskMillis),
                new Event(EventType.ASTRONOMY, 0x21, civilDuskMillis, nauticalDuskMillis),
                new Event(EventType.ASTRONOMY, 0x22, nauticalDuskMillis, astronomicalDuskMillis)
        );
    }

    private Location getUserLocation() {
        mGoogleApiClient.blockingConnect();
        if (mGoogleApiClient.isConnected()) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                return lastLocation;
            }
        }

        return Astronomy.estimateLocation(TimeZone.getDefault());
    }

    private Collection<Event> getWeatherEvents(int julianDay) {
        List<Event> events = new ArrayList<>();

        return events;
    }
}
