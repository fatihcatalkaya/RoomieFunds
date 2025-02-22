package de.flur4.roomiefunds.infrastructure.web;

import de.flur4.roomiefunds.domain.api.flurbeitrag.GetFlurbeitrag;
import de.flur4.roomiefunds.domain.api.flurbeitrag.SetFlurbeitrag;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.flurbeitrag.Flurbeitrag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/flurbeitrag")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class FlurbeitragController {
    final GetFlurbeitrag getFlurbeitrag;
    final SetFlurbeitrag setFlurbeitrag;
    final JsonWebToken jwt;

    @GET
    public Flurbeitrag getFlurbeitrag() {
        return new Flurbeitrag(getFlurbeitrag.getFlurbeitrag());
    }

    @PUT
    public void setFlurbeitrag(@Valid Flurbeitrag flurbeitrag) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try{
            setFlurbeitrag.setFlurbeitrag(modifyingPerson, flurbeitrag.flurbeitrag());
        } catch (Exception e) {
            log.error("An error occurred while updating flurbeitrag", e);
            throw new InternalServerErrorException("An error occurred while updating flurbeitrag", e);
        }
    }
}
