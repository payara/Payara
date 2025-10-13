/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * 
 * Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
 */
package com.sun.enterprise.v3.admin.cluster;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.admin.CommandLock.LockType.NONE;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.StringUtils;

import fish.payara.nucleus.executorservice.PayaraExecutorService;


/**
 * AdminCommand to start the instance server.
 * 
 * <p>
 * <ul>
 *   <li>If this is DAS -- we call the instance
 *   <li>If this is an instance we start it
 * </ul>
 *
 * @author Carla Mott
 */
@Service(name = "start-instance")
@CommandLock(NONE) // don't prevent _synchronize-files
@PerLookup
@I18n("start.instance.command")
@RestEndpoints({
    @RestEndpoint(configBean = Server.class,
        opType = POST, 
        path = "start-instance", 
        description = "Start Instance",
        params = {
            @RestParam(name = "id", value = "$parent")
        })
})
public class StartInstanceCommand implements AdminCommand {

    @Param(name = "instance_name", primary = true)
    private String instanceName;

    @Param(optional = true, defaultValue = "normal", acceptableValues="none, normal, full")
    private String sync="normal";

    @Param(optional = true, defaultValue = "false")
    private boolean debug;

    @Param(optional = true, defaultValue = "false")
    private boolean terse;

    @Param(optional = true, obsolete = true)
    private String setenv;

    @Min(message = "Timeout must be at least 1 second long.", value = 1)
    @Param(optional = true, defaultValue = "120")
    private int timeout;
    
    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Nodes nodes;

    @Inject
    private ServerEnvironment env;

    @Inject
    private Servers servers;
    
    @Inject
    private PayaraExecutorService executor;

    private Logger logger;

    private Node   node;
    private String noderef;
    private String nodedir;
    private String nodeHost;
    private Server instance;

    private static final String NL = System.getProperty("line.separator");
    

    /**
     * restart-instance needs to try to start the instance from scratch if it is not
     * running.  We need to do some housekeeping first.
     * 
     * <p>
     * There is no clean way to do this through CommandRunner -- it is twisted together
     * with Grizzly parameters and so on.  So we short-circuit this way!
     * Do NOT make this public!!
     * @author Byron Nevins
     */
    StartInstanceCommand(ServiceLocator serviceLocator, String instanceName, boolean debug, ServerEnvironment env) {
        this.instanceName = instanceName;
        this.debug = debug;
        this.serviceLocator = serviceLocator;
        nodes = serviceLocator.getService(Nodes.class);

        // env:  neither getByType or getByContract works.  Not worth the effort
        //to find the correct magic incantation for HK2!
        this.env = env;
        servers = serviceLocator.getService(Servers.class);
    }

    /**
     * we have to declare this since HK2 needs it and we have another ctor
     * defined.
     */
    public StartInstanceCommand() {
    }

    @Override
    public void execute(AdminCommandContext ctx) {
        logger = ctx.getLogger();
        ActionReport report = ctx.getActionReport();
        String msg = "";
        report.setActionExitCode(FAILURE);

        if (!StringUtils.ok(instanceName)) {
            msg = Strings.get("start.instance.noInstanceName");
            logger.severe(msg);
            report.setMessage(msg);
            return;
        }
        
        instance = servers.getServer(instanceName);
        if (instance == null) {
            msg = Strings.get("start.instance.noSuchInstance", instanceName);
            logger.severe(msg);
            report.setMessage(msg);
            return;
        }

        if (instance.isRunning()) {
            msg = Strings.get("start.instance.already.running", instanceName);
            logger.info(msg);
            report.setMessage(msg);
            report.setActionExitCode(SUCCESS);
            return;
        }

        noderef = instance.getNodeRef();
        if (!StringUtils.ok(noderef)) {
            msg = Strings.get("missingNodeRef", instanceName);
            logger.severe(msg);
            report.setMessage(msg);
            return;
        }

        node = nodes.getNode(noderef);
        if (node != null) {
            nodedir = node.getNodeDirAbsolute();
            nodeHost = node.getNodeHost();
        } else {
            msg = Strings.get("missingNode", noderef);
            logger.severe(msg);
            report.setMessage(msg);
            return;
        }

        report.setActionExitCode(SUCCESS);
        if (env.isDas()) {
            startInstance(ctx);
        } else {
            msg = Strings.get("start.instance.notAnInstanceOrDas", env.getRuntimeType().toString());
            logger.severe(msg);
            report.setMessage(msg);
            report.setActionExitCode(FAILURE);
        }

        if (report.getActionExitCode() == SUCCESS) {
            // Make sure instance is really up
            if (!pollForLife(instance, executor, timeout)) {
                report.setMessage(Strings.get("start.instance.timeout", instanceName));
                report.setActionExitCode(FAILURE);
            }
        }
    }

    private void startInstance(AdminCommandContext ctx) {
        if (node.getType().equals("DOCKER")) {
            startDockerContainer(ctx);
            return;
        }

        NodeUtils nodeUtils = new NodeUtils(serviceLocator, logger);
        ArrayList<String> command = new ArrayList<String>();
        String humanCommand = null;

        command.add("start-local-instance");

        command.add("--node");
        command.add(noderef);

        if (nodedir != null) {
            command.add("--nodedir");
            command.add(nodedir); //XXX escape space?
        }

        command.add("--sync");
        command.add(sync);
        
        command.add("--timeout");
        command.add(timeout + "");
      
        if (debug) {
            command.add("--debug");
        }

        command.add(instanceName);

        // Convert the command into a string representing the command
        // a human should run.
        humanCommand = makeCommandHuman(command);

        // First error message displayed if we fail
        String firstErrorMessage = Strings.get("start.instance.failed", instanceName, noderef, nodeHost );

        StringBuilder output = new StringBuilder();

        // There is a problem on Windows waiting for IO to complete on a
        // child process which runs a long running grandchild. See IT 12777.
        boolean waitForReaderThreads = !OS.isWindows();

        // Run the command on the node and handle errors.
        nodeUtils.runAdminCommandOnNode(node, command, ctx, firstErrorMessage, humanCommand, output, waitForReaderThreads);

        ActionReport report = ctx.getActionReport();
        if (report.getActionExitCode() == SUCCESS) {
            // If it was successful say so and display the command output
            String msg = Strings.get("start.instance.success", instanceName, nodeHost);
            if (!terse) {
                msg = StringUtils.cat(NL, output.toString().trim(), msg);
            }
            report.setMessage(msg);
        }

    }

    private void startDockerContainer(AdminCommandContext adminCommandContext) {
        ParameterMap parameterMap = new ParameterMap();

        parameterMap.add("node", node.getName());
        parameterMap.add("instanceName", instanceName);

        CommandRunner commandRunner = serviceLocator.getService(CommandRunner.class);
        commandRunner.getCommandInvocation(
                "_start-docker-container", adminCommandContext.getActionReport(), adminCommandContext.getSubject())
                .parameters(parameterMap)
                .execute();
    }

    /**
     * Poll for the specified amount of time to check if the instance is running.
     * Returns whether the instance was started before the timeout.
     * 
     * @return true if the instance started up, or false otherwise.
     */
    static boolean pollForLife(Server instance, PayaraExecutorService executor, int timeout) {

        // Start a new thread to check when the instance has started
        CountDownLatch instanceTimeout = new CountDownLatch(1);
        ScheduledFuture<?> instancePollFuture = executor.scheduleWithFixedDelay(() -> {
            if (instance.isRunning()) {
                instanceTimeout.countDown();
            }
        }, 500, 500, MILLISECONDS);

        // If the timeout is reached, the instance isn't started so return false
        try {
            instanceTimeout.await(timeout, SECONDS);
        } catch (InterruptedException e) {
            return false;
        } finally {
            instancePollFuture.cancel(true);
        }
        
        return true;
    }

    private String makeCommandHuman(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        // Don't use file.separator since this is a local command
        // that may run on a different computer.  We don't necessarily know
        // what it is.

        fullCommand.append("lib/nadmin ");

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }
}
