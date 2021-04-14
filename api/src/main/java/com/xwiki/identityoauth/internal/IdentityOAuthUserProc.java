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

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.oidc.auth.store.OIDCUserStore;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.identityoauth.IdentityOAuthException;
import com.xwiki.identityoauth.IdentityOAuthProvider;

/**
 * Access and manipulation of the users' objects.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = IdentityOAuthUserProc.class)
@Singleton
public class IdentityOAuthUserProc implements IdentityOAuthConstants
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

    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private OIDCUserStore oidcUserStore;

    /**
     * Updates or creates the XWiki user as found.
     *
     * @param id       The information gathered from the OpenID request.
     * @param provider The IdentotyOAuthProvider that delivered.
     * @param token    The token so sa to fetch the picture.
     * @return the name of the user created (or null if none was created).
     */
    String updateXWikiUser(IdentityOAuthProvider.AbstractIdentityDescription id, IdentityOAuthProvider provider,
            String token)
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument xwikiUser = null;
        xwikiUser = findExistingUser(id);

        if (xwikiUser != null) {
            // user found.. we should update it if needed
            updateUser(xwikiUser, id, provider, token);
        } else {
            xwikiUser = createUser(id, provider, token);
        }
        return xwikiUser.getDocumentReference().getName();
    }

    private XWikiDocument findExistingUser(IdentityOAuthProvider.AbstractIdentityDescription id)
    {
        try {
            XWikiDocument candidateDoc = oidcUserStore.searchDocument(id.getIssuerURL(), id.internalId);
            if (candidateDoc != null) {
                return candidateDoc;
            }
            List<String> wikiUserList = null;
            for (String email : id.emails) {
                wikiUserList = queryManager.createQuery(
                        "from doc.object(XWiki.XWikiUsers) as user where user.email=:email",
                        Query.XWQL)
                        .bindValue(EMAIL, email).execute();
                if (wikiUserList != null && !wikiUserList.isEmpty()) {
                    break;
                }
            }

            if (wikiUserList != null && !wikiUserList.isEmpty()) {
                String userName = (String) wikiUserList.get(0);
                candidateDoc = contextProvider.get().getWiki()
                        .getDocument(userResolver.resolve(userName), contextProvider.get());
            }
            return candidateDoc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IdentityOAuthException(e);
        }
    }

    private XWikiDocument createUser(IdentityOAuthProvider.AbstractIdentityDescription id,
            IdentityOAuthProvider provider, String token)
    {
        try {
            // user not found.. need to create new user
            String email = id.emails.get(0);
            XWikiContext context = contextProvider.get();
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

            if (id.firstName != null) {
                userAttributes.put(FIRSTNAME, id.firstName);
            }
            if (id.lastName != null) {
                userAttributes.put(LASTNAME, id.lastName);
            }
            userAttributes.put(EMAIL, email);
            userAttributes.put(PASSWORD, randomPassword);
            context.getWiki().createUser(xwikiUser, userAttributes,
                    userDirRef, null, null, "edit", context);
            // Add remote user id to the user
            log.debug("Creating user " + xwikiUser);
            XWikiDocument userDoc = context.getWiki()
                    .getDocument(createUserReference(xwikiUser), context);
            BaseObject userObj = userDoc.getXObject(getXWikiUserClassRef());

            userObj.set(ACTIVE, 1, context);
            fetchUserImage(userDoc, userObj, id, provider, token);

            oidcUserStore.updateOIDCUser(userDoc, getIssuerURL(provider, id), id.internalId);

            context.getWiki().saveDocument(userDoc, "IdentityOAuth user creation", false, context);
            return userDoc;
        } catch (Exception e) {
            throw new IdentityOAuthException(e);
        }
    }

    private String getIssuerURL(IdentityOAuthProvider provider, IdentityOAuthProvider.AbstractIdentityDescription id)
    {
        String issuer = id.getIssuerURL();
        if (issuer == null) {
            issuer = provider.getProviderHint();
        }
        return issuer;
    }

    private void updateUser(XWikiDocument userDoc,
            IdentityOAuthProvider.AbstractIdentityDescription id,
            IdentityOAuthProvider provider,
            String token)
    {

        try {
            XWikiContext context = contextProvider.get();
            BaseObject userObj = userDoc.getXObject(getXWikiUserClassRef());
            if (userObj == null) {
                log.debug("User found is not a user");
                return;
            }

            boolean changed = updateBaseFields(userObj, id);

            changed = changed || oidcUserStore.updateOIDCUser(userDoc, getIssuerURL(provider, id), id.internalId);

            changed = changed || fetchUserImage(userDoc, userObj, id, provider, token);

            changed = changed || provider.enrichUserObject(id, userDoc);

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

    private boolean updateBaseFields(BaseObject userObj,
            IdentityOAuthProvider.AbstractIdentityDescription id)
    {
        XWikiContext context = contextProvider.get();
        boolean changed = updateField(userObj, id.firstName, FIRSTNAME, context)
                || updateField(userObj, id.lastName, LASTNAME, context);
        if (id.emails != null && id.emails.size() > 0) {
            changed = changed || updateField(userObj, id.emails.get(0), EMAIL, context);
        }
        return changed;
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
            IdentityOAuthProvider.AbstractIdentityDescription id,
            IdentityOAuthProvider provider, String token)
    {
        String fileName = userObj.getStringValue(AVATAR);
        Date lastModified = null;
        if (fileName != null && fileName.length() > 0) {
            XWikiAttachment attachment = userDoc.getAttachment(fileName);
            if (attachment != null) {
                // randomise modification date 15 minutes before so as to prevent tracing by caching
                lastModified = new Date((int) (attachment.getDate().getTime()
                        - 1000 * Math.floor(Math.random() * 15 * 60)));
            }
        }
        Triple<InputStream, String, String> triple = provider.fetchUserImage(lastModified, id, token);
        if (triple != null && triple.getLeft() != null) {
            try {
                log.debug("Obtained user-image: " + triple.getLeft());
                fileName = triple.getRight();
                String mediaType = triple.getMiddle();
                if (fileName == null) {
                    if ("image/jpeg".equals(mediaType)) {
                        fileName = "image.jpeg";
                    } else if ("image/png".equals(mediaType)) {
                        fileName = "image.png";
                    } else {
                        throw new IllegalStateException("Unsupported avatar picture type \"" + mediaType + "\".");
                    }
                }
                log.debug("Avatar changed " + fileName);
                userObj.set(AVATAR, fileName, contextProvider.get());
                userDoc.setAttachment(fileName, triple.getLeft(), contextProvider.get());
                return true;
            } catch (Exception e) {
                throw new IdentityOAuthException("Trouble at fetching user-image.", e);
            }
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
}
