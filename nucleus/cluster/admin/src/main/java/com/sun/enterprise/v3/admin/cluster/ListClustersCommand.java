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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.admin.util.InstanceStateService;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.cluster.InstanceInfo;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.InstanceState;
import org.glassfish.api.admin.config.ReferenceContainer;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import com.sun.enterprise.admin.util.RemoteInstanceCommandHelper;
import org.glassfish.api.admin.*;

/**
 *  This is a remote command that lists the clusters.
 * Usage: list-clusters

 * @author Bhakti Mehta
 * @author Byron Nevins
 */
@Service(name = "list-clusters")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.clusters.command")
@RestEndpoints({
    @RestEndpoint(configBean=Clusters.class,
        opType=RestEndpoint.OpType.GET,
        path="list-clusters",
        description="List Clusters")
})
public final class ListClustersCommand implements AdminCommand {

    @Inject
    private ServiceLocator habitat;
    @Inject
    Domain domain;
    @Inject
    InstanceStateService stateService;

    private static final String NONE = "Nothing to list.";
    private static final String EOL = "\n";

    @Param(optional = true, primary = true, defaultValue = "domain")
    String whichTarget;

    @Inject
    private Clusters allClusters;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        Logger logger = context.getLogger();
        ActionReport.MessagePart top = report.getTopMessagePart();

        List<Cluster> clusterList = null;
        //Fix for issue 13057 list-clusters doesn't take an operand
        //defaults to domain
        if (whichTarget.equals("domain" )) {
            Clusters clusters = domain.getClusters();
            clusterList = clusters.getCluster();
        } else {

            clusterList = createClusterList();

            if (clusterList == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(Strings.get("list.instances.badTarget", whichTarget));
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        if (clusterList.isEmpty()) {
            sb.append(NONE);
        }

        int timeoutInMsec = 2000;
        Map<String,ClusterInfo> clusterMap = new HashMap<String,ClusterInfo>();

        List<InstanceInfo> infos = new LinkedList<InstanceInfo>();
        for (Cluster cluster : clusterList) {
            String clusterName = cluster.getName();
            List<Server> servers = cluster.getInstances();

            if (servers.isEmpty()) {
                ClusterInfo ci = clusterMap.get(clusterName);
                if (ci == null ) {
                    ci = new ClusterInfo(clusterName);

                }
                ci.serversEmpty = true;
                clusterMap.put(clusterName,ci);
            }
            //Fix for issue 16273 create all InstanceInfos which will ping the instances
            //Then check the status for them
            for (Server server : servers) {
                String name = server.getName();

                if (name != null) {
                    ActionReport tReport = habitat.getService(ActionReport.class, "html");
                    InstanceInfo ii = new InstanceInfo(
                            habitat, server,
                            new RemoteInstanceCommandHelper(habitat).getAdminPort(server),
                            server.getAdminHost(),
                            clusterName, logger, timeoutInMsec, tReport, stateService);
                    infos.add(ii);
                }
            }
        }

        for(InstanceInfo ii : infos) {

            String clusterforInstance = ii.getCluster();
            ClusterInfo ci = clusterMap.get(clusterforInstance);
            if (ci == null ) {
                ci = new ClusterInfo(clusterforInstance);
            }
            ci.allInstancesRunning &= ii.isRunning();

            if (ii.isRunning()) {
                ci.atleastOneInstanceRunning = true;
            }

            clusterMap.put(clusterforInstance,ci);
        }

        //List the cluster and also the state
        //A cluster is a three-state entity and
        //list-cluster should return one of the following:

        //running (all instances running)
        //not running (no instance running)
        //partially running (at least 1 instance is not running)

        String display;
        String value ;
        for(ClusterInfo ci : clusterMap.values()) {

            if (ci.serversEmpty ||  !ci.atleastOneInstanceRunning) {
                display = InstanceState.StateType.NOT_RUNNING.getDisplayString();
                value = InstanceState.StateType.NOT_RUNNING.getDescription();
            }
            else if (ci.allInstancesRunning) {
                display = InstanceState.StateType.RUNNING.getDisplayString();
                value = InstanceState.StateType.RUNNING.getDescription();
            }
            else {
                display = Constants.PARTIALLY_RUNNING_DISPLAY;
                value = Constants.PARTIALLY_RUNNING;
            }
            sb.append(ci.getName()).append(display).append(EOL);
            top.addProperty(ci.getName(), value);

        }

        String output = sb.toString();
        //Fix for isue 12885
        report.setMessage(output.substring(0,output.length()-1 ));
    }

    /*
    * if target was junk then return all the clusters
    */
    private List<Cluster> createClusterList() {
        // 1. no whichTarget specified
        if (!StringUtils.ok(whichTarget))
            return allClusters.getCluster();

        ReferenceContainer rc = domain.getReferenceContainerNamed(whichTarget);
        // 2. Not a server or a cluster. Could be a config or a Node
        if (rc == null) {
            return getClustersForNodeOrConfig();
        }
        else if (rc.isServer()) {
            Server s =((Server) rc);
            List<Cluster> cl = new LinkedList<Cluster>();
            cl.add(s.getCluster());
            return  cl;
        }
        else if (rc.isCluster()) {
            Cluster cluster = (Cluster) rc;
            List<Cluster> cl = new LinkedList<Cluster>();
            cl.add(cluster);
            return cl;
        }
        else
            return null;
    }

     private List<Cluster> getClustersForNodeOrConfig() {
        if (whichTarget == null)
            throw new NullPointerException("impossible!");

        List<Cluster> list = getClustersForNode();

        if (list == null)
            list = getClustersForConfig();

        return list;
    }

     private List<Cluster> getClustersForNode() {
        boolean foundNode = false;
        Nodes nodes = domain.getNodes();

        if (nodes != null) {
            List<Node> nodeList = nodes.getNode();
            if (nodeList != null) {
                for (Node node : nodeList) {
                    if (whichTarget.equals(node.getName())) {
                        foundNode = true;
                        break;
                    }
                }
            }
        }
         if (!foundNode)
             return null;
         else
             return domain.getClustersOnNode(whichTarget);
    }

     private List<Cluster> getClustersForConfig() {
        Config config = domain.getConfigNamed(whichTarget);

        if (config == null)
            return null;

        List<ReferenceContainer> rcs = domain.getReferenceContainersOf(config);
        List<Cluster> clusters = new LinkedList<Cluster>();

        for (ReferenceContainer rc : rcs)
            if (rc.isCluster())
                clusters.add((Cluster) rc);

        return clusters;
    }

     private static class ClusterInfo {

        private boolean atleastOneInstanceRunning = false;
        private boolean allInstancesRunning = true;
        private boolean serversEmpty = false;
        private String name;

        public String getName() {
            return name;
        }

        private ClusterInfo (String name) {
            this.name = name;
        }

    }
}


