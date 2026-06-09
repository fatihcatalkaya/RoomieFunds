package de.flur4.roomiefunds.infrastructure.hbci;

import de.flur4.roomiefunds.domain.spi.HbciClient;
import de.flur4.roomiefunds.models.hbci.DateRange;
import de.flur4.roomiefunds.models.hbci.HbciCredentials;
import de.flur4.roomiefunds.models.hbci.HbciFetchResult;
import de.flur4.roomiefunds.models.hbci.HbciSyncException;
import de.flur4.roomiefunds.models.hbci.HbciTransactionEntry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@ApplicationScoped
@JBossLog
public class HbciClientImpl implements HbciClient {
    private String currentBlz;
    private String currentUsername;
    private String currentPin;

    @PostConstruct
    void init() {
        HBCIUtils.init(new Properties(), new HbciCallback());
    }

    @Override
    public synchronized HbciFetchResult fetchTransactions(HbciCredentials credentials, DateRange dateRange) {
        this.currentBlz = credentials.blz();
        this.currentUsername = credentials.username();
        this.currentPin = credentials.decryptedPin();

        File passportFile = null;
        try {
            passportFile = File.createTempFile("hbci-passport-", ".dat");
            if (credentials.passportBytes() != null && credentials.passportBytes().length > 0) {
                Files.write(passportFile.toPath(), credentials.passportBytes());
            }

            HBCIUtils.setParam("client.passport.default", "PinTan");
            HBCIUtils.setParam("client.passport.PinTan.init", "1");

            HBCIPassport passport = AbstractHBCIPassport.getInstance(passportFile);
            passport.setCountry("DE");
            BankInfo info = HBCIUtils.getBankInfo(currentBlz);
            if (info == null) {
                throw new HbciSyncException("Unknown BLZ: " + currentBlz);
            }
            passport.setHost(info.getPinTanAddress());
            passport.setPort(443);
            passport.setFilterType("Base64");

            HBCIHandler handle = null;
            try {
                handle = new HBCIHandler(HBCIVersion.HBCI_300.getId(), passport);

                HBCIJob umsatzJob = handle.newJob("KUmsAllCamt");
                if (passport.getAccounts() == null || passport.getAccounts().length == 0) {
                    throw new HbciSyncException("No bank accounts found in HBCI passport for BLZ: " + currentBlz);
                }
                umsatzJob.setParam("my", passport.getAccounts()[0]);
                umsatzJob.setParam("startdate", toDate(dateRange.from()));
                umsatzJob.setParam("enddate", toDate(dateRange.to()));
                umsatzJob.addToQueue();

                HBCIExecStatus status = handle.execute();
                if (!status.isOK()) {
                    throw new HbciSyncException("HBCI execution failed: " + status.toString());
                }

                GVRKUms result = (GVRKUms) umsatzJob.getJobResult();
                if (!result.isOK()) {
                    throw new HbciSyncException("HBCI transaction fetch failed: " + result.toString());
                }

                List<HbciTransactionEntry> entries = new ArrayList<>();
                for (GVRKUms.UmsLine buchung : result.getFlatData()) {
                    if (buchung.id == null || buchung.id.isBlank()) {
                        log.warnf("Skipping HBCI entry with no CAMT ID on %s", buchung.valuta);
                        continue;
                    }
                    int amountCents = buchung.value != null
                            ? buchung.value.getBigDecimalValue().multiply(BigDecimal.valueOf(100)).intValue()
                            : 0;
                    String iban = (buchung.other != null) ? buchung.other.iban : null;
                    String usage = (buchung.usage != null && !buchung.usage.isEmpty())
                            ? String.join(" ", buchung.usage)
                            : "";
                    LocalDate valueDate = buchung.valuta.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    entries.add(new HbciTransactionEntry(buchung.id, valueDate, amountCents, iban, usage));
                }

                byte[] updatedPassport = Files.readAllBytes(passportFile.toPath());
                return new HbciFetchResult(entries, updatedPassport);
            } finally {
                if (handle != null) handle.close();
                passport.close();
            }
        } catch (HbciSyncException e) {
            throw e;
        } catch (HBCI_Exception e) {
            throw new HbciSyncException("HBCI communication failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new HbciSyncException("Passport I/O failed: " + e.getMessage(), e);
        } finally {
            if (passportFile != null) passportFile.delete();
        }
    }

    private Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private class HbciCallback extends AbstractHBCICallback {
        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace) {
            HbciClientImpl.this.log.debugf("HBCI: %s", msg);
        }

        @Override
        public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
            switch (reason) {
                case NEED_PASSPHRASE_LOAD, NEED_PASSPHRASE_SAVE ->
                        retData.replace(0, retData.length(), currentPin);
                case NEED_PT_PIN ->
                        retData.replace(0, retData.length(), currentPin);
                case NEED_BLZ ->
                        retData.replace(0, retData.length(), currentBlz);
                case NEED_USERID, NEED_CUSTOMERID ->
                        retData.replace(0, retData.length(), currentUsername);
                case NEED_PT_DECOUPLED ->
                        HbciClientImpl.this.log.info("HBCI: Waiting for pushTAN approval on phone...");
                case HAVE_ERROR ->
                        HbciClientImpl.this.log.errorf("HBCI error: %s", msg);
                default -> { }
            }
        }

        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {
        }
    }
}
