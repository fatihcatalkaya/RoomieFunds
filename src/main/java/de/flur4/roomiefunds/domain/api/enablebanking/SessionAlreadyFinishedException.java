package de.flur4.roomiefunds.domain.api.enablebanking;

public class SessionAlreadyFinishedException extends Exception {
    public SessionAlreadyFinishedException(long sessionId) {
        super("The session with id " + sessionId + " has already been finished");
    }
}
