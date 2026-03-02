package de.flur4.roomiefunds.domain.api.group.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.domain.api.group.*;
import de.flur4.roomiefunds.domain.spi.GroupRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import de.flur4.roomiefunds.infrastructure.jooq.enums.LogOperations;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.group.CreateGroupDto;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.group.UpdateGroupDto;
import de.flur4.roomiefunds.models.log.InsertLogEntryDto;
import de.flur4.roomiefunds.models.person.Person;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class GroupService implements GetGroup, CreateGroup, UpdateGroup, DeleteGroup, ManagePersonGroups {
    final GroupRepository groupRepository;
    final LogRepository logRepository;

    @Override
    public List<Group> getGroups() {
        return groupRepository.getAllGroups();
    }

    @Override
    public Optional<Group> getGroup(long groupId) {
        return groupRepository.getGroupById(groupId);
    }

    @Override
    public Group createGroup(ModifyingPersonDto modifyingPerson, CreateGroupDto dto) throws JsonProcessingException {
        Group group = groupRepository.createGroup(dto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create, "group", Optional.empty(), Optional.of(group)
        ));
        return group;
    }

    @Override
    public Group updateGroup(ModifyingPersonDto modifyingPerson, long groupId, UpdateGroupDto dto) throws JsonProcessingException {
        var before = groupRepository.getGroupById(groupId);
        Group updated = groupRepository.updateGroup(groupId, dto);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.update, "group", Optional.of(before), Optional.of(updated)
        ));
        return updated;
    }

    @Override
    public void deleteGroup(ModifyingPersonDto modifyingPerson, long groupId) throws JsonProcessingException {
        var group = groupRepository.getGroupById(groupId);
        groupRepository.deleteGroup(groupId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete, "group", Optional.of(group), Optional.empty()
        ));
    }

    @Override
    public List<Group> getGroupsForPerson(long personId) {
        return groupRepository.getGroupsForPerson(personId);
    }

    @Override
    public List<Person> getPersonsInGroup(long groupId) {
        return groupRepository.getPersonsInGroup(groupId);
    }

    @Override
    public void addPersonToGroup(ModifyingPersonDto modifyingPerson, long personId, long groupId) throws JsonProcessingException {
        groupRepository.addPersonToGroup(personId, groupId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.create, "person_group", Optional.empty(),
                Optional.of(new PersonGroupAssociation(personId, groupId))
        ));
    }

    @Override
    public void removePersonFromGroup(ModifyingPersonDto modifyingPerson, long personId, long groupId) throws JsonProcessingException {
        groupRepository.removePersonFromGroup(personId, groupId);
        logRepository.insertLogEntry(modifyingPerson, new InsertLogEntryDto(
                LogOperations.delete, "person_group",
                Optional.of(new PersonGroupAssociation(personId, groupId)), Optional.empty()
        ));
    }

    private record PersonGroupAssociation(long personId, long groupId) {}
}
