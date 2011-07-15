/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.net.*;
import java.beans.PropertyVetoException;
import java.util.*;
import java.util.logging.*;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Hiding place for the remarkably complex logic of assigning ports to instances
 * GUARANTEE -- the only thing thrown from here is TransactionFailure
 * @author Byron Nevins
 */
public final class PortManager {

    public PortManager(Cluster cluster, Config config, Domain theDomain,
            Server theNewServer) throws TransactionFailure {
        try {
            if (theNewServer == null || theDomain == null)
                throw new TransactionFailure(Strings.get("internal.error", "null argument in PortManager constructor"));

            newServer = theNewServer;
            domain = theDomain;
            serverName = newServer.getName();

            // bnevins 7-23-2010
            // we are probably being called from inside the create decorator for a server.
            // the server is not yet committed.  We can't call ducktype methods
            // on the server yet.  So we do this self-serve call to get the host

            //host = newServer.getHost();

            host = new ServerHelper(theNewServer, config).getAdminHost();

            allPorts = new TreeSet<Integer>();
            newServerPorts = new ServerPorts(cluster, config, domain, newServer);

            isLocal = NetUtils.isThisHostLocal(host);


            allServers = domain.getServers().getServer();

            // why all this nonsense?  ConcurrentModificationException!!!
            for (Iterator<Server> it = allServers.iterator(); it.hasNext();) {
                Server curr = it.next();
                if (serverName.equals(curr.getName())) {
                    it.remove();
                }
            }
            serversOnHost = new ArrayList<ServerPorts>();
        }
        catch (TransactionFailure tf) {
            throw tf;
        }
        catch (Exception e) {
            // this Exception will not take just a Throwable.  I MUST give a string
            throw new TransactionFailure(e.toString(), e);
        }
    }

    public String process() throws TransactionFailure {
        try {
            // if there are no port system-property's -- no point in going on!
            if (newServerPorts.getMap().isEmpty())
                return null; // all done!

            // make sure user-supplied props are not flaky
            PortUtils.checkInternalConsistency(newServer);

            // create a list of ALL servers running on the same machine
            createServerList();

            // create a sorted list of every port on every other server on the same machine.
            populateAllPorts();

            // we have a list of all possible conflicting server ports.
            // let's find some unused ports and reassign the variables inside
            // the ServerPorts class
            Map<String, Integer> reassigned = reassignPorts();
            Set<Map.Entry<String, Integer>> entries = reassigned.entrySet();
            List<SystemProperty> sps = newServer.getSystemProperty();

            // We want to display EVERY port assignment -- no matter if it overrides or
            // not.  Tricky to get the display string just right.
            // if there are reassignments -- we overwrite the ones in finalPorts.
            // if not -- finalPorts contains all the stock ones
            // in any case ALL port assignments should be displayed.
            Map<String, Integer> finalPorts = newServerPorts.getMap();

            if (entries.size() > 0) {
                for (Map.Entry<String, Integer> entry : entries) {
                    String name = entry.getKey();
                    int port = entry.getValue();
                    changeSystemProperty(sps, name, "" + port); // do not want commas in the int!
                    finalPorts.put(name, port);
                }
            }
            return generateAssignedPortMessage(finalPorts);
        }
        catch (Exception e) {
            throw new TransactionFailure(e.toString(), e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PortManager Dump:");

        for (ServerPorts sp : serversOnHost)
            sb.append(sp).append('\n');

        sb.append("All Ports in all other servers on same host: " + allPorts);
        return sb.toString();
    }

    private String generateAssignedPortMessage(final Map<String, Integer> ports) {
        try {
            Set<Map.Entry<String, Integer>> entries = ports.entrySet();
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, Integer> entry : entries) {
                String name = entry.getKey();
                int port = entry.getValue();
                sb.append("\n").append(name).append("=").append("" + port);
            }
            return Strings.get("PortManager.port.summary", serverName, sb.toString());
        }
        catch (Exception e) {
            // fall through
        }
        return null;
    }

    private void createServerList() {
        if (isLocal)
            createLocalServerList();
        else
            createRemoteServerList();

    }

    private void createLocalServerList() {
        for (Server server : allServers) {
            if (server.isDas())
                serversOnHost.add(new ServerPorts(domain, server));
            else if (NetUtils.isThisHostLocal(server.getAdminHost())) {
                serversOnHost.add(new ServerPorts(domain, server));
            }
        }
    }

    private void createRemoteServerList() {
        for (Server server : allServers) {
            // no DAS!
            if (server.isInstance() && sameHost(server))
                serversOnHost.add(new ServerPorts(domain, server));
        }
    }

    private boolean sameHost(Server server) {
        return NetUtils.isEqual(server.getAdminHost(), host);
    }

    private Map<String, Integer> reassignPorts() throws TransactionFailure {
        // inefficient, probably a slicker way to do it.  Not worth the effort
        // there are at most 8 items...

        Map<String, Integer> portProps = newServerPorts.getMap();
        Map<String, Integer> changedPortProps = new HashMap<String, Integer>();
        Set<Map.Entry<String, Integer>> entries = portProps.entrySet();

        for (Map.Entry<String, Integer> entry : entries) {
            String name = entry.getKey();
            Integer num = entry.getValue();
            Integer newNum = reassignPort(num);

            if (!newNum.equals(num))
                changedPortProps.put(name, newNum);
        }
        return changedPortProps;
    }

    private Integer reassignPort(Integer num) throws TransactionFailure {
        int max = num + 100;

        while (num < max) {
            num = getNextUnassignedPort(num);

            if (isPortFree(num)) {
                allPorts.add(num);
                return num;
            }
            else
                ++num;
        }
        throw new TransactionFailure(Strings.get("PortManager.noFreePort"));
    }

    private Integer getNextUnassignedPort(Integer num) throws TransactionFailure {
        int max = num + MAX_PORT_TRIES;   // to avoid infinite loop

        for (int inum = num; inum < max; inum++) {
            if (!allPorts.contains(inum))
                return inum;
        }
        throw new TransactionFailure(Strings.get("PortManager.noFreePort", num, max));
    }

    private void changeSystemProperty(List<SystemProperty> sps, String name, String port) throws PropertyVetoException, TransactionFailure {
        for (SystemProperty sp : sps) {
            if (name.equals(sp.getName())) {
                sp.setValue(port);
                return;
            }
        }
        // does not exist -- let's add one!
        SystemProperty sp = newServer.createChild(SystemProperty.class);
        sp.setName(name);
        sp.setValue(port);
        sps.add(sp);
    }

    private boolean isPortFree(int num) {
        if (isLocal)
            return NetUtils.isPortFree(num);

        return NetUtils.isPortFree(host, num);
    }

    private void populateAllPorts() {

        for (ServerPorts sp : serversOnHost) {
            allPorts.addAll(sp.getMap().values());
        }
    }

    private final String serverName;
    private final Server newServer;
    private final boolean isLocal;
    private final Domain domain;
    private final String host;
    private final Set<Integer> allPorts;
    private final List<Server> allServers;
    private final List<ServerPorts> serversOnHost;
    private final ServerPorts newServerPorts;
    private static final int MAX_PORT_TRIES = 1100;
}
