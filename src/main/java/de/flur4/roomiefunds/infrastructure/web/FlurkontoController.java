package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.account.AccountNotFoundException;
import de.flur4.roomiefunds.domain.api.flurkonto.GetFlurkonto;
import de.flur4.roomiefunds.domain.api.flurkonto.SetFlurkonto;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.account.Account;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Optional;

@Path("/api/flurkonto")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class FlurkontoController {

    final GetFlurkonto getFlurkonto;
    final SetFlurkonto setFlurkonto;
    final JsonWebToken jwt;

    @GET
    public Optional<Account> getFlurkonto() {
        return getFlurkonto.getFlurkonto();
    }

    @PUT
    public Account setFlurkonto(@NotNull @Positive Long flurkontoId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return setFlurkonto.setFlurkontoId(modifyingPerson, flurkontoId);
        } catch (AccountNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            log.error("An error occurred while updating flurkonto", e);
            throw new InternalServerErrorException("An error occurred while updating flurkonto", e);
        }
    }
}
