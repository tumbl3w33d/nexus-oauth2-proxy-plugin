package com.github.tumbl3w33d;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.rest.Resource;

import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.anonymous.AnonymousHelper.getAuthenticationRealms;

@Named
@Singleton
@Consumes(MediaType.TEXT_PLAIN)
@Path("/oauth2-proxy-api-token")
public class OAuth2ProxyApiTokenEndpoint implements Resource {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProxyApiTokenEndpoint.class);

    private static final String ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final RealmManager realmManager;

    private final List<String> authenticationRealms;

    @Inject
    public OAuth2ProxyApiTokenEndpoint(
            final RealmManager realmManager,
            List<UserManager> userManagers) {
        this.realmManager = checkNotNull(realmManager);
        checkNotNull(userManagers);
        authenticationRealms = getAuthenticationRealms(userManagers);
    }

    public static String generateRandomString(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALLOWED_CHARACTERS.length());
            char randomChar = ALLOWED_CHARACTERS.charAt(randomIndex);
            sb.append(randomChar);
        }

        return sb.toString();
    }

    @POST
    @Path("/reset-token")
    // @RequiresPermissions("nexus:settings:read")
    @Produces(MediaType.TEXT_PLAIN)
    public String resetToken() {
        /*
         * return realmManager.getAvailableRealms(true).stream()
         * .filter(securityRealm ->
         * authenticationRealms.contains(securityRealm.getId()))
         * .collect(toList());
         */
        String generatedToken = generateRandomString(32);
        return generatedToken;
    }
}
