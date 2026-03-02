package de.flur4.roomiefunds.domain.api.group;

import de.flur4.roomiefunds.models.group.Group;
import java.util.List;
import java.util.Optional;

public interface GetGroup {
    List<Group> getGroups();
    Optional<Group> getGroup(long groupId);
}
