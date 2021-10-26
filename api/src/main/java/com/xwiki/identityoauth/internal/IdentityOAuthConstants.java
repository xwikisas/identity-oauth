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

/**
 * A set of string constants used across the classes of the app.
 *
 * @version $Id$
 * @since 1.0
 */
public interface IdentityOAuthConstants
{
    /**
     * avatar.
     */
    String AVATAR = "avatar";
    /**
     * user.
     */
    String USER = "user";
    /**
     * IdentityOAuth.
     */
    String SPACENAME = "IdentityOAuth";
    /**
     * view.
     */
    String VIEWACTION = "view";
    /**
     * xwiki.
     */
    String WIKINAME = "xwiki";
    /**
     * active.
     */
    String ACTIVE = "active";
    /**
     * id.
     */
    String ID = "id";
    /**
     * The name of the XWiki space.
     */
    String XWIKISPACE = "XWiki";
    /**
     * The name of the XWiki login page.
     */
    String XWIKILOGIN = "XWikiLogin";
    /**
     * The name of the guest user in XWiki.
     */
    String XWIKIGUEST = "XWikiGuest";
    /**
     * auto.
     */
    String AUTOAPPROVAL = "auto";
    /**
     * email.
     */
    String EMAIL = "email";
    /**
     * password.
     */
    String PASSWORD = "password";
    /**
     * first_name.
     */
    String FIRSTNAME = "first_name";
    /**
     * last_name.
     */
    String LASTNAME = "last_name";
    /**
     * OAuth.
     */
    String OAUTH = "OAuth";
    /**
     * "failed login": a constant sent to the UI to indicate a failed login.
     */
    String FAILEDLOGIN = "failed login";
    /**
     * "no user": a constant sent to the UI to indicate that the provider could not give us a user, e.g. because the
     * user rejected the authorization request.
     */
    String NOUSER = "no user";
    /**
     * The provider parameter name.
     */
    String PROVIDER = "provider";
    /**
     * externalProviderId.
     */
    String EXTERNAL_PROVIDER_ID = "externalProviderId";
    /**
     * OAUTH_RETURN.
     */
    String OAUTH_RETURN = "OAUTH_RETURN";
    /**
     * --image-base64--.
     */
    String BASE64_MARKER = "--image-base64--";

    /**
     * _CHANGE_ME_LOGIN_URL_.
     */
    String CHANGE_ME_LOGIN_URL = "_CHANGE_ME_LOGIN_URL_";
    /**
     * providerHint.
     */
    String PROVIDER_HINT = "providerHint";
}
