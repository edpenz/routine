package nz.edpe.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BroadcastDispatcher extends BroadcastReceiver {
    public static final String TAG = BroadcastDispatcher.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_PROVIDER_CHANGED.equals(action) && "com.android.calendar".equals(intent.getData().getHost())) {
            Log.v(TAG, "Detected calendar change");
            SyncService.notifyCalendarChanged(context);
        } else if ("com.getpebble.action.PEBBLE_CONNECTED".equals(action)) {
            Log.v(TAG, "Detected Pebble reconnect");
            SyncService.notifyPebbleConnected(context);
        }
    }
}
