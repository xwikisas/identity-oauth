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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthProvider;

import static com.xwiki.identityoauth.internal.IdentityOAuthConstants.CHANGE_ME_LOGIN_URL;

/**
 * Collection of functions used by {@link DefaultIdentityOAuthManager}.
 *
 * @version $Id$
 * @since 1.7.6
 */
@Component(roles = DefaultIdentityOAuthManagerUtils.class)
@Singleton
public class DefaultIdentityOAuthManagerUtils
{
    @Inject
    private IdentityOAuthConfigTools ioConfigObjects;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    /* The authService is a subclass of XWikiAuthServiceImpl which may be impossible to construct
     *  at the initialization of this component;
     *  Thus a provider is injected which lets the authService be built at the application startup event. */
    @Inject
    private Provider<IdentityOAuthAuthService> authServiceProvider;

    @Inject
    private IdentityOAuthUserTools ioUserProc;

    @Inject
    private Logger log;

    /**
     * Clears and rebuilds the entire provider map.
     *
     * @param providers the provider map to be rebuilt.
     * @return the list of the {@link ProviderConfig}.
     */
    List<ProviderConfig> rebuildProviders(Map<String, IdentityOAuthProvider> providers)
    {
        providers.clear();
        List<ProviderConfig> providerConfigs = ioConfigObjects.loadAndRebuildProviders();
        for (ProviderConfig config : providerConfigs) {
            providers.put(config.getName(), config.getProvider());
        }
        return providerConfigs;
    }

    /**
     * Called to express links that invite the user to start an OAuth flow e.g. to add an extra feature. The user should
     * not be invited to choose the provider but may need to give her authorization.
     *
     * @param provider the provider that we expect will process the return of this URL if followed.
     * @return the URL to redirect to (which may include a redirect to the current URL).
     * @since 1.1
     */
    String processOAuthStartUrl(IdentityOAuthProvider provider)
    {
        String loginPageUrl = ioConfigObjects.getLoginPageUrl();
        if (!loginPageUrl.contains("?")) {
            loginPageUrl += '?';
        }
        loginPageUrl += "provider=" + provider.getProviderHint() + "&identityOAuth=start";
        return loginPageUrl;
    }

    /**
     * Check the given redirect URL and modify it if needed.
     *
     * @param redirectURL the given URL.
     * @return a modified URL if the given {@link String} is incomplete, or the original link otherwise.
     * @throws UnsupportedEncodingException if the named encoding is not supported.
     */
    String maybeModifyRedirectURL(String redirectURL) throws UnsupportedEncodingException
    {
        String modifiedURL = redirectURL;
        if (modifiedURL.contains(CHANGE_ME_LOGIN_URL)) {
            String loginPageUrl = ioConfigObjects.getLoginPageUrl();
            modifiedURL = redirectURL.replace(CHANGE_ME_LOGIN_URL, URLEncoder.encode(loginPageUrl, "UTF-8"));
        }
        return modifiedURL;
    }

    /**
     * Check if the {@link XWikiContext} has been initialized and return the {@link XWiki}.
     *
     * @return the current {@link XWiki} if the {@link XWikiContext} has been initialized, or null otherwise.
     */
    XWiki getXWiki()
    {
        XWiki result = null;
        XWikiContext xc = this.xwikiContextProvider.get();
        // XWikiContext could be null at startup when the Context Provider has not been initialized yet (it's
        // initialized after the first request).
        if (xc != null) {
            result = xc.getWiki();
        }
        return result;
    }

    /**
     * Initiate the authentication service.
     */
    void tryInitiatingAuthService()
    {
        XWiki xwiki = getXWiki();
        if (xwiki != null) {
            log.debug("Initting authService.");
            // We do not verify with the context if the plugin is active and if the license is active
            // this will be done by the IdentityOAuthAuthService and UI pages later on, when it is called
            // within a request
            try {
                xwiki.setAuthService(authServiceProvider.get());
                log.debug("Succeeded initting authService,");
            } catch (Exception e) {
                log.warn("Failed initting authService", e);
            }
        }
    }

    /**
     * Performs the necessary communication with the OAuth provider to fetch identity and update the XWiki-user.
     *
     * @param provider current provider.
     * @param providerHint provider hint.
     * @param sessionInfo the session info.
     * @return "ok"
     * @throws IOException if an input or output exception occur.
     */
    String processOAuthReturn(IdentityOAuthSessionInfo sessionInfo, IdentityOAuthProvider provider,
        String providerHint) throws IOException
    {
        checkIfProviderIsActive(provider);
        log.debug("Return OAuth.");
        // collect provider-name (an OAuth return is single-use)
        sessionInfo.setProviderAuthorizationRunning(null);
        String authorization =
            provider.readAuthorizationFromReturn(xwikiContextProvider.get().getRequest().getParameterMap());
        Pair<String, Date> token = provider.createToken(authorization);

        // store auth and token
        sessionInfo.setAuthorizationCode(providerHint, authorization);
        sessionInfo.setToken(providerHint, token.getLeft());
        sessionInfo.setTokenExpiry(providerHint, token.getRight());

        IdentityOAuthProvider.AbstractIdentityDescription id = provider.fetchIdentityDetails(token.getLeft());
        String xwikiUser = ioUserProc.updateXWikiUser(id, provider, token.getLeft());

        // login at next call to the authenticator (the next http request)
        sessionInfo.setUserToLogIn(xwikiUser);
        log.debug("User will be logged-in.");

        // process redirect
        // getSessionInfo().xredirect is guaranteed to be not null in processOAuthStart
        // we expect the final redirect to be an "allowed redirect" (probably the same URL as the current page)
        xwikiContextProvider.get().getResponse().sendRedirect(sessionInfo.pickXredirect());
        log.debug("Redirecting user to originally intended URL.");

        return "ok";
    }

    /**
     * Check if the provider is active.
     *
     * @param provider the checked provider.
     */
    void checkIfProviderIsActive(IdentityOAuthProvider provider)
    {
        if (provider == null) {
            throw new IdentityOAuthException("Provider \"" + provider + "\" not found.");
        }
        if (!provider.isActive()) {
            throw new IdentityOAuthException("The provider \"" + provider + "\" is inactive.");
        }
        if (!provider.isReady()) {
            throw new IdentityOAuthException(
                "The provider  \"" + provider + "\" is not ready (probably a missing license).");
        }
    }
}
