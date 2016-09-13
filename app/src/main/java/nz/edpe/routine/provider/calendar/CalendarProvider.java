package nz.edpe.routine.provider.calendar;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nz.edpe.routine.provider.calendar.db.CalendarIterable;
import nz.edpe.routine.provider.calendar.db.CalendarMappingDB;
import nz.edpe.routine.provider.calendar.db.CalendarMappingRow;
import nz.edpe.routine.schedule.Event;
import nz.edpe.routine.schedule.EventScheduleProvider;
import nz.edpe.routine.schedule.EventType;
import nz.edpe.routine.date.JulianDay;

public class CalendarProvider extends EventScheduleProvider {
    private final Context mContext;
    private final CalendarMappingDB mMapping;

    public CalendarProvider(Context context) {
        mContext = context;
        mMapping = CalendarMappingDB.open(mContext);
    }

    @Override
    public void close() {
        mMapping.close();
    }

    @Override
    public Collection<Event> getEventsForDay(int julianDay) {
        Calendar dayBegin = JulianDay.toDate(julianDay);
        Calendar dayEnd = (Calendar) dayBegin.clone();
        dayEnd.add(GregorianCalendar.DAY_OF_MONTH, 1);

        Map<String, CalendarMappingRow> eventMapping = getMapping();
        CalendarIterable calendarEvents = new CalendarIterable(mContext, dayBegin, dayEnd);

        List<Event> mappedEvents = new ArrayList<>();
        try {
            for (CalendarIterable.CalendarEvent event : calendarEvents) {
                CalendarMappingRow mapping = eventMapping.get(event.CalendarID);
                Event mappedEvent = mapEvent(event, mapping, dayBegin);
                mappedEvents.add(mappedEvent);
            }
        } finally {
            calendarEvents.close();
        }

        return mappedEvents;
    }

    private Map<String, CalendarMappingRow> getMapping() {
        Map<String, CalendarMappingRow> mapping = new HashMap<>();

        Cursor cursor = mMapping.getMappingTable().queryAll();
        try {
            for (CalendarMappingRow row : CalendarMappingRow.createIterator(cursor)) {
                mapping.put(row.CalendarId, row);
            }
            return mapping;
        } finally {
            cursor.close();
        }
    }

    private Event mapEvent(CalendarIterable.CalendarEvent event, CalendarMappingRow mapping, Calendar dayBeginTime) {
        EventType priority = EventType.PRIORITY_MEDIUM;
        if (event.IsAllDay) {
            priority = EventType.NONE;
        } else if (mapping != null) {
            priority = EventType.fromCode(mapping.Priority, priority);
        }

        long startOffsetMillis = event.BeginTime - dayBeginTime.getTimeInMillis();
        long endOffsetMillis = event.EndTime - dayBeginTime.getTimeInMillis();

        return new Event(priority, 0, startOffsetMillis, endOffsetMillis);
    }
}
