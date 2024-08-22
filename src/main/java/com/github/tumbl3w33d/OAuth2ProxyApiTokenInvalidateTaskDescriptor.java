package com.github.tumbl3w33d;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@Named
@Singleton
public class OAuth2ProxyApiTokenInvalidateTaskDescriptor
 extends TaskDescriptorSupport  {

    public static final String TYPE_ID = "oauth2-proxy-api-token.cleanup";

    public static final String CONFIG_EXPIRY = TYPE_ID + "-expiry";
        private static final NumberTextFormField field = new
    NumberTextFormField(CONFIG_EXPIRY,
    "Expiration in days",
    "After this duration the API token will be overwritten and the user must renew it interactively."
    ,
    FormField.MANDATORY).withMinimumValue(1).withInitialValue(30);
    
    @Inject
    public OAuth2ProxyApiTokenInvalidateTaskDescriptor() {
        super(TYPE_ID, OAuth2ProxyApiTokenInvalidateTask.class,       "OAuth2 Proxy API token invalidator",
        TaskDescriptorSupport.VISIBLE, TaskDescriptorSupport.EXPOSED,
        TaskDescriptorSupport.REQUEST_RECOVERY, new FormField[] { field });
    }
    
    @Override
    public boolean allowConcurrentRun() {
        return false;
    }
    
}
