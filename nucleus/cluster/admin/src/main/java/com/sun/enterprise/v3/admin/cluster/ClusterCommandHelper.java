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


import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ProgressStatus;


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
class ClusterCommandHelper {

    private static final String NEWLINE = System.getProperty("line.separator");

    private final Domain domain;

    private final CommandRunner runner;

    private ProgressStatus progress;

    /**
     * Construct a ClusterCommandHelper
     *
     * @param domain The Domain we are running in
     * @param runner A CommandRunner to use for running commands
     */
    ClusterCommandHelper(Domain domain, CommandRunner runner) {
        this.domain = domain;
        this.runner = runner;
    }

    /**
     * Loop through all instances in a cluster and execute a command for
     * each one.
     *
     * @param command       The string of the command to run. The instance
     *                      name will be used as the operand for the command.
     * @param map           A map of parameters to use for the command. May be
     *                      null if no parameters. When the command is
     *                      executed for a server instance, the instance name
     *                      is set as the DEFAULT parameter (operand)
     * @param clusterName   The name of the cluster containing the instances
     *                      to run the command against.
     * @param context       The AdminCommandContext to use when executing the
     *                      command.
     * @param verbose       true for more verbose output
     * @return              An ActionReport containing the results
     * @throws CommandException
     */
    ActionReport runCommand(
            String  command,
            ParameterMap map,
            String  clusterName,
            AdminCommandContext context,
            boolean verbose) throws CommandException {

        // When we started
        final long startTime = System.currentTimeMillis();

        Logger logger = context.getLogger();
        ActionReport report = context.getActionReport();

        // Get the cluster specified by clusterName
        Cluster cluster = domain.getClusterNamed(clusterName);
        if (cluster == null) {
            String msg = Strings.get("cluster.command.unknownCluster",
                    clusterName);
            throw new CommandException(msg);
        }

        // Get the list of servers in the cluster.
        List<Server> targetServers = domain.getServersInTarget(clusterName);

        // If the cluster is empty, say so
        if (targetServers == null || targetServers.isEmpty()) {
            report.setActionExitCode(ExitCode.SUCCESS);
            report.setMessage(Strings.get("cluster.command.noInstances",
                                            clusterName));
            return report;
        }
        int nInstances = targetServers.size();

        // We will save the name of the instances that worked and did
        // not work so we can summarize our results.
        StringBuilder failedServerNames = new StringBuilder();
        StringBuilder succeededServerNames = new StringBuilder();
        List<String> waitingForServerNames = new ArrayList<String>();
        String msg;
        ReportResult reportResult = new ReportResult();
        boolean failureOccurred = false;
        progress = context.getProgressStatus();

        // Save command output to return in ActionReport
        StringBuilder output = new StringBuilder();


        // Optimize the oder of server instances to avoid clumping on nodes
        if (logger.isLoggable(Level.FINE))
            logger.fine(String.format("Original instance list %s",
                serverListToString(targetServers)));
        targetServers = optimizeServerListOrder(targetServers);

        // Holds responses from the threads running the command
        ArrayBlockingQueue<CommandRunnable> responseQueue = 
                    new ArrayBlockingQueue<CommandRunnable>(nInstances);

        // Make the thread pool use the smaller of the number of instances
        // or half the admin thread pool size.
        int adminThreadPoolSize = getAdminThreadPoolSize();
        int threadPoolSize = Math.min(nInstances, adminThreadPoolSize / 2);
        if (threadPoolSize < 1)
            threadPoolSize = 1;

        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        if (map == null) {
            map = new ParameterMap();
        }

        msg = String.format(
            "Executing %s on %d instances using a thread pool of size %d: %s",
            command, nInstances, threadPoolSize,
            serverListToString(targetServers));
        logger.info(msg);

         msg = Strings.get("cluster.command.executing",
                 command, Integer.toString(nInstances));
        progress.setTotalStepCount(nInstances);
        progress.progress(msg);

        // Loop through instance names, construct the command for each
        // instance name, and hand it off to the threadpool.
        for (Server server : targetServers) {
            String iname = server.getName();
            waitingForServerNames.add(iname);

            ParameterMap instanceParameterMap = new ParameterMap(map);
            // Set the instance name as the operand for the commnd
            instanceParameterMap.set("DEFAULT", iname);

            ActionReport instanceReport = runner.getActionReport("plain");
            instanceReport.setActionExitCode(ExitCode.SUCCESS);
            CommandInvocation invocation = runner.getCommandInvocation(
                        command, instanceReport, context.getSubject());
            invocation.parameters(instanceParameterMap);           

            msg = command + " " + iname;
            logger.info(msg);
            if (verbose) {
                output.append(msg).append(NEWLINE);
            }

            // Wrap the command invocation in a runnable and hand it off
            // to the thread pool
            CommandRunnable cmdRunnable = new CommandRunnable(invocation,
                    instanceReport, responseQueue);
            cmdRunnable.setName(iname);
            threadPool.execute(cmdRunnable);
        }

        if (logger.isLoggable(Level.FINE))
            logger.fine(String.format(
                "%s commands queued, waiting for responses", command));

        // Make sure we don't wait longer than the admin read timeout. Set
        // our limit to be 3 seconds less.
        long adminTimeout = RemoteRestAdminCommand.getReadTimeout() - 3000;
        if (adminTimeout <= 0) {
            // This should never be the case
            adminTimeout = 57 * 1000;
        }
        if (logger.isLoggable(Level.FINE))
            logger.fine(String.format("Initial cluster command timeout: %d ms",
                adminTimeout));

        // Now go get results from the response queue.
        for (int n = 0; n < nInstances; n++) {
            long timeLeft = adminTimeout - (System.currentTimeMillis() - startTime);
            if (timeLeft < 0) {
                timeLeft = 0;
            }
            CommandRunnable cmdRunnable = null;
            try {
                
                cmdRunnable = responseQueue.poll(timeLeft, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // This thread has been interrupted. Abort
                threadPool.shutdownNow();
                msg = Strings.get("cluster.command.interrupted", clusterName,
                        Integer.toString(n), Integer.toString(nInstances),
                        command);
                logger.warning(msg);
                output.append(msg).append(NEWLINE);
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
            if (logger.isLoggable(Level.FINE))
                logger.fine(String.format(
                    "Instance %d of %d (%s) has responded with %s",
                    n+1, nInstances, iname, instanceReport.getActionExitCode()));
            if (instanceReport.getActionExitCode() != ExitCode.SUCCESS) {
                // Bummer, the command had an error. Log and save output
                failureOccurred = true;
                failedServerNames.append(iname).append(" ");
                reportResult.failedServerNames.add(iname);
                msg = iname + ": " + instanceReport.getMessage();
                logger.severe(msg);
                output.append(msg).append(NEWLINE);
                msg = Strings.get("cluster.command.instancesFailed", command, iname);
                progress.progress(1, msg);
            } else {
                // Command worked. Note that too.
                succeededServerNames.append(iname).append(" ");
                reportResult.succeededServerNames.add(iname);
                progress.progress(1, iname);
            }
        }

        report.setActionExitCode(ExitCode.SUCCESS);
        
        if (failureOccurred) {
            report.setResultType(List.class, reportResult.failedServerNames);
        } else {
            report.setResultType(List.class, reportResult.succeededServerNames);
        }


        // Display summary of started servers if in verbose mode or we
        // had one or more failures.
        if (succeededServerNames.length() > 0 && (verbose || failureOccurred)) {
            output.append(NEWLINE).append(Strings.get("cluster.command.instancesSucceeded", command, succeededServerNames));
        }

        if (failureOccurred) {
            // Display summary of failed servers if we have any
            output.append(NEWLINE).append(Strings.get("cluster.command.instancesFailed", command, failedServerNames));
            if (succeededServerNames.length() > 0) {
                // At least one instance started. Warning.
                report.setActionExitCode(ExitCode.WARNING);
            } else {
                // No instance started. Failure
                report.setActionExitCode(ExitCode.FAILURE);
            }
        }

        // Check for server that did not respond
        if (!waitingForServerNames.isEmpty()) {
            msg = Strings.get("cluster.command.instancesTimedOut", command, listToString(waitingForServerNames));
            logger.warning(msg);
            if (output.length() > 0) {
                output.append(NEWLINE);
            }
            output.append(msg);
            report.setActionExitCode(ExitCode.WARNING);
        }

        report.setMessage(output.toString());
        threadPool.shutdown();
        return report;
    }

    /**
     * Get the size of the admin threadpool
     */
    private int getAdminThreadPoolSize() {

        final int DEFAULT_POOL_SIZE = 5;

        // Get the DAS configuratoin
        Config config = domain.getConfigNamed("server-config");
        if (config == null)
            return DEFAULT_POOL_SIZE;

        AdminEndpointDecider aed = new AdminEndpointDecider(config);
        return aed.getMaxThreadPoolSize();
    }

    /**
     * Optimize the order of the list of servers. Basically we
     * want the server list to be ordered such that we rotate over
     * the nodes. For example we want: n1, n2, n3, n1, n2, n3
     * not: n1, n1, n2, n2, n3, n3. This is to spread the load
     * of operations across nodes.
     *
     * @param original a list of servers
     * @return a list of servers with a more optimal order
     */
    List<Server> optimizeServerListOrder(List<Server> original) {

        // Don't bother with all this if it's just two instances
        if (original.size() < 3) {
            return original;
        }

        // There must be a more efficient way to do this, but this is what
        // we do. We first distribute all the server instances into a table
        // that is indexed by node name. Then we snake over that table to
        // create the final list.

        // Key is the node name, value is the list of servers on that node
        HashMap<String, List<Server>> serverTable =
                new HashMap<String, List<Server>>();

        // Distribute servers into serverTable
        int count = 0;
        for (Server server : original) {
            String nodeName = server.getNodeRef();

            List<Server> serverList = serverTable.get(nodeName);
            if (serverList == null) {
                serverList = new ArrayList<Server>();
                serverTable.put(nodeName, serverList);
            }
            serverList.add(server);
            count++;
        }

        // Now snake through server table moving server entries from the
        // table to the final optimized list.
        List<Server> optimized = new ArrayList<Server>(count);
        Set<String> nodes = serverTable.keySet();
        while (count > 0) {
            for (String nodeName : nodes) {
                List<Server> serverList = serverTable.get(nodeName);
                if (! serverList.isEmpty()) {
                    optimized.add(serverList.remove(0));
                    count--;
                }
            }
        }

        return optimized;
    }

    private static String serverListToString(List<Server> servers) {
        StringBuilder sb = new StringBuilder();
        for (Server s : servers) {
            sb.append(s.getNodeRef()).append(":").append(s.getName()).append(" ");
        }
        return sb.toString().trim();
    }

    private static String listToString(List<String> slist) {
        StringBuilder sb = new StringBuilder();
        for (String s : slist) {
            sb.append(s).append(" ");
        }
        return sb.toString().trim();
    }

    public static class ReportResult {
        public final List<String> succeededServerNames = new ArrayList<String>();
        public final List<String> failedServerNames = new ArrayList<String>();
    }
}
