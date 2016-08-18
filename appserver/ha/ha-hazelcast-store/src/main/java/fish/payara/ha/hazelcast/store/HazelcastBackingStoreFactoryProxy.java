/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.ha.hazelcast.store;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
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
 *
 * @author steve
 */
@Service(name = "hazelcast")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastBackingStoreFactoryProxy implements PostConstruct, BackingStoreFactory {

    @Inject
    ServiceLocator habitat;

    @Override
    public <K extends Serializable, V extends Serializable> BackingStore<K, V> createBackingStore(BackingStoreConfiguration<K, V> bsc) throws BackingStoreException {
        try {
            BackingStoreFactory storeFactory = habitat.getService(BackingStoreFactory.class, "hazelcast-factory");
            return storeFactory.createBackingStore(bsc);
        } catch (Exception ex) {
            Logger.getLogger(HazelcastBackingStoreFactoryProxy.class.getName()).log(Level.WARNING, "Exception while creating hazelcast cache", ex);
            throw new BackingStoreException("Exception while creating hazelcast cache", ex);
        }
    }

    @Override
    public BackingStoreTransaction createBackingStoreTransaction() {
        try {
            BackingStoreFactory storeFactory = habitat.getService(BackingStoreFactory.class, "hazelcast-factory");
            return storeFactory.createBackingStoreTransaction();
        } catch (Exception ex) {
            throw new RuntimeException("Exception while creating hazelcast transaction", ex);
        }
    }

    @Override
    public void postConstruct() {
        BackingStoreFactoryRegistry.register("hazelcast", this);
        Logger.getLogger(HazelcastBackingStoreFactory.class.getName()).log(Level.INFO, "Registered Hazelcast BackingStoreFactory with persistence-type = hazelcast");
    }

}
