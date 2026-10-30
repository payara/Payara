/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.store;

import fish.payara.notification.requesttracing.RequestTrace;
import java.util.Collection;
import java.util.function.IntSupplier;

/**
 * An interface for a store of
 * {@link RequestTrace} objects.
 */
public interface RequestTraceStoreInterface {

    /**
     * Adds a request trace to the store.
     *
     * @param trace the trace to add.
     * @return The trace that was removed, or null if no trace was removed
     */
    RequestTrace addTrace(RequestTrace trace);

    /**
     * Adds a request trace to the store, removing the specified trace if present and necessary.
     * @param trace The trace to add
     * @param traceToRemove The trace to remove if present
     * @return The trace that was removed, or null if no trace was removed
     */
    RequestTrace addTrace(RequestTrace trace, RequestTrace traceToRemove);
    
    /**
     * Gets the entire contents of the store.
     *
     * @return every request trace in the store.
     */
    Collection<RequestTrace> getTraces();

    /**
     * Gets the contents of the store, up to a limit number of items.
     *
     * @param limit the maximum number of traces to return.
     * @return up to the limit number of items from the store.
     */
    Collection<RequestTrace> getTraces(int limit);

    /**
     * Sets the supplier for the maximum size of the store.
     * 
     * Any traces added after this size will cause another to be removed. The trace removed depends on the store
     * implementation.
     *
     * @param size A supplier for the maximum size of the store.
     */
    void setSize(IntSupplier size);

    /**
     * Gets the current size of the store.
     *
     * @return the size of the store.
     */
    int getStoreSize();

    /**
     *  Removes all of the traces from the store. 
     *  The store will be empty after this method is called. 
     * 
     * @return The traces that were stored
     */
    Collection<RequestTrace> emptyStore();

    /**
     * @return true in case of a clustered store, false in case of a local store.
     */
    default boolean isShared() {
       return false;
    }
}
