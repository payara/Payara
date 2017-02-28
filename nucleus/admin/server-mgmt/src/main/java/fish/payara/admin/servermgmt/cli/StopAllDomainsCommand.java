/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2017 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.servermgmt.cli.*;
import com.sun.enterprise.admin.cli.CLIConstants;
import com.sun.enterprise.admin.cli.remote.DASUtils;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.DomainsManager;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.*;
/**
 * The stop-all-domains command
 * 
 * @author coustick
 */
@Service(name = "stop-all-domains")
@PerLookup
public class StopAllDomainsCommand extends StopDomainCommand {

    @Param(name = "force", optional = true, defaultValue = "true")
    Boolean force;
    @Param(optional = true, defaultValue = "false")
    Boolean kill;
    private static final long WAIT_FOR_DAS_TIME_MS = 60000; // 1 minute
      
    private static final LocalStringsImpl strings = new LocalStringsImpl(ListDomainsCommand.class);
    
     @Override
    protected void validate()
            throws CommandException, CommandValidationException {
    }    
    
    @Override
    protected int executeCommand() throws CommandException {
        try {
            String[] domainsList = getDomains();
            for (String domain : domainsList){
                setConfig(domain);
                RemoteCLICommand cmd = new RemoteCLICommand("stop-domain", programOpts, env);
                if (kill){
                    
                } else {
                    logger.fine("Stopping domain " + domain);
                    try {
                    cmd.executeAndReturnOutput("stop-domain","--force", force.toString());
                    } catch (Exception e){
                        //System.out.println(e.getLocalizedMessage());
                    }
                    logger.fine("Stopped domain");
                }
                
            }          
        return 0;
        } catch (Exception ex) {
            throw new CommandException(ex.getLocalizedMessage());
        }
        
        
    }
    
    //Copied from ListDomainCommand.java
    private String[] getDomains() throws DomainException, IOException{
        
         File domainsDirFile = ok(domainDirParam)
                    ? new File(domainDirParam) : DomainDirs.getDefaultDomainsDir();

            DomainConfig domainConfig = new DomainConfig(null, domainsDirFile.getAbsolutePath());
            DomainsManager manager = new PEDomainsManager();
            String[] domainsList = manager.listDomains(domainConfig);
            if (domainsList.length == 0){
                logger.fine(strings.get("NoDomainsToList"));
            }
            return domainsList;
    }
    
    //Copied from com.sun.enterprise.admin.servermgmt.cli.StopDomainCommand
    private int setConfig(String domain) throws CommandException{
        setDomainName(domain);
        super.initDomain();
        if (isLocal()) {
            // if the local password isn't available, the domain isn't running
            // (localPassword is set by initDomain)
            if (getServerDirs().getLocalPassword() == null){
                return dasNotRunning();
            }
            programOpts.setHostAndPort(getAdminAddress());
            logger.finer("Stopping local domain on port "
                    + programOpts.getPort());

            /*
             * If we're using the local password, we don't want to prompt
             * for a new password.  If the local password doesn't work it
             * most likely means we're talking to the wrong server.
             */
            programOpts.setInteractive(false);

            // in the local case, make sure we're talking to the correct DAS
            if (!isThisDAS(getDomainRootDir()))
                return dasNotRunning();

            logger.finer("It's the correct DAS");
        }
        else { // remote
            // Verify that the DAS is running and reachable
            if (!DASUtils.pingDASQuietly(programOpts, env)){
                return dasNotRunning();
            }
            logger.finer("DAS is running");
            programOpts.setInteractive(false);
        }
        return 0;
    }
}
