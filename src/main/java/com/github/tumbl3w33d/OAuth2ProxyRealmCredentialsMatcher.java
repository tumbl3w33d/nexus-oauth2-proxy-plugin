package com.github.tumbl3w33d;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.sonatype.goodies.common.ComponentSupport;

final class OAuth2ProxyRealmCredentialsMatcher extends ComponentSupport implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        log.debug("authInfo: {}", info);

        return true;
    }
}