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

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/* jvm.operating-system */
// v2 mbean: com.sun.appserv:name=operating-system,type=operating-system,category=monitor,server=server
// v3 mbean: 
@AMXMetadata(type="operating-system-mon", group="monitoring")
@ManagedObject
@Description( "JVM Operating System Statistics" )
public class JVMOSStatsProvider {

    private OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    private StringStatisticImpl arch = new StringStatisticImpl("Architecture", "String",
                "Operating system architecture" );
    private CountStatisticImpl availableProcessors = new CountStatisticImpl(
            "AvailableProcessors", CountStatisticImpl.UNIT_COUNT,
                "Number of processors available to the Java virtual machine" );
    private StringStatisticImpl osName = new StringStatisticImpl("Name", "String",
                "Operating system name" );
    private StringStatisticImpl osVersion = new StringStatisticImpl("Version", "String",
                "operating system version" );
    //private CountStatisticImpl sysLoadAverage = new CountStatisticImpl(
    //        "SystemLoadAverage", CountStatisticImpl.UNIT_COUNT,
    //            "System load average for the last minute" );

    @ManagedAttribute(id="arch-current")
    @Description( "operating system architecture" )
    public StringStatistic getArch() {
        arch.setCurrent(osBean.getArch());
        return arch;
    }

    @ManagedAttribute(id="availableprocessors-count")
    @Description( "number of processors available to the Java virtual machine" )
    public CountStatistic getAvailableProcessors() {
        availableProcessors.setCount(osBean.getAvailableProcessors());
        return availableProcessors;
    }

    @ManagedAttribute(id="name-current")
    @Description( "operating system name" )
    public StringStatistic getOSName() {
        osName.setCurrent(osBean.getName());
        return osName;
    }

    @ManagedAttribute(id="version-current")
    @Description( "operating system version" )
    public StringStatistic getOSVersion() {
        osVersion.setCurrent(osBean.getVersion());
        return osVersion;
    }

    /*@ManagedAttribute(id="systemloadaverage-current")
    @Description( "system load average for the last minute" )
    public CountStatistic getSystemLoadAverage() {
        sysLoadAverage.setCurrent(osBean.getSystemLoadAverage());
        return sysLoadAverage;
    }*/
}
