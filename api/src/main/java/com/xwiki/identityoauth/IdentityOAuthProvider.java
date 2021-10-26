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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The specification of the methods that an implementation identity provider should offer.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface IdentityOAuthProvider
{
    /**
     * Configures the object according to the data.
     *
     * @param config A map to hold each configuration collected from the configuration page's object-properties. Among
     *               others, the following properties are expected:
     *               <p>
     *               clientId: A string representing the API client (obtained from the remote server) secret: A string
     *               representing the API client's authorization (obtained from the remote server) scopes: A list of
     *               strings representing the authorizations asked for. If <code>null</code> then the scope is limited
     *               to fetching a (verified) identity as returned by {@link IdentityOAuthProvider#getMinimumScopes()}.
     *               redirectUrl: external URL of the Login page as configured at the service or inferred by the
     *               platform (this value must be fixed so as to insure a secure process).
     */
    void initialize(Map<String, String> config);

    /**
     * @return true if the configuration (and possibly other conditions) make it possible for this provider to be active
     * and thus presented to the user. This flag is read at initialization.
     */
    boolean isActive();

    /**
     * @return true if the provider can be presented among the login options.
     *           This flag is read at rendering of login pages.
     */
    boolean isReady();

    /**
     * @return the short-name of this provider to match UI values
     */
    String getProviderHint();

    /**
     * Defines the name extracted from the configuration object.
     *
     * @param hint The name used various url-parameters.
     */
    void setProviderHint(String hint);

    /**
     * Verifies the assigned configuration in an automatic fashion. For use by the administration UI.
     *
     * @return "ok" for an apparently correct configuration or a key for translation or user-side text indicating a
     * possible error that the administrator could adjust (either inside XWiki or outside).
     */
    String validateConfiguration();

    /**
     * For use by the administration UI.
     *
     * @return The minimum set of scopes of this provider so as to be able to provide any authorization form and, thus,
     * involves identifying the user.
     */
    List<String> getMinimumScopes();

    /**
     * Invoke the OAuth URL so as to fetch the URL where the user should be redirected to so that he or she can be
     * verified and possibly pronounce an authorization.
     *
     * @param redirectUrl The internal URL of this wiki, to which the user should be redirected after the verification
     *                    is finished.
     * @return the URL to send the browser to
     */
    String getRemoteAuthorizationUrl(String redirectUrl);

    /**
     * Return a (short-lived) token that allows to operate on behalf of the user.
     *
     * @param authCode the long-term authorization code
     * @return the token using which a resource operation can be performed
     */
    Pair<String, Date> createToken(String authCode);

    /**
     * Analyzes the parameters of the redirected URL of the browser coming from the authorization dialog and fetches the
     * authorization code that can be used to create tokens.
     *
     * @param params the request's parameters
     * @return the string representing the authorization as obtained by analyzing the URL
     */
    String readAuthorizationFromReturn(Map<String, String[]> params);

    /**
     * Fetches the identity resource and decodes it.
     *
     * @param token The token obtained through the authorization to act on behalf of the user.
     * @return A decoding of the identity resource
     */
    AbstractIdentityDescription fetchIdentityDetails(String token);

    /**
     * Opens the stream of the user image file if it was modified later than the given date.
     *
     * @param ifModifiedSince Only fetch the file if it is modified after this date.
     * @param id              the currently collected identity-description.
     * @param token           the currently valid token.
     * @return A triple made of inputstream, media-type, and possibly guessed filename.
     */
    Triple<InputStream, String, String> fetchUserImage(Date ifModifiedSince, AbstractIdentityDescription id,
            String token);

    /**
     * Allows to add provider-specific XWikiObjects to the user-object.
     *
     * @param idDescription The object returned by {@link #fetchIdentityDetails(String)}.
     * @param doc           The user-document where objects can be manipulated. This document will be saved thereafter.
     * @return true if the object was changed
     */
    boolean enrichUserObject(AbstractIdentityDescription idDescription, XWikiDocument doc);

    /**
     * Receives the token in the session after a call to {@link IdentityOAuthManager#requestCurrentToken(String)}.
     *
     * @param token the token stored in the session
     */
    void receiveFreshToken(String token);

    /**
     * Gets the reference configuration.
     *
     * @param page The reference string to the XWiki page containin the configuration object(s).
     */
    void setConfigPage(String page);

    /**
     * An object view on the information fetched from the OAuth service.
     */
    abstract class AbstractIdentityDescription
    {
        /**
         * The first part of the name.
         */
        public String firstName;

        /**
         * The last part of the name.
         */
        public String lastName;

        /**
         * The service-internal-ID used to find back the user in the XWiki's user-storage.
         */
        public String internalId;

        /**
         * The list of emails.
         */
        public List<String> emails = new ArrayList<>();

        /**
         * A URL where to fetch the user-image.
         */
        public String userImageUrl;

        /**
         * Left for implementing subclasses to specify.
         *
         * @return the issuer-URL (which may be configuration dependent) or null (then the provider name will be used).
         */
        public abstract String getIssuerURL();
    }
}
