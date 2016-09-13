package nz.edpe.routine.provider.routine.db;

import android.database.Cursor;

import nz.edpe.routine.schedule.EventType;

public class RoutineRow {
    public long RowId;

    public int Priority;
    public int BeginTime;
    public int EndTime;
    public int DaysOfWeek;

    public RoutineRow() {
        RowId = 0;
        Priority = EventType.PRIORITY_MEDIUM.getCode();
        BeginTime = 9 * 60;
        EndTime = 17 * 60;
        DaysOfWeek = 0b1111111; // MTWTF
    }

    public RoutineRow(Cursor cursor) {
        final int rowIdIndex = cursor.getColumnIndex(RoutineTable.COLUMN_ROWID);
        if (rowIdIndex >= 0) RowId = cursor.getInt(rowIdIndex);

        final int priorityIndex = cursor.getColumnIndex(RoutineTable.COLUMN_PRIORITY);
        if (priorityIndex >= 0) Priority = cursor.getInt(priorityIndex);

        final int beginTimeIndex = cursor.getColumnIndex(RoutineTable.COLUMN_BEGIN_TIME);
        if (beginTimeIndex >= 0) BeginTime = cursor.getInt(beginTimeIndex);

        final int endTimeIndex = cursor.getColumnIndex(RoutineTable.COLUMN_END_TIME);
        if (endTimeIndex >= 0) EndTime = cursor.getInt(endTimeIndex);

        final int daysOfWeekIndex = cursor.getColumnIndex(RoutineTable.COLUMN_DAYS_OF_WEEK);
        if (daysOfWeekIndex >= 0) DaysOfWeek = cursor.getInt(daysOfWeekIndex);
    }
}
