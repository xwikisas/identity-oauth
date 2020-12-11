/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.identityoauth.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Data object class meant to store short-term user authorization (tokens and authorization-code) for the OAuth provider
 * within the user-session.
 *
 * @since 1.0
 */
class IdentityOAuthSessionInfo implements Serializable
{
    private static final String serialVersionUID = "323424";

    private String providerAuthorizationRunning;

    private String userToLogIn;

    private Map<String, String> authorizationCodes = new HashMap<>();

    private Map<String, String> tokens = new HashMap<>();

    private Map<String, Date> expiries = new HashMap<>();

    private String xredirect;

    static IdentityOAuthSessionInfo getFromSession(HttpServletRequest request)
    {
        String sessKey = IdentityOAuthSessionInfo.class.getName();
        HttpSession session = request.getSession();
        IdentityOAuthSessionInfo si = (IdentityOAuthSessionInfo) session.getAttribute(sessKey);
        if (si == null) {
            si = new IdentityOAuthSessionInfo();
            session.setAttribute(sessKey, si);
        }
        return si;
    }

    public String getProviderAuthorizationRunning()
    {
        return providerAuthorizationRunning;
    }

    public void setProviderAuthorizationRunning(String providerAuthorizationRunning)
    {
        this.providerAuthorizationRunning = providerAuthorizationRunning;
    }

    public static boolean hasSessionIdentityInfo(String provider, HttpServletRequest request)
    {
        return getFromSession(request).tokens.containsKey(provider);
    }

    void clear(String provider)
    {
        Set<String> providers;
        if (provider != null) {
            providers = Collections.singleton(provider);
        } else {
            providers = authorizationCodes.keySet();
        }

        for (String prov : providers) {
            tokens.remove(prov);
            authorizationCodes.remove(prov);
            xredirect = null;
        }
    }

    String getUserToLogIn()
    {
        return userToLogIn;
    }

    void setUserToLogIn(String userToLogIn)
    {
        this.userToLogIn = userToLogIn;
    }

    String getAuthorizationCode(String provider)
    {
        return authorizationCodes.get(provider);
    }

    void setAuthorizationCode(String provider, String authorizationCode)
    {
        this.authorizationCodes.put(provider, authorizationCode);
    }

    String getToken(String provider)
    {
        return tokens.get(provider);
    }

    void setToken(String provider, String token)
    {
        this.tokens.put(provider, token);
    }

    Date getTokenExpiry(String provider)
    {
        return expiries.get(provider);
    }

    void setTokenExpiry(String provider, Date expDate)
    {
        expiries.put(provider, expDate);
    }

    String pickXredirect()
    {
        String x = xredirect;
        xredirect = null;
        return x;
    }

    void setXredirect(String xredirect)
    {
        this.xredirect = xredirect;
    }
}
