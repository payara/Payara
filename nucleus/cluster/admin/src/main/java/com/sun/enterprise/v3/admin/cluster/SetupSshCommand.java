/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

/**
 * This is a remote command implementation of setup-ssh local command
 * The command invokes the setup-ssh local command using ProcessManager APIs
 * @author Yamini K B
 */
@Service(name = "_setup-ssh")
@I18n("setup.ssh")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
public class SetupSshCommand implements AdminCommand {
    @Param(name = "sshuser", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(optional = false, password = true)
    private String sshpassword;
    @Param(optional = true, password = true)
    private String sshkeypassphrase;
    @Param(optional = true, password = true)
    private String masterpassword;
    @Param(name = "sshport", optional = true, defaultValue = "22")
    private String port;
    @Param(optional = true)
    private String sshkeyfile;
    @Param(optional = true)
    private String sshpublickeyfile;
    @Param(optional = true, defaultValue = "false")
    private boolean generatekey;
    @Param(optional = false, primary = true, multiple = true)
    private String[] hosts;
    private Logger logger;
    
    @Inject
    Habitat habitat;
    
    @Inject
    Nodes nodes;
    
    static final String NL = System.getProperty("line.separator");
    static final int DEFAULT_TIMEOUT_MSEC = 300000; // 5 minutes

    @Override
    public final void execute(AdminCommandContext context) {
        logger = context.getLogger();

        ActionReport report = context.getActionReport();
        
        ArrayList<String> command = new ArrayList<String>();

        command.add("setup-ssh");

        command.add("--sshuser");
        command.add(user);

        command.add("--sshport");
        command.add(port);
        
        if (StringUtils.ok(sshkeyfile)) {
            if (!isAbsolutePath(sshkeyfile)) {
                report.setMessage(Strings.get("setup.ssh.invalid.path", sshkeyfile));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            
            command.add("--sshkeyfile");
            command.add(sshkeyfile);
        }
        
        if (StringUtils.ok(sshpublickeyfile)) {
            if (!isAbsolutePath(sshkeyfile)) {
                report.setMessage(Strings.get("setup.ssh.invalid.path", sshpublickeyfile));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            
            command.add("--sshpublickeyfile");
            command.add(sshpublickeyfile);
        }
        
        if (generatekey) {
            command.add("--generatekey");
        }
        
        command.addAll(Arrays.asList(hosts));

        StringBuilder out = new StringBuilder();
        
        int exitCode = execCommand(command, out);
        
        String output = out.toString().trim();
        //capture the output in server.log
        logger.info(output);

        report.setMessage(output);
        if (exitCode != 0) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
    
    /**
     * Invokes setup-ssh using ProcessManager and returns the exit message/status.
     * @param cmdLine list of args
     * @param output  contains output message
     * @return        exit status of setup-ssh
     *
     * This method was copied over from CreateNodeSshCommand on 9/14/11
     */
    final int execCommand(List<String> cmdLine, StringBuilder output) {
        int exit = -1;
        List<String> fullcommand = new ArrayList<String>();
        String installDir = nodes.getDefaultLocalNode().getInstallDirUnixStyle() + "/glassfish";

        if (!StringUtils.ok(installDir)) {
            throw new IllegalArgumentException(Strings.get("setup.ssh.no.installdir"));
        }

        File asadmin = new File(SystemPropertyConstants.getAsAdminScriptLocation(installDir));
        
        if (!asadmin.canExecute())
            throw new IllegalArgumentException(asadmin.getAbsolutePath() + " is not executable.");
        
        fullcommand.add(asadmin.getAbsolutePath());

        // passwords are passed directly through input stream
        List<String> pass = new ArrayList<String>();

        fullcommand.add("--passwordfile");
        fullcommand.add("-");
        pass = getPasswords();


        fullcommand.add("--interactive=false");
        fullcommand.addAll(cmdLine);

        ProcessManager pm = new ProcessManager(fullcommand);
        if (!pass.isEmpty())
            pm.setStdinLines(pass);

        if (logger.isLoggable(Level.INFO)) {
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
            if (logger.isLoggable(Level.FINE)) {
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
        return exit;
    }

    final String commandListToString(List<String> command) {
        StringBuilder fullCommand = new StringBuilder();

        for (String s : command) {
            fullCommand.append(" ");
            fullCommand.append(s);
        }

        return fullCommand.toString();
    }
    
    List<String> getPasswords() {
        List list = new ArrayList<String>();
        NodeUtils nodeUtils = new NodeUtils(habitat, logger);
        list.add("AS_ADMIN_SSHPASSWORD=" + nodeUtils.sshL.expandPasswordAlias(sshpassword));

        if (sshkeypassphrase != null) {
            list.add("AS_ADMIN_SSHKEYPASSPHRASE=" + nodeUtils.sshL.expandPasswordAlias(sshkeypassphrase));
        }
        
        if (masterpassword != null) {
            list.add("AS_ADMIN_MASTERPASSWORD=" + nodeUtils.sshL.expandPasswordAlias(masterpassword)); 
        }
        return list;
    }
    
    boolean isAbsolutePath(String path) {
        boolean ret = false;
        File f = new File(path);
        if (f.isAbsolute())
            ret = true;
        return ret;
    }
}
