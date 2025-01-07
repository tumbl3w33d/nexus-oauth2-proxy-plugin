package com.github.tumbl3w33d.logout;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.CapabilitySupport;

@Named(OAuth2ProxyLogoutCapabilityDescriptor.TYPE_ID)
public class OAuth2ProxyLogoutCapability extends CapabilitySupport<OAuth2ProxyLogoutCapabilityConfiguration> {

    private final OAuth2ProxyLogoutCapabilityConfigurationState state;

    @Inject
    public OAuth2ProxyLogoutCapability(OAuth2ProxyLogoutCapabilityConfigurationState state) {
        this.state = checkNotNull(state);
    }

    @Override
    protected OAuth2ProxyLogoutCapabilityConfiguration createConfig(Map<String, String> properties) {
        return new OAuth2ProxyLogoutCapabilityConfiguration(properties);
    }

    @Override
    protected void onActivate(OAuth2ProxyLogoutCapabilityConfiguration config) throws Exception {
        state.set(config);
    }

    @Override
    protected void onUpdate(OAuth2ProxyLogoutCapabilityConfiguration config) throws Exception {
        state.set(config);
    }

    @Override
    protected void onPassivate(OAuth2ProxyLogoutCapabilityConfiguration config) throws Exception {
        state.reset();
    }

    @Override
    protected void onRemove(OAuth2ProxyLogoutCapabilityConfiguration config) throws Exception {
        state.reset();
    }

}
