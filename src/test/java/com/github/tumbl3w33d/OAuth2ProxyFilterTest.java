package com.github.tumbl3w33d;

import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.createMockRequestViaProxy;
import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.groups;
import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.host;
import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.mail;
import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.userUuid;
import static com.github.tumbl3w33d.OAuth2ProxyHeaderAuthTokenFactoryTest.username;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class OAuth2ProxyFilterTest {

    private OAuth2ProxyFilter filter;

    @Test
    void testCreateToken() {
        HttpServletRequest request = createMockRequestViaProxy(false);
        ServletResponse response = Mockito.mock(ServletResponse.class);

        AuthenticationToken token = filter.createToken(request, response);
        assertNotNull(token);
        assertDoesNotThrow(() -> (OAuth2ProxyHeaderAuthToken) token);
        OAuth2ProxyHeaderAuthToken proxyToken = (OAuth2ProxyHeaderAuthToken) token;
        assertEquals(OAuth2ProxyHeaderAuthTokenFactoryTest.username, proxyToken.getPrincipal());
        assertNull(proxyToken.getCredentials());
        assertEquals(userUuid, proxyToken.user.getHeaderValue());
        assertEquals(username, proxyToken.preferred_username.getHeaderValue());
        assertEquals(mail, proxyToken.email.getHeaderValue());
        assertEquals(groups, proxyToken.groups.getHeaderValue());
        assertEquals(host, proxyToken.getHost());

        // incomplete set of proxy headers
        request = createMockRequestViaProxy(true);
        Mockito.reset(response);
        assertNull(filter.createToken(request, response));

        // programmatic access - reject requests with Authorization header
        Mockito.when(request.getHeader("Authorization")).thenReturn("Basic foo123==");

        assertNull(filter.createToken(request, response));
    }

    @Test
    void testIsLoginAttempt() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        assertFalse(filter.isLoginAttempt(request, response));

        request = createMockRequestViaProxy(false);
        assertTrue(filter.isLoginAttempt(request, response));
    }

    @Test
    void testOnAccessDenied() throws Exception {

        filter = Mockito.spy(new OAuth2ProxyFilter());
        Mockito.doReturn(true).when(filter).executeLogin(any(ServletRequest.class), any(ServletResponse.class));

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        ServletResponse response = Mockito.mock(ServletResponse.class);
        assertFalse(filter.onAccessDenied(request, response));

        request = createMockRequestViaProxy(false);
        boolean onAccessDeniedResult = filter.onAccessDenied(request, response);
        assertTrue(onAccessDeniedResult);
    }

    @BeforeEach
    private void prepareFilter() {
        filter = new OAuth2ProxyFilter();
    }
}
