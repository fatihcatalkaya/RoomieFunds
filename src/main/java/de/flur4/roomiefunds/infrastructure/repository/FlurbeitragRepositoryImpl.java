package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.FlurbeitragRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.SETTINGS;

@ApplicationScoped
@RequiredArgsConstructor
public class FlurbeitragRepositoryImpl implements FlurbeitragRepository {
    final static String SETTINGS_FLURBEITRAG_AMOUNT = "flurbeitrag_amount";
    final DSLContext jooq;

    @Override
    public long getFlurbeitrag() {
        return jooq.select(SETTINGS.VALUE_INT)
                .from(SETTINGS)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURBEITRAG_AMOUNT))
                .fetchOne()
                .value1();
    }

    @Override
    public void setFlurbeitrag(long flurbeitrag) {
        jooq.update(SETTINGS)
                .set(SETTINGS.VALUE_INT, flurbeitrag)
                .where(SETTINGS.SETTING_KEY.eq(SETTINGS_FLURBEITRAG_AMOUNT))
                .execute();
    }
}
