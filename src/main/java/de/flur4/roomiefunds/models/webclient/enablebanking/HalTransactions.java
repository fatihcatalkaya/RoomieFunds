package de.flur4.roomiefunds.models.webclient.enablebanking;

import java.util.List;

public record HalTransactions(List<Transaction> transactions,
                              String continuationKey) {
}
