package com.github.tumbl3w33d.users.db;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.shiro.authc.credential.PasswordService;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.transaction.OrientTransactional;
import org.sonatype.nexus.security.user.User;

import com.github.tumbl3w33d.OAuth2ProxyDatabase;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Named
@Singleton
public class OAuth2ProxyUserStore extends ComponentSupport {

    private final Provider<DatabaseInstance> databaseProvider;
    private final OAuth2ProxyUserEntityAdapter entityAdapter;
    private final PasswordService passwordService;

    @Inject
    public OAuth2ProxyUserStore(
            @Named(OAuth2ProxyDatabase.NAME) final Provider<DatabaseInstance> databaseProvider,
            final OAuth2ProxyUserEntityAdapter entityAdapter,
            final PasswordService passwordService) {

        this.databaseProvider = databaseProvider;
        this.entityAdapter = entityAdapter;
        this.passwordService = passwordService;

        init();
    }

    private void init() {
        DatabaseInstance databaseInstance = databaseProvider.get();

        try (ODatabaseDocumentTx db = databaseInstance.acquire()) {
            entityAdapter.register(db);
        } catch (Exception e) {
            log.error("unabled to create schema for OAuth2ProxyUserStore - {}", e);
        }
    }

    public Set<User> getAllUsers() {
        Set<User> users = OrientTransactional.inTx(this.databaseProvider)
                .call(db -> StreamSupport.stream(this.entityAdapter.browse(db).spliterator(), false)
                        .map(OAuth2ProxyUser::toNexusUser).collect(Collectors.toSet()));

        return ImmutableSet.copyOf(users);
    }

    public Optional<OAuth2ProxyUser> getInternalUser(String userId) {
        String userQuery = "select from " + OAuth2ProxyUserEntityAdapter.DB_USER_CLASS + " where "
                + OAuth2ProxyUserEntityAdapter.DB_USER_PROP_NAME + " = ?";
        log.debug("about to query for user {}", userQuery.replace("?", userId));
        try {
            List<ODocument> userDocs = OrientTransactional
                    .inTx(this.databaseProvider)
                    .call(db -> {
                        OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<>(userQuery);
                        return db.command(query).execute(userId);

                    });

            log.debug("retrieved {} user docs from db", userDocs.size());

            if (userDocs != null && !userDocs.isEmpty()) {

                if (userDocs.size() > 1) {
                    log.warn("found more than one user in db for name {}", userId);
                }

                ODocument firstUserDoc = userDocs.get(0);
                log.debug("about to convert the retrieved user doc to internal user object");
                OAuth2ProxyUser internalUserObject = entityAdapter.convertToOAuth2ProxyUser(firstUserDoc);
                log.debug("conversion result: {}", internalUserObject);
                return Optional.of(internalUserObject);
            }
        } catch (Exception e) {
            log.error("query for user failed: {}", e);
        }
        return Optional.empty();
    }

    public Optional<User> getUser(String userId) {
        Optional<OAuth2ProxyUser> maybeUser = getInternalUser(userId);

        if (maybeUser.isPresent()) {
            log.debug("retrieved internal user for {}, trying conversion to nexus user", userId);
            return Optional.of(maybeUser.get().toNexusUser());
        }

        log.debug("user {} not found in db", userId);

        return Optional.empty();
    }

    public void addUser(OAuth2ProxyUser newUser) {

        newUser.setApiToken(hashToken(newUser.getApiToken()));

        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> entityAdapter.addEntity(db, newUser));

        log.debug("added user {} to db", newUser.getPreferredUsername());
    }

    public void deleteUser(OAuth2ProxyUser userToDelete) {
        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> {
                    entityAdapter.deleteEntity(db, userToDelete);
                    return null;
                });

        log.info("deleted user {}", userToDelete);
    }

    public User updateUser(User user) {
        Optional<OAuth2ProxyUser> maybeDbuser = getInternalUser(user.getUserId());

        if (!maybeDbuser.isPresent()) {
            log.warn("could not retrieve existing user {} for update", user.getUserId());
            return null;
        }

        OAuth2ProxyUser dbuser = maybeDbuser.get();

        if (!dbuser.getEmail().equals(user.getEmailAddress())) {
            dbuser.setEmail(user.getEmailAddress());
        }

        if (!dbuser.getGroups().equals(user.getRoles())) {
            dbuser.setGroups(user.getRoles());
        }

        ODocument updatedUser = OrientTransactional.inTx(this.databaseProvider)
                .call(db -> {
                    return entityAdapter.editEntity(db, dbuser);
                });

        return entityAdapter.convertToOAuth2ProxyUser(updatedUser).toNexusUser();
    }

    private String hashToken(String plaintextToken) {
        if (plaintextToken == null || plaintextToken.isEmpty()) {
            // should not happen because the call is internal by our implementation
            throw new RuntimeException("empty/null password passed to hash method");
        }
        return this.passwordService.encryptPassword(plaintextToken);
    }

    public boolean updateUserApiToken(String userId, String token) {
        Optional<User> maybeDbuser = getUser(userId);

        if (!maybeDbuser.isPresent()) {
            log.warn("could not retrieve existing user {} for token update", userId);
            return false;
        }

        User dbuser = maybeDbuser.get();

        // we only update the token, not the rest that possibly changed
        OAuth2ProxyUser userForUpdate = OAuth2ProxyUser.of(dbuser);
        userForUpdate.setApiToken(hashToken(token));

        log.debug("about to update the api token of user {}", userForUpdate);

        OrientTransactional.inTx(this.databaseProvider)
                .call(db -> {
                    return entityAdapter.editEntity(db, userForUpdate);
                });

        log.debug("updated api token of user {}", dbuser.getUserId());

        return true;
    }

    public Optional<String> getApiToken(String principal) {
        Optional<OAuth2ProxyUser> maybeInternalUser = getInternalUser(principal);

        if (maybeInternalUser.isPresent()) {
            OAuth2ProxyUser internalUser = maybeInternalUser.get();
            log.debug("retrieved internal user {} for getting api token", internalUser.getPreferredUsername());
            return Optional.ofNullable(internalUser.getApiToken());
        }

        log.warn("unable to retrieve password of user {}", principal);

        return Optional.empty();
    }

}
