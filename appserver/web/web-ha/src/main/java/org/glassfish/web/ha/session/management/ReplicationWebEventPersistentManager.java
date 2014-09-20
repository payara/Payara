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

/*
 * ReplicationWebEventPersistentManager.java
 *
 * Created on November 18, 2005, 3:38 PM
 *
 */

package org.glassfish.web.ha.session.management;

import org.apache.catalina.Session;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.ha.common.GlassFishHAReplicaPredictor;
import org.glassfish.ha.common.HACookieInfo;
import org.glassfish.ha.common.HACookieManager;
import org.glassfish.ha.common.NoopHAReplicaPredictor;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;
import org.glassfish.ha.store.api.Storeable;
import org.glassfish.logging.annotation.LogMessageInfo;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 *
 * @author Rajiv Mordani
 */
@Service
@PerLookup
public class ReplicationWebEventPersistentManager<T extends Storeable> extends ReplicationManagerBase<T>
        implements WebEventPersistentManager {

    @Inject
    private ServiceLocator services;

    @Inject
    private GMSAdapterService gmsAdapterService;


    private GlassFishHAReplicaPredictor predictor;

    private String clusterName = "";

    private String instanceName = "";

    @LogMessageInfo(
            message = "Could not create backing store",
            level = "WARNING")
    public static final String COULD_NOT_CREATE_BACKING_STORE = "AS-WEB-HA-00008";


    /**
     * The descriptive information about this implementation.
     */
    private static final String info = "ReplicationWebEventPersistentManager/1.0";


    /**
     * The descriptive name of this Manager implementation (for logging).
     */
    private static final String name = "ReplicationWebEventPersistentManager";    


    // ------------------------------------------------------------- Properties


    /**
     * Return descriptive information about this Manager implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (this.info);

    }
    
    /** Creates a new instance of ReplicationWebEventPersistentManager */
    public ReplicationWebEventPersistentManager() {
        super();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("ReplicationWebEventPersistentManager created");
        }
    }
    
    /**
    * called from valve; does the save of session
    *
    * @param session 
    *   The session to store
    */    
    public void doValveSave(Session session) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("in doValveSave");
        }

            try {
                ReplicationStore replicationStore = (ReplicationStore) this.getStore();
                replicationStore.doValveSave(session);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("FINISHED repStore.valveSave");
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                _logger.log(Level.FINE, "exception occurred in doValveSave id=" + session.getIdInternal(),
                                ex);
                
            }
    }
   

    //START OF 6364900
    public void postRequestDispatcherProcess(ServletRequest request, ServletResponse response) {
        Session sess = this.getSession(request);
        
        if(sess != null) {         
            doValveSave(sess);            
        }
        return;
    }
    
    private Session getSession(ServletRequest request) {
        javax.servlet.http.HttpServletRequest httpReq =
            (javax.servlet.http.HttpServletRequest) request;
        javax.servlet.http.HttpSession httpSess = httpReq.getSession(false);
        if(httpSess == null) {
            return null;
        }
        String id = httpSess.getId();
        Session sess = null;
        try {
            sess = this.findSession(id);
        } catch (java.io.IOException ex) {}

        return sess;
    } 
    //END OF 6364900 
    
    //new code start    
    

    //private static int NUMBER_OF_REQUESTS_BEFORE_FLUSH = 1000;
    //volatile Map<String, String> removedKeysMap = new ConcurrentHashMap<String, String>();
    //private static AtomicInteger requestCounter = new AtomicInteger(0);
    private static int _messageIDCounter = 0;
    //private AtomicBoolean  timeToChange = new AtomicBoolean(false);
    

    // ------------------------------------------------------------- Properties


    /**
     * Return the descriptive short name of this Manager implementation.
     */
    public String getName() {

        return this.name;

    }

    /**
     * Back up idle sessions.
     * Hercules: modified method we do not want
     * background saves when we are using web-event persistence-frequency
     */
    protected void processMaxIdleBackups() {
        //this is a deliberate no-op for this manager
        return;
    }
    
    /**
     * Swap idle sessions out to Store if too many are active
     * Hercules: modified method
     */
    protected void processMaxActiveSwaps() {
        //this is a deliberate no-op for this manager
        return;
    }

    /**
     * Swap idle sessions out to Store if they are idle too long.
     */
    protected void processMaxIdleSwaps() {
        //this is a deliberate no-op for this manager
        return;
    }

    public String getReplicaFromPredictor(String sessionId, String oldJreplicaValue) {
        if (isDisableJreplica()) {
            return null;
        }
        HACookieInfo cookieInfo = predictor.makeCookie(gmsAdapterService.getGMSAdapter().getClusterName(), sessionId, oldJreplicaValue);
        HACookieManager.setCurrrent(cookieInfo);
        return cookieInfo.getNewReplicaCookie();
    }


    @Override
    public void createBackingStore(String persistenceType, String storeName, Class<T> metadataClass, Map<String, Object> vendorMap) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Create backing store invoked with persistence type " + persistenceType + " and store name " + storeName);
        }
        BackingStoreFactory factory = services.getService(BackingStoreFactory.class, persistenceType);
        BackingStoreConfiguration<String, T> conf = new BackingStoreConfiguration<String, T>();

        if(gmsAdapterService.isGmsEnabled()) {
            clusterName = gmsAdapterService.getGMSAdapter().getClusterName();
            instanceName = gmsAdapterService.getGMSAdapter().getModule().getInstanceName();
        }
        conf.setStoreName(storeName)
                .setClusterName(clusterName)
                .setInstanceName(instanceName)
                .setStoreType(persistenceType)
                .setKeyClazz(String.class).setValueClazz(metadataClass)
                .setClassLoader(this.getClass().getClassLoader());
        if (vendorMap != null) {
            conf.getVendorSpecificSettings().putAll(vendorMap);
        }

        try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("About to create backing store " + conf);
            }
            this.backingStore = factory.createBackingStore(conf);
        } catch (BackingStoreException e) {
            _logger.log(Level.WARNING, COULD_NOT_CREATE_BACKING_STORE, e);
        }
        Object obj = conf.getVendorSpecificSettings().get("key.mapper");
        if (obj != null && obj instanceof GlassFishHAReplicaPredictor) {
            predictor = (GlassFishHAReplicaPredictor)obj;
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("ReplicatedManager.keymapper is " + predictor);
            }
        } else {
            predictor = new NoopHAReplicaPredictor();
        }
    }
}
