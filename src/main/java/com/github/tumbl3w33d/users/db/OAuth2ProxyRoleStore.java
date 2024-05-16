package com.github.tumbl3w33d.users.db;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.transaction.OrientTransactional;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;

import com.github.tumbl3w33d.OAuth2ProxyDatabase;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

@Named
@Singleton
public class OAuth2ProxyRoleStore extends ComponentSupport {

    private final Provider<DatabaseInstance> databaseProvider;
    private final OAuth2ProxyRoleEntityAdapter entityAdapter;

    @Inject
    public OAuth2ProxyRoleStore(
            @Named(OAuth2ProxyDatabase.NAME) final Provider<DatabaseInstance> databaseProvider,
            final OAuth2ProxyRoleEntityAdapter entityAdapter) {
        this.databaseProvider = databaseProvider;
        this.entityAdapter = entityAdapter;

        init();
    }

    private void init() {
        DatabaseInstance databaseInstance = databaseProvider.get();

        try (ODatabaseDocumentTx db = databaseInstance.acquire()) {
            entityAdapter.register(db);
        } catch (Exception e) {
            log.error("unabled to create schema for OAuth2ProxyRoleStore - {}", e);
        }
    }

    public Set<Role> getAllRoles() {
        Set<Role> roles = OrientTransactional.inTx(this.databaseProvider)
                .call(db -> StreamSupport.stream(this.entityAdapter.browse(db).spliterator(), false)
                        .map(OAuth2ProxyRole::toNexusRole).collect(Collectors.toSet()));

        return ImmutableSet.copyOf(roles);
    }

    public void addRole(OAuth2ProxyRole role) {
        if (role.getName() == null || role.getName() == "") {
            throw new RuntimeException("cannot create role without name - received: " + role.getName());
        }
        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> entityAdapter.addEntity(db, role));

        log.debug("added role {} to db", role.getName());
    }

    public void addRolesIfMissing(Set<RoleIdentifier> idpGroups) {
        Set<String> existingRoles = getAllRoles().stream().map(existingRole -> existingRole.getName())
                .collect(Collectors.toSet());
        Set<String> idpGroupStrings = idpGroups.stream().map(idpGroup -> idpGroup.getRoleId())
                .collect(Collectors.toSet());
        Set<String> newGroups = idpGroupStrings.stream().filter(s -> !existingRoles.contains(s))
                .collect(Collectors.toSet());

        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> {
                    newGroups.forEach(newGroup -> entityAdapter.addEntity(db,
                            OAuth2ProxyRole.of(newGroup)));
                    return null;
                });
    }

    public void deleteRole(OAuth2ProxyRole roleToDelete) {
        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> {
                    entityAdapter.deleteEntity(db, roleToDelete);
                    return null;
                });

        log.info("deleted role {}", roleToDelete);
    }

    public Role getRole(String roleId) {
        try (ODatabaseDocumentTx con = databaseProvider.get().acquire()) {
            return entityAdapter.convertToOAuth2ProxyRole(entityAdapter.getEntityByName(con, roleId)).toNexusRole();
        } catch (Exception e) {
            log.warn("unable to retrieve role with id {}", roleId);
            return null;
        }
    }

}
