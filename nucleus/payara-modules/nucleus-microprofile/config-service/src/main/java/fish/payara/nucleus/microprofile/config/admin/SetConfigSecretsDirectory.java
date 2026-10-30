/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.microprofile.config.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;
import java.beans.PropertyVetoException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * asAdmin command to the set the directory for the Secrets Dir Config Source
 *
 * @since 4.1.2.181
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "set-config-dir") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn()
@TargetType()
@RestEndpoints({ // creates a REST endpoint needed for integration with the admin interface
    
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "set-config-dir",
            description = "Sets the Directory for the Config Source")
})
public class SetConfigSecretsDirectory implements AdminCommand {
    
    @Param
    String directory;
    
    @Param(optional = true, defaultValue = "server") // if no target is specified it will be the DAS
    String target;
       
    @Inject
    Target targetUtil;
    
    @Inject
    ServerEnvironment env;

    @Override
    public void execute(AdminCommandContext context) {
        
        // do validation
        Path directoryPath = Paths.get(directory);
        boolean absolute = true;
        boolean relativeFound = false;
        if (!Files.isDirectory(directoryPath) || !Files.exists(directoryPath) || !Files.isReadable(directoryPath)) {
            absolute = false;
            // ok try relative to the server root
            context.getActionReport().appendMessage("Could not find readable directory at " + directoryPath.toString() + "\n");
            Path instanceRoot = env.getInstanceRoot().toPath();
            Path relative = Paths.get(instanceRoot.toString(), directory);
            if (!Files.isDirectory(relative) || !Files.exists(relative) || !Files.isReadable(relative)) {
                context.getActionReport().appendMessage("Could not find readable directory at " + relative.toString() + "\n");
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            relativeFound = true;
            directoryPath = relative;
            context.getActionReport().appendMessage("Using readable directory at " + relative.toString() + "\n");
        } else {
            context.getActionReport().appendMessage("Using readable directory at " + directoryPath.toString() + "\n");            
        }
        
        Config configVal = targetUtil.getConfig(target);
        MicroprofileConfigConfiguration serviceConfig = configVal.getExtensionByType(MicroprofileConfigConfiguration.class);
        try {
            final Path toSet = directoryPath;
            ConfigSupport.apply(new SingleConfigCode<MicroprofileConfigConfiguration>() {
                @Override
                public Object run(MicroprofileConfigConfiguration t) throws PropertyVetoException, TransactionFailure {
                    t.setSecretDir(toSet.toString());
                    return null;
                }
            }, serviceConfig);
        } catch (TransactionFailure ex) {
            Logger.getLogger(SetConfigSecretsDirectory.class.getName()).log(Level.SEVERE, "Could not set Directory", ex);
            context.getActionReport().failure(Logger.getLogger(this.getClass().getName()), "Could not set Directory", ex);
        }
    }
    
}
