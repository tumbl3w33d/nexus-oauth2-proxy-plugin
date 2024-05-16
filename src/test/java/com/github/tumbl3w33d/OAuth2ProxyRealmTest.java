package com.github.tumbl3w33d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;
import org.sonatype.nexus.security.internal.DefaultSecurityPasswordService;
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager.UserWithPrincipals;
import com.github.tumbl3w33d.users.db.OAuth2ProxyRoleStore;
import com.google.common.collect.ImmutableSet;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;

@ExtendWith(MockitoExtension.class)
public class OAuth2ProxyRealmTest {

    @Mock
    private Logger logger;

    private OAuth2ProxyRealm oauth2ProxyRealm;

    @Test
    public void testFormatDateString() {
        assertEquals("unknown", OAuth2ProxyRealm.formatDateString(null));
        assertEquals("2024-05-10", OAuth2ProxyRealm.formatDateString(new Date(1715343438000l)));
    }

    @Test
    void testDoGetAuthorizationInfo() {
        OAuth2ProxyUserManager userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        oauth2ProxyRealm = getTestRealm(null, userManager, null);

        User existingUser = OAuth2ProxyUserManager.createUserObject("test.user", "test.user@example.com");
        existingUser.addAllRoles(
                ImmutableSet.of(new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, "administrators@idm.example.com"),
                        new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, "devs@idm.example.com")));
        PrincipalCollection principalCollection = new SimplePrincipalCollection("test.user",
                OAuth2ProxyUserManager.SOURCE);

        try {
            Mockito.when(userManager.getUser("test.user")).thenReturn(existingUser);
        } catch (UserNotFoundException e) {
        }
        AuthorizationInfo authzInfo = oauth2ProxyRealm.doGetAuthorizationInfo(principalCollection);
        assertNotNull(authzInfo);
        assertEquals(ImmutableSet.of("administrators@idm.example.com", "devs@idm.example.com"), authzInfo.getRoles());
    }

    @Test
    void testDoGetAuthenticationInfo() {
        testProgrammaticAccess();

        testInteractiveAccessExistingUser();

        testInteractiveAccessNewUser();
    }

    /*
     * Make sure there was a call to user creation in case no user
     * for the provided proxy headers exists yet.
     */
    private void testInteractiveAccessNewUser() {
        OAuth2ProxyUserManager userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        try {
            Mockito.when(userManager.getUser(anyString())).thenThrow(new UserNotFoundException("test.user"));
        } catch (UserNotFoundException e) {
        }
        oauth2ProxyRealm = getTestRealm(null, userManager, null);

        OAuth2ProxyHeaderAuthToken token = createTestOAuth2ProxyHeaderAuthToken();

        AuthenticationInfo authInfo = oauth2ProxyRealm.doGetAuthenticationInfo(token);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userManager).addUser(userCaptor.capture(), anyString());
        User capturedUser = userCaptor.getValue();
        assertEquals("test.user", capturedUser.getUserId());
        assertEquals("Test", capturedUser.getFirstName());
        assertEquals("User", capturedUser.getLastName());
        assertEquals(OAuth2ProxyUserManager.SOURCE, capturedUser.getSource());
        assertNotNull(authInfo);
        assertEquals(authInfo.getPrincipals().getPrimaryPrincipal(), "test.user");
    }

    private void testInteractiveAccessExistingUser() {
        OAuth2ProxyUserManager userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        oauth2ProxyRealm = getTestRealm(null, userManager, null);

        OAuth2ProxyHeaderAuthToken token = createTestOAuth2ProxyHeaderAuthToken();

        User existingUser = OAuth2ProxyUserManager.createUserObject("test.user", "test.user@example.com");

        try {
            Mockito.when(userManager.getUser("test.user")).thenReturn(existingUser);
        } catch (UserNotFoundException e) {
        }
        AuthenticationInfo authInfo = oauth2ProxyRealm.doGetAuthenticationInfo(token);
        assertNotNull(authInfo);
        assertEquals(authInfo.getPrincipals().getPrimaryPrincipal(), "test.user");
    }

    private OAuth2ProxyHeaderAuthToken createTestOAuth2ProxyHeaderAuthToken() {
        OAuth2ProxyHeaderAuthToken token = new OAuth2ProxyHeaderAuthToken();
        String testHost = "127.0.0.1";
        token.user = new HttpHeaderAuthenticationToken(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_USER,
                UUID.randomUUID().toString(), testHost);
        token.preferred_username = new HttpHeaderAuthenticationToken(
                OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_PREFERRED_USERNAME, "test.user", testHost);
        token.email = new HttpHeaderAuthenticationToken(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_EMAIL,
                "test.user@example.com", testHost);
        token.groups = new HttpHeaderAuthenticationToken(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_GROUPS,
                "administrators@idm.example.com,devs@idm.example.com", testHost);
        return token;
    }

    private void testProgrammaticAccess() {
        OAuth2ProxyUserManager userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        oauth2ProxyRealm = getTestRealm(null, userManager, null);

        String hashedPassword = "$shiro1$SHA-512$1024$tz9GiwuH8w6FVj0kz+tEEQ==$DocY8XBn+cySKW6u3ZXy6fKnjpYJpFoTeqe9W8VFYmzdR0y6oFZu40faVDe6Clnb+vrpElRQhXDoVmnESLNa2A==";
        Mockito.when(userManager.getApiToken(anyString())).thenReturn(Optional.of(hashedPassword));

        UsernamePasswordToken upToken = new UsernamePasswordToken("test.user", "secret123");
        AuthenticationInfo authInfo = oauth2ProxyRealm.doGetAuthenticationInfo(upToken);
        String primaryPrincipal = (String) authInfo.getPrincipals().getPrimaryPrincipal();
        assertNotNull(authInfo);
        assertEquals("test.user", primaryPrincipal);
        upToken = new UsernamePasswordToken("test.user", "foobar123");
        assertNull(oauth2ProxyRealm.doGetAuthenticationInfo(upToken));
    }

    @Test
    void testEnsureUserLoginTimestampSchema() {

    }

    @Test
    void testGenerateSecureRandomString() {
        final String allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        String first = OAuth2ProxyRealm.generateSecureRandomString(32);
        assertNotNull(first);
        assertTrue(first.chars().allMatch(c -> allowedCharacters.indexOf(c) != -1));
        String second = OAuth2ProxyRealm.generateSecureRandomString(32);
        assertNotEquals(first, second);
        assertTrue(second.chars().allMatch(c -> allowedCharacters.indexOf(c) != -1));
    }

    @Test
    void testSupports() {

        BearerToken wrongToken = new BearerToken("foobar");
        assertFalse(oauth2ProxyRealm.supports(wrongToken));

        UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("foo", "bar");
        assertTrue(oauth2ProxyRealm.supports(usernamePasswordToken));

        OAuth2ProxyHeaderAuthToken proxyHeaderToken = new OAuth2ProxyHeaderAuthToken();
        assertTrue(oauth2ProxyRealm.supports(proxyHeaderToken));
    }

    @Test
    void testDoCredentialsMatch() {
        // this realm does not handle anything credential-related
        // (delegated to oauth2 proxy)
        assertTrue(oauth2ProxyRealm.getCredentialsMatcher().doCredentialsMatch(null, null));
    }

    @Test
    void testRecordLogin() {
        // causes an exception due to db absence
        oauth2ProxyRealm.recordLogin("foo123");
        verify(logger).error(eq("Failed to persist login timestamp for user {} - {}"), anyString(),
                any(Exception.class));

        // mock db
        DatabaseInstance dbInstance = Mockito.mock(DatabaseInstance.class);
        ODatabaseDocumentTx docTx = Mockito.mock(ODatabaseDocumentTx.class);
        Mockito.when(dbInstance.acquire()).thenReturn(docTx);

        // mock login records
        ODocument userLogin = Mockito.mock(ODocument.class);
        Mockito.when(userLogin.field(OAuth2ProxyRealm.FIELD_USER_ID)).thenReturn("test.user");
        Mockito.when(userLogin.field(OAuth2ProxyRealm.FIELD_LAST_LOGIN)).thenReturn(new Date(0));
        Mockito.when(userLogin.field(anyString(), any(Date.class))).thenReturn(userLogin);

        List<ODocument> userLogins = Arrays.asList(userLogin);
        Mockito.when(docTx.query(any(), any())).thenReturn(userLogins);

        oauth2ProxyRealm = getTestRealm(dbInstance, null, null);
        oauth2ProxyRealm.recordLogin("bar123");
        Mockito.verify(userLogin).save();
        Mockito.verify(docTx).commit();

        Mockito.reset(docTx);
        Mockito.reset(userLogin);

        // first login case
        Mockito.when(docTx.query(any(), any())).thenReturn(Collections.emptyList());
        // override the internal creation of a login document
        OAuth2ProxyRealm spyRealm = Mockito.spy(oauth2ProxyRealm);
        ODocument mockDocument = Mockito.mock(ODocument.class);
        Mockito.doReturn(mockDocument).when(spyRealm).createUserLoginDoc(anyString());
        spyRealm.recordLogin("baz123");
        Mockito.verify(mockDocument).save();
        Mockito.verify(docTx).commit();
    }

    @Test
    void testUserWithPrincipals() {
        UserWithPrincipals newUser = new UserWithPrincipals();
        assertFalse(newUser.hasPrincipals());

        newUser.addPrincipal("test.user", "TestAuthRealm");
        assertTrue(newUser.hasPrincipals());
    }

    @Test
    void testSyncExternalRolesForGroups() throws UserNotFoundException {
        OAuth2ProxyRoleStore roleStore = Mockito.mock(OAuth2ProxyRoleStore.class);
        OAuth2ProxyUserManager userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        oauth2ProxyRealm = getTestRealm(null, userManager, roleStore);

        User user = OAuth2ProxyUserManager.createUserObject("test.user", "test.user@example.com");

        // user had another idp group before
        user.addRole(new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, "other@idm.example.com"));
        // and was assigned a group from default source
        user.addRole(new RoleIdentifier(UserManager.DEFAULT_SOURCE, "nx-big-boss"));

        String groups = "administrators@idm.example.com,devs@idm.example.com";

        oauth2ProxyRealm.syncExternalRolesForGroups(user, groups);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userManager, times(2)).updateUser(userCaptor.capture());
        List<User> capturedUsers = userCaptor.getAllValues();
        user = capturedUsers.get(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<RoleIdentifier>> roleCaptor = ArgumentCaptor.forClass(Set.class);
        verify(roleStore).addRolesIfMissing(roleCaptor.capture());
        Set<RoleIdentifier> capturedRoles = roleCaptor.getValue();
        Set<RoleIdentifier> testGroups = Stream.of(groups.split(","))
                .map(group -> new RoleIdentifier(OAuth2ProxyUserManager.SOURCE, group)).collect(Collectors.toSet());
        assertEquals(testGroups, capturedRoles);

        assertTrue(user.getRoles().stream().anyMatch(
                role -> role.getRoleId().equals("administrators@idm.example.com")));
        assertTrue(user.getRoles().stream().anyMatch(
                role -> role.getRoleId().equals("devs@idm.example.com")));
        assertTrue(user.getRoles().stream().anyMatch(
                role -> role.getRoleId().equals("nx-big-boss")),
                "expected group sync to leave non-idp groups untouched");
        assertFalse(user.getRoles().stream().anyMatch(
                role -> role.getRoleId().equals("other@idm.example.com")));
    }

    @Test
    void testCreateUserLoginDoc() {
        ODocument doc = oauth2ProxyRealm.createUserLoginDoc("test.user");
        assertEquals("test.user", doc.field(OAuth2ProxyRealm.FIELD_USER_ID));
    }


    @BeforeEach
    void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        oauth2ProxyRealm = getTestRealm(null, null, null);

        // Force our logger into the class field
        Field loggerField = OAuth2ProxyRealm.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(oauth2ProxyRealm, logger);
    }

    private OAuth2ProxyRealm getTestRealm(DatabaseInstance dbInstance, OAuth2ProxyUserManager userManager,
            OAuth2ProxyRoleStore roleStore) {
        PasswordService passwordService = new DefaultSecurityPasswordService(Mockito.mock(PasswordService.class));

        if (dbInstance == null) {
            dbInstance = Mockito.mock(DatabaseInstance.class);
        }

        if (userManager == null) {
            userManager = Mockito.mock(OAuth2ProxyUserManager.class);
        }

        if (roleStore == null) {
            roleStore = Mockito.mock(OAuth2ProxyRoleStore.class);
        }

        OAuth2ProxyRealm realm = new OAuth2ProxyRealm(dbInstance, userManager, roleStore, passwordService);
        return realm;
    }

}
