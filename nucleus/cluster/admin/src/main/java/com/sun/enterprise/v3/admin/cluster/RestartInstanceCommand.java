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
import com.sun.enterprise.admin.remote.ServerRemoteRestAdminCommand;
import com.sun.enterprise.admin.util.*;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ObjectAnalyzer;
import com.sun.enterprise.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.glassfish.api.*;
import org.glassfish.api.admin.*;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author bnevins
 */
@Service(name = "restart-instance")
@PerLookup
@CommandLock(CommandLock.LockType.NONE) // don't prevent _synchronize-files
@I18n("restart.instance.command")
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "_restart-instance",
            description = "_restart-instance"),
    @RestEndpoint(configBean = Server.class,
            opType = RestEndpoint.OpType.POST,
            path = "restart-instance",
            description = "restart-instance",
            params = {
                @RestParam(name = "id", value = "$parent")
            })
})
public class RestartInstanceCommand implements AdminCommand {

    @Inject
    private InstanceStateService stateSvc;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Nodes nodes;

    @Inject
    private ServerEnvironment env;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config dasConfig;

    @Param(optional = false, primary = true)
    private String instanceName;

    // no default value!  We use the Boolean as a tri-state.
    @Param(name = "debug", optional = true)
    private String debug;

    @Param(name = "sync", optional = true, defaultValue = "normal", acceptableValues = "none, normal, full")
    private String sync;

    private Logger logger;

    private RemoteInstanceCommandHelper helper;

    private ActionReport report;

    private static final long WAIT_TIME_MS = 600000; // 10 minutes

    private Server instance;

    private String host;

    private int port;

    private String oldPid;

    private AdminCommandContext context;

    @Override
    public void execute(AdminCommandContext ctx) {
        try {
            context = ctx;
            helper = new RemoteInstanceCommandHelper(habitat);
            report = context.getActionReport();
            logger = context.getLogger();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

            // Each of the methods below immediately returns if there has been an error
            // This is just to avoid a ton of error-checking in this top-level method
            // i.e. it's for readability.

            if (!env.isDas())
                setError(Strings.get("restart.instance.notDas", env.getRuntimeType().toString()));

            prepare();
            setOldPid();
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Restart-instance old-pid = {0}", oldPid);
            callInstance();
            waitForRestart();

            if (!isError()) {
                String msg = Strings.get("restart.instance.success", instanceName);
                logger.info(msg);
                report.setMessage(msg);
            }
            synchronizeInstance();
        } catch (InstanceNotRunningException inre) {
            start();
        } catch (CommandException ce) {
            setError(Strings.get("restart.instance.racError", instanceName,
                    ce.getLocalizedMessage()));
        }
    }

    private void synchronizeInstance() {

        NodeUtils nodeUtils = new NodeUtils(habitat, logger);
        ArrayList<String> command = new ArrayList<>();
        String humanCommand;

        command.add("_synchronize-instance");
        if (sync != null) {
            command.add("--sync");
            command.add(sync);
        }
        if (instanceName != null) {
            command.add(instanceName);
        }

        // Convert the command into a string representing the command a human should run.
        humanCommand = makeCommandHuman(command);

        String noderef = instance.getNodeRef();
        String msg;
        String nodeHost;

        Node node = nodes.getNode(noderef);
        if (node != null) {
            nodeHost = node.getNodeHost();
        } else {
            msg = Strings.get("missingNode", noderef);
            logger.severe(msg);
            report.setMessage(msg);
            return;
        }

        // First error message displayed if we fail
        String firstErrorMessage = Strings.get("restart.instance.syncFailed", instanceName, noderef, nodeHost);

        StringBuilder output = new StringBuilder();

        // There is a problem on Windows waiting for IO to complete on a
        // child process which runs a long running grandchild. See IT 12777.
        boolean waitForReaderThreads = true;
        if (OS.isWindows()) {
            waitForReaderThreads = false;
        }

        // Run the command on the node and handle errors.
        nodeUtils.runAdminCommandOnNode(node, command, context, firstErrorMessage,
                humanCommand, output, waitForReaderThreads);

        if (report.getActionExitCode() == ActionReport.ExitCode.SUCCESS) {
            // If it was successful say so and display the command output
            msg = Strings.get("restart.instance.success", instanceName);
            report.setMessage(msg);
        }

    }

    private String makeCommandHuman(List<String> command) {
        StringBuilder fullCommand = new StringBuilder("lib/nadmin ");
        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }
        return fullCommand.toString();
    }

    private void prepare() throws InstanceNotRunningException {
        if (isError())
            return;

        if (!StringUtils.ok(instanceName)) {
            setError(Strings.get("stop.instance.noInstanceName"));
            return;
        }

        instance = helper.getServer(instanceName);

        if (instance == null) {
            setError(Strings.get("stop.instance.noSuchInstance", instanceName));
            return;
        }

        host = instance.getAdminHost();

        if (host == null) {
            setError(Strings.get("stop.instance.noHost", instanceName));
            return;
        }
        port = helper.getAdminPort(instance);

        if (port < 0) {
            setError(Strings.get("stop.instance.noPort", instanceName));
            return;
        }

        if (!isInstanceRestartable())
            setError(Strings.get("restart.notRestartable", instanceName));

        if (logger.isLoggable(Level.FINER))
            logger.finer(ObjectAnalyzer.toString(this));
    }

    /**
     * return null if all went OK...
     *
     */
    private void callInstance() throws CommandException {
        if (isError())
            return;

        String cmdName = "_restart-instance";

        RemoteRestAdminCommand rac = createRac(cmdName);
        // notice how we do NOT send in the instance's name as an operand!!
        ParameterMap map = new ParameterMap();

        if (debug != null)
            map.add("debug", debug);

        rac.executeCommand(map);
    }

    private boolean isInstanceRestartable() throws InstanceNotRunningException {
        if (isError())
            return false;

        String cmdName = "_get-runtime-info";

        RemoteRestAdminCommand rac;
        try {
            rac = createRac(cmdName);
            rac.executeCommand(new ParameterMap());
        }
        catch (CommandException ex) {
            // there is only one reason that _get-runtime-info would have a problem
            // namely if the instance isn't running.
            throw new InstanceNotRunningException();
        }

        String val = rac.findPropertyInReport("restartable");
        if (val != null && val.equals("false")) {
            return false;
    }
        return true;
    }

    private void waitForRestart() {
        if (isError())
            return;

        long deadline = System.currentTimeMillis() + WAIT_TIME_MS;

        while (System.currentTimeMillis() < deadline) {
            try {
                String newpid = getPid();

                // when the next statement is true -- the server has restarted.
                if (StringUtils.ok(newpid) && !newpid.equals(oldPid)) {
                    if (logger.isLoggable(Level.FINE))
                        logger.fine("Restarted instance pid = " + newpid);
                    return;
                }
            }
            catch (Exception e) {
                // ignore.  This is normal!
            }
        }
        setError(Strings.get("restart.instance.timeout", instanceName));
    }

    private RemoteRestAdminCommand createRac(String cmdName) throws CommandException {
        // I wonder why the signature is so unwieldy?
        // hiding it here...
        return new ServerRemoteRestAdminCommand(habitat, cmdName, host,
                port, false, "admin", null, logger);
    }

    private void setError(String s) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(s);
    }

    private void setSuccess(String s) {
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage(s);
    }

    private boolean isError() {
        return report.getActionExitCode() == ActionReport.ExitCode.FAILURE;
    }

    private void setOldPid() throws CommandException {
        if (isError())
            return;

        oldPid = getPid();

        if (!StringUtils.ok(oldPid))
            setError(Strings.get("restart.instance.nopid", instanceName));
    }

    private String getPid() throws CommandException {
        String cmdName = "_get-runtime-info";
        RemoteRestAdminCommand rac = createRac(cmdName);
        rac.executeCommand(new ParameterMap());
        return rac.findPropertyInReport("pid");
    }

    /* 
     * The instance is not running -- so let's try to start it.
     * There is no good way to call a Command on ourself.  So use the
     * command directly.
     * See issue 16322 for more details
     */
    private void start() {

        try {
            StartInstanceCommand sic = new StartInstanceCommand(habitat, instanceName, Boolean.parseBoolean(debug), env);
            sic.execute(context);
        }
        catch (Exception e) {
            // this is NOT normal!  start-instance communicates errors via the
            // reporter.  This catch should never happen.  It is here for robustness.
            // and especially for programmer/regression errors.

            // Perhaps a NPE or something **after** the report was set to success???
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }

        // save start-instance's message
        String messageFromStartCommand = report.getMessage();
        if (isError()) {
            setError(Strings.get("restart.instance.startFailed", messageFromStartCommand));
        } else {
            setSuccess(Strings.get("restart.instance.startSucceeded", messageFromStartCommand));
        }
    }

    private static class InstanceNotRunningException extends Exception {
    }

}
