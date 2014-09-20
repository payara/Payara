/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.util.i18n.StringManager;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 */
class MemoryReporter {
    private final MBeanServerConnection mbsc;
    private RuntimeMXBean rmbean;
    private MemoryMXBean mmbean;
    private List<MemoryPoolMXBean> pools;
    private List<GarbageCollectorMXBean> gcmbeans;
    private final static StringManager sm = StringManager.getManager(MemoryReporter.class);
    
    public MemoryReporter(final MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }
    public String getMemoryReport() {
        init();
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        sb.append(getMemoryPoolReport());
        sb.append(getGarbageCollectionReport());
        sb.append(getMemoryMXBeanReport());
        return ( sb.toString() );
    }
    
    private void init() throws RuntimeException {
        try {
            this.rmbean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                                 ManagementFactory.RUNTIME_MXBEAN_NAME,
                                                 RuntimeMXBean.class);
            this.mmbean = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                                 ManagementFactory.MEMORY_MXBEAN_NAME,
                                                 MemoryMXBean.class);
            ObjectName poolName = new ObjectName(ManagementFactory.MEMORY_POOL_MXBEAN_DOMAIN_TYPE+",*");;
            ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE+",*");
            Set mbeans = mbsc.queryNames(poolName, null);
            if (mbeans != null) {
                pools = new ArrayList<MemoryPoolMXBean>();
                Iterator iterator = mbeans.iterator();
                MemoryPoolMXBean p = null;
                    while (iterator.hasNext()) {
                        ObjectName objName = (ObjectName) iterator.next();
                        p = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                                   objName.getCanonicalName(),
                                                   MemoryPoolMXBean.class);
                        pools.add(p);
                }
            }
            mbeans = mbsc.queryNames(gcName, null);
            if (mbeans != null) {
                gcmbeans = new ArrayList<GarbageCollectorMXBean>();
                Iterator iterator = mbeans.iterator();
                GarbageCollectorMXBean gc = null;
                while (iterator.hasNext()) {
                    ObjectName objName = (ObjectName) iterator.next();
                    gc = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                                               objName.getCanonicalName(),
                                               GarbageCollectorMXBean.class);
                    gcmbeans.add(gc);
                }
            }        
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private String getMemoryPoolReport() {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        final long millis = rmbean.getUptime();
        final String uptime = sm.getString("uptime", JVMInformationCollector.millis2HoursMinutesSeconds(millis));
        sb.append(uptime);
        for (final MemoryPoolMXBean m : pools) {
            final String n = m.getName();
            sb.append(sm.getString("memory.pool.name", n));
            MemoryUsage mu = m.getUsage();
            sb.append(mu2String(mu));
        }
        return ( sb.toString() );
    }
    private String mu2String(final MemoryUsage mu) {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        final String init = JVMInformationCollector.formatLong(mu.getInit());
        sb.append(sm.getString("memory.usage.init", init));
        final String comm = JVMInformationCollector.formatLong(mu.getCommitted());
        sb.append(sm.getString("memory.usage.comm", comm));
        final String max  = JVMInformationCollector.formatLong(mu.getMax());
        sb.append(sm.getString("memory.usage.max", max));
        final String used = JVMInformationCollector.formatLong(mu.getUsed());
        sb.append(sm.getString("memory.usage.used", used));        
        return ( sb.toString() );
    }
    private String getGarbageCollectionReport() {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        for (final GarbageCollectorMXBean m : gcmbeans) {
            final String name = sm.getString("gc.name", m.getName());
            sb.append(name);
            final String cc = sm.getString("gc.numcol", JVMInformationCollector.formatLong(m.getCollectionCount()));
            sb.append(cc);
            final String gct = sm.getString("gc.coltime", JVMInformationCollector.millis2SecondsMillis(m.getCollectionTime()));
            sb.append(gct);
        }
        return ( sb.toString() );
    }
    private String getMemoryMXBeanReport() {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        sb.append(sm.getString("heap.mem.usage"));
        sb.append(mu2String(mmbean.getHeapMemoryUsage()));
        sb.append(sm.getString("nonheap.mem.usage"));
        sb.append(mu2String(mmbean.getNonHeapMemoryUsage()));
        sb.append(sm.getString("obj.fin.pending", mmbean.getObjectPendingFinalizationCount()));
        return ( sb.toString() );
    }
}
