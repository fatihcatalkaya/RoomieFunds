package de.flur4.roomiefunds.models.webclient.enablebanking;

public record ExchangeRate(CurrencyCode unitCurrency,
                           String exchangeRate,
                           RateType rateType,
                           String contractIdentification,
                           AmountType instructedAmount) {
}
