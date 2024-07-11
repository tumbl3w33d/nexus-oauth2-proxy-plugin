package com.github.tumbl3w33d;

import static java.util.Arrays.asList;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.sisu.Priority;
import org.eclipse.sisu.space.ClassSpace;

import org.sonatype.nexus.ui.UiPluginDescriptor;

@Named
@Singleton
@Priority(Integer.MAX_VALUE - 300)
public class OAuth2ProxyUiPluginDescriptor implements UiPluginDescriptor {

    private final List<String> scripts;

    private final List<String> styles;

    @Inject
    public OAuth2ProxyUiPluginDescriptor(final ClassSpace space) {
        scripts = asList("/static/nexus-oauth2-proxy-bundle.js");
        styles = asList("/static/rapture/resources/nexus-oauth2-proxy-bundle.css");
    }

    @Override
    public String getName() {
        return "nexus-oauth2-proxy-plugin";
    }

    @Nullable
    @Override
    public List<String> getScripts(final boolean isDebug) {
      return scripts;
    }

    @Nullable
    @Override
    public List<String> getStyles() {
      return styles;
    }
}