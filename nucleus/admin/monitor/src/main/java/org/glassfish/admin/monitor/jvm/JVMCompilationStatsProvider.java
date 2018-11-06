/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package org.glassfish.admin.monitor.jvm;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Provides the MBean for JVM compilation statistics
 * <p>
 * The MBean will be of the format
 * {@code amx:pp=/mon/server-mon[server],type=compilation-system-mon,name=jvm/compilation-system}
 * and can be enabled by turning the Jvm monitoring level in the admin console to LOW
 * @since v2
 */
@AMXMetadata(type="compilation-system-mon", group="monitoring")
@ManagedObject
@Description( "JVM Compilation Statistics" )
public class JVMCompilationStatsProvider {

    private final CompilationMXBean compBean = ManagementFactory.getCompilationMXBean();

    private final StringStatisticImpl compilerName = new StringStatisticImpl("Name", "String",
                "Name of the Just-in-time (JIT) compiler" );
    private final CountStatisticImpl totalCompilationTime = new CountStatisticImpl(
            "TotalCompilationTime", CountStatisticImpl.UNIT_MILLISECOND,
                "Approximate accumlated elapsed time (in milliseconds) spent in compilation" );

    /**
     * Gets the name of the Just-in-time (JIT) compiler
     * @return a {@link StringStatistic} with the compiler name
     */
    @ManagedAttribute(id="name-current")
    @Description( "name of the Just-in-time (JIT) compiler" )
    public StringStatistic getCompilerName() {
        compilerName.setCurrent(compBean.getName());
        return compilerName;
    }

    /**
     * Gets the approximate accumulated elapsed time spent in compilation
     * @return a {@link StringStatistic} with the elapsed time in milliseconds
     */
    @ManagedAttribute(id="totalcompilationtime-current")
    @Description( "approximate accumlated elapsed time (in milliseconds) spent in compilation" )
    public CountStatistic getTotalCompilationTime() {
        totalCompilationTime.setCount(compBean.getTotalCompilationTime());
        return totalCompilationTime;
    }
}
