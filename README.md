# shiro-casdoor

[![build](https://github.com/casdoor/shiro-casdoor/actions/workflows/maven-ci.yml/badge.svg)](https://github.com/casdoor/shiro-casdoor/actions/workflows/maven-ci.yml)
[![License](https://img.shields.io/github/license/casdoor/casdoor-spring-boot-starter.svg?style=flat-square&color=blue)](http://www.apache.org/licenses/LICENSE-2.0.txt)
[![Discord](https://img.shields.io/discord/1022748306096537660?logo=discord&label=discord&color=5865F2)](https://discord.gg/5rPsrAzK7S)

This plugin contains a Shiro Realm for Casdoor which will validate Casdoor JWT access tokens.

This plugin provides a Shiro Realm that will authenticate requests with an Authorization: `Bearer <access-token>` header.

To use the realm, first [define and configure](https://shiro.apache.org/realm.html#realm-configuration) the `CasdoorShiroRealm`.

The JWT claim information can be retrieved from the current Shiro Subject by casting the principal to `CasdoorUser`:

```java
import org.casbin.casdoor.entity.CasdoorUser;
...
        
CasdoorUser casdoorUser = (CasdoorUser) SecurityUtils.getSubject().getPrincipal();
```

See the examples to help you get started even faster:

- [casdoor-spring-boot-shiro-example](https://github.com/casdoor/casdoor-spring-boot-shiro-example)

We also publish these libraries for Java:

- [casdoor-java-sdk](https://github.com/casdoor/casdoor-java-sdk)
- [casdoor-spring-boot-starter](https://github.com/casdoor/casdoor-spring-boot-starter)