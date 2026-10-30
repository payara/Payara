/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author lprimak
 */
public class InstanceCounterTest {
    private static final long TIMEOUT_FOR_GC = 100;
    private static final long TIMEOUT_FOR_NO_GC = 1;

    private static class Counted {
        int field;
        private final InstanceCounter counter = new InstanceCounter(this);

        Counted(int field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return String.format("Counted: %d", field);
        }
    }

    @Test
    public void simplestFantom() throws InterruptedException {
        Object s = new Object();

        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        PhantomReference<Object> ref = new PhantomReference<>(s, queue);

        s = null;
        System.gc();
        assertNotNull(queue.remove(TIMEOUT_FOR_GC));
    }

    @Test
    public void phantom() throws InterruptedException {
        ReferenceQueue<Counted> queue = new ReferenceQueue<>();
        Counted counted1 = new Counted(1);
        PhantomReference<Counted> ref1 = new PhantomReference<>(counted1, queue);
        counted1 = null;
        System.gc();
        Reference<?> ref2 = queue.remove(TIMEOUT_FOR_GC);
        assertNotNull(ref2);
        ref2.clear();
        assertEquals(0, InstanceCounter.getInstanceCount(Counted.class, 0));
    }

    @Test
    public void counter() {
        assertEquals(0, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        Counted counted1 = new Counted(1);
        assertEquals(1, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        System.gc();
        assertEquals(1, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        Counted counted2 = new Counted(2);
        assertEquals(2, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        System.gc();
        assertEquals(2, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        counted2 = null;
        System.gc();
        assertEquals(1, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_GC));
        counted1 = null;
        System.gc();
        assertEquals(0, InstanceCounter.getInstanceCount(Counted.class, 0));
    }

    @Test
    public void instances() {
        assertEquals(0, InstanceCounter.getInstanceCount(Counted.class, TIMEOUT_FOR_NO_GC));
        Counted counted1 = new Counted(1);
        Counted counted2 = new Counted(2);
        Set<Counted> expectedSet = Collections.newSetFromMap(new IdentityHashMap<>());
        expectedSet.add(counted1);
        expectedSet.add(counted2);
        assertEquals(2, InstanceCounter.getInstances(Counted.class, TIMEOUT_FOR_NO_GC).size());
        assertEquals(expectedSet, InstanceCounter.getInstances(Counted.class, TIMEOUT_FOR_NO_GC)
                .stream().map(WeakReference::get).filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        expectedSet = null;
        counted1 = null;
        counted2 = null;
        System.gc();
        assertEquals(0, InstanceCounter.getInstanceCount(Counted.class, 0));
    }
}
