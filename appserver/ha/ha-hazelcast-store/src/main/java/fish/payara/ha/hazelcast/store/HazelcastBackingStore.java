/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.ha.hazelcast.store;

import fish.payara.nucleus.store.ClusteredStore;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class HazelcastBackingStore<K extends Serializable, V extends Serializable>
        extends BackingStore<K, V> {

    private final BackingStoreFactory factory;
    private final String storeName;
    private String instanceName;
    private ClusteredStore clusteredStore;

    public HazelcastBackingStore(BackingStoreFactory factory, String storeName, ClusteredStore clusteredStore) {
        this.factory = factory;
        this.storeName = storeName;
        this.clusteredStore = clusteredStore;
    }

    @Override
    public BackingStoreFactory getBackingStoreFactory() {
        return factory;
    }

    @Override
    public V load(K k, String string) throws BackingStoreException {
        init();
        try {
            return (V) clusteredStore.get(storeName, k);
        } catch (ClassCastException cce) {
            Logger.getLogger(HazelcastBackingStore.class.getName()).log(Level.WARNING,
                    "ClassCastException when reading value from store", cce);
            throw new BackingStoreException(cce.getMessage());
        }
    }

    @Override
    public String save(K k, V v, boolean bln) throws BackingStoreException {
        init();
        clusteredStore.set(storeName, k, v);
        
        return instanceName;
    }

    @Override
    public void remove(K k) throws BackingStoreException {
        init();
        clusteredStore.remove(storeName, k);
    }

    @Override
    public int size() throws BackingStoreException {
        init();
        return clusteredStore.getMap(storeName).size();
    }

    private void init() throws BackingStoreException {
        if(instanceName != null) {
            return;
        }
        if (clusteredStore == null) {
            throw new BackingStoreException("Clustered Store not available, cannot use sessions yet", new IllegalStateException("Initializing"));
        }
        if (!clusteredStore.isEnabled()) {
            throw new BackingStoreException("Hazelcast is not enabled, please enable Hazelcast");
        }
        instanceName = clusteredStore.getInstanceId().toString();
    }
}
