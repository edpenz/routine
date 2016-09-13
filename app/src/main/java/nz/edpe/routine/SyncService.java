package nz.edpe.routine;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import nz.edpe.routine.provider.routine.RoutineProvider;
import nz.edpe.routine.provider.calendar.CalendarProvider;
import nz.edpe.routine.provider.weather.WeatherEventProvider;
import nz.edpe.routine.schedule.Event;
import nz.edpe.routine.schedule.EventSchedule;
import nz.edpe.routine.schedule.EventScheduleProvider;
import nz.edpe.routine.schedule.EventScheduleProvider.ScheduleChangeListener;
import nz.edpe.routine.date.JulianDay;

public class SyncService extends IntentService {
    public static final String TAG = SyncService.class.getSimpleName();

    public static UUID WATCHFACE_UUID = UUID.fromString("ae9c2de9-dd67-4335-95ac-f0c0e757571c");

    private static final String ACTION_REPARSE_CALENDAR = "nz.edpe.routine.action.REPARSE_CALENDAR";
    private static final String ACTION_SYNC_IF_DIRTY = "nz.edpe.routine.action.SYNC_IF_DIRTY";

    private static final int DAYS_TO_SYNC = 4;

    private PebbleSocket mSocket;
    private List<EventSchedule> mSchedules = null;

    private List<EventScheduleProvider> mScheduleEventProviders;
    private ScheduleChangeListener mScheduleChangeListener = new ScheduleChangeListener() {
        @Override
        public void onScheduleChanged(EventScheduleProvider provider) {
            handleScheduleChanged(provider);
        }
    };

    public SyncService() {
        super("SyncService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        connectPebble();
        createProviders();
    }

    @Override
    public void onDestroy() {
        destroyProviders();
        disconnectPebble();

        super.onDestroy();
    }

    private void connectPebble() {
        mSocket = new PebbleSocket(this, WATCHFACE_UUID);
    }

    private void createProviders() {
        mScheduleEventProviders = new ArrayList<>();

        mScheduleEventProviders.add(new CalendarProvider(this));
        mScheduleEventProviders.add(new RoutineProvider(this));
        mScheduleEventProviders.add(new WeatherEventProvider(this));

        for (EventScheduleProvider provider : mScheduleEventProviders) {
            provider.addScheduleChangeListener(mScheduleChangeListener);
        }
    }

    private void disconnectPebble() {
        mSocket.close();
    }

    private void destroyProviders() {
        for (EventScheduleProvider provider : mScheduleEventProviders) {
            provider.removeScheduleChangeListener(mScheduleChangeListener);
            IOUtils.closeQuietly(provider);
        }
        mScheduleEventProviders.clear();
        mScheduleEventProviders = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_REPARSE_CALENDAR.equals(action)) {
                reparseCalendar();
                syncIfDirty();
            } else if (ACTION_SYNC_IF_DIRTY.equals(action)) {
                syncIfDirty();
            }
        }
    }

    private void handleScheduleChanged(EventScheduleProvider provider) {
        SyncService.notifyCalendarChanged(this);
    }

    private void reparseCalendar() {
        int today = JulianDay.today();

        mSchedules = new ArrayList<>();
        for (int i = 0; i < DAYS_TO_SYNC; ++i) {
            int scheduleDay = today + i;

            EventSchedule schedule = new EventSchedule(scheduleDay);
            for (EventScheduleProvider provider : mScheduleEventProviders) {
                Collection<Event> events = provider.getEventsForDay(scheduleDay);
                schedule.addEvents(events);
            }
            mSchedules.add(schedule);
        }
    }

    private void syncIfDirty() {
        if (mSchedules == null) reparseCalendar();

        if (!PebbleKit.isWatchConnected(this)) {
            Log.w(TAG, "Sync cancelled as Pebble isn't connected");
            // TODO Queue future sync.
            return;
        }

        for (EventSchedule schedule : mSchedules) {
            final boolean scheduleIsDirty = differentFromCache(schedule);

            if (scheduleIsDirty) {
                final int slot = schedule.getJulianDay() % 4;

                // TODO Put in library.
                PebbleDictionary packet = new PebbleDictionary();
                packet.addUint8(0, (byte) 1);
                packet.addUint32(1, slot);
                packet.addBytes(2, schedule.toByteArray());

                Log.i(TAG, "Sending updated schedule for JD " + schedule.getJulianDay() + " to slot #" + slot + "...");
                final long startTime = System.nanoTime();
                PebbleSocket.MessageStatus status = mSocket.send(packet, 5000);
                final long endTime = System.nanoTime();

                switch (status) {
                    case ACK:
                        Log.v(TAG, "...acknowledged after " + (endTime - startTime) / 1000000 + "ms");
                        cacheSchedule(schedule);
                        break;

                    default:
                        Log.w(TAG, "...failed with status " + status);
                        // TODO Queue future resend.
                        break;
                }
            } else {
                Log.v(TAG, "Skipping schedule for JD " + schedule.getJulianDay() + " as it has not changed");
            }
        }
    }

    private boolean differentFromCache(EventSchedule schedule) {
        final File cachedFile = new File(getFilesDir(), schedule.getJulianDay() + ".schedule");

        if (!cachedFile.exists()) return true;

        final byte[] newData = schedule.toByteArray();
        if (cachedFile.length() != newData.length) return true;

        InputStream fileStream = null;
        byte[] block = new byte[512];
        try {
            fileStream = new FileInputStream(cachedFile);

            int bytesRead;
            while ((bytesRead = fileStream.read(block)) > 0) {
                for (int i = 0; i < bytesRead; ++i) {
                    if (block[i] != newData[i]) return true;
                }
            }
        } catch (IOException e) {
            return true;
        } finally {
            IOUtils.closeQuietly(fileStream);
        }

        return false;
    }

    private void cacheSchedule(EventSchedule schedule) {
        final File cachedFile = new File(getFilesDir(), schedule.getJulianDay() + ".schedule");
        cachedFile.delete();

        FileOutputStream fileStream = null;
        try {
            fileStream = new FileOutputStream(cachedFile);

            fileStream.write(schedule.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, "Failed to save cached schedule", e);
        } finally {
            IOUtils.closeQuietly(fileStream);
        }

        deleteExpiredCachedSchedules();
    }

    private void deleteExpiredCachedSchedules() {
        final int todaysJulianDay = JulianDay.today();

        final File[] toDelete = getFilesDir().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                final String[] parts = pathname.getName().split("\\.");

                // Only accept *.schedule files.
                if (parts.length != 2 || !parts[1].equals("schedule")) return false;

                // Only delete schedules older than today.
                return Integer.parseInt(parts[0]) < todaysJulianDay;
            }
        });

        if (toDelete.length > 0) {
            for (File file : toDelete) {
                file.delete();
            }

            Log.v(TAG, "Deleted " + toDelete.length + " expired cached schedule[s]");
        }
    }

    public static void notifyCalendarChanged(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_REPARSE_CALENDAR);
        context.startService(intent);
    }

    public static void notifyPebbleConnected(Context context) {
        Intent intent = new Intent(context, SyncService.class);
        intent.setAction(ACTION_SYNC_IF_DIRTY);
        context.startService(intent);
    }

    public static ServiceConnection bindService(Context context) {
        ServiceConnection genericConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

            @Override
            public void onServiceDisconnected(ComponentName componentName) {}
        };

        Intent syncServiceIntent = new Intent(context, SyncService.class);
        context.bindService(syncServiceIntent, genericConnection, Context.BIND_AUTO_CREATE);

        return genericConnection;
    }
}
