/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.mbeanserver;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;

import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.glassfish.external.amx.BootAMXMBean;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.List;
import java.util.ArrayList;

import java.lang.management.ManagementFactory;

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import com.sun.enterprise.config.serverbeans.Domain;

import org.glassfish.grizzly.config.dom.Ssl;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;


import java.io.IOException;
import java.util.Set;
import javax.management.JMException;
import org.glassfish.api.admin.*;

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.PostStartupRunLevel;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 * Responsible for creating the {@link BootAMXMBean}, and starting JMXConnectors,
 * which will initialize (boot) AMX when a connection arrives.
 */
@Service
@RunLevel(mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING, value=PostStartupRunLevel.VAL)
public final class JMXStartupService implements PostConstruct {

    private static void debug(final String s) {
        System.out.println("### " + s);
    }

    private static final Logger JMX_LOGGER = Util.JMX_LOGGER;
    
    @Inject
    private MBeanServer mMBeanServer;
    @Inject
    private Domain mDomain;
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private AdminService mAdminService;
    @Inject
    private ServiceLocator mHabitat;
    @Inject
    Events mEvents;
     
    static ServiceLocator habitat = Globals.getDefaultHabitat();
    
    @Inject
    private ServerEnvironment serverEnv;

    public enum JMXConnectorStatus {STOPPED, STARTED};

    private volatile JMXConnectorStatus jmxConnectorstatus = JMXConnectorStatus.STOPPED;
    private Object lock = new Object();
    
    private volatile BootAMX mBootAMX;
    private volatile JMXConnectorsStarterThread mConnectorsStarterThread;
    
    
    @LogMessageInfo(message = "JMXStartupService and JMXConnectors have been shut down.", level="INFO")
    private static final String JMX_STARTUPSERVICE_SHUTDOWN=Util.LOG_PREFIX + "00001";
    
    @LogMessageInfo(message="JMXStartupService: Stopped JMXConnectorServer: {0}", level="INFO")
    private static final String JMX_STARTUPSERVICE_STOPPED_JMX_CONNECTOR=Util.LOG_PREFIX + "00002";
    
    @LogMessageInfo(message="MBean Registration Exception thrown {0}", level="SEVERE", 
            cause="JMX Connector Server MBean could not be unregistered.", 
            action="Take appropriate action based on the exception message.")
    private static final String JMX_MBEAN_REG_EXCEPTION=Util.LOG_PREFIX + "00003";

    @LogMessageInfo(message="Instance Not Found Exception thrown {0}", level="SEVERE", 
            cause="JMX Connector Server MBean instance not found.", 
            action="Take appropriate action based on the exception message.")
    private static final String JMX_INSTANCE_NOT_FOUND_EXCEPTION=Util.LOG_PREFIX + "00004";

    @LogMessageInfo(message = "JMXStartupService has started JMXConnector on JMXService URL {0}", level="INFO")
    private static final String JMX_STARTED_SERVICE=Util.LOG_PREFIX + "00005";

    @LogMessageInfo(message = "JMXStartupService has disabled JMXConnector {0}", level="INFO")
    private static final String JMX_STARTED_SERVICE_DISABLED=Util.LOG_PREFIX + "00006";

    @LogMessageInfo(message = "Cannot start JMX connector {0} due to exception {1}", level="WARNING")
    private static final String JMX_CANNOT_START_CONNECTOR=Util.LOG_PREFIX + "00007";

    public JMXStartupService() {
        mMBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    private final class ShutdownListener implements EventListener {

        public void event(EventListener.Event event) {
            if (event.is(EventTypes.PREPARE_SHUTDOWN)) {
                shutdown();
            }
        }
    }

    public void waitUntilJMXConnectorStarted() {
        synchronized (lock) {
            while (jmxConnectorstatus != JMXConnectorStatus.STARTED) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

    public void postConstruct() {
        mBootAMX = BootAMX.create(mHabitat, mMBeanServer);

        final List<JmxConnector> configuredConnectors = mAdminService.getJmxConnector();

        final boolean autoStart = false;

        mConnectorsStarterThread = new JMXConnectorsStarterThread(
                AdminAuthorizedMBeanServer.newInstance(mMBeanServer, serverEnv.isInstance(), mBootAMX), configuredConnectors, mBootAMX, !autoStart, this);
        mConnectorsStarterThread.start();

        // start AMX *first* (if auto start) so that it's ready
        if (autoStart) {
            new BootAMXThread(mBootAMX).start();
        }

        mEvents.register(new ShutdownListener());
    }

    private synchronized void shutdown() {
        JMX_LOGGER.fine("JMXStartupService: shutting down AMX and JMX");

        if (mBootAMX != null) mBootAMX.shutdown();
        mBootAMX = null;

        if (mConnectorsStarterThread != null) mConnectorsStarterThread.shutdown();
        mConnectorsStarterThread = null;

        if (javax.management.MBeanServerFactory.findMBeanServer(null).size() > 0) {
            MBeanServer server = javax.management.MBeanServerFactory.findMBeanServer(null).get(0);
            javax.management.MBeanServerFactory.releaseMBeanServer(server);
        }

        // we can't block here waiting, we have to assume that the rest of the AMX modules do the right thing
        JMX_LOGGER.log(java.util.logging.Level.INFO, JMX_STARTUPSERVICE_SHUTDOWN);


    }

    private static final class BootAMXThread extends Thread {

        private final BootAMX mBooter;

        public BootAMXThread(final BootAMX booter) {
            mBooter = booter;
        }

        public void run() {
            mBooter.bootAMX();
        }
    }

    /**
     * Thread that starts the configured JMXConnectors.
     */
    private static final class JMXConnectorsStarterThread extends Thread {

        private final List<JmxConnector> mConfiguredConnectors;
        private final MBeanServer mMBeanServer;
        private final BootAMX mAMXBooterNew;
        private final boolean mNeedBootListeners;
        ConnectorStarter starter;
        ObjectName connObjectName;
        JMXStartupService service;

        public JMXConnectorsStarterThread(
                final MBeanServer mbs,
                final List<JmxConnector> configuredConnectors,
                final BootAMX amxBooter,
                final boolean needBootListeners,
                JMXStartupService service) {
            mMBeanServer = mbs;
            mConfiguredConnectors = configuredConnectors;
            mAMXBooterNew = amxBooter;
            mNeedBootListeners = needBootListeners;
            this.service = service;
        }

        void shutdown() {
            if (starter != null && starter instanceof RMIConnectorStarter) {
                ((RMIConnectorStarter) starter).stopAndUnexport();
            }
            try {
                if (connObjectName != null) {
                    mMBeanServer.unregisterMBean(connObjectName);
                    connObjectName = null;
                }
            } catch (MBeanRegistrationException ex) {
                JMX_LOGGER.log(Level.SEVERE, JMX_MBEAN_REG_EXCEPTION, ex);
            } catch (InstanceNotFoundException ex) {
                JMX_LOGGER.log(Level.SEVERE, JMX_INSTANCE_NOT_FOUND_EXCEPTION, ex);
            }
            synchronized (service.lock) {
                for (final JMXConnectorServer connector : mConnectorServers) {
                    try {
                        final JMXServiceURL address = connector.getAddress();
                        connector.stop();
                        JMX_LOGGER.log(Level.INFO, JMX_STARTUPSERVICE_STOPPED_JMX_CONNECTOR, address);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
                service.jmxConnectorstatus = JMXConnectorStatus.STOPPED;
            }
            mConnectorServers.clear();
        }

        private static String toString(final JmxConnector c) {
            return "JmxConnector config: { name = " + c.getName() +
                    ", Protocol = " + c.getProtocol() +
                    ", Address = " + c.getAddress() +
                    ", Port = " + c.getPort() +
                    ", AcceptAll = " + c.getAcceptAll() +
                    ", AuthRealmName = " + c.getAuthRealmName() +
                    ", SecurityEnabled = " + c.getSecurityEnabled() +
                    "}";
        }

        private JMXConnectorServer startConnector(final JmxConnector connConfig)
                throws IOException {
            JMX_LOGGER.log(Level.FINE, "Starting JMXConnector: {0}", toString(connConfig));

            final String protocol = connConfig.getProtocol();
            final String address = connConfig.getAddress();
            final int port = Integer.parseInt(connConfig.getPort());
            final boolean securityEnabled = Boolean.parseBoolean(connConfig.getSecurityEnabled());
            final Ssl ssl = connConfig.getSsl();

            JMXConnectorServer server = null;
            final BootAMXListener listener = mNeedBootListeners ? 
                    new BootAMXListener(mAMXBooterNew) : null;
            if (protocol.equals("rmi_jrmp")) {
                starter = new RMIConnectorStarter(mMBeanServer, address, port,
                        protocol, securityEnabled, habitat,
                        listener, ssl);
                server = ((RMIConnectorStarter) starter).start();
            } else if (protocol.equals("jmxmp")) {
                starter = new JMXMPConnectorStarter(mMBeanServer, address, port,
                        securityEnabled,
                        habitat, listener);
                server = ((JMXMPConnectorStarter) starter).start();
            } else {
                throw new IllegalArgumentException("JMXStartupService.startConnector(): Unknown protocol: " + protocol);
            }
            if (listener != null) {
                listener.setServer(server);
            }
            final JMXServiceURL url = server.getAddress();
            JMX_LOGGER.log(Level.INFO, JMX_STARTED_SERVICE, url);

            try {
                connObjectName = new ObjectName(JMX_CONNECTOR_SERVER_PREFIX + ",protocol=" + protocol + ",name=" + connConfig.getName());
                mMBeanServer.registerMBean(server, connObjectName).getObjectName();
            } catch (final Exception e) {
                // it's not critical to have it registered as an MBean
                e.printStackTrace();
            }

            return server;
        }

        private final List<JMXConnectorServer> mConnectorServers = new ArrayList<JMXConnectorServer>();

        public void run() {
            synchronized (service.lock) {
                for (final JmxConnector c : mConfiguredConnectors) {
                    if (!Boolean.parseBoolean(c.getEnabled())) {
                        JMX_LOGGER.log(Level.INFO, JMX_STARTED_SERVICE_DISABLED, c.getName());
                        continue;
                    }

                    try {
                        final JMXConnectorServer server = startConnector(c);
                        mConnectorServers.add(server);
                    } catch (final Throwable t) {
                        JMX_LOGGER.log(Level.WARNING, JMX_CANNOT_START_CONNECTOR, new Object[]{toString(c), t});
                        t.printStackTrace();
                    }
                }
                service.jmxConnectorstatus = JMXConnectorStatus.STARTED;
                service.lock.notifyAll();
            }
        }
    }

    public static final String JMX_CONNECTOR_SERVER_PREFIX = "jmxremote:type=jmx-connector-server";

    public static final Set<ObjectName> getJMXConnectorServers(final MBeanServer server) {
        try {
            final ObjectName queryPattern = new ObjectName(JMX_CONNECTOR_SERVER_PREFIX + ",*");
            return server.queryNames(queryPattern, null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the JMXServiceURLs for all connectors we've loaded.
     */
    public static JMXServiceURL[] getJMXServiceURLs(final MBeanServer server) {
        final Set<ObjectName> objectNames = getJMXConnectorServers(server);

        final List<JMXServiceURL> urls = new ArrayList<JMXServiceURL>();
        for (final ObjectName objectName : objectNames) {
            try {
                urls.add((JMXServiceURL) server.getAttribute(objectName, "Address"));
            } catch (JMException e) {
                e.printStackTrace();
                // ignore
            }
        }

        return urls.toArray(new JMXServiceURL[urls.size()]);
    }
}











