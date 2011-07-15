/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.registration.impl;

//import com.sun.enterprise.registration.*;
import com.sun.enterprise.registration.RegistrationException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tranfers service tag information from the local product repository to the
 * SysNet repository using the SysNet stclient utility.
 *
 * @author tjquinn
 */
public class SysnetTransferManager {
    
    private Logger logger = RegistrationLogger.getLogger();

    private RepositoryManager rm;
    
    /**
     * Creates a new SysnetTransferManager to transfer the service tags in the
     * specified repository to SysNet.
     */
    public SysnetTransferManager(File repositoryFile) throws RegistrationException {
        rm = new RepositoryManager(repositoryFile);
    }
    
    /**
     * Transfers previously-untransferred service tags from the local repository
     * to SysNet using the stclient utility for the current platform.
     *<p>
     * Each local service tag is marked as transferred immediately after the
     * corresponding stclient invocation completes.  So, an error during the
     * stclient invocation for a given service tag does not erase the fact that
     * earlier transfers of other service tags succeeded.
     * @return the number of tags transferred; -1 if the stclient command is not available
     * @throws RegistrationException for errors encountered during transfer
     */
    public int transferServiceTags() throws RegistrationException {
        int result = -1;
        try {
            /*
             * If the SysNet stclient utility is not present there is no work to do.
             */
            if ( ! isSTClientInstalled()) {
                logger.info("stclient tool not found; tag transfer to SysNet skipped");
                return result;
            }

            /*
             * Make sure the runtime values are up-to-date.
             */
            rm.updateRuntimeValues();
            
            List<ServiceTag> unTransferredTags = getUntransferredServiceTags(rm);

            /*
             * Transfer each untransferred tag.
             */
            for (ServiceTag tag : unTransferredTags) {
                addTagToSysNet(tag);
                rm.setStatus(tag, ServiceTag.Status.TRANSFERRED);
            }
            result = unTransferredTags.size();
            logger.info( result + " service tags successfully transferred to SysNet");
            return result;
        } catch (Exception e) {
            throw new RegistrationException(StringManager.getString("xfmgr.errTransTags"), e);
}
    }
    
    /**
     * Reports whether the stclient utility is installed on this system.
     * @return true if stclient is present; false otherwise
     */
    private boolean isSTClientInstalled() {
        return (chooseSTClientCommandForPlatform() != null);
    }

    /**
     * Lists all the ServiceTags in the local repository that have not yet
     * been transferred to SysNet.
     * @param rm the RepositoryManager to use for the local repository
     * @return List<ServiceTag> containing the untransferred tags
     */
    private List<ServiceTag> getUntransferredServiceTags(RepositoryManager rm) {
        List<ServiceTag> candidateTags = rm.getServiceTags();
        /*
         * Keep only those tags that are not already transferred.
         */
        for (Iterator<ServiceTag> it = candidateTags.iterator(); it.hasNext();) {
            ServiceTag tag = it.next();
            if (ServiceTag.Status.valueOf(tag.getSvcTag().getStatus()).equals(ServiceTag.Status.TRANSFERRED)) {
                it.remove();
            }
        }
        return candidateTags;
    }
    
    /**
     * Creates and runs a command that invokes stclient to add a service tag
     * to the SysNet local repository.
     * @param tag the ServiceTag to add to SysNet
     */
    private void addTagToSysNet(ServiceTag tag) throws RegistrationException, IOException, InterruptedException {
        STClientCommand stClientCommand = new STClientCommand(tag);
        stClientCommand.run();
    }

    /**
     * Represents information about the platform-dependent commands used to
     * run the stclient tool.
     * <p>
     * This class reocrds the path to the tool and the "command prefix" needed
     * to successfully invoke that tool.  For non-Windows system the prefix is
     * typically null.  For Windows the prefix is cmd/c which starts the
     * command shell to run the command and exits the shell and process when
     * the single command completes.
     */
    private static class STClientCommandInfo {
        /** the OS name prefix which this info applies to */
        private String osNamePrefix;
        
        /** the path to the tool */
        private String toolPath;
        
        private STClientCommandInfo(String osNamePrefix, String toolPath /*, String commandPrefix */) {
            this.osNamePrefix = osNamePrefix;
            this.toolPath = toolPath;
        }

        private String getToolPath() {
            return toolPath;
        }

        /**
         * Reports whether this stclient command info instance handles the
         * operating system name specified.
         * @param osName the name of the OS from the os.name property
         * @return true if this command info handles the OS; false otherwise
         */
        private boolean handles(String osName) {
            return osName.startsWith(osNamePrefix);
        }
    }

    /**
     * Builds the list of client command info objects for the platforms on which
     * SysNet is supported.  
     */
    private static List<STClientCommandInfo> prepareSTClientLocationMap() {
        List<STClientCommandInfo> result = new ArrayList<STClientCommandInfo>();
        result.add( 
            new STClientCommandInfo(
                "Windows", 
                "C:\\Program Files\\Sun\\servicetag\\stclient.exe"));
        result.add(
            new STClientCommandInfo(
                "Sun", 
                "/usr/bin/stclient"));
        result.add(
            new STClientCommandInfo(
                "Linux", 
                "/opt/sun/servicetag/bin/stclient"));
        return result;
    }
    /** holds the STClientCommandInfo objects */
    private static final List<STClientCommandInfo> osToSTClientCommand = prepareSTClientLocationMap();

    /**
     * Returns the path to the installed stclient tool, if the tool is present
     * on this system.
     * <p>
     * If present, the stclient tool resides in different places depending on 
     * the platform type.  The method identifies the expected location based on
     * the platform type and then checks to see if the tool is there.
     * @return the path to the tool, if this platform type is one where we expect
     * to find the tool and we find the tool where expected; null otherwise
     */
    private STClientCommandInfo chooseSTClientCommandForPlatform() {
        String stClientPath = null;
        for (STClientCommandInfo info : osToSTClientCommand) {
            if (info.handles(OS_NAME)) {
                stClientPath = info.getToolPath();
                File stClientFile = new File(stClientPath);
                if (stClientFile.exists()) {
                    logger.fine("Found stclient as expected at " + stClientPath);
                    return info;
                } else {
                    logger.fine("Looked for stclient tool but did not find it at " + stClientPath);
                    return null;
                }
            }
        }
        logger.info("Platform " + OS_NAME + " is not currently supported by SysNet registration");
        return null;
    }
    
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String LINE_SEP = System.getProperty("line.separator");
    
    /**
     * Encapsulates all details related to building and running an stclient command that
     * enters a single service tag into the local SysNet repository.
     */
    private class STClientCommand {

        /** stclient command timeout */
        private static final long STCLIENT_COMMAND_TIMEOUT_MS = 25 * 1000;
        
        /** buffer size for holding output and error text from the process */
        private static final int INPUT_BUFFER_SIZE = 1024;
        
        /** delay between successive attempts to read the process output and error streams */
        private static final int PROCESS_IO_FLUSH_DELAY_MS = 500;

        /** the service tag to be added to SysNet's repository */
        private ServiceTag tag;
        
        /** the command elements - executable and arguments - to be executed */
        private List<String> command;

        /** output and error streams from the process */
        private InputStreamReader outputFromProcess;
        private InputStreamReader errorFromProcess;
        
        /** accumulation of all output and error text from the process */
        private StringBuilder outputTextFromProcess = new StringBuilder();
        private StringBuilder errorTextFromProcess = new StringBuilder();
        
        /**
         * Creates a new instance of the STClientCommand for adding the specified
         * service tag to the SysNet repository.
         * @param tag the ServiceTag to be added to SysNet
         */
        public STClientCommand(ServiceTag tag) throws RegistrationException {
            this.tag = tag;
            constructCommand();
        }

        /**
         * Executes the previously-constructed command to add a SysNet
         * service tag to the local SysNet repository, using information from
         * the service tag stored in the product's local repository.
         */
        public void run() throws IOException, InterruptedException, RegistrationException {

            /*
             * Prepare the process builder with the command elements
             * already computed that make up the command to be executed.
             */
            ProcessBuilder pb = new ProcessBuilder(command);
            
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder sb = new StringBuilder("Preparing to run the following command:\n");
                for (String s : command) {
                    sb.append(s).append(" ");
                }
                logger.fine(sb.toString());
            }
            
            /*
             * Start the process and set up readers for its error and output streams.
             */
            final Process commandProcess = pb.start();
            outputFromProcess = 
                    new InputStreamReader(new BufferedInputStream(commandProcess.getInputStream()));
            errorFromProcess = 
                    new InputStreamReader(new BufferedInputStream(commandProcess.getErrorStream()));
            
            /** records the exit status from the command process */
            final AtomicInteger processExitStatus = new AtomicInteger();
            
            /** records whether the process completion occurred before the monitoring thread ended */
            final AtomicBoolean processExitDetected = new AtomicBoolean(false);
            
            /*
             * Create and start a thread to monitor the command process.  
             */
            Thread processMonitorThread = new Thread(new Runnable() {
                    /**
                     * Waits for the previously-started command process to
                     * complete, then records the exit status from the process
                     * and sets processExited to indicate that the monitor thread
                     * did detect the completion of the process.
                     */
                    public void run() {
                        try {
                            int status = commandProcess.waitFor();
                            processExitStatus.set(status);
                            processExitDetected.set(true);
                            logger.fine("Process monitor thread detected process completion with status " + status);
                        } catch (InterruptedException e) {
                            logger.fine("Process monitor thread was interrupted and is forcibly destroying the command process");
                            commandProcess.destroy();
                            /*
                             * Just fall through.
                             */
                        }
                    }
                }
                
                );
            processMonitorThread.start();

            /*
             * Wait for the process monitor thread to complete, flushing the
             * stream carrying the process's output and error text in the meantime.  The 
             * command process will either end on its own or will be destroyed
             * if it takes too long.  In either case, the process monitor
             * thread will complete shortly after the process completes.
             */
            boolean isProcessDestroyRequested = false;
            long processWaitDeadline = System.currentTimeMillis() + 
                    STCLIENT_COMMAND_TIMEOUT_MS;
            
            while (processMonitorThread.isAlive()) {
                /*
                 * If the command has now run longer than it should, stop
                 * the process.  The monitor thread will then detect the process
                 * destruction and complete normally.  Destroy the process
                 * only once in case it takes it a while.
                 */
                if ((System.currentTimeMillis() > processWaitDeadline) &&
                    ( ! isProcessDestroyRequested )) {
                    logger.fine("Command process has taken too long to complete; destroying it");
                    commandProcess.destroy();
                    isProcessDestroyRequested = true;
                }
                
                /*
                 * In any case, continue flushing the output from the process
                 * to the buffers.
                 */
                flushProcessIO();
                Thread.sleep(PROCESS_IO_FLUSH_DELAY_MS);
            }
            
            /*
             * Flush once more because the process may have written more output
             * or error text before ending, all since the last time through the 
             * while loop above.
             */
            flushProcessIO();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Output stream from command process:" + LINE_SEP + outputTextFromProcess.toString());
                logger.fine("Error stream from command process:" + LINE_SEP + errorTextFromProcess.toString());
            }

            /*
             * The command process monitor thread has finished.  Normally that 
             * means the process has ended, but the thread could have been
             * interrupted for some reason.  So make sure the command process exit 
             * was actually detected before trying to interpret the status.
             */
            if ( ! processExitDetected.get()) {
                logger.fine("Command process monitoring thread stopped before the command process, so the command's state is unknown");
                throw new RegistrationException(StringManager.getString("xfmgr.unknownCmdProcResult"));
            }

            /*
             * By checking processExited first, we know that at this point the 
             * processExitStatus has meaning.  So go ahead and check it to 
             * decide if the process completed successfully or not.
             */
            if (processExitStatus.get() != 0) {
                /*
                 * Build an exception using the error output from the process
                 * and throw it.
                 */
                RegistrationException e = 
                    new RegistrationException(StringManager.getString("xfmgr.cmdProcFailed", errorTextFromProcess.toString()));
                throw e;
            }

            logger.fine("Command process seems to have completed successfully");
        }
        
        /**
         * Empties the process output and error streams into the variables holding
         * that output.
         */
        private void flushProcessIO() throws IOException {
            copyFromReader(outputFromProcess, outputTextFromProcess);
            copyFromReader(errorFromProcess, errorTextFromProcess);
            }
    
        /**
         * Copies all available output from the specified reader to the string 
         * builder accumulating a copy of that output.
         * @param reader the InputStreamReader from which to copy
         * @param sb the StringBuilder that holds the accumulated contents of the stream
         */
        private void copyFromReader(InputStreamReader reader, StringBuilder sb) throws IOException {
            char[] buffer = new char[INPUT_BUFFER_SIZE];
            boolean eoStream = false;
            while (reader.ready() && ! eoStream) {
                int charsRead = reader.read(buffer);
                if (charsRead == -1) {
                    eoStream = true;
                } else {
                    sb.append(buffer, 0, charsRead);
                }
            }
        }

        /**
         * Builds the command that will invoke the stclient utility.
         */
        private void constructCommand() throws RegistrationException {
            /*
             * The format of the stclient command to add a tag is
             
     stclient -a [-i instance_URN] -p product_name -e product_version
         -t product_URN [-F parent_URN] -P product_parent
         [-I product_defined_instance_id] -m product_vendor -A platform_arch
         -z container -S source [-r root_dir]             
             
             */
        
            command = new ArrayList<String>();
            STClientCommandInfo commandInfo = chooseSTClientCommandForPlatform();
            
            /*
             * Any value that could contain spaces should be sent through
             * formatValue which placed double quotes around any value that
             * does contain a space.
             */
            command.add(formatValue(commandInfo.getToolPath()));

            command.add("-a"); /* add a tag */
            
            addRequired("-p", tag.getProductName());
            addRequired("-e", tag.getProductVersion());
            addRequired("-t", tag.getProductURN());
            
            addOptional("-F", tag.getProductParentURN());

            addRequired("-P", tag.getProductParentURN());

            addOptional("-I", tag.getProductDefinedInstID());
            
            addRequired("-m", tag.getProductVendor());
            addRequired("-A", tag.getPlatformArch());
            addRequired("-z", tag.getContainer());
            addRequired("-S", tag.getSource());
            addOptional("-i", tag.getInstanceURN());
        }
        
        /**
         * Makes sure the value is not null and, if not, adds it to the command
         * list.
         * @param the option to be added
         * @param value the option's value
         */
        private void addRequired(String option, String value) throws RegistrationException {
            if (value == null) {
                throw new RegistrationException(StringManager.getString("xfmgr.reqdValueNull", option));
            }
            command.add(option);
            command.add(formatValue(value));
        }

        /**
         * Adds an optional option and its value to the command if the value
         * is not null.
         * @param option the option to be added to the command
         * @param value the option's value
         */
        private void addOptional(String option, String value) {
            if (value != null && value.length() > 0) {
                command.add(option);
                command.add(formatValue(value));
            }
        }

        /**
         * Places double quotes around an empty value or a value that contains
         * a blank.
         * @param value the value to be formatted
         */
        private String formatValue(String value) {
            if (value.indexOf(' ') == -1 && value.length() > 0) {
                return value;
            } else {
                return "\"" + value + "\"";
            }
        }

    }
}
