
package com.github.tumbl3w33d.users.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.NoSuchUserManagerException;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.github.tumbl3w33d.OAuth2ProxyRealm;
import com.github.tumbl3w33d.users.OAuth2ProxyUserManager;

@Named
@Singleton
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/oauth2-proxy/user")
public class OAuth2ProxyUserResource extends ComponentSupport implements Resource {

    private final SecuritySystem securitySystem;

    @Inject
    public OAuth2ProxyUserResource(final SecuritySystem securitySystem) {
        this.securitySystem = checkNotNull(securitySystem);
    }

    private User getCurrentUser() throws UserNotFoundException {
        User user = securitySystem.currentUser();
        if (user == null) {
            throw new UserNotFoundException("Unable to get current user");
        }
        return user;
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

            securitySystem.changePassword(user.getUserId(), generatedToken, true);

            log.debug("user {} reset their api token", user.getUserId());

        } catch (UserNotFoundException e) {
            log.debug("user not found for token reset");
            throw new WebApplicationMessageException(Status.NOT_FOUND, "no user found for token reset",
                    MediaType.APPLICATION_JSON);
        }

        return generatedToken;
    }

    @DELETE
    @Path("/{userId}")
    @RequiresAuthentication
    @RequiresPermissions("nexus:users:delete")
    public void deleteUser(@PathParam("userId") final String userId) {
        User user = null;
        try {
            user = securitySystem.getUser(userId, OAuth2ProxyUserManager.SOURCE);

            securitySystem.deleteUser(userId, user.getSource());

        } catch (NoSuchUserManagerException e) {
            throw new WebApplicationMessageException(Status.INTERNAL_SERVER_ERROR,
                    "unable to access related user manager",
                    MediaType.APPLICATION_JSON);
        } catch (UserNotFoundException e) {
            log.debug("unable to retrieve user with id {} for deletion - {}", userId, e);
            throw new WebApplicationMessageException(Status.NOT_FOUND, "no such user", MediaType.APPLICATION_JSON);
        }
    }

}