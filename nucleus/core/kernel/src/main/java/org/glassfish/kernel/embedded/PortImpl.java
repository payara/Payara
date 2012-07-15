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

package org.glassfish.kernel.embedded;

import java.beans.PropertyVetoException;
import java.util.List;

import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import org.glassfish.grizzly.config.dom.Http;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.Transport;
import org.glassfish.grizzly.config.dom.Transports;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.embedded.Port;
import javax.inject.Inject;
import javax.inject.Named;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.*;

/**
 * Abstract to port creation and destruction
 */
@Service
@PerLookup
public class PortImpl implements Port {
    @Inject
    CommandRunner runner = null;
    @Inject @Named("plain")
    ActionReport report = null;
    @Inject
    PortsImpl ports;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    NetworkConfig config;
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    HttpService httpService;
    String listenerName;
    int number;
    String defaultVirtualServer = "server";

    public void setPortNumber(final int portNumber) {
        number = portNumber;
    }

    public void close() {
        removeListener();
        ports.remove(this);
    }

    private void removeListener() {
        try {
            ConfigSupport.apply(new ConfigCode() {
                public Object run(ConfigBeanProxy[] params) throws PropertyVetoException, TransactionFailure {
                    final NetworkListeners nt = (NetworkListeners) params[0];
                    final VirtualServer vs = (VirtualServer) params[1];
                    final Protocols protocols = (Protocols) params[2];
                    List<Protocol> protos = protocols.getProtocol();
                    for (Protocol proto : protos) {
                        if (proto.getName().equals(listenerName)) {
                            protos.remove(proto);
                            break;
                        }
                    }
                    final List<NetworkListener> list = nt.getNetworkListener();
                    for (NetworkListener listener : list) {
                        if (listener.getName().equals(listenerName)) {
                            list.remove(listener);
                            break;
                        }
                    }
                    String regex = listenerName + ",?";
                    String lss = vs.getNetworkListeners();
                    if (lss != null) {
                        vs.setNetworkListeners(lss.replaceAll(regex, ""));
                    }
                    return null;
                }
            }, config.getNetworkListeners(),
                httpService.getVirtualServerByName(defaultVirtualServer),
                config.getProtocols());
        } catch (TransactionFailure tf) {
            tf.printStackTrace();
            throw new RuntimeException(tf);
        }
    }

    public int getPortNumber() {
        return number;
    }
}
