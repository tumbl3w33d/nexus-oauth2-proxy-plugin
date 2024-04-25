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

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.rest.Resource;

import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.security.anonymous.AnonymousHelper.getAuthenticationRealms;

@Named
@Singleton
@Consumes(MediaType.TEXT_PLAIN)
@Path("/oauth2-proxy-api-token")
public class OAuth2ProxyApiTokenEndpoint extends ComponentSupport implements Resource {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProxyApiTokenEndpoint.class);

    

    private final SecuritySystem securitySystem;

    private final UserManager nexusAuthenticatingRealm;

    @Inject
    public OAuth2ProxyApiTokenEndpoint(
            final List<UserManager> userManagers,
            final SecuritySystem securitySystem) {
        this.securitySystem = checkNotNull(securitySystem);
        checkNotNull(userManagers);

        this.nexusAuthenticatingRealm = userManagers.stream()
                .filter(um -> um.getAuthenticationRealmName() == "NexusAuthenticatingRealm")
                .findFirst().get();
    }

    private User getCurrentUser() throws UserNotFoundException {
        User user = securitySystem.currentUser();
        if (user != null) {
            return user;
        } else {
            throw new UserNotFoundException("Unable to get current user");
        }
    }

    @POST
    @Path("/reset-token")
    @RequiresAuthentication
    @RequiresUser
    @Produces(MediaType.TEXT_PLAIN)
    public String resetToken() {
        String generatedToken = OAuth2ProxyRealm.generateSecureRandomString(32);

        try {
            User user = getCurrentUser();
            nexusAuthenticatingRealm.changePassword(user.getUserId(), generatedToken);
            logger.debug("user {} reset their api token", user.getUserId());

        } catch (UserNotFoundException e) {
            logger.error("user not found for token reset");
        }

        return generatedToken;
    }
}
