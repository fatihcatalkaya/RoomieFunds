package de.flur4.roomiefunds.domain.api.account;

public interface PrintAccountStatement {
    byte[] printAccountStatement(long accountId) throws AccountNotFoundException;
}
