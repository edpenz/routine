package nz.edpe.routine.provider.routine;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;

import org.apache.commons.collections4.IteratorUtils;

import java.util.Date;
import java.util.List;

import nz.edpe.routine.OvalOutlineProvider;
import nz.edpe.routine.R;
import nz.edpe.routine.SyncService;
import nz.edpe.routine.db.CursorIterable;
import nz.edpe.routine.provider.routine.db.RoutineDB;
import nz.edpe.routine.provider.routine.db.RoutineRow;
import nz.edpe.routine.provider.routine.db.RoutineTable;

public class RoutineAdapter extends BaseAdapter {
    private final Context mContext;
    private final RoutineTable mRoutineTable;

    private java.text.DateFormat mDateFormat;

    private int mColorPrimary;

    private String[] mPriorityStatuses;
    private int[] mPriorityValues;
    private TypedArray mPriorityDrawables;

    private String mRoutineDefaultName;
    private String[] mRoutineNames;
    private int[] mRoutineValues;

    private List<RoutineRow> mCalendars;

    public RoutineAdapter(Context context) {
        mContext = context;
        mRoutineTable = RoutineDB.open(mContext).getRoutineTable();

        loadResources();
        loadRoutines();
    }

    private void loadResources() {
        mDateFormat = DateFormat.getTimeFormat(mContext);

        final Resources res = mContext.getResources();
        mColorPrimary = res.getColor(R.color.primary);

        mPriorityStatuses = res.getStringArray(R.array.priority_statuses);
        mPriorityValues = res.getIntArray(R.array.priority_values);
        mPriorityDrawables = res.obtainTypedArray(R.array.priority_drawables);

        mRoutineDefaultName = res.getString(R.string.custom_series);
        mRoutineNames = res.getStringArray(R.array.custom_series_names);
        mRoutineValues = res.getIntArray(R.array.custom_series_values);
    }

    private void loadRoutines() {
        CursorIterable<RoutineRow> routines = mRoutineTable.iterateAll();
        try {
            mCalendars = IteratorUtils.toList(routines.iterator());
        } finally {
            routines.closeCursor();
        }
    }

    @Override
    public int getCount() {
        return mCalendars.size();
    }

    @Override
    public RoutineRow getItem(int position) {
        return mCalendars.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mCalendars.get(position).RowId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) convertView = newView(parent, position);
        bindView(convertView, position);
        return convertView;
    }

    public void addNewRoutine() {
        RoutineRow newCalendar = new RoutineRow();
        mRoutineTable.save(newCalendar);

        mCalendars.add(newCalendar);
        notifyDataSetChanged();

        SyncService.notifyCalendarChanged(mContext);
    }

    private View newView(ViewGroup parent, final int position) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);

        final View view = inflater.inflate(R.layout.listitem_series_custom, parent, false);
        final ViewTag tag = new ViewTag(view);
        view.setTag(tag);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tag.Delete.setOutlineProvider(new OvalOutlineProvider(mContext, 8));
            tag.Delete.setClipToOutline(true);
        }

        tag.Priority.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleClick(tag);
            }
        });

        tag.Delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDeleteClick(tag);
            }
        });

        tag.Begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleBeginTimeClick(tag);
            }
        });

        tag.End.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleEndTimeClick(tag);
            }
        });

        for (int i = 0; i < tag.DaysOfWeek.getChildCount(); ++i) {
            final CheckBox checkBox = (CheckBox) tag.DaysOfWeek.getChildAt(i);
            final int dayMask = Integer.parseInt(checkBox.getTag().toString());

            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleDayChecked(tag, dayMask, checkBox.isChecked());
                }
            });
        }

        return view;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void bindView(View view, int position) {
        final ViewTag tag = (ViewTag) view.getTag();
        tag.Position = position;
        final RoutineRow calendar = getItem(position);

        int priorityIndex = 0;
        for (int i = 0; i < mPriorityValues.length; i++) {
            if (mPriorityValues[i] == calendar.Priority) {
                priorityIndex = i;
                break;
            }
        }

        tag.Bullet.setBackground(mPriorityDrawables.getDrawable(priorityIndex));
        tag.Bullet.getBackground().setColorFilter(mColorPrimary, PorterDuff.Mode.MULTIPLY);

        tag.Title.setText(getSeriesName(calendar.DaysOfWeek));
        tag.Status.setText(mPriorityStatuses[priorityIndex]);

        tag.Begin.setText(formatTime(calendar.BeginTime / 60, calendar.BeginTime % 60));
        tag.End.setText(formatTime(calendar.EndTime / 60, calendar.EndTime % 60));

        for (int i = 0; i < tag.DaysOfWeek.getChildCount(); ++i) {
            CheckBox checkBox = (CheckBox) tag.DaysOfWeek.getChildAt(i);
            final int dayMask = Integer.parseInt(checkBox.getTag().toString());

            checkBox.setChecked((calendar.DaysOfWeek & dayMask) != 0);
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

    private void handlePriorityClick(ViewTag tag, int priorityIndex) {
        tag.Status.setText(mPriorityStatuses[priorityIndex]);

        tag.Bullet.setBackground(mPriorityDrawables.getDrawable(priorityIndex));
        tag.Bullet.getBackground().setColorFilter(mColorPrimary, PorterDuff.Mode.MULTIPLY);

        RoutineRow item = getItem(tag.Position);
        item.Priority = mPriorityValues[priorityIndex];
        mRoutineTable.update(item);

        SyncService.notifyCalendarChanged(mContext);
    }

    private void handleDeleteClick(ViewTag tag) {
        RoutineRow item = getItem(tag.Position);
        mRoutineTable.delete(item);

        mCalendars.remove(tag.Position);
        notifyDataSetChanged();

        SyncService.notifyCalendarChanged(mContext);
    }

    private void handleBeginTimeClick(final ViewTag tag) {
        RoutineRow item = getItem(tag.Position);

        TimePickerDialog dialog = new TimePickerDialog(mContext, R.style.AppTheme_Dialog, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                handleBeginTimeSet(tag, hourOfDay, minute);
            }
        }, item.BeginTime / 60, item.BeginTime % 60, false);
        dialog.show();
    }

    private void handleBeginTimeSet(ViewTag tag, int hourOfDay, int minute) {
        tag.Begin.setText(formatTime(hourOfDay, minute));

        RoutineRow item = getItem(tag.Position);
        item.BeginTime = hourOfDay * 60 + minute;
        mRoutineTable.update(item);

        SyncService.notifyCalendarChanged(mContext);
    }

    private void handleEndTimeClick(final ViewTag tag) {
        RoutineRow item = getItem(tag.Position);

        TimePickerDialog dialog = new TimePickerDialog(mContext, R.style.AppTheme_Dialog, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                handleEndTimeSet(tag, hourOfDay, minute);
            }
        }, item.EndTime / 60, item.EndTime % 60, false);
        dialog.show();
    }

    private void handleEndTimeSet(ViewTag tag, int hourOfDay, int minute) {
        tag.End.setText(formatTime(hourOfDay, minute));

        RoutineRow item = getItem(tag.Position);
        item.EndTime = hourOfDay * 60 + minute;
        mRoutineTable.update(item);

        SyncService.notifyCalendarChanged(mContext);
    }

    private void handleDayChecked(ViewTag tag, int dayMask, boolean isChecked) {
        RoutineRow item = getItem(tag.Position);
        if (isChecked) item.DaysOfWeek |= dayMask;
        else item.DaysOfWeek &= ~dayMask;
        mRoutineTable.update(item);

        tag.Title.setText(getSeriesName(item.DaysOfWeek));

        SyncService.notifyCalendarChanged(mContext);
    }

    private String getSeriesName(int dayMask) {
        for (int i = 0; i < mRoutineValues.length; i++) {
            if (mRoutineValues[i] == dayMask) return mRoutineNames[i];
        }
        return mRoutineDefaultName;
    }

    private String formatTime(int hourOfDay, int minute) {
        Date date = new Date();
        date.setHours(hourOfDay);
        date.setMinutes(minute);

        return mDateFormat.format(date);
    }

    private static class ViewTag {
        public int Position;

        public final View Bullet;
        public final TextView Title;
        public final TextView Status;
        public final View Priority;
        public final View Delete;
        public final TextView Begin;
        public final TextView End;
        public final ViewGroup DaysOfWeek;

        public ViewTag(View view) {
            Bullet = view.findViewById(R.id.bullet);
            Title = (TextView) view.findViewById(R.id.title);
            Status = (TextView) view.findViewById(R.id.calendar_status);
            Priority = view.findViewById(R.id.priority);
            Delete = view.findViewById(R.id.delete);
            Begin = (TextView) view.findViewById(R.id.begin);
            End = (TextView) view.findViewById(R.id.end);
            DaysOfWeek = (ViewGroup) view.findViewById(R.id.days_of_week);
        }
    }
}
