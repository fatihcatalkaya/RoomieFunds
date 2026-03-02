package de.flur4.roomiefunds.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.group.*;
import de.flur4.roomiefunds.infrastructure.Utils;
import de.flur4.roomiefunds.models.group.CreateGroupDto;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.group.UpdateGroupDto;
import de.flur4.roomiefunds.models.person.Person;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/group")
@RolesAllowed({"roomiefunds-admin"})
@JBossLog
@RequiredArgsConstructor
public class GroupController {
    final GetGroup getGroup;
    final CreateGroup createGroup;
    final UpdateGroup updateGroup;
    final DeleteGroup deleteGroup;
    final ManagePersonGroups managePersonGroups;
    final JsonWebToken jwt;

    @GET
    public List<Group> getGroups() {
        return getGroup.getGroups();
    }

    @GET
    @Path("/{groupId:\\d+}")
    public Group getGroup(@PathParam("groupId") long groupId) {
        var result = getGroup.getGroup(groupId);
        if (result.isEmpty()) {
            throw new NotFoundException("Group with id " + groupId + " not found");
        }
        return result.get();
    }

    @POST
    public Group createGroup(@Valid CreateGroupDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return createGroup.createGroup(modifyingPerson, dto);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while creating group", e);
            throw new InternalServerErrorException("An error occurred while creating group", e);
        }
    }

    @PATCH
    @Path("/{groupId:\\d+}")
    public Group patchGroup(@PathParam("groupId") long groupId, @Valid UpdateGroupDto dto) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            return updateGroup.updateGroup(modifyingPerson, groupId, dto);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while updating group", e);
            throw new InternalServerErrorException("An error occurred while updating group", e);
        }
    }

    @DELETE
    @Path("/{groupId:\\d+}")
    public void deleteGroup(@PathParam("groupId") long groupId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            deleteGroup.deleteGroup(modifyingPerson, groupId);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while deleting group", e);
            throw new InternalServerErrorException("An error occurred while deleting group", e);
        }
    }

    @GET
    @Path("/{groupId:\\d+}/persons")
    public List<Person> getPersonsInGroup(@PathParam("groupId") long groupId) {
        return managePersonGroups.getPersonsInGroup(groupId);
    }

    @POST
    @Path("/{groupId:\\d+}/persons")
    public void addPersonToGroup(@PathParam("groupId") long groupId, AddPersonToGroupRequest request) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            managePersonGroups.addPersonToGroup(modifyingPerson, request.personId(), groupId);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while adding person to group", e);
            throw new InternalServerErrorException("An error occurred while adding person to group", e);
        }
    }

    @DELETE
    @Path("/{groupId:\\d+}/persons/{personId:\\d+}")
    public void removePersonFromGroup(@PathParam("groupId") long groupId, @PathParam("personId") long personId) {
        var modifyingPerson = Utils.createModifyingPersonDtoFromJwt(jwt);
        try {
            managePersonGroups.removePersonFromGroup(modifyingPerson, personId, groupId);
        } catch (JsonProcessingException e) {
            log.error("An error occurred while removing person from group", e);
            throw new InternalServerErrorException("An error occurred while removing person from group", e);
        }
    }

    public record AddPersonToGroupRequest(long personId) {}
}
