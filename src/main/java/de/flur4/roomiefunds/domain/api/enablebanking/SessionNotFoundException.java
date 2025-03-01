package de.flur4.roomiefunds.domain.api.enablebanking;

public class SessionNotFoundException extends Exception {
    public SessionNotFoundException(long sessionId) {
        super("Could not find enable banking session with id %d".formatted(sessionId));
    }
}
