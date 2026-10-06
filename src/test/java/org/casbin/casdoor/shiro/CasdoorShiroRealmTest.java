// Copyright 2026 The casbin Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.casbin.casdoor.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.casbin.casdoor.config.Config;
import org.casbin.casdoor.entity.Permission;
import org.casbin.casdoor.entity.Role;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.exception.AuthException;
import org.casbin.casdoor.service.AuthService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CasdoorShiroRealmTest {

    private static final String TOKEN = "valid-token";

    private static User newUser() {
        User user = new User();
        user.owner = "built-in";
        user.name = "alice";

        Role auditor = new Role("built-in", "auditor", "", "Auditor", "");
        user.roles = Arrays.asList(new Role("built-in", "admin", "", "Admin", ""), auditor);

        Permission allow = new Permission("built-in", "foos", "", "Foos", "",
                new String[]{"built-in/alice"}, new String[0], new String[0], "", "Application",
                new String[]{"/api/foos"}, new String[]{"Read", "Write"}, "Allow", true);
        Permission deny = new Permission("built-in", "bars", "", "Bars", "",
                new String[]{"built-in/alice"}, new String[0], new String[0], "", "Application",
                new String[]{"/api/bars"}, new String[]{"Read"}, "Deny", true);
        Permission disabled = new Permission("built-in", "bazs", "", "Bazs", "",
                new String[]{"built-in/alice"}, new String[0], new String[0], "", "Application",
                new String[]{"/api/bazs"}, new String[]{"Read"}, "Allow", false);
        user.permissions = Arrays.asList(allow, deny, disabled);
        return user;
    }

    private static CasdoorShiroRealm newRealm(User user) {
        AuthService authService = new AuthService(new Config("https://door.casdoor.com", "client-id", "client-secret", "", "built-in", "app-built-in")) {
            @Override
            public User parseJwtToken(String token) {
                if (!TOKEN.equals(token)) {
                    throw new AuthException("Cannot verify signature.");
                }
                return user;
            }
        };
        CasdoorShiroRealm realm = new CasdoorShiroRealm(authService);
        realm.init();
        return realm;
    }

    @Test
    void authenticatesTheUserOfTheToken() {
        User user = newUser();
        CasdoorShiroRealm realm = newRealm(user);

        assertTrue(realm.supports(new BearerToken(TOKEN)));
        AuthenticationInfo info = realm.getAuthenticationInfo(new BearerToken(TOKEN));
        assertSame(user, info.getPrincipals().getPrimaryPrincipal());
        assertEquals(realm.getName(), info.getPrincipals().getRealmNames().iterator().next());
    }

    @Test
    void rejectsAnInvalidToken() {
        CasdoorShiroRealm realm = newRealm(newUser());

        assertThrows(AuthenticationException.class, () -> realm.getAuthenticationInfo(new BearerToken("invalid-token")));
    }

    @Test
    void mapsCasdoorRolesAndPermissions() {
        CasdoorShiroRealm realm = newRealm(newUser());
        PrincipalCollection principals = realm.getAuthenticationInfo(new BearerToken(TOKEN)).getPrincipals();

        assertTrue(realm.hasRole(principals, "admin"));
        assertTrue(realm.hasRole(principals, "auditor"));
        assertFalse(realm.hasRole(principals, "user"));

        assertTrue(realm.isPermitted(principals, "/api/foos:read"));
        assertTrue(realm.isPermitted(principals, "/api/foos:Write"));
        assertFalse(realm.isPermitted(principals, "/api/foos:admin"));
        assertFalse(realm.isPermitted(principals, "/api/bars:read"));
        assertFalse(realm.isPermitted(principals, "/api/bazs:read"));
    }

    @Test
    void handlesAUserWithoutRolesAndPermissions() {
        User user = newUser();
        user.roles = null;
        user.permissions = null;
        CasdoorShiroRealm realm = newRealm(user);
        PrincipalCollection principals = realm.getAuthenticationInfo(new BearerToken(TOKEN)).getPrincipals();

        assertFalse(realm.hasRole(principals, "admin"));
        assertFalse(realm.isPermitted(principals, "/api/foos:read"));
    }

    @Test
    void createsTheAuthServiceFromTheSettings() {
        CasdoorShiroRealm realm = new CasdoorShiroRealm("https://door.casdoor.com", "client-id", "client-secret",
                "-----BEGIN CERTIFICATE-----", "casbin", "app-example");
        realm.init();

        // the real AuthService rejects a token that is not a JWT
        assertThrows(AuthenticationException.class, () -> realm.getAuthenticationInfo(new BearerToken("not-a-jwt")));
    }

    @Test
    void requiresTheConnectionSettings() {
        CasdoorShiroRealm realm = new CasdoorShiroRealm();
        realm.setEndpoint("https://door.casdoor.com");

        assertThrows(IllegalStateException.class, realm::init);
    }
}
