package com.github.tumbl3w33d;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

@AvailabilityVersion(from = "1.0")
@Named
@Singleton
public class OAuth2ProxyApiTokenInvalidateTaskDescriptor extends TaskDescriptorSupport {

    public static final String TYPE_ID = "oauth2-proxy-api-token.cleanup";

    public static final String CONFIG_IDLE_EXPIRY = TYPE_ID + "-expiry";
    public static final int CONFIG_IDLE_EXPIRY_DEFAULT = 30;
    private static final NumberTextFormField maxIdleAge = new NumberTextFormField(CONFIG_IDLE_EXPIRY, //
            "User idle time in days", //
            "After the user has been inactive for this amount of days the API token will be overwritten and the user must renew it interactively. Setting this to 0 or a negative value disables max idle time entirely. Default is "
                    + CONFIG_IDLE_EXPIRY_DEFAULT + " days.",
            FormField.MANDATORY)//
            .withMinimumValue(1)//
            .withInitialValue(CONFIG_IDLE_EXPIRY_DEFAULT);

    public static final String CONFIG_AGE = TYPE_ID + "-max-age";
    public static final int CONFIG_AGE_DEFAULT = -1;
    private static final NumberTextFormField maxAge = new NumberTextFormField(CONFIG_AGE, //
            "Max token age in days", //
            "After this amount of days the API token will be overwritten and the user must renew it interactively. Setting this to 0 or a negative value disables max token age entirely. Default is "
                    + CONFIG_AGE_DEFAULT + " days.",
            FormField.MANDATORY)//
            .withInitialValue(CONFIG_AGE_DEFAULT);

    public static final String NOTIFY = TYPE_ID + "-notify";
    public static final Boolean NOTIFY_DEFAULT = false;
    private static final CheckboxFormField notify = new CheckboxFormField(NOTIFY, //
            "Send Email on token invalidation", //
            "Defines whether an email is send to the affected user if their API token is invalidated automatically based on any condition. Default is "
                    + NOTIFY_DEFAULT,
            FormField.OPTIONAL)//
            .withInitialValue(NOTIFY_DEFAULT);

    @Inject
    public OAuth2ProxyApiTokenInvalidateTaskDescriptor() {
        super(TYPE_ID, OAuth2ProxyApiTokenInvalidateTask.class, "OAuth2 Proxy API token invalidator",
                TaskDescriptorSupport.VISIBLE, TaskDescriptorSupport.EXPOSED, TaskDescriptorSupport.REQUEST_RECOVERY,
                new FormField[] { maxIdleAge, maxAge, notify });
    }

    @Override
    public boolean allowConcurrentRun() {
        return false;
    }

}
