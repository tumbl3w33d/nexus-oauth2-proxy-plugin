package com.github.tumbl3w33d;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@Named
@Singleton
public class OAuth2ProxyApiTokenInvalidateTaskDescriptor
        extends TaskDescriptorSupport {
    public static final String TYPE_ID = "oauth2-proxy-api-token.cleanup";

    @Inject
    public OAuth2ProxyApiTokenInvalidateTaskDescriptor() {
        super(TYPE_ID, OAuth2ProxyApiTokenInvalidateTask.class, "OAuth2 Proxy API token invalidator",
                TaskDescriptorSupport.NOT_VISIBLE, TaskDescriptorSupport.NOT_EXPOSED);
    }
}
