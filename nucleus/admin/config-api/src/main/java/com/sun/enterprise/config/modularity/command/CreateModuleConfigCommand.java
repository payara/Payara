/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.enterprise.config.util.ConfigApiLoggerInfo.*;

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.modularity.annotation.CustomConfiguration;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Dom;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A remote command to create the default configuration for a given service using the snippets available in the relevant module.
 *
 * @author Masoud Kalali
 */
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTER,
        CommandTarget.CONFIG, CommandTarget.STANDALONE_INSTANCE})
@ExecuteOn(RuntimeType.ALL)
@Service(name = "create-module-config")
@PerLookup
@I18n("create.module.config")
public final class CreateModuleConfigCommand extends AbstractConfigModularityCommand implements AdminCommand, AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {
    
    private final Logger LOG = getLogger();
    
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateModuleConfigCommand.class);

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Inject
    private Domain domain
            ;
    @Inject
    StartupContext startupContext;


    @Inject
    ServiceLocator serviceLocator;

    @Param(optional = true, defaultValue = "false", name = "dryRun")
    private Boolean dryRun;

    @Param(optional = true, defaultValue = "false", name = "all")
    private Boolean isAll;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    ServerEnvironmentImpl serverenv;

    @Param(optional = true, name = "serviceName", primary = true)
    private String serviceName;
    private ActionReport report;

    @Override
    public void execute(AdminCommandContext context) {
        String defaultConfigurationElements;

        if (!isAll && (serviceName == null)) {
            //TODO check for usability options, should we create the default configs, show them or fail the execution?
            report.setMessage(localStrings.getLocalString("create.module.config.no.service.no.all",
                    "No service name specified and the --all is not used either. Showing all default configurations not merged with domain configuration under target {0}.", target));

            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.failure",
                        "Failed to execute the command due to: {0}. For more details check the log file.", e.getLocalizedMessage());
                LOG.log(Level.INFO, CREATE_MODULE_CONFIG_FAILURE, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && dryRun) {
            report.setMessage(localStrings.getLocalString("create.module.config.show.all",
                    "Showing all default configurations not merged with domain configuration under target {0}.", target));
            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.module.config.show.all.failed",
                        "Failed to show all default configurations not merged with domain configuration under target {0} due to: {1}.", target, e.getLocalizedMessage());
                LogHelper.log(LOG, Level.INFO, CREATE_MODULE_CONFIG_SHOW_ALL_FAILED, e, target);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && !dryRun) {
            report.appendMessage(localStrings.getLocalString("create.module.config.creating.all",
                    "Creating all default configuration elements that are not present in the domain.xml under target {0}.", target));
            report.appendMessage(LINE_SEPARATOR);
            synchronized (configModularityUtils) {
                boolean oldv = configModularityUtils.isCommandInvocation();
                try {
                    configModularityUtils.setCommandInvocation(true);
                    createAllMissingElements(report);
                } catch (Exception e) {
                    String msg = localStrings.getLocalString("create.module.config.creating.all.failed",
                            "Failed to create all default configuration elements that are not present in the domain.xml under target {0} due to: {1}.", target, e.getLocalizedMessage());
                    LogHelper.log(LOG, Level.INFO, CREATE_MODULE_CONFIG_CREATING_ALL_FAILED, e, target);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setFailureCause(e);
                    return;
                } finally {
                    configModularityUtils.setCommandInvocation(oldv);
                }
            }
        } else if (serviceName != null) {
            String className = configModularityUtils.convertConfigElementNameToClassName(serviceName);
            Class configBeanType = configModularityUtils.getClassFor(serviceName);
            if (configBeanType == null) {
                String msg = localStrings.getLocalString("create.module.config.not.such.a.service.found",
                        "A ConfigBean of type {0} which translates to your service name\\'s configuration elements does not exist.", className, serviceName);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            synchronized (configModularityUtils) {
                boolean oldv = configModularityUtils.isCommandInvocation();
                try {
                    if (dryRun) {
                        String serviceDefaultConfig = getDefaultConfigFor(configBeanType);
                        if (serviceDefaultConfig != null) {
                            report.setMessage(serviceDefaultConfig);
                        }
                    } else {
                        configModularityUtils.setCommandInvocation(true);
                        createMissingElementFor(configBeanType, report);
                    }

                } catch (Exception e) {
                    String msg = localStrings.getLocalString("create.module.config.creating.for.service.name.failed",
                            "Failed to create module configuration for {0} under the target {1} due to: {2}.", serviceName, target, e.getMessage());
                    LogHelper.log(LOG, Level.INFO, CREATE_MODULE_CONFIG_CREATING_FOR_SERVICE_NAME_FAILED, e, serviceName, target);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    report.setFailureCause(e);
                    return;
                } finally {
                    configModularityUtils.setCommandInvocation(oldv);
                }
            }
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private boolean createMissingElementFor(Class configBeanType, ActionReport report) throws Exception {
        boolean defaultConfigCreated = false;
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            if (config.checkIfExtensionExists(configBeanType)) {
                report.appendMessage(localStrings.getLocalString("create.module.config.already.exists",
                        "Configuration for {0} already exists. The command didn't change the existing configuration.", Dom.convertName(configBeanType.getSimpleName())));
                report.appendMessage(LINE_SEPARATOR);
                return false;
            }
            config.getExtensionByType(configBeanType);
            report.appendMessage(localStrings.getLocalString("create.module.config.done",
                    "Configuration for {0} added to domain.xml", Dom.convertName(configBeanType.getSimpleName())));
            report.appendMessage(LINE_SEPARATOR);
            defaultConfigCreated = true;
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfExtensionExists(configBeanType)) {
                report.appendMessage(localStrings.getLocalString("create.module.config.already.exists",
                        "Configuration for {0} already exists. The command didn't change the existing configuration.", Dom.convertName(configBeanType.getSimpleName())));
                report.appendMessage(LINE_SEPARATOR);
                return false;
            }
            domain.getExtensionByType(configBeanType);
            report.appendMessage(localStrings.getLocalString("create.module.config.done",
                    "Configuration for {0} added to domain.xml", Dom.convertName(configBeanType.getSimpleName())));
            report.appendMessage(LINE_SEPARATOR);
            defaultConfigCreated = true;
        }
        return defaultConfigCreated;
    }

    private String getDefaultConfigFor(Class configBeanType) throws Exception {
        if (!configModularityUtils.hasCustomConfig(configBeanType)) {
            return configModularityUtils.serializeConfigBeanByType(configBeanType);
        } else {

            List<ConfigBeanDefaultValue> defaults = configModularityUtils.getDefaultConfigurations(configBeanType, configModularityUtils.getRuntimeTypePrefix(serverenv.getStartupContext()));
            StringBuilder builder = new StringBuilder();
            for (ConfigBeanDefaultValue value : defaults) {
                builder.append(localStrings.getLocalString("at.location",
                        "At Location: "));
                builder.append(replaceExpressionsWithValues(value.getLocation()));
//                builder.append(LINE_SEPARATOR);
                String substituted = replacePropertiesWithDefaultValues(value.getCustomizationTokens(),
                        value.getXmlConfiguration());
                builder.append(substituted);
//                builder.append(LINE_SEPARATOR);
            }
            builder.deleteCharAt(builder.length() - 1);
            return builder.toString();
        }
    }

    private String replacePropertiesWithDefaultValues(List<ConfigCustomizationToken> tokens, String xmlConfig) {
        for (ConfigCustomizationToken token : tokens) {
            String toReplace = "\\$\\{" + token.getName() + "\\}";
            xmlConfig = xmlConfig.replaceAll(toReplace, token.getValue());
        }
        return xmlConfig;
    }

    private void createAllMissingElements(ActionReport report) throws Exception {
        List<Class> clzs = configModularityUtils.getAnnotatedConfigBeans(CustomConfiguration.class);
        for (Class clz : clzs) {
            createMissingElementFor(clz, report);
        }
    }

    private String getAllDefaultConfigurationElements(String target) throws Exception {
        //TODO for now just cover the config beans with annotations, at a later time cover all config beans of type
        // config extension or domain extension.
        List<Class> clzs = configModularityUtils.getAnnotatedConfigBeans(CustomConfiguration.class);
        StringBuilder sb = new StringBuilder();
        for (Class clz : clzs) {
            sb.append(getDefaultConfigFor(clz));
//            sb.append(LINE_SEPARATOR);
        }
        return sb.toString();
    }

    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        Class configBeanType = null;
        if (serviceName == null && isAll) {
            List<AccessRequired.AccessCheck> l = new ArrayList<AccessRequired.AccessCheck>();
            List<Class> clzs = configModularityUtils.getAnnotatedConfigBeans(CustomConfiguration.class);
            for (Class clz : clzs) {
                List<ConfigBeanDefaultValue> configBeanDefaultValueList =
                        configModularityUtils.getDefaultConfigurations(clz, configModularityUtils.getRuntimeTypePrefix(startupContext));
                l.addAll(getAccessChecksForDefaultValue(configBeanDefaultValueList, target, Arrays.asList("read", "create", "delete")));
            }
            return l;
        } else if (serviceName == null) {
            return Collections.emptyList();
        } else {
            configBeanType = configModularityUtils.getClassFor(serviceName);
            if (configBeanType == null) {
                //TODO check if this is the correct course of action.
                return Collections.emptyList();
            }

            if (configModularityUtils.hasCustomConfig(configBeanType)) {
                List<ConfigBeanDefaultValue> defaults = configModularityUtils.getDefaultConfigurations(configBeanType,
                        configModularityUtils.getRuntimeTypePrefix(serverenv.getStartupContext()));
                return getAccessChecksForDefaultValue(defaults, target, Arrays.asList("read"));
            }

            if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
                return getAccessChecksForConfigBean(config.getExtensionByType(configBeanType), target, Arrays.asList("read", "create", "delete"));
            }
            if (configBeanType.isAssignableFrom(DomainExtension.class)) {
                return getAccessChecksForConfigBean(domain.getExtensionByType(configBeanType), target, Arrays.asList("read", "create", "delete"));
            }
            //TODO check if this is right course of action
            return Collections.emptyList();
        }
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        report = context.getActionReport();
        if (target != null) {
            Config newConfig = getConfigForName(target, serviceLocator, domain);
            if (newConfig != null) {
                config = newConfig;
            }
            if (config == null) {
                report.setMessage(localStrings.getLocalString("create.module.config.target.name.invalid",
                        "The target name specified is invalid. Please double check the target name and try again"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }
        }
        if (isAll && (serviceName != null)) {
            report.setMessage(localStrings.getLocalString("create.module.config.service.name.ignored",
                    "One of the --all service name parameters can be used at a time. These two options can not be used together."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }
}
