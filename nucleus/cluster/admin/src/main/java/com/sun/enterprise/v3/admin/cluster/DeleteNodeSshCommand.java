/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.cluster;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;

import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;

/**
 * Remote AdminCommand to create a config node.  This command is run only on DAS.
 *  Register the config node on DAS
 *
 * @author Carla Mott
 */
@Service(name = "delete-node-ssh")
@I18n("delete.node.ssh")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
public class DeleteNodeSshCommand implements AdminCommand {
    private static final int DEFAULT_TIMEOUT_MSEC = 300000; // 5 minutes
    
    @Inject
    Habitat habitat;

    @Inject
    Node[] nodeList;

    @Inject
    Nodes nodes;

    @Inject
    private CommandRunner cr;
    
    @Param(name="name", primary = true)
    String name;
    
    @Param(optional = true, defaultValue = "false")
    boolean uninstall;

    @Param(optional = true, defaultValue = "false")
    boolean force;
    
    private String sshpassword = null;
    private String sshkeypassphrase = null;
    
    private static final String NL = System.getProperty("line.separator");    
    private Logger logger = null;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        logger = context.logger;
        Node node = nodes.getNode(name);

        if (node == null) {
            //no node to delete  nothing to do here
            String msg = Strings.get("noSuchNode", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if ((node.getType() != null) && !(node.getType().equals("SSH") )){
            //no node to delete  nothing to do here
            String msg = Strings.get("notSshNodeType", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;

        }
        
        ParameterMap info = new ParameterMap();
        
        if(uninstall) {
            //store needed info for uninstall
            SshConnector sshC = node.getSshConnector();
            SshAuth sshAuth = sshC.getSshAuth();
            
            if(sshAuth.getPassword() != null)
                info.add(NodeUtils.PARAM_SSHPASSWORD, sshAuth.getPassword());
            
            if(sshAuth.getKeyPassphrase() != null)
                info.add(NodeUtils.PARAM_SSHKEYPASSPHRASE, sshAuth.getKeyPassphrase());
            
            if(sshAuth.getKeyfile() != null)
                info.add(NodeUtils.PARAM_SSHKEYFILE, sshAuth.getKeyfile());
            
            info.add(NodeUtils.PARAM_INSTALLDIR, node.getInstallDir());
            info.add(NodeUtils.PARAM_SSHPORT, sshC.getSshPort());
            info.add(NodeUtils.PARAM_SSHUSER, sshAuth.getUserName());
            
            
            info.add(NodeUtils.PARAM_NODEHOST, node.getNodeHost());
            
        }
     
        CommandInvocation ci = cr.getCommandInvocation("_delete-node", report);
        ParameterMap map = new ParameterMap();
        map.add("DEFAULT", name);
        ci.parameters(map);
        ci.execute();
        
        //uninstall GlassFish after deleting the node
        if (uninstall) {
            boolean s = uninstallNode(context, info);
            if(!s && !force) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return; 
            }
        }
    }

    /**
     * Prepares for invoking uninstall-node on DAS
     * @param ctx command context
     * @return true if uninstall-node succeeds, false otherwise
     */
    private boolean uninstallNode(AdminCommandContext ctx, ParameterMap map) {
        boolean res = false;
        
        sshpassword = map.getOne(NodeUtils.PARAM_SSHPASSWORD);
        sshkeypassphrase = map.getOne(NodeUtils.PARAM_SSHKEYPASSPHRASE);
        
        ArrayList<String> command = new ArrayList<String>();

        command.add("uninstall-node");

        command.add("--installdir");
        command.add(map.getOne(NodeUtils.PARAM_INSTALLDIR));

        if (force) {
            command.add("--force");
        }
        
        command.add("--sshport");
        command.add(map.getOne(NodeUtils.PARAM_SSHPORT));
        
        command.add("--sshuser");
        command.add(map.getOne(NodeUtils.PARAM_SSHUSER));
        
        String key = map.getOne(NodeUtils.PARAM_SSHKEYFILE);
        
        if (key != null) {
            command.add("--sshkeyfile");
            command.add(key);
        }

        String host = map.getOne(NodeUtils.PARAM_NODEHOST);
        command.add(host);

        String firstErrorMessage = Strings.get("delete.node.ssh.uninstall.failed", host);
        StringBuilder out = new StringBuilder();
        int exitCode = execCommand(command, out);

        //capture the output in server.log
        logger.info(out.toString().trim());
        
        ActionReport report = ctx.getActionReport();
        if (exitCode == 0) {
            // If it was successful say so and display the command output
            String msg = Strings.get("delete.node.ssh.uninstall.success", host);
            report.setMessage(msg);
            res=true;
        } else {
            report.setMessage(firstErrorMessage);
        }
        return res;
    }

    /**
     * Invokes install-node using ProcessManager and returns the exit message/status.
     * @param cmdLine list of args
     * @param output contains output message
     * @return exit status of uninstall-node
     */
    private int execCommand(List<String> cmdLine, StringBuilder output) {
        int exit = -1;
        
        List<String> fullcommand = new ArrayList<String>();
        String installDir = nodes.getDefaultLocalNode().getInstallDirUnixStyle() + "/glassfish";
        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException(Strings.get("create.node.ssh.no.installdir"));
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        fullcommand.add(asadmin.getAbsolutePath());
        
        BufferedWriter out = null;
        File f = null;
        //if password auth is used by node, use the same auth mechanism for
        //uninstall-node as well. The passwords are passed using a temporary password file
        if(sshpassword != null) {        
            try {
                NodeUtils nu = new NodeUtils(habitat, logger);
                f = new File(System.getProperty("java.io.tmpdir"), "pass.tmp");
                out = new BufferedWriter(new FileWriter(f));
                out.newLine();
                out.write("AS_ADMIN_SSHPASSWORD=" + nu.sshL.expandPasswordAlias(sshpassword) + "\n");
                if(sshkeypassphrase != null)
                    out.write("AS_ADMIN_SSHKEYPASSPHRASE=" + nu.sshL.expandPasswordAlias(sshkeypassphrase) + "\n");
                out.flush();
            } catch (IOException ioe) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine("Failed to create password file: " + ioe.getMessage());
                }
                output.append(Strings.get("create.node.ssh.passfile.error"));
                return 1;
            }
            finally {
                try {
                    if (out != null)
                        out.close();
                } catch(final Exception ex){
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Failed to close stream: " + ex.getMessage());
                    }
                }
            }
            
            fullcommand.add("--passwordfile");
            fullcommand.add(f.getAbsolutePath());
        }
        
        fullcommand.addAll(cmdLine);
        
        ProcessManager pm = new ProcessManager(fullcommand);

        if(logger.isLoggable(Level.INFO)) {
            logger.info("Running command on DAS: " + commandListToString(fullcommand));
        }
        pm.setTimeoutMsec(DEFAULT_TIMEOUT_MSEC);

        if (logger.isLoggable(Level.FINER))
            pm.setEcho(true);
        else
            pm.setEcho(false);

        try {
            exit = pm.execute();            
        }
        catch (ProcessManagerException ex) {
            if(logger.isLoggable(Level.FINE)) {
                logger.fine("Error while executing command: " + ex.getMessage());
            }
            exit = 1;
        }

        String stdout = pm.getStdout();
        String stderr = pm.getStderr();
        
        if (output != null) {
            if (StringUtils.ok(stdout)) {
                output.append(stdout);
            }

            if (StringUtils.ok(stderr)) {
                if (output.length() > 0) {
                    output.append(NL);
                }
                output.append(stderr);
            }
        }
        
        if (f != null) {
            boolean didDelete = f.delete();
            if(!didDelete) {
                if(logger.isLoggable(Level.WARNING)) {
                    logger.log(Level.WARNING, Strings.get("node.ssh.passfile.delete.error", f.getPath()));
                }
            }
        }
        return exit;
    }
    
    private String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }
}
