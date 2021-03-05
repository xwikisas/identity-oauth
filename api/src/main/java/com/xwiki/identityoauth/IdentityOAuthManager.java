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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * The specification of the methods that the manager of the IdentityOAuth application is doing. Methods of this
 * interface are mostly called by the script-service (itself called by the views).
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface IdentityOAuthManager
{

    /**
     * Called by the request for login page to start an OAuth authorization dialog.
     *
     * @return if found a credential
     * @throws IdentityOAuthException if a communication problem with the service occured
     * @since 1.0
     */
    @Unstable
    boolean processOAuthStart() throws IdentityOAuthException;

    /**
     * Performs the necessary communication with OAuth provider to fetch identity and update the XWiki-user object.
     *
     * @return "failed login" if failed, "no user", or "ok" if successful
     * @since 1.0
     */
    @Unstable
    String processOAuthReturn();

    /**
     * Invoked when the configuration and the dependent objects needs to be fetched again and recreated.
     */
    void reloadConfig();

    /**
     * Indicates if the session contains information about a remote identity.
     *
     * @param provider the name of the provider
     * @return true if a session-information is present (and thus expected to be active).
     */
    boolean hasSessionIdentityInfo(String provider);

    /**
     * @param name the name of the provider as return by {@link IdentityOAuthProvider#getProviderHint()} and contained
     *             in the OAuthProviderClass.
     * @return the provider object.
     */
    IdentityOAuthProvider getProvider(String name);

    /**
     * Collects the rendered login-codes for each of the providers.
     *
     * @return a list of the renderings (strings) of each of the providers
     * are shown to users.
     */
    List<String> renderLoginCodes();


    /**
     * Inspects the request to detect if an OAuth return needs to be processed.
     *
     * @return true if the relevant information (cookie, parameter, ...) is found.
     */
    boolean doesDetectReturn();


    /**
     * Launches a request for the user-specific token.
     *
     * @param providerHint The name of the provider for which the token should be searched for.
     */
    void requestCurrentToken(String providerHint);

    /**
     * Removes all information about the services of IdentityOAuth within the session of this user.
     */
    void clearAllSessionInfos();

}
