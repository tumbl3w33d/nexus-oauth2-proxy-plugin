package com.github.tumbl3w33d;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserStatus;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

@Named
@Singleton
@Description("OAuth2 Proxy Realm")
public class OAuth2ProxyRealm extends AuthenticatingRealm {

    private static final String NAME = OAuth2ProxyRealm.class.getName();

    private final Logger logger = LoggerFactory.getLogger(NAME);

    private static final String ID = "oauth2-proxy-realm";

    private static final String IDP_GROUP_PREFIX = "idp-";

    private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    static final String CLASS_USER_LOGIN = "OAuth2ProxyUserLogin";
    static final String FIELD_USER_ID = "userId";
    static final String FIELD_LAST_LOGIN = "lastLogin";

    private final List<UserManager> userManagers;
    private final UserManager nexusAuthenticatingRealm;
    private final DatabaseInstance databaseInstance;

    @Inject
    public OAuth2ProxyRealm(final List<UserManager> userManagers,
            @Named(OAuth2ProxyDatabase.NAME) DatabaseInstance databaseInstance) {
        this.userManagers = checkNotNull(userManagers);
        this.nexusAuthenticatingRealm = userManagers.stream()
                .filter(um -> um.getAuthenticationRealmName() == "NexusAuthenticatingRealm")
                .findFirst().get();
        this.databaseInstance = databaseInstance;

        setName(ID);

        setCredentialsMatcher(new CredentialsMatcher() {

            @Override
            public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
                return true;
            }

        });

        ensureUserLoginTimestampSchema(databaseInstance, logger);
    }


    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        OAuth2ProxyHeaderAuthToken oauth2Token = (OAuth2ProxyHeaderAuthToken) token;

        String userid = oauth2Token.user.getHeaderValue();
        String preferred_username = oauth2Token.preferred_username.getHeaderValue();
        String email = oauth2Token.email != null ? oauth2Token.email.getHeaderValue() : null;
        String accessToken = oauth2Token.accessToken != null ? oauth2Token.accessToken.getHeaderValue() : null;
        String groups = oauth2Token.groups != null ? oauth2Token.groups.getHeaderValue() : null;

        logger.debug(
                "getting authentication info - user: {} - preferred username: {} - email: {} - access token: {} - groups: {}",
                userid, preferred_username, email, accessToken != null ? "<hidden>" : null, groups);

        final String oauth2proxyUserId = token.getPrincipal().toString();
        logger.debug("determined user id {}", oauth2proxyUserId);

        UserWithPrincipals userWithPrincipals = findUserById(oauth2proxyUserId);

        try {
            if (!userWithPrincipals.hasPrincipals()) {
                logger.debug("need to create a new user object for {}", oauth2proxyUserId);
                createUser(preferred_username, email, userWithPrincipals);
                logger.info("created new user object for {}", oauth2proxyUserId);
            }
        } catch (ORecordDuplicatedException e) {
            logger.debug(
                    "ignoring duplicate record exception, probably caused by concurrent requests creating new user object");

            userWithPrincipals = findUserById(oauth2proxyUserId);
        } catch (Exception e) {
            logger.error("unexpected error on user creation: {}", e);
        }

        if (userWithPrincipals.user != null) {
            String userId = userWithPrincipals.user.getUserId();
            logger.debug("user {} has roles {} before sync", userId,
                    userWithPrincipals.user.getRoles());
            if (oauth2Token.groups != null) {
                logger.trace("user {} has identity provider groups {}", userId, oauth2Token.groups);
                syncExternalRolesForGroups(userWithPrincipals.user, oauth2Token.groups.getHeaderValue());
            }

        }

        if (userWithPrincipals.hasPrincipals()) {
            logger.debug("found principals for OAuth2 proxy user '{}': '{}' from realms '{}'", oauth2proxyUserId,
                    userWithPrincipals.principals,
                    userWithPrincipals.principals.getRealmNames());
            recordLogin(oauth2proxyUserId);
            return new SimpleAuthenticationInfo(userWithPrincipals.principals, null);
        }

        logger.debug("No found principals for OAuth2 proxy user '{}'", oauth2proxyUserId);

        return null;
    }

    private void createUser(String preferred_username, String email,
            UserWithPrincipals userWithPrincipals) {

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
        newUser.setSource(NAME);
        newUser.setReadOnly(true);
        newUser.setStatus(UserStatus.active);
        nexusAuthenticatingRealm.addUser(newUser, generateSecureRandomString(32));

        userWithPrincipals.user = newUser;
        userWithPrincipals.addPrincipal(newUser.getUserId(),
                nexusAuthenticatingRealm.getAuthenticationRealmName());
    }

    private void syncExternalRolesForGroups(User user, String groupsString) {
        // mark idp groups with prefix to recognize them later
        Set<RoleIdentifier> idpGroups = Stream.of(groupsString.split(","))
                .map(groupString -> new RoleIdentifier(UserManager.DEFAULT_SOURCE, IDP_GROUP_PREFIX + groupString))
                .collect(Collectors.toSet());

        user.addAllRoles(idpGroups);
        logger.debug("added idp groups as role to user {}: {}", user.getUserId(),
                user.getRoles().stream().map(group -> group.getRoleId()).collect(Collectors.toSet()));

        Set<RoleIdentifier> rolesToDelete = new HashSet<>();

        for (RoleIdentifier role : user.getRoles()) {
            if (!role.getRoleId().startsWith(IDP_GROUP_PREFIX)) {
                // not touching roles assigned outside of this realm logic
                logger.debug("group sync leaving {}'s role {} untouched", user.getUserId(),
                        role.getRoleId(), role.getSource());
                continue;
            }
            if (!idpGroups.stream().anyMatch(idpGroup -> idpGroup.getRoleId().equals(role.getRoleId()))) {
                logger.warn("marking role {} of user {} for deletion", role.getRoleId(), user.getUserId());
                rolesToDelete.add(role);
            } else {
                logger.trace("user {} still has group for role {} in identity provider", user.getUserId(),
                        role.getRoleId());
            }
        }

        for (RoleIdentifier role : rolesToDelete) {
            user.removeRole(role);
            logger.warn("deleted role {} from user {}", role.getRoleId(), user.getUserId());
        }

        try {
            nexusAuthenticatingRealm.updateUser(user);
            logger.debug("user source: {} - roles: {}", user.getSource(), user.getRoles());
            logger.debug("updated user {} according to identity provider group information", user.getUserId());
        } catch (UserNotFoundException e) {
            logger.error("unable to find user {} for synchronizing groups", user.getUserId());
        } catch (Exception e) {
            logger.error("unexpected error when updating user {}", user.getUserId());
        }
    }

    private UserWithPrincipals findUserById(final String oauth2proxyUserId) {
        UserWithPrincipals userWithPrincipals = new UserWithPrincipals();
        for (UserManager userManager : userManagers) {
            if (userManager.getAuthenticationRealmName() == null) {
                continue;
            }

            try {
                final User user = userManager.getUser(oauth2proxyUserId);
                logger.debug("found user {} in user manager {}", oauth2proxyUserId, userManager);
                userWithPrincipals.user = user;
                userWithPrincipals.addPrincipal(user.getUserId(), userManager.getAuthenticationRealmName());
                logger.debug("added {} to principal collection", user.getUserId());
            } catch (UserNotFoundException e) {
                logger.debug("user {} not found in user manager {}", oauth2proxyUserId, userManager);
                continue;
            }
        }
        return userWithPrincipals;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        if (token instanceof OAuth2ProxyHeaderAuthToken) {
            logger.debug("announcing support for token {}", token);
            return true;
        } else {
            logger.debug("token of type {} is not handled by this realm", token);
            return false;
        }
    }

    public static String generateSecureRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
            char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    private static final class UserWithPrincipals {
        private User user;
        private final SimplePrincipalCollection principals = new SimplePrincipalCollection();

        boolean hasPrincipals() {
            return !principals.isEmpty();
        }

        void addPrincipal(String userId, String authRealmName) {
            this.principals.add(userId, authRealmName);
        }
    }

    public static void ensureUserLoginTimestampSchema(DatabaseInstance dbInstance, Logger logger) {
        try (ODatabaseDocumentTx db = dbInstance.acquire()) {

            OSchema schema = db.getMetadata().getSchema();
            final String className = CLASS_USER_LOGIN.toUpperCase();
            if (!schema.existsClass(className)) {
                OClass oAuth2ProxyUserLogin = schema.createClass(className);
                oAuth2ProxyUserLogin.createProperty(FIELD_USER_ID, OType.STRING).setNotNull(true);
                oAuth2ProxyUserLogin.createProperty(FIELD_LAST_LOGIN, OType.DATETIME)
                        .setNotNull(true);
            }
            logger.info("created schema class for " + CLASS_USER_LOGIN);
        } catch (Exception e) {
            logger.error("Failed to ensure schema for " + CLASS_USER_LOGIN);
        }
    }

    private void recordLogin(String userId) {
        try (ODatabaseDocumentTx db = databaseInstance.acquire()) {
            db.begin();

            List<ODocument> userLogins = db.query(new OSQLSynchQuery<ODocument>(
                    "select from " + CLASS_USER_LOGIN + " where " + FIELD_USER_ID + " = ?", 1),
                    userId);

            ODocument userLogin;

            boolean shouldUpdate = false;

            if (userLogins.isEmpty()) {
                logger.debug("No login recorded for {} yet", userId);
                userLogin = new ODocument(CLASS_USER_LOGIN).field(FIELD_USER_ID, userId);
                shouldUpdate = true;
            } else {
                userLogin = userLogins.get(0);
                Date lastLoginDate = userLogin.field(FIELD_LAST_LOGIN);
                LocalDate lastLoginLocalDate = lastLoginDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate nowLocalDate = LocalDate.now();

                if (!lastLoginLocalDate.equals(nowLocalDate)) {
                    logger.info("Last known login for {} was {}", userId, formatDateString(lastLoginDate));
                    shouldUpdate = true;
                }
            }

            if (shouldUpdate) {
                userLogin.field(FIELD_LAST_LOGIN, new Date()); // Use current date
                userLogin.save();
                db.commit();
            }

        } catch (Exception e) {
            logger.error("Failed to persist login timestamp for user {} - {}", userId, e);
        }
    }

    static String formatDateString(Date date) {
        if (date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return localDateTime.format(formatter);
        } else {
            return "unknown";
        }
    }
}