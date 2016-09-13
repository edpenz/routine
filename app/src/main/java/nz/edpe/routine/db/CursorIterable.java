package nz.edpe.routine.db;

import android.database.Cursor;

import java.util.Iterator;

public abstract class CursorIterable<T> implements Iterable<T> {
    private final Cursor mCursor;

    public CursorIterable(Cursor cursor) {
        mCursor = cursor;
    }

    protected abstract T getItem(Cursor row);

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            boolean mHasNext = mCursor.moveToFirst();

            @Override
            public boolean hasNext() {
                return mHasNext;
            }

            @Override
            public T next() {
                T item = getItem(mCursor);
                mHasNext = mCursor.moveToNext();
                return item;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("Remove not supported");
            }
        };
    }

    public void closeCursor() {
        mCursor.close();
    }
}
