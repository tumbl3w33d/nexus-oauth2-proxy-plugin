package com.github.tumbl3w33d.logout;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.capability.CapabilityDescriptorSupport;
import org.sonatype.nexus.capability.CapabilityType;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import com.google.common.collect.Lists;

@AvailabilityVersion(from = "1.0")
@Named(OAuth2ProxyLogoutCapabilityDescriptor.TYPE_ID)
@Singleton
public class OAuth2ProxyLogoutCapabilityDescriptor
        extends CapabilityDescriptorSupport<OAuth2ProxyLogoutCapabilityConfiguration> {

    public static final String TYPE_ID = "oauth2-proxy.logout";
    public static final CapabilityType TYPE = CapabilityType.capabilityType(TYPE_ID);

    @SuppressWarnings("rawtypes")
    private final List<FormField> formFields;

    public OAuth2ProxyLogoutCapabilityDescriptor() {
        formFields = Lists.newArrayList(new StringTextFormField(OAuth2ProxyLogoutCapabilityConfiguration.LOGOUT_URL_ID,
                OAuth2ProxyLogoutCapabilityConfiguration.LOGOUT_URL_LABEL,
                OAuth2ProxyLogoutCapabilityConfiguration.LOGOUT_URL_HELP, FormField.OPTIONAL));
    }

    @Override
    public CapabilityType type() {
        return TYPE;
    }

    @Override
    public String name() {
        return "OAuth2 Proxy: Logout";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<FormField> formFields() {
        return formFields;
    }

    @Override
    protected OAuth2ProxyLogoutCapabilityConfiguration createConfig(Map<String, String> properties) {
        return new OAuth2ProxyLogoutCapabilityConfiguration(properties);
    }

    @Override
    protected String renderAbout() throws Exception {
        return "Specify settings regarding logout of OAuth2 Proxy triggered by Nexus. If this capability is disabled, no OAuth2 Proxy logout will be performed, effectively rendering the Nexus logout button ineffective";
    }

}
