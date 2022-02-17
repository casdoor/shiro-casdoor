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

import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.Assert;
import org.casbin.casdoor.config.CasdoorConfig;
import org.casbin.casdoor.entity.CasdoorUser;
import org.casbin.casdoor.exception.CasdoorAuthException;
import org.casbin.casdoor.service.CasdoorAuthService;

/**
 * A {@link org.apache.shiro.realm.Realm Realm} implementation that parses and validate Casdoor JWT Access Tokens.
 * <p>
 * <b>NOTE:</b> This realm MUST be use with the Shiro Bearer Token Filter {@code authcBearer}
 * <p>
 *
 * @author Yixiang Zhao (@seriouszyx)
 * @date 2022-02-16 18:51
 **/
public class CasdoorShiroRealm extends AuthorizingRealm {

    private CasdoorAuthService casdoorAuthService;

    private String endpoint;

    private String clientId;

    private String clientSecret;

    private String jwtPublicKey;

    private String organizationName;

    private String applicationName;

    public CasdoorShiroRealm() {
        setAuthenticationTokenClass(BearerToken.class);
    }

    public CasdoorShiroRealm(String endpoint, String clientId, String clientSecret, String jwtPublicKey, String organizationName, String applicationName) {
        this.endpoint = endpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.jwtPublicKey = jwtPublicKey;
        this.organizationName = organizationName;
        this.applicationName = applicationName;

        setAuthenticationTokenClass(BearerToken.class);
    }

    @Override
    protected void onInit() {
        super.onInit();

        Assert.hasText(endpoint, "An endpoint is required for the " + getClass());
        Assert.hasText(clientId, "A clientId is required for the " + getClass());
        Assert.hasText(clientSecret, "A clientSecret is required for the " + getClass());
        Assert.hasText(jwtPublicKey, "A jwtPublicKey is required for the " + getClass());
        Assert.hasText(organizationName, "A organizationName is required for the " + getClass());

        casdoorAuthService = new CasdoorAuthService(
                new CasdoorConfig(
                        endpoint,
                        clientId,
                        clientSecret,
                        jwtPublicKey,
                        organizationName,
                        applicationName));
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        BearerToken token = (BearerToken) authenticationToken;
        try {
            CasdoorUser user = casdoorAuthService.parseJwtToken(token.getToken());
            return new SimpleAuthenticationInfo(user, token.getCredentials(), getName());
        } catch (CasdoorAuthException e) {
            throw new AuthenticationException("Could not validate bearer token", e);
        }
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
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

    public String getJwtPublicKey() {
        return jwtPublicKey;
    }

    public void setJwtPublicKey(String jwtPublicKey) {
        this.jwtPublicKey = jwtPublicKey;
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
