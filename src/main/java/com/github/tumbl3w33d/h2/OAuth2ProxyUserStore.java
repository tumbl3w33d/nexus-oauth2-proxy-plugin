package com.github.tumbl3w33d.h2;

import static com.github.tumbl3w33d.h2.OAuth2ProxyStores.userDAO;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.PasswordService;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.DuplicateUserException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.TransactionalStore;

import com.github.tumbl3w33d.users.db.OAuth2ProxyUser;
import com.google.common.collect.ImmutableSet;

@Named("mybatis")
@Singleton
public class OAuth2ProxyUserStore extends StateGuardLifecycleSupport implements TransactionalStore<DataSession<?>> {

    private final PasswordService passwordService;
    private final DataSessionSupplier sessionSupplier;

    @Inject
    public OAuth2ProxyUserStore(final DataSessionSupplier sessionSupplier, final PasswordService passwordService) {
        this.passwordService = passwordService;
        this.sessionSupplier = checkNotNull(sessionSupplier);
    }

    @Override
    public DataSession<?> openSession() {
        return OAuth2ProxyStores.openSession(sessionSupplier);
    }

    @Transactional
    public Set<User> getAllUsers() {
        Set<User> allUsers = StreamSupport.stream(userDAO().browse().spliterator(), false)
                .map(OAuth2ProxyUser::toNexusUser).collect(Collectors.toSet());
        return ImmutableSet.copyOf(allUsers);
    }

    @Transactional
    public Optional<OAuth2ProxyUser> getInternalUser(String userId) {
        log.debug("attempting to find user with id {} in db", userId);
        return userDAO().read(userId);
    }

    @Transactional
    public Optional<User> getUser(String userId) {
        Optional<OAuth2ProxyUser> maybeUser = getInternalUser(userId);

        if (maybeUser.isPresent()) {
            log.debug("retrieved internal user for {}, trying conversion to nexus user", userId);
            return Optional.of(maybeUser.get().toNexusUser());
        }

        log.debug("user {} not found in db", userId);

        return Optional.empty();
    }

    @Transactional
    public void addUser(OAuth2ProxyUser newUser) {
        newUser.setApiToken(hashToken(newUser.getApiToken()));

        log.debug("hashed API token of new user {} before persisting", newUser.getPreferredUsername());
        try {

            userDAO().create(newUser);
        } catch (DuplicateKeyException e) {
            throw new DuplicateUserException(newUser.getId());
        }

        log.debug("added user {} to db", newUser.getPreferredUsername());
    }

    private String hashToken(String plaintextToken) {
        if (plaintextToken == null || plaintextToken.isEmpty()) {
            // should not happen because the call is internal by our implementation
            throw new RuntimeException("empty/null password passed to hash method");
        }

        try {
            return this.passwordService.encryptPassword(plaintextToken);
        } catch (IllegalArgumentException e) {
            log.error("failed to hash token - {}", e);
            throw e;
        }
    }

    @Transactional
    public User updateUser(User user) throws UserNotFoundException {
        OAuth2ProxyUser proxyUser = OAuth2ProxyUser.of(user);

        log.debug("generic update call for user {}. Just updating user's groups, because no other use case exists.",
                user.getUserId());

        userDAO().updateGroups(proxyUser.getId(), proxyUser.getGroupString());

        return user;
    }

    @Transactional
    public void deleteUser(OAuth2ProxyUser userToDelete) {
        String userToDeleteId = userToDelete.getId();
        log.debug("about to delete user {}", userToDeleteId);
        userDAO().delete(userToDelete.getPreferredUsername());
        log.info("deleted user {}", userToDeleteId);
    }

    @Transactional
    public boolean updateUserApiToken(String userId, String token) {
        Optional<User> maybeDbuser = getUser(userId);

        if (!maybeDbuser.isPresent()) {
            log.warn("could not retrieve existing user {} for token update", userId);
            return false;
        }

        User dbuser = maybeDbuser.get();

        log.debug("about to update the api token of user {}", dbuser.getUserId());

        try {
            // we only update the token, not the rest that possibly changed
            userDAO().updateApiToken(dbuser.getUserId(), hashToken(token));
        } catch (Exception e) {
            log.error("failed to update API token of user {} - {}", dbuser.getUserId(), e);
            throw e;
        }

        log.debug("updated api token of user {}", dbuser.getUserId());

        return true;
    }

    @Transactional
    public void updateUserGroups(String userId, Set<RoleIdentifier> groups) throws UserNotFoundException {
        String sortedCommaSepGroups = groups.stream()
                .map(RoleIdentifier::getRoleId)
                .sorted()
                .collect(Collectors.joining(","));

        log.debug("updating group string of user {}: {}", userId, sortedCommaSepGroups);

        userDAO().updateGroups(userId, sortedCommaSepGroups);
    }

    @Transactional
    public Optional<String> getApiToken(String principal) {
        Optional<OAuth2ProxyUser> maybeInternalUser = getInternalUser(principal);

        if (maybeInternalUser.isPresent()) {
            OAuth2ProxyUser internalUser = maybeInternalUser.get();
            log.debug("retrieved internal user {} for getting api token", internalUser.getPreferredUsername());
            return Optional.ofNullable(internalUser.getApiToken());
        }

        log.debug("unable to retrieve API token of user {}", principal);

        return Optional.empty();
    }

}
