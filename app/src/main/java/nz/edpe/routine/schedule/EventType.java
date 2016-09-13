package nz.edpe.routine.schedule;

public enum EventType {
    NONE(0),
    PRIORITY_BACKGROUND(0x10), PRIORITY_LOW(0x11), PRIORITY_MEDIUM(0x12), PRIORITY_HIGH(0x13),
    WEATHER(0x20),
    ASTRONOMY(0x30);

    private final byte mCode;

    EventType(int value) {
        mCode = (byte) value;
    }

    public byte getCode() {
        return mCode;
    }

    public static EventType fromCode(int code) {
        for (EventType priority : EventType.values()) {
            if (priority.getCode() == code) return priority;
        }
        throw new IllegalArgumentException("Unknown code 0x" + Integer.toHexString(code));
    }

    public static EventType fromCode(int code, EventType defaultType) {
        for (EventType priority : EventType.values()) {
            if (priority.getCode() == code) return priority;
        }
        return defaultType;
    }
}
