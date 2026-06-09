package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;

public interface HbciClient {
    HbciFetchResult fetchTransactions(HbciCredentials credentials, DateRange dateRange);
}
