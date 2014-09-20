/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.ha.session.management;

import org.apache.catalina.Session;
import org.apache.catalina.session.PersistentManagerBase;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.Storeable;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Rajiv Mordani
 */
public abstract class ReplicationManagerBase<T extends Storeable> extends PersistentManagerBase {

    protected BackingStore<String, T> backingStore;
    protected SessionFactory sessionFactory;

    protected static final String name = "ReplicationManagerBase";

    protected Logger _logger = HAStoreBase._logger;

    @LogMessageInfo(
            message = "Failed to remove session from backing store",
            level = "WARNING")
    public static final String FAILED_TO_REMOVE_SESSION = "AS-WEB-HA-00006";

    @LogMessageInfo(
            message = "Required version NumberFormatException",
            level = "INFO")
    public static final String REQUIRED_VERSION_NFE = "AS-WEB-HA-00007";

    protected boolean relaxCacheVersionSemantics = false;
    protected boolean disableJreplica = false;

    public BackingStore<String, T> getBackingStore() {
        return this.backingStore;
    }

    public abstract void createBackingStore(String persistenceType, String storeName, Class<T> metadataClass, Map<String, Object> vendorMap);
    
    public Session createNewSession() {
        return sessionFactory.createSession(this);
    }

    public Session createEmptySession() {
        Session sess = sessionFactory.createSession(this);
        return sess;
    }


    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setBackingStore(BackingStore<String, T> backingStore) {
        this.backingStore = backingStore;
    }

    public void doRemove(String id) {
        try {
            backingStore.remove(id);
        } catch (BackingStoreException e) {
            _logger.warning(FAILED_TO_REMOVE_SESSION);
        }
    }

    public boolean isSessionVersioningSupported() {
        return true; 
    }

    public Session findSession(String id, String version) throws IOException {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("in findSession: version=" + version);
        }
        if(!this.isSessionIdValid(id) || version == null) {
            return null;
        }
        Session loadedSession = null;
        long requiredVersion = 0L;
        long cachedVersion = -1L;
        try {
            requiredVersion = (Long.valueOf(version)).longValue();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Required version " + requiredVersion);
            }
        } catch (NumberFormatException ex) {
             _logger.log(Level.INFO, REQUIRED_VERSION_NFE, ex);
            //deliberately do nothing
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("findSession:requiredVersion=" + requiredVersion);
        }
        Session cachedSession = sessions.get(id);
        if(cachedSession != null) {
            cachedVersion = cachedSession.getVersion();
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("findSession:cachedVersion=" + cachedVersion);
        }
        //if version match return cached session else purge it from cache
        //if relaxCacheVersionSemantics is set true then we return the
        //cached version even if it is greater than the required version
        if(cachedVersion == requiredVersion || (isRelaxCacheVersionSemantics() && (cachedVersion > requiredVersion))) {
            return cachedSession;
        } else {
            //if relaxCacheVersionSemantics - we do not remove because even
            //though stale we might return it as the best we can do
            if(cachedVersion < requiredVersion && (!isRelaxCacheVersionSemantics())) {
                this.removeSessionFromManagerCache(cachedSession);
                cachedSession = null;
                cachedVersion = -1L;
            }
        }
        // See if the Session is in the Store
        if(requiredVersion != -1L) {
            loadedSession = swapIn(id, version);
        } else {
            loadedSession = swapIn(id);
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("findSession:swappedInSession=" + loadedSession);
        }

        if(loadedSession == null || loadedSession.getVersion() < cachedVersion) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicationManagerBase>>findSession:returning cached version:" + cachedVersion);
            }
            return cachedSession;
        }
        if(loadedSession.getVersion() < requiredVersion && (!isRelaxCacheVersionSemantics())) {
            loadedSession = null;
        }
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationManagerBase>>findSession:returning:" + loadedSession);
        }
        return (loadedSession);

    }

    /** should relax cache version semantics be applied */
    public boolean isRelaxCacheVersionSemantics() {
        return relaxCacheVersionSemantics;
    }

    /**
     * set the relaxCacheVersionSemantics
     * @param value
     */
    public void setRelaxCacheVersionSemantics(boolean value) {
        relaxCacheVersionSemantics = value;
    }


    public void removeSessionFromManagerCache(Session session) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.fine("in " + this.getClass().getName() + ">>removeSessionFromManagerCache:session = " + session);
        }
        if(session == null) {
            return;
        }
        Session removed = null;
        removed = sessions.remove(session.getIdInternal());
        if (removed != null && _logger.isLoggable(Level.FINE)){
            _logger.fine("Remove from manager cache id=" + session.getId());
        }
    }

    public void setDisableJreplica(boolean disableJreplica) {
        this.disableJreplica = disableJreplica;
    }

    public boolean isDisableJreplica() {
        return  this.disableJreplica;
    }



    public abstract void doValveSave(Session session);

    public abstract String getReplicaFromPredictor(String sessionId, String oldJreplicaValue); 
}
