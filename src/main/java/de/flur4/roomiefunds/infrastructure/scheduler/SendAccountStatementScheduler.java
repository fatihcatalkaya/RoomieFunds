package de.flur4.roomiefunds.infrastructure.scheduler;

import de.flur4.roomiefunds.domain.api.account.SendAccountStatements;
import de.flur4.roomiefunds.models.person.Person;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
@JBossLog
public class SendAccountStatementScheduler {
    final SendAccountStatements sendAccountStatements;

    @Scheduled(cron = "0 0 12 1 * ?")
    void sendScheduledAccountStatements() {
        log.info("Starting sending account statements");
        var sendResult = sendAccountStatements.sendAccountStatements();
        log.info("Finished sending account statements");
        if (!sendResult.successfulSendPersons().isEmpty()) {
            log.info("Sent emails successfully to: %s".formatted(sendResult.successfulSendPersons().stream().map(Person::email).collect(Collectors.joining(", "))));
        }
        if (!sendResult.failedPersons().isEmpty()) {
            for (var failed : sendResult.failedPersons()) {
                log.error("Failed to sent account statement email to %s".formatted(failed.getValue0().accountId()), failed.getValue1());
            }
        }
    }
}
