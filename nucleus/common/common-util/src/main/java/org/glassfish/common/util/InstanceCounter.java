/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.common.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Counts instances of a particular class, helpful in leak detection
 * <p>
 * Example: Put the following line into the class you want counted:
 * <p>
 * {@code
 *     private final InstanceCounter instanceCounter = new InstanceCounter<>(this);
 * }
 *
 * @author Cuba Stanley
 */
public class InstanceCounter {
    private static class Count {
        int instanceCount;
        private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
        private final Set<WeakReference<?>> references = Collections.newSetFromMap(new IdentityHashMap<>());

        public Count() {
            instanceCount = 1;
        }

        public Count(int instanceCount) {
            this.instanceCount = instanceCount;
        }
    }

    private static final ConcurrentMap<Class<?>, Count> INSTANCE_COUNT = new ConcurrentHashMap<>();

    /**
     * triggers instance counting of the specified
     *
     * @param counted object to be counted
     */
    public InstanceCounter(Object counted) {
        INSTANCE_COUNT.compute(counted.getClass(), (clazz, value) -> {
            Count count;
            if (value == null) {
                count = new Count();
            } else {
                count = value;
                ++count.instanceCount;
            }
            count.references.add(new WeakReference<>(counted, count.referenceQueue));
            return count;
        });
    }

    /**
     * Returns instance count of a particular class.
     *
     * @param countedType
     * @param timeout in milliseconds to wait to collect removed instances
     * @return current instance count
     */
    public static int getInstanceCount(Class<?> countedType, long timeout) {
        INSTANCE_COUNT.compute(countedType, (clazz, value) -> {
            if (value == null) {
                return new Count(0);
            } else {
                try {
                    value.instanceCount -= countRemovedInstances(value, timeout);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                return value;
            }
        });
        return INSTANCE_COUNT.get(countedType).instanceCount;
    }

    /**
     * gets actual references to counted objects
     *
     * @param countedType
     * @param timeout in milliseconds
     * @return list of counted objects
     */
    public static Set<WeakReference<?>> getInstances(Class<?> countedType, long timeout) {
        getInstanceCount(countedType, timeout);   // update the count, create if necessary
        return INSTANCE_COUNT.get(countedType).references;
    }

    private static int countRemovedInstances(Count count, long timeout) throws InterruptedException {
        int removedInstances = 0;
        Reference<?> reference = count.referenceQueue.remove(timeout);
        while (reference != null) {
            ++removedInstances;
            reference.clear();
            assert count.references.remove(reference) : "non-existent reference";

            reference = count.referenceQueue.poll();
        }
        return removedInstances;
    }
}
