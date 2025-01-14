package com.github.tumbl3w33d.logout;

import java.util.Collections;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.StateContributor;

@Named
@Singleton
public class OAuth2ProxyLogoutCapabilityConfigurationState implements StateContributor {

    private OAuth2ProxyLogoutCapabilityConfiguration config;

    @Override
    public Map<String, Object> getState() {
        if (config != null) {
            return Collections.singletonMap("oauth2-proxy-logout", config);
        }
        return null;
    }

    public OAuth2ProxyLogoutCapabilityConfiguration getConfig() {
        return config;
    }

    public void set(OAuth2ProxyLogoutCapabilityConfiguration config) {
        this.config = config;
    }

    public void reset() {
        this.config = null;
    }
}
