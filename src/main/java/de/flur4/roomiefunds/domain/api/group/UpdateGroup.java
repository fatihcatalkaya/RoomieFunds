package de.flur4.roomiefunds.domain.api.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.group.UpdateGroupDto;
import de.flur4.roomiefunds.models.group.Group;

public interface UpdateGroup {
    Group updateGroup(ModifyingPersonDto modifyingPerson, long groupId, UpdateGroupDto dto) throws JsonProcessingException;
}
