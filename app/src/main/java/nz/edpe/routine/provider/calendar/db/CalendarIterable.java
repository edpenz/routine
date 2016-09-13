package nz.edpe.routine.provider.calendar.db;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.io.Closeable;
import java.util.Calendar;

import nz.edpe.routine.db.CursorIterable;

public class CalendarIterable extends CursorIterable<CalendarIterable.CalendarEvent> implements Closeable {
    private static final int EVENT_CAL_ID_INDEX = 0;
    private static final int EVENT_BEGIN_INDEX = 1;
    private static final int EVENT_END_INDEX = 2;
    private static final int EVENT_ALL_DAY_INDEX = 3;

    private static final String[] EVENT_INSTANCE_PROJECTION = new String[]{
            CalendarContract.Instances.OWNER_ACCOUNT,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
    };

    private final Cursor mCursor;

    public CalendarIterable(Context context, Calendar start, Calendar end) {
        this(createQuery(context, start, end));
    }

    private CalendarIterable(Cursor cursor) {
        super(cursor);
        mCursor = cursor;
    }

    private static Cursor createQuery(Context context, Calendar start, Calendar end) {
        Uri.Builder uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uriBuilder, start.getTimeInMillis());
        ContentUris.appendId(uriBuilder, end.getTimeInMillis());

        return context.getContentResolver().query(
                uriBuilder.build(), EVENT_INSTANCE_PROJECTION, null, null, null);
    }

    @Override
    protected CalendarEvent getItem(Cursor row) {
        String calendarId = row.getString(EVENT_CAL_ID_INDEX);
        long beginTime = row.getLong(EVENT_BEGIN_INDEX);
        long endTime = row.getLong(EVENT_END_INDEX);
        boolean allDay = (row.getInt(EVENT_ALL_DAY_INDEX) == 1);

        return new CalendarEvent(calendarId, beginTime, endTime, allDay);
    }

    @Override
    public void close() {
        mCursor.close();
    }

    public static class CalendarEvent {
        public final String CalendarID;

        public final long BeginTime;
        public final long EndTime;

        public final boolean IsAllDay;

        public CalendarEvent(String calendarID, long beginTime, long endTime, boolean allDay) {
            CalendarID = calendarID;
            BeginTime = beginTime;
            EndTime = endTime;
            IsAllDay = allDay;
        }
    }
}
