/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.enterprise.admin.util.RemoteInstanceCommandHelper;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.cluster.InstanceInfo;
import static com.sun.enterprise.v3.admin.cluster.Constants.*;
import java.util.*;
import java.util.logging.*;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * AdminCommand to list all instances and their states
 *
 * This is so clumsy & hard to remember I leave it here as a comment:
 * @Inject(name = ServerEnvironment.DEFAULT_INSTANCE_NAME)
 * @author Byron Nevins
 */
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@Service(name = "list-instances")
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.instances.command")
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.GET,
        path="list-instances",
        description="List Cluster Instances",
        params={
            @RestParam(name="id", value="$parent")
        }),
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET,
        path="list-instances",
        description="List Instances")
})
public class ListInstancesCommand implements AdminCommand {

    @Inject
    private ServiceLocator habitat;
    @Inject
    private Domain domain;
    @Inject
    private ServerEnvironment env;
    @Inject
    private Servers allServers;
    @Inject
    InstanceStateService stateService;
    @Param(optional = true, defaultValue = "false", name = "long", shortName = "l")
    private boolean long_opt;
    @Param(optional = true, defaultValue = "60000")
    private String timeoutmsec;
    @Param(optional = true, defaultValue = "false")
    private boolean standaloneonly;
    @Param(optional = true, defaultValue = "false")
    private boolean nostatus;

    // We are setting the whichTarget to an empty String because FindBugs - LL
    // does not understand that HK2 is going to set it to a Sting for us later.
    // This garbage empty String will be replaced by HK2 soon...
    @Param(optional = true, primary = true, defaultValue = "domain")
    String whichTarget = "";

    private List<InstanceInfo> infos = new LinkedList<InstanceInfo>();
    private List<Server> serverList;
    private ActionReport report;
    private ActionReport.MessagePart top = null;
    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        // setup
        int timeoutInMsec;
        try {
            timeoutInMsec = Integer.parseInt(timeoutmsec);
        }
        catch (Exception e) {
            timeoutInMsec = 60000;
        }

        report = context.getActionReport();
        top = report.getTopMessagePart();

        Logger logger = context.getLogger();

        if (!validateParams()) {
            return;
        }

        serverList = createServerList();

        if (serverList == null) {
            fail(Strings.get("list.instances.badTarget", whichTarget ));
            return;
        }
        // Require that we be a DAS
        if (!env.isDas()) {
            String msg = Strings.get("list.instances.onlyRunsOnDas");
            logger.warning(msg);
            fail(msg);
            return;
        }

        if (nostatus) {
            noStatus(serverList);
        }
        else {
            yesStatus(serverList, timeoutInMsec, logger);
        }

        report.setActionExitCode(ExitCode.SUCCESS);
    }

    private void noStatus(List<Server> serverList) {
        if (serverList.isEmpty()) {
            report.setMessage(NONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        boolean firstServer = true;
        Properties extraProps = new Properties();
        List instanceList = new ArrayList();

        for (Server server : serverList) {
            boolean clustered = server.getCluster() != null;

            if (standaloneonly && clustered) {
                continue;
            }

            String name = server.getName();

            if (notDas(name)) {
                if (firstServer) {
                    firstServer = false;
                }
                else {
                    sb.append(EOL);
                }

                sb.append(name);
                top.addProperty(name, "");
                HashMap<String, Object> insDetails = new HashMap<String, Object>();
                insDetails.put("name", name);
                instanceList.add(insDetails);
            }
        }
        extraProps.put("instanceList", instanceList);
        report.setMessage(sb.toString());
        report.setExtraProperties(extraProps);
    }

    private boolean notDas(String name) {
        return !SystemPropertyConstants.DAS_SERVER_NAME.equals(name);
    }

    private void yesStatus(List<Server> serverList, int timeoutInMsec, Logger logger) {
        // Gather a list of InstanceInfo -- one per instance in domain.xml
        RemoteInstanceCommandHelper helper = new RemoteInstanceCommandHelper(habitat);

        for (Server server : serverList) {
            boolean clustered = server.getCluster() != null;
            int port = helper.getAdminPort(server);
            String host = server.getAdminHost();

            if (standaloneonly && clustered) {
                continue;
            }

            String name = server.getName();

            if (name == null) {
                continue;   // can this happen?!?
            }

            Cluster cluster = domain.getClusterForInstance(name);
            String clusterName = (cluster != null) ? cluster.getName() : null;
            // skip DAS
            if (notDas(name)) {
                ActionReport tReport = habitat.getService(ActionReport.class, "html");
                InstanceInfo ii = new InstanceInfo(
                        habitat,
                        server,
                        port,
                        host,
                        clusterName,
                        logger,
                        timeoutInMsec,
                        tReport,
                        stateService);
                infos.add(ii);
            }
        }
        if (infos.isEmpty()) {
            report.setMessage(NONE);
            return;
        }

        Properties extraProps = new Properties();
        List instanceList = new ArrayList();

        for (InstanceInfo ii : infos) {
            String name = ii.getName();
            String value = (ii.isRunning()) ? InstanceState.StateType.RUNNING.getDescription()
                    : InstanceState.StateType.NOT_RUNNING.getDescription();
            InstanceState.StateType state = (ii.isRunning())
                    ? (stateService.setState(name, InstanceState.StateType.RUNNING, false))
                    : (stateService.setState(name, InstanceState.StateType.NOT_RUNNING, false));
            List<String> failedCmds = stateService.getFailedCommands(name);
            if (state == InstanceState.StateType.RESTART_REQUIRED) {
                if (ii.isRunning()) {
                    value = InstanceState.StateType.RESTART_REQUIRED.getDescription();
                }
            }

            HashMap<String, Object> insDetails = new HashMap<String, Object>();
            insDetails.put("name", name);
            insDetails.put("status", value);
            if (state == InstanceState.StateType.RESTART_REQUIRED) {
                insDetails.put("restartReasons", failedCmds);
            }
            if (ii.isRunning()) {
                insDetails.put("uptime", ii.getUptime());
            }
            instanceList.add(insDetails);
        }
        extraProps.put("instanceList", instanceList);
        report.setExtraProperties(extraProps);

        if (long_opt) {
            report.setMessage(InstanceInfo.format(infos));
        }
        else {
            report.setMessage(InstanceInfo.formatBrief(infos));
        }
    }

    /*
     * return null means the whichTarget is garbage
     * return empty list means the whichTarget was an empty cluster
     */
    private List<Server> createServerList() {
        // 1. no whichTarget specified
        if (!StringUtils.ok(whichTarget))
            return allServers.getServer();

        ReferenceContainer rc = domain.getReferenceContainerNamed(whichTarget);
        // 2. Not a server or a cluster. Could be a config or a Node
        if (rc == null) {
            return getServersForNodeOrConfig();
        }
        else if (rc.isServer()) {
            List<Server> l = new LinkedList<Server>();
            l.add((Server) rc);
            return l;
        }
        else if (rc.isCluster()) { // can't be anything else currently! (June 2010)
            Cluster cluster = (Cluster) rc;
            return cluster.getInstances();
        }
        else {
            return null;
        }
    }

    private List<Server> getServersForNodeOrConfig() {
        if (whichTarget == null)
            throw new NullPointerException("impossible!");

        List<Server> list = getServersForNode();

        if (list == null) {
            list = getServersForConfig();
        }

        return list;
    }

    private List<Server> getServersForNode() {
        if (whichTarget == null) // FindBugs can't figure out that our caller already checked.
            throw new NullPointerException("impossible!");

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
        if (!foundNode) {
            return null;
        }
        else {
            return domain.getInstancesOnNode(whichTarget);
        }
    }

    private List<Server> getServersForConfig() {
        Config config = domain.getConfigNamed(whichTarget);

        if (config == null) {
            return null;
        }

        List<ReferenceContainer> rcs = domain.getReferenceContainersOf(config);
        List<Server> servers = new LinkedList<Server>();

        for (ReferenceContainer rc : rcs) {
            if (rc.isServer()) {
                servers.add((Server) rc);
            }
        }

        return servers;
    }

    /*
     * false means error
     */
    private boolean validateParams() {
        // another sort of weird scenario is that if the whichTarget is set to "domain",
        // that means ALL instances in the domains.  To make life easier -- we just
        //set the whichTarget to zilch to signal all instances in domain

        if ("domain".equals(whichTarget)) {
            whichTarget = null;
        }

        // standaloneonly AND a whichTarget are mutually exclusive
        if (standaloneonly && StringUtils.ok(whichTarget)) {
            fail(Strings.get("list.instances.targetWithStandaloneOnly"));
            return false;
        }

        // long_opt is not allowed with nostatus.
        // It could be allowed in the future if desired but the table code needs
        // to change.
        if (long_opt && nostatus) {
            fail(Strings.get("list.instances.longAndNoStatus"));
            return false;
        }

        // details details details!
        // if the whichTarget is the weird screwy "server" then fail.
        // TODO - we *could* show DAS status in the future but it's stupid
        // since this command ONLY runs on DAS -- it is obviously running!!

        if (!notDas(whichTarget)) {
            fail(Strings.get("list.instances.serverTarget"));
            return false;
        }
        return true;
    }

    // avoid ugly boilerplate...
    private void fail(String s) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(s);
    }
}
