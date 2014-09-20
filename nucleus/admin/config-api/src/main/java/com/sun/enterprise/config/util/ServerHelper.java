/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.util;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import java.util.List;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.PropertyResolver;
import org.jvnet.hk2.config.Dom;

/**
 * The Server.java file is getting pretty bloated.
 * Offload some utilities here.
 *
 * @author Byron Nevins
 */
public class ServerHelper {

    public ServerHelper(Server theServer, Config theConfig) {
        server = theServer;
        config = theConfig;

        if (server == null || config == null)
            throw new IllegalArgumentException();
    }

    public final int getAdminPort() {
        try {
            if (server == null)
                return -1;

            if (config == null)
                return -1;

            String portString = getAdminPortString(server, config);

            if (portString == null)
                return -1; // get out quick.  it is kosher to call with a null Server

            return Integer.parseInt(portString);
        }
        catch (Exception e) {
            // drop through...
        }
        return -1;
    }

    public final String getAdminHost() {
         if (server == null || config == null) {
            return null;
        }
        // Look at the address for the admin-listener first
        String addr = translateAddressAndPort(getAdminListener(config), server, config)[0];
        if (addr != null && !addr.equals("0.0.0.0")) {
            return addr;
        }

        Dom serverDom = Dom.unwrap(server);
        Domain domain = serverDom.getHabitat().getService(Domain.class);
        Nodes nodes = serverDom.getHabitat().getService(Nodes.class);
        ServerEnvironment env =
                serverDom.getHabitat().getService(ServerEnvironment.class);

        if (server.isDas()) {
            if (env.isDas()) {
                // We are the DAS. Return our hostname
                return System.getProperty(
                        SystemPropertyConstants.HOST_NAME_PROPERTY);
            } else {
                return null;    // IT 12778 -- it is impossible to know
            }
        }

        String hostName = null;

        // Get it from the node associated with the server
        String nodeName = server.getNodeRef();
        if (StringUtils.ok(nodeName)) {
            Node node = nodes.getNode(nodeName);
            if (node != null) {
                hostName = node.getNodeHost();
            }
            // XXX Hack to get around the fact that the default localhost
            // node entry is malformed
            if (hostName == null && nodeName.equals("localhost-" + domain.getName())) {
                hostName = "localhost";
            }
        }

        if (StringUtils.ok(hostName)) {
            return hostName;
        }
        return null;
    }

    // very simple generic check
    public final boolean isRunning() {
        try {
            return NetUtils.isRunning(getAdminHost(), getAdminPort());
        }
        catch (Exception e) {
            // fall through
        }
        return false;
    }

    public static NetworkListener getAdminListener(Config config) {
        NetworkConfig nwc = config.getNetworkConfig();
        if (nwc == null)
            throw new IllegalStateException("Can't operate without <http-service>");
        List<NetworkListener> lss = nwc.getNetworkListeners().getNetworkListener();
        if (lss == null || lss.isEmpty())
            throw new IllegalStateException("Can't operate without at least one <network-listener>");
        for (NetworkListener ls : lss) {
            if (ServerTags.ADMIN_LISTENER_ID.equals(ls.getName())) {
                return ls;
            }
        }
        // if we can't find the admin-listener, then use the first one
        return lss.get(0);
    }

    ///////////////////////////////////////////
    ///////////////////  all private below
    ///////////////////////////////////////////
    private static String getAdminPortString(Server server, Config config) {
        if (server == null || config == null)
            return null;

        return translateAddressAndPort(getAdminListener(config), server, config)[1];
    }

    /**
     *
     * @param adminListener
     * @param server
     * @param config
     * @return ret[0] == address, ret[1] == port
     */
    private static String[] translateAddressAndPort(NetworkListener adminListener, Server server, Config config) {
        NetworkListener adminListenerRaw = null;
        String[] ret = new String[2];
            String portString = null;
            String addressString = null;

        try {
            Dom serverDom = Dom.unwrap(server);
            Domain domain = serverDom.getHabitat().getService(Domain.class);

            adminListenerRaw = GlassFishConfigBean.getRawView(adminListener);
            portString = adminListenerRaw.getPort();
            addressString = adminListenerRaw.getAddress();
            PropertyResolver resolver = new PropertyResolver(domain, server.getName());

            if (isToken(portString))
                ret[1] = resolver.getPropertyValue(portString);
            else
                ret[1] = portString;

            if (isToken(addressString))
                ret[0] = resolver.getPropertyValue(addressString);
            else
                ret[0] = addressString;
        }
        catch (ClassCastException e) {
            //jc: workaround for issue 12354
            // TODO severe error
            ret[0] = translatePortOld(addressString, server, config);
            ret[1] = translatePortOld(portString, server, config);
        }
        return ret;
    }

    private static String translatePortOld(String portString, Server server, Config config) {
        if (!isToken(portString))
            return portString;

        // isToken returned true so we are NOT assuming anything below!
        String key = portString.substring(2, portString.length() - 1);

        // check cluster and the cluster's config if applicable
        // bnevins Jul 18, 2010 -- don't botehr this should never be called anymore
        SystemProperty prop = server.getSystemProperty(key);

        if (prop != null) {
            return prop.getValue();
        }

        prop = config.getSystemProperty(key);

        if (prop != null) {
            return prop.getValue();
        }

        return null;
    }

    private static boolean isToken(String s) {
        return s != null
                && s.startsWith("${")
                && s.endsWith("}")
                && s.length() > 3;
    }
    private final Server server;
    private final Config config;
}
