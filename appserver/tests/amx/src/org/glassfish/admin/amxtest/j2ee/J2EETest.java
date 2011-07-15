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

package org.glassfish.admin.amxtest.j2ee;

import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.ClusterConfig;
import com.sun.appserv.management.config.ServerConfig;
import com.sun.appserv.management.j2ee.EventProvider;
import com.sun.appserv.management.j2ee.J2EECluster;
import com.sun.appserv.management.j2ee.J2EEManagedObject;
import com.sun.appserv.management.j2ee.J2EEServer;
import com.sun.appserv.management.j2ee.J2EETypes;
import com.sun.appserv.management.j2ee.JVM;
import com.sun.appserv.management.j2ee.StateManageable;
import com.sun.appserv.management.monitor.Monitoring;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.MapUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.ObjectName;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 */
public final class J2EETest
        extends AMXTestBase {
    public J2EETest()
            throws IOException {
        turnOnMonitoring();
    }

    private static boolean FAILURES_WARNED = false;

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(false);
    }
    
    /**
     Verify that there is one J2EEServer for each ServerConfig (standalone or not)
     */
    public void
    testJ2EEServerMatchesServerConfig() {
        final Map<String, ServerConfig> serverConfigMap = getServerConfigMap( getDomainConfig().getServersConfig() );

        final Map<String, J2EEServer> j2eeServerMap =
                getDomainRoot().getJ2EEDomain().getJ2EEServerMap();

        assert (serverConfigMap.keySet().equals(j2eeServerMap.keySet())) :
                "ServerConfig names do not match J2EEServer names, ServerConfig names = " +
                        toString(serverConfigMap.keySet()) + ", J2EEServer names = " +
                        toString(j2eeServerMap.keySet());

    }


    /**
     Verify that there is one J2EEServer for each ServerConfig (standalone or not)
     */
    public void
    testJ2EEClusterMatchesClusterConfig() {
        final Map<String, ClusterConfig> clusterConfigMap = getDomainConfig().getClustersConfig().getClusterConfigMap();
        final Map<String, J2EECluster> j2eeClusterMap = getJ2EEDomain().getJ2EEClusterMap();

        assert (clusterConfigMap.keySet().equals(j2eeClusterMap.keySet())) :
                "ClusterConfig names do not match J2EECluster names, ClusterConfig names = " +
                        toString(clusterConfigMap.keySet()) + ", J2EECluster names = " +
                        toString(j2eeClusterMap.keySet());
    }

    public void
    testJVMs() {
        final QueryMgr queryMgr = getQueryMgr();

        final Set jvms = queryMgr.queryJ2EETypeSet(J2EETypes.JVM);
        final Iterator iter = jvms.iterator();

        String lastVendor = null;
        String lastVersion = null;
        while (iter.hasNext()) {
            final JVM jvm = (JVM) iter.next();

            // the ObjectName of the Node must match the String version for "node"
            assert (jvm.getnode() != null);

            // the JVMs should all have the same vendor (presumably)
            assert (jvm.getjavaVendor() != null);
            if (lastVendor == null) {
                lastVendor = jvm.getjavaVendor();
            } else {
                assert (lastVendor.equals(jvm.getjavaVendor()));
            }

            // the JVMs should all have the same version (presumably)
            assert (jvm.getjavaVersion() != null);
            if (lastVersion == null) {
                lastVersion = jvm.getjavaVersion();
            } else {
                assert (lastVersion.equals(jvm.getjavaVersion()));
            }
        }
    }

    private boolean
    appearsToBeDefaultWebModule(final String webModuleName) {
        return webModuleName.startsWith("//") && webModuleName.endsWith("/");
    }

    /**
     Map from JSR77 j2eeType to our config j2eeType.
     */
    private static final Map<String, String> ToConfigMap = MapUtil.newMap(new String[]
            {
                    J2EETypes.J2EE_DOMAIN, XTypes.DOMAIN_CONFIG,
                    J2EETypes.J2EE_CLUSTER, XTypes.CLUSTER_CONFIG,
                    J2EETypes.J2EE_SERVER, XTypes.STANDALONE_SERVER_CONFIG,
                    J2EETypes.JVM, XTypes.JAVA_CONFIG,

                    J2EETypes.J2EE_APPLICATION, XTypes.J2EE_APPLICATION_CONFIG,
                    J2EETypes.EJB_MODULE, XTypes.EJB_MODULE_CONFIG,
                    J2EETypes.WEB_MODULE, XTypes.WEB_MODULE_CONFIG,
                    J2EETypes.APP_CLIENT_MODULE, XTypes.APP_CLIENT_MODULE_CONFIG,

                    J2EETypes.JAVA_MAIL_RESOURCE, XTypes.MAIL_RESOURCE_CONFIG,
                    J2EETypes.JDBC_RESOURCE, XTypes.JDBC_RESOURCE_CONFIG,
                    J2EETypes.JNDI_RESOURCE, XTypes.JNDI_RESOURCE_CONFIG,
                    J2EETypes.WEB_SERVICE_ENDPOINT, XTypes.WEB_SERVICE_ENDPOINT_CONFIG,
            }
    );

    private static String
    getConfigPeerJ2EEType(final String j2eeType) {
        return ToConfigMap.get(j2eeType);
    }

    /**
     Maps a j2eeType to its peer monitoring j2eeType
     */
    private static final Map<String, String> ToMonitorMap =
            Collections.unmodifiableMap(MapUtil.newMap(new String[]
                    {
                            J2EETypes.J2EE_SERVER, XTypes.SERVER_ROOT_MONITOR,
                            J2EETypes.J2EE_APPLICATION, XTypes.APPLICATION_MONITOR,

                            J2EETypes.WEB_MODULE, XTypes.WEB_MODULE_VIRTUAL_SERVER_MONITOR,
                            J2EETypes.SERVLET, XTypes.SERVLET_MONITOR,

                            J2EETypes.EJB_MODULE, XTypes.EJB_MODULE_MONITOR,
                            J2EETypes.STATELESS_SESSION_BEAN, XTypes.STATELESS_SESSION_BEAN_MONITOR,
                            J2EETypes.STATEFUL_SESSION_BEAN, XTypes.STATEFUL_SESSION_BEAN_MONITOR,
                            J2EETypes.ENTITY_BEAN, XTypes.ENTITY_BEAN_MONITOR,
                            J2EETypes.MESSAGE_DRIVEN_BEAN, XTypes.MESSAGE_DRIVEN_BEAN_MONITOR,
                    }));

    // has a monitoring peer, but no Stats
    private static final Set<String> HasNoStats =
            GSetUtil.newUnmodifiableStringSet(J2EETypes.J2EE_SERVER);


    protected String
    getMonitoringPeerJ2EEType(final String j2eeType) {
        return ToMonitorMap.get(j2eeType);
    }

    protected String
    getMonitoringPeerProps(final J2EEManagedObject item) {
        final String j2eeType = item.getJ2EEType();
        final String monitoringPeerJ2EEType = getMonitoringPeerJ2EEType(j2eeType);
        final ObjectName objectName = Util.getObjectName(item);

        String props = null;
        if (monitoringPeerJ2EEType != null) {
            props = Util.makeRequiredProps(monitoringPeerJ2EEType, item.getName());

            for (final String propKey : ToMonitorMap.keySet()) {
                final String value = objectName.getKeyProperty(propKey);
                if (value != null) {
                    final String prop =
                            Util.makeProp(getMonitoringPeerJ2EEType(propKey), value);
                    props = Util.concatenateProps(props, prop);
                }
            }
        }

        return props;
    }

    public void
    testJ2EE()
            throws ClassNotFoundException {
        final QueryMgr queryMgr = getQueryMgr();

        final Set<J2EEManagedObject> j2eeAll =
                queryMgr.queryInterfaceSet(J2EEManagedObject.class.getName(), null);

        final Set<ObjectName> failedSet = new HashSet<ObjectName>();
        final Set<ObjectName> noPeerSet = new HashSet<ObjectName>();

        for (final J2EEManagedObject item : j2eeAll) {
            final ObjectName objectName = Util.getObjectName(item);
            assert (objectName.equals(Util.newObjectName(item.getobjectName())));

            final String j2eeType = item.getJ2EEType();

            if (item.isstateManageable()) {
                assert (item instanceof StateManageable);

                final StateManageable sm = (StateManageable) item;

                final int state = sm.getstate();
                assert (
                        state == StateManageable.STATE_STARTING ||
                                state == StateManageable.STATE_RUNNING ||
                                state == StateManageable.STATE_STOPPING ||
                                state == StateManageable.STATE_STOPPED ||
                                state == StateManageable.STATE_FAILED);

                if (state == StateManageable.STATE_RUNNING) {
                    try {
                        final long startTime = sm.getstartTime();

                        // assume it was started less than 30 days ago
                        final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;
                        final long days30 = 30L * MILLIS_PER_DAY;
                        if (startTime < now() - days30) {
                            warning("MBean " + quote(objectName) +
                                    " claims a start time of " + new Date(startTime) + ", which is more than 30 days prior to now = " +
                                    new Date(now()));
                            failedSet.add(objectName);
                        }
                    }
                    catch (Exception e) {
                        final Throwable rootCause = ExceptionUtil.getRootCause(e);
                        warning("MBean " + quote(objectName) +
                                " is 'stateManageable' and in 'STATE_RUNNING', but could not supply Attribute 'startTime', " +
                                "threw an exception of class " +
                                rootCause.getClass().getName());
                        failedSet.add(objectName);
                    }
                }
            }

            if (item.iseventProvider()) {
                assert (item instanceof EventProvider);

                final EventProvider ep = (EventProvider) item;
                final String[] types = ep.gettypes();
                assert types != null :
                        "Item claims to be EventProvider, but provides null 'types': " +
                                toString(objectName);
            }

            /*
                    monitoring was enabled so monitoring peers should exist
                    Can't just call isStatisticProvider(), since it will be false
                    if the monitoring peer is null (correctly or incorrectly).
                */
            final String monitoringPeerJ2EEType = getMonitoringPeerJ2EEType(j2eeType);
            final Monitoring monitoringPeer = item.getMonitoringPeer();
            if (monitoringPeerJ2EEType != null) {
                // See if there actually is a monitoring peer, but null is being returned.
                if (monitoringPeer == null) {
                    final String props = getMonitoringPeerProps(item);
                    final Set<Monitoring> monitors = getQueryMgr().queryPropsSet(props);
                    if (monitors.size() != 0) {
                        warning("MBean " + quote(objectName) +
                                " returned null for its monitoring peer, but found the following:" +
                                NEWLINE +
                                CollectionUtil.toString(Util.toObjectNames(monitors), NEWLINE));

                        failedSet.add(objectName);
                    }
                } else {
                    // we have a monitoring peer, verify that it states that it has
                    // statistics
                    if (!HasNoStats.contains(j2eeType)) {
                        assert item.isstatisticProvider() && item.isstatisticsProvider();
                    }
                }
            } else {
                // it has a monitoring peer
                if (item.isstatisticProvider() || item.isstatisticsProvider()) {
                    warning("MBean " + quote(objectName) +
                            " should not have its statisticProvider set to true");
                    failedSet.add(objectName);
                }
            }


            if (item.isConfigProvider()) {
                final AMXConfig config = item.getConfigPeer();

                if (config == null) {
                    // Some auto-generated items do not have config.  See if it's there
                    final String props = Util.makeRequiredProps(
                            getConfigPeerJ2EEType(j2eeType), item.getName());

                    if (getQueryMgr().queryPropsSet(props).size() != 0) {
                        warning("MBean " + quote(objectName) +
                                " has existing config peer, but returned null");
                        failedSet.add(objectName);
                    }
                }
            }
        }

        if (noPeerSet.size() != 0) {
            warning("The following MBeans do not have a Monitoring peer:" +
                    NEWLINE + toString(noPeerSet));

        }

        if (failedSet.size() != 0) {
            failure("Failures in the following " + failedSet.size() + " MBeans:\n" +
                    toString(failedSet) );
		}
	}
}



















