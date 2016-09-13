package nz.edpe.routine.provider.routine;

import android.content.Context;
import android.database.Cursor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nz.edpe.routine.db.CursorIterable;
import nz.edpe.routine.provider.routine.db.RoutineDB;
import nz.edpe.routine.provider.routine.db.RoutineRow;
import nz.edpe.routine.schedule.Event;
import nz.edpe.routine.schedule.EventScheduleProvider;
import nz.edpe.routine.schedule.EventType;
import nz.edpe.routine.date.JulianDay;

public class RoutineProvider extends EventScheduleProvider {
    private final RoutineDB mRoutineDB;

    public RoutineProvider(Context context) {
        mRoutineDB = RoutineDB.open(context);
    }

    @Override
    public void close() throws IOException {
        mRoutineDB.close();
    }

    @Override
    public Collection<Event> getEventsForDay(int julianDay) {
        Calendar date = JulianDay.toDate(julianDay);
        int dayOfWeek = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int dayOfWeekMask = 1 << dayOfWeek;

        CursorIterable<RoutineRow> routines = getRoutines();

        List<Event> events = new ArrayList<>();
        try {
            for (RoutineRow routine : routines) {
                Event event = createEvent(routine, dayOfWeekMask);
                events.add(event);
            }
        } finally {
            routines.closeCursor();
        }

        return events;
    }

    private CursorIterable<RoutineRow> getRoutines() {
        Cursor cursor = mRoutineDB.getRoutineTable().queryAll();

        return new CursorIterable<RoutineRow>(cursor) {
            @Override
            protected RoutineRow getItem(Cursor row) {
                return new RoutineRow(row);
            }
        };
    }

    private Event createEvent(RoutineRow routine, int dayOfWeekMask) {
        boolean isToday = (routine.DaysOfWeek & dayOfWeekMask) != 0;

        EventType priority = isToday ? EventType.fromCode(routine.Priority, EventType.PRIORITY_MEDIUM): EventType.NONE;

        final long startOffsetMillis = routine.BeginTime * 60 * 1000;
        final long endOffsetMillis = routine.EndTime * 60 * 1000;

        return new Event(priority, 0, startOffsetMillis, endOffsetMillis);
    }
}
