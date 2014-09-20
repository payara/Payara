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

package org.glassfish.ha.store.adapter.cache;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;
import org.glassfish.ha.store.api.BackingStoreTransaction;
import org.glassfish.ha.store.spi.BackingStoreFactoryRegistry;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * @author Mahesh Kannan
 */
@Service(name = "replicated")
@RunLevel(StartupRunLevel.VAL)
public class ShoalBackingStoreProxy
        implements PostConstruct, BackingStoreFactory {

    @Inject
    ServiceLocator habitat;

    @Inject
    Events events;

    @Override
    public <K extends Serializable, V extends Serializable> BackingStore<K, V> createBackingStore(BackingStoreConfiguration<K, V> conf) throws BackingStoreException {
        try {
            BackingStoreFactory storeFactory = habitat.getService(BackingStoreFactory.class, "shoal-backing-store-factory");
            return storeFactory.createBackingStore(conf);
        } catch (IllegalStateException ex) {
            String msg = "ReplicatedBackingStore requires GMS to be running in the target cluster before the application is deployed. ";
            throw new BackingStoreException("Exception while creating replicated BackingStore. " + msg, ex);
        } catch (Exception ex) {
            throw new BackingStoreException("Exception while creating shoal cache", ex);
        }
    }

    @Override
    public void postConstruct() {
        BackingStoreFactoryRegistry.register("replicated", this);
        Logger.getLogger(ShoalBackingStoreProxy.class.getName()).log(Level.FINE, "Registered SHOAL BackingStore Proxy with persistence-type = replicated");
        EventListener glassfishEventListener = new EventListener() {
            @Override
            public void event(Event event) {
                if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                    BackingStoreFactoryRegistry.unregister("replicated");
                    Logger.getLogger(ShoalBackingStoreProxy.class.getName()).log(Level.FINE, "Unregistered SHOAL BackingStore Proxy with persistence-type = replicated");
                } // else if (event.is(EventTypes.SERVER_READY)) {  }
            }
        };
        events.register(glassfishEventListener);
    }

    @Override
    public BackingStoreTransaction createBackingStoreTransaction() {
        try {
            BackingStoreFactory storeFactory = habitat.getService(BackingStoreFactory.class, "shoal-backing-store-factory");
            return storeFactory.createBackingStoreTransaction();
        } catch (Exception ex) {
            //FIXME avoid runtime exception
            throw new RuntimeException("Exception while creating shoal cache", ex);
        }
    }
}
