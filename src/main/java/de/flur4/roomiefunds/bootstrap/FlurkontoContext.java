package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.flurkonto.GetFlurkonto;
import de.flur4.roomiefunds.domain.api.flurkonto.SetFlurkonto;
import de.flur4.roomiefunds.domain.api.flurkonto.impl.FlurkontoService;
import de.flur4.roomiefunds.domain.spi.AccountRepository;
import de.flur4.roomiefunds.domain.spi.FlurkontoRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class FlurkontoContext {
    @Produces
    @ApplicationScoped
    public GetFlurkonto getFlurkonto(FlurkontoRepository flurkontoRepository, AccountRepository accountRepository, LogRepository logRepository) {
        return new FlurkontoService(flurkontoRepository, accountRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public SetFlurkonto setFlurkonto(FlurkontoRepository flurkontoRepository, AccountRepository accountRepository, LogRepository logRepository) {
        return new FlurkontoService(flurkontoRepository, accountRepository, logRepository);
    }
}
