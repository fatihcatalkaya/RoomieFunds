package de.flur4.roomiefunds.infrastructure.renderer.accountreport;

import de.flur4.roomiefunds.domain.spi.AccountReportRenderer;
import io.github.fatihcatalkaya.javatypst.JavaTypst;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@ApplicationScoped
public class TypstAccountReportRenderer implements AccountReportRenderer {

    @Location("pdf/account_report.typ")
    Template accountReportTemplate;

    @Override
    public byte[] getAccountReport() {



        // Now we can render the typst template
        String typstTemplate = accountReportTemplate
//                .data("persons", persons)
//                .data("test", )
                .render();

        return JavaTypst.render(typstTemplate);
    }
}
