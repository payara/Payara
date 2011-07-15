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

package org.glassfish.admin.amx.base;

import java.util.List;
import java.util.Map;

import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;

import org.glassfish.admin.amx.monitoring.MonitoringRoot;
import org.glassfish.admin.amx.annotation.*;

import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.config.AMXConfigProxy;

import org.glassfish.admin.amx.core.PathnameConstants;
import org.glassfish.admin.amx.core.AMXMBeanMetadata;
import org.glassfish.admin.amx.logging.Logging;


import org.glassfish.external.amx.AMX;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.external.arc.Stability;


/**
The top-level interface for an appserver domain. Access to all other
{@link AMXProxy} begins here.
<p>
Not all children of DomainRoot have getter method; they could be added
dynamically.
<p>
The 'name' property in the ObjectName of DomainRoot is the name of the
appserver domain.  For example, appserver domains 'domain' and 'domain2' would
have ObjectNames for DomainRoot as follows:
<pre>
amx:type=DomainRoot:name=domain1
amx:type=DomainRoot:name=domain2
</pre>
Of course, these two MBeans would normally be found in different MBeanServers.
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true)
public interface DomainRoot extends AMXProxy
{
    public static final String PARENT_PATH = "";

    public static final String PATH = PARENT_PATH + PathnameConstants.SEPARATOR;

    @ManagedOperation(impact = MBeanOperationInfo.ACTION)
    @Description("Stop the domain immediately")
    public void stopDomain();

    /**
    Return the {@link Ext} MBean, parent of top-level utility and specialty MBeans.
     */
    @ManagedAttribute
    @Description("Get the primary extension point for AMX MBeans other than monitoring")
    public Ext getExt();

    /**
    Return the {@link Tools} MBean.
     */
    @ManagedAttribute
    @Description("Get the Tools MBean")
    public Tools getTools();

    /**
    @return the singleton {@link Query}.
     */
    @ManagedAttribute
    @Description("Get the Query MBean")
    public Query getQueryMgr();

    /**
    @return the singleton {@link Logging}.
     */
    @ManagedAttribute
    @Description("Get the Logging MBean")
    public Logging getLogging();

    /**
    @return the singleton {@link BulkAccess}.
     */
    @ManagedAttribute
    @Description("Get the BulkAccess MBean")
    public BulkAccess getBulkAccess();

    /**
    @return the singleton {@link Pathnames}.
     */
    @ManagedAttribute
    public Pathnames getPathnames();

    /**
    @return the singleton {@link Sample}.
     */
    @ManagedAttribute
    public Sample getSample();

    /**
    Return the name of this appserver domain.  Not to be confused with the
    JMX domain name, which may be derived from this name and is
    available from any ObjectName in AMX by calling
    {@link Util#getObjectName}

    The domain name is equivalent to the name of
    the directory containing the domain configuration.  This name
    is not part of the configuration and can only be changed by
    using a different directory to house the configuration for the
    domain.
    @return the name of the Appserver domain
     */
    @ManagedAttribute
    public String getAppserverDomainName();

    /**
    For module dependency reasons, the returned object must be cast to the appropriate type,
    as it cannot be used here.
    @return the JSR 77 J2EEDomain.
     */
    @ManagedAttribute
    public AMXProxy getJ2EEDomain();

    /**
    Get the DomainConfig.
    For module dependency reasons, the returned object must be converted (if desired)
    to DomainConfig using getDomain().as(DomainConfig.class).
    @return the singleton DomainConfig
     */
    @ManagedAttribute
    public AMXConfigProxy getDomain();

    /**
    @return the singleton {@link MonitoringRoot}. 
     */
    @ManagedAttribute
    @Description("Get the root MBean of all monitoring MBeans")
    public MonitoringRoot getMonitoringRoot();

    @ManagedAttribute
    @Description("Get the root MBean of all runtime MBeans")
    public RuntimeRoot getRuntime();

    /**
    @return the singleton SystemInfo
     */
    @ManagedAttribute
    public SystemInfo getSystemInfo();

    /**
    Notification type for JMX Notification issued when AMX MBeans are loaded
    and ready for use.
    @see #getAMXReady
     */
    public static final String AMX_READY_NOTIFICATION_TYPE =
            AMX.NOTIFICATION_PREFIX + "DomainRoot" + ".AMXReady";

    /**
    Poll to see if AMX is ready for use. It is more efficient to instead listen
    for a Notification of type {@link #AMX_READY_NOTIFICATION_TYPE}.  That
    should be done  by first registering the listener, then checking
    just after registration in case the Notification was issued in the ensuing
    interval just before the listener became registered.

    @return true if AMX is ready for use, false otherwise.
    @see #AMX_READY_NOTIFICATION_TYPE
     */
    @ManagedAttribute
    public boolean getAMXReady();

    /**
    Wait (block) until AMX is ready for use. Upon return, AMX is ready for use.
     */
    @ManagedOperation(impact = MBeanOperationInfo.INFO)
    public void waitAMXReady();

    /**
    @since Glassfish V3
     */
    @ManagedAttribute
    public String getDebugPort();

    /**
    @since Glassfish V3
     */
    @ManagedAttribute
    public String getApplicationServerFullVersion();

    /**
    @since Glassfish V3
     */
    @ManagedAttribute
    public String getInstanceRoot();

    /**
    @return the directory for the domain
    @since Glassfish V3
     */
    @ManagedAttribute
    public String getDomainDir();

    /**
    @return the configuration directory, typically 'config' subdirectory of {@link #getDomainDir}
    @since Glassfish V3
     */
    @ManagedAttribute
    public String getConfigDir();

    /**
    @return the installation directory
    @since Glassfish V3
     */
    @ManagedAttribute
    @Description("the installation directory")
    public String getInstallDir();

    /**
    Return the time the domain admin server has been running.
    uptime[0] contains the time in milliseconds.  uptime[1] contains a human-readable
    string describing the duration.
     */
    @ManagedAttribute
    @Description("Return the time the domain admin server has been running.  uptime[0] contains the time in milliseconds.  uptime[1] contains a human-readable string describing the duration.")
    public Object[] getUptimeMillis();
    
    /**
        Return a Map of all non-compliant MBeans (MBeans might no longer be registered).
        The List&lt;String> contains all issues with that MBean.
        @since Glassfish V3
     */
    @ManagedAttribute
    @Description("Return a Map of all non-compliant MBeans (MBeans might no longer be registered).  The List&lt;String> contains all issues with that MBean")
    public Map<ObjectName, List<String>> getComplianceFailures();
}














