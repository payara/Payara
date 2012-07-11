/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity.command;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.modularity.ZeroConfigUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A remote command to create the default configuration for a given service using the snippets available in the relevant module.
 *
 * @author Masoud Kalali
 */
@TargetType(value = {CommandTarget.DAS, CommandTarget.DOMAIN, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE})
@ExecuteOn(RuntimeType.ALL)
@Service(name = "create-module-config")
@Scoped(PerLookup.class)
@I18n("create.module.config")
public final class CreateModuleConfigCommand extends AbstractZeroConfigCommand implements AdminCommand {
    private final Logger LOG = Logger.getLogger(CreateModuleConfigCommand.class.getName());
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateModuleConfigCommand.class);
    private static final String DEFAULT_FORMAT = "";

    @Inject
    private Domain domain;

    @Inject
    Habitat habitat;

    @Param(optional = true, defaultValue = "false", name = "dryRun")
    private Boolean dryRun;

    @Param(optional = true, defaultValue = "false", name = "all")
    private Boolean isAll;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_CONFIG, name = "target")
    private String target;

    @Param(optional = true, name = "serviceName", primary = true)
    private String serviceName;


    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        String defaultConfigurationElements;
        if (target != null) {
            if (domain.getConfigNamed(target) == null) {
                report.setMessage(localStrings.getLocalString("create.module.config.target.name.invalid",
                        "The target name you specified is invalid. Please double check the target name and try again"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }
        if (isAll && (serviceName != null)) {
            report.setMessage(localStrings.getLocalString("create.module.config.service.name.ignored",
                    "You can only use --all or specify a service name. These two options are exclusive."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!isAll && (serviceName == null)) {
            //TODO check for usability options, should we create the default configs, show them or fail the execution?
            report.setMessage(localStrings.getLocalString("create.module.config.no.service.no.all",
                    DEFAULT_FORMAT, target));

            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.failure",
                        DEFAULT_FORMAT, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && dryRun) {
            report.setMessage(localStrings.getLocalString("create.module.config.show.all",
                    DEFAULT_FORMAT, target));
            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.show.all.failed",
                        DEFAULT_FORMAT, target, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && !dryRun) {
            report.setMessage(localStrings.getLocalString("create.module.config.creating.all",
                    DEFAULT_FORMAT, target));
            try {
                createAllMissingElements(target);
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.creating.all.failed",
                        DEFAULT_FORMAT, target, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (serviceName != null) {
            String className = ZeroConfigUtils.convertConfigElementNameToClassName(serviceName);
            Class configBeanType = ZeroConfigUtils.getClassFor(serviceName, habitat);
            if (configBeanType == null) {
                String msg = localStrings.getLocalString("create.module.config.not.such.a.service.found",
                        DEFAULT_FORMAT, className, serviceName);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            try {
                if (dryRun) {
                    String serviceDefaultConfig = getDefaultConfigFor(configBeanType, habitat);
                    if (serviceDefaultConfig != null) {
                        report.setMessage(serviceDefaultConfig);
                    }
                } else {
                    createMissingElementFor(configBeanType, serviceName, target, report);
                }

            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.creating.for.service.name.failed",
                        DEFAULT_FORMAT, serviceName, target, e.getMessage());
                LOG.log(Level.INFO, msg, e);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                report.setFailureCause(e);
                return;
            }
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private boolean createMissingElementFor(Class configBeanType, String serviceName, String target, ActionReport report) throws Exception {
        boolean defaultConfigCreated = false;
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            Config c = domain.getConfigNamed(target);
            if (c.checkIfExtensionExists(configBeanType)) {
                report.setMessage(localStrings.getLocalString("create.module.config.already.exists", DEFAULT_FORMAT, serviceName));
            }
            c.getExtensionByType(configBeanType);
            defaultConfigCreated = true;
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfExtensionExists(configBeanType)) {
                report.setMessage(localStrings.getLocalString("create.module.config.already.exists", DEFAULT_FORMAT, serviceName));
            }
            domain.getExtensionByType(configBeanType);
            defaultConfigCreated = true;
        }
        return defaultConfigCreated;
    }

    private String getDefaultConfigFor(Class configBeanType, Habitat habitat) throws Exception {
        if (!ZeroConfigUtils.hasCustomConfig(configBeanType)) {
            return ZeroConfigUtils.serializeConfigBeanByType(configBeanType, habitat);
        } else {
            List<ConfigBeanDefaultValue> defaults = ZeroConfigUtils.getDefaultConfigurations(configBeanType);
            StringBuilder builder = new StringBuilder();
            for (ConfigBeanDefaultValue value : defaults) {
                builder.append("At location: ");
                builder.append(replaceExpressionsWithValues(value.getLocation()));
                builder.append(System.getProperty("line.separator"));
                String substituted = replacePropertiesWithDefaultValues(value.getCustomizationTokens(),
                        value.getXmlConfiguration());
                builder.append(substituted);
                builder.append(System.getProperty("line.separator"));
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }

    private String replacePropertiesWithDefaultValues(List<ConfigCustomizationToken> tokens, String xmlConfig) {
        for (ConfigCustomizationToken token : tokens) {
            String toReplace = "\\$\\{" + token.getKey() + "\\}";
            xmlConfig = xmlConfig.replaceAll(toReplace, token.getDefaultValue());
        }
        return xmlConfig;
    }

    private void createAllMissingElements(String target) throws Exception {
        //reburies scanning and finding all snippets and then checking which
        // ones are not present in the domain.xml and then creating them.
    }

    private String getAllDefaultConfigurationElements(String target) throws Exception {
        //reburies scanning and finding all snippets and then checking which
        // ones are not present in the domain.xml and then returning them in one go.
        return "";
    }
}
