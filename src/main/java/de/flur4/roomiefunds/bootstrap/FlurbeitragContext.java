package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.flurbeitrag.GetFlurbeitrag;
import de.flur4.roomiefunds.domain.api.flurbeitrag.SetFlurbeitrag;
import de.flur4.roomiefunds.domain.api.flurbeitrag.impl.FlurbeitragService;
import de.flur4.roomiefunds.domain.spi.FlurbeitragRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class FlurbeitragContext {
    @Produces
    @ApplicationScoped
    public GetFlurbeitrag getFlurbeitrag(FlurbeitragRepository flurbeitragRepository, LogRepository logRepository) {
        return new FlurbeitragService(flurbeitragRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public SetFlurbeitrag setFlurbeitrag(FlurbeitragRepository flurbeitragRepository, LogRepository logRepository) {
        return new FlurbeitragService(flurbeitragRepository, logRepository);
    }
}
