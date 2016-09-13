package nz.edpe.routine.provider.routine.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import nz.edpe.routine.db.CursorIterable;

public class RoutineTable {
    private final SQLiteDatabase mDb;

    public static final String TABLE_NAME = "ROUTINE_SERIES";

    public static final String COLUMN_ROWID = "rowid";
    public static final String COLUMN_PRIORITY = "PRIORITY";
    public static final String COLUMN_BEGIN_TIME = "BEGIN_TIME";
    public static final String COLUMN_END_TIME = "END_TIME";
    public static final String COLUMN_DAYS_OF_WEEK = "DAYS_OF_WEEK";

    public static final String[] COLUMNS = new String[]{
            COLUMN_ROWID,
            COLUMN_PRIORITY,
            COLUMN_BEGIN_TIME,
            COLUMN_END_TIME,
            COLUMN_DAYS_OF_WEEK
    };

    public RoutineTable(SQLiteDatabase db) {
        mDb = db;
    }

    public void create() {
        mDb.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_PRIORITY + " INT, " +
                COLUMN_BEGIN_TIME + " INT, " +
                COLUMN_END_TIME + " INT, " +
                COLUMN_DAYS_OF_WEEK + " INT);");
    }

    public void save(RoutineRow row) {
        ContentValues data = new ContentValues();
        data.put(COLUMN_PRIORITY, row.Priority);
        data.put(COLUMN_BEGIN_TIME, row.BeginTime);
        data.put(COLUMN_END_TIME, row.EndTime);
        data.put(COLUMN_DAYS_OF_WEEK, row.DaysOfWeek);

        row.RowId = (int) mDb.insert(TABLE_NAME, null, data);
    }

    public void update(RoutineRow row) {
        ContentValues data = new ContentValues();
        data.put(COLUMN_PRIORITY, row.Priority);
        data.put(COLUMN_BEGIN_TIME, row.BeginTime);
        data.put(COLUMN_END_TIME, row.EndTime);
        data.put(COLUMN_DAYS_OF_WEEK, row.DaysOfWeek);

        mDb.update(TABLE_NAME, data, COLUMN_ROWID + " = ?", new String[]{String.valueOf(row.RowId)});
    }

    public void delete(RoutineRow row) {
        mDb.delete(TABLE_NAME, COLUMN_ROWID + " = ?", new String[]{String.valueOf(row.RowId)});
    }

    public Cursor queryAll() {
        return mDb.query(TABLE_NAME, COLUMNS,
                null, null,
                null, null, null);
    }

    public CursorIterable<RoutineRow> iterateAll() {
        return  new CursorIterable<RoutineRow>(queryAll()) {
            @Override
            protected RoutineRow getItem(Cursor row) {
                return new RoutineRow(row);
            }
        };
    }
}
