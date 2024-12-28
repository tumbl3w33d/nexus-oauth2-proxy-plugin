package com.github.tumbl3w33d;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.security.authc.LogoutEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

@Singleton
@Named
public class OAuth2ProxyLogoutHandler {

    private final Logger logger = LoggerFactory.getLogger(OAuth2ProxyLogoutHandler.class);

    @AllowConcurrentEvents
    @Subscribe
    public void handle(final LogoutEvent event) {
        if (OAuth2ProxyRealm.NAME.equals(event.getRealm())) {
            logger.debug("Triggering OAuth2 proxy logout for user " + event.getPrincipal());
            URI logoutUrl = URI.create(joinUri(BaseUrlHolder.get(), "oauth2/sign_out"));
            BasicCookieStore cookieStore = prepareCookieStore(logoutUrl);
            try (CloseableHttpClient client = constructClient(cookieStore)) {
                performOAuth2ProxyLogout(client, logoutUrl);
                logger.info("User " + event.getPrincipal() + " was logged out from oauth2 proxy successfully");
            } catch (IOException e) {
                logger.error("Failed to logout from oauth2 proxy", e);
            }
        }
    }

    private String joinUri(String... parts) {
        return Arrays.stream(parts).map(this::stripLeadingAndTrailingSlashes).collect(Collectors.joining("/"));
    }

    private String stripLeadingAndTrailingSlashes(String s) {
        if (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private BasicCookieStore prepareCookieStore(URI logoutUrl) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        HttpServletRequest request = WebUtils.getHttpRequest(SecurityUtils.getSubject());
        Arrays.stream(request.getCookies()).map(c -> toHttpCookie(logoutUrl, c)).forEach(c -> cookieStore.addCookie(c));
        return cookieStore;
    }

    private BasicClientCookie toHttpCookie(URI logoutUrl, Cookie cookie) {
        BasicClientCookie result = new BasicClientCookie(cookie.getName(), cookie.getValue());
        result.setDomain(logoutUrl.getHost());
        result.setPath("/");
        return result;
    }

    private CloseableHttpClient constructClient(BasicCookieStore cookieStore) {
        return HttpClients.custom()//
                .disableCookieManagement() // disable response cookie processing as we don't care
                .setDefaultCookieStore(cookieStore) // set cookie store containing the servlet requests cookies
                .addInterceptorLast(new RequestAddCookies()) // disable also disabled request cookie processing, re-enable it manually
                .build();
    }

    private void performOAuth2ProxyLogout(CloseableHttpClient client, URI logoutUrl) throws IOException {
        HttpGet req = new HttpGet(logoutUrl);
        req.setConfig(constructRequestConfig());

        // oauth2 proxy will respond with 302, which means success
        try (CloseableHttpResponse resp = client.execute(req)) {
            if (resp.getStatusLine().getStatusCode() == 302) {
                // pass the Set-Cookie header(s) to the frontend caller so the client session is invalidated as well
                HttpServletResponse response = WebUtils.getHttpResponse(SecurityUtils.getSubject());
                Arrays.stream(resp.getHeaders("Set-Cookie"))
                        .forEach(h -> response.addHeader("Set-Cookie", h.getValue()));
            }
        }
    }

    private RequestConfig constructRequestConfig() {
        return RequestConfig.copy(RequestConfig.DEFAULT)//
                .setRedirectsEnabled(false) // don't follow the redirect oauth2 proxy will respond with
                .setConnectTimeout(5000) // very limited waiting time, might be that the URL is not configured
                .build();
    }

}
