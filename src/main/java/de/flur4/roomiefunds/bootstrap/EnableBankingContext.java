package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.enablebanking.DeleteSession;
import de.flur4.roomiefunds.domain.api.enablebanking.FinishSession;
import de.flur4.roomiefunds.domain.api.enablebanking.GetSession;
import de.flur4.roomiefunds.domain.api.enablebanking.SynchronizeTransactions;
import de.flur4.roomiefunds.domain.api.enablebanking.impl.EnableBankingService;
import de.flur4.roomiefunds.domain.spi.EnableBankingRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.webclient.enablebanking.EnableBankingClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class EnableBankingContext {
    @Produces
    @ApplicationScoped
    public GetSession getSession(@RestClient EnableBankingClient client, EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(client, enableBankingRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public FinishSession finishSession(@RestClient EnableBankingClient client,EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(client, enableBankingRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteSession deleteSession(@RestClient EnableBankingClient client, EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(client, enableBankingRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public SynchronizeTransactions synchronizeTransactions(@RestClient EnableBankingClient client, EnableBankingRepository enableBankingRepository, LogRepository logRepository) {
        return new EnableBankingService(client, enableBankingRepository, logRepository);
    }
}
