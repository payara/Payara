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

import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.servermgmt.cli.LocalDomainCommand;
import com.sun.enterprise.admin.servermgmt.cli.StartDomainCommand;
import java.io.File;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * Starts a comma separated list of domains
 * 
 * @since 4.1.2.173
 * @author jonathan coustick
 */
@Service(name = "start-domains")
@PerLookup
public class StartDomainsCommand extends StartDomainCommand {

    @Param(optional = true, defaultValue = "false")
    private Boolean upgrade;
    @Param(optional = true, shortName = "w", defaultValue = "false")
    private Boolean watchdog;
    @Param(optional = true, shortName = "d", defaultValue = "false")
    private Boolean debug;
    @Param(name = "domain_name", primary = true, optional = true)
    private String domainName0;
    @Param(name = "dry-run", shortName = "n", optional = true,
            defaultValue = "false")
    private Boolean dry_run;
    @Param(name = "drop-interrupted-commands", optional = true, defaultValue = "false")
    private Boolean drop_interrupted_commands;
    @Param(name = "prebootcommandfile", optional = true)
    private String preBootCommand;
    @Param(name = "postbootcommandfile", optional = true)
    private String postBootCommand;
    
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        if (preBootCommand != null){
            File prebootfile = new File(preBootCommand);
            if (!prebootfile.exists()){
                throw new CommandValidationException("preboot commands file does not exist: " + prebootfile.getAbsolutePath());
            }
        }
        
        if (postBootCommand != null){
            File postbootFile = new File(postBootCommand);
            if (!postbootFile.exists()){
                throw new CommandValidationException("postboot commands file does not exist: "+ postbootFile.getAbsolutePath());
            }
        }
    }
    
    @Override
    protected int executeCommand() throws CommandException {
        try{
        String[] domains = domainName0.split(",");
        for (String domainName : domains){
            setDomainName(domainName);
            super.initDomain();
            programOpts.setHostAndPort(getAdminAddress());
            super.executeCommand();
            logger.fine("Started domain " + domainName);
        }
        return 0;
        } catch (Exception ex){
            throw new CommandException(ex.getLocalizedMessage());
        }
    }
    
    
    
    
}
