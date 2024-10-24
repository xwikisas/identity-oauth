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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.identityoauth.IdentityOAuthProvider;

import static com.xwiki.identityoauth.internal.IdentityOAuthConstants.CHANGE_ME_LOGIN_URL;

/**
 * Collection of functions used by {@link DefaultIdentityOAuthManager} at initializing, resetting and processing
 * resources.
 *
 * @version $Id$
 * @since 1.7.6
 */
@Component(roles = DefaultIdentityOAuthManagerInitiator.class)
@Singleton
public class DefaultIdentityOAuthManagerInitiator
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
    private Execution execution;

    @Inject
    private Logger log;

    /**
     * If the {@link Execution} context is initialized, it will set the flag for "bypassDomainSecurityCheck" to
     * {@code true}.
     */
    public void tryBypassDomainSecurityCheck()
    {
        if (execution.getContext() != null) {
            execution.getContext().setProperty("bypassDomainSecurityCheck", true);
        }
    }

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
}
