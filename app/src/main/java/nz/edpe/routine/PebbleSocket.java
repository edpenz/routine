package nz.edpe.routine;

import android.content.Context;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.Closeable;
import java.util.UUID;

public class PebbleSocket implements Closeable {
    private static final String TAG = PebbleSocket.class.getSimpleName();

    private final Context mContext;
    private final UUID mWatchappUuid;

    private MessageStatus mTransactionStatus;

    private final PebbleKit.PebbleAckReceiver mAckReceiver;
    private final PebbleKit.PebbleNackReceiver mNackReceiver;

    public PebbleSocket(Context context, UUID watchappUuid) {
        this.mContext = context;
        this.mWatchappUuid = watchappUuid;

        mAckReceiver = new AckReceiver(watchappUuid);
        mNackReceiver = new NackReceiver(watchappUuid);

        PebbleKit.registerReceivedAckHandler(context, mAckReceiver);
        PebbleKit.registerReceivedNackHandler(context, mNackReceiver);
    }

    @Override
    public void close() {
        mContext.unregisterReceiver(mAckReceiver);
        mContext.unregisterReceiver(mNackReceiver);
    }

    public MessageStatus send(PebbleDictionary data, long timeout) {
        final long timeoutTime = (System.nanoTime() / 1000000) + timeout;

        mTransactionStatus = MessageStatus.PENDING;

        PebbleKit.sendDataToPebble(mContext, mWatchappUuid, data);

        synchronized (this) {
            while (mTransactionStatus == MessageStatus.PENDING) {
                final long timeoutRemaining = timeoutTime - (System.nanoTime() / 1000000);

                if (timeoutRemaining <= 0) {
                    mTransactionStatus = MessageStatus.TIMEOUT;
                    break;
                }

                try {
                    wait(timeoutRemaining);
                } catch (InterruptedException e) {
                    return MessageStatus.INTERRUPTED;
                }
            }

            return mTransactionStatus;
        }
    }

    private class AckReceiver extends PebbleKit.PebbleAckReceiver {
        AckReceiver(UUID watchappUuid) {
            super(watchappUuid);
        }

        @Override
        public void receiveAck(Context context, int i) {
            synchronized (PebbleSocket.this) {
                mTransactionStatus = MessageStatus.ACK;
                PebbleSocket.this.notifyAll();
            }
        }
    }

    private class NackReceiver extends PebbleKit.PebbleNackReceiver {
        NackReceiver(UUID watchappUuid) {
            super(watchappUuid);
        }

        @Override
        public void receiveNack(Context context, int i) {
            synchronized (PebbleSocket.this) {
                mTransactionStatus = MessageStatus.NACK;
            }
        }
    }

    public enum MessageStatus {
        PENDING, NACK, ACK, TIMEOUT, INTERRUPTED;

        boolean succeeded() {
            return this == ACK;
        }
    }
}
