package com.github.tumbl3w33d.logout;

import java.util.Map;
import java.util.Objects;

public class OAuth2ProxyLogoutCapabilityConfiguration {

    public static final String LOGOUT_URL_ID = "oauth2-proxy-logout-url";
    public static final String LOGOUT_URL_LABEL = "OAuth2 Proxy logout url";
    public static final String LOGOUT_URL_HELP = "URL to be called for backchannel logout in OAuth2 Proxy when the Nexus logout button is pressed. Defaults to '{nexus-base-url}/oauth2/sign_out' if not specified";

    private String logoutUrl;

    public OAuth2ProxyLogoutCapabilityConfiguration(Map<String, String> properties) {
        if (properties != null) {
            logoutUrl = properties.get(LOGOUT_URL_ID);
        }
    }

    public String getLogoutUrl() {
        return logoutUrl;
    }

    public void setLogoutUrl(String logoutUrl) {
        this.logoutUrl = logoutUrl;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logoutUrl);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OAuth2ProxyLogoutCapabilityConfiguration other = (OAuth2ProxyLogoutCapabilityConfiguration) obj;
        return Objects.equals(logoutUrl, other.logoutUrl);
    }

    @Override
    public String toString() {
        return "OAuth2ProxyLogoutCapabilityConfiguration [logoutUrl=" + logoutUrl + "]";
    }

}
