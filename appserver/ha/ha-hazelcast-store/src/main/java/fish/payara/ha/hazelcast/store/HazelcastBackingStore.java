/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 1014 C2B2 Consulting Limited. All rights reserved.

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

    BackingStoreFactory factory;
    IMap<K, V> imap;

    public HazelcastBackingStore(IMap storeMap, BackingStoreFactory factory) {
        this.factory = factory;
        imap = storeMap;

    }

    @Override
    public BackingStoreFactory getBackingStoreFactory() {
        return factory;
    }

    @Override
    public V load(K k, String string) throws BackingStoreException {
        return imap.get(k);
    }

    @Override
    public String save(K k, V v, boolean bln) throws BackingStoreException {
        imap.put(k, v);
        return getBackingStoreConfiguration().getInstanceName();
    }

    @Override
    public void remove(K k) throws BackingStoreException {
        imap.remove(k);
    }

    @Override
    public int size() throws BackingStoreException {
        return imap.size();
    }

}
