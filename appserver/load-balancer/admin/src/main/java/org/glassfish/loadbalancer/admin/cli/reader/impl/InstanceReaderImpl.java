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

package org.glassfish.loadbalancer.admin.cli.reader.impl;

import java.net.UnknownHostException;
import org.glassfish.loadbalancer.admin.cli.transform.Visitor;
import org.glassfish.loadbalancer.admin.cli.transform.InstanceVisitor;

import org.glassfish.loadbalancer.admin.cli.reader.api.InstanceReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.LbReaderException;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.ServerTags;

import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.grizzly.config.dom.Protocol;
import org.glassfish.grizzly.config.dom.Protocols;
import java.net.InetAddress;
import java.util.Iterator;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;
import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;

/**
 * Provides instance information relavant to Load balancer tier.
 *
 * @author Kshitiz Saxena
 */
public class InstanceReaderImpl implements InstanceReader {

    /**
     * Constructor
     */
    public InstanceReaderImpl(Domain domain, ServerRef ref) {
        _domain = domain;
        _serverRef = ref;
        _server = domain.getServerNamed(ref.getRef());
    }

    public InstanceReaderImpl(Domain domain, Server server) {
        _domain = domain;
        _server = server;
    }

    /**
     * Return server instance's name.
     *
     * @return String           instance' name
     */
    @Override
    public String getName() throws LbReaderException {
        return _server.getName();
    }

    /**
     * Returns if the server is enabled in the load balancer or not.
     *
     * @return boolean          true if enabled in LB; false if disabled
     */
    @Override
    public boolean getLbEnabled() throws LbReaderException {
        if(_serverRef != null){
            return Boolean.valueOf(_serverRef.getLbEnabled()).booleanValue();
        }
        return LoadbalancerReader.LBENABLED_VALUE;
    }

    /**
     * This is used in quicescing. Timeouts after this interval and disables the
     * instance in the load balancer. 
     *
     * @return String           Disable time out in minutes
     */
    @Override
    public String getDisableTimeoutInMinutes() throws LbReaderException {
        if(_serverRef != null) {
            return _serverRef.getDisableTimeoutInMinutes();
        }
        return LoadbalancerReader.DISABLE_TIMEOUT_IN_MINUTES_VALUE;
    }

    /**
     * This is used in weighted round robin. returns the weight of the instance
     *
     * @return String           Weight of the instance
     */
    @Override
    public String getWeight() throws LbReaderException {
        return _server.getLbWeight();
    }

    /**
     * Enlists both http and https listeners of this server instance
     * It will be form "http:<hostname>:<port> https:<hostname>:<port>"
     *
     * @return String   Listener(s) info.
     */
    @Override
    public String getListeners() throws LbReaderException {
        StringBuffer listenerStr = new StringBuffer();

        Config config = _domain.getConfigNamed(_server.getConfigRef());
        NetworkConfig networkConfig = config.getNetworkConfig();
        Protocols protocols = networkConfig.getProtocols();
        NetworkListeners nls = networkConfig.getNetworkListeners();
        Iterator<NetworkListener> listenerIter = nls.getNetworkListener().iterator();

        int i = 0;
        PropertyResolver resolver = new PropertyResolver(_domain, _server.getName());
        while (listenerIter.hasNext()) {
            NetworkListener listener = listenerIter.next();
            NetworkListener rawListener = GlassFishConfigBean.getRawView(listener);
            if (rawListener.getName().equals(ADMIN_LISTENER)) {
                continue;
            }

            String prot = rawListener.getProtocol();
            Protocol protocol = protocols.findProtocol(prot);

            if (i > 0) {
                listenerStr.append(' '); // space between listener names
            }
            i++;

            if (Boolean.valueOf(protocol.getHttp().getJkEnabled())){
                listenerStr.append(AJP_PROTO);
            } else {
            if (Boolean.valueOf(protocol.getSecurityEnabled()).booleanValue()) {
                listenerStr.append(HTTPS_PROTO);
            } else {
                listenerStr.append(HTTP_PROTO);
            }
            }
            String hostName = getResolvedHostName(rawListener.getAddress());
            listenerStr.append(hostName);
            listenerStr.append(':');
            // resolve the port name
            String port = rawListener.getPort();

            // If it is system variable, resolve it
            if ((port != null) && (port.length() > 1) && (port.charAt(0) == '$')
                    && (port.charAt(1) == '{') && (port.charAt(port.length() - 1) == '}')) {
                String portVar = port.substring(2, port.length() - 1);
                port = resolver.getPropertyValue(portVar);
                if (port == null) {
                    throw new LbReaderException(LbLogUtil.getStringManager().getString("UnableToResolveSystemProperty", portVar, _server.getName()));
                }
            }
            listenerStr.append(port);
        }
        return listenerStr.toString();
    }

    // --- VISITOR IMPLEMENTATION ---
    @Override
    public void accept(Visitor v) throws Exception {
		if (v instanceof InstanceVisitor) {
			InstanceVisitor pv = (InstanceVisitor) v;
			pv.visit(this);
		}
    }

    private String getResolvedHostName(String address) throws LbReaderException {
        InetAddress addr = null;
        if (!address.equals(BIND_TO_ANY)) {
            try {
                addr = InetAddress.getByName(address);
            } catch (UnknownHostException ex) {
                String msg = LbLogUtil.getStringManager().getString("CannotResolveHostName", address);
                throw new LbReaderException(msg, ex);
            }
            if (!addr.isLoopbackAddress()) {
                return address;
            }
        }
        String nodeName = _server.getNodeRef();
        Node node = _domain.getNodes().getNode(nodeName);
        if (node == null) {
            String msg = LbLogUtil.getStringManager().getString("UnableToGetNode", _server.getName());
            throw new LbReaderException(msg);
        }
        if (node.getNodeHost() != null && !node.getNodeHost().equals(LOCALHOST)) {
            return node.getNodeHost();
        }
        return System.getProperty("com.sun.aas.hostName");
    }
    // --- PRIVATE VARS -------
    private Domain _domain = null;
    private ServerRef _serverRef = null;
    private Server _server = null;
    private static final String HTTP_PROTO = "http://";
    private static final String HTTPS_PROTO = "https://";
    private static final String AJP_PROTO = "ajp://";
    private static final String ADMIN_LISTENER = ServerTags.ADMIN_LISTENER_ID;
    private static final String BIND_TO_ANY = "0.0.0.0";
    private static final String LOCALHOST = "localhost";
}
