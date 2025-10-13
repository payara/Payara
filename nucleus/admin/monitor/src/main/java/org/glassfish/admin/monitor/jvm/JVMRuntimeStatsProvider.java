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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates

package org.glassfish.admin.monitor.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Class providing the MBean for JVM runtime statistics
 * <p>
 * The MBean will be of the format
 * {@code amx:pp=/mon/server-mon[server],type=runtime-mon,name=jvm/runtime}
 * and can be enabled by turning the Jvm monitoring level in the admin console to LOW
 * @since v2
 */
@AMXMetadata(type="runtime-mon", group="monitoring")
@ManagedObject
@Description( "JVM Runtime Statistics" )
public class JVMRuntimeStatsProvider {
    
    private final RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();

    private final StringStatisticImpl bootClassPath = new StringStatisticImpl("BootClassPath", "String",
                "Boot class path that is used by the bootstrap class loader to search for class files" );
    private final StringStatisticImpl classPath = new StringStatisticImpl("ClassPath", "String",
                "Java class path that is used by the system class loader to search for class files" );
    private final StringStatisticImpl inputArguments = new StringStatisticImpl("InputArguments", "String",
                "Input arguments passed to the Java virtual machine which does not include the arguments to the main method" );
    private final StringStatisticImpl libraryPath = new StringStatisticImpl("LibraryPath", "String",
                "Java library path" );
    private final StringStatisticImpl mgmtSpecVersion = new StringStatisticImpl("ManagementSpecVersion", "String",
                "Version of the specification for the management interface implemented by the running Java virtual machine" );
    private final StringStatisticImpl runtimeName = new StringStatisticImpl("Name", "String",
                "Name representing the running Java virtual machine" );
    private final StringStatisticImpl specName = new StringStatisticImpl("SpecName", "String",
                "Java virtual machine specification name" );
    private final StringStatisticImpl specVendor = new StringStatisticImpl("SpecVendor", "String",
                "Java virtual machine specification vendor" );
    private final StringStatisticImpl specVersion = new StringStatisticImpl("SpecVersion", "String",
                "Java virtual machine specification version" );
    private CountStatisticImpl uptime = new CountStatisticImpl("Uptime", CountStatisticImpl.UNIT_MILLISECOND,
            "Uptime of the Java virtual machine in milliseconds");
    private final StringStatisticImpl vmName = new StringStatisticImpl("VmName", "String",
                "Java virtual machine implementation name" );
    private final StringStatisticImpl vmVendor = new StringStatisticImpl("VmVendor", "String",
                "Java virtual machine implementation vendor" );
    private final StringStatisticImpl vmVersion = new StringStatisticImpl("VmVersion", "String",
                "Java virtual machine implementation version" );

    
    /**
     * Gets the boot class path that is used by the bootstrap class loader to search for class files
     * @return a {@link StringStatistic} with the boot classpath
     */
    @ManagedAttribute(id="bootclasspath-current")
    @Description( "boot class path that is used by the bootstrap class loader to search for class files" )
    public StringStatistic getBootClassPath() {
        if (rtBean.isBootClassPathSupported()) {
            bootClassPath.setCurrent(rtBean.getBootClassPath());
        }
        return bootClassPath;
    }

    /**
     * Gets the Java class path that is used by the system class loader to search for class files
     * @return a {@link StringStatistic} with the system classpath
     */
    @ManagedAttribute(id="classpath-current")
    @Description( "Java class path that is used by the system class loader to search for class files" )
    public StringStatistic getClassPath() {
        classPath.setCurrent(rtBean.getClassPath());
        return classPath;
    }

    /**
     * Gets the input arguments passed to the Java virtual machine.
     * @return a {@link StringStatistic} with a comma separated list of input arguments. This does not include arguments
     * to the main method.
     */
    @ManagedAttribute(id="inputarguments-current")
    @Description( "input arguments passed to the Java virtual machine which does not include the arguments to the main method" )
    public StringStatistic getInputArguments() {
        List<String> inputList = rtBean.getInputArguments();
        StringBuilder sb = new StringBuilder();
        for (String arg : inputList) {
            sb.append(arg);
            sb.append(", ");
        }
        String finalString = sb.substring(0, sb.lastIndexOf(","));
        inputArguments.setCurrent(finalString);
        return inputArguments;
    }

    /**
     * Gets the Java library path
     * @return a {@link StringStatistic} with the lib path
     */
    @ManagedAttribute(id="librarypath-current")
    @Description( "Java library path" )
    public StringStatistic getLibraryPath() {
        libraryPath.setCurrent(rtBean.getLibraryPath());
        return libraryPath;
    }

    /**
     * Gets the version of the specification for the management interface implemented by the running Java virtual machine
     * @return a {@link StringStatistic} with the specification version. This is version 1.2 in Payara.
     */
    @ManagedAttribute(id="managementspecversion-current")
    @Description( "version of the specification for the management interface implemented by the running Java virtual machine" )
    public StringStatistic getManagementSpecVersion() {
        mgmtSpecVersion.setCurrent(rtBean.getManagementSpecVersion());
        return mgmtSpecVersion;
    }

    /**
     * Gets the name representing the running Java virtual machine
     * @return a {@link StringStatistic} with the name of the machine
     */
    @ManagedAttribute(id="name-current")
    @Description( "name representing the running Java virtual machine" )
    public StringStatistic getRuntimeName() {
        runtimeName.setCurrent(rtBean.getName());
        return runtimeName;
    }

    /**
     * Gets the Java virtual machine specification name
     * @return a {@link StringStatistic} with the name of the specification.
     * This is normally {@code Java Virtual Machine Specification}
     */
    @ManagedAttribute(id="specname-current")
    @Description( "Java virtual machine specification name" )
    public StringStatistic getSpecName() {
        specName.setCurrent(rtBean.getSpecName());
        return specName;
    }

    /**
     * Gets the Java virtual machine specification vendor
     * @return a {@link StringStatistic} with the name of the vendor
     */
    @ManagedAttribute(id="specvendor-current")
    @Description( "Java virtual machine specification vendor" )
    public StringStatistic getSpecVendor() {
        specVendor.setCurrent(rtBean.getSpecVendor());
        return specVendor;
    }

    /**
     * Gets the Java virtual machine specification version
     * @return a {@link StringStatistic} with the specification version
     */
    @ManagedAttribute(id="specversion-current")
    @Description( "Java virtual machine specification version" )
    public StringStatistic getSpecVersion() {
        specVersion.setCurrent(rtBean.getSpecVersion());
        return specVersion;
    }

    /**
     * Gets the uptime of the Java virtual machine
     * @return a {@link CountStatistic} with the uptime in milliseconds
     */
    @ManagedAttribute(id="uptime-count")
    @Description( "uptime of the Java virtual machine in milliseconds" )
    public CountStatistic getUptime() {
        uptime.setCount(rtBean.getUptime());
        return uptime;
    }

    /**
     * Gets the Java virtual machine implementation name
     * @return a {@link StringStatistic} with the implementation name
     */
    @ManagedAttribute(id="vmname-current")
    @Description( "Java virtual machine implementation name" )
    public StringStatistic getVmName() {
        vmName.setCurrent(rtBean.getVmName());
        return vmName;
    }

    /**
     * Gets the Java virtual machine implementation vendor
     * @return a {@link StringStatistic} with the name of the vendor
     */
    @ManagedAttribute(id="vmvendor-current")
    @Description( "Java virtual machine implementation vendor" )
    public StringStatistic getVmVendor() {
        vmVendor.setCurrent(rtBean.getVmVendor());
        return vmVendor;
    }

    /**
     * Gets the Java virtual machine implementation version
     * @return a {@link StringStatistic} with the implementation version
     */
    @ManagedAttribute(id="vmversion-current")
    @Description( "Java virtual machine implementation version" )
    public StringStatistic getVmVersion() {
        vmVersion.setCurrent(rtBean.getVmVersion());
        return vmVersion;
    }   
}
