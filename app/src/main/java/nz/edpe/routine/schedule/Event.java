package nz.edpe.routine.schedule;

import java.io.DataOutputStream;
import java.io.IOException;

public class Event implements Comparable<Event> {
    public static final Event EMPTY_EVENT = new Event(EventType.NONE, 0, 0, 0);

    private final byte mPriority;
    private final byte mValue;
    private final byte mStartHours, mStartMinutes;
    private final byte mEndHours, mEndMinutes;

    public Event(EventType priority, int value, int startHours, int startMinutes, int endHours, int endMinutes) {
        this(priority, value, (startHours * 60 + startMinutes) * 60 * 1000, (endHours * 60 + endMinutes) * 60 * 1000);
    }

    public Event(EventType priority, int value, long startOffsetMillis, long endOffsetMillis) {
        mPriority = priority.getCode();
        mValue = (byte) value;

        final long startOffsetTotalMinutes = Math.max(0, startOffsetMillis / 1000 / 60);
        mStartMinutes = (byte) (startOffsetTotalMinutes % 60);
        mStartHours = (byte) (startOffsetTotalMinutes / 60);

        final long endOffsetTotalMinutes = Math.min(endOffsetMillis / 1000 / 60, 24 * 60);
        mEndMinutes = (byte) (endOffsetTotalMinutes % 60);
        mEndHours = (byte) (endOffsetTotalMinutes / 60);
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeByte(mPriority);
        stream.writeByte(mValue);
        stream.writeShort(mStartHours * 60 + mStartMinutes);
        stream.writeShort(mEndHours * 60 + mEndMinutes);
    }

    public EventType getPriority() {
        return EventType.fromCode(mPriority);
    }

    public int getStartOffsetMillis() {
        return (mStartHours * 60 + mStartMinutes) * 60 * 1000;
    }

    public int getEndOffsetMillis() {
        return (mEndHours * 60 + mEndMinutes) * 60 * 1000;
    }

    @Override
    public int compareTo(Event another) {
        if (mPriority != another.mPriority) {
            return another.mPriority - mPriority;
        } else if (getStartOffsetMillis() != another.getStartOffsetMillis()) {
            return getStartOffsetMillis() - another.getStartOffsetMillis();
        } else if (getEndOffsetMillis() != another.getEndOffsetMillis()) {
            return getEndOffsetMillis() - another.getEndOffsetMillis();
        } else {
            return 0;
        }
    }

}
