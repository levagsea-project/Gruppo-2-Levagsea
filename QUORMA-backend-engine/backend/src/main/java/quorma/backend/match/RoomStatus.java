package quorma.backend.match;

/** Lifecycle of a room: it is open until someone joins it, it expires or its creator closes it. */
public enum RoomStatus {
    OPEN,
    MATCHED,
    EXPIRED,
    CANCELLED
}
