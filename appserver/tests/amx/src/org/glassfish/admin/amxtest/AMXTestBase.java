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

package org.glassfish.admin.amxtest;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.BulkAccess;
import com.sun.appserv.management.base.NotificationServiceMgr;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.SystemInfo;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.client.AppserverConnectionSource;
import com.sun.appserv.management.client.ConnectionSource;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.ModuleMonitoringLevelValues;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.config.NodeAgentConfig;
import com.sun.appserv.management.config.ServerConfig;
import com.sun.appserv.management.config.ServersConfig;
import com.sun.appserv.management.j2ee.J2EEDomain;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.jmx.MBeanServerConnectionConnectionSource;
import com.sun.appserv.management.util.misc.ClassUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.TypeCast;
import com.sun.appserv.management.base.AMXDebugSupportMBean;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.AttributeList;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 Base class for testing the AMX API
 */
public class AMXTestBase
        extends JMXTestBase {
    private ConnectionSource mConnectionSource;
    private final DomainRoot mDomainRoot;
    private final ProxyFactory mProxyFactory;
    private final Set<ObjectName> mNotTested;
    private final TestUtil mTestUtil;

    /**
     config name for PE (non-EE) configuration
     */
    protected static final String PE_CONFIG_NAME = "server-config";

    private final static boolean WEB_MODULE_MONITOR_BROKEN = true;

    private static boolean MONITORING_ENABLED = false;

    protected static final String NEWLINE = System.getProperty("line.separator");


    public AMXTestBase() {
        checkConnection(getConnection());

        mNotTested = new HashSet<ObjectName>();
        try {
            mConnectionSource =
                    new MBeanServerConnectionConnectionSource(getConnection());

            mProxyFactory = ProxyFactory.getInstance(mConnectionSource, true);
            mDomainRoot = mProxyFactory.getDomainRoot();
        }
        catch (Exception e) {
            throw new RuntimeException(e.toString(), e);
        }

        mTestUtil = new TestUtil(getDomainRoot());
    }


        protected Map<String, ServerConfig>
    getServerConfigMap( final ServersConfig sc )
    {
        final Map<String,ServerConfig> all = new HashMap<String,ServerConfig>();

        all.putAll( sc.getStandaloneServerConfigMap() );
        all.putAll( sc.getClusteredServerConfigMap() );
        return all;
    }

    
    protected Set<AMX>
    getAllAMX() {
        return getTestUtil().getAllAMX();
    }

    protected ModuleMonitoringLevelsConfig
    getModuleMonitoringLevelsConfig(final String configName) {
        final ConfigConfig cc = configName == null ? getConfigConfig() : getConfigConfig(configName);
        assert (cc != null);

        final ModuleMonitoringLevelsConfig mon =
                cc.getMonitoringServiceConfig().getModuleMonitoringLevelsConfig();

        return (mon);
    }

    protected ModuleMonitoringLevelsConfig
    getModuleMonitoringLevelsConfig() {
        return getModuleMonitoringLevelsConfig(null);
    }


   private void changeAllLevels( final ModuleMonitoringLevelsConfig config, final String value )
   {
        final Set<String> names = ModuleMonitoringLevelsConfig.ALL_LEVEL_NAMES;
        
        final AttributeList attrs = new AttributeList();
        for( final String name : names )
        {
            attrs.add( new Attribute( name, value ) );
        }
        try {
            Util.getExtra(config).setAttributes( attrs );
        } catch ( Exception e ) {
           // ignore
        }
   }
   
    /**
     Ensure that monitoring is enabled so that unit tests don't miss anything
     */
    protected synchronized void
    setMonitoring( final String value ) {
        synchronized (AMXTestBase.class) {
            final String[] configNames = getConfigNames();
            for (int i = 0; i < configNames.length; ++i) {
                final ModuleMonitoringLevelsConfig mml =   getModuleMonitoringLevelsConfig(configNames[i] );
                changeAllLevels( mml, value );
            }
        }
    }

    protected void
    turnOffMonitoring() {
        setMonitoring( ModuleMonitoringLevelValues.OFF );
        MONITORING_ENABLED = false;
    }

    protected void
    turnOnMonitoring() {
        setMonitoring( ModuleMonitoringLevelValues.HIGH );
        MONITORING_ENABLED = true;
    }

    private static final Set<String> EXPECTED_REMOTE_INCOMPLETE_TYPES =
            GSetUtil.newUnmodifiableStringSet(XTypes.CALL_FLOW_MONITOR, XTypes.LOGGING);

    protected boolean
    isRemoteIncomplete(final ObjectName objectName) {
        final AMX amx = getProxyFactory().getProxy(objectName, AMX.class);
        final String j2eeType = amx.getJ2EEType();

        final boolean isRemote = !amx.isDAS();

        if (isRemote &&
                !EXPECTED_REMOTE_INCOMPLETE_TYPES.contains(j2eeType)) {
            warning("isRemoteIncomplete: not expecting j2eeType=" + j2eeType +
                    ", has the implementation changed?");
        }

        return isRemote;
    }


    protected boolean
    shouldTest(final ObjectName objectName) {
        boolean shouldTest = !isRemoteIncomplete(objectName);

        return (shouldTest);
    }

    protected void
    waitUnregistered(final ObjectName objectName)
            throws IOException {
        final MBeanServerConnection conn = getConnection();
        if (conn.isRegistered(objectName)) {
            mySleep(100);
        }

        while (conn.isRegistered(objectName)) {
            trace("waitUnregistered: " + objectName);
            mySleep(100);
        }
    }


    /**
     Because proxies are cleaned up asynchronously, it's possible for one
     to remain in the factory until the factory processes the Notification
     that it's MBean has been unregistered.
     */
    protected void
    waitProxyGone(
            final ProxyFactory factory,
            final ObjectName objectName) {
        long millis = 1;

        while (factory.getProxy(objectName, AMX.class, false) != null) {
            mySleep(millis);
            millis *= 2;
            trace("waitProxyGone: waiting for proxy to disappear: " + objectName);
        }
    }

    protected final void
    notTested(final ObjectName objectName) {
        if (isRemoteIncomplete(objectName)) {
            trace("remoteIncomplete (this is OK): " + objectName);
        } else {
            mNotTested.add(objectName);
        }
    }

    protected final Set<ObjectName>
    getNotTestedSet() {
        return (mNotTested);
    }

    public static void
    mySleep(final long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
        }
    }

    protected final void
    warnNotTested() {
        final Set<ObjectName> notTested = getNotTestedSet();

        if (notTested.size() != 0) {
            final Set j2eeTypes =
                    JMXUtil.getKeyPropertySet(AMX.J2EE_TYPE_KEY, notTested);

            trace("WARNING: DID NOT TEST: " + notTested.size() + " MBeans of types {" +
                    toString(j2eeTypes) + "}");
        }
    }


    protected void
    checkConnection(final MBeanServerConnection conn) {
        assert (getConnection() != null);

        try {
            conn.isRegistered(JMXUtil.getMBeanServerDelegateObjectName());
        }
        catch (Exception e) {
            fail("Connection failed:\n" +
                    ExceptionUtil.getStackTrace(getRootCause(e)));
        }
    }

    protected void
    checkConnection() {
        checkConnection(getConnection());
    }

    protected TestUtil
    getTestUtil() {
        return mTestUtil;
    }


    protected final AMX
    getProxy(final ObjectName objectName) {
        final ProxyFactory factory = ProxyFactory.getInstance(getConnectionSource(), true);

        final AMX proxy = factory.getProxy(objectName, AMX.class);

        return (proxy);
    }

    /**
     We don't have T extend AMX because not all mixin interfaces extend AMX.
     */
    protected final <T> T
    getProxy(
            final ObjectName objectName,
            final Class<T> theClass) {
        return (theClass.cast(getProxy(objectName)));
    }


    protected final DomainRoot
    getDomainRoot() {
        assert (mDomainRoot != null) : "mDomainRoot is null";
        return (mDomainRoot);
    }

    protected final DomainConfig
    getDomainConfig() {
        return (getDomainRoot().getDomainConfig());
    }

    protected final J2EEDomain
    getJ2EEDomain() {
        return (getDomainRoot().getJ2EEDomain());
    }

    protected String[]
    getConfigNames() {
        final Map<String, ConfigConfig> configMap =
                getDomainConfig().getConfigsConfig().getConfigConfigMap();

        return (GSetUtil.toStringArray(configMap.keySet()));
    }

    protected ConfigConfig
    getConfigConfig(final String name) {
        final Map<String, ConfigConfig> configs = getDomainConfig().getConfigsConfig().getConfigConfigMap();

        return configs.get(name == null ? PE_CONFIG_NAME : name);
    }

    protected static ConfigConfig
    getConfigConfig(final AMXConfig any) {
        final ObjectName objectName = Util.getObjectName(any);
        final String configName = objectName.getKeyProperty(XTypes.CONFIG_CONFIG);

        return any.getDomainRoot().getDomainConfig().getConfigsConfig().getConfigConfigMap().get(configName);
    }

    protected ConfigConfig
    getConfigConfig() {
        return (getConfigConfig(PE_CONFIG_NAME));
    }


    protected QueryMgr
    getQueryMgr() {
        assert (mDomainRoot != null);
        final QueryMgr proxy = getDomainRoot().getQueryMgr();
        assert (proxy != null);
        return (proxy);
    }


    protected NotificationServiceMgr
    getNotificationServiceMgr() {
        return (getDomainRoot().getNotificationServiceMgr());
    }

    protected BulkAccess
    getBulkAccess() {
        return (getDomainRoot().getBulkAccess());
    }

    protected ConnectionSource
    getConnectionSource() {
        assert (mConnectionSource != null);
        return (mConnectionSource);
    }

    protected MBeanServerConnection
    getConnection() {
        return (getGlobalMBeanServerConnection());
    }

    protected ProxyFactory
    getProxyFactory() {
        return (mProxyFactory);
    }


    protected Class
    getInterfaceClass(AMX proxy)
            throws ClassNotFoundException {
        final String name = Util.getExtra(proxy).getInterfaceName();
        return (ClassUtil.getClassFromName(name));
    }

    protected <T extends AMX> boolean
    testOnProxies(
            final Collection<T> proxies,
            final Method method)
            throws Exception {
        final long start = now();

        boolean failed = false;

        int testCount = 0;

        final Object[] args = new Object[1];
        for (final T proxy : proxies) {
            final ObjectName objectName = Util.getExtra(proxy).getObjectName();

            if (!shouldTest(objectName)) {
                notTested(objectName);
                continue;
            }

            ++testCount;
            try {
                args[0] = proxy;
                method.invoke(this, args);
            }
            catch (Exception e) {
                trace(method.getName() + " failed for proxy: " +
                        quote(JMXUtil.toString(objectName)));
                failed = true;
                trace(ExceptionUtil.toString(e));
            }
        }


        final long elapsed = now() - start;
        printVerbose("Ran test method " + method.getName() + " on " + testCount +
                " candidates in " + elapsed + "ms");
        warnNotTested();

        warnNotTested();

        return (!failed);
    }


    protected boolean
    testOnObjectNames(
            final Collection<ObjectName> objectNames,
            final Method method)
            throws Exception {
        boolean failed = false;

        final Object[] args = new Object[1];

        int testCount = 0;
        final long start = now();

        for (final ObjectName objectName : objectNames) {
            if (!shouldTest(objectName)) {
                notTested(objectName);
                continue;
            }

            ++testCount;
            try {
                args[0] = objectName;
                method.invoke(this, args);
            }
            catch (Exception e) {
                final Throwable rootCause = getRootCause(e);
                trace(method.getName() + " failed for: " +
                        quote(JMXUtil.toString(objectName)) + " with Exception of type " +
                        rootCause.getClass().getName() + ", msg = " + rootCause.getMessage());
                failed = true;
            }
        }

        final long elapsed = now() - start;
        printVerbose("Ran test method " + method.getName() + " on " + testCount +
                " candidates in " + elapsed + "ms");
        warnNotTested();

        return (!failed);
    }


    protected final static Class[] OBJECTNAME_SIG = new Class[]{ObjectName.class};
    protected final static Class[] PROXY_SIG = new Class[]{AMX.class};

    protected void
    testAll(
            final Collection<ObjectName> objectNames,
            final String methodName)
            throws Exception {
        final boolean success = testOnObjectNames(objectNames,
                                                  this.getClass().getMethod(methodName, OBJECTNAME_SIG));

        assert (success);
    }

    protected <T extends AMX> void
    testAllProxies(
            final Collection<T> proxies,
            final String methodName)
            throws Exception {
        final boolean success = testOnProxies(proxies,
                                              this.getClass().getMethod(methodName, PROXY_SIG));

        assert (success);
    }

    protected void
    testAll(String methodName)
            throws Exception {
        final Set<ObjectName> names = getTestUtil().getAllObjectNames();

        testAll(names, methodName);
    }

    public void
    setUp()
            throws Exception {
        super.setUp();

        turnOnMonitoring();
    }

    public void
    testAssertsOn() {
        checkAssertsOn();
    }

    private static final String DEFAULT_INSTANCE_NAME = "test";

    protected static String
    getDefaultInstanceName(final String qualifier) {
        String name = null;

        if (qualifier == null) {
            name = DEFAULT_INSTANCE_NAME;
        } else {
            name = qualifier + "." + DEFAULT_INSTANCE_NAME;
        }
        return name;
    }

    protected Throwable
    getRootCause(Throwable t) {
        return ExceptionUtil.getRootCause(t);
    }

    protected String
    getStackTrace(Throwable t) {
        return ExceptionUtil.getStackTrace(t);
    }

    protected String
    getRootCauseStackTrace(Throwable t) {
        return getStackTrace(getRootCause(t));
    }

    protected Map<String, AppserverConnectionSource>
    getNodeAgents() {
        final Map<?, ?> m = Map.class.cast(getEnvValue(PropertyKeys.NODE_AGENTS_KEY));
        return TypeCast.checkedMap(m,
                                   String.class, AppserverConnectionSource.class);
    }


    protected String
    getDASNodeAgentName() {
        return getEnvString(PropertyKeys.DAS_NODE_AGENT_NAME, null);
    }

    protected NodeAgentConfig
    getDASNodeAgentConfig() {
        final String name = getDASNodeAgentName();

        NodeAgentConfig config = null;
        if (name != null) {
            config = getDomainConfig().getNodeAgentsConfig().getNodeAgentConfigMap().get(name);
        }

        return config;
    }

    protected boolean
    getTestOffline() {
        return getEnvBoolean(PropertyKeys.TEST_OFFLINE_KEY, false);
    }

    /**
     Check if we're testing in Offline mode, which means that
     Config MBeans are loaded in-process. If so, issue a warning.

     @return true if test should be run, false if in offline mode
     */
    protected boolean
    checkNotOffline(final String testName) {
        boolean offline = getTestOffline();

        if (offline) {
            //warning( "amxtest.testOffline=true, skipping test " + testName + "()" );
        }

        return !offline;
    }


    public static Capabilities
    getDefaultCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    protected static Capabilities
    getOfflineCapableCapabilities(boolean offlineCapable) {
        final Capabilities c = new Capabilities();
        c.setOfflineCapable(offlineCapable);

        return c;
    }

    AMXDebugSupportMBean
    getAMXDebugSupportMBean() {
        final ObjectName objectName = Util.newObjectName(AMXDebugSupportMBean.OBJECT_NAME);

        try {
            return (AMXDebugSupportMBean) newProxy(objectName, AMXDebugSupportMBean.class);
        }
        catch (Exception e) {
            assert false : "Can't get proxy to " + objectName;
        }
        return null;
    }

    protected boolean
    supportsMultipleServers() {
        return supportsMultipleServers(getDomainRoot());
    }

    public static boolean
    supportsMultipleServers(final DomainRoot domainRoot) {
        return domainRoot.getSystemInfo().supportsFeature(SystemInfo.MULTIPLE_SERVERS_FEATURE);
    }


    protected boolean
    supportsClusters() {
        return getDomainRoot().getSystemInfo().supportsFeature(SystemInfo.CLUSTERS_FEATURE);
    }
}










