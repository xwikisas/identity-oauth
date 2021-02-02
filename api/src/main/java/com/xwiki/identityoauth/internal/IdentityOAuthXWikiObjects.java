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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.httpclient.util.DateUtil;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthProvider;

/**
 * The objects representing the configuration of the application as well as methods to connect to XWiki for the
 * manipulation of users and attachments.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = IdentityOAuthXWikiObjects.class)
@Singleton
public class IdentityOAuthXWikiObjects implements IdentityOAuthConstants
{
    // environment
    @Inject
    private Logger log;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> userResolver;

    @Inject
    private DocumentReferenceResolver<String> documentResolver;

    // internal objects
    private EntityReference providerConfigRef;

    private Set<DocumentReference> configDocReferences = new HashSet<>();

    private DocumentReference authClassRef;

    EntityReference getProviderConfigRef()
    {
        if (providerConfigRef == null) {
            DocumentReference docRef = new DocumentReference(WIKINAME,
                    SPACENAME, "OAuthProviderClass");
            providerConfigRef = new ObjectReference("IdentityOAuth.OAuthProviderClass", docRef);
        }
        return providerConfigRef;
    }

    boolean hasIOConfigObject(XWikiDocument doc)
    {
        if (configDocReferences.contains(doc.getDocumentReference())) {
            return true;
        }
        BaseObject obj2 = doc.getXObject(getProviderConfigRef(), false, contextProvider.get());
        log.debug("Obtained object " + obj2 + " for " + " " + getProviderConfigRef());
        return obj2 != null;
    }

    String updateXWikiUser(IdentityOAuthProvider.IdentityDescription id, IdentityOAuthProvider provider)
    {
        XWikiContext context = contextProvider.get();
        String xwikiUser = null;
        String currentWiki = context.getWikiId();
        try {
            // Force main wiki database to create the user as global
            context.setMainXWiki(WIKINAME);
            String email = id.emails.get(0);
            List<Object> wikiUserList = findExistingUser(id.internalId, email);

            if (wikiUserList == null || wikiUserList.size() == 0) {
                xwikiUser = createUser(id.internalId, email, id.firstName, id.lastName, id.fetchedUserImage, context);
            } else {
                // user found.. we should update it if needed
                xwikiUser = (String) (wikiUserList.get(0));
                if (xwikiUser.startsWith(XWIKISPACE + '.')) {
                    xwikiUser = xwikiUser.substring(XWIKISPACE.length() + 1);
                }
                updateUser(xwikiUser, id, provider, context);
            }
        } finally {
            // Restore database
            context.setMainXWiki(currentWiki);
        }
        return xwikiUser;
    }

    private List<Object> findExistingUser(String remoteUserId, String email)
    {
        try {
            List<Object> wikiUserList = queryManager.createQuery(
                    "from doc.object(IdentityOAuth.IdentityOAuthIdentityClass) as auth "
                            + "where auth.externalProviderId=:id",
                    Query.XWQL).bindValue(ID, remoteUserId).execute();
            if ((wikiUserList == null) || (wikiUserList.size() == 0)) {
                wikiUserList = queryManager.createQuery(
                        "from doc.object(XWiki.XWikiUsers) as user where user.email=:email",
                        Query.XWQL)
                        .bindValue(EMAIL, email).execute();
            }
            return wikiUserList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IdentityOAuthException(e);
        }
    }

    private String createUser(String remoteUserId,
            String email, String firstName, String lastName, URL avatarUrl, XWikiContext context)
    {
        try {
            // user not found.. need to create new user
            String xwikiUser = email.substring(0, email.indexOf("@"));
            // make sure user is unique
            xwikiUser = context.getWiki().getUniquePageName(XWIKISPACE, xwikiUser, context);
            // create user
            DocumentReference userDirRef = new DocumentReference(
                    context.getWikiId(), "Main", "UserDirectory");
            String randomPassword =
                    Integer.toString((int) (Math.pow(10, 8)
                            + Math.floor(Math.random() * Math.pow(10, 7))), 10);
            Map<String, String> userAttributes = new HashMap<>();

            if (firstName != null) {
                userAttributes.put(FIRSTNAME, firstName);
            }
            if (lastName != null) {
                userAttributes.put(LASTNAME, lastName);
            }
            userAttributes.put(EMAIL, email);
            userAttributes.put(PASSWORD, randomPassword);
            int isCreated = context.getWiki().createUser(xwikiUser, userAttributes,
                    userDirRef, null, null, "edit", context);
            // Add remote user id to the user
            if (isCreated == 1) {
                log.debug("Creating user " + xwikiUser);
                XWikiDocument userDoc = context.getWiki()
                        .getDocument(createUserReference(xwikiUser), context);
                BaseObject userObj = userDoc.getXObject(getXWikiUserClassRef());

                userObj.set(ACTIVE, 1, context);
                if (avatarUrl != null) {
                    fetchUserImage(userDoc, userObj, avatarUrl.toExternalForm());
                }

                userDoc.createXObject(getIdentitOAuthAuthClassName(), context);
                BaseObject gAppsAuthClass = userDoc.getXObject(getIdentitOAuthAuthClassName());
                gAppsAuthClass.set(EXTERNAL_PROVIDER_ID, remoteUserId, context);
                context.getWiki().saveDocument(userDoc, "IdentityOAuth user creation", false, context);
            } else {
                log.warn("User creation failed");
                xwikiUser = null;
            }
            return xwikiUser;
        } catch (Exception e) {
            throw new IdentityOAuthException(e);
        }
    }

    private void updateUser(String xwikiUser,
            IdentityOAuthProvider.IdentityDescription id,
            IdentityOAuthProvider identityOAuthProvider,
            XWikiContext context)
    {

        try {
            log.debug("Found user " + xwikiUser);
            XWikiDocument userDoc = context.getWiki().getDocument(createUserReference(xwikiUser), context);
            BaseObject userObj = userDoc.getXObject(getXWikiUserClassRef());
            if (userObj == null) {
                log.debug("User found is not a user");
                return;
            }

            boolean changed = updateBaseFields(userDoc, userObj, id, context);

            changed = changed || getOrMakeIdentityObject(userDoc, id, context);

            changed = changed || identityOAuthProvider.enrichUserObject(id, userDoc);

            if (changed) {
                log.debug("User changed.");
                context.getWiki().saveDocument(userDoc, "Identity OAuth login user updated.", context);
            } else {
                log.debug("User unchanged.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IdentityOAuthException(e);
        }
    }

    private boolean updateBaseFields(XWikiDocument userDoc, BaseObject userObj,
            IdentityOAuthProvider.IdentityDescription id,
            XWikiContext context) throws
            XWikiException
    {
        boolean changed = updateField(userObj, id.firstName, FIRSTNAME, context)
                || updateField(userObj, id.lastName, LASTNAME, context);
        if (id.emails != null && id.emails.size() > 0) {
            changed = changed || updateField(userObj, id.emails.get(0), EMAIL, context);
        }
        if (id.fetchedUserImage != null) {
            changed = changed || fetchUserImage(userDoc, userObj,
                    id.fetchedUserImage.toExternalForm());
        }
        return changed;
    }

    private boolean getOrMakeIdentityObject(XWikiDocument userDoc, IdentityOAuthProvider.IdentityDescription id,
            XWikiContext context) throws
            XWikiException
    {
        BaseObject identityObject = userDoc.getXObject(getIdentitOAuthAuthClassName());
        if (identityObject == null) {
            userDoc.createXObject(getIdentitOAuthAuthClassName(), context);
            identityObject = userDoc.getXObject(getIdentitOAuthAuthClassName());
            updateField(identityObject, id.internalId, EXTERNAL_PROVIDER_ID, context);
            return true;
        } else {
            return updateField(identityObject, id.internalId, EXTERNAL_PROVIDER_ID, context);
        }
    }

    private boolean updateField(BaseObject userObj, String value, String fieldName, XWikiContext context)
    {
        if (!userObj.getStringValue(fieldName).equals(value)) {
            userObj.set(fieldName, value, context);
            return true;
        } else {
            return false;
        }
    }

    private boolean fetchUserImage(XWikiDocument userDoc, BaseObject userObj,
            String imgUrl)
    {
        try {
            if (imgUrl != null) {
                String imageUrl = imgUrl
                        + (imgUrl.indexOf('?') > -1 ? "&" : '?')
                        + "sz=512";
                XWikiAttachment attachment =
                        userObj.getStringValue(AVATAR) == null ? null
                                : userDoc.getAttachment(userObj.getStringValue(AVATAR));
                java.net.URL u = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();

                if (attachment != null) {
                    conn.addRequestProperty("If-Modified-Since",
                            DateUtil.formatDate(attachment.getDate()));
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    XWikiContext context = contextProvider.get();
                    log.debug("Pulling avatar " + imageUrl);

                    String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                    if (fileName.contains("?")) {
                        fileName = fileName.substring(0, fileName.indexOf('?'));
                    }
                    log.debug("Avatar changed " + fileName);
                    userObj.set(AVATAR, fileName, context);
                    userDoc.addAttachment(fileName, conn.getInputStream(), context).setDate(
                            new Date(conn.getLastModified()));
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    DocumentReference getXWikiUserClassRef()
    {
        return new DocumentReference(WIKINAME, XWIKISPACE, "XWikiUsers");
    }

    /**
     * @param userName the name of the user
     * @return A DocumentReference for the given username.
     * @since 1.0
     */
    @Unstable
    DocumentReference createUserReference(String userName)
    {
        return userResolver.resolve(userName);
    }

    private DocumentReference getIdentitOAuthAuthClassName()
    {
        if (authClassRef == null) {
            authClassRef = new DocumentReference(WIKINAME, SPACENAME, "IdentityOAuthIdentityClass");
        }
        return authClassRef;
    }

    /**
     * Fetches the provider-configurations from the XWIki objects.
     *
     * @return the config objects collected from the XWiki Objects; the list is mutable.
     */
    public List<ProviderConfig> loadProviderConfigs()
    {
        try {
            List results = queryManager.createQuery(
                    "from doc.object(IdentityOAuth.OAuthProviderClass) as obj",
                    //"select from doc.object(IdentityOAuth.OAuthProviderClass) as obj",
                    Query.XWQL).execute();
            configDocReferences.clear();
            List<ProviderConfig> configs = new LinkedList<>();
            for (Object r : results) {
                XWikiDocument doc = contextProvider.get().getWiki().getDocument((String) r, contextProvider.get());
                BaseObject o = doc.getXObject(getProviderConfigRef(), false, contextProvider.get());
                if (o.getIntValue(ACTIVE) != 0) {
                    ProviderConfig c = new ProviderConfig();
                    c.setName(o.getStringValue("providerName"));
                    c.setLoginCode(o.getStringValue("loginTemplate"));
                    c.setDocumentSyntax(doc.getSyntax());
                    c.setConfigPage("configPage");
                    c.setOrderHint(o.getIntValue("orderHint"));
                    DocumentReference configDocRef = documentResolver.resolve(
                            o.getStringValue("configurationObjectsPage"));
                    configDocReferences.add(configDocRef);
                    c.setConfig(readConfigurationMap(configDocRef));
                    configs.add(c);
                }
            }
            return configs;
        } catch (Exception e) {
            String msg = "Trouble at loading IdentityOAuthProvider-configurations.";
            log.warn(msg, e);
            throw new IdentityOAuthException(msg, e);
        }
    }

    private Map<String, String> readConfigurationMap(DocumentReference configurationPage)
    {
        try {
            Map<String, String> map = new HashMap<>();
            XWikiContext context = contextProvider.get();
            XWikiDocument doc = context.getWiki().getDocument(configurationPage, context);
            for (List<BaseObject> l : doc.getXObjects().values()) {
                for (BaseObject obj : l) {
                    for (String name : obj.getPropertyNames()) {
                        map.put(name, obj.getStringValue(name));
                    }
                }
            }
            return map;
        } catch (XWikiException e) {
            throw new IdentityOAuthException("Trouble at reading configuration.", e);
        }
    }
}

