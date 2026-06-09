package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.domain.api.hbcisync.impl.HbciSyncService;
import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.domain.spi.HbciConfigRepository;
import de.flur4.roomiefunds.domain.spi.HbciSyncLogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Dependent
public class HbciSyncContext {

    @ConfigProperty(name = "app.hbci.initial-lookback-days", defaultValue = "90")
    int initialLookbackDays;

    @Produces
    @ApplicationScoped
    public GetHbciConfig getHbciConfig(HbciConfigRepository configRepo,
                                       AccountIbanRepository ibanRepo,
                                       HbciClient hbciClient,
                                       TransactionRepository txRepo,
                                       HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SaveHbciConfig saveHbciConfig(HbciConfigRepository configRepo,
                                         AccountIbanRepository ibanRepo,
                                         HbciClient hbciClient,
                                         TransactionRepository txRepo,
                                         HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }

    @Produces
    @ApplicationScoped
    public SyncBankTransactions syncBankTransactions(HbciConfigRepository configRepo,
                                                      AccountIbanRepository ibanRepo,
                                                      HbciClient hbciClient,
                                                      TransactionRepository txRepo,
                                                      HbciSyncLogRepository logRepo) {
        return new HbciSyncService(configRepo, ibanRepo, logRepo, hbciClient, txRepo, initialLookbackDays);
    }
}
