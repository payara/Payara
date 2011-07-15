/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.installer.conf;

import java.io.Writer;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.installer.util.GlassFishUtils;
import org.openinstaller.util.ClassUtils;
import org.openinstaller.util.ExecuteCommand;
import org.openinstaller.util.Msg;

/** Manages glassfish domain related operations.
 * Operations such as start/stop/delete domain are not exposed
 * through installer hence are not implemented in this class yet.
 * This utility class is invoked when the user chooses to create a
 * domain.
 * Holds reference to Product object to get product wide information.
 *
 * @author sathyan
 */
public class DomainManager {

    /* Reference to Product to obtain installation directory
     * and path to administration script.
     */
    private final Product productRef;
    /* Holds asadmin command output including the command line
     * of the recent runs. Gets overwritten on repeated calls to
     * asadmin commands. This text content will be used to construct
     * summary panel that displays the status/results of user configuration
     * actions.
     */
    private String outputFromRecentRun;
    /* Holds status of the configuration. Currently not used as createDomain
     * returns a valid Domain object when the configuation is successful.
     * Can be used to double check in the calling code to make sure that
     * configuration was indeed successful/failure.
     */
    private static boolean domainConfigSuccessful;

    /* Logger related. */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    /* @return true/false, the value of the overall configuration status flag. */
    public boolean isDomainConfigSuccessful() {
        return domainConfigSuccessful;
    }

    public DomainManager(Product productRef) {
        this.productRef = productRef;
        outputFromRecentRun = null;
    }

    /* Creates the domain by invoking asadmin's create-domain command.
     * @param domainName, name of the domain to create.
     * @param domainRoot, installation location of the domain.
     * @param instancePort, HTTP port.
     * @param adminPort, Admin Port to get into console.
     * @param saveLogin, default to false.
     * @param checkPorts, default to true.
     * @param adminUser, username for administering the server.
     * @param adminPassword, password to be used for administrator logins.
     * @param runningMode, "DRYRUN"/"REAL" "DRYRUN" mode would just return the
     * commandline and not execute it.
     * @return Domain, the newly created Domain Object, null if the creation fails.
     */
    public Domain createDomain(String domainName, String domainRoot,
            String instancePort, String adminPort,
            boolean saveLogin, boolean checkPorts,
            String adminUser, String adminPassword,
            String runningMode) {


        domainConfigSuccessful = true;
        char[] masterPasswordArray = {'c','h','a','n','g','e','i','t'};

        /* Setup domain attributes. */
        Domain glassfishDomain = new Domain(domainName, domainRoot, instancePort,
                adminPort, saveLogin, checkPorts, adminUser, adminPassword.toCharArray(), masterPasswordArray);

        /* Setup value for --domainproperties of create-domain command. */
        glassfishDomain.setDomainProperties(GlassFishUtils.getDomainProperties(adminPort, instancePort, glassfishDomain.getGlassfishPortBases()));

        /* Create password file content. */
        PasswordFile pFile = new PasswordFile(glassfishDomain.getAdminPassword(),
                glassfishDomain.getMasterPassword());

        /* Update the password file content with actual passwords. */
        pFile.setupPasswordFile();
       
        /* Assemble the create-domain command line. */
        String[] asadminExecuteCommand =
                GlassFishUtils.assembleCreateDomainCommand(productRef, glassfishDomain);

        outputFromRecentRun = "";
        if (asadminExecuteCommand != null) {
            LOGGER.log(Level.INFO, Msg.get("CREATE_DOMAIN", new String[]{domainName}));
            String asadminExecuteCommandExpanded = "";
            for(int i=0; i<asadminExecuteCommand.length; i++) {
               asadminExecuteCommandExpanded += asadminExecuteCommand[i] + " ";
            }

            outputFromRecentRun += asadminExecuteCommandExpanded;

            if (runningMode.contains("DRYRUN")) {
                /*
                Do not execute the command, this is useful when the clients just
                wanted the actual commandline and not execute the command.*/
                return glassfishDomain;
            }
            
            final long JOIN_TIMEOUT = 4000;
            int exitValue = 0;

            try {

                //Use upgrade tool execute implementation instead of ExecuteCommand since latter
                //does not properly process stdin 

                // start process and threads to watch output
                Process proc = Runtime.getRuntime().exec(asadminExecuteCommand);
                StreamWatcher errWatcher =
                    new StreamWatcher(proc.getErrorStream(), "ERR");
                StreamWatcher outWatcher =
                   new StreamWatcher(proc.getInputStream(), "OUT");
                errWatcher.start();
                outWatcher.start();

                // pass password file content to asadmin

                Writer writer = new BufferedWriter(
                    new OutputStreamWriter(proc.getOutputStream()));
                
                for (char c : pFile.getFileContent()) {
                    writer.write(c);
                }
                writer.close();
                pFile.emptyPasswordArrays();

                // wait for asadmin to finish

                exitValue = proc.waitFor();
                errWatcher.join(JOIN_TIMEOUT);
                outWatcher.join(JOIN_TIMEOUT);

                outputFromRecentRun += outWatcher.getOutput();
                outputFromRecentRun += errWatcher.getOutput();

                if (exitValue != 0) {
                    domainConfigSuccessful = false;
                    glassfishDomain = null;
                }

            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
                glassfishDomain = null;
                domainConfigSuccessful = false;
            }
        } else {
            outputFromRecentRun = Msg.get("INVALID_CREATE_DOMAIN_COMMAND_LINE");
            domainConfigSuccessful = false;
            glassfishDomain = null;
        }
        
        LOGGER.log(Level.INFO, outputFromRecentRun);
        return glassfishDomain;
    }

    public boolean deleteDomain(String domainName) {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));
    }

    public boolean stopDomain(String domainName) {
        throw new UnsupportedOperationException(Msg.get("NOT_SUPPORTED_YET"));

    }

    public void startDomain(String domainName, String runningMode) {
        /* Assemble the create-domain command line. */
        ExecuteCommand asadminExecuteCommand =
                GlassFishUtils.assembleStartDomainCommand(productRef, domainName);

        outputFromRecentRun = "";
        if (asadminExecuteCommand != null) {
            LOGGER.log(Level.INFO, Msg.get("START_DOMAIN", new String[]{domainName}));

            outputFromRecentRun += asadminExecuteCommand.expandCommand(asadminExecuteCommand.getCommand()) + "\n";

            if (runningMode.contains("DRYRUN")) {
                /*
                Do not execute the command, this is useful when the clients just
                wanted the actual commandline and not execute the command.*/
                return;
            }

            try {
                asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
                asadminExecuteCommand.setCollectOutput(true);
                asadminExecuteCommand.execute();
                outputFromRecentRun += asadminExecuteCommand.getAllOutput();
                if (asadminExecuteCommand.getResult() == 1) {
                    domainConfigSuccessful = false;
                }

            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
                domainConfigSuccessful = false;
            }
        } else {
            outputFromRecentRun = Msg.get("INVALID_START_DOMAIN_COMMAND_LINE");
            domainConfigSuccessful = false;
        }
        LOGGER.log(Level.INFO, outputFromRecentRun);
    }

    /* Checks to make sure that the domain is running, currently used during
     * cluster creation as create-cluster requires the domain to be up.
     * @param domainName, name of the domain to check.
     * @return true if the domain is running, false if the domain is not running
     * or there are other errors running asadmin list-domains command.
     */
    public boolean isDomainRunning(String domainName) {

        boolean retStatus = false;

        /* Assemble asadmin list-domains command. */
        ExecuteCommand asadminExecuteCommand =
                GlassFishUtils.assembleListDomainsCommand(productRef);

        if (asadminExecuteCommand != null) {
            outputFromRecentRun = asadminExecuteCommand.expandCommand(asadminExecuteCommand.getCommand());
            try {
                asadminExecuteCommand.setOutputType(ExecuteCommand.ERRORS | ExecuteCommand.NORMAL);
                asadminExecuteCommand.setCollectOutput(true);
                asadminExecuteCommand.execute();
                outputFromRecentRun += asadminExecuteCommand.getAllOutput();
                /* Look for the string "Name:<domainname> Status: Running" in the output. */
                String expectedOutput = Msg.get("DOMAIN_RUNNING_OUTPUT", new String[]{domainName});
                if (outputFromRecentRun.indexOf(expectedOutput) != -1) {
                    retStatus = true;
                }

            } catch (Exception e) {
                LOGGER.log(Level.FINEST, e.getMessage());
            }
        } else {
            outputFromRecentRun = Msg.get("INVALID_LIST_DOMAIN_COMMAND_LINE");
            retStatus = false;
        }
        LOGGER.log(Level.INFO, outputFromRecentRun);
        return retStatus;
    }

    /* Caller can get the output of recent asadmin command run. This has to be used
     * along with configSuccessful flag to find out the overall status of configuration.
     * @return String the whole of asadmin recent run's output including the command line.
     */
    public String getOutputFromRecentRun() {
        return this.outputFromRecentRun;
    }

    public boolean isConfigSuccessful() {
        return this.domainConfigSuccessful;
    }

    private static class StreamWatcher extends Thread {

        private final BufferedReader reader;

        private String output;

        public StreamWatcher(InputStream stream, String name) {
            super(name);
            reader = new BufferedReader(new InputStreamReader(stream));
            output = "";
        }

       public String getOutput() {
           return this.output;
       }

        public void run() {
            try {
                String line = reader.readLine();
                if (line != null) {
                    while (line != null) {                 
                        output += line;
                        output += "\n";
                        line = reader.readLine();
                        Thread.sleep(2);
                    }
                }
            } catch (Throwable t) {               
            } finally {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    
                }
            }
        }

   }

    
}
