/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.metrics.test;

import org.glassfish.elasticity.metric.TabularMetricEntry;
import org.glassfish.elasticity.util.TabularMetricHolder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for simple App.
 */
public class SimpleMetricTest {

    TabularMetricHolder<MemStat> table;

    @Before
    public void setup() {
        table = new TabularMetricHolder("mem", MemStat.class);
    }

    @Test
    public void testEmpty() {
        assert(table.isEmpty());
    }

    @Test
    public void testInsert() {
        for (int i=1; i<8; i++) {
            table.add(i*1000, new MemStat(i,i));
        }

        assert(! table.isEmpty());
    }

    @Test
    public void testInsert2() {
        for (int i=1; i<8; i++) {
            table.add(i*1000, new MemStat(i,i));
        }

        int count = 0;
        Iterator<TabularMetricEntry<MemStat>> iter = table.iterator(3, TimeUnit.SECONDS);
        while (iter.hasNext()) {
            count++;
            TabularMetricEntry<MemStat> entry = iter.next();
            //System.out.println("Data: " + entry.getTimestamp() + " : " + entry.getV());
        }

        assert (count == 4);
    }

    @Test
    public void testInsert3() {
        for (int i=1; i<8; i++) {
            table.add(i*1000, new MemStat(i,i));
        }

        Iterator<TabularMetricEntry<MemStat>> iter = table.iterator(3, TimeUnit.SECONDS);
        while (iter.hasNext()) {
            TabularMetricEntry<MemStat> entry = iter.next();
            //System.out.println("Data: " + entry.getTimestamp() + " : " + entry.getV());
        }

        for (int i=10; i<18; i++) {
            table.add(i*1000, new MemStat(i,i));
        }

        int count = 0;
        Iterator<TabularMetricEntry<MemStat>> iter2 = table.iterator(5, TimeUnit.SECONDS);
        while (iter2.hasNext()) {
            count++;
            TabularMetricEntry<MemStat> entry = iter2.next();
            System.out.println("Data: " + entry.getTimestamp() + " : " + entry.getV());
        }

        assert(count == 6);
    }
    
    private static class MemStat {
        public int v1;
        public int v2;

        private MemStat(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
        
        public String toString() {
            return v1 + " : " + v2;
        }
    }
}
