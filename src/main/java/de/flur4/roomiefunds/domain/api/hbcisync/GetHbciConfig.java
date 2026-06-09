package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.HbciConfig;

import java.util.List;
import java.util.Optional;

public interface GetHbciConfig {
    List<HbciConfig> getConfigs();
    Optional<HbciConfig> getConfigByAccountId(long accountId);
    List<AccountIban> getIbans();
}
