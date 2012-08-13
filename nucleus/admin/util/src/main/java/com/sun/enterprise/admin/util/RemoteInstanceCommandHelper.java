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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.*;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.PropertyResolver;

/**
 * @author Byron Nevins
 *
 * Implementation Note:
 *
 * Ideally this class would be extended by AdminCommand's that need these
 * services.  The problem is getting the values out of the habitat.  The ctor
 * call would be TOO EARLY  in the derived classes.  The values are injected AFTER
 * construction.  We can't easily inject here -- because we don't want this class
 * to be a Service.
 * We could do it by having the derived class call a set method in here but that
 * gets very messy as we have to make sure we are in a valid state for every single
 * method call.
 *
 */
public final class RemoteInstanceCommandHelper {

    public RemoteInstanceCommandHelper(ServiceLocator habitatIn) {

        try {
            habitat = habitatIn;
            servers = habitat.<Servers>getService(Servers.class).getServer();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public final String getHost(final String serverName) {
        String host = null;
        Server server = getServer(serverName);
        if (server != null) {
            host = server.getAdminHost();
        }
        return host;
    }

    public final Server getServer(final String serverName) {
        for (Server server : servers) {
            final String name = server.getName();

            // ??? TODO is this crazy?
            if (serverName == null) {
                if (name == null) // they match!!
                    return server;
            }
            else if (serverName.equals(name))
                return server;
        }
        return null;
    }

    public final String getNode(final Server server) {

        if (server == null)
            return null;

        String node = server.getNodeRef();

        if (StringUtils.ok(node))
            return node;
        else
            return "no node";
    }

    public final int getAdminPort(final String serverName) {
        return getAdminPort(getServer(serverName));
    }

    public final int getAdminPort(Server server) {
        return server.getAdminPort();
    }

    ///////////////////////////////////////////////////////////////////////////
    //  All private below.  If you need something below in a derived class then
    // upgrade to pkg-private and move it above this line.  Change the keyword
    // private to final on the method
    ///////////////////////////////////////////////////////////////////////////

    final private List<Server> servers;
    final private ServiceLocator habitat;
}
