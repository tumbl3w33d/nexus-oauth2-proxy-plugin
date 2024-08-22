package com.github.tumbl3w33d.h2;

import static com.github.tumbl3w33d.h2.OAuth2ProxyStores.roleDAO;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.security.role.DuplicateRoleException;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.github.tumbl3w33d.users.db.OAuth2ProxyRole;
import com.google.common.collect.ImmutableSet;

@Named("mybatis")
@Singleton
public class OAuth2ProxyRoleStore extends StateGuardLifecycleSupport implements TransactionalStore<DataSession<?>> {

    private final DataSessionSupplier sessionSupplier;

    @Inject
    public OAuth2ProxyRoleStore(
            final DataSessionSupplier sessionSupplier) {
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public DataSession<?> openSession() {
        return OAuth2ProxyStores.openSession(sessionSupplier);
    }

    @Transactional
    public Set<Role> getAllRoles() {
        Set<Role> allRoles = StreamSupport.stream(roleDAO().browse().spliterator(), false)
                .map(OAuth2ProxyRole::toNexusRole).collect(Collectors.toSet());

        return ImmutableSet.copyOf(allRoles);
    }

    @Transactional
    public Optional<OAuth2ProxyRole> getRole(String roleId) {
        return roleDAO().read(roleId);
    }

    @Transactional
    private void addRole(String newRoleId) {
        if (newRoleId == null || newRoleId.trim().isEmpty()) {
            throw new RuntimeException("cannot create role without name - received: " + newRoleId);
        }

        try {
            roleDAO().create(OAuth2ProxyRole.of(newRoleId));
        } catch (DuplicateKeyException e) {
            throw new DuplicateRoleException(newRoleId);
        }
    }

    @Transactional
    public void addRolesIfMissing(Set<RoleIdentifier> idpGroups) {
        Set<String> existingRoles = getAllRoles().stream().map(existingRole -> existingRole.getName())
                .collect(Collectors.toSet());
        Set<String> idpGroupStrings = idpGroups.stream().map(idpGroup -> idpGroup.getRoleId())
                .collect(Collectors.toSet());
        Set<String> newGroups = idpGroupStrings.stream().filter(s -> !existingRoles.contains(s))
                .collect(Collectors.toSet());

        newGroups.forEach(newGroup -> addRole(newGroup));
    }

    @Transactional
    public void deleteRole(String roleIdToDelete) {
        roleDAO().delete(roleIdToDelete);

        log.info("deleted role {}", roleIdToDelete);
    }

}
