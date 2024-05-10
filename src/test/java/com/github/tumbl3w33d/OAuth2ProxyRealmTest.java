package com.github.tumbl3w33d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.security.user.UserManager;

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
    void testDoGetAuthenticationInfo() {

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

        UsernamePasswordToken wrongToken = new UsernamePasswordToken("foo", "bar");
        assertFalse(oauth2ProxyRealm.supports(wrongToken));

        OAuth2ProxyHeaderAuthToken rightToken = new OAuth2ProxyHeaderAuthToken();
        assertTrue(oauth2ProxyRealm.supports(rightToken));
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

        oauth2ProxyRealm = getTestRealm(dbInstance);
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

    @BeforeEach
    void setUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        oauth2ProxyRealm = getTestRealm(null);

        // Force our logger into the class field
        Field loggerField = OAuth2ProxyRealm.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(oauth2ProxyRealm, logger);
    }

    private OAuth2ProxyRealm getTestRealm(DatabaseInstance dbInstance) {
        UserManager mockUserManager = Mockito.mock(UserManager.class);
        Mockito.when(mockUserManager.getAuthenticationRealmName()).thenReturn("NexusAuthenticatingRealm");
        List<UserManager> userManagers = Arrays.asList(mockUserManager);

        if (dbInstance == null) {
            dbInstance = Mockito.mock(DatabaseInstance.class);
        }

        OAuth2ProxyRealm realm = new OAuth2ProxyRealm(userManagers, dbInstance);
        return realm;
    }

}
