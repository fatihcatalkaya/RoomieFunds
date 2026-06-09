package de.flur4.roomiefunds.domain.api.hbcisync;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;

public interface SaveHbciConfig {
    HbciConfig createConfig(SaveHbciConfigDto dto);
    void updateConfig(long accountId, SaveHbciConfigDto dto);
    void deleteConfig(long accountId);
    AccountIban addIban(CreateAccountIbanDto dto);
    void deleteIban(long id);
}
