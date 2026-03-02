package de.flur4.roomiefunds.domain.spi;

import de.flur4.roomiefunds.models.keycloak.KeycloakUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KeycloakUserService {
    Optional<KeycloakUser> getUserBySubject(String keycloakUserId);
    Optional<KeycloakUser> getUserByUsername(String username);
    String createUser(String username, String firstName, String lastName, String email, Map<String, List<String>> attributes);
    void updateUser(String keycloakUserId, String firstName, String lastName, String email, Map<String, List<String>> attributes);
    List<String> getUserGroupIds(String keycloakUserId);
    void addUserToGroup(String keycloakUserId, String groupId);
    void removeUserFromGroup(String keycloakUserId, String groupId);
    List<KeycloakUser> getUsersInGroup(String groupId);
    Optional<String> getGroupIdByName(String groupName);
}
