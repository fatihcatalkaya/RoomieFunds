package de.flur4.roomiefunds.domain.api.enablebanking;

public class SessionExpiredException extends Exception {
    public SessionExpiredException(long sessionId) {
        super("Session %d has expired".formatted(sessionId));
    }
}
