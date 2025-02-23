package de.flur4.roomiefunds.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.person.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.person.CreatePersonDto;
import de.flur4.roomiefunds.models.person.Person;
import de.flur4.roomiefunds.models.person.UpdatePersonDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/person")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class PersonController {
    final GetPerson getPerson;
    final CreatePerson createPerson;
    final UpdatePerson updatePerson;
    final DeletePerson deletePerson;
    final JsonWebToken jwt;

    @GET
    public List<Person> getPersons() {
        return getPerson.getPersons();
    }

    @GET
    @Path("/{personId}:\\d+")
    public Person getPerson(@PathParam("personId") long personId) {
        var result = getPerson.getPerson(personId);
        if(result.isEmpty()) {
            throw new NotFoundException("Person with id " + personId + " not found");
        }
        return result.get();
    }

    @POST
    public Person createPerson(@Valid CreatePersonDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createPerson.createPerson(modifyingPerson, dto);
        } catch (Exception e) {
            log.error("An error occurred while creating person", e);
            throw new InternalServerErrorException("An error occurred while creating person", e);
        }
    }

    @PATCH
    @Path("/{personId}:\\d+")
    public Person patchPerson(@PathParam("personId") long personId, @Valid UpdatePersonDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updatePerson.updatePerson(modifyingPerson, personId, dto);
        } catch (PersonNotFoundException e) {
            throw new NotFoundException("Person with id " + personId + " not found");
        } catch (JsonProcessingException e) {
            log.error("An error occurred while updating person", e);
            throw new InternalServerErrorException("An error occurred while updating person", e);
        }
    }

    @DELETE
    @Path("/{personId}:\\d+")
    public void deletePerson(@PathParam("personId") long personId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deletePerson.deletePerson(modifyingPerson, personId);
        } catch (PersonNotFoundException e) {
            throw new NotFoundException("Person with id " + personId + " not found");
        } catch (PersonUndeletableException e) {
            throw new ClientErrorException(e.getMessage(), Response.Status.CONFLICT);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while delete person", e);
            throw new InternalServerErrorException("An error occurred while delete person", e);
        }
    }
}
