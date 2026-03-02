package de.flur4.roomiefunds.domain.api.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;
import de.flur4.roomiefunds.models.group.CreateGroupDto;
import de.flur4.roomiefunds.models.group.Group;

public interface CreateGroup {
    Group createGroup(ModifyingPersonDto modifyingPerson, CreateGroupDto dto) throws JsonProcessingException;
}
