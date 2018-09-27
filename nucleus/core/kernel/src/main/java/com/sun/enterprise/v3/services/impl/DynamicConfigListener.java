/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017-2018] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.services.impl;

import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Ssl;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.config.serverbeans.SystemProperty;

import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.Result;
import org.glassfish.grizzly.config.dom.FileCache;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;

/**
 * Grizzly dynamic configuration handler
 *
 * @author Alexey Stashok
 */
public class DynamicConfigListener implements ConfigListener {

    private static final String ADMIN_LISTENER = "admin-listener";
    private static final String OFF = "OFF";
    private GrizzlyService grizzlyService;
    private Config config;
    private final Logger logger;
    private static final int RECONFIG_LOCK_TIMEOUT_SEC = 30;
    private static final ReentrantLock reconfigLock = new ReentrantLock();
    private static final Map<Integer, FutureImpl> reconfigByPortLock
            = new HashMap<Integer, FutureImpl>();

    public DynamicConfigListener(final Config parent, final Logger logger) {
        config = parent;
        this.logger = logger;
    }

    @Override
    public synchronized UnprocessedChangeEvents changed(final PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(
                events, new Changed() {
            @Override
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type,
                    Class<T> tClass, T t) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "NetworkConfig changed {0} {1} {2}",
                            new Object[]{type, tClass, t});
                }
                if (tClass == NetworkListener.class && t instanceof NetworkListener) {
                    return processNetworkListener(type, (NetworkListener) t, events);
                } else if (tClass == Http.class && t instanceof Http) {
                    return processProtocol(type, (Protocol) t.getParent(), events);
                } else if (tClass == FileCache.class && t instanceof FileCache) {
                    return processProtocol(type, (Protocol) t.getParent().getParent(), null);
                } else if (tClass == Ssl.class && t instanceof Ssl) {
                    /*
                         * Make sure the SSL parent is in fact a protocol.  It could
                         * be a jmx-connector.
                     */
                    final ConfigBeanProxy parent = t.getParent();
                    if (parent instanceof Protocol) {
                        return processProtocol(type, (Protocol) parent, null);
                    }
                } else if (tClass == Protocol.class && t instanceof Protocol) {
                    return processProtocol(type, (Protocol) t, null);
                } else if (tClass == ThreadPool.class && t instanceof ThreadPool) {
                    NotProcessed notProcessed = null;
                    ThreadPool threadPool = (ThreadPool) t;
                    for (NetworkListener listener : threadPool.findNetworkListeners()) {
                            notProcessed = processNetworkListener(type, listener, null);
                    }
                    
                    // Throw an unprocessed event change if one hasn't already if HTTP or ThreadPool monitoring is enabled. 
                    MonitoringService ms = config.getMonitoringService();
                    String threadPoolLevel = ms.getModuleMonitoringLevels().getThreadPool();
                    String httpServiceLevel = ms.getModuleMonitoringLevels().getHttpService();
                    
                    if (((threadPoolLevel != null && !threadPoolLevel.equals(OFF)) 
                            || (httpServiceLevel != null && !httpServiceLevel.equals(OFF))) && notProcessed == null) {
                        notProcessed = new NotProcessed("Monitoring statistics will be incorrect for " 
                                + threadPool.getName() + " until restart due to changed attribute(s).");
                    }
                    
                    return notProcessed;
                } else if (tClass == Transport.class && t instanceof Transport) {
                    NotProcessed notProcessed = null;
                    for (NetworkListener listener : ((Transport) t).findNetworkListeners()) {
                        notProcessed = processNetworkListener(type, listener, null);
                    }
                    return notProcessed;
                } else if (tClass == VirtualServer.class
                        && t instanceof VirtualServer
                        && !grizzlyService.hasMapperUpdateListener()) {
                    return processVirtualServer(type, (VirtualServer) t);
                } else if (tClass == SystemProperty.class && t instanceof SystemProperty) {
                    NetworkConfig networkConfig = config.getNetworkConfig();
                        if ((networkConfig != null) && ((SystemProperty)t).getName().endsWith("LISTENER_PORT")) {
                        for (NetworkListener listener : networkConfig.getNetworkListeners().getNetworkListener()) {
                                if (listener.getPort().equals(((SystemProperty)t).getValue())) {
                                    return processNetworkListener(Changed.TYPE.CHANGE, listener, events);
                                }
                            }
                        }
                        return null;
                }
                return null;
            }
        }, logger);
    }

    private <T extends ConfigBeanProxy> NotProcessed processNetworkListener(Changed.TYPE type,
            NetworkListener listener, final PropertyChangeEvent[] changedProperties) {

        if (findConfigName(listener).equals(findConfigName(config))) {

            boolean isAdminListener = ADMIN_LISTENER.equals(listener.getName());
            Lock portLock = null;
            try {
                portLock = acquirePortLock(listener);
                switch (type) {
                    case ADD:
                        final int[] ports = portLock.getPorts();
                        if (isAdminListener && ports[ports.length - 1] == -1) {
                            return null;
                        }   final Future future = grizzlyService.createNetworkProxy(listener);
                        if (future != null) {
                            future.get(RECONFIG_LOCK_TIMEOUT_SEC, TimeUnit.SECONDS);
                            grizzlyService.registerContainerAdapters();
                        } else {
                            logger.log(Level.FINE, "Skipping proxy registration for the listener {0}",
                                    listener.getName());
                        }   break;
                    case REMOVE:
                        if (!isAdminListener) {
                            grizzlyService.removeNetworkProxy(listener);
                        }   break;
                    case CHANGE:
                        // If the listener is the admin listener
                        if (isAdminListener) {
                            return null;
                        }   

                        // Only restart the network listener if something hasn't changed
                        if (!isRedundantChange(changedProperties)) {
                            MonitoringService ms = config.getMonitoringService();
                            String level = ms.getModuleMonitoringLevels().getHttpService();
                            
                            // We only need to throw an unprocessed change event if monitoring is enabled for the HTTP service
                            if (level != null && (!level.equals(OFF))) {
                                String eventsMessage = "Monitoring statistics will be incorrect for "
                                        + listener.findHttpProtocolName() + " until restart due to changed attribute(s): ";
                                
                                // Add list of changed events. Usually only one message is sent to this method at a time,
                                // so the for loop should only run once, but it's there just in case.
                                for (PropertyChangeEvent event : changedProperties) {
                                    eventsMessage += ("\"" + event.getPropertyName() + "\" changed from \""
                                            + event.getOldValue() + "\" to \"" + event.getNewValue() + "\".\n");
                                }
                                
                                // Still restart the network listener, as it's only the monitoring that breaks.
                                grizzlyService.restartNetworkListener(listener, RECONFIG_LOCK_TIMEOUT_SEC, TimeUnit.SECONDS);
                                
                                return new NotProcessed(eventsMessage);
                            } else {
                                // Restart the network listener without throwing an unprocessed change
                                grizzlyService.restartNetworkListener(listener, RECONFIG_LOCK_TIMEOUT_SEC, TimeUnit.SECONDS);
                            }
                        }   break;
                    default:
                        break;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Network listener configuration error. Type: " + type, e);
            } finally {
                if (portLock != null) {
                    releaseListenerLock(portLock);
                }
            }
        }
        return null;
    }

    private String findConfigName(final ConfigBeanProxy child) {
        ConfigBeanProxy bean = child;
        while (bean != null && !(bean instanceof Config)) {
            bean = bean.getParent();
        }
        return bean != null ? ((Config) bean).getName() : "";
    }

    /**
     * Checks if a signalled event change is redundant
     * e.g. first event on startup is always changing admin-thread-pool name from admin-thread-pool to itself,
     * which is redundant.
     * @param events List of events to check for redundancy
     * @return boolean true if event change is redundant, false otherwise
     */
    private boolean isRedundantChange(PropertyChangeEvent[] events) {
        boolean redundant = true;

        if(events == null || events.length == 0) {
            return redundant;
        }

        for (PropertyChangeEvent event : events) {
            if (!event.getNewValue().equals(event.getOldValue())) {
                redundant = false;
            }
        }

        return redundant;
    }

    private boolean isAdminDynamic(PropertyChangeEvent[] events) {
        if (events == null || events.length == 0) {
            return false;
        }
        // for now, anything other than comet support will require a restart
        for (PropertyChangeEvent e : events) {
            if ("comet-support-enabled".equals(e.getPropertyName())) {
                return true;
            }
        }
        return false;
    }

    private NotProcessed processProtocol(Changed.TYPE type, Protocol protocol, PropertyChangeEvent[] events) {
        NotProcessed notProcessed = null;
        for (NetworkListener listener : protocol.findNetworkListeners()) {
            notProcessed = processNetworkListener(type, listener, events);
        }
        return notProcessed;
    }

    private NotProcessed processVirtualServer(Changed.TYPE type, VirtualServer vs) {
        NotProcessed notProcessed = null;
        for (NetworkListener n : vs.findNetworkListeners()) {
            notProcessed = processNetworkListener(type, n, null);
        }
        return notProcessed;
    }

    public void setGrizzlyService(GrizzlyService grizzlyService) {
        this.grizzlyService = grizzlyService;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Lock TCP ports, which will take part in the reconfiguration to avoid
     * collisions
     */
    private Lock acquirePortLock(NetworkListener listener) throws InterruptedException, TimeoutException {
        final boolean isLoggingFinest = logger.isLoggable(Level.FINEST);
        final int port = getPort(listener);
        try {
            while (true) {
                logger.finest("Aquire reconfig lock");
                if (reconfigLock.tryLock(RECONFIG_LOCK_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    Future lock = reconfigByPortLock.get(port);
                    if (isLoggingFinest) {
                        logger.log(Level.FINEST, "Reconfig lock for port: {0} is {1}",
                                new Object[]{port, lock});
                    }
                    int proxyPort = -1;
                    if (lock == null) {
                        final NetworkProxy runningProxy = grizzlyService.lookupNetworkProxy(listener);
                        if (runningProxy != null) {
                            proxyPort = runningProxy.getPort();
                            if (port != proxyPort) {
                                lock = reconfigByPortLock.get(proxyPort);
                                if (isLoggingFinest) {
                                    logger.log(Level.FINEST, "Reconfig lock for proxyport: {0} is {1}",
                                            new Object[]{proxyPort, lock});
                                }
                            } else {
                                proxyPort = -1;
                            }
                        }
                    }
                    if (lock != null) {
                        reconfigLock.unlock();
                        try {
                            logger.finest("Waiting on reconfig lock");
                            lock.get(RECONFIG_LOCK_TIMEOUT_SEC, TimeUnit.SECONDS);
                        } catch (ExecutionException e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        final FutureImpl future = SafeFutureImpl.create();
                        if (isLoggingFinest) {
                            logger.log(Level.FINEST, "Set reconfig lock for ports: {0} and {1}: {2}",
                                    new Object[]{port, proxyPort, future});
                        }
                        reconfigByPortLock.put(port, future);
                        if (proxyPort != -1) {
                            reconfigByPortLock.put(proxyPort, future);
                        }
                        return new Lock(port, proxyPort);
                    }
                } else {
                    throw new TimeoutException("Lock timeout");
                }
            }
        } finally {
            if (reconfigLock.isHeldByCurrentThread()) {
                reconfigLock.unlock();
            }
        }
    }

    private void releaseListenerLock(Lock lock) {
        final boolean isLoggingFinest = logger.isLoggable(Level.FINEST);
        reconfigLock.lock();
        try {
            final int[] ports = lock.getPorts();
            if (isLoggingFinest) {
                logger.log(Level.FINEST, "Release reconfig lock for ports: {0}",
                        Arrays.toString(ports));
            }
            FutureImpl future = null;
            for (int port : ports) {
                if (port != -1) {
                    future = reconfigByPortLock.remove(port);
                }
            }
            if (future != null) {
                if (isLoggingFinest) {
                    logger.log(Level.FINEST, "Release reconfig lock, set result: {0}",
                            future);
                }
                future.result(new Result<Thread>(Thread.currentThread()));
            }
        } finally {
            reconfigLock.unlock();
        }
    }

    private int getPort(NetworkListener listener) {
        int listenerPort = -1;
        try {
            listenerPort = Integer.parseInt(listener.getPort());
        } catch (NumberFormatException e) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "Can not parse network-listener port number: {0}",
                        listener.getPort());
            }
        }
        return listenerPort;
    }

    private static final class Lock {

        private final int[] ports;

        public Lock(int... ports) {
            this.ports = ports;
        }

        public int[] getPorts() {
            return ports;
        }
    }
}
