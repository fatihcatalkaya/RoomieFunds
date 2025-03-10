package de.flur4.roomiefunds.infrastructure.mailer;

import de.flur4.roomiefunds.domain.spi.AccountStatementMailer;
import de.flur4.roomiefunds.models.person.Person;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

@ApplicationScoped
public class AccountStatementMailerImpl implements AccountStatementMailer {

    @Inject
    Mailer mailer;

    @ConfigProperty(name = "app.email.replyto")
    String emailReplyto;


    @Override
    public void sendAccountStatement(Person person, byte[] accountStatementPdfBytes) {
        LocalDate today = LocalDate.now();
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String filename = "Account Statement - %s %d.pdf".formatted(monthName, today.getYear());

        String subject = "Your account statement for your floor account - %s %d".formatted(monthName, today.getYear());
        String body = String.format("""
                Hi there,
                
                to this email, I've attached your latest account statement as of %s %d. If you're currently
                owing any money, I'd like to kindly ask you to transfer the money as soon as possible. You can
                find the bank details in the WhatsApp group chat description.
                
                If there has been an error with your account, please contact the floor financier.
                
                Kind regards,
                The floor financier
                """, monthName, today.getYear());
        var mail = Mail
                .withText(person.email(), subject, body)
                .addAttachment(filename, accountStatementPdfBytes, "application/pdf")
                .setReplyTo(emailReplyto);
        mailer.send(mail);
    }
}
