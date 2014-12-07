/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

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

import com.hazelcast.transaction.TransactionContext;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreTransaction;

/**
 *
 * @author steve
 */
public class HazelcastBackingStoreTransaction implements BackingStoreTransaction {

    private TransactionContext ctx;

    HazelcastBackingStoreTransaction(TransactionContext newTransactionContext) {
        ctx = newTransactionContext;
    }

    @Override
    public void commit() throws BackingStoreException {
        ctx.commitTransaction();
    }

}
