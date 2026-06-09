package de.flur4.roomiefunds.domain.api.hbcisync.impl;

import de.flur4.roomiefunds.domain.api.hbcisync.GetHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SaveHbciConfig;
import de.flur4.roomiefunds.domain.api.hbcisync.SyncBankTransactions;
import de.flur4.roomiefunds.domain.spi.AccountIbanRepository;
import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.domain.spi.HbciConfigRepository;
import de.flur4.roomiefunds.domain.spi.HbciSyncLogRepository;
import de.flur4.roomiefunds.domain.spi.TransactionRepository;
import de.flur4.roomiefunds.models.hbci.AccountIban;
import de.flur4.roomiefunds.models.hbci.CreateAccountIbanDto;
import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciConfig;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciSyncResult;
import de.flur4.roomiefunds.models.hbci.HbciTransactionEntry;
import de.flur4.roomiefunds.models.hbci.SaveHbciConfigDto;
import de.flur4.roomiefunds.models.transaction.CreateTransactionDto;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class HbciSyncService implements GetHbciConfig, SaveHbciConfig, SyncBankTransactions {
    private final HbciConfigRepository hbciConfigRepository;
    private final AccountIbanRepository accountIbanRepository;
    private final HbciSyncLogRepository syncLogRepository;
    private final HbciClient hbciClient;
    private final TransactionRepository transactionRepository;
    private final int initialLookbackDays;

    @Override
    public List<HbciConfig> getConfigs() {
        return hbciConfigRepository.findAll();
    }

    @Override
    public Optional<HbciConfig> getConfigByAccountId(long accountId) {
        return hbciConfigRepository.findByAccountId(accountId);
    }

    @Override
    public List<AccountIban> getIbans() {
        return accountIbanRepository.findAll();
    }

    @Override
    public HbciConfig createConfig(SaveHbciConfigDto dto) {
        return hbciConfigRepository.createConfig(dto);
    }

    @Override
    public void updateConfig(long accountId, SaveHbciConfigDto dto) {
        hbciConfigRepository.updateConfig(accountId, dto);
    }

    @Override
    public void deleteConfig(long accountId) {
        hbciConfigRepository.deleteByAccountId(accountId);
    }

    @Override
    public AccountIban addIban(CreateAccountIbanDto dto) {
        return accountIbanRepository.save(dto);
    }

    @Override
    public void deleteIban(long id) {
        accountIbanRepository.deleteById(id);
    }

    @Override
    public HbciSyncResult sync(long accountId) {
        var credentials = hbciConfigRepository.loadCredentials(accountId)
                .orElseThrow(() -> new HbciSyncException("No HBCI config found for account " + accountId));

        DateRange dateRange = buildDateRange(credentials.lastSyncedAt());

        HbciFetchResult fetchResult;
        try {
            fetchResult = hbciClient.fetchTransactions(credentials, dateRange);
        } catch (Exception e) {
            syncLogRepository.saveLog(accountId, 0, 0, false, e.getMessage());
            throw e instanceof HbciSyncException ? (HbciSyncException) e
                    : new HbciSyncException("HBCI fetch failed", e);
        }

        hbciConfigRepository.savePassportBytes(accountId, fetchResult.updatedPassportBytes());

        int imported = 0, skipped = 0;
        try {
            for (var entry : fetchResult.entries()) {
                if (entry.amountCents() <= 0) { skipped++; continue; }
                if (transactionRepository.camtIdExists(entry.camtId())) { skipped++; continue; }
                if (entry.counterpartyIban() == null) { skipped++; continue; }
                var sourceAccountId = accountIbanRepository.findAccountByIban(entry.counterpartyIban());
                if (sourceAccountId.isEmpty()) { skipped++; continue; }
                transactionRepository.createTransactionWithCamtId(
                        new CreateTransactionDto(sourceAccountId.get(), accountId,
                                entry.amountCents(), entry.valueDate(), entry.usage()),
                        entry.camtId());
                imported++;
            }
            hbciConfigRepository.updateLastSyncedAt(accountId, OffsetDateTime.now());
            syncLogRepository.saveLog(accountId, imported, skipped, true, null);
        } catch (Exception e) {
            syncLogRepository.saveLog(accountId, imported, skipped, false, e.getMessage());
            throw e;
        }
        return new HbciSyncResult(imported, skipped);
    }

    private DateRange buildDateRange(OffsetDateTime lastSyncedAt) {
        LocalDate to = LocalDate.now();
        LocalDate from = lastSyncedAt != null
                ? lastSyncedAt.toLocalDate().minusDays(3)
                : LocalDate.now().minusDays(initialLookbackDays);
        return new DateRange(from, to);
    }
}
