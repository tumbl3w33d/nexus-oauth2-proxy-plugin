package com.github.tumbl3w33d;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;

public class OAuth2ProxyHeaderAuthTokenFactoryTest {

    static final String userUuid = UUID.randomUUID().toString();
    static final String username = "test.user";
    static final String mail = "test.user@example.com";
    static final String groups = "administrators@idm.example.com,devs@idm.example.com";
    static final String host = "127.0.0.1";

    @Test
    void testCreateToken() {
        OAuth2ProxyHeaderAuthTokenFactory factory = new OAuth2ProxyHeaderAuthTokenFactory();
        HttpServletRequest request = createMockRequestViaProxy(false);
        ServletResponse response = Mockito.mock(ServletResponse.class);

        AuthenticationToken token = factory.createToken(request, response);
        assertNotNull(token);
        assertDoesNotThrow(() -> (OAuth2ProxyHeaderAuthToken) token);
        OAuth2ProxyHeaderAuthToken proxyToken = (OAuth2ProxyHeaderAuthToken) token;
        assertToken(proxyToken);

        // programmatic access - accept requests with Authorization header AND complete oauth2 proxy headers
        // this allows oauth2 proxy's --skip-jwt-bearer-tokens option to be used for realising alternative oidc login flows
        // e.g. obtaining an access token via device flow, then using it to login to nexus without having to deal with redirects
        // in this case oauth2 proxy validates the token, populates all headers, but also forwards the Authorization header 
        Mockito.when(request.getHeader("Authorization")).thenReturn("Basic foo123==");
        assertNotNull(token);
        assertDoesNotThrow(() -> (OAuth2ProxyHeaderAuthToken) token);
        proxyToken = (OAuth2ProxyHeaderAuthToken) token;
        assertToken(proxyToken);

        // programmatic access - reject requests with Authorization header but with incomplete oauth2 proxy headers
        Mockito.when(request.getHeader("Authorization")).thenReturn("Basic foo123==");
        Mockito.when(request.getHeader(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_USER)).thenReturn(null);
        assertNull(factory.createToken(request, response));
    }

    private static void assertToken(OAuth2ProxyHeaderAuthToken proxyToken) {
        assertEquals(username, proxyToken.getPrincipal());
        assertNull(proxyToken.getCredentials());
        assertEquals(userUuid, proxyToken.user.getHeaderValue());
        assertEquals(username, proxyToken.preferred_username.getHeaderValue());
        assertEquals(mail, proxyToken.email.getHeaderValue());
        assertEquals(groups, proxyToken.groups.getHeaderValue());
        assertEquals(host, proxyToken.getHost());
        assertNull(proxyToken.accessToken);
    }

    static HttpServletRequest createMockRequestViaProxy(boolean fakeMissingHeader) {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getHeader(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_USER)).thenReturn(userUuid);
        if (!fakeMissingHeader) {
            Mockito.when(request.getHeader(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_PREFERRED_USERNAME))
                    .thenReturn(username);
        }
        Mockito.when(request.getHeader(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_EMAIL)).thenReturn(mail);
        Mockito.when(request.getHeader(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_GROUPS)).thenReturn(groups);
        Mockito.when(request.getRemoteHost()).thenReturn(host);
        return request;
    }

    @Test
    void testGetHttpHeaderNames() {
        OAuth2ProxyHeaderAuthTokenFactory factory = new OAuth2ProxyHeaderAuthTokenFactory();
        List<String> headers = factory.getHttpHeaderNames();
        headers.containsAll(ImmutableSet.of(OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_USER,
                OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_PREFERRED_USERNAME,
                OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_EMAIL,
                OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_GROUPS,
                OAuth2ProxyHeaderAuthTokenFactory.X_FORWARDED_ACCESS_TOKEN));
    }
}
