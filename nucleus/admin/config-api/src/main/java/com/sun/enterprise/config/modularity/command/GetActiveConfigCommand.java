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
import org.jvnet.hk2.config.ConfigBeanProxy;

import javax.inject.Inject;
import javax.inject.Named;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get the current active configuration of a service and print it out for the user's review.
 *
 * @author Masoud Kalali
 */
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTER,
        CommandTarget.CONFIG, CommandTarget.STANDALONE_INSTANCE, CommandTarget.DOMAIN})
@ExecuteOn(RuntimeType.ALL)
@Service(name = "get-active-module-config")
@PerLookup
@I18n("get.active.config")
public final class GetActiveConfigCommand extends AbstractConfigModularityCommand implements AdminCommand, AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    private final Logger LOG = getLogger();
    
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(GetActiveConfigCommand.class);

    private ActionReport report;

    @Inject
    private Domain domain;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Inject
    StartupContext startupContext;

    @Inject
    private
    ServiceLocator serviceLocator;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(optional = true, defaultValue = "false", name = "all")
    private Boolean isAll;

    @Param(name = "serviceName", primary = true, optional = true)
    private String serviceName;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    ServerEnvironmentImpl serverenv;

    @Override
    public void execute(AdminCommandContext context) {
        if (serviceName != null) {
            String className = configModularityUtils.convertConfigElementNameToClassName(serviceName);
            Class configBeanType = configModularityUtils.getClassFor(serviceName);
            if (configBeanType == null) {
                String msg = localStrings.getLocalString("get.active.config.not.such.a.service.found",
                        "A ConfigBean of type {0} which translates to your service name\\'s does not exist.", className, serviceName);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            try {
                String serviceDefaultConfig = getActiveConfigFor(configBeanType);
                if (serviceDefaultConfig != null) {
                    report.setMessage(serviceDefaultConfig);
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return;
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("get.active.config.getting.active.config.for.service.failed",
                        "Failed to get active configuration for {0} under the target {1} due to: {2}.", serviceName, target, e.getMessage());
                LogHelper.log(LOG, Level.INFO, GET_ACTIVE_CONFIG_FOR_SERVICE_FAILED, e, serviceName, target);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                report.setFailureCause(e);
                return;
            }
        }

        if (isAll) {
            List<Class> configBeans = configModularityUtils.getAnnotatedConfigBeans(CustomConfiguration.class);
            for (Class clz : configBeans) {
                try {
                    String serviceDefaultConfig = getActiveConfigFor(clz);
                    if (serviceDefaultConfig != null) {
                        report.setMessage(serviceDefaultConfig);
                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return;
                    }
                } catch (Exception e) {
                    String msg = localStrings.getLocalString("get.active.config.getting.active.config.for.service.failed",
                            "Failed to get active configuration for {0} under the target {1} due to: {2}.", serviceName, target, e.getMessage());
                    LogHelper.log(LOG, Level.INFO, GET_ACTIVE_CONFIG_FOR_SERVICE_FAILED, e, serviceName, target);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage(msg);
                    report.setFailureCause(e);
                    return;
                }
            }
        }
        //TODO for now just cover the config beans with annotations, at a later time cover all config beans of type
        // config extension or domain extension.
        List<Class> clzs = configModularityUtils.getAnnotatedConfigBeans(CustomConfiguration.class);
        StringBuilder sb = new StringBuilder();

        try {
            for (Class clz : clzs) {
                sb.append(getActiveConfigFor(clz));
                sb.append(LINE_SEPARATOR);
            }
            report.appendMessage(sb.toString());
        } catch (Exception e) {
            String msg = localStrings.getLocalString("get.active.config.getting.active.config.for.service.failed",
                    "Failed to get active configuration for {0} under the target {1} due to: {2}.", serviceName, target, e.getMessage());
            LogHelper.log(LOG, Level.INFO, GET_ACTIVE_CONFIG_FOR_SERVICE_FAILED, e, serviceName, target);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            report.setFailureCause(e);
            return;
        }
    }

    private String getActiveConfigFor(Class configBeanType) throws InvocationTargetException, IllegalAccessException {

        if (configModularityUtils.hasCustomConfig(configBeanType)) {
            List<ConfigBeanDefaultValue> defaults = configModularityUtils.getDefaultConfigurations(configBeanType,
                    configModularityUtils.getRuntimeTypePrefix(serverenv.getStartupContext()));
            return getCompleteConfiguration(defaults);
        }

        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            if (config.checkIfExtensionExists(configBeanType)) {
                return configModularityUtils.serializeConfigBean(config.getExtensionByType(configBeanType));
            } else {
                return configModularityUtils.serializeConfigBeanByType(configBeanType);
            }

        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfExtensionExists(configBeanType)) {
                return configModularityUtils.serializeConfigBean(domain.getExtensionByType(configBeanType));
            }
            return configModularityUtils.serializeConfigBeanByType(configBeanType);
        }
        return null;
    }

    private String getCompleteConfiguration(List<ConfigBeanDefaultValue> defaults)
            throws InvocationTargetException, IllegalAccessException {
        StringBuilder builder = new StringBuilder();
        for (ConfigBeanDefaultValue value : defaults) {
            builder.append(localStrings.getLocalString("at.location",
                    "At Location: "));
            builder.append(replaceExpressionsWithValues(value.getLocation()));
            builder.append(LINE_SEPARATOR);
            String substituted = configModularityUtils.replacePropertiesWithCurrentValue(
                    getDependentConfigElement(value), value);
            builder.append(substituted);
            builder.append(LINE_SEPARATOR);
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }


    private String getDependentConfigElement(ConfigBeanDefaultValue defaultValue)
            throws InvocationTargetException, IllegalAccessException {
        ConfigBeanProxy configBean = configModularityUtils.getCurrentConfigBeanForDefaultValue(defaultValue);
        if (configBean != null) {
            return configModularityUtils.serializeConfigBean(configBean);
        } else {
            return defaultValue.getXmlConfiguration();
        }
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
                l.addAll(getAccessChecksForDefaultValue(configBeanDefaultValueList, target, Arrays.asList("read")));
            }
            return l;
        } else if(serviceName == null) {
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
                return getAccessChecksForConfigBean(config.getExtensionByType(configBeanType), target, Arrays.asList("read"));
            }
            if (configBeanType.isAssignableFrom(DomainExtension.class)) {
                return getAccessChecksForConfigBean(domain.getExtensionByType(configBeanType), target, Arrays.asList("read"));
            }
            //TODO check if this is right course of action
            return Collections.emptyList();
        }
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        report = context.getActionReport();
        if (serviceName == null && isAll == false) {
            report.setMessage(localStrings.getLocalString("get.active.config.service.name.required",
                    "You need to specify a service name to get it's active configuration."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        if (target != null) {
            Config newConfig = getConfigForName(target, serviceLocator, domain);
            if (newConfig != null) {
                config = newConfig;
            }
            if (config == null) {
                report.setMessage(localStrings.getLocalString("get.active.config.target.name.invalid",
                        "The target name you specified is invalid. Please double check the target name and try again"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }
        }

        if (isAll == true && serviceName != null) {
            report.setMessage(localStrings.getLocalString("get.active.config.target.service.and.all.exclusive",
                    "Specifying a service name and using --all=true can not be used together."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }
}
