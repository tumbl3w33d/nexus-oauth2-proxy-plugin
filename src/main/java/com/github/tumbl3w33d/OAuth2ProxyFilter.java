package com.github.tumbl3w33d;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;

@Named
@Singleton
public class OAuth2ProxyFilter extends NexusAuthenticationFilter {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProxyFilter.class);

    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        return OAuth2ProxyHeaderAuthTokenFactory.OAUTH2_PROXY_HEADERS.stream()
                .anyMatch(header -> httpRequest.getHeader(header) != null);
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        HttpServletRequest webReq = WebUtils.toHttp(request);

        if (webReq.getHeader("Authorization") != null) {
            logger.debug("not handling requests with Authorization header");
            return null;
        }

        OAuth2ProxyHeaderAuthTokenFactory tokenFactory = new OAuth2ProxyHeaderAuthTokenFactory();

        logger.debug("creating token from OAuth2 proxy headers");
        return tokenFactory.createToken(request, response);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        // If it's a login attempt, try to execute the login.
        if (isLoginAttempt(request, response)) {
            return executeLogin(request, response);
        }
        // If it's not a login attempt, we deny access
        return false;
    }
}