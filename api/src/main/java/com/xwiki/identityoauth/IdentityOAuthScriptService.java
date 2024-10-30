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
package com.xwiki.identityoauth;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xwiki.identityoauth.configuration.IdentityOAuthGeneralConfiguration;

/**
 * Script service containing the methods used by the view files contained in the ui module.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("idoauth")
@Singleton
public class IdentityOAuthScriptService implements ScriptService
{
    @Inject
    private IdentityOAuthManager manager;

    @Inject
    @Named("default")
    private IdentityOAuthGeneralConfiguration authGeneralConfiguration;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * Get the set default provider from the Identity OAuth general configuration.
     *
     * @return the default provider.
     */
    public String getDefaultProvider()
    {
        return authGeneralConfiguration.getDefaultProvider();
    }

    /**
     * Triggers a request to the identity provider and a redirect of the browser t the OAuth identity-provider's
     * auhtorization screens.
     *
     * @return if found a credential
     * @since 1.0
     */
    @Unstable
    public boolean processOAuthStart()
    {
        return manager.processOAuthStart();
    }

    /**
     * Invoked when the OAuth provider has returned the browser to the wiki. Having confirmed the identity that the
     * provider references, the user can be logged in.
     *
     * @return "failed login" if failed, "no user", or "ok" if successful
     * @since 1.0
     */
    @Unstable
    public String processOAuthReturn()
    {
        return manager.processOAuthReturn();
    }

    /**
     * Indicates if the session contains information about a remote identity.
     *
     * @param provider the name of the provider
     * @return true if a session-information is present (and thus expected to be active).
     */
    public boolean hasSessionIdentityInfo(String provider)
    {
        return manager.hasSessionIdentityInfo(provider);
    }

    /**
     * Used by the admin UI to access the internal methods.
     *
     * @param name the provider name as returned by {@link IdentityOAuthProvider#getProviderHint()} and contained in
     *     the OAuthProviderClass object.
     * @return the provider of the given name except if the action is not admin.
     */
    public IdentityOAuthProvider getProvider(String name)
    {
        if (!authorizationManager.hasAccess(Right.ADMIN)) {
            throw new IllegalStateException("This method is for the admin editing");
        }
        return manager.getProvider(name);
    }

    /**
     * Performs XWiki rendering on each of the providers' loginCode's property and provides them ordered.
     *
     * @return A list of strings rendered in the current context (incl language).
     */
    public List<String> renderLoginCodes()
    {
        return manager.renderLoginCodes();
    }

    /**
     * Inspects the request to detect if an OAuth return needs to be processed.
     *
     * @return true if the relevant information (cookie, parameter, ...) is found.
     */
    public boolean doesDetectReturn()
    {
        return manager.doesDetectReturn();
    }

    /**
     * Removes all information about the services of IdentityOAuth within the session of this user.
     */
    public void clearAllSessionInfos()
    {
        manager.clearAllSessionInfos();
    }
}
