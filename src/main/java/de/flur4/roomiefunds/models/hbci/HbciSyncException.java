package de.flur4.roomiefunds.models.hbci;

public class HbciSyncException extends RuntimeException {
    public HbciSyncException(String message) {
        super(message);
    }

    public HbciSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
