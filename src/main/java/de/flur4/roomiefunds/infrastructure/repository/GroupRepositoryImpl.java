package de.flur4.roomiefunds.infrastructure.repository;

import de.flur4.roomiefunds.domain.spi.GroupRepository;
import de.flur4.roomiefunds.models.group.CreateGroupDto;
import de.flur4.roomiefunds.models.group.Group;
import de.flur4.roomiefunds.models.group.UpdateGroupDto;
import de.flur4.roomiefunds.models.person.Person;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.Optional;

import static de.flur4.roomiefunds.infrastructure.jooq.Tables.*;
import static org.jooq.Records.mapping;

@ApplicationScoped
@RequiredArgsConstructor
public class GroupRepositoryImpl implements GroupRepository {

    final DSLContext jooq;

    @Override
    public List<Group> getAllGroups() {
        return jooq.select(GROUP.ID, GROUP.NAME, GROUP.KEYCLOAK_GROUP_ID)
                .from(GROUP)
                .orderBy(GROUP.NAME)
                .fetch(mapping(Group::new));
    }

    @Override
    public Optional<Group> getGroupById(long groupId) {
        return jooq.select(GROUP.ID, GROUP.NAME, GROUP.KEYCLOAK_GROUP_ID)
                .from(GROUP)
                .where(GROUP.ID.eq(groupId))
                .fetchOptional(mapping(Group::new));
    }

    @Override
    public Group createGroup(CreateGroupDto dto) {
        return jooq.insertInto(GROUP)
                .columns(GROUP.NAME, GROUP.KEYCLOAK_GROUP_ID)
                .values(dto.name(), dto.keycloakGroupId())
                .returningResult(GROUP.ID, GROUP.NAME, GROUP.KEYCLOAK_GROUP_ID)
                .fetchOne(mapping(Group::new));
    }

    @Override
    public Group updateGroup(long groupId, UpdateGroupDto dto) {
        var group = jooq.selectFrom(GROUP).where(GROUP.ID.eq(groupId)).fetchOne();
        assert group != null;
        dto.name().ifPresent(group::setName);
        dto.keycloakGroupId().ifPresent(group::setKeycloakGroupId);
        group.store();
        return new Group(group.getId(), group.getName(), group.getKeycloakGroupId());
    }

    @Override
    public void deleteGroup(long groupId) {
        jooq.deleteFrom(GROUP).where(GROUP.ID.eq(groupId)).execute();
    }

    @Override
    public List<Group> getGroupsForPerson(long personId) {
        return jooq.select(GROUP.ID, GROUP.NAME, GROUP.KEYCLOAK_GROUP_ID)
                .from(GROUP)
                .join(PERSON_GROUP).on(PERSON_GROUP.GROUP_ID.eq(GROUP.ID))
                .where(PERSON_GROUP.PERSON_ID.eq(personId))
                .orderBy(GROUP.NAME)
                .fetch(mapping(Group::new));
    }

    @Override
    public List<Person> getPersonsInGroup(long groupId) {
        return jooq.select(
                        PERSON.ID, PERSON.FIRST_NAME, PERSON.LAST_NAME, PERSON.ROOM,
                        PERSON.PAYS_FLOOR_FEES, PERSON.ACCOUNT_ID,
                        PERSON.PRINT_ON_PRODUCT_TALLY_LIST, PERSON.EMAIL,
                        PERSON.EMAIL_ACCOUNT_STATEMENT, PERSON.KEYCLOAK_USER_ID)
                .from(PERSON)
                .join(PERSON_GROUP).on(PERSON_GROUP.PERSON_ID.eq(PERSON.ID))
                .where(PERSON_GROUP.GROUP_ID.eq(groupId))
                .orderBy(PERSON.FIRST_NAME)
                .fetch(mapping(Person::new));
    }

    @Override
    public void addPersonToGroup(long personId, long groupId) {
        jooq.insertInto(PERSON_GROUP)
                .columns(PERSON_GROUP.PERSON_ID, PERSON_GROUP.GROUP_ID)
                .values(personId, groupId)
                .onConflictDoNothing()
                .execute();
    }

    @Override
    public void removePersonFromGroup(long personId, long groupId) {
        jooq.deleteFrom(PERSON_GROUP)
                .where(PERSON_GROUP.PERSON_ID.eq(personId))
                .and(PERSON_GROUP.GROUP_ID.eq(groupId))
                .execute();
    }
}
