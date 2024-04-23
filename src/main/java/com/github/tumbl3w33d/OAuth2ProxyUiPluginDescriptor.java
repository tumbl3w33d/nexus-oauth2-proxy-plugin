package com.github.tumbl3w33d;

import javax.annotation.Priority;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.rapture.UiPluginDescriptorSupport;

@Named
@Singleton
@Priority(Integer.MAX_VALUE - 200)
public class OAuth2ProxyUiPluginDescriptor extends UiPluginDescriptorSupport {
    public OAuth2ProxyUiPluginDescriptor() {
        super("nexus-oauth2-proxy-plugin"); // this must match the pom's artifactId
        setNamespace("NX.oauth2proxy");
        setConfigClassName("NX.oauth2proxy.app.PluginConfig");
    }
}