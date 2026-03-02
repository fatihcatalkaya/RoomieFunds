package de.flur4.roomiefunds.domain.api.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.flur4.roomiefunds.models.common.ModifyingPersonDto;

public interface DeleteGroup {
    void deleteGroup(ModifyingPersonDto modifyingPerson, long groupId) throws JsonProcessingException;
}
