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

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBeanProxy;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get the current active configuration of a service and print it out for the user's review.
 *
 * @author Masoud Kalali
 */
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTER,
                     CommandTarget.CONFIG, CommandTarget.STANDALONE_INSTANCE})
@ExecuteOn(RuntimeType.ALL)
@Service(name = "get-active-config")
@PerLookup
@I18n("get.active.config")
public final class GetActiveConfigCommand extends AbstractConfigModularityCommand implements AdminCommand {

    private final Logger LOG = Logger.getLogger(GetActiveConfigCommand.class.getName());
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(GetActiveConfigCommand.class);
    private static final String DEFAULT_FORMAT = "";

    private ActionReport report;

    @Inject
    private Domain domain;

    @Inject
    private
    Habitat habitat;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
        @Named (ServerEnvironmentImpl.DEFAULT_INSTANCE_NAME)
        ServerEnvironmentImpl serverenv;

    @Param(name = "serviceName", primary = true, optional = false)
    private String serviceName;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        if (serviceName == null) {
            report.setMessage(localStrings.getLocalString("get.active.config.service.name.required",
                    "You need to specify a service name to get it's active configuration."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        if (target != null) {
            Config newConfig =getConfigForName(target,habitat,domain);
            if (newConfig!=null){
                config= newConfig;
            }
            if (config == null) {
                report.setMessage(localStrings.getLocalString("get.active.config.target.name.invalid",
                        "The target name you specified is invalid. Please double check the target name and try again"));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

            String className = ConfigModularityUtils.convertConfigElementNameToClassName(serviceName);
            Class configBeanType = ConfigModularityUtils.getClassFor(serviceName, habitat);
            if (configBeanType == null) {
                String msg = localStrings.getLocalString("get.active.config.not.such.a.service.found",
                        DEFAULT_FORMAT, className, serviceName);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            try {
                String serviceDefaultConfig = getActiveConfigFor(configBeanType, habitat);
                if (serviceDefaultConfig != null) {
                    report.setMessage(serviceDefaultConfig);
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("get.active.config.getting.active.config.for.service.failed",
                        DEFAULT_FORMAT, serviceName, target, e.getMessage());
                LOG.log(Level.INFO, msg, e);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                report.setFailureCause(e);
            }
    }

    private String getActiveConfigFor(Class configBeanType, Habitat habitat) throws InvocationTargetException, IllegalAccessException {

        if (ConfigModularityUtils.hasCustomConfig(configBeanType)) {
            List<ConfigBeanDefaultValue> defaults = ConfigModularityUtils.getDefaultConfigurations(configBeanType, serverenv.isDas());
            return getCompleteConfiguration(defaults);
        }

        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            if (config.checkIfExtensionExists(configBeanType)) {
                return ConfigModularityUtils.serializeConfigBean(config.getExtensionByType(configBeanType));
            } else {
                return ConfigModularityUtils.serializeConfigBeanByType(configBeanType, habitat);
            }

        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfExtensionExists(configBeanType)) {
                return ConfigModularityUtils.serializeConfigBean(domain.getExtensionByType(configBeanType));
            }
            return ConfigModularityUtils.serializeConfigBeanByType(configBeanType, habitat);
        }
        return null;
    }

    private String getCompleteConfiguration(List<ConfigBeanDefaultValue> defaults) throws InvocationTargetException, IllegalAccessException {
        StringBuilder builder = new StringBuilder();
        for (ConfigBeanDefaultValue value : defaults) {
            builder.append(localStrings.getLocalString("at.location",
                    "At Location:"));
            builder.append(replaceExpressionsWithValues(value.getLocation(),habitat));
            builder.append(System.getProperty("line.separator"));
            String substituted = ConfigModularityUtils.replacePropertiesWithCurrentValue(
                    getDependentConfigElement(value), value, habitat);
            builder.append(substituted);
            builder.append(System.getProperty("line.separator"));
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();

    }

    private String getDependentConfigElement(ConfigBeanDefaultValue defaultValue) throws InvocationTargetException, IllegalAccessException {
        ConfigBeanProxy configBean = ConfigModularityUtils.getCurrentConfigBeanForDefaultValue(defaultValue, habitat);
        if (configBean != null) {
            return ConfigModularityUtils.serializeConfigBean(configBean);
        } else {
            return defaultValue.getXmlConfiguration();
        }

    }


}
