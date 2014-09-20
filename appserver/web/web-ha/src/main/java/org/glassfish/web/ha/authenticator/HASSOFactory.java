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
 * HASSOFactory.java
 *
 * Created on August 24, 2004, 5:27 PM
 */

package org.glassfish.web.ha.authenticator;


import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;

import com.sun.enterprise.web.SSOFactory;
import com.sun.enterprise.web.session.PersistenceType;
import com.sun.enterprise.security.web.GlassFishSingleSignOn;

import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.PerLookup;

/**
 *
 * @author lwhite
 * @author Shing Wai Chan
 */
@Service
@PerLookup
public class HASSOFactory implements SSOFactory {
    private static final String STORE_NAME = "SSOStore";

    private static BackingStore<String, HASingleSignOnEntryMetadata> ssoEntryMetadataBackingStore = null;

    @Inject
    private ServiceLocator services;
   
    @Inject
    private JavaEEIOUtils ioUtils;
    
    /**
     * Create a SingleSignOn valve
     * HASingleSignOnValve is created is global availability-enabled
     * is true and sso-failover-enabled
     */
    @Override
    public GlassFishSingleSignOn createSingleSignOnValve(String virtualServerName) {
        String persistenceType = PersistenceType.REPLICATED.getType();
        return new HASingleSignOn(ioUtils,
                getSsoEntryMetadataBackingStore(persistenceType, STORE_NAME, services));
    }   
    
    protected static synchronized BackingStore<String, HASingleSignOnEntryMetadata>
            getSsoEntryMetadataBackingStore(
            String persistenceType, String storeName, ServiceLocator services) {

        if (ssoEntryMetadataBackingStore == null) {
            BackingStoreFactory factory = services.getService(BackingStoreFactory.class, persistenceType);
            BackingStoreConfiguration<String, HASingleSignOnEntryMetadata> conf =
                    new BackingStoreConfiguration<String, HASingleSignOnEntryMetadata>();

            String clusterName = "";
            String instanceName = "";
            GMSAdapterService gmsAdapterService = services.getService(GMSAdapterService.class);
            if(gmsAdapterService.isGmsEnabled()) {
                clusterName = gmsAdapterService.getGMSAdapter().getClusterName();
                instanceName = gmsAdapterService.getGMSAdapter().getModule().getInstanceName();
            }

            conf.setStoreName(storeName)
                    .setClusterName(clusterName)
                    .setInstanceName(instanceName)
                    .setStoreType(persistenceType)
                    .setKeyClazz(String.class).setValueClazz(HASingleSignOnEntryMetadata.class);

            try {
                ssoEntryMetadataBackingStore = factory.createBackingStore(conf);
            } catch (BackingStoreException e) {
                e.printStackTrace();  
            }
        }

        return ssoEntryMetadataBackingStore;
    }
}
