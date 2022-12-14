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

import java.util.Comparator;
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

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

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
@Component(roles = IdentityOAuthConfigTools.class)
@Singleton
public class IdentityOAuthConfigTools implements IdentityOAuthConstants
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
    @Named("context")
    private ComponentManager componentManager;

    /**
     * EntitytReference of the Identity OAuth Provider Config Class.
     */
    private EntityReference providerConfigRef;

    /**
     * Reference of the Identity OAuth Configuration class.
     */
    private EntityReference configRef;

    private Set<DocumentReference> configDocReferences = new HashSet<>();

    EntityReference getProviderConfigRef()
    {
        if (providerConfigRef == null) {
            DocumentReference docRef = new DocumentReference(WIKINAME,
                    SPACENAME, "OAuthProviderClass");
            providerConfigRef = new ObjectReference("IdentityOAuth.OAuthProviderClass", docRef);
        }
        return providerConfigRef;
    }

    EntityReference getConfigRef()
    {
        if (configRef == null) {
            DocumentReference docRef = new DocumentReference(WIKINAME,
                SPACENAME, "IdentityOAuthConfigClass");
            configRef = new ObjectReference("IdentityOAuth.IdentityOAuthConfigClass", docRef);
        }
        return configRef;
    }

    boolean hasIOConfigObject(XWikiDocument doc)
    {
        if (configDocReferences.contains(doc.getDocumentReference())) {
            return true;
        }
        BaseObject providerObj = doc.getXObject(getProviderConfigRef(), false, contextProvider.get());
        BaseObject configObj = doc.getXObject(getConfigRef(), false, contextProvider.get());

        return providerObj != null || configObj != null;
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
                    Query.XWQL).setWiki(contextProvider.get().getMainXWiki()).execute();
            configDocReferences.clear();
            List<ProviderConfig> configs = new LinkedList<>();
            log.info("Found providers: " + results);
            for (Object r : results) {
                XWikiDocument doc = contextProvider.get().getWiki().getDocument((String) r, contextProvider.get());
                BaseObject o = doc.getXObject(getProviderConfigRef(), false, contextProvider.get());
                log.info("Provider " + o.getStringValue(PROVIDER_HINT));
                if (o.getIntValue(ACTIVE) != 0) {
                    log.info("... active");
                    ProviderConfig c = new ProviderConfig();
                    c.setName(o.getStringValue(PROVIDER_HINT));
                    c.setLoginCode(o.getStringValue("loginTemplate"));
                    c.setLoginCodeSyntax(doc.getSyntax());
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
                        String value = obj.getStringValue(name);
                        if ("redirectUrl".equals(name)) {
                            if (value == null || value.trim().length() == 0) {
                                value = getLoginPageUrl();
                            }
                        }
                        map.put(name, value);
                    }
                }
            }
            return map;
        } catch (XWikiException e) {
            throw new IdentityOAuthException("Trouble at reading configuration.", e);
        }
    }

    /**
     * Extracts the content of an attachment and encodes it as a data-url (using media-type and base64). This
     * implementations returns "file-size-is-too-large" if bigger than 2 Mb.
     *
     * @param attachmentRef the reference to the attachment e.g. Space/Page/filename.png
     * @return a data-url suitable to embed within a web-page.
     */
    String createDataUrl(String attachmentRef)
    {
        try {
            XWikiContext context = contextProvider.get();
            AttachmentReference ref = attachmentResolver.resolve(attachmentRef);
            XWikiDocument doc = context.getWiki().getDocument(ref.getParent(), context);
            XWikiAttachment attachment = doc.getAttachment(ref.getName());
            if (attachment == null) {
                return "attachment " + attachmentRef + " not found";
            }
            if (attachment.getLongSize() > 2 * 1024 * 1024) {
                return "file-size-is-too-large";
            }

            return "data:" + attachment.getMimeType() + ";base64,"
                    + new Base64(2 * 1024 * 1024 * 2)
                    .encodeAsString(attachment.getContent(context));
        } catch (Exception e) {
            log.warn("Issue at creating data-url", e);
            throw new IdentityOAuthException("Trouble at creating data-url", e);
        }
    }

    String getLoginPageUrl()
    {
        XWikiContext context = contextProvider.get();
        String loginUrl = context.getWiki().getURL(
                documentResolver.resolve("xwiki:XWiki.XWikiLogin"),
                "login", contextProvider.get());
        if (!(loginUrl.startsWith("http://") || loginUrl.startsWith("https://"))) {
            String abs = context.getURL().toExternalForm();
            if (loginUrl.startsWith("/")) {
                loginUrl = abs.substring(0, abs.indexOf('/', 8)) + loginUrl;
                // 8 = "http://".length or "https://".length
            } else {
                loginUrl = abs;
            }
        }
        if (loginUrl.contains("?")) {
            loginUrl = loginUrl.substring(0, loginUrl.indexOf('?'));
        }
        log.info("Setting automatic redirect URL to " + loginUrl + ".");
        return loginUrl;
    }

    List<ProviderConfig> loadAndRebuildProviders()
    {
        List<ProviderConfig> providerConfigs = this.loadProviderConfigs();
        providerConfigs.sort(new Comparator<ProviderConfig>()
        {
            @Override
            public int compare(ProviderConfig o1, ProviderConfig o2)
            {
                return Integer.compare(o1.getOrderHint(), o2.getOrderHint());
            }
        });

        // initialize the configured providers
        for (ProviderConfig config : providerConfigs) {
            try {
                IdentityOAuthProvider pr = componentManager.getInstance(IdentityOAuthProvider.class, config.getName());
                pr.setProviderHint(config.getName());
                pr.setConfigPage(config.getConfigPage());
                pr.initialize(config.getConfig());
                config.setProvider(pr);
                String loginCode = config.getLoginCode();
                if (loginCode.contains(BASE64_MARKER)) {
                    int cursor = -1;
                    while ((cursor = loginCode.indexOf(BASE64_MARKER, cursor + 1)) > -1) {
                        int startOfPictName = cursor + BASE64_MARKER.length();
                        int endOfPictName = loginCode.indexOf("--", startOfPictName);
                        loginCode = loginCode.substring(0, cursor)
                                + this.createDataUrl(loginCode.substring(startOfPictName, endOfPictName))
                                + loginCode.substring(endOfPictName + 2);
                    }
                }
                config.setPreparedLoginCode(loginCode.replaceAll("-PROVIDER-", config.getName()));
            } catch (Exception e) {
                log.warn("Trouble at creating provider \"" + config.getName() + "\":", e);
            }
        }
        return providerConfigs;
    }
}

