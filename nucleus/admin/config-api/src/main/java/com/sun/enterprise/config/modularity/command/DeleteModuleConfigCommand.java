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
import com.sun.enterprise.config.modularity.customization.ConfigBeanDefaultValue;
import com.sun.enterprise.config.modularity.customization.ConfigCustomizationToken;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.config.serverbeans.SystemPropertyBag;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import javax.inject.Named;

import java.beans.PropertyVetoException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A remote command to delete a module's configuration.
 *
 * @author Masoud Kalali
 */
@TargetType(value = {CommandTarget.DAS, CommandTarget.CLUSTER,
        CommandTarget.CONFIG, CommandTarget.STANDALONE_INSTANCE, CommandTarget.DOMAIN})
@ExecuteOn(RuntimeType.ALL)
@Service(name = "delete-module-config")
@PerLookup
@I18n("delete.module.config")
public final class DeleteModuleConfigCommand extends AbstractConfigModularityCommand implements AdminCommand, AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {
    private final Logger LOG = getLogger();
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DeleteModuleConfigCommand.class);
    private static final String DEFAULT_FORMAT = "";
    private ActionReport report;

    @Inject
    private Domain domain;

    @Inject
    private
    ServiceLocator serviceLocator;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Param(name = "serviceName", primary = true)
    private String serviceName;


    @Inject
    ServerEnvironment serverenv;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        if (target != null) {
            Config newConfig = getConfigForName(target, serviceLocator, domain);
            if (newConfig != null) {
                config = newConfig;
            }
            if (config == null) {
                report.setMessage(localStrings.getLocalString("delete.module.config.target.name.invalid",
                        "The target name specified is invalid. Please double check the target name and try again."));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (serviceName == null) {
            report.setMessage(localStrings.getLocalString("delete.module.config.service.name.is.required",
                    "The service name is required, please specify which service you want to delete its default configuration."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final String className = configModularityUtils.convertConfigElementNameToClassName(serviceName);
        Class configBeanType = configModularityUtils.getClassFor(serviceName);
        if (configBeanType == null) {
            String msg = localStrings.getLocalString("delete.module.config.not.such.a.service.found",
                    "Your service name does not match any service installed on this domain.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        if (configModularityUtils.hasCustomConfig(configBeanType)) {
            List<ConfigBeanDefaultValue> defaults = configModularityUtils.getDefaultConfigurations(configBeanType, configModularityUtils.getRuntimeTypePrefix(serverenv.getStartupContext()));
            deleteDependentConfigElements(defaults);
        } else {
            deleteTopLevelExtensionByType(config, className, configBeanType);
        }
    }

    private void deleteDependentConfigElements(final List<ConfigBeanDefaultValue> defaults) {
        for (ConfigBeanDefaultValue configBeanDefaultValue : defaults) {
            deleteDependentConfigElement(configBeanDefaultValue);
        }
    }

    private void deleteDependentConfigElement(final ConfigBeanDefaultValue defaultValue) {
        Class parentClass = configModularityUtils.getOwningClassForLocation(defaultValue.getLocation());
        final Class configBeanClass = configModularityUtils.getClassForFullName(defaultValue.getConfigBeanClassName());
        final Method m = configModularityUtils.findSuitableCollectionGetter(parentClass, configBeanClass);
        if (m != null) {
            try {
                final ConfigBeanProxy parent = configModularityUtils.getOwningObject(defaultValue.getLocation());
                ConfigSupport.apply(new SingleConfigCode<ConfigBeanProxy>() {
                    @Override
                    public Object run(ConfigBeanProxy param) throws PropertyVetoException,
                            TransactionFailure {
                        List col = null;
                        ConfigBeanProxy configBean = null;
                        try {
                            col = (List) m.invoke(param);
                            if (col != null) {
                                configBean = configModularityUtils.getCurrentConfigBeanForDefaultValue(defaultValue);
                            }
                        } catch (Exception e) {
                            String message = localStrings.getLocalString("delete.module.config.failed.deleting.dependant",
                                    "Failed to remove all configuration elements related to your service form domain.xml. You can use create-module-config --dryRun with your module name to see all relevant configurations and try removing the config elements ");
                            report.setMessage(message);
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            LOG.log(Level.INFO, DELETE_MODULE_CONFIG_FAILED_DELETING_DEPENDENT, e);
                        }

                        if (configBean != null) {
                            boolean deleted = configModularityUtils.deleteConfigurationForConfigBean(configBean, col, defaultValue);
                            if (!deleted) {
                                for (int i = 0; i < col.size(); i++) {
                                    if (configBeanClass.isAssignableFrom(col.get(i).getClass())) {
                                        col.remove(col.get(i));
                                        removeCustomTokens(defaultValue, configBean, parent);
                                        return param;
                                    }
                                }
                            }

                        }
                        return param;
                    }
                }, parent);
            } catch (Exception e) {
                String message = localStrings.getLocalString("delete.module.config.failed.deleting.dependant",
                        "Failed to remove all configuration elements related to your service form domain.xml. You can use create-module-config --dryRun with your module name to see all relevant configurations and try removing the config elements ");
                report.setMessage(message);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                LOG.log(Level.INFO, DELETE_MODULE_CONFIG_FAILED_DELETING_DEPENDENT, e);

            }
        } else {
            report.setMessage(localStrings.getLocalString("delete.module.config.failed.deleting.dependant",
                    "Failed to remove all configuration elements related to your service form domain.xml. You can use create-module-config --dryRun with your module name to see all relevant configurations and try removing the config elements "));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }

    private void deleteTopLevelExtensionByType(Config config, final String className, Class configBeanType) {
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            if (config.checkIfExtensionExists(configBeanType)) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Config>() {
                        @Override
                        public Object run(Config param) throws PropertyVetoException,
                                TransactionFailure {
                            List<ConfigExtension> configExtensions;
                            configExtensions = param.getExtensions();
                            for (ConfigExtension ext : configExtensions) {
                                String configExtensionClass = GlassFishConfigBean.unwrap(ext).getProxyType().getSimpleName();
                                if (configExtensionClass.equals(className)) {
                                    configExtensions.remove(ext);
                                    break;
                                }
                            }
                            return configExtensions;
                        }
                    }, config);
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                } catch (TransactionFailure e) {
                    String actual = e.getMessage();
                    String msg = localStrings.getLocalString("delete.module.config.failed.to.delete.config",
                            DEFAULT_FORMAT, serviceName, actual);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setFailureCause(e);
                }
            } else {
                report.setMessage(localStrings.getLocalString("delete.module.config.no.configuration",
                        "No customized configuration exist for this service nor the default configuration has been added to the domain.xml."));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }

        } else if (DomainExtension.class.isAssignableFrom(configBeanType)) {
            if (domain.checkIfExtensionExists(configBeanType)) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Domain>() {
                        @Override
                        public Object run(Domain param) throws PropertyVetoException,
                                TransactionFailure {
                            List<DomainExtension> domainExtensions;
                            domainExtensions = param.getExtensions();
                            for (DomainExtension ext : domainExtensions) {
                                String configExtensionClass = GlassFishConfigBean.unwrap(ext).getProxyType().getSimpleName();
                                if (configExtensionClass.equals(className)) {
                                    domainExtensions.remove(ext);
                                    break;
                                }
                            }
                            return domainExtensions;
                        }
                    }, domain);
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                } catch (TransactionFailure e) {
                    String actual = e.getMessage();
                    String msg = localStrings.getLocalString("delete.module.config.failed.to.delete.config",
                            DEFAULT_FORMAT, serviceName, actual);
                    report.setMessage(msg);
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setFailureCause(e);
                }
            } else {
                report.setMessage(localStrings.getLocalString("delete.module.config.no.configuration",
                        "No customized configuration exist for this service nor the default configuration has been added to the domain.xml."));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }

        }
    }

    private static <T extends ConfigBeanProxy> boolean removeCustomTokens(final ConfigBeanDefaultValue configBeanDefaultValue, T finalConfigBean, ConfigBeanProxy parent) throws TransactionFailure, PropertyVetoException {
        if (parent instanceof SystemPropertyBag) {
            removeSystemPropertyForTokens(configBeanDefaultValue.getCustomizationTokens(), (SystemPropertyBag) parent);
            return true;
        } else {
            ConfigBeanProxy curParent = finalConfigBean;
            while (!(curParent instanceof SystemPropertyBag)) {
                curParent = curParent.getParent();
            }
            if (configBeanDefaultValue.getCustomizationTokens().size() != 0) {
                final SystemPropertyBag bag = (SystemPropertyBag) curParent;
                final List<ConfigCustomizationToken> tokens = configBeanDefaultValue.getCustomizationTokens();
                removeSystemPropertyForTokens(tokens, bag);
                return true;
            }
            return false;
        }
    }

    private static void removeSystemPropertyForTokens(List<ConfigCustomizationToken> tokens, SystemPropertyBag bag) throws TransactionFailure {
        for (ConfigCustomizationToken token : tokens) {
            if (bag.containsProperty(token.getName())) {
                bag.getSystemProperty().remove(bag.getSystemProperty(token.getName()));
            }
        }
    }

    @Override
    public Collection<? extends AccessRequired.AccessCheck> getAccessChecks() {
        Class configBeanType = configModularityUtils.getClassFor(serviceName);
        if (configBeanType == null) {
            //TODO check if this is the correct course of action.
            return Collections.emptyList();
        }

        if (configModularityUtils.hasCustomConfig(configBeanType)) {
            List<ConfigBeanDefaultValue> defaults = configModularityUtils.getDefaultConfigurations(configBeanType,
                    configModularityUtils.getRuntimeTypePrefix(serverenv.getStartupContext()));
            return getAccessChecksForDefaultValue(defaults, target, Arrays.asList("read", "delete"));
        }

        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            return getAccessChecksForConfigBean(config.getExtensionByType(configBeanType), target, Arrays.asList("read", "delete"));
        }
        if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            return getAccessChecksForConfigBean(config.getExtensionByType(configBeanType), target, Arrays.asList("read", "delete"));
        }
        //TODO check if this is right course of action
        return Collections.emptyList();
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        //TODO check if it is actually required to use this method to infer the resources etc or only using the
        return true;
    }
}
