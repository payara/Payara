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

import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 * Contains complete Grizzly configuration.
 */
@Configured
public interface NetworkConfig extends ConfigBeanProxy, PropertyBag {
    /**
     * Describes low level transports configuration.  Like tcp, udp, ssl
     * transports configuration
     */
    @Element(required = true)
    Transports getTransports();

    void setTransports(Transports value);

    /**
     * Describes higher level protocols like: http, https, iiop
     */
    @Element(required = true)
    Protocols getProtocols();

    void setProtocols(Protocols value);

    /**
     * Binds protocols with lower level transports
     */
    @Element(required = true)
    NetworkListeners getNetworkListeners();

    void setNetworkListeners(NetworkListeners value);

    @DuckTyped
    NetworkListener getNetworkListener(String name);

    @DuckTyped
    Protocol findProtocol(String name);

    class Duck {
        public static Protocol findProtocol(NetworkConfig config, String name) {
            for (final Protocol protocol : config.getProtocols().getProtocol()) {
                if (protocol.getName().equals(name)) {
                    return protocol;
                }
            }
            return null;
        }

        public static NetworkListener getNetworkListener(NetworkConfig config, String name) {
            if (name != null) {
                for (final NetworkListener listener : config.getNetworkListeners().getNetworkListener()) {
                    if (listener.getName().equals(name)) {
                        return listener;
                    }
                }
            }
            return null;
        }
    }
}
