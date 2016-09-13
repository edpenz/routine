package nz.edpe.routine.provider.routine.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RoutineDB extends SQLiteOpenHelper {
    private static RoutineDB sSingleton;
    private static int sSingletonOpenCount = 0;

    private final SQLiteDatabase mDb;

    public static final String DB_NAME = "ROUTINE_PROVIDER_CONFIG";
    private static final int CURRENT_VERSION = 1;

    private RoutineDB(Context context) {
        super(context, DB_NAME, null, CURRENT_VERSION);
        mDb = getWritableDatabase();
    }

    public synchronized static RoutineDB open(Context context) {
        if (++sSingletonOpenCount == 1) {
            sSingleton = new RoutineDB(context);
        }
        return sSingleton;
    }

    @Override
    public void close() {
        synchronized (RoutineDB.class) {
            if (--sSingletonOpenCount == 0) {
                super.close();
                sSingleton = null;
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        getRoutineTable(db).create();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalArgumentException("Can't upgrade DB");
    }

    public RoutineTable getRoutineTable() {
        return getRoutineTable(getWritableDatabase());
    }

    public RoutineTable getRoutineTable(SQLiteDatabase db) {
        return new RoutineTable(db);
    }
}
