package de.flur4.roomiefunds.bootstrap;

import de.flur4.roomiefunds.domain.api.keycloaksync.FullKeycloakSync;
import de.flur4.roomiefunds.domain.api.keycloaksync.SyncGroupsFromKeycloak;
import de.flur4.roomiefunds.domain.api.keycloaksync.SyncPersonToKeycloak;
import de.flur4.roomiefunds.domain.api.keycloaksync.impl.KeycloakSyncService;
import de.flur4.roomiefunds.domain.spi.GroupRepository;
import de.flur4.roomiefunds.domain.spi.KeycloakUserService;
import de.flur4.roomiefunds.domain.spi.PersonRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class KeycloakSyncContext {

    @ConfigProperty(name = "app.keycloak.sync.floor-member-group")
    String floorMemberGroupName;

    @ConfigProperty(name = "app.keycloak.sync.admin-group")
    String adminGroupName;

    @ConfigProperty(name = "app.keycloak.sync.attribute.room")
    String roomAttributeName;

    @ConfigProperty(name = "app.keycloak.sync.attribute.is-current-tenant")
    String isCurrentTenantAttributeName;

    @Produces
    @ApplicationScoped
    public SyncPersonToKeycloak syncPersonToKeycloak(PersonRepository personRepository,
                                                      GroupRepository groupRepository,
                                                      KeycloakUserService keycloakUserService) {
        return new KeycloakSyncService(personRepository, groupRepository, keycloakUserService,
                floorMemberGroupName, adminGroupName, roomAttributeName, isCurrentTenantAttributeName);
    }

    @Produces
    @ApplicationScoped
    public SyncGroupsFromKeycloak syncGroupsFromKeycloak(PersonRepository personRepository,
                                                          GroupRepository groupRepository,
                                                          KeycloakUserService keycloakUserService) {
        return new KeycloakSyncService(personRepository, groupRepository, keycloakUserService,
                floorMemberGroupName, adminGroupName, roomAttributeName, isCurrentTenantAttributeName);
    }

    @Produces
    @ApplicationScoped
    public FullKeycloakSync fullKeycloakSync(PersonRepository personRepository,
                                              GroupRepository groupRepository,
                                              KeycloakUserService keycloakUserService) {
        return new KeycloakSyncService(personRepository, groupRepository, keycloakUserService,
                floorMemberGroupName, adminGroupName, roomAttributeName, isCurrentTenantAttributeName);
    }
}
