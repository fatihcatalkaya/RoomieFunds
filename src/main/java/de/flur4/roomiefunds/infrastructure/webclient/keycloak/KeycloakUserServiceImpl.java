package de.flur4.roomiefunds.infrastructure.webclient.keycloak;

import de.flur4.roomiefunds.domain.spi.KeycloakUserService;
import de.flur4.roomiefunds.models.keycloak.KeycloakUser;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@JBossLog
public class KeycloakUserServiceImpl implements KeycloakUserService {

    final Keycloak keycloak;
    final String realm;

    public KeycloakUserServiceImpl(Keycloak keycloak,
                                   @ConfigProperty(name = "app.keycloak.sync.realm") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    @Override
    public Optional<KeycloakUser> getUserBySubject(String keycloakUserId) {
        try {
            var user = keycloak.realm(realm).users().get(keycloakUserId).toRepresentation();
            return Optional.of(toKeycloakUser(user));
        } catch (jakarta.ws.rs.NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<KeycloakUser> getUserByUsername(String username) {
        var users = keycloak.realm(realm).users().searchByUsername(username, true);
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .map(this::toKeycloakUser);
    }

    @Override
    public String createUser(String username, String firstName, String lastName, String email,
                             Map<String, List<String>> attributes) {
        var user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setEnabled(true);
        user.setAttributes(attributes);

        try (var response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() == 201) {
                String location = response.getHeaderString("Location");
                return location.substring(location.lastIndexOf('/') + 1);
            }
            throw new RuntimeException("Failed to create Keycloak user: " + response.getStatus());
        }
    }

    @Override
    public void updateUser(String keycloakUserId, String firstName, String lastName, String email,
                           Map<String, List<String>> attributes) {
        var userResource = keycloak.realm(realm).users().get(keycloakUserId);
        var user = userResource.toRepresentation();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        if (user.getAttributes() != null) {
            user.getAttributes().putAll(attributes);
        } else {
            user.setAttributes(attributes);
        }
        userResource.update(user);
    }

    @Override
    public List<String> getUserGroupIds(String keycloakUserId) {
        return keycloak.realm(realm).users().get(keycloakUserId).groups()
                .stream()
                .map(GroupRepresentation::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void addUserToGroup(String keycloakUserId, String groupId) {
        keycloak.realm(realm).users().get(keycloakUserId).joinGroup(groupId);
    }

    @Override
    public void removeUserFromGroup(String keycloakUserId, String groupId) {
        keycloak.realm(realm).users().get(keycloakUserId).leaveGroup(groupId);
    }

    @Override
    public List<KeycloakUser> getUsersInGroup(String groupId) {
        return keycloak.realm(realm).groups().group(groupId).members()
                .stream()
                .map(this::toKeycloakUser)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getGroupIdByName(String groupName) {
        return keycloak.realm(realm).groups().groups(groupName, 0, 1)
                .stream()
                .filter(g -> g.getName().equals(groupName))
                .map(GroupRepresentation::getId)
                .findFirst();
    }

    private KeycloakUser toKeycloakUser(UserRepresentation user) {
        return new KeycloakUser(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail());
    }
}
