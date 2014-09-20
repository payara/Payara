/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ant.tasks;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

import java.io.*;

public class AdminTask extends Task {

    // default value for installdir?
    String installDir, command;

    public AdminTask() {
        setCommand("");
    }

    public void setTarget(String target) {
        optionIgnored("target");
    }

    public void setInstallDir(String installDir) {
        this.installDir = installDir;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public void addCommandParameter(String name, String value) {
        command += " --" + name + "=" + value;
    }

    public void addCommandOperand(String value) {
        command += " " + value;
    }

    public void setUser(String user) {
        addCommandParameter("user", user);
    }

    public void setPasswordFile(String passwordfile) {
        addCommandParameter("passwordfile", passwordfile);
    }

    public void setHost(String host) {
        addCommandParameter("host", host);
    }

    public void setPort(String port) {
        addCommandParameter("port", port);
    }

    public String getInstallDir() {
        if (installDir == null) {
            String home = getProject().getProperty("asinstall.dir");
            if (home != null) {
                return home;
            }
        }
        return installDir;
    }

    public void execute() throws BuildException {
        execute(this.command);
    }

	public void execute(String commandExec) throws BuildException {
        log ("Running command " + commandExec);
        String installDirectory = getInstallDir();
        if (installDirectory == null) {
            log("Install Directory of application server not known. Specify either the installDir attribute or the asinstall.dir property",
                Project.MSG_WARN);
            return;
        }

        File f = new File(installDirectory);
        if (!f.exists()) {
            log("Glassfish install directory : " + installDirectory + " not found. Specify the correct directory as installDir attribute or asinstall.dir property");
            return;
        }
        BufferedReader error = null;
        BufferedReader input = null;
        try {
            File asadmin = getAsAdmin(f);
            Process pr = Runtime.getRuntime().exec(asadmin.getAbsolutePath() + " " + commandExec);

            error = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
            String errorLine=null;
            while((errorLine=error.readLine()) != null) {
                log(errorLine);
            }

            input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String inputLine=null;
            while((inputLine=input.readLine()) != null) {
                log(inputLine);
            }

            int exitVal = pr.waitFor();
            if (exitVal != 0)
                log("asadmin command exited with error code "+exitVal);

        } catch (Exception ex) {
            log(ex.getMessage());
        }
        finally {
            if (error != null) {
                try {
                    error.close();
                }
                catch (Exception e) {
                    // nothing can be or should be done...
                }
            }
            if (input != null) {
                try {
                    input.close();
                }
                catch (Exception e) {
                    // nothing can be or should be done...
                }
            }
        }
    }

    void optionIgnored(String option) {
        log("Option Ignored : " + option);
    }

    private File getAsAdmin(File installDir) {
        String osName = System.getProperty("os.name");
        File binDir = new File(installDir, "bin");
        if (osName.indexOf("Windows") == -1) {
            return new File(binDir, "asadmin");
        } else {
            return new File(binDir, "asadmin.bat");
        }

    }
}
