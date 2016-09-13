package nz.edpe.routine.provider.calendar.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CalendarMappingDB extends SQLiteOpenHelper {
    private static CalendarMappingDB sSingleton;
    private static int sSingletonOpenCount = 0;

    private final SQLiteDatabase mDb;

    public static final String DB_NAME = "ANDROID_CALENDAR_PROVIDER_CONFIG";
    private static final int CURRENT_VERSION = 1;

    private CalendarMappingDB(Context context) {
        super(context, DB_NAME, null, CURRENT_VERSION);
        mDb = getWritableDatabase();
    }

    public synchronized static CalendarMappingDB open(Context context) {
        if (++sSingletonOpenCount == 1) {
            sSingleton = new CalendarMappingDB(context);
        }
        return sSingleton;
    }

    @Override
    public void close() {
        synchronized (CalendarMappingDB.class) {
            if (--sSingletonOpenCount == 0) {
                super.close();
                sSingleton = null;
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        getMappingTable(db).create();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalArgumentException("Can't upgrade DB");
    }

    public CalendarMappingTable getMappingTable() {
        return getMappingTable(getWritableDatabase());
    }

    public CalendarMappingTable getMappingTable(SQLiteDatabase db) {
        return new CalendarMappingTable(db);
    }
}
