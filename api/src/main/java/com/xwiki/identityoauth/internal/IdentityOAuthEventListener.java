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
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.identityoauth.IdentityOAuthManager;

/**
 * Registered object to listen to document changes.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(IdentityOAuthEventListener.NAME)
@Singleton
public class IdentityOAuthEventListener extends AbstractEventListener
{
    static final String NAME = "IdentityOAuthEventListener";

    @Inject
    private IdentityOAuthConfigTools ioXWikiObjects;

    @Inject
    private IdentityOAuthManager identityOAuthManager;

    @Inject
    private Logger log;

    /**
     * Creates an event-listener filtering for ApplicationReadyEvent and DocumentUpdatedEvent.
     */
    public IdentityOAuthEventListener()
    {
        super(NAME, new ApplicationReadyEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    /**
     * Triggers a configuration reload (if the configuration is changed or the app is started) or an initialization (if
     * the app is started).
     *
     * @param event  The event listened to.
     * @param source The object sending the event.
     * @param data   Data about the event.
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (log.isDebugEnabled()) {
            log.debug("Event! " + event + " from " + source);
        }
        boolean applicationStarted = false;
        boolean configChanged = false;
        if (event instanceof ApplicationReadyEvent) {
            applicationStarted = true;
        }
        if (event instanceof DocumentUpdatedEvent || event instanceof DocumentDeletedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            if (document != null && ioXWikiObjects.hasIOConfigObject(document)) {
                configChanged = true;
            }
        }

        if (configChanged || applicationStarted) {
            log.info("Reloading IdentityOAuth providers! ");
            identityOAuthManager.reloadConfig();
        }
    }
}
