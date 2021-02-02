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

import java.net.URLEncoder;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * An authenticator that can include a negotiation with remote Clouds services. This authenticator is created,
 * configured and maintained by the IdentityOAuthScriptService.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = IdentityOAuthAuthService.class)
@Singleton
public class IdentityOAuthAuthService extends XWikiAuthServiceImpl implements Initializable
{
    private static final String XWIKISPACE = "XWiki.";

    @Inject
    private Logger log;

    @Inject
    private IdentityOAuthXWikiObjects gaXwikiObjects;

    @Inject
    private CookieAuthenticationPersistence cookiePersistance;

    @Inject
    @Named("xwikicfg")
    private Provider<ConfigurationSource> xwikiCfg;

    private Pattern logoutRequestMatcher;

    /**
     * Reads the configuration.
     */
    public void initialize()
    {
        this.logoutRequestMatcher = Pattern.compile(
                xwikiCfg.get().getProperty("xwiki.authentication.logoutpage", ""));
    }

    /**
     * Evaluates if the user can be authenticated based on request info such as cookies.
     *
     * @param context the context representign the request
     * @return a valid user, if found.
     * @throws XWikiException if anything went wrong
     */
    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        try {
            log.debug("checkAuth");
            IdentityOAuthSessionInfo sessionInfo = IdentityOAuthSessionInfo.getFromSession(context.getRequest());
            if (isLogoutRequest(context)) {
                log.info("caught a logout request");
                cookiePersistance.clear();
                sessionInfo.clear(null);
                log.info("cleared cookie");
                return null;
            }
            return super.checkAuth(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new XWikiException(e.getMessage(), e);
        }
    }

    /**
     * Processes a password entry and creates the appropriate principal.
     *
     * @param username the provided user-name
     * @param password the provided password (ignored)
     * @param context  the context describing the request
     * @return a null Principal Object if the user hasn't been authenticated or a valid Principal Object if the user is
     * correctly authenticated
     * @throws XWikiException if something goes wrong.
     */
    public Principal authenticate(String username, String password, XWikiContext context) throws XWikiException
    {
        IdentityOAuthSessionInfo sessionInfo = IdentityOAuthSessionInfo.getFromSession(context.getRequest());
        try {
            log.debug("authenticate");
            // TODO: use cookies if opted-in for
            String userToLogin = sessionInfo.getUserToLogIn();
            if (userToLogin != null) {
                log.debug("User to login found.");
                sessionInfo.setUserToLogIn(null);
                if (!userToLogin.startsWith(XWIKISPACE)) {
                    userToLogin = XWIKISPACE + userToLogin;
                }
                log.debug("Authenticating user " + userToLogin);
                return new SimplePrincipal(userToLogin);
            } else {
                log.debug("attempt default authenticate method for user : " + username);
                return super.authenticate(username, password, context);
            }
        } catch (Exception ex) {
            XWikiException e =  new XWikiException("Trouble at authenticating", ex);
            log.warn("Trouble at authenticating.", e);
            throw e;
        }
    }

    /**
     * Redirect user to the login.
     *
     * @param context the xwiki-context of the request
     * @throws XWikiException a wrapped exception
     */
    public void showLogin(XWikiContext context) throws XWikiException
    {
        log.debug("IdentityOAuth authentificator - showLogin");
        boolean redirected = false;
        try {
            // TODO: revise this to create a user-to-login from the cookie infos
            //  ... or the one below
            String url = context.getWiki().getExternalURL("IdentityOAuth.Login", "view", context);
            // gaXwikiObjects.doesUseCookies() && gaXwikiObjects.doesSkipLoginPage()
            //if (true) {
            log.info("skip the login page ");
            XWikiRequest request = context.getRequest();
            String userCookie = cookiePersistance.getUserId();
            log.info("retrieved user from cookie : " + userCookie);
            String savedRequestId = request.getParameter(
                    SavedRequestManager.getSavedRequestIdentifier());
            if (StringUtils.isEmpty(savedRequestId)) {
                // Save this request
                savedRequestId = SavedRequestManager.saveRequest(request);
            }
            String sridParameter = SavedRequestManager.getSavedRequestIdentifier() + "=" + savedRequestId;

            StringBuilder redirectBack = new StringBuilder(request.getRequestURI());
            redirectBack.append('?');
            String delimiter = "";
            if (StringUtils.isNotEmpty(request.getQueryString())) {
                redirectBack.append(request.getQueryString());
                delimiter = "&";
            }
            if (!request.getParameterMap().containsKey(SavedRequestManager.getSavedRequestIdentifier())) {
                redirectBack.append(delimiter);
                redirectBack.append(sridParameter);
            }

            String finalURL = url + "?" + sridParameter + "&xredirect="
                    + URLEncoder.encode(redirectBack.toString(), "UTF-8");
            log.info("Redirecting to " + finalURL);
            redirected = true;
            context.getResponse().sendRedirect(finalURL);
            //}
        } catch (Exception e) {
            log.error("Exception in showLogin : " + e);
        } finally {
            if (!redirected) {
                super.showLogin(context);
            }
            log.info("IdentityOAuth authentificator - showLogin end");
        }
    }

    /**
     * @return true if the current request match the configured logout page pattern.
     */
    private boolean isLogoutRequest(XWikiContext context)
    {
        return logoutRequestMatcher.matcher(context.getRequest().getPathInfo()).matches();
    }
}
