# shiro-casdoor

[![Build](https://github.com/casdoor/shiro-casdoor/actions/workflows/ci.yml/badge.svg)](https://github.com/casdoor/shiro-casdoor/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.casbin/shiro-casdoor.svg)](https://central.sonatype.com/artifact/org.casbin/shiro-casdoor)
[![License](https://img.shields.io/github/license/casdoor/shiro-casdoor)](https://github.com/casdoor/shiro-casdoor/blob/master/LICENSE)
[![Discord](https://img.shields.io/discord/1022748306096537660?logo=discord&label=discord&color=5865F2)](https://discord.gg/5rPsrAzK7S)

An [Apache Shiro](https://shiro.apache.org/) realm for [Casdoor](https://casdoor.ai/): `CasdoorShiroRealm` signs users in with a Casdoor access token (a JWT, verified with the certificate of the Casdoor application) and maps their Casdoor roles and permissions to Shiro.

| shiro-casdoor | Shiro | casdoor-java-sdk | Java |
|---------------|-------|------------------|------|
| 2.x           | 2.x   | 1.47+            | 17+  |
| 1.x           | 1.x   | 1.5              | 8+   |

## Installation

```xml
<dependency>
    <groupId>org.casbin</groupId>
    <artifactId>shiro-casdoor</artifactId>
    <version>2.0.0</version>
</dependency>
```

It depends on `shiro-core` only, so it works with both the `javax` and the `jakarta` flavor of Shiro's web modules.

## Usage

### With casdoor-spring-boot-starter

[casdoor-spring-boot-starter](https://github.com/casdoor/casdoor-spring-boot-starter) 2.x (Spring Boot 3 and 4) provides an `AuthService` bean, configured by the `casdoor.*` properties. Pass it to the realm:

```java
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.casbin.casdoor.service.AuthService;
import org.casbin.casdoor.shiro.CasdoorShiroRealm;

@Configuration
public class ShiroConfig {

    @Bean
    public CasdoorShiroRealm casdoorShiroRealm(AuthService authService) {
        return new CasdoorShiroRealm(authService);
    }

    // the realm is an Authorizer too, so Shiro's auto-configuration skips this bean but still looks it up by name
    @Bean
    public Authorizer authorizer() {
        return new ModularRealmAuthorizer();
    }
}
```

```yaml
casdoor:
  endpoint: https://door.casdoor.com
  client-id: <client ID of the application>
  client-secret: <client secret of the application>
  certificate: |
    -----BEGIN CERTIFICATE-----
    ...
    -----END CERTIFICATE-----
  organization-name: <organization of the application>
  application-name: <name of the application>
```

### Without Spring

Pass the connection settings of the Casdoor application to the realm:

```java
CasdoorShiroRealm realm = new CasdoorShiroRealm(endpoint, clientId, clientSecret, certificate, organizationName, applicationName);
```

or [configure](https://shiro.apache.org/realm.html#realm-configuration) it in `shiro.ini` with the properties `endpoint`, `clientId`, `clientSecret`, `certificate`, `organizationName` and `applicationName`.

### Signing in

The realm accepts Shiro's `BearerToken`:

- For an API, protect the paths with the `authcBearer` filter: it signs in with the `Authorization: Bearer <access-token>` header of each request.
- For a web app, exchange the `code` of the OAuth callback for an access token and sign in with it:

```java
String token = authService.getOAuthToken(code, state);
SecurityUtils.getSubject().login(new BearerToken(token));
```

The principal is the Casdoor user:

```java
import org.casbin.casdoor.entity.User;

User user = (User) SecurityUtils.getSubject().getPrincipal();
```

### Roles and permissions

- The names of the user's Casdoor roles are Shiro roles: `subject.hasRole("admin")`, `@RequiresRoles("admin")`.
- The user's enabled Casdoor permissions with the "Allow" effect are Shiro permissions `<resource>:<action>`: `subject.isPermitted("/api/foos:read")`, `@RequiresPermissions("/api/foos:read")`. Shiro compares them case-insensitively.

The roles and permissions come from the access token, which only has them when the **Token format** of the Casdoor application is `JWT` (the default).

## Example

- [casdoor-spring-boot-shiro-example](https://github.com/casdoor/casdoor-spring-boot-shiro-example): Shiro 2 on Spring Boot 3 with casdoor-spring-boot-starter

## See also

- [casdoor-java-sdk](https://github.com/casdoor/casdoor-java-sdk)
- [casdoor-spring-boot-starter](https://github.com/casdoor/casdoor-spring-boot-starter)

## License

[Apache-2.0](LICENSE)
