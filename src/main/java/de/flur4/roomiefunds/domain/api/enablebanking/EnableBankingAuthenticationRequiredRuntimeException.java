package de.flur4.roomiefunds.domain.api.enablebanking;

public class EnableBankingAuthenticationRequiredRuntimeException extends RuntimeException {
    private final String bankAccountUid;

    public EnableBankingAuthenticationRequiredRuntimeException(String bankAccountUid, Throwable cause) {
        super("Authentication required for bank account UID: " + bankAccountUid, cause);
        this.bankAccountUid = bankAccountUid;
    }

    public String getBankAccountUid() {
        return bankAccountUid;
    }
}
