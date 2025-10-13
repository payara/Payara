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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
 */

// Portions Copyright [2018-2025] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import fish.payara.admin.cluster.ExecutorServiceFactory;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.FINE;
import static org.glassfish.api.ActionReport.ExitCode.*;

/*
 * ClusterCommandHelper is a helper class that knows how to execute an
 * asadmin command in the DAS for each instance in a cluster. For example
 * it is used by start-cluster and stop-cluster to execute start-instance
 * or stop-instance for each instance in a cluster. Note this is not the
 * same as cluster replication where general commands are executed on
 * each instances of a cluster.
 *
 * @author Joe Di Pol
 */
public class ClusterCommandHelper {

    private static final String NL = System.getProperty("line.separator");
    private static final int ADMIN_DEFAULT_POOL_SIZE = 5;

    private final Domain domain;
    private final CommandRunner runner;

    private ProgressStatus progress;
    private long adminTimeout;

    /**
     * Construct a ClusterCommandHelper
     *
     * @param domain The Domain we are running in
     * @param runner A CommandRunner to use for running commands
     */
    public ClusterCommandHelper(Domain domain, CommandRunner runner) {
        this.domain = domain;
        this.runner = runner;
        this.adminTimeout = RemoteRestAdminCommand.getReadTimeout();
    }

    /**
     * Loop through all instances in a cluster and execute a command for each one.
     *
     * @param command    The string of the command to run. The instance name will be used as the operand for the command.
     * @param map        A map of parameters to use for the command. May be null if no parameters. When the command is executed for
     *                   a server instance, the instance name is set as the DEFAULT parameter (operand)
     * @param targetName The name of the cluster or deployment group containing the instances to run the command against.
     * @param context    The AdminCommandContext to use when executing the command.
     * @param verbose    true for more verbose output
     * @return An ActionReport containing the results
     * @throws CommandException
     */
    public ActionReport runCommand(String command, ParameterMap map, String targetName, AdminCommandContext context, boolean verbose) throws CommandException {
        return this.runCommand(command, map, targetName, context, verbose, false);

    }

    /**
     * Loop through all instances in a cluster and execute a command for each one.
     *
     * @param command    The string of the command to run. The instance name will be used as the operand for the command.
     * @param map        A map of parameters to use for the command. May be null if no parameters. When the command is executed for
     *                   a server instance, the instance name is set as the DEFAULT parameter (operand)
     * @param targetName The name of the cluster or deployment group containing the instances to run the command against.
     * @param context    The AdminCommandContext to use when executing the command.
     * @param verbose    true for more verbose output
     * @param rolling    Whether calls should be serialized to help with rolling restarts
     * @return An ActionReport containing the results
     * @throws CommandException
     */
    public ActionReport runCommand(String command, ParameterMap map, String targetName, AdminCommandContext context, boolean verbose, boolean rolling) throws CommandException {

        // When we started
        long startTime = System.currentTimeMillis();

        Logger logger = context.getLogger();
        ActionReport report = context.getActionReport();

        // Get the cluster specified by clusterName
        Cluster cluster = domain.getClusterNamed(targetName);
        if (cluster == null) {
            if (domain.getDeploymentGroupNamed(targetName) == null) {
                throw new CommandException(Strings.get("cluster.command.unknownCluster", targetName));
            }
        }

        // Get the list of servers in the cluster or deployment group.
        List<Server> targetServers = domain.getServersInTarget(targetName);

        // If the list of servers is empty, say so
        if (targetServers == null || targetServers.isEmpty()) {
            report.setActionExitCode(SUCCESS);
            report.setMessage(Strings.get("cluster.command.noInstances", targetName));
            return report;
        }
        int nInstances = targetServers.size();

        // We will save the name of the instances that worked and did
        // not work so we can summarize our results.
        StringBuilder failedServerNames = new StringBuilder();
        StringBuilder succeededServerNames = new StringBuilder();
        List<String> waitingForServerNames = new ArrayList<>();
        ReportResult reportResult = new ReportResult();
        boolean failureOccurred = false;
        progress = context.getProgressStatus();

        // Save command output to return in ActionReport
        StringBuilder output = new StringBuilder();

        // Optimize the oder of server instances to avoid clumping on nodes
        if (logger.isLoggable(FINE)) {
            logger.fine(String.format("Original instance list %s", serverListToString(targetServers)));
        }
        targetServers = optimizeServerListOrder(targetServers);

        // Holds responses from the threads running the command
        ArrayBlockingQueue<CommandRunnable> responseQueue = new ArrayBlockingQueue<>(nInstances);

        ExecutorServiceFactory.ExecutorServiceHolder holder = ExecutorServiceFactory.newFixedThreadPool(domain, nInstances, rolling);
        ExecutorService threadPool = holder.getExecutorService();

        if (map == null) {
            map = new ParameterMap();
        }


        logger.info(String.format(
                "Executing %s on %d instances using a thread pool of size %d: %s", command, nInstances, holder.getThreadPoolSize(),
                serverListToString(targetServers)));

        progress.setTotalStepCount(nInstances);
        progress.progress(Strings.get("cluster.command.executing", command, Integer.toString(nInstances)));

        // Loop through instance names, construct the command for each
        // instance name, and hand it off to the threadpool.
        for (Server server : targetServers) {
            String serverName = server.getName();
            waitingForServerNames.add(serverName);

            ParameterMap instanceParameterMap = new ParameterMap(map);
            // Set the instance name as the operand for the commnd
            instanceParameterMap.set("DEFAULT", serverName);

            ActionReport instanceReport = runner.getActionReport("plain");
            instanceReport.setActionExitCode(SUCCESS);
            CommandInvocation invocation = runner.getCommandInvocation(command, instanceReport, context.getSubject());
            invocation.parameters(instanceParameterMap);

            String msg = command + " " + serverName;
            logger.info(msg);
            if (verbose) {
                output.append(msg).append(NL);
            }

            // Wrap the command invocation in a runnable and hand it off
            // to the thread pool
            CommandRunnable cmdRunnable = new CommandRunnable(invocation, instanceReport, responseQueue);
            cmdRunnable.setName(serverName);
            threadPool.execute(cmdRunnable);
        }

        if (logger.isLoggable(FINE)) {
            logger.fine(String.format("%s commands queued, waiting for responses", command));
        }

        // Make sure we don't wait longer than the admin read timeout. Set
        // our limit to be 3 seconds less.
        adminTimeout = adminTimeout - 3000;
        if (adminTimeout <= 0) {
            // This should never be the case
            adminTimeout = 57 * 1000;
        }

        if (logger.isLoggable(FINE)) {
            logger.fine(String.format("Initial cluster command timeout: %d ms", adminTimeout));
        }

        // Now go get results from the response queue.
        for (int n = 0; n < nInstances; n++) {
            long timeLeft = adminTimeout - (System.currentTimeMillis() - startTime);
            if (timeLeft < 0) {
                timeLeft = 0;
            }
            CommandRunnable cmdRunnable = null;
            try {

                cmdRunnable = responseQueue.poll(timeLeft, MILLISECONDS);
            } catch (InterruptedException e) {
                // This thread has been interrupted. Abort
                threadPool.shutdownNow();
                String msg = Strings.get("cluster.command.interrupted", targetName, Integer.toString(n), Integer.toString(nInstances), command);
                logger.warning(msg);
                output.append(msg).append(NL);
                failureOccurred = true;
                // Re-establish interrupted state on thread
                Thread.currentThread().interrupt();
                break;
            }

            if (cmdRunnable == null) {
                // We've timed out.
                break;
            }
            String iname = cmdRunnable.getName();
            waitingForServerNames.remove(iname);
            ActionReport instanceReport = cmdRunnable.getActionReport();
            if (logger.isLoggable(FINE)) {
                logger.fine(String.format("Instance %d of %d (%s) has responded with %s", n + 1, nInstances, iname, instanceReport.getActionExitCode()));
            }

            if (instanceReport.getActionExitCode() != SUCCESS) {
                // Bummer, the command had an error. Log and save output
                failureOccurred = true;
                failedServerNames.append(iname).append(" ");
                reportResult.failedServerNames.add(iname);
                String msg = iname + ": " + instanceReport.getMessage();
                logger.severe(msg);
                output.append(msg).append(NL);
                msg = Strings.get("cluster.command.instancesFailed", command, iname);

                progress.progress(1, msg);
            } else {
                // Command worked. Note that too.
                succeededServerNames.append(iname).append(" ");
                reportResult.succeededServerNames.add(iname);
                progress.progress(1, iname);
            }
        }

        report.setActionExitCode(SUCCESS);

        if (failureOccurred) {
            report.setResultType(List.class, reportResult.failedServerNames);
        } else {
            report.setResultType(List.class, reportResult.succeededServerNames);
        }

        // Display summary of started servers if in verbose mode or we
        // had one or more failures.
        if (succeededServerNames.length() > 0 && (verbose || failureOccurred)) {
            output.append(NL).append(Strings.get("cluster.command.instancesSucceeded", command, succeededServerNames));
        }

        if (failureOccurred) {
            // Display summary of failed servers if we have any
            output.append(NL).append(Strings.get("cluster.command.instancesFailed", command, failedServerNames));
            if (succeededServerNames.length() > 0) {
                // At least one instance started. Warning.
                report.setActionExitCode(WARNING);
            } else {
                // No instance started. Failure
                report.setActionExitCode(FAILURE);
            }
        }

        // Check for server that did not respond
        if (!waitingForServerNames.isEmpty()) {
            String msg = Strings.get("cluster.command.instancesTimedOut", command, listToString(waitingForServerNames));
            logger.warning(msg);
            if (output.length() > 0) {
                output.append(NL);
            }
            output.append(msg);
            report.setActionExitCode(WARNING);
        }

        report.setMessage(output.toString());
        threadPool.shutdown();
        return report;
    }

    /**
     * Get the size of the admin threadpool
     */
    private int getAdminThreadPoolSize() {
        // Get the DAS configuratoin
        Config config = domain.getConfigNamed("server-config");
        if (config == null) {
            return ADMIN_DEFAULT_POOL_SIZE;
        }

        return new AdminEndpointDecider(config).getMaxThreadPoolSize();
    }

    /**
     * Optimize the order of the list of servers. Basically we want the server list to be ordered such that we rotate over
     * the nodes. For example we want: n1, n2, n3, n1, n2, n3 not: n1, n1, n2, n2, n3, n3. This is to spread the load of
     * operations across nodes.
     *
     * @param original a list of servers
     * @return a list of servers with a more optimal order
     */
    public List<Server> optimizeServerListOrder(List<Server> original) {

        // Don't bother with all this if it's just two instances
        if (original.size() < 3) {
            return original;
        }

        // There must be a more efficient way to do this, but this is what
        // we do. We first distribute all the server instances into a table
        // that is indexed by node name. Then we snake over that table to
        // create the final list.

        // Key is the node name, value is the list of servers on that node
        HashMap<String, List<Server>> serverTable = new HashMap<>();

        // Distribute servers into serverTable
        int count = 0;
        for (Server server : original) {
            String nodeName = server.getNodeRef();

            List<Server> serverList = serverTable.get(nodeName);
            if (serverList == null) {
                serverList = new ArrayList<>();
                serverTable.put(nodeName, serverList);
            }
            serverList.add(server);
            count++;
        }

        // Now snake through server table moving server entries from the
        // table to the final optimized list.
        List<Server> optimized = new ArrayList<>(count);
        Set<String> nodes = serverTable.keySet();
        while (count > 0) {
            for (String nodeName : nodes) {
                List<Server> serverList = serverTable.get(nodeName);
                if (!serverList.isEmpty()) {
                    optimized.add(serverList.remove(0));
                    count--;
                }
            }
        }

        return optimized;
    }

    private static String serverListToString(List<Server> servers) {
        StringBuilder serverListBuilder = new StringBuilder();
        for (Server server : servers) {
            serverListBuilder.append(server.getNodeRef())
                    .append(":")
                    .append(server.getName())
                    .append(" ");
        }

        return serverListBuilder.toString().trim();
    }

    private static String listToString(List<String> slist) {
        StringBuilder sb = new StringBuilder();
        for (String s : slist) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }

    public static class ReportResult {
        public final List<String> succeededServerNames = new ArrayList<>();
        public final List<String> failedServerNames = new ArrayList<>();
    }

    /**
     * Set the timeout for ClusterCommandHelper
     *
     * @param adminTimeout in milliseconds
     */
    public void setAdminTimeout(long adminTimeout) {
        this.adminTimeout = adminTimeout;
    }
}
