package com.github.tumbl3w33d.users;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.security.authz.AbstractReadOnlyAuthorizationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.role.Role;

import com.github.tumbl3w33d.OAuth2ProxyRealm;
import com.github.tumbl3w33d.h2.OAuth2ProxyRoleStore;
import com.github.tumbl3w33d.users.db.OAuth2ProxyRole;

@Named(OAuth2ProxyRealm.NAME)
@Singleton
public class OAuth2ProxyAuthorizationManager extends AbstractReadOnlyAuthorizationManager {

    private final OAuth2ProxyRoleStore roleStore;

    @Inject
    public OAuth2ProxyAuthorizationManager(final OAuth2ProxyRoleStore roleStore) {
        this.roleStore = roleStore;
    }

    @Override
    public String getSource() {
        return OAuth2ProxyRealm.NAME;
    }

    @Override
    public Set<Role> listRoles() {
        return roleStore.getAllRoles();
    }

    @Override
    public Role getRole(String roleId) {
        Optional<OAuth2ProxyRole> maybeRole = roleStore.getRole(roleId);
        if (maybeRole.isPresent()) {
            return maybeRole.get().toNexusRole();
        }
        return null;
    }

    @Override
    public Set<Privilege> listPrivileges() {
        return null;
    }

    @Override
    public Privilege getPrivilege(String privilegeId) throws NoSuchPrivilegeException {
        return null;
    }

    @Override
    public Privilege getPrivilegeByName(String privilegeName) throws NoSuchPrivilegeException {
        return null;
    }

    @Override
    public List<Privilege> getPrivileges(Set<String> privilegeIds) {
        return Collections.emptyList();
    }

    @Override
    public String getRealmName() {
        return OAuth2ProxyRealm.NAME;
    }

}
