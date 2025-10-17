/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import com.sun.enterprise.util.SystemPropertyConstants;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.APPLICATION;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.CLOUD;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.CLUSTER;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.CONFIG;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.DOMAIN;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.JNDI;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.MODULE;
import static fish.payara.nucleus.microprofile.config.admin.ConfigSourceConstants.SERVER;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.nucleus.microprofile.config.source.ApplicationConfigSource;
import fish.payara.nucleus.microprofile.config.source.ClusterConfigSource;
import fish.payara.nucleus.microprofile.config.source.ConfigConfigSource;
import fish.payara.nucleus.microprofile.config.source.DomainConfigSource;
import fish.payara.nucleus.microprofile.config.source.JNDIConfigSource;
import fish.payara.nucleus.microprofile.config.source.ModuleConfigSource;
import fish.payara.nucleus.microprofile.config.source.ServerConfigSource;
import fish.payara.nucleus.microprofile.config.source.extension.ExtensionConfigSource;
import fish.payara.nucleus.microprofile.config.source.extension.ExtensionConfigSourceService;
import fish.payara.nucleus.microprofile.config.spi.MicroprofileConfigConfiguration;

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

    @Param(optional = true, acceptableValues = "domain,config,server,application,module,cluster,jndi,cloud", defaultValue = DOMAIN)
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
    private ExtensionConfigSourceService extensionService;

    @Override
    public void execute(AdminCommandContext context) {

        try {
            switch (source) {
                case DOMAIN: {
                    DomainConfigSource csource = new DomainConfigSource();
                    csource.setValue(propertyName, propertyValue);
                    break;
                }
                case CONFIG: {
                    if (sourceName == null) {
                        context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the configuration if config is the source");
                    } else {
                        ConfigConfigSource csource = new ConfigConfigSource(sourceName);
                        if (!csource.setValue(propertyName, propertyValue)) {
                            context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. Please check the configuration named " + sourceName + " is in your domain");
                        }
                    }
                    break;
                }
                case SERVER: {
                    if (sourceName == null) {
                        context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the server if server is the source");
                    } else {
                        ServerConfigSource csource = new ServerConfigSource(sourceName);
                        if (!csource.setValue(propertyName, propertyValue)) {
                            context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. Please check the server named " + sourceName + " is in your domain");
                        }
                    }
                    break;
                }
                case APPLICATION: {
                    if (sourceName == null) {
                        context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName is a required parameter and the name of the application if application is the source");
                    } else {
                        ApplicationConfigSource csource = new ApplicationConfigSource(sourceName);
                        if (!csource.setValue(propertyName, propertyValue)) {
                            context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. Please check the application named " + sourceName + " is in your domain");
                        }
                    }
                    break;
                }
                case MODULE: {
                    if (sourceName == null || moduleName == null) {
                        context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "sourceName and moduleName are required parameters if module is the source. The sourceName should be the name of the application where the module is deployed.");
                    } else {
                        ModuleConfigSource csource = new ModuleConfigSource(sourceName, moduleName);
                        if (!csource.setValue(propertyName, propertyValue)) {
                            context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. Please check the application named " + sourceName + " with the module " + moduleName + " is in your domain");
                        }
                    }
                    break;
                }
                case CLUSTER: {
                    ClusterConfigSource csource = new ClusterConfigSource();
                    csource.setValue(propertyName, propertyValue);
                    break;
                }

                case JNDI: {
                    JNDIConfigSource jsource = new JNDIConfigSource();
                    if (!jsource.setValue(propertyName, propertyValue, target)) {
                        context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. See the server log for details");
                    }
                    break;
                }

                case CLOUD: {
                    Collection<ExtensionConfigSource> extensionSources = extensionService.getExtensionSources();
                    for (ExtensionConfigSource extension : extensionSources) {
                        if (extension.getName().equals(sourceName)) {
                            if (!extension.setValue(propertyName, propertyValue)) {
                                context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getName()), "Failed to set the Microprofile Config Value. See the server log for details");
                            }
                        }
                    }
                }

            }

        } catch (TransactionFailure txFailure) {
            context.getActionReport().failure(Logger.getLogger(SetConfigProperty.class.getCanonicalName()), "Failed to set config property", txFailure);
        }
    }
}
