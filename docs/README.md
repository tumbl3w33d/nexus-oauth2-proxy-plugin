⚠️ Note: With 3.78 Sonatype [broke the loading of custom plugins](https://community.sonatype.com/t/custom-plugins-still-possible-starting-with-3-78/14589). Do not upgrade to that version or higher unless you have a solution for that problem. Unfortunately, there is nothing that can be done about it for now.

# OpenID Connect for Sonatype Nexus Artifact Repository

This plugin has been developed to facilitate the integration of Nexus with any identity provider that is compatible with [OAuth2 Proxy](https://github.com/oauth2-proxy/oauth2-proxy).

Rather than executing its own OIDC (OpenID Connect) authentication flow, this plugin leverages OAuth2 Proxy to undertake the authentication process, relying on it to provide the necessary information through headers.

Furthermore, acknowledging the importance of non-interactive programmatic access within the Nexus environment, this plugin incorporates an API token feature. The plugin introduces an additional endpoint that allows authenticated users to reset their own API token to a system-generated one via the Nexus UI, with the caveat that this token is displayed solely once and is subject to reset with each access of this user menu item.

## Disclaimer

It is important to highlight that this plugin is provided on an 'as-is' basis, without any form of express or implied warranty. Under no circumstances shall the authors be held accountable for any damages or liabilities arising from the utilization of this plugin. Users are advised to proceed at their own risk.

## Features

* makes use of several headers sent by OAuth2 proxy (depending on its configuration)
  * see constants in [OAuth2ProxyHeaderAuthTokenFactory](../src/main/java/com/github/tumbl3w33d/OAuth2ProxyHeaderAuthTokenFactory.java)
  * creates an AuthenticationToken used by Nexus
* creates a user in a dedicated database table (i.e., not where Nexus checks for 'Local' users) if none with the given id (`preferred_username`) exists
  * anyone authenticated with your identity provider can access Nexus
  * you would control access by granting necessary scopes accessing OAuth2 Proxy only to eligible user groups
  * user creation currently has a rather simplistic strategy to extract `<firstname>.<lastname>` from `preferred_username`
* group/scope to role sync
  * if you configure OAuth2 Proxy with the well-known `groups` claim, it will retrieve that information from the identity provider
  * the groups received in the related header will be stored in a dedicated database table and become available for the 'external role mapping' functionality
  * ⚠️ note: [currently it is necessary](https://github.com/tumbl3w33d/nexus-oauth2-proxy-plugin/issues/26) to use this mapping mechanism because assigning Nexus' default roles to users created via plugin has no effect
* automatic expiry of API tokens
  * there is a configurable task that lets API tokens expire, so another login and manual renewal by the user is necessary
  * by default, the token will expire after 30 days of inactivity. As long as the user keeps showing up regularly, their token will not expire
  * The expiration can be configured as a regular nexus task. You can:
    * adjust inactivity period that leads to token invalidation
    * enable and set max token age, meaning the token automatically expires after a certain time, regardless of activity
    * enable mail notifications on token expiry (requires having a mail server configured in Nexus to work)
* backchannel logout in IDP via oauth2 proxy (if supported) when the logout is performed in Nexus
  * make sure to enable the OAuth2 Proxy: Logout capability in Nexus to make this work

**Note**: If the OAuth2 Proxy: Logout capability is not enabled, the logout button is non-operative, which is a common limitation with header-based authentication methods. To force a logout, you need to logout from your identity provider and/or delete the OAuth2 Proxy cookie if you must logout for some reason.

## Supported Nexus version

**Note**: See the warning on top of this document.

This plugin moves along with the latest OSS version of Nexus.

When they introduce breaking changes, like the change of underlying database with version 3.71.0, this results in a new major version of this plugin being released when adjustments have been made. You are free to use older versions but they will probably not receive maintenance, unless you contribute it yourself. In addition, as long as the user base is small and quiet, there will not be much effort invested in adding complex migration logic. Since this plugin is mostly developed for internal use (so far), an appropriate solution for that use case will be found and that might mean dropping existing data (which basically means persisted API tokens) and start over in order keep things simple.

## Installation

The recommended way to install the plugin is by dropping the `.kar` into the Nexus `/deploy` folder, as described [here](https://sonatype-nexus-community.github.io/nexus-development-guides/plugin-install.html#more-permanent-install).

Once Nexus picked it up, it will offer you to activate the new realm this plugin comes with:

![alt text](images/realm_installation.png)

You can see the `OAuth2ProxyRealm` activated and first in the list, so it takes effect first in the authentication chain.

Now you should be able to access the Nexus via your OAuth2 Proxy and end up logged in. If you encounter strange behavior during this activation phase, it is easiest to test with a private browser window to rule out cached cookies or basic authentication can interfere.

Users that logged in using this realm will now appear in the Nexus administration section under `Security -> Users -> Source: OAuth2Proxy`.

You also want to visit `System -> Tasks` where you will find a new task for invalidating API tokens for users who did not show up for a while. While it has a default of 30d defined in the code, it appeared that it does not take effect until once set and saved via UI, so make sure you set an appropriate value there.

## Necessary infrastructure

You typically put an OAuth2 Proxy in front of your application and make sure that related `X-Forwarded-` headers do not reach the application other than those originating from the OAuth2 Proxy.

For non-interactive programmatic access you circumvent the OAuth2 Proxy and go straight to the Nexus application. To achieve that, you could check for the presence of an `Authorization: Basic` header earlier in the chain of proxies. In that case the required credentials are the user's id and the generated API token.

## Example with HAProxy as entrypoint

```apacheconf
# the entrypoint to your nexus + oauth2 proxy setup
frontend you-name-it
  # just illustrating that you must ensure TLS
  bind *:443 ssl crt /usr/local/etc/haproxy/cert alpn h2,http/1.1

  # if this is too invasive for your use case, be more specific
  http-request del-header ^X-Forwarded.*

  # use case: artifact download from /repository via UI
  use_backend be_oauth2-proxy if { req.cook(_oauth2_proxy) -m found }

  # use case: programmatic access, circumvent oauth2 proxy
  acl is_basic_auth hdr_beg(Authorization) -i basic
  acl is_repo_req path_beg /repository/
  use_backend be_nexus if is_basic_auth OR is_repo_req

  # use case: interactive access via browser
  default_backend oauth2-proxy


backend oauth2-proxy
  # interactive OIDC login
  option httpchk GET /ping
  server oauth2-proxy oauth2-proxy:4180 check


backend nexus
  # non-interactive programmatic access
  server nexus nexus:8081 check
```

## Example with Nginx as entrypoint

```apacheconf
...
...
# The block server only
server {
    listen 443 ssl;
    server_name your_nexus_host;

    proxy_headers_hash_bucket_size 128;

    ssl_certificate /etc/nginx/certs/server-tls.crt;
    ssl_certificate_key /etc/nginx/certs/user.key;


    location / {
        # Clear existing headers that will be added upstream by oauth2-proxy (Optional, depends on your config)
        proxy_set_header X-Forwarded-Email "";
        proxy_set_header X-Forwarded-Groups "";
        proxy_set_header X-Forwarded-User "";

        # Set proxy pass headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Cookie $http_cookie;

        proxy_pass http://oauth2-proxy:4180;

        # Usually oauth2-proxy delivers the login screen with a 403 status code.
        # Safari can't handle that so we intercept 403 errors and return 200 instead.
        proxy_intercept_errors on;
        error_page 403 =200 @change_status;
    }

    location @change_status {
        # proxy to the auth-proxy, but this time with status code 200
        proxy_pass http://auth2-proxy:4180;
    }
}
...
...
```

## Example OAuth2 Proxy config

```apacheconf
reverse_proxy = true

http_address = "0.0.0.0:4180"

email_domains = [ "*" ]

# make sure to request group information for mapping to roles
scope = "openid email profile groups"

# your nexus is the backend
upstreams = ["http://nexus:8081"]

provider = "oidc"
oidc_issuer_url = "https://idm.example.com/consult/your/idp-documentation"
code_challenge_method = "S256" # PKCE, if your idp supports that
client_id = "get the client id from your identity provider"
client_secret = "get the secret from your identity provider"
cookie_secret = "generate an individual cookie secret"
backend_logout_url = "https://idm.example.com/consult/your/idp-documentation/for/logout-url?id_token_hint={id_token}"

# we don't need to wait for people to press the button, just redirect
skip_provider_button = true
```

**Note**: Depending on the amount of data the OAuth2 Proxy receives from your IDP (especially list of groups) you might want to look into [changing its session storage to redis/valkey](https://oauth2-proxy.github.io/oauth2-proxy/configuration/session_storage/#redis-storage). The proxy will warn you about data exceeding limits which results in multiple cookies being set for the proxy session.

## Optional: Bearer token authentication and token rotation

If for some reason you need to use a Bearer token for machine-to-machine communcation or in general accessing Nexus programmatically e.g. because corporate guidelines prevent you from using Basic Auth using the api token, it is possible to set this up: 

Add 'skip_jwt_bearer_tokens = true' to your OAuth2 Proxy configuration. This flag makes OAuth2 Proxy optionally accept Bearer tokens instead of performing the auth flow itself as long as the Bearer token is valid and the audience matches the configured client id. For details, check the [official documentation](https://oauth2-proxy.github.io/oauth2-proxy/configuration/overview/). OAuth2 Proxy will then populate the x-forwarded headers based on information from this token, so for Nexus the login mechanism is still transparent.

Leveraging this way of authenticating it is possible to create an automatic token rotation even if the API token is invalidated by obtaining the Bearer token via some kind of OIDC login and then using it to perform an authenticated REST call  against https://your_nexus_host/service/rest/oauth2-proxy/user/reset-token to obtain a new API token. Keep in mind that this REST call immediately invalidates the old token! Also make sure your reverse proxy is configured to route this URL to Nexus via OAuth2 Proxy (if you use the above example configs, this should automatically be the case)

## Troubleshooting

If you encounter authentication issues, you can activate logging for the plugin classes by creating a logger in the Nexus administration section (`Support -> Logging -> Create Logger`), e.g. for the top level package `com.github.tumbl3w33d`.

## Use with Authentik

A user of the plugin [successfully configured their Authentik installation](https://github.com/tumbl3w33d/nexus-oauth2-proxy-plugin/issues/25#issuecomment-2563165385) in place of OAuth2 Proxy. While this setup is not being tested during development, it will probably work fine.
