package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ExchangeRate(CurrencyCode unitCurrency,
                           String exchangeRate,
                           RateType rateType,
                           String contractIdentification,
                           AmountType instructedAmount) {
}
