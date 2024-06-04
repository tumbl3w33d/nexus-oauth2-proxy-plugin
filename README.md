# OpenID Connect for Sonatype Nexus Artifact Repository

This plugin has been developed to facilitate the integration of Nexus with any identity provider that is compatible with [OAuth2 Proxy](https://github.com/oauth2-proxy/oauth2-proxy).

Rather than executing its own OIDC (OpenID Connect) authentication flow, this plugin leverages OAuth2 Proxy to undertake the authentication process, relying on it to provide the necessary information through headers.

Furthermore, acknowledging the importance of non-interactive programmatic access within the Nexus environment, this plugin incorporates an API token feature. The plugin introduces an additional endpoint that allows authenticated users to reset their own API token to a system-generated one via the Nexus UI, with the caveat that this token is displayed solely once and is subject to reset with each access of this user menu item.

## ⚠️ Disclaimer

It is important to highlight that this plugin is provided on an 'as-is' basis, without any form of express or implied warranty. Under no circumstances shall the authors be held accountable for any damages or liabilities arising from the utilization of this plugin. Users are advised to proceed at their own risk.

## Features

* makes use of several headers sent by OAuth2 proxy (depending on its configuration)
  * see constants in [OAuth2ProxyHeaderAuthTokenFactory](src/main/java/com/github/tumbl3w33d/OAuth2ProxyHeaderAuthTokenFactory.java)
  * creates an AuthenticationToken used by Nexus
* creates a user in a dedicated database (i.e., not the 'local db' of Nexus) if none with the given id (`preferred_username`) exists
  * anyone authenticated with your identity provider can access Nexus
  * you would control access by granting necessary scopes accessing OAuth2 Proxy only to eligible user groups
  * user creation currently has a rather simplistic strategy to extract `<firstname>.<lastname>` from `preferred_username`
* group/scope to role sync
  * if you configure OAuth2 Proxy with the well-known `groups` claim, it will retrieve that information from the identity provider
  * the groups received in the related header will be stored in a database and become available for the 'external role mapping' functionality
* automatic expiry of API tokens
  * there is a configurable task that lets API tokens expire, so another login by the user is necessary to renew it
  * as long as the user keeps showing up regularly, their token will not expire

**Note**: After authenticating with this realm, the logout button is non-operative, which is a common limitation with header-based authentication methods. To force a logout, you need to logout from your identity provider and/or delete the OAuth2 Proxy cookie if you must logout for some reason.

## Necessary infrastructure

You typically put an OAuth2 Proxy in front of your application and make sure that related `X-Forwarded-` headers do not reach the application other than those originating from the OAuth2 Proxy.

For non-interactive programmatic access you circumvent the OAuth2 Proxy and go straight to the Nexus application. To achieve that, you could check for the presence of an `Authorization: Basic` header earlier in the chain of proxies. In that case the required credentials are the user's id and the generated API token.

## Example with HAProxy as entrypoint

```
# the entrypoint to your nexus + oauth2 proxy setup
frontend you-name-it
  # just illustrating that you must ensure TLS
  bind *:443 ssl crt /usr/local/etc/haproxy/cert alpn h2,http/1.1

  # if this is too invasive for your use case, be more specific
  http-request del-header ^X-Forwarded.*

  # circumvent oauth2 proxy for programmatic access
  acl is_basic_auth hdr_beg(Authorization) -i basic
  # clients often send a HEAD request without Authorization header first
  # and this must reach nexus directly, else the OAuth2 dance starts
  acl is_head method HEAD
  use_backend nexus if is_basic_auth OR is_head

  default_backend oauth2-proxy


backend oauth2-proxy
  # interactive OIDC login
  option httpchk GET /ping
  server oauth2-proxy oauth2-proxy:4180 check


backend nexus
  # non-interactive programmatic access
  server nexus nexus:8081 check
```

## Example OAuth2 Proxy config

```
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

# we don't need to wait for people to press the button, just redirect
skip_provider_button = true
```

**Note**: Depending on the amount of data the OAuth2 Proxy receives from your IDP (especially list of groups) you might want to look into [changing its session storage to redis/valkey](https://oauth2-proxy.github.io/oauth2-proxy/configuration/session_storage/#redis-storage). The proxy will warn you about data exceeding limits which results in multiple cookies being set for the proxy session.
