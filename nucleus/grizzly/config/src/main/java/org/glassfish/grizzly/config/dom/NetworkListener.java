/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Pattern;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Binds protocol to a specific endpoint to listen on
 */
@Configured
public interface NetworkListener extends ConfigBeanProxy, PropertyBag {
    boolean ENABLED = true;
    boolean JK_ENABLED = false;
    String DEFAULT_ADDRESS = "0.0.0.0";
    String DEFAULT_CONFIGURATION_FILE = "${com.sun.aas.instanceRoot}/config/glassfish-jk.properties";
    String TYPE_PATTERN = "(standard|proxy)";
    String DEFAULT_TYPE = "standard";
    

    /**
     * IP address to listen on
     */
    @Attribute(defaultValue = DEFAULT_ADDRESS)
    @NetworkAddress
    String getAddress();

    void setAddress(String value);

    /**
     * If false, a configured listener, is disabled
     */
    @Attribute(defaultValue = "" + ENABLED, dataType = Boolean.class)
    String getEnabled();

    void setEnabled(String enabled);

    @Attribute(defaultValue = DEFAULT_CONFIGURATION_FILE)
    String getJkConfigurationFile();

    void setJkConfigurationFile(String file);

    /**
     * If true, a jk listener is enabled
     */
    @Attribute(defaultValue = "" + JK_ENABLED, dataType = Boolean.class)
    String getJkEnabled();

    void setJkEnabled(String enabled);

    /**
     * Network-listener name, which could be used as reference
     */
    @Attribute(required = true, key = true)
    String getName();

    void setName(String value);

    /**
     * Network-listener name, which could be used as reference
     */
    @Attribute(required = true, dataType = String.class, defaultValue = DEFAULT_TYPE)
    @Pattern(regexp = TYPE_PATTERN)
    String getType();

    void setType(String type);
    
    /**
     * Port to listen on
     */
    @Attribute(required = true, dataType = Integer.class)
    @Range(min=0, max=65535)
    String getPort();

    void setPort(String value);

    /**
     * Reference to a protocol
     */
    @Attribute(required = true)
    String getProtocol();

    void setProtocol(String value);

    /**
     * Reference to a thread-pool, defined earlier in the document.
     */
    @Attribute
    String getThreadPool();

    void setThreadPool(String value);

    /**
     * Reference to a low-level transport
     */
    @Attribute(required = true)
    String getTransport();

    void setTransport(String value);

    @DuckTyped
    Protocol findHttpProtocol();

    @DuckTyped
    String findHttpProtocolName();

    @DuckTyped
    Protocol findProtocol();

    @DuckTyped
    ThreadPool findThreadPool();

    @DuckTyped
    Transport findTransport();

    @DuckTyped
    NetworkListeners getParent();

    class Duck {

        public static Protocol findHttpProtocol(NetworkListener listener) {
            return findHttpProtocol(new HashSet<String>(), findProtocol(listener));
        }

        private static Protocol findHttpProtocol(Set<String> tray, Protocol protocol) {
            if (protocol == null) {
                return null;
            }

            final String protocolName = protocol.getName();
            if (tray.contains(protocolName)) {
                throw new IllegalStateException(
                    "Loop found in Protocol definition. Protocol name: " + protocol.getName());
            }

            if (protocol.getHttp() != null) {
                return protocol;
            } else if (protocol.getPortUnification() != null) {
                final List<ProtocolFinder> finders = protocol.getPortUnification().getProtocolFinder();
                tray.add(protocolName);

                try {
                    Protocol foundHttpProtocol = null;
                    for (ProtocolFinder finder : finders) {
                        final Protocol subProtocol = finder.findProtocol();
                        if (subProtocol != null) {
                            final Protocol httpProtocol = findHttpProtocol(tray, subProtocol);
                            if (httpProtocol != null) {
                                foundHttpProtocol = httpProtocol;
                            }
                        }
                    }
                    return foundHttpProtocol;
                } finally {
                    tray.remove(protocolName);
                }
            }

            return null;
        }

        public static String findHttpProtocolName(NetworkListener listener) {
            final Protocol httpProtocol = findHttpProtocol(listener);
            if (httpProtocol != null) {
                return httpProtocol.getName();
            }

            return null;
        }

        public static Protocol findProtocol(NetworkListener listener) {
            return listener.getParent().getParent().findProtocol(listener.getProtocol());
        }

        public static ThreadPool findThreadPool(NetworkListener listener) {
            final NetworkListeners listeners = listener.getParent();
            List<ThreadPool> list = listeners.getThreadPool();
            if (list == null || list.isEmpty()) {
                final ConfigBeanProxy parent = listener.getParent().getParent().getParent();
                final Dom proxy = Dom.unwrap(parent).element("thread-pools");
                final List<Dom> domList = proxy.nodeElements("thread-pool");
                list = new ArrayList<ThreadPool>(domList.size());
                for (Dom dom : domList) {
                    list.add(dom.<ThreadPool>createProxy());
                }
            }
            for (ThreadPool pool : list) {
                if (listener.getThreadPool().equals(pool.getName())) {
                    return pool;
                }
            }
            return null;
        }

        public static Transport findTransport(NetworkListener listener) {
            List<Transport> list = listener.getParent().getParent().getTransports().getTransport();
            for (Transport transport : list) {
                if (listener.getTransport().equals(transport.getName())) {
                    return transport;
                }
            }
            return null;
        }

        public static NetworkListeners getParent(NetworkListener listener) {
            return listener.getParent(NetworkListeners.class);
        }
    }
}
