package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;

import java.util.List;
import java.util.Optional;

public interface AccountIbanRepository {
    List<AccountIban> findAll();
    Optional<Long> findAccountByIban(String iban);
    AccountIban save(CreateAccountIbanDto dto);
    void deleteById(long id);
}
