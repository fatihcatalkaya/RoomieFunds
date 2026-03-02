package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.group.*;
import de.flur4.roomiefunds.domain.api.group.impl.GroupService;
import de.flur4.roomiefunds.domain.spi.GroupRepository;
import de.flur4.roomiefunds.domain.spi.LogRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class GroupContext {
    @Produces
    @ApplicationScoped
    public GetGroup getGroup(GroupRepository groupRepository, LogRepository logRepository) {
        return new GroupService(groupRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public CreateGroup createGroup(GroupRepository groupRepository, LogRepository logRepository) {
        return new GroupService(groupRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public UpdateGroup updateGroup(GroupRepository groupRepository, LogRepository logRepository) {
        return new GroupService(groupRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public DeleteGroup deleteGroup(GroupRepository groupRepository, LogRepository logRepository) {
        return new GroupService(groupRepository, logRepository);
    }

    @Produces
    @ApplicationScoped
    public ManagePersonGroups managePersonGroups(GroupRepository groupRepository, LogRepository logRepository) {
        return new GroupService(groupRepository, logRepository);
    }
}
