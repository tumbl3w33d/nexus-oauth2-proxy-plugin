package com.github.tumbl3w33d.users.db;

import java.io.Serializable;
import java.util.Objects;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.security.role.Role;
import org.sonatype.nexus.security.role.RoleIdentifier;

import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;

public class OAuth2ProxyRole extends AbstractEntity implements Comparable<OAuth2ProxyRole>, Serializable {

    private String name;

    public String toString() {
        return "OAuth2ProxyRole(" + name + ")";
    }

    @Override
    public int compareTo(OAuth2ProxyRole o) {
        if (o == null) {
            return 1;
        }
        return getName().compareTo(o.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        OAuth2ProxyRole other = (OAuth2ProxyRole) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static OAuth2ProxyRole of(RoleIdentifier nexusRole) {
        OAuth2ProxyRole role = new OAuth2ProxyRole();
        role.setName(nexusRole.getRoleId());
        return role;
    }

    public static OAuth2ProxyRole of(String roleName) {
        OAuth2ProxyRole role = new OAuth2ProxyRole();
        role.setName(roleName);
        return role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role toNexusRole() {
        Role nexusRole = new Role();
        nexusRole.setRoleId(getName());
        nexusRole.setSource(OAuth2ProxyUserManager.SOURCE);
        nexusRole.setName(getName());
        // nexusRole.setReadOnly(true);
        nexusRole.setDescription("identity provider role '" + getName() + "'");

        return nexusRole;
    }

}
