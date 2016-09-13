package nz.edpe.routine.ui;

import android.app.Activity;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.commonsware.cwac.merge.MergeAdapter;

import nz.edpe.routine.R;
import nz.edpe.routine.SyncService;
import nz.edpe.routine.provider.calendar.CalendarSeriesAdapter;
import nz.edpe.routine.provider.routine.RoutineAdapter;

public class ConfigureCalendarsActivity extends Activity {
    public static final String TAG = ConfigureCalendarsActivity.class.getSimpleName();

    private ListView mSeriesListView;

    private CalendarSeriesAdapter mCalendarSeriesAdapter;
    private RoutineAdapter mRoutineAdapter;
    private ServiceConnection mSyncService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_calendars);
        bindViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_configure_calendars, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.add_custom_calendar) {
            mRoutineAdapter.addNewRoutine();
            mSeriesListView.smoothScrollToPosition(mSeriesListView.getAdapter().getCount() - 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SyncService.notifyCalendarChanged(this);
        mSyncService = SyncService.bindService(this);
    }

    @Override
    protected void onStop() {
        unbindService(mSyncService);

        super.onStop();
    }

    private void bindViews() {
        mSeriesListView = (ListView) findViewById(R.id.calendar_list);

        mCalendarSeriesAdapter = new CalendarSeriesAdapter(this);
        mRoutineAdapter = new RoutineAdapter(this);

        MergeAdapter mergeAdapter = new MergeAdapter();
        mergeAdapter.addAdapter(mCalendarSeriesAdapter);
        mergeAdapter.addAdapter(mRoutineAdapter);

        mSeriesListView.setAdapter(mergeAdapter);
    }
}
