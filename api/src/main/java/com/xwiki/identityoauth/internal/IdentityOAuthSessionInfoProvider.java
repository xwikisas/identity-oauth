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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;

/**
 * A class to carry the identity information relevant for IdentityOAuth and their implementations.
 *
 * @version $Id$
 * @since 1.0
 * */
@Component
@Singleton
public class IdentityOAuthSessionInfoProvider implements Provider<IdentityOAuthSessionInfo>
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override public IdentityOAuthSessionInfo get()
    {
        String sessKey = IdentityOAuthSessionInfo.class.getName();
        HttpSession session = contextProvider.get().getRequest().getSession();
        IdentityOAuthSessionInfo si = (IdentityOAuthSessionInfo) session.getAttribute(sessKey);
        if (si == null) {
            si = new IdentityOAuthSessionInfo();
            session.setAttribute(sessKey, si);
        }
        return si;
    }
}
