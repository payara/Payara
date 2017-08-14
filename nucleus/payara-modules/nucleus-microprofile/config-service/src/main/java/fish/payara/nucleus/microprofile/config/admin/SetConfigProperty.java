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

import com.sun.enterprise.util.SystemPropertyConstants;
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
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * asAdmin command to the set the value of a config property
 *
 * @since 4.1.2.173
 * @author Steve Millidge (Payara Foundation)
 */
@Service(name = "set-config-property") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn()
@TargetType()
@RestEndpoints({ // creates a REST endpoint needed for integration with the admin interface

    @RestEndpoint(configBean = MicroprofileConfigConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "set-config-property",
            description = "Sets a configuration property")
})
public class SetConfigProperty implements AdminCommand {

    @Param(optional = true, acceptableValues = "domain,config,server,application,module,cluster, jndi", defaultValue = "domain")
    String source;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME) // if no target is specified it will be the DAS
    String target;

    @Param
    String propertyName;

    @Param
    String propertyValue;

    @Param(optional = true)
    String sourceName;

    @Param(optional = true)
    String moduleName;

    @Inject
    MicroprofileConfigService service;

    @Override
    public void execute(AdminCommandContext context) {

        switch (source) {
            case "domain": {
                service.setDomainProperty(propertyName, propertyValue);
                break;
            }
            case "config": {
                if (sourceName == null) {
                    context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the configuration if config is the source");
                } else {
                    service.setConfigProperty(sourceName, propertyName, propertyValue);
                }
                break;
            }
            case "server": {
                if (sourceName == null) {
                    context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the server if server is the source");
                } else {
                    service.setServerProperty(sourceName, propertyName, propertyValue);
                }
                break;
            }
            case "application": {
                if (sourceName == null) {
                    context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the application if application is the source");
                } else {
                    service.setApplicationProperty(sourceName, propertyName, propertyValue);
                }
                break;
            }
            case "module": {
                if (sourceName == null || moduleName == null) {
                    context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName and moduleName are required parameters if module is the source. The sourceName should be the name of the application where the module is deployed.");
                } else {
                    service.setModuleProperty(sourceName, moduleName, propertyName, propertyValue);
                }
                break;
            }
            case "cluster": {
                service.setClusteredProperty(propertyName, propertyValue);
                break;
            }

            case "jndi": {
                service.setJNDIProperty(propertyName, propertyValue, target);
                break;
            }

        }

    }

}
