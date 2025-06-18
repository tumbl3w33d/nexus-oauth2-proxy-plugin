package com.github.tumbl3w33d.users.db;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.HasStringId;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserStatus;

import com.github.tumbl3w33d.users.IncompleteOAuth2ProxyUserDataException;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;

public class OAuth2ProxyUser extends AbstractEntity implements Comparable<OAuth2ProxyUser>, Serializable, HasStringId {

    private static final long serialVersionUID = 982589242389412L;
    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProxyUser.class.getName());

    private String preferred_username;
    private String email;
    private String apiToken;

    private Set<RoleIdentifier> groups;

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPreferredUsername() {
        return this.preferred_username;
    }

    public void setPreferredUsername(String preferred_username) {
        this.preferred_username = preferred_username;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public Set<RoleIdentifier> getGroups() {
        return Collections.unmodifiableSet(this.groups);
    }

    public void setGroups(String groupString) {
        this.groups = Stream.of(groupString.split(","))
                .map(group -> new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, group))
                .collect(Collectors.toSet());
    }

    public void setGroups(Set<RoleIdentifier> groups) {
        this.groups = groups;
    }

    public String toString() {
        return String.format("OAuth2ProxyUser(%s) [email: %s | apiToken: %s]", preferred_username, email,
                apiToken != null ? "<hidden>" : "null");
    }

    @Override
    public int compareTo(OAuth2ProxyUser o) {
        if (o == null) {
            return 1;
        }
        return getPreferredUsername().compareTo(o.getPreferredUsername());
    }

    public User toNexusUser() {
        User user = new User();
        user.setUserId(getPreferredUsername());
        logger.debug("user conversion set id {}", user.getUserId());

        String email = getEmail();
        if (email != null) {
            email = email.trim();
        }
        user.setEmailAddress(email);
        logger.debug("user conversion set email {}", user.getEmailAddress());

        Optional<String[]> maybeNameParts = getNameParts(getPreferredUsername());

        if (maybeNameParts.isPresent()) {
            String[] nameParts = maybeNameParts.get();
            logger.debug("user conversion about to set name parts {}", Arrays.toString(nameParts));
            user.setFirstName(nameParts[0]);
            user.setLastName(nameParts[1]);
            logger.debug("succeeded setting firstname {} and lastname {}", user.getFirstName(), user.getLastName());
        } else {
            throw new IncompleteOAuth2ProxyUserDataException(
                    "preferredUsername missing or in unexpected format - " + getPreferredUsername());
        }

        user.setSource(OAuth2ProxyUserManager.SOURCE);
        user.setStatus(UserStatus.active);
        user.addAllRoles(getGroups());
        logger.debug("set {} user {} active and added their groups as roles", OAuth2ProxyUserManager.SOURCE,
                user.getUserId());
        return user;
    }

    public static OAuth2ProxyUser of(User nexusUser) {
        if (nexusUser.getEmailAddress() == null || nexusUser.getEmailAddress().isEmpty()) {
            throw new IncompleteOAuth2ProxyUserDataException("a nexus user must have an email - " + nexusUser);
        }

        OAuth2ProxyUser user = new OAuth2ProxyUser();

        user.setPreferredUsername(nexusUser.getUserId());
        user.setEmail(nexusUser.getEmailAddress());
        user.setGroups(nexusUser.getRoles());

        return user;
    }

    public static Optional<String[]> getNameParts(String preferredUsername) {
        String[] ret = new String[2];

        if (preferredUsername == null || preferredUsername.trim().isEmpty()) {
            logger.debug("unable to extract name parts from null or empty input as preferredUsername");
            ret[0] = "Unknown";
            ret[1] = "Unknown";
            return Optional.of(ret);
        }

        String localPart = preferredUsername.contains("@")
                ? preferredUsername.split("@")[0]
            : preferredUsername;

        String[] name_parts = localPart.split("\\.");

        if (name_parts.length == 0) {
            logger.debug("preferred username in unexpected format (no name parts) - " + preferredUsername);
            ret[0] = "Unknown";
            ret[1] = "Unknown";
        } else if (name_parts.length == 1) {
            ret[0] = capitalize(name_parts[0]);
            ret[1] = "Unknown";
        } else {
            ret[0] = capitalize(name_parts[0]);
            ret[1] = capitalize(name_parts[name_parts.length - 1]);
        }

        return Optional.of(ret);
    }

    static String capitalize(String s) {
        return (s == null || s.isEmpty())
                ? "Unknown"
            : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public String getGroupString() {
        return groups.stream().map(group -> group.getRoleId()).collect(Collectors.joining(","));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        OAuth2ProxyUser other = (OAuth2ProxyUser) obj;
        return Objects.equals(preferred_username, other.preferred_username) &&
                Objects.equals(groups, other.groups) &&
                Objects.equals(email, other.email) &&
                Objects.equals(apiToken, other.apiToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferred_username, groups, email, apiToken);
    }

    @Override
    public String getId() {
        return getPreferredUsername();
    }

    @Override
    public void setId(String id) {
        setPreferredUsername(id);
    }
}
