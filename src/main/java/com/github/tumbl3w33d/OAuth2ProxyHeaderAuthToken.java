package com.github.tumbl3w33d;

import org.apache.shiro.authc.HostAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;

public class OAuth2ProxyHeaderAuthToken implements HostAuthenticationToken {

    private static final long serialVersionUID = 234235998981350L;
    private final Logger logger = LoggerFactory.getLogger(OAuth2ProxyHeaderAuthToken.class);

    HttpHeaderAuthenticationToken user;
    HttpHeaderAuthenticationToken preferred_username;
    HttpHeaderAuthenticationToken email;
    HttpHeaderAuthenticationToken accessToken;
    HttpHeaderAuthenticationToken groups;

    String host;

    @Override
    public Object getPrincipal() {
        Object principal = preferred_username == null ? null : preferred_username.getHeaderValue();
        logger.debug("returning principal: {}", principal);
        return principal;
    }

    @Override
    public Object getCredentials() {
        logger.trace("there are no credentials for oauth2 proxy authentication - returning null");
        return null;
    }

    @Override
    public String getHost() {
        return this.host;
    }
}