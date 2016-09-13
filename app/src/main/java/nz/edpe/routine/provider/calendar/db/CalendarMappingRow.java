package nz.edpe.routine.provider.calendar.db;

import android.database.Cursor;

import nz.edpe.routine.db.CursorIterable;

public class CalendarMappingRow {
    public long RowId;

    public String CalendarId;
    public int Priority;

    public CalendarMappingRow(String calendarId) {
        CalendarId = calendarId;
    }

    public CalendarMappingRow(Cursor cursor) {
        final int rowIdIndex = cursor.getColumnIndex(CalendarMappingTable.COLUMN_ROWID);
        if (rowIdIndex >= 0) RowId = cursor.getInt(rowIdIndex);

        final int calendarIdIndex = cursor.getColumnIndex(CalendarMappingTable.COLUMN_CALENDAR_ID);
        if (calendarIdIndex >= 0) CalendarId = cursor.getString(calendarIdIndex);

        final int priorityIndex = cursor.getColumnIndex(CalendarMappingTable.COLUMN_PRIORITY);
        if (priorityIndex >= 0) Priority = cursor.getInt(priorityIndex);
    }

    public static Iterable<CalendarMappingRow> createIterator(Cursor cursor) {
        return new CursorIterable<CalendarMappingRow>(cursor) {
            @Override
            protected CalendarMappingRow getItem(Cursor row) {
                return new CalendarMappingRow(row);
            }
        };
    }
}
