package nz.edpe.routine.schedule;

import java.io.Closeable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class EventScheduleProvider implements Closeable {
    private final Set<ScheduleChangeListener> mScheduleChangeListeners = new HashSet<>();

    public abstract Collection<Event> getEventsForDay(int julianDay);

    protected void notifyScheduleChanged() {
        for (ScheduleChangeListener listener : mScheduleChangeListeners) {
            listener.onScheduleChanged(this);
        }
    }

    public void addScheduleChangeListener(ScheduleChangeListener listener) {
        mScheduleChangeListeners.add(listener);
    }

    public void removeScheduleChangeListener(ScheduleChangeListener listener) {
        mScheduleChangeListeners.remove(listener);
    }

    public interface ScheduleChangeListener {
        void onScheduleChanged(EventScheduleProvider provider);
    }
}
