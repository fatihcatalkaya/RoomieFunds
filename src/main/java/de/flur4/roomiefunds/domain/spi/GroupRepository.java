package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.group.CreateGroupDto;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.group.UpdateGroupDto;
import de.flur4.roomiefunds.models.person.Person;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {
    List<Group> getAllGroups();
    Optional<Group> getGroupById(long groupId);
    Group createGroup(CreateGroupDto dto);
    Group updateGroup(long groupId, UpdateGroupDto dto);
    void deleteGroup(long groupId);
    List<Group> getGroupsForPerson(long personId);
    List<Person> getPersonsInGroup(long groupId);
    void addPersonToGroup(long personId, long groupId);
    void removePersonFromGroup(long personId, long groupId);
}
