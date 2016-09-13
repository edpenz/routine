package nz.edpe.routine.schedule;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EventSchedule {
    private final int mJulianDay;

    private List<Event> mEvents = new ArrayList<>();

    public EventSchedule(int julianDay) {
        mJulianDay = julianDay;
    }

    public void addEvents(Collection<Event> events) {
        mEvents.addAll(events);
        normalize();
    }

    public void write(DataOutputStream stream) throws IOException {
        stream.writeInt(mJulianDay);
        for (Event event : mEvents) {
            event.write(stream);
        }
        stream.write(0);
    }

    public byte[] toByteArray() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(buffer);
        try {
            write(writer);
        } catch (IOException e) {
            return null;
        }
        return buffer.toByteArray();
    }

    public int getJulianDay() {
        return mJulianDay;
    }

    private void normalize() {
        removeInvisibleEvents();
        orderEvents();
    }

    private void removeInvisibleEvents() {
        for (Iterator<Event> it = mEvents.iterator(); it.hasNext(); ) {
            Event event = it.next();
            if (event.getPriority() == EventType.NONE) {
                it.remove();
            }
        }
    }

    private void orderEvents() {
        Collections.sort(mEvents);
    }
}
