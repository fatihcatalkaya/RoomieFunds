package de.flur4.roomiefunds.models.webclient.enablebanking;

public enum TransactionFetchStrategy {
    DEFAULT,
    LONGEST;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
