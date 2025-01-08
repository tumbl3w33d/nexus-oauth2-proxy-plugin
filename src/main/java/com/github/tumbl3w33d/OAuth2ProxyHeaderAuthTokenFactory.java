package com.github.tumbl3w33d;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationToken;
import org.sonatype.nexus.security.authc.HttpHeaderAuthenticationTokenFactorySupport;

@Named
@Singleton
public class OAuth2ProxyHeaderAuthTokenFactory extends HttpHeaderAuthenticationTokenFactorySupport {

    private final Logger logger = LoggerFactory.getLogger(OAuth2ProxyHeaderAuthTokenFactory.class);

    static final String X_FORWARDED_USER = "X-Forwarded-User";
    static final String X_FORWARDED_PREFERRED_USERNAME = "X-Forwarded-Preferred-Username";
    static final String X_FORWARDED_EMAIL = "X-Forwarded-Email";
    static final String X_FORWARDED_ACCESS_TOKEN = "X-Forwarded-Access-Token";
    static final String X_FORWARDED_GROUPS = "X-Forwarded-Groups";

    static final List<String> OAUTH2_PROXY_HEADERS = Collections.unmodifiableList(Arrays.asList(X_FORWARDED_USER,
            X_FORWARDED_PREFERRED_USERNAME, X_FORWARDED_EMAIL, X_FORWARDED_ACCESS_TOKEN, X_FORWARDED_GROUPS));

    @Override
    public AuthenticationToken createToken(ServletRequest request, ServletResponse response) {

        HttpServletRequest httpRequest = WebUtils.toHttp(request);
        String xForwardedUserHeader = httpRequest.getHeader(X_FORWARDED_USER);
        String xForwardedEmailHeader = httpRequest.getHeader(X_FORWARDED_EMAIL);
        String xForwardedPrefUsernameHeader = httpRequest.getHeader(X_FORWARDED_PREFERRED_USERNAME);

        if (xForwardedUserHeader == null || xForwardedEmailHeader == null || xForwardedPrefUsernameHeader == null) {
            // any proxy header is missing...
            if (httpRequest.getHeader("Authorization") == null) {
                // ...and Auth header is missing too -> probably UI based access
                logger.debug("required OAuth2 proxy headers incomplete - {}: {} - {}: {} - {}: {}", X_FORWARDED_USER,
                        xForwardedUserHeader, X_FORWARDED_EMAIL, xForwardedEmailHeader, X_FORWARDED_PREFERRED_USERNAME,
                        xForwardedPrefUsernameHeader);
                return null;
            } else {
                // ...and Auth header is present -> some access bypassing oauth2 proxy, so we are not responsible
                logger.debug("not handling requests with Authorization header, but without any oauth2 proxy headers");
                return null;
            }
        }

        OAuth2ProxyHeaderAuthToken token = new OAuth2ProxyHeaderAuthToken();

        token.user = new HttpHeaderAuthenticationToken(X_FORWARDED_USER, xForwardedUserHeader, request.getRemoteHost());

        token.email = new HttpHeaderAuthenticationToken(X_FORWARDED_EMAIL, xForwardedEmailHeader,
                request.getRemoteHost());

        token.preferred_username = new HttpHeaderAuthenticationToken(X_FORWARDED_PREFERRED_USERNAME,
                xForwardedPrefUsernameHeader, request.getRemoteHost());

        // (unused) depending on oauth2 proxy config, this might be missing
        String accessToken = httpRequest.getHeader(X_FORWARDED_ACCESS_TOKEN);
        if (accessToken != null && !accessToken.isEmpty()) {
            token.accessToken = new HttpHeaderAuthenticationToken(X_FORWARDED_ACCESS_TOKEN, accessToken,
                    request.getRemoteHost());
        }

        // depending on oauth2 proxy claims, this might be missing
        String groups = httpRequest.getHeader(X_FORWARDED_GROUPS);
        if (groups != null && !groups.isEmpty()) {
            token.groups = new HttpHeaderAuthenticationToken(X_FORWARDED_GROUPS, groups, request.getRemoteHost());
        }

        token.host = request.getRemoteHost();

        logger.debug(
                "created token from oauth2 proxy headers: user: {} - preferred_username: {} - email: {} - access token: {} - groups: {}",
                token.user.getHeaderValue(), token.preferred_username.getHeaderValue(), token.email.getHeaderValue(),
                token.accessToken != null ? "<hidden>" : null,
                token.groups != null ? token.groups.getHeaderValue() : null);

        if (httpRequest.getHeader("Authorization") == null) {
            // "normal" oauth2 login, probably via UI -> create a user session

            // NexusBasicHttpAuthenticationFilter which is for reasons Sonatype itself does not know the root of the 
            // inheritance hierarchy of nexus auth filters turns off shiro session creation by setting this flag to false

            // Nexus solely relies on the fact that the session is manually created by POSTing to SessionServlet as part of the login dialog
            // As we never get a login dialog, this does not trigger, which means there is no user session ever created.

            // While that does not hurt normal login as we continuously login with the oauth proxy anyways, 
            // it causes the logout button to throw exceptions instead of triggering a LogoutEvent we can listen to for redirecting
            // logout to the oauth proxy
            request.setAttribute(DefaultSubjectContext.SESSION_CREATION_ENABLED, Boolean.TRUE);
        } else {
            // most likely programmatic access making use of skip_jwt_bearer_tokens option in oauth2 proxy, meaning an already existing 
            // jwt token not created (but validated!) by oauth2 proxy is used for login. In this case we don't need a session, so nothing else to do
        }

        return token;

    }

    @Override
    protected List<String> getHttpHeaderNames() {
        return OAUTH2_PROXY_HEADERS;
    }
}