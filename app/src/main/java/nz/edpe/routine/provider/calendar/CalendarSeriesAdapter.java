package nz.edpe.routine.provider.calendar;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.provider.CalendarContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import nz.edpe.routine.R;
import nz.edpe.routine.SyncService;
import nz.edpe.routine.db.CursorIterable;
import nz.edpe.routine.provider.calendar.db.CalendarMappingDB;
import nz.edpe.routine.provider.calendar.db.CalendarMappingRow;
import nz.edpe.routine.provider.calendar.db.CalendarMappingTable;

public class CalendarSeriesAdapter extends CursorAdapter {
    private static String[] CALENDAR_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR};

    private final Context mContext;
    private final CalendarMappingTable mMappingTable;

    private final String[] mPriorityStatuses;
    private final int[] mPriorityValues;
    private final TypedArray mPriorityDrawables;

    private Map<Long, CalendarMappingRow> mCalendarConfigs;

    public CalendarSeriesAdapter(Context context) {
        super(context, context.getContentResolver().query(
                        CalendarContract.Calendars.CONTENT_URI, CALENDAR_PROJECTION,
                        null, null, null),
                FLAG_REGISTER_CONTENT_OBSERVER);

        mContext = context;
        mMappingTable = CalendarMappingDB.open(mContext).getMappingTable();

        final Resources res = mContext.getResources();
        mPriorityStatuses = res.getStringArray(R.array.priority_statuses);
        mPriorityValues = res.getIntArray(R.array.priority_values);
        mPriorityDrawables = res.obtainTypedArray(R.array.priority_drawables);

        loadCalendars();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);

        final View view = inflater.inflate(R.layout.listitem_series_calendar, parent, false);
        final ViewTag tag = new ViewTag(view);
        view.setTag(tag);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(tag);
            }
        });

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewTag tag = (ViewTag) view.getTag();
        tag.CalendarId = cursor.getLong(0);
        tag.CalendarName = cursor.getString(1);
        tag.CalendarColor = cursor.getInt(2);

        CalendarMappingRow calendarConfig = mCalendarConfigs.get(tag.CalendarId);
        int priorityIndex = 0;
        if (calendarConfig != null) {
            for (int i = 0; i < mPriorityValues.length; ++i) {
                if (calendarConfig.Priority == mPriorityValues[i]) {
                    priorityIndex = i;
                    break;
                }
            }
        }

        tag.Bullet.setBackground(mPriorityDrawables.getDrawable(priorityIndex));
        tag.Bullet.getBackground().setColorFilter(tag.CalendarColor, PorterDuff.Mode.MULTIPLY);

        tag.Name.setText(tag.CalendarName);

        tag.Status.setText(mPriorityStatuses[priorityIndex]);
    }

    private void loadCalendars() {
        mCalendarConfigs = new HashMap<>();

        CursorIterable<CalendarMappingRow> mappings = mMappingTable.iterateAll();
        try {
            for (CalendarMappingRow mapping : mappings) {
                mCalendarConfigs.put(mapping.CalendarId, mapping);
            }
        } finally {
            mappings.closeCursor();
        }
    }

    private void handleClick(final ViewTag tag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.title_priority_dialog)
                .setItems(R.array.priority_names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handlePriorityClick(tag, which);
                    }
                }).setNegativeButton(R.string.button_cancel, null)
                .show();
    }

    private void handlePriorityClick(ViewTag tag, int which) {
        tag.Status.setText(mPriorityStatuses[which]);

        tag.Bullet.setBackground(mPriorityDrawables.getDrawable(which));
        tag.Bullet.getBackground().setColorFilter(tag.CalendarColor, PorterDuff.Mode.MULTIPLY);

        CalendarMappingRow calendarConfig = mCalendarConfigs.get(tag.CalendarId);
        if (calendarConfig != null) {
            calendarConfig.Priority = mPriorityValues[which];
            mMappingTable.update(calendarConfig);
        } else {
            calendarConfig = new CalendarMappingRow(tag.CalendarId);
            mCalendarConfigs.put(tag.CalendarId, calendarConfig);

            calendarConfig.Priority = mPriorityValues[which];
            mMappingTable.save(calendarConfig);
        }

        SyncService.notifyCalendarChanged(mContext);
    }

    private static class ViewTag {
        public String CalendarName;
        public long CalendarId;
        public int CalendarColor;

        public final View Bullet;
        public final TextView Name;
        public final TextView Status;

        public ViewTag(View view) {
            Bullet = view.findViewById(R.id.bullet);
            Name = (TextView) view.findViewById(R.id.calendar_name);
            Status = (TextView) view.findViewById(R.id.calendar_status);
        }
    }
}
