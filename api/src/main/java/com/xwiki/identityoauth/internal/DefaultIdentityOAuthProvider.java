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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.identityoauth.IdentityOAuthProvider;
import com.xwiki.licensing.Licensor;

/**
 * IdentityOAuthProvider implementation specific to the XWiki default login action.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(DefaultIdentityOAuthProvider.PROVIDER_HINT)
@Singleton
public class DefaultIdentityOAuthProvider implements IdentityOAuthProvider
{
    protected static final String PROVIDER_HINT = "default";

    @Inject
    protected Logger logger;

    @Inject
    protected Provider<Licensor> licensorProvider;

    private boolean active;

    @Override
    public void initialize(Map<String, String> config)
    {
        String activeParam = config.get("active");
        this.active = activeParam.equals("1") || Boolean.parseBoolean(activeParam);
    }

    @Override
    public boolean isActive()
    {
        return this.active;
    }

    @Override
    public boolean isReady()
    {
        return true;
    }

    @Override
    public String getProviderHint()
    {
        return PROVIDER_HINT;
    }

    @Override
    public void setProviderHint(String hint)
    {
        if (!PROVIDER_HINT.equals(hint)) {
            throw new IllegalStateException("Only \"default\" is accepted as hint.");
        }
    }

    @Override
    public String validateConfiguration()
    {
        return "ok";
    }

    @Override
    public List<String> getMinimumScopes()
    {
        return null;
    }

    @Override
    public String getRemoteAuthorizationUrl(String redirectUrl)
    {
        return null;
    }

    @Override
    public Pair<String, Date> createToken(String authCode)
    {
        return null;
    }

    @Override
    public String readAuthorizationFromReturn(Map<String, String[]> params)
    {
        return null;
    }

    @Override
    public AbstractIdentityDescription fetchIdentityDetails(String token)
    {
        return null;
    }

    @Override
    public Triple<InputStream, String, String> fetchUserImage(Date ifModifiedSince, AbstractIdentityDescription id,
        String token)
    {
        return null;
    }

    @Override
    public boolean enrichUserObject(AbstractIdentityDescription idDescription, XWikiDocument doc)
    {
        return false;
    }

    @Override
    public void receiveFreshToken(String token)
    {
        // Not implemented
    }

    @Override
    public void setConfigPage(String page)
    {
        // Not implemented
    }
}
