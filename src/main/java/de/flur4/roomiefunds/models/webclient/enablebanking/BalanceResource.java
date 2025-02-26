package de.flur4.roomiefunds.models.webclient.enablebanking;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BalanceResource(
        String name,
        AmountType balanceAmount,
        BalanceStatus balanceType,
        String lastChangeDateTime,
        String referenceDate,
        String lastCommittedTransaction
) {}