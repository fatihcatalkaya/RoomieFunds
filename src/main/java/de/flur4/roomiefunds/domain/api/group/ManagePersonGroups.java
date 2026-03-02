package de.flur4.roomiefunds.domain.api.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.person.Person;

import java.util.List;

public interface ManagePersonGroups {
    List<Group> getGroupsForPerson(long personId);
    List<Person> getPersonsInGroup(long groupId);
    void addPersonToGroup(ModifyingPersonDto modifyingPerson, long personId, long groupId) throws JsonProcessingException;
    void removePersonFromGroup(ModifyingPersonDto modifyingPerson, long personId, long groupId) throws JsonProcessingException;
}
