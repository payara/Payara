/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.kernel.services.impl;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.v3.services.impl.GlassfishNetworkListener;
import com.sun.enterprise.v3.services.impl.GrizzlyService;

import org.glassfish.grizzly.config.dom.NetworkListener;

/**
 * Extension of the default network listener that reusing ports allocated by the
 * Payara Micro {@link fish.payara.micro.impl.PortBinder PortBinder} when
 * <code>--autoBindHttp</code> is enabled.
 */
public class MicroNetworkListener extends GlassfishNetworkListener {

    private static final Logger LOGGER = Logger.getLogger(MicroNetworkListener.class.getName());

    /**
     * A list of open sockets that Payara Micro already has bound.
     */
    private static Map<Integer, ServerSocket> reservedSocketMap = new HashMap<>();

    public MicroNetworkListener(final GrizzlyService grizzlyService, final NetworkListener networkListener,
            final Logger logger) {
        super(grizzlyService, networkListener, logger);
    }

    @Override
    public void start() throws IOException {
        if (reservedSocketMap.containsKey(port)) {
            ServerSocket reservedSocket = reservedSocketMap.get(port);
            LOGGER.log(Level.INFO, "Found reserved socket on port: {0,number,#}.", port);
            if (reservedSocket.isBound()) {
                reservedSocket.close();
                reservedSocketMap.remove(port);
            }
        }
        super.start();
    }

    /**
     * Adds a currently open socket to be allocated to a listener.
     */
    public static void addReservedSocket(int boundPort, ServerSocket socket) {
        LOGGER.log(Level.INFO, "Reserving port: {0,number,#}", boundPort);
        reservedSocketMap.put(boundPort, socket);
    }

    /**
     * Clears the list of open sockets, to make sure none are left open but unused.
     */
    public static void clearReservedSockets() throws IOException {
        for (ServerSocket socket : reservedSocketMap.values()) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
        reservedSocketMap.clear();
    }

}