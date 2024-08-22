package com.github.tumbl3w33d;

import static com.google.common.base.Preconditions.checkNotNull;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
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
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.h2.OAuth2ProxyLoginRecordStore;
import com.github.tumbl3w33d.h2.OAuth2ProxyRoleStore;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager.UserWithPrincipals;
import com.github.tumbl3w33d.users.db.OAuth2ProxyLoginRecord;

@Named(OAuth2ProxyRealm.NAME)
@Singleton
@Description(OAuth2ProxyRealm.NAME)
public class OAuth2ProxyRealm extends AuthorizingRealm {

    public static final String NAME = "OAuth2ProxyRealm";

    final Logger logger = LoggerFactory.getLogger(OAuth2ProxyRealm.class.getName());

    private static final String ID = "oauth2-proxy-realm";

    private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    static final String CLASS_USER_LOGIN = "OAuth2ProxyUserLogin";
    static final String FIELD_USER_ID = "userId";
    static final String FIELD_LAST_LOGIN = "lastLogin";

    private final OAuth2ProxyUserManager userManager;
    private final OAuth2ProxyRoleStore roleStore;
    private final OAuth2ProxyLoginRecordStore loginRecordStore;
    private PasswordService passwordService;

    @Inject
    public OAuth2ProxyRealm(
            @Named OAuth2ProxyUserManager userManager, @Named OAuth2ProxyRoleStore roleStore,
            @Named OAuth2ProxyLoginRecordStore loginRecordStore,
            @Named PasswordService passwordService) {
        this.userManager = checkNotNull(userManager);
        this.roleStore = roleStore;
        this.loginRecordStore = loginRecordStore;
        this.passwordService = passwordService;

        setName(ID);

        setCredentialsMatcher(new OAuth2ProxyRealmCredentialsMatcher());

        // authentication is provided by oauth2 proxy headers with every request
        setAuthenticationCachingEnabled(false);
        setAuthorizationCachingEnabled(false);
    }

    private boolean isApiTokenMatching(AuthenticationToken token) {
        logger.debug("token principal for matching api token: {}", token.getPrincipal());

        if (token.getPrincipal() instanceof String) {
            Optional<String> maybeApiToken = userManager.getApiToken((String) token.getPrincipal());

            if (!maybeApiToken.isPresent()) {
                logger.debug(
                        "unable to retrieve API token from database user in order to match against provided auth token secret");
                return false;
            }

            String apiToken = maybeApiToken.get();

            return passwordService.passwordsMatch(token.getCredentials(), apiToken);
        }

        logger.debug("principal received from auth token is not a string");

        return false;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        logger.trace("call to doGetAuthenticationInfo with token {}", token);

        if (token instanceof UsernamePasswordToken) {
            if (isApiTokenMatching(token)) {
                // the condition method already ensures the principal is a string
                String userId = (String) token.getPrincipal();

                logger.debug("programmatic access by {} succeeded", userId);

                UserWithPrincipals userWithPrincipals = findUserById(userId);

                logger.debug("found principal {} for programmatic access", userWithPrincipals);

                recordLogin(userId);

                return new SimpleAuthenticationInfo(userWithPrincipals.getPrincipals(), null);
            }

            logger.debug("programmatic access failed because credentials did not match");

            return null;
        }

        OAuth2ProxyHeaderAuthToken oauth2Token = (OAuth2ProxyHeaderAuthToken) token;

        String userid = oauth2Token.user.getHeaderValue();
        String preferred_username = oauth2Token.preferred_username.getHeaderValue();
        String email = oauth2Token.email != null ? oauth2Token.email.getHeaderValue() : null;
        String accessToken = oauth2Token.accessToken != null ? oauth2Token.accessToken.getHeaderValue() : null;
        String groups = oauth2Token.groups != null ? oauth2Token.groups.getHeaderValue() : null;

        logger.trace(
                "getting authentication info - user: {} - preferred username: {} - email: {} - access token: {} - groups: {}",
                userid, preferred_username, email, accessToken != null ? "<hidden>" : null, groups);

        final String oauth2proxyUserId = token.getPrincipal().toString();
        logger.debug("determined user id {}", oauth2proxyUserId);

        UserWithPrincipals userWithPrincipals = findUserById(oauth2proxyUserId);
        logger.debug("found principal {} for interactive access", userWithPrincipals);

        if (!userWithPrincipals.hasPrincipals()) {
            logger.debug("need to create a new user object for {}", oauth2proxyUserId);

            User newUserObject = OAuth2ProxyUserManager.createUserObject(preferred_username, email);
            logger.debug("created preliminary user object {}", newUserObject);
            userManager.addUser(newUserObject, generateSecureRandomString(32));
            logger.debug("created the user via userManager");
            userWithPrincipals.setUser(newUserObject);
            userWithPrincipals.addPrincipal(newUserObject.getUserId(), OAuth2ProxyUserManager.AUTHENTICATING_REALM);
            logger.info("created new user object for {}", oauth2proxyUserId);
        }

        if (userWithPrincipals.hasUser()) {
            String userId = userWithPrincipals.getUser().getUserId();

            logger.trace("user {} (source {}) has roles {} before sync", userId,
                            userWithPrincipals.getUser().getSource(),
                    userWithPrincipals.getUser().getRoles());

            if (oauth2Token.groups != null) {
                logger.trace("user {} has identity provider groups {}", userId, oauth2Token.groups);
                syncExternalRolesForGroups(userWithPrincipals.getUser(), oauth2Token.groups.getHeaderValue());
            }

        }

        if (userWithPrincipals.hasPrincipals()) {
            logger.debug("found principals for OAuth2 proxy user '{}': '{}' from realms '{}'", oauth2proxyUserId,
                    userWithPrincipals.getPrincipals(),
                    userWithPrincipals.getPrincipals().getRealmNames());

            recordLogin(oauth2proxyUserId);

            return new SimpleAuthenticationInfo(userWithPrincipals.getPrincipals(), null);
        }

        logger.debug("No found principals for OAuth2 proxy user '{}'", oauth2proxyUserId);

        return null;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        String principal = principals.getPrimaryPrincipal().toString();

        logger.debug("call to doGetAuthorizationInfo found primary principle {}", principal);

        try {
            User user = userManager.getUser(principal);

            Set<String> roles = user.getRoles().stream().map(role -> role.getRoleId()).collect(Collectors.toSet());

            logger.trace("user {} has roles {}", principal, roles);

            return new SimpleAuthorizationInfo(roles);
        } catch (UserNotFoundException e) {
            logger.debug("unable to find user with principal {} in source {} for authorization", principal,
                    OAuth2ProxyUserManager.SOURCE);

            return null;
        }
    }

    void syncExternalRolesForGroups(User user, String groupsString) {

        Set<RoleIdentifier> idpGroups = Stream.of(groupsString.trim().split(","))
                .map(groupString -> new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, groupString))
                .collect(Collectors.toSet());

        roleStore.addRolesIfMissing(idpGroups);

        user.addAllRoles(idpGroups);

        logger.trace("added idp groups as role to user {}: {}", user.getUserId(),
                user.getRoles().stream().map(group -> group.getRoleId()).collect(Collectors.toSet()));

        Set<RoleIdentifier> rolesToDelete = new HashSet<>();

        for (RoleIdentifier role : user.getRoles()) {
            if (!role.getSource().equals(OAuth2ProxyUserManager.SOURCE)) {
                // not touching roles assigned outside of this realm logic
                logger.debug("group sync leaving {}'s role {} untouched", user.getUserId(),
                        role.getRoleId(), role.getSource());
                continue;
            }

            if (!idpGroups.stream().anyMatch(idpGroup -> idpGroup.getRoleId().equals(role.getRoleId()))) {
                logger.warn("marking role {} of user {} for deletion", role.getRoleId(),
                        user.getUserId());
                rolesToDelete.add(role);
            } else {
                logger.trace("user {} still has group for role {} in identity provider",
                        user.getUserId(),
                        role.getRoleId());
            }
        }

        for (RoleIdentifier role : rolesToDelete) {
            user.removeRole(role);
            logger.warn("deleted role {} from user {}", role.getRoleId(),
                    user.getUserId());
        }
        
        try {
            userManager.updateUserGroups(user);
        } catch (UserNotFoundException e) {
            logger.warn("user {} cannot be found in db for group synchronization", user.getUserId());
            return;
        } catch (Exception e) {
            logger.error("unexpected error when updating user groups - {}", e);
            return;
        }
    }

    private UserWithPrincipals findUserById(final String oauth2proxyUserId) {
        UserWithPrincipals userWithPrincipals = new UserWithPrincipals();
        try {
            User user = userManager.getUser(oauth2proxyUserId);
            userWithPrincipals.setUser(user);
            userWithPrincipals.addPrincipal(oauth2proxyUserId, OAuth2ProxyUserManager.AUTHENTICATING_REALM);
        } catch (UserNotFoundException e) {
            logger.debug("meh, no user {} yet", oauth2proxyUserId);
        }
        return userWithPrincipals;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        if (token instanceof OAuth2ProxyHeaderAuthToken) {
            logger.debug("announcing support for token {}", token);
            return true;
        } else if (token instanceof UsernamePasswordToken) {
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

    void recordLogin(String userId) {
        Optional<OAuth2ProxyLoginRecord> maybeRecord = loginRecordStore.getLoginRecord(userId);

        if (!maybeRecord.isPresent()) {
            logger.debug("No login recorded for {} yet. Creating it now.", userId);
            loginRecordStore.createLoginRecord(userId);
            return;
        }

        Timestamp lastLoginDate = maybeRecord.get().getLastLogin();
        LocalDate lastLoginLocalDate = lastLoginDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate nowLocalDate = LocalDate.now();

        logger.debug("Last known login for {} was {}", userId, lastLoginDate);

        if (!lastLoginLocalDate.equals(nowLocalDate)) {
            logger.debug("Updating last known login for {} (was {})", userId, lastLoginDate);
            loginRecordStore.updateLoginRecord(userId);
        } else {
            logger.debug("login record of {} is already up-to-date", userId);
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