/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.amx.core.proxy;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.util.AMXDebugHelper;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

import javax.management.*;
import javax.management.relation.MBeanServerNotificationFilter;
import javax.management.remote.JMXConnectionNotification;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.glassfish.external.amx.AMX.DESC_STD_IMMUTABLE_INFO;
import static org.glassfish.external.amx.AMX.NAME_KEY;

//import org.glassfish.api.amx.AMXUtil;
/**
 * @deprecated Factory for {@link AMXProxy} proxies.
 */
@Deprecated
@Taxonomy(stability = Stability.UNCOMMITTED)
public final class ProxyFactory implements NotificationListener {

    private final MBeanServerConnection mMBeanServerConnection;
    private final String mMBeanServerID;
    private final ObjectName mDomainRootObjectName;
    private final DomainRoot mDomainRoot;
    /**
     * For immutable MBeanInfo, we want to pay the cost once and only once of a
     * trip to the server. <p> Can we assume it's unique per *type* so that we
     * can cache it once per type? If we could so so, the size of the cache
     * would stay much smaller.
     */
    private final ConcurrentMap<ObjectName, MBeanInfo> mMBeanInfoCache = new ConcurrentHashMap<ObjectName, MBeanInfo>();
    private static final AMXDebugHelper mDebug =
            new AMXDebugHelper(ProxyFactory.class.getName());

    private static void debug(final Object... args) {
        //mDebug.println( args );
        System.out.println(StringUtil.toString(", ", args));
    }
    private static final Map<MBeanServerConnection, ProxyFactory> INSTANCES =
            Collections.synchronizedMap(new HashMap<MBeanServerConnection, ProxyFactory>());

    /**
     * Because ProxyFactory is used on both client and server, emitting anything
     * to stdout or to the log is unacceptable in some circumstances. Warnings
     * remain available if the AMX-DEBUG system property allows it.
     */
    private static void warning(final Object... args) {
        debug(args);
    }

    private ProxyFactory(final MBeanServerConnection conn) {
        mDebug.setEchoToStdOut(true);
        assert (conn != null);

        mMBeanServerConnection = conn;

        try {
            mMBeanServerID = JMXUtil.getMBeanServerID(conn);

            mDomainRootObjectName = AMXGlassfish.DEFAULT.domainRoot();
            if (mDomainRootObjectName == null) {
                throw new IllegalStateException("ProxyFactory: AMX has not been started");
            }
            mDomainRoot = getProxy(mDomainRootObjectName, DomainRoot.class);

            // we should always be able to listen to MBeans--
            // but the http connector does not support listeners
            try {
                final MBeanServerNotificationFilter filter = new MBeanServerNotificationFilter();
                filter.enableAllObjectNames();
                filter.disableAllTypes();
                filter.enableType(MBeanServerNotification.UNREGISTRATION_NOTIFICATION);
                JMXUtil.listenToMBeanServerDelegate(conn, this, filter, null);
            } catch (Exception e) {
                warning("ProxyFactory: connection does not support notifications: ",
                        mMBeanServerID, conn);
            }
        } catch (Exception e) {
            warning("ProxyFactory.ProxyFactory:\n", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * The connection is bad. Tell each proxy its gone and remove it.
     */
    private void connectionBad() {
        final Set<AMXProxy> proxies = new HashSet<AMXProxy>();

        for (final AMXProxy amx : proxies) {
            final AMXProxyHandler proxy = AMXProxyHandler.unwrap(amx);
            proxy.connectionBad();
        }
    }

    /**
     * Verify that the connection is still alive.
     */
    public boolean checkConnection() {
        boolean connectionGood = true;

        try {
            getMBeanServerConnection().isRegistered(JMXUtil.getMBeanServerDelegateObjectName());
            connectionGood = true;
        } catch (Exception e) {
            connectionBad();
        }

        return (connectionGood);
    }

    void notifsLost() {
        // should probably check each proxy for validity, but not clear if it's important...
    }

    /**
     * Listens for MBeanServerNotification.UNREGISTRATION_NOTIFICATION and
     * JMXConnectionNotification and takes appropriate action. <br> Used
     * internally as callback for {@link javax.management.NotificationListener}.
     * <b>DO NOT CALL THIS METHOD</b>.
     */
    public void handleNotification(
            final Notification notifIn,
            final Object handback) {
        final String type = notifIn.getType();

        if (type.equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION)) {
            // do nothing
        } else if (notifIn instanceof JMXConnectionNotification) {
            if (type.equals(JMXConnectionNotification.CLOSED)
                    || type.equals(JMXConnectionNotification.FAILED)) {
                debug("ProxyFactory.handleNotification: connection closed or failed: ", notifIn);
                connectionBad();
            } else if (type.equals(JMXConnectionNotification.NOTIFS_LOST)) {
                debug("ProxyFactory.handleNotification: notifications lost: ", notifIn);
                notifsLost();
            }
        } else {
            debug("ProxyFactory.handleNotification: UNKNOWN notification: ", notifIn);
        }
    }
    private final static String DOMAIN_ROOT_KEY = "DomainRoot";

    public DomainRoot createDomainRoot()
            throws IOException {
        return (mDomainRoot);
    }

    public DomainRoot initDomainRoot()
            throws IOException {
        final ObjectName domainRootObjectName = getDomainRootObjectName();

        final DomainRoot dr = getProxy(domainRootObjectName, DomainRoot.class);

        return (dr);
    }

    /**
     * Return the ObjectName for the DomainMBean.
     */
    public ObjectName getDomainRootObjectName() {
        return (mDomainRootObjectName);
    }

    /**
     * Return the DomainRoot. AMX is guaranteed to be ready after this call
     * returns.
     *
     * @return the DomainRoot for this factory.
     */
    public DomainRoot getDomainRootProxy() {
        return getDomainRootProxy(false);
    }

    /**
     * If 'waitReady' is true, then upon return AMX is guaranteed to be fully
     * loaded. Otherwise AMX MBeans may continue to initialize asynchronously.
     *
     * @param waitReady
     * @return the DomainRoot for this factory.
     */
    public DomainRoot getDomainRootProxy(boolean waitReady) {
        if (waitReady) {
            mDomainRoot.waitAMXReady();
        }

        return (mDomainRoot);
    }

    /**
     * @return the JMX MBeanServerID for the MBeanServer in which MBeans reside.
     */
    public String getMBeanServerID() {
        return (mMBeanServerID);
    }

    /**
     * Get an instance of the ProxyFactory for the MBeanServer. Generally not
     * applicable for remote clients.
     *
     * @param server
     */
    public static ProxyFactory getInstance(final MBeanServer server) {
        return getInstance(server, true);
    }

    /**
     * Get an instance of the ProxyFactory for the MBeanServerConnection.
     * Creates a ConnectionSource for it and calls getInstance( connSource, true
     * ).
     */
    public static ProxyFactory getInstance(final MBeanServerConnection conn) {
        return getInstance(conn, true);
    }

    /**
     * Get an instance. If 'useMBeanServerID' is false, and the ConnectionSource
     * is not one that has been passed before, a new ProxyFactory is
     * instantiated which will not share its proxies with any
     * previously-instantiated ones. Such usage is discouraged, as it duplicates
     * proxies. Pass 'true' unless there is an excellent reason to pass 'false'.
     *
     * @param connSource	the ConnectionSource
     * @param useMBeanServerID	use the MBeanServerID to determine if it's the
     * same server
     */
    public static synchronized ProxyFactory getInstance(
            final MBeanServerConnection conn,
            final boolean useMBeanServerID) {
        ProxyFactory instance = findInstance(conn);

        if (instance == null) {
            try {
                // if not found, match based on MBeanServerID as requested, or if this
                // is an in-process MBeanServer
                if (useMBeanServerID) {
                    final String id = JMXUtil.getMBeanServerID(conn);
                    instance = findInstanceByID(id);
                }

                if (instance == null) {
                    //debug( "Creating new ProxyFactory for ConnectionSource / conn", connSource, conn );
                    instance = new ProxyFactory(conn);
                    INSTANCES.put(conn, instance);
                }
            } catch (Exception e) {
                warning("ProxyFactory.getInstance: failure creating ProxyFactory: ", e);
                throw new RuntimeException(e);
            }
        }

        return (instance);
    }

    /**
     * @return ProxyFactory corresponding to the MBeanServerConnection
     */
    public static synchronized ProxyFactory findInstance(final MBeanServerConnection conn) {
        ProxyFactory instance = null;

        final Collection<ProxyFactory> values = INSTANCES.values();
        for (final ProxyFactory factory : values) {
            if (factory.getMBeanServerConnection() == conn) {
                instance = factory;
                break;
            }
        }
        return (instance);
    }

    /**
     * @return ProxyFactory corresponding to the MBeanServerID
     */
    public static synchronized ProxyFactory findInstanceByID(final String mbeanServerID) {
        ProxyFactory instance = null;

        final Collection<ProxyFactory> values = INSTANCES.values();
        for (final ProxyFactory factory : values) {
            if (factory.getMBeanServerID().equals(mbeanServerID)) {
                instance = factory;
                break;
            }
        }

        return (instance);
    }

    /**
     * Return (possibly cached) MBeanInfo. If the MBean does not exist, then
     * null is returned.
     */
    public MBeanInfo getMBeanInfo(final ObjectName objectName) {
        try {
            MBeanInfo info = mMBeanInfoCache.get(objectName);
            if (info == null) {
                // race condition: doesn't matter if two threads both get it
                info = getMBeanServerConnection().getMBeanInfo(objectName);
                if (invariantMBeanInfo(info)) {
                    mMBeanInfoCache.put(objectName, info);
                }
            }
            return info;
        } catch (final InstanceNotFoundException e) {
            // OK, return null
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static boolean invariantMBeanInfo(final MBeanInfo info) {
        final Descriptor d = info.getDescriptor();

        final String value = "" + d.getFieldValue(DESC_STD_IMMUTABLE_INFO);
        return Boolean.valueOf(value);
    }

    /**
     * @return MBeanServerConnection used by this factory
     */
    protected MBeanServerConnection getMBeanServerConnection() {
        return mMBeanServerConnection;
    }

    /**
     * Get any existing proxy, returning null if none exists and 'create' is
     * false. If an MBean is no longer registered, the proxy returned will be
     * null.
     *
     * @param objectName	ObjectName for which a proxy should be created
     * @param intf class of returned proxy, avoids casts and compiler warnings
     * @return an appropriate {@link AMXProxy} interface for the ObjectName
     */
    public <T extends AMXProxy> T getProxy(
            final ObjectName objectName,
            Class<T> intf) {
        final MBeanInfo info = getMBeanInfo(objectName);
        if (info == null) {
            return null;
        }

        final T proxy = getProxy(objectName, info, intf);
        return proxy;
    }

    /**
     * Call getProxy(objectName, getGenericAMXInterface()
     */
    public AMXProxy getProxy(final ObjectName objectName) {
        final MBeanInfo info = getMBeanInfo(objectName);
        if (info == null) {
            return null;
        }

        final Class<? extends AMXProxy> intf = genericInterface(info);
        final AMXProxy proxy = getProxy(objectName, info, intf);
        return proxy;
    }

    public static Class<? extends AMXProxy> genericInterface(final MBeanInfo info) {
        final String intfName = AMXProxyHandler.genericInterfaceName(info);
        Class<? extends AMXProxy> intf = AMXProxy.class;

        if (intfName == null || AMXProxy.class.getName().equals(intfName)) {
            intf = AMXProxy.class;
        } else if (AMXConfigProxy.class.getName().equals(intfName)) {
            intf = AMXConfigProxy.class;
        } else if (intfName.startsWith(AMXProxy.class.getPackage().getName())) {
            try {
                intf = Class.forName(intfName, false, ProxyFactory.class.getClassLoader()).asSubclass(AMXProxy.class);
            } catch (final Exception e) {
                // ok, use generic
                debug("ProxyFactory.getInterfaceClass(): Unable to load interface " + intfName);
            }
        } else {
            intf = AMXProxy.class;
        }
        return intf;
    }

    /**
     * NOTE: a null proxy may be returned if the MBean is no longer registered
     */
    <T extends AMXProxy> T getProxy(
            final ObjectName objectName,
            final MBeanInfo mbeanInfoIn,
            final Class<T> intfIn) {
        //debug( "ProxyFactory.createProxy: " + objectName + " of class " + expected.getName() + " with interface " + JMXUtil.interfaceName(mbeanInfo) + ", descriptor = " + mbeanInfo.getDescriptor() );
        AMXProxy proxy = null;

        try {
            MBeanInfo mbeanInfo = mbeanInfoIn;
            if (mbeanInfo == null) {
                mbeanInfo = getMBeanInfo(objectName);
            }

            // if it's a plain AMXProxy, it might have a more generic sub-interface we should use.
            Class<? extends AMXProxy> intf = intfIn;
            if (AMXProxy.class == intf) {
                intf = genericInterface(mbeanInfoIn);
            }

            final AMXProxyHandler handler = new AMXProxyHandler(getMBeanServerConnection(), objectName, mbeanInfo);
            proxy = (AMXProxy) Proxy.newProxyInstance(intf.getClassLoader(), new Class[]{intf}, handler);
            //debug( "CREATED proxy of type " + intf.getName() + ", metadata specifies " + AMXProxyHandler.interfaceName(mbeanInfo) );
        } catch (IllegalArgumentException e) {
            //debug( "createProxy", e );
            throw e;
        } catch (Exception e) {
            final Throwable rootCause = ExceptionUtil.getRootCause(e);
            if (!(rootCause instanceof InstanceNotFoundException)) {
                //debug( "createProxy", e );
                throw new RuntimeException(e);
            }
            proxy = null;
        }

        return proxy == null ? null : intfIn.cast(proxy);
    }

    protected static String toString(final Object o) {
        //return( org.glassfish.admin.amx.util.stringifier.SmartStringifier.toString( o ) );
        return "" + o;
    }

    /**
     * Array entries for MBeans that are no longer registered will contain null
     * values.
     */
    public AMXProxy[] toProxy(final ObjectName[] objectNames) {
        final AMXProxy[] result = new AMXProxy[objectNames.length];
        for (int i = 0; i < objectNames.length; ++i) {
            result[i] = getProxy(objectNames[i]);
        }
        return result;
    }

    /**
     * Convert a Set of ObjectName to a Set of AMX. The resulting Set may be
     * smaller than the original if, for example, some MBeans are no longer
     * registered.
     */
    public Set<AMXProxy> toProxySet(final Set<ObjectName> objectNames) {
        final Set<AMXProxy> s = new HashSet<AMXProxy>();

        for (final ObjectName objectName : objectNames) {
            try {
                final AMXProxy proxy = getProxy(objectName);
                if (proxy != null) {
                    s.add(proxy);
                }
            } catch (final Exception e) {
                debug("ProxyFactory.toProxySet: exception for MBean ",
                        objectName, " = ", ExceptionUtil.getRootCause(e));
            }
        }

        return (s);
    }

    /**
     * Convert a Set of ObjectName to a Set of AMX. The resulting Set may be
     * smaller than the original if, for example, some MBeans are no longer
     * registered.
     */
    public Set<AMXProxy> toProxySet(final ObjectName[] objectNames, final Class<? extends AMXProxy> intf) {
        final Set<AMXProxy> result = new HashSet<AMXProxy>();
        for (final ObjectName objectName : objectNames) {
            final AMXProxy proxy = getProxy(objectName, intf);
            if (proxy != null) {
                result.add(proxy);
            }
        }
        return (result);
    }

    /**
     * Convert a Collection of ObjectName to a List of AMX. Resulting Map could
     * differ in size if some MBeans are no longer registered.
     *
     * @return a List of AMX from a List of ObjectName.
     */
    public List<AMXProxy> toProxyList(final Collection<ObjectName> objectNames) {
        final List<AMXProxy> list = new ArrayList<AMXProxy>();

        for (final ObjectName objectName : objectNames) {
            try {
                final AMXProxy proxy = getProxy(objectName);
                if (proxy != null) {
                    list.add(proxy);
                }
            } catch (final Exception e) {
                debug("ProxyFactory.toProxySet: exception for MBean ",
                        objectName, " = ", ExceptionUtil.getRootCause(e));
            }
        }

        return (list);
    }

    /**
     * Convert a Map of ObjectName, and convert it to a Map of AMX, with the
     * same keys. Resulting Map could differ in size if some MBeans are no
     * longer registered.
     *
     * @return a Map of AMX from a Map of ObjectName.
     */
    public Map<String, AMXProxy> toProxyMap(
            final Map<String, ObjectName> objectNameMap) {
        final Map<String, AMXProxy> resultMap = new HashMap<String, AMXProxy>();

        for (final Map.Entry<String, ObjectName> me : objectNameMap.entrySet()) {
            final ObjectName objectName = me.getValue();

            try {
                final AMXProxy proxy = getProxy(objectName);
                if (proxy != null) {
                    resultMap.put(me.getKey(), proxy);
                }
            } catch (final Exception e) {
                debug("ProxyFactory.toProxySet: exception for MBean ",
                        objectName, " = ", ExceptionUtil.getRootCause(e));
            }
        }

        return (resultMap);
    }

    /**
     * Resulting Map could differ in size if some MBeans are no longer
     * registered
     */
    public Map<String, AMXProxy> toProxyMap(final ObjectName[] objectNames, final Class<? extends AMXProxy> intf) {
        final Map<String, AMXProxy> resultMap = new HashMap<String, AMXProxy>();

        for (final ObjectName objectName : objectNames) {
            final String key = Util.unquoteIfNeeded(objectName.getKeyProperty(NAME_KEY));

            final AMXProxy proxy = getProxy(objectName, intf);
            if (proxy != null) {
                resultMap.put(key, proxy);
            }
        }

        return (resultMap);
    }

    /**
     * Resulting list could differ in size if some MBeans are no longer
     * registered
     */
    public List<AMXProxy> toProxyList(final ObjectName[] objectNames, final Class<? extends AMXProxy> intf) {
        final List<AMXProxy> result = new ArrayList<AMXProxy>();
        for (final ObjectName objectName : objectNames) {
            final AMXProxy proxy = getProxy(objectName, intf);
            if (proxy != null) {
                result.add(proxy);
            }
        }
        return (result);
    }
}
