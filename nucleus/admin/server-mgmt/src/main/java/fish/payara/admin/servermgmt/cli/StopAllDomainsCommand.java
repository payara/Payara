/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.servermgmt.cli.*;
import com.sun.enterprise.admin.cli.remote.DASUtils;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.DomainConfig;
import com.sun.enterprise.admin.servermgmt.DomainException;
import com.sun.enterprise.admin.servermgmt.DomainsManager;
import com.sun.enterprise.admin.servermgmt.pe.PEDomainsManager;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.io.DomainDirs;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.PerLookup;
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

    @Param(optional = true, defaultValue = "600")
    private int timeout;

    @Param(optional = true, defaultValue = "60")
    private int domainTimeout;

    private static final long WAIT_FOR_DAS_TIME_MS = 60000; // 1 minute
      
    private static final LocalStringsImpl strings = new LocalStringsImpl(ListDomainsCommand.class);

    @Override
    protected void prevalidate() throws CommandException {
        if (operands != null && operands.size() > 0) {
            throw new CommandException(strings.get("StopAllDomains.tooManyOperands"));
        }
        super.prevalidate();
    }

    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        // no-op to prevent initDomain being called
        if (timeout < 1 || domainTimeout < 1) {
            throw new CommandValidationException("Timeout must be at least 1 second long.");
        }
    }    
    
    @Override
    @SuppressWarnings("ThrowFromFinallyBlock")
    protected int executeCommand() throws CommandException {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        Future future = executorService.submit(() -> {
            MultiException allExceptions = new MultiException();
            try {
                String[] domainsList = getDomains();
                for (String domain : domainsList) {
                    setConfig(domain);
                    ParameterMap parameterMap = new ParameterMap();
                    parameterMap.insert("timeout", String.valueOf(domainTimeout));
                    programOpts.updateOptions(parameterMap);
                    RemoteCLICommand cmd = new RemoteCLICommand("stop-domain", programOpts, env);
                    cmd.setReadTimeout(domainTimeout * 1000);
                    logger.log(Level.FINE, "Stopping domain {0}", domain);
                    try {
                        cmd.executeAndReturnOutput("stop-domain", "--force", force.toString());
                    } catch (Exception e) {
                        allExceptions.addError(e);
                    }
                    logger.fine("Stopped domain");
                }
                return 0;
            } catch (Exception ex) {
                allExceptions.addError(ex);
                return 1; // required to compile, but exception should be thrown in finally block
            } finally {
                if (!allExceptions.getErrors().isEmpty()){
                    throw new CommandException(allExceptions.getMessage());
                }
            }
        });
        long startTime = System.currentTimeMillis();
        while (!timedOut(startTime)) {
            if (future.isDone()) {
                return 0;
            }
        }
        throw new CommandException("Command Timed Out");
    }
    
    //Copied from ListDomainCommand.java
    private String[] getDomains() throws DomainException, IOException{

        File domainsDirFile = ok(domainDirParam) ? new File(domainDirParam) : DomainDirs.getDefaultDomainsDir();

        DomainConfig domainConfig = new DomainConfig(null, domainsDirFile.getAbsolutePath());
        DomainsManager manager = new PEDomainsManager();
        String[] domainsList = manager.listDomains(domainConfig);
        if (domainsList.length == 0) {
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
            logger.log(Level.FINER, "Stopping local domain on port {0}", programOpts.getPort());

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

    // PAYARA-1704 now fails gracefully
    @Override
    protected int dasNotRunning() throws CommandException {
        if (kill && isLocal()) {
            return kill();
        }
        return 0;
    }

    private boolean timedOut(long startTime) {
        return (System.currentTimeMillis() - startTime) > (timeout * 1000);
    }
}
