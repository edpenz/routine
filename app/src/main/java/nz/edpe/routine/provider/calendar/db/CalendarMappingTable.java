package nz.edpe.routine.provider.calendar.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.CursorAdapter;

import nz.edpe.routine.db.CursorIterable;

public class CalendarMappingTable {
    public static final String TABLE_NAME = "CALENDAR_SERIES_CONFIG";

    public static final String COLUMN_ROWID = "rowid";
    public static final String COLUMN_CALENDAR_ID = "CALENDAR_ID";
    public static final String COLUMN_PRIORITY = "PRIORITY";

    public static final String[] COLUMNS = new String[]{
            COLUMN_ROWID,
            COLUMN_CALENDAR_ID,
            COLUMN_PRIORITY
    };

    private final SQLiteDatabase mDb;

    CalendarMappingTable(SQLiteDatabase db) {
        mDb = db;
    }

    void create() {
        mDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_CALENDAR_ID + " INT, " +
                COLUMN_PRIORITY + " INT);");
    }

    public void save(CalendarMappingRow row) {
        ContentValues data = new ContentValues();
        data.put(COLUMN_CALENDAR_ID, row.CalendarId);
        data.put(COLUMN_PRIORITY, row.Priority);

        row.RowId = (int) mDb.insert(TABLE_NAME, null, data);
    }

    public void update(CalendarMappingRow row) {
        ContentValues data = new ContentValues();
        data.put(COLUMN_CALENDAR_ID, row.CalendarId);
        data.put(COLUMN_PRIORITY, row.Priority);

        mDb.update(TABLE_NAME, data, COLUMN_ROWID + " = ?", new String[]{String.valueOf(row.RowId)});
    }

    public void delete(CalendarMappingRow row) {
        mDb.delete(TABLE_NAME, COLUMN_ROWID + " = ?", new String[]{String.valueOf(row.RowId)});
    }

    public Cursor queryAll() {
        return mDb.query(TABLE_NAME, COLUMNS,
                null, null,
                null, null, null);
    }

    public CursorIterable<CalendarMappingRow> iterateAll() {
        return new CursorIterable<CalendarMappingRow>(queryAll()) {
            @Override
            protected CalendarMappingRow getItem(Cursor row) {
                return new CalendarMappingRow(row);
            }
        };
    }
}
