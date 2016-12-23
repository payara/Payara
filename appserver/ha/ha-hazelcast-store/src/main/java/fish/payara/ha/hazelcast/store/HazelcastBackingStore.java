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

import com.hazelcast.core.IMap;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;

/**
 *
 * @author steve
 */
public class HazelcastBackingStore<K extends Serializable, V extends Serializable>
        extends BackingStore<K, V> {

    private final BackingStoreFactory factory;
    private final HazelcastCore core;
    private final String storeName;
    private IMap<K, V> imap;
    private String instanceName;

    public HazelcastBackingStore(BackingStoreFactory factory, String storeName, HazelcastCore core) {
        this.factory = factory;
        this.storeName = storeName;
        this.core = core;
    }

    @Override
    public BackingStoreFactory getBackingStoreFactory() {
        return factory;
    }

    @Override
    public V load(K k, String string) throws BackingStoreException {
        init();
        return imap.get(k);
    }

    @Override
    public String save(K k, V v, boolean bln) throws BackingStoreException {
        init();
        imap.set(k, v);
        
        return instanceName;
    }

    @Override
    public void remove(K k) throws BackingStoreException {
        init();
        imap.delete(k);
    }

    @Override
    public int size() throws BackingStoreException {
        init();
        return imap.size();
    }

    private void init() throws BackingStoreException {
        if(imap != null) {
            return;
        }
        if (!core.isEnabled()) {
            throw new BackingStoreException("Hazelcast is NOT Enabled please enable Hazelcast");
        }
        if(core.getInstance() == null) {
            throw new BackingStoreException("Hazelcast not yet initialized, cannot use sessions yet", new IllegalStateException("Initializing"));
        }
        imap = core.getInstance().getMap(storeName);
        instanceName = core.getInstance().getLocalEndpoint().getUuid();
    }
}
