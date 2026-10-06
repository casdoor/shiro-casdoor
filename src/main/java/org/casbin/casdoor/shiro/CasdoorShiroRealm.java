// Copyright 2022 The casbin Authors. All Rights Reserved.
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
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.casbin.casdoor.config.Config;
import org.casbin.casdoor.entity.Permission;
import org.casbin.casdoor.entity.Role;
import org.casbin.casdoor.entity.User;
import org.casbin.casdoor.exception.AuthException;
import org.casbin.casdoor.service.AuthService;

/**
 * A {@link org.apache.shiro.realm.Realm Realm} that signs in with a Casdoor access token: the token is a JWT,
 * verified with the certificate of the Casdoor application.
 * <p>
 * <b>NOTE:</b> this realm must be used with the Shiro Bearer Token Filter {@code authcBearer}.
 * <p>
 * The principal is the Casdoor {@link User}. The names of its Casdoor roles become Shiro roles, and its enabled
 * Casdoor permissions with the "Allow" effect become Shiro permissions {@code <resource>:<action>}, e.g.
 * {@code /api/foos:read} (Shiro compares permissions case-insensitively). The roles and permissions are only in
 * the access token when the token format of the Casdoor application is "JWT" (the default).
 *
 * @author Yixiang Zhao (@seriouszyx)
 **/
public class CasdoorShiroRealm extends AuthorizingRealm {

    private AuthService authService;

    private String endpoint;

    private String clientId;

    private String clientSecret;

    private String certificate;

    private String organizationName;

    private String applicationName;

    /**
     * Creates a realm that is configured with the setters, e.g. in shiro.ini.
     */
    public CasdoorShiroRealm() {
        setAuthenticationTokenClass(BearerToken.class);
    }

    /**
     * Creates a realm with the connection settings of a Casdoor application.
     *
     * @param endpoint         the URL of the Casdoor server, e.g. https://door.casdoor.com
     * @param clientId         the client ID of the application
     * @param clientSecret     the client secret of the application
     * @param certificate      the certificate (PEM) that verifies the access tokens of the application
     * @param organizationName the organization of the application
     * @param applicationName  the name of the application
     */
    public CasdoorShiroRealm(String endpoint, String clientId, String clientSecret, String certificate, String organizationName, String applicationName) {
        this();
        this.endpoint = endpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.certificate = certificate;
        this.organizationName = organizationName;
        this.applicationName = applicationName;
    }

    /**
     * Creates a realm with an existing {@link AuthService}, e.g. the bean of casdoor-spring-boot-starter.
     *
     * @param authService the service that verifies the access tokens
     */
    public CasdoorShiroRealm(AuthService authService) {
        this();
        this.authService = authService;
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (authService == null) {
            requireText(endpoint, "endpoint");
            requireText(clientId, "clientId");
            requireText(clientSecret, "clientSecret");
            requireText(certificate, "certificate");
            requireText(organizationName, "organizationName");
            authService = new AuthService(new Config(endpoint, clientId, clientSecret, certificate, organizationName, applicationName));
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("The " + name + " (or an AuthService) is required for the " + getClass());
        }
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        BearerToken token = (BearerToken) authenticationToken;
        try {
            User user = authService.parseJwtToken(token.getToken());
            return new SimpleAuthenticationInfo(user, token.getCredentials(), getName());
        } catch (AuthException e) {
            throw new AuthenticationException("Could not validate the Casdoor access token", e);
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        User user = principals.oneByType(User.class);
        if (user == null) {
            return info;
        }

        if (user.roles != null) {
            for (Role role : user.roles) {
                if (role != null && role.name != null) {
                    info.addRole(role.name);
                }
            }
        }

        if (user.permissions != null) {
            for (Permission permission : user.permissions) {
                if (permission == null || !permission.isEnabled || "Deny".equalsIgnoreCase(permission.effect)
                        || permission.resources == null || permission.actions == null) {
                    continue;
                }
                for (String resource : permission.resources) {
                    for (String action : permission.actions) {
                        info.addStringPermission(resource + ":" + action);
                    }
                }
            }
        }

        return info;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
