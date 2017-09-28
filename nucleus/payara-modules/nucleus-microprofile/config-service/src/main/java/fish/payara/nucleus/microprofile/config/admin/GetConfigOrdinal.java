/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
import fish.payara.nucleus.microprofile.config.service.MicroprofileConfigConfiguration;
import fish.payara.nucleus.microprofile.config.service.MicroprofileConfigService;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * asAdmin command to the get the ordinal for one of the built in Config Sources
 * @since 4.1.2.173
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "get-config-ordinal") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn(RuntimeType.DAS)
@TargetType()
@RestEndpoints({ // creates a REST endpoint needed for integration with the admin interface
    
    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "get-config-ordinal",
            description = "Gets the Ordinal of a builtin Config Source")
})
public class GetConfigOrdinal implements AdminCommand {

    @Param(optional = true, acceptableValues = "domain,config,server,application,module,cluster,jndi", defaultValue = "domain")
    String source;

    @Param(optional = true, defaultValue = "server") // if no target is specified it will be the DAS
    String target;

    @Inject
    MicroprofileConfigService service;

    @Inject
    Target targetUtil;

    @Override
    public void execute(AdminCommandContext context) {
        Config configVal = targetUtil.getConfig(target);
        MicroprofileConfigConfiguration serviceConfig = configVal.getExtensionByType(MicroprofileConfigConfiguration.class);
        if (serviceConfig != null) {
            Integer result = -1;
            switch (source) {
                case "domain": {
                    result = serviceConfig.getDomainOrdinality();
                    break;
                }
                case "config": {
                    result = serviceConfig.getConfigOrdinality();
                    break;
                }
                case "server": {
                    result = serviceConfig.getServerOrdinality();
                    break;
                }
                case "application": {
                    result = serviceConfig.getApplicationOrdinality();
                    break;
                }
                case "module": {
                    result = serviceConfig.getModuleOrdinality();
                    break;
                }
                case "cluster": {
                    result = serviceConfig.getClusterOrdinality();
                    break;
                }case "jndi": {
                    result = serviceConfig.getJNDIOrdinality();
                    break;
                }
            }
            context.getActionReport().setMessage(result.toString());
        } else {
            context.getActionReport().failure(Logger.getLogger(SetConfigOrdinal.class.getName()), "No configuration with name " + target);
        }

    }

}
