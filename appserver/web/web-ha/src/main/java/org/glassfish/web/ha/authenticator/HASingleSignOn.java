/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.web.ha.authenticator;

import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.security.web.GlassFishSingleSignOn;

import org.apache.catalina.Session;
import org.apache.catalina.authenticator.SingleSignOnEntry;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.web.ha.session.management.HAStoreBase;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.Principal;

/**
 * @author Shing Wai Chan
 */
public class HASingleSignOn extends GlassFishSingleSignOn {
    private static final Logger logger = HAStoreBase._logger;

    private BackingStore<String, HASingleSignOnEntryMetadata> ssoEntryMetadataBackingStore = null;

    private JavaEEIOUtils ioUtils = null;

    public HASingleSignOn(JavaEEIOUtils ioUtils,
            BackingStore<String, HASingleSignOnEntryMetadata> ssoEntryMetadataBackingStore) {
        super();
        this.ioUtils = ioUtils;
        this.ssoEntryMetadataBackingStore = ssoEntryMetadataBackingStore;
    }

    @Override
    protected void deregister(String ssoId) {

        //S1AS8 6155481 START        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Deregistering sso id '" + ssoId + "'");
        }
        //S1AS8 6155481 END 
        // Look up and remove the corresponding SingleSignOnEntry
        SingleSignOnEntry sso = null;
        synchronized (cache) {
            sso = cache.remove(ssoId);
        }

        if (sso == null)
            return;

        // Expire any associated sessions
        sso.expireSessions();

        try {
            ssoEntryMetadataBackingStore.remove(ssoId);
        } catch(BackingStoreException ex) {
            throw new IllegalStateException(ex);
        }
        // NOTE:  Clients may still possess the old single sign on cookie,
        // but it will be removed on the next request since it is no longer
        // in the cache
    }

    @Override
    protected void register(String ssoId, Principal principal, String authType,
                  String username, char[] password, String realmName) {

        if (logger.isLoggable(Level.FINE)) {
            String pName = ((principal != null)? principal.getName() : null);
            log("Registering sso id '" + ssoId + "' for principal '" + pName + "' and username '" + username
                + "' with auth type '" + authType + "' and realmName '" + realmName + "'");
        }

        HASingleSignOnEntry ssoEntry = null;
        synchronized (cache) {
            ssoEntry = new HASingleSignOnEntry(ssoId, principal, authType,
                    username, realmName,
                    // revisit maxIdleTime 1000000, version 0
                    System.currentTimeMillis(), 1000000, 0,
                    ioUtils);
            cache.put(ssoId, ssoEntry);
        }

        try {
            ssoEntryMetadataBackingStore.save(ssoId, ssoEntry.getMetadata(), true);
        } catch(BackingStoreException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void associate(String ssoId, long ssoVersion, Session session) {

        if (!started) {
            return;
        }

        if (debug >= 1)
            log("Associate sso id " + ssoId + " with session " + session);

        HASingleSignOnEntry sso = (HASingleSignOnEntry)lookup(ssoId, ssoVersion);
        if (sso != null) {
            session.setSsoId(ssoId);
            sso.addSession(this, session);

            try {
                ssoEntryMetadataBackingStore.save(ssoId, sso.getMetadata(), false);
            } catch(BackingStoreException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Override
    protected SingleSignOnEntry lookup(String ssoId, long ssoVersion) {
        SingleSignOnEntry ssoEntry = super.lookup(ssoId, ssoVersion);
        if (ssoEntry != null && ssoVersion > ssoEntry.getVersion()) {
            // clean the old cache
            synchronized(cache) {
                cache.remove(ssoId);
            }
            ssoEntry = null;
        }
        if (ssoEntry == null) {
            // load from ha store
            try {
                HASingleSignOnEntryMetadata mdata =
                    ssoEntryMetadataBackingStore.load(ssoId, null);
                if (mdata != null) {
                    ssoEntry = new HASingleSignOnEntry(getContainer(), mdata, ioUtils);
                    cache.put(ssoId, ssoEntry);
                }
            } catch(BackingStoreException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return ssoEntry;
    }

    @Override
    protected void removeSession(String ssoId, Session session) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Removing session " + session + " from sso id " + ssoId );
        }

        // Get a reference to the SingleSignOn
        HASingleSignOnEntry entry = (HASingleSignOnEntry)lookup(ssoId);
        if (entry == null)
            return;

        // Remove the inactive session from SingleSignOnEntry
        entry.removeSession(session);

        // If there are not sessions left in the SingleSignOnEntry,
        // deregister the entry.
        if (entry.isEmpty()) {
            deregister(ssoId);
        } else {
            try {
                ssoEntryMetadataBackingStore.save(ssoId, entry.getMetadata(), false);
            } catch(BackingStoreException ex) {
            }
        }
    }

    @Override
    public boolean isVersioningSupported() {
        return true;
    }
}
