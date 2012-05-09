/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.virtualization;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.virtualization.config.Action;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstraction to external command execution
 * @author Jerome Dochez
 */
@Service
public class ShellExecutor {

    final static boolean debug = false;

    @Inject
    CommandRunner commandRunner;

    @Inject @Optional
    Virtualizations virtualizations=null;

    @Inject
    ServerEnvironment env;

    final Logger logger = RuntimeContext.logger;

    public String output(Process pr) throws IOException {
        // dirty hack for the lazy
        StringBuilder sb = new StringBuilder();
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            while ((line=buf.readLine())!=null) {
                if (debug) System.out.println(line);
                sb.append(line);
            }
        } finally {
            try {
                if (buf!=null) buf.close();
            } catch(IOException ioe) {
                // ignore
            }
        }
        return sb.toString();
    }

    public String error(Process pr) throws IOException {
        // dirty hack for the lazy
        StringBuilder sb = new StringBuilder();
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String line = "";
            while ((line=buf.readLine())!=null) {
                if (debug) System.out.println(line);
                sb.append(line);
            }
        } finally {
            try {
                if (buf!=null) buf.close();
            } catch(IOException ioe) {
                // ignore
            }
        }
        return sb.toString();
    }

    public Process execute(File dir, ShellCommand command) throws IOException, InterruptedException {
        String commandString = command.build();
        return executeAndWait(dir, commandString);
    }

    public Process executeAndWait(File dir, String command) throws IOException, InterruptedException {
        Process pr = execute(dir, command);
        pr.waitFor();
        return pr;
    }

    public Process execute(File dir, String command) throws IOException, InterruptedException {
        Runtime run = Runtime.getRuntime();
        dir = dir==null?new File(System.getProperty("user.dir")):dir;
        return  run.exec(dir.getAbsolutePath() + "/" + command);
    }

    public void executionActions(ActionReport report, String provider, Action.Timing timing, ParameterResolver resolver) {

        if (virtualizations==null) return;
        // now we configure it with all cluster creation commands
        // cluster is now created, let's run the cluster creation related actions...
        for (Virtualization virtualization : virtualizations.getVirtualizations()) {
            if (provider.equals(virtualization.getName())) {
                for (Action action : virtualization.getActions()) {

                    if (action.getTiming().equals(timing.name())) {
                        String path = virtualization.getScriptsLocation();
                        if (path==null) {
                            path = (new File(env.getConfigDirPath(), provider)).getAbsolutePath();
                        }
                        // the script location might be an absolute path
                        File script = new File(action.getCommand());
                        if (!script.exists()) {
                            // not an absolute path, must be in our config provider directory
                            script = new File(path, action.getCommand());
                            if (!script.exists()) {
                                logger.log(Level.SEVERE, "Cannot find script " + action.getCommand() + " in " + path);
                                return;
                            }
                        }
                        ShellCommand command = new ShellCommand(path, action, resolver);
                        logger.info("Running " + command.build());
                        try {
                            Process result = execute(null, command);
                            if (result.exitValue()!=0) {
                                logger.info("Command failed with exit code " + result.exitValue());
                                String output = output(result);
                                if (output!=null)
                                    logger.severe(output);
                            }
                        } catch (InterruptedException e) {
                            report.failure(logger, "Interrupted exception while running " + command.build(), e);
                        } catch (IOException ioe) {
                            report.failure(logger, "IOException while running " + command.build(), ioe);
                        }
                    }
                }
            }
        }
    }

    public boolean executeAdminCommand(ActionReport report, String commandName, String operand, String... parameters) {

        ParameterMap params = new ParameterMap();
        if (operand!=null) {
            params.add("DEFAULT", operand);
        }
        for (int i=0;i<parameters.length;) {
            String key = parameters[i++];
            String value=null;
            if (i<parameters.length) {
                value = parameters[i++];
            }
            params.add(key, value);
        }
        CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(commandName, report);
        inv.parameters(params);
        inv.execute();
        return (report.getActionExitCode()==ActionReport.ExitCode.SUCCESS);
    }

    public void installScript(File destDir, String provider, String scriptName) throws IOException {

        URL url = getClass().getClassLoader().getResource(provider+"/"+scriptName);
        if (url==null) {
            return;
        }
        InputStream is=null;
        OutputStream os=null;
        try {
            is = url.openStream();
            File outDir = new File(destDir, provider);
            os = new BufferedOutputStream(new FileOutputStream(new File(outDir, scriptName)));
            byte[] mem = new byte[2048];
            int read=0;
            do {
                read = is.read(mem);
                if (read>0)
                    os.write(mem, 0, read);
            } while (read>0);
        } finally {
            if (is!=null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Cannot close input file", e);
                }
            }
            if (os!=null) {
                try {
                    os.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Cannot close output file", e);
                }
            }
        }

    }
}
