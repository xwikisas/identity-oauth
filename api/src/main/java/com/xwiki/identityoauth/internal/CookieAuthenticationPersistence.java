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

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.identityoauth.IdentityOAuthException;

/**
 * Tools to help storing and retrieving enriched information within cookies such as the linked OAuth identity profile.
 * <p>
 * Inspiration: xwiki-authenticator-trusted https://github.com/xwiki-contrib/xwiki-authenticator-trusted/edit/master\
 * /xwiki-authenticator-trusted-api/src/main/java/org/xwiki/contrib/authentication\
 * /internal/CookieAuthenticationPersistenceStore.java.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = CookieAuthenticationPersistence.class)
@Singleton
public class CookieAuthenticationPersistence implements Initializable
{
    private static final String AUTHENTICATION_CONFIG_PREFIX = "xwiki.idoauth";

    private static final String COOKIE_PREFIX_PROPERTY = AUTHENTICATION_CONFIG_PREFIX + ".cookieprefix";

    private static final String COOKIE_PATH_PROPERTY = AUTHENTICATION_CONFIG_PREFIX + ".cookiepath";

    private static final String COOKIE_DOMAINS_PROPERTY = AUTHENTICATION_CONFIG_PREFIX + ".cookiedomains";

    private static final String ENCRYPTION_KEY_PROPERTY = AUTHENTICATION_CONFIG_PREFIX + ".encryptionKey";

    private static final String XWIKI_ENCRYPTION_KEY_PROPERTY = "xwiki.authentication.encryptionKey";

    private static final String CIPHER_ALGORITHM = "TripleDES";

    private static final String AUTHENTICATION_COOKIE = "XWIKITRUSTEDAUTH";

    /**
     * The string used to prefix cookie domain to conform to RFC 2109.
     */
    private static final String COOKIE_DOT_PFX = ".";

    private static final String EQUAL_SIGN = "=";

    private static final String UNDERSCORE = "_";

    private static final String PERMANENT_HINT = "permanent";

    @Inject
    private Logger logger;

    @Inject
    private Provider<IdentityOAuthConfigTools> gaXwikiObjects;

    private String cookiePrefix;

    private String cookiePath;

    private String[] cookieDomains;

    private Cipher encryptionCipher;

    private Cipher decryptionCipher;

    private String encryptionKey;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfg;

    @Inject
    private ComponentManager componentManager;

    /**
     * Builds a configured object.
     */
    public void initialize()
    {
        this.cookiePrefix = xwikiCfg.getProperty(COOKIE_PREFIX_PROPERTY, "");
        this.cookiePath = xwikiCfg.getProperty(COOKIE_PATH_PROPERTY, "/");

        this.encryptionKey = getEncryptionKey();

        String[] cdlist = StringUtils.split(xwikiCfg.getProperty(COOKIE_DOMAINS_PROPERTY), ',');

        if (cdlist != null && cdlist.length > 0) {
            this.cookieDomains = new String[cdlist.length];
            for (int i = 0; i < cdlist.length; ++i) {
                this.cookieDomains[i] = conformCookieDomain(cdlist[i]);
            }
        } else {
            this.cookieDomains = null;
        }

        try {
            encryptionCipher = getCipher(true);
            decryptionCipher = getCipher(false);
        } catch (Exception e) {
            throw new IdentityOAuthException("Unable to initialize ciphers", e);
        }
    }

    /**
     * Erases the information stored.
     *
     * @since 1.0
     */
    void clear()
    {
        this.setUserId("XWikiGuest");
    }

    /**
     * Retrieving the login read from the cookie.
     *
     * @return the login name found, or null.
     * @since 1.0
     */
    String getUserId()
    {
        logger.debug("retrieve cookie " + cookiePrefix + AUTHENTICATION_COOKIE);
        String cookie = getCookieValue(cookiePrefix + AUTHENTICATION_COOKIE);
        return decryptText(cookie);
    }

    /**
     * Store the user-information within the cookie.
     *
     * @param userUid the user-name (without xwiki. prefix)
     * @since 1.0
     */
    void setUserId(String userUid)
    {
        Cookie cookie = new Cookie(cookiePrefix + AUTHENTICATION_COOKIE, encryptText(userUid));
        cookie.setMaxAge(3600);
        cookie.setPath(cookiePath);
        String cookieDomain = getCookieDomain();
        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }
        if (contextProvider.get().getRequest().isSecure()) {
            cookie.setSecure(true);
        }
        contextProvider.get().getResponse().addCookie(cookie);
    }

    private String getEncryptionKey()
    {
        String key = xwikiCfg.getProperty(ENCRYPTION_KEY_PROPERTY);
        // In case the property was not defined, fall back on the XWiki encryption key property value.
        if (key == null) {
            key = xwikiCfg.getProperty(XWIKI_ENCRYPTION_KEY_PROPERTY);
        }
        // Starting with XWIKI-542:The cookie encryption keys should be randomly generated, when the encryption key is
        // not declared, it's value is automatically generated and stored in the permanent directory, instead of using
        // default values as before.
        if (key == null) {
            ConfigurationSource permConfiguration = getPermanentConfiguration();
            key = permConfiguration != null ? permConfiguration.getProperty(XWIKI_ENCRYPTION_KEY_PROPERTY, String.class)
                : null;
        }

        // If no key was found, or if it is too short, the ciphers cannot be initialized.
        if (key == null) {
            throw new IdentityOAuthException(
                "Unable to get encryption key. Please check the documentation for indications.");
        } else if (key.length() < 24) {
            throw new IdentityOAuthException(
                "The encryption key value defined in xwiki.cfg needs to have a length of at least 24 characters. Please"
                    + " check documentation and update it.");
        }

        return key;
    }

    private ConfigurationSource getPermanentConfiguration()
    {
        // Try to get the current XWiki implementation, in case it is present.
        if (componentManager.hasComponent(ConfigurationSource.class, PERMANENT_HINT)) {
            try {
                return componentManager.getInstance(ConfigurationSource.class, PERMANENT_HINT);
            } catch (ComponentLookupException e) {
                // Nothing to do.
            }
        }
        return null;
    }

    private Cipher getCipher(boolean encrypt)
        throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IdentityOAuthException
    {
        Cipher cipher;
        String secretKey = encryptionKey;
        if (secretKey != null) {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), 0, 24, CIPHER_ALGORITHM);
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key);
        } else {
            throw new IdentityOAuthException("Unable to initialize cipher.");
        }
        return cipher;
    }

    private String encryptText(String text)
    {
        try {
            logger.debug("text to encrypt : " + text);
            String encryptedText = new String(Base64.encodeBase64(
                    encryptionCipher.doFinal(text.getBytes()))).replaceAll(EQUAL_SIGN, UNDERSCORE);
            logger.debug("encrypted text : " + encryptedText);
            return encryptedText;
        } catch (Exception e) {
            logger.error("Failed to encrypt text.", e);
            return text;
        }
    }

    private String decryptText(String text)
    {
        if (text == null) {
            return null;
        }
        try {
            logger.debug("text to decrypt : " + text);
            String decryptedText = new String(decryptionCipher.doFinal(
                    Base64.decodeBase64(text.replaceAll(UNDERSCORE, EQUAL_SIGN).getBytes(
                            StandardCharsets.ISO_8859_1))));
            logger.debug("decrypted text : " + decryptedText);
            return decryptedText;
        } catch (Exception e) {
            logger.error("Failed to decrypt text.", e);
            return text;
        }
    }

    /**
     * Retrieve given cookie null-safe.
     *
     * @param cookieName name of the cookie
     * @return the cookie
     * @since 1.0
     */
    private String getCookieValue(String cookieName)
    {
        if (contextProvider.get().getRequest() != null) {
            Cookie cookie = contextProvider.get().getRequest().getCookie(cookieName);
            if (cookie != null) {
                logger.debug("cookie : " + cookie);
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Compute the actual domain the cookie is supposed to be set for. Search through the list of generalized domains
     * for a partial match. If no match is found, then no specific domain is used, which means that the cookie will be
     * valid only for the requested host.
     *
     * @return The configured domain generalization that matches the request, or null if no match is found.
     * @since 1.0
     */
    private String getCookieDomain()
    {
        String cookieDomain = null;
        if (this.cookieDomains != null) {
            // Conform the server name like we conform cookie domain by prefixing with a dot.
            // This will ensure both localhost.localdomain and any.localhost.localdomain will match
            // the same cookie domain.
            String servername = conformCookieDomain(contextProvider.get().getRequest().getServerName());
            for (String domain : this.cookieDomains) {
                if (servername.endsWith(domain)) {
                    cookieDomain = domain;
                    break;
                }
            }
        }
        logger.debug("Cookie domain is:" + cookieDomain);
        return cookieDomain;
    }

    /**
     * Ensure cookie domains are prefixed with a dot to conform to RFC 2109.
     *
     * @param domain a cookie domain.
     * @return a conform cookie domain.
     * @since 1.0
     */
    private String conformCookieDomain(String domain)
    {
        if (domain != null && !domain.startsWith(COOKIE_DOT_PFX)) {
            return COOKIE_DOT_PFX.concat(domain);
        } else {
            return domain;
        }
    }
}
