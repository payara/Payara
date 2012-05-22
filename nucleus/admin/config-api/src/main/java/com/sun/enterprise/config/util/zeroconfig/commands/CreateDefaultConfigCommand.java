/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.config.util.zeroconfig.commands;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.DomainExtension;
import com.sun.enterprise.config.util.zeroconfig.ZeroConfigUtils;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigBeanProxy;

import javax.inject.Inject;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
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
@Service(name = "create-default-config")
@Scoped(PerLookup.class)
@I18n("create.default.config")
public final class CreateDefaultConfigCommand implements AdminCommand {
    private final Logger LOG = Logger.getLogger(CreateDefaultConfigCommand.class.getName());
    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(CreateDefaultConfigCommand.class);
    private static final String DEFAULT_FORMAT = "";

    @Inject
    private Domain domain;

    @Inject
    ModulesRegistry registry;

    private final static String PROPERTY_PREFIX = "-";

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
        String defaultConfigurationElements = null;
        if (isAll && serviceName != null) {
            report.setMessage(localStrings.getLocalString("create.default.config.service.name.ignored",
                    "You can only use --all or specify a service name. These two options are exclusive."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!isAll && serviceName == null) {
            //TODO check for usability options, should we create the default configs, show them or fail the execution?
            report.setMessage(localStrings.getLocalString("create.default.config.no.service.no.all",
                    DEFAULT_FORMAT, target));
//            report.appendMessage("\n");

            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.default.config.failure",
                        DEFAULT_FORMAT, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && dryRun) {
            report.setMessage(localStrings.getLocalString("create.default.config.show.all",
                    DEFAULT_FORMAT, target));
//            report.appendMessage("\n");
            try {
                defaultConfigurationElements = getAllDefaultConfigurationElements(target);
                if (defaultConfigurationElements != null) {
                    report.setMessage(defaultConfigurationElements);
                }
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.default.config.show.all.failed",
                        DEFAULT_FORMAT, target, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (isAll && !dryRun) {
            report.setMessage(localStrings.getLocalString("create.default.config.creating.all",
                    DEFAULT_FORMAT, target));
            try {
                createAllMissingElements(target);
            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.default.config.creating.all.failed",
                        DEFAULT_FORMAT, target, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
        } else if (serviceName != null) {
            report.setMessage(localStrings.getLocalString("create.default.config.creating.for.service.name",
                    DEFAULT_FORMAT, serviceName, target));
            String className = ZeroConfigUtils.convertConfigElementNameToClassNAme(serviceName);
            Class configBeanType = null;
            configBeanType = getClassFor(className, serviceName, report);
            if (configBeanType == null) {
                String msg = localStrings.getLocalString("create.default.config.not.such.a.service.found",
                        DEFAULT_FORMAT, className, serviceName);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                return;
            }
            try {
                if (!dryRun) {
                    createMissingElementFor(configBeanType, serviceName, target, report);
                } else {
                    String serviceDefaultConfig = getDefaultConfigFor(configBeanType, report, serviceName);
                    report.setMessage(serviceDefaultConfig);
                }

            } catch (Exception e) {
                String msg = localStrings.getLocalString("create.default.config.creating.for.service.name",
                        DEFAULT_FORMAT, serviceName, target, e.getLocalizedMessage());
                LOG.log(Level.INFO, msg, e);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(msg);
                report.setFailureCause(e);
                return;
            }
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        return;

    }

    private String getDefaultConfigFor(Class configBean, ActionReport report, String serviceName) throws Exception {
        String configurationContent = null;
        if (getDefaultSnippetUrl(configBean) == null) {
            report.setMessage(localStrings.getLocalString("create.default.config.config.embedded.in.class", DEFAULT_FORMAT, serviceName));
            //No snippet, try creating the xml using the bean itself. but add the message telling the user it comes
            // form the bean itself and not the snippet.
        } else {
            InputStream st = getDefaultSnippetUrl(configBean).openStream();
            configurationContent = ZeroConfigUtils.streamToString(st, "utf-8");
        }
        return configurationContent;
    }

    private void createMissingElementFor(Class configBeanType, String serviceName, String target, ActionReport report) throws Exception {
        if (ConfigExtension.class.isAssignableFrom(configBeanType)) {
            Config c = domain.getConfigNamed(target);
            if (c.checkIfConfigExists(configBeanType)) {
                report.setMessage(localStrings.getLocalString("create.default.config.already.exists", DEFAULT_FORMAT, serviceName));
                return;
            }
            c.getExtensionByType(configBeanType);
        } else if (configBeanType.isAssignableFrom(DomainExtension.class)) {
            if (domain.checkIfConfigExists(configBeanType)) {
                report.setMessage(localStrings.getLocalString("create.default.config.already.exists", DEFAULT_FORMAT, serviceName));
                return;
            }
            domain.getExtensionByType(configBeanType);
        }
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

    private <P extends ConfigBeanProxy> URL getDefaultSnippetUrl(Class<P> configBean) {
        Collection<Module> modules = registry.getModules();
        Class cls = null;
        String xmlSnippetFileLocation = "META-INF/" + configBean.getSimpleName() + ".xml";
        for (Module m : modules) {
            URL url = m.getClassLoader().getResource(xmlSnippetFileLocation);
            if (url != null) return url;
        }
        return null;
    }

    private Class getClassFor(String className, String serviceName, ActionReport report) {
        Collection<Module> modules = registry.getModules();
        Class cls = null;
        for (Module m : modules) {
            try {
                cls = m.getClassLoader().loadClass(className);
                if (cls != null) {
                 break;
                }
            } catch (ClassNotFoundException e) {
                //ignore it, not the right classloader
            }
        }
        return cls;
    }
}
