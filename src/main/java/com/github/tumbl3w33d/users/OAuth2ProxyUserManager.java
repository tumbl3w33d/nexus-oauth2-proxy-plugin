package com.github.tumbl3w33d.users;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.eclipse.sisu.Description;
import org.sonatype.nexus.security.user.AbstractUserManager;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;
import org.sonatype.nexus.security.user.UserStatus;

import com.github.tumbl3w33d.OAuth2ProxyRealm;
import com.github.tumbl3w33d.h2.OAuth2ProxyUserStore;
import com.github.tumbl3w33d.users.db.OAuth2ProxyUser;

@Named(OAuth2ProxyUserManager.SOURCE)
@Singleton
@Description(OAuth2ProxyUserManager.SOURCE)
public class OAuth2ProxyUserManager extends AbstractUserManager {

    public static final String AUTHENTICATING_REALM = OAuth2ProxyRealm.NAME;
    public static final String SOURCE = "OAuth2Proxy";

    private final OAuth2ProxyUserStore userStore;

    @Inject
    public OAuth2ProxyUserManager(final OAuth2ProxyUserStore userStore) {
        this.userStore = userStore;
    }

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public String getAuthenticationRealmName() {
        return AUTHENTICATING_REALM;
    }


    @Override
    public Set<User> listUsers() {
        return userStore.getAllUsers();
    }

    @Override
    public Set<String> listUserIds() {
        return listUsers().stream().map(User::getUserId).collect(Collectors.toSet());
    }

    @Override
    public Set<User> searchUsers(UserSearchCriteria criteria) {
        Set<User> ret = new HashSet<>();

        Set<User> users = userStore.getAllUsers();

        if (users == null || users.isEmpty()) {
            log.debug("call to searchUsers returns empty result");
            return Collections.emptySet();
        }

        ret.addAll(users);

        String email = criteria.getEmail();
        if (email != null && !email.isEmpty()) {
            ret = ret.stream().filter(user -> user.getEmailAddress().equals(email))
                    .collect(Collectors.toSet());
        }

        String id = criteria.getUserId();
        if (id != null && !id.isEmpty()) {
            ret = ret.stream().filter(user -> user.getUserId().equals(id))
                    .collect(Collectors.toSet());
        }

        log.debug("call to searchUsers returns {}", ret);

        return ret;
    }

    @Override
    public User getUser(String userId) throws UserNotFoundException {
        Optional<User> maybeUser = userStore.getUser(userId);

        if (maybeUser.isPresent()) {
            log.debug("getUser returning a user object found in db for {}", userId);
            return maybeUser.get();
        }

        log.debug("getUser unable to find a user object for {}", userId);

        throw new UserNotFoundException("no user {} found in db");
    }

    @Override
    public User getUser(final String userId, final Set<String> roleIds) throws UserNotFoundException {
        // FIXME: make use of the roleIds
        return getUser(userId);
    }

    public static User createUserObject(String preferred_username, String email) {

        User newUser = new User();
        newUser.setUserId(preferred_username);

        // naive approach to figure out names from username
        if (preferred_username.contains(".")) {
            String[] name_parts = preferred_username.split("\\.");
            String assumed_firstname = name_parts[0].substring(0, 1).toUpperCase() + name_parts[0].substring(1);
            newUser.setFirstName(assumed_firstname);
            String assumed_lastname = name_parts[1].substring(0, 1).toUpperCase() + name_parts[1].substring(1);
            newUser.setLastName(assumed_lastname);
        }

        newUser.setEmailAddress(email);
        newUser.setSource(OAuth2ProxyUserManager.SOURCE);
        newUser.setReadOnly(false);
        newUser.setStatus(UserStatus.active);

        return newUser;
    }

    @Override
    public boolean supportsWrite() {
        return true;
    }

    @Override
    public User addUser(User user, String password) {
        OAuth2ProxyUser internalUser = OAuth2ProxyUser.of(user);
        internalUser.setApiToken(password);
        log.debug("converted preliminary new user to internal user object for persisting");
        userStore.addUser(internalUser);
        log.debug("persisted new user {}", user.getUserId());
        return user;
    }

    /**
     * Nexus requires this generic update method to be available, but
     * we reduce it to an update of groups as there is no other use case
     * in this context.
     */
    @Deprecated
    @Override
    public User updateUser(User user) throws UserNotFoundException {
        return userStore.updateUser(user);
    }

    public User updateUserGroups(User user) throws UserNotFoundException {
        userStore.updateUserGroups(user.getUserId(), user.getRoles());
        return user;
    }

    @Override
    public void deleteUser(String userId) throws UserNotFoundException {
        Optional<User> maybeUserToDel = userStore.getUser(userId);

        if (maybeUserToDel.isPresent()) {
            userStore.deleteUser(OAuth2ProxyUser.of(maybeUserToDel.get()));
        } else {
            log.warn("could not retrieve user {} for deletion", userId);
        }
    }

    @Override
    public void changePassword(String userId, String newPassword) throws UserNotFoundException {
        log.trace("changePassword called for {}, will set as api token", userId);

        Optional<User> maybeUserToUpdate = userStore.getUser(userId);

        if (maybeUserToUpdate.isPresent()) {
            userStore.updateUserApiToken(userId, newPassword);
        } else {
            log.warn("could not retrieve user {} for changing password", userId);
        }
    }

    public static final class UserWithPrincipals {
        private User user;

        private final SimplePrincipalCollection principals = new SimplePrincipalCollection();

        public boolean hasPrincipals() {
            return !principals.isEmpty();
        }

        public SimplePrincipalCollection getPrincipals() {
            return principals;
        }

        public void addPrincipal(String userId, String authRealmName) {
            this.principals.add(userId, authRealmName);
        }

        public boolean hasUser() {
            return user != null;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return "user: " + user + " - principals: " + principals;
        }
    }

    public Optional<String> getApiToken(String principal) {
        return userStore.getApiToken(principal);
    }

}
