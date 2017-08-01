/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.admin.servermgmt.cli;

import com.sun.enterprise.admin.cli.remote.DASUtils;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.cli.StopDomainCommand;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Stops a comma separated list of domains
 * 
 * @since 4.1.2.173
 * @author jonathan coustick
 */
@Service(name = "stop-domains")
@PerLookup
public class StopDomainsCommand extends StopDomainCommand {

    @Param(name = "domain_name", primary = true, optional = true)
    protected String userArgDomainName;
    @Param(name = "force", optional = true, defaultValue = "true")
    Boolean force;
    @Param(optional = true, defaultValue = "false")
    Boolean kill;
    
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
    }
    
    @Override
    protected int executeCommand() throws CommandException {
        try{
        String[] domains = userArgDomainName.split(",");
        for (String domainName : domains){
            setConfig(domainName);
            RemoteCLICommand cmd = new RemoteCLICommand("stop-domain", programOpts, env);
            logger.fine("Stopping domain " + domainName);
                try {
                    cmd.executeAndReturnOutput("stop-domain");
                } catch (Exception e){
                }
                logger.fine("Stopped domain");
        }
        return 0;
        } catch (Exception ex){
            throw new CommandException(ex.getLocalizedMessage());
        }
    }
    
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
    
    @Override
    protected int dasNotRunning() throws CommandException {
        if (kill && isLocal()) {
            return kill();
        }
        return 0;
    }
    
}
