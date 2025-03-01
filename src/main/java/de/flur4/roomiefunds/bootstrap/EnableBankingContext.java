package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.enablebanking.DeleteSession;
import de.flur4.roomiefunds.domain.api.enablebanking.FinishSession;
import de.flur4.roomiefunds.domain.api.enablebanking.GetSession;
import de.flur4.roomiefunds.domain.api.enablebanking.impl.EnableBankingService;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class EnableBankingContext {
    @Produces
    @ApplicationScoped
    public GetSession getSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(enableBankingRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public FinishSession finishSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(enableBankingRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteSession deleteSession(EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(enableBankingRepository, logRepository);
    }
}
