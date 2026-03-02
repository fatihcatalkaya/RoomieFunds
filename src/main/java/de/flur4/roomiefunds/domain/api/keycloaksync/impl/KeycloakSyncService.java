package de.flur4.roomiefunds.domain.api.keycloaksync.impl;

import de.flur4.roomiefunds.domain.api.keycloaksync.FullKeycloakSync;
import de.flur4.roomiefunds.domain.api.keycloaksync.SyncGroupsFromKeycloak;
import de.flur4.roomiefunds.domain.api.keycloaksync.SyncPersonToKeycloak;
import de.flur4.roomiefunds.domain.spi.GroupRepository;
import de.flur4.roomiefunds.domain.spi.KeycloakUserService;
import de.flur4.roomiefunds.domain.spi.PersonRepository;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.person.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@JBossLog
public class KeycloakSyncService implements SyncPersonToKeycloak, SyncGroupsFromKeycloak, FullKeycloakSync {
    final PersonRepository personRepository;
    final GroupRepository groupRepository;
    final KeycloakUserService keycloakUserService;
    final String floorMemberGroupName;
    final String adminGroupName;
    final String roomAttributeName;
    final String isCurrentTenantAttributeName;

    @Override
    public void syncPerson(long personId) {
        var personOpt = personRepository.getPersonById(personId);
        if (personOpt.isEmpty()) {
            log.warnf("Person %d not found, skipping sync", personId);
            return;
        }
        var person = personOpt.get();
        var groups = groupRepository.getGroupsForPerson(personId);

        // Build attributes
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(roomAttributeName, List.of(person.room()));
        attributes.put(isCurrentTenantAttributeName, List.of(String.valueOf(person.paysFloorFees())));

        String keycloakUserId = person.keycloakUserId();
        if (keycloakUserId == null || keycloakUserId.isBlank()) {
            // Create new KC user
            String username = generateUniqueUsername(person.firstName());
            keycloakUserId = keycloakUserService.createUser(
                    username, person.firstName(), person.lastName(), person.email(), attributes);
            personRepository.updatePersonKeycloakUserId(personId, keycloakUserId);
            log.infof("Created Keycloak user '%s' for person %d", username, personId);
        } else {
            // Update existing KC user
            keycloakUserService.updateUser(keycloakUserId, person.firstName(), person.lastName(), person.email(), attributes);
            log.infof("Updated Keycloak user for person %d", personId);
        }

        // Sync group memberships
        syncGroupMemberships(keycloakUserId, person, groups);
    }

    /**
     * Resolves a Group to its Keycloak group UUID.
     * Uses the stored keycloakGroupId if set, otherwise falls back to looking up by group name.
     */
    private Optional<String> resolveGroupToKeycloakId(Group group) {
        if (group.keycloakGroupId() != null && !group.keycloakGroupId().isBlank()) {
            return resolveKeycloakGroupId(group.keycloakGroupId());
        }
        // No keycloakGroupId stored — try to find KC group by name
        var resolved = keycloakUserService.getGroupIdByName(group.name());
        if (resolved.isEmpty()) {
            log.warnf("Could not find Keycloak group for DB group '%s'", group.name());
        }
        return resolved;
    }

    /**
     * Resolves a keycloak_group_id value to an actual Keycloak group UUID.
     * The stored value may already be a UUID or it may be a group name.
     */
    private Optional<String> resolveKeycloakGroupId(String keycloakGroupId) {
        try {
            UUID.fromString(keycloakGroupId);
            return Optional.of(keycloakGroupId);
        } catch (IllegalArgumentException e) {
            // Not a UUID — treat as group name and look up
            var resolved = keycloakUserService.getGroupIdByName(keycloakGroupId);
            if (resolved.isEmpty()) {
                log.warnf("Could not resolve Keycloak group name '%s' to a UUID", keycloakGroupId);
            }
            return resolved;
        }
    }

    private String generateUniqueUsername(String firstName) {
        String base = firstName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (base.isEmpty()) base = "user";
        String candidate = base;
        int counter = 2;
        while (keycloakUserService.getUserByUsername(candidate).isPresent()) {
            candidate = base + counter;
            counter++;
        }
        return candidate;
    }

    private void syncGroupMemberships(String keycloakUserId, Person person, List<Group> dbGroups) {
        // Collect desired KC group UUIDs from explicit DB group associations
        Set<String> desiredKcGroupIds = dbGroups.stream()
                .map(this::resolveGroupToKeycloakId)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toCollection(HashSet::new));

        // Add the implicit floor-member group directly from Keycloak if person pays floor fees
        if (person.paysFloorFees()) {
            keycloakUserService.getGroupIdByName(floorMemberGroupName)
                    .ifPresentOrElse(
                            desiredKcGroupIds::add,
                            () -> log.warnf("Floor-member KC group '%s' not found in Keycloak", floorMemberGroupName)
                    );
        }

        // Get current KC group IDs
        List<String> currentKcGroupIds = keycloakUserService.getUserGroupIds(keycloakUserId);

        // Add missing groups
        for (String groupId : desiredKcGroupIds) {
            if (!currentKcGroupIds.contains(groupId)) {
                keycloakUserService.addUserToGroup(keycloakUserId, groupId);
            }
        }

        // Remove stale groups (but never remove admin group)
        for (String groupId : currentKcGroupIds) {
            if (!desiredKcGroupIds.contains(groupId)) {
                keycloakUserService.removeUserFromGroup(keycloakUserId, groupId);
            }
        }
    }

    @Override
    public void syncGroupsFromKeycloak() {
        var allGroups = groupRepository.getAllGroups();
        for (var group : allGroups) {
            // Skip admin group to avoid locking out admins
            if (adminGroupName.equals(group.name())) {
                log.infof("Skipping backward sync for admin group '%s'", group.name());
                continue;
            }

            var resolvedGroupId = resolveGroupToKeycloakId(group);
            if (resolvedGroupId.isEmpty()) {
                continue;
            }

            var kcUsers = keycloakUserService.getUsersInGroup(resolvedGroupId.get());
            var dbPersons = groupRepository.getPersonsInGroup(group.id());
            Set<String> dbKeycloakUserIds = dbPersons.stream()
                    .map(Person::keycloakUserId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (var kcUser : kcUsers) {
                if (!dbKeycloakUserIds.contains(kcUser.id())) {
                    keycloakUserService.removeUserFromGroup(kcUser.id(), resolvedGroupId.get());
                    log.infof("Removed stale KC user '%s' from group '%s'", kcUser.username(), group.name());
                }
            }
        }
    }

    @Override
    public void fullSync() {
        log.info("Starting full Keycloak sync");

        // Forward sync: sync all persons that have group associations or pay floor fees
        var allPersons = personRepository.getAllPersons();
        for (var person : allPersons) {
            var groups = groupRepository.getGroupsForPerson(person.id());
            if (!groups.isEmpty() || person.paysFloorFees() || person.keycloakUserId() != null) {
                try {
                    syncPerson(person.id());
                } catch (Exception e) {
                    log.errorf(e, "Failed to sync person %d to Keycloak", person.id());
                }
            }
        }

        // Backward sync: clean up stale KC group memberships
        try {
            syncGroupsFromKeycloak();
        } catch (Exception e) {
            log.error("Failed backward sync from Keycloak", e);
        }

        log.info("Full Keycloak sync completed");
    }
}
