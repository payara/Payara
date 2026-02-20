/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.hazelcast.admin;

import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.FileSystemYamlConfig;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.nucleus.hazelcast.HazelcastConfigSpecificConfiguration;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import java.beans.PropertyVetoException;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.glassfish.internal.deployment.DeploymentTargetResolver;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author steve
 */
@Service(name = "set-hazelcast-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.hazelcast.configuration")
@TargetType(value = {CommandTarget.CONFIG, CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE})
@ExecuteOn(value = {RuntimeType.ALL})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-hazelcast-configuration",
            description = "Set Hazelcast Configuration")
})
public class SetHazelcastConfiguration implements AdminCommand, DeploymentTargetResolver {
    
    @Inject
    protected Logger logger;

    @Inject
    protected HazelcastCore hazelcastCore;

    @Inject
    private Domain domain;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "domain")
    String target;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "hazelcastConfigurationFile", shortName = "f", optional = true)
    private String configFile;

    @Param(name = "startPort", optional = true)
    private String startPort;

    @Param(name = "publicAddress", optional = true)
    private String publicAddress;
    
    @Param(name = "dasPublicAddress", optional = true)
    private String dasPublicAddress;

    @Param(name = "dasBindAddress", optional = true)
    private String dasBindAddress;

    @Param(name = "dasPort", optional = true)
    private String dasPort;

    @Param(name = "clusterMode", optional = true, acceptableValues = "domain,multicast,tcpip,dns,kubernetes")
    private String clusterMode;

    @Param(name = "tcpIpMembers", optional = true)
    private String tcpipMembers;
    
    @Param(name = "dnsMembers", optional = true)
    private String dnsMembers;

    @Param(name = "interfaces", optional = true)
    private String interfaces;

    @Param(name = "multicastGroup", shortName = "g", optional = true)
    private String multiCastGroup;

    @Param(name = "multicastPort", optional = true)
    private String multicastPort;

    @Param(name = "clusterName", optional = true)
    private String hzClusterName;

    @Param(name = "jndiName", shortName = "j", optional = true)
    private String jndiName;

    @Param(name = "cacheManagerJndiName", optional = true)
    private String cacheManagerJndiName;

    @Param(name = "cachingProviderJndiName", optional = true)
    private String cachingProviderJndiName;

    @Param(name = "executorPoolSize", optional = true)
    private String executorPoolSize;

    @Param(name = "executorQueueCapacity", optional = true)
    private String executorQueueCapacity;

    @Param(name = "scheduledExecutorPoolSize", optional = true)
    private String scheduledExecutorPoolSize;

    @Param(name = "scheduledExecutorQueueCapacity", optional = true)
    private String scheduledExecutorQueueCapacity;

    @Param(name = "licenseKey", shortName = "lk", optional = true)
    private String licenseKey;

    @Param(name = "lite", optional = true)
    private Boolean lite;

    @Param(name = "hostawarePartitioning", optional = true)
    private Boolean hostawarePartitioning;

    @Param(name = "memberName", optional = true)
    private String memberName;

    @Param(name = "memberGroup", optional = true)
    private String memberGroup;
    
    @Param(name = "kubernetesNamespace", optional = true, alias = "kubernetesnamespace")
    private String kubernetesNamespace;
    
    @Param(name = "kubernetesServiceName", optional = true, alias = "kubernetesservicename")
    private String kubernetesServiceName;

    @Param(name = "autoIncrementPort", optional = true)
    private Boolean autoIncrementPort;
  
    @Param(name = "configSpecificDataGridStartPort", optional = true, alias = "configspecificdatagridstartport")
    private String configSpecificDataGridStartPort;

    @Param(name = "encryptDatagrid", optional = true, alias = "encryptdatagrid")
    private Boolean encryptDatagrid;
    
    @Inject
    ServiceLocator serviceLocator;

    @Inject
    ServerEnvironment server;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (!validate(actionReport)) {
            return;
        }
        if (configFile != null && !"hazelcast-config.xml".equals(configFile)) {
            File configFile = new File(this.configFile);
            if (!configFile.exists()) {
                String message = "Hazelcast config file not found: " + this.configFile;
                logger.log(Level.INFO, message);
                actionReport.setMessage(message);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
            try {
                final String fileName = configFile.getName();
                if (fileName.endsWith(".xml")) {
                    new FileSystemXmlConfig(configFile);
                } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                    new FileSystemYamlConfig((configFile));
                } else {
                    String message = "Hazelcast config file type not allowed. Should be a XML or YAML file.";
                    logger.log(Level.INFO, message);
                    actionReport.setMessage(message);
                    actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            } catch (Exception ex) {
                String message = "Hazelcast config file parsing exception: " + ex.toString();
                logger.log(Level.INFO, message);
                actionReport.setMessage(message);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        HazelcastRuntimeConfiguration hazelcastRuntimeConfiguration = domain.getExtensionByType(HazelcastRuntimeConfiguration.class);
        if (hazelcastRuntimeConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<HazelcastRuntimeConfiguration>() {
                    @Override
                    public Object run(final HazelcastRuntimeConfiguration hazelcastRuntimeConfigurationProxy) throws PropertyVetoException, TransactionFailure {
                        if (configFile != null) {
                            if (!configFile.equals(hazelcastRuntimeConfiguration.getHazelcastConfigurationFile())
                                    && configFile.equals("hazelcast-config.xml")) {
                                hazelcastRuntimeConfigurationProxy.setChangeToDefault("true");
                            }
                            hazelcastRuntimeConfigurationProxy.setHazelcastConfigurationFile(configFile);
                        }
                        if (startPort != null) {
                            hazelcastRuntimeConfigurationProxy.setStartPort(startPort);
                        }
                        if (multiCastGroup != null) {
                            hazelcastRuntimeConfigurationProxy.setMulticastGroup(multiCastGroup);
                        }
                        if (multicastPort != null) {
                            hazelcastRuntimeConfigurationProxy.setMulticastPort(multicastPort);
                        }
                        if (hostawarePartitioning != null) {
                            hazelcastRuntimeConfigurationProxy.setHostAwarePartitioning(hostawarePartitioning.toString());
                        }
                        if (hzClusterName != null) {
                            hazelcastRuntimeConfigurationProxy.setClusterGroupName(hzClusterName);
                        }
                        if (licenseKey != null) {
                            hazelcastRuntimeConfigurationProxy.setLicenseKey(licenseKey);
                        }
                        if (dasPublicAddress != null) {
                            hazelcastRuntimeConfigurationProxy.setDASPublicAddress(dasPublicAddress);
                        }
                        if (dasBindAddress != null) {
                            hazelcastRuntimeConfigurationProxy.setDASBindAddress(dasBindAddress);
                        }
                        if (dasPort != null) {
                            hazelcastRuntimeConfigurationProxy.setDasPort(dasPort);
                        }
                        if (clusterMode != null) {
                            hazelcastRuntimeConfigurationProxy.setDiscoveryMode(clusterMode);
                        }
                        if (tcpipMembers != null) {
                            hazelcastRuntimeConfigurationProxy.setTcpipMembers(tcpipMembers);
                        }
                        if (dnsMembers != null) {
                            hazelcastRuntimeConfigurationProxy.setDnsMembers(dnsMembers);
                        }
                        if (interfaces != null) {
                            hazelcastRuntimeConfigurationProxy.setInterface(interfaces);
                        }
                        if (kubernetesNamespace != null) {
                            hazelcastRuntimeConfigurationProxy.setKubernetesNamespace(kubernetesNamespace);
                        }
                        if (kubernetesServiceName != null) {
                            hazelcastRuntimeConfigurationProxy.setKubernetesServiceName(kubernetesServiceName);
                        }
                        if (autoIncrementPort != null) {
                            hazelcastRuntimeConfigurationProxy.setAutoIncrementPort(autoIncrementPort.toString());
                        }
                        if (encryptDatagrid != null) {
                            hazelcastRuntimeConfigurationProxy.setDatagridEncryptionEnabled(encryptDatagrid.toString());
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return null;
                    }
                }, hazelcastRuntimeConfiguration);

                // get the configs that need the change applied if target is domain it is all configs
                Config config = targetUtil.getConfig(target);
                List<Config> configsToApply = new ArrayList<>(5);
                if (config == null && target.equals("domain")) {
                    configsToApply.addAll(domain.getConfigs().getConfig());
                } else if (config != null) {
                    configsToApply.add(config);
                }

                for(Config configToApply : configsToApply) {
                    HazelcastConfigSpecificConfiguration nodeConfiguration = configToApply.getExtensionByType(HazelcastConfigSpecificConfiguration.class);
                    ConfigSupport.apply(new SingleConfigCode<HazelcastConfigSpecificConfiguration>() {
                        @Override
                        public Object run(final HazelcastConfigSpecificConfiguration hazelcastRuntimeConfigurationProxy) throws PropertyVetoException, TransactionFailure {
                            if (jndiName != null) {
                                hazelcastRuntimeConfigurationProxy.setJNDIName(jndiName);
                            }
                            if (enabled != null) {
                                hazelcastRuntimeConfigurationProxy.setEnabled(enabled.toString());
                            }
                            if (lite != null) {
                                hazelcastRuntimeConfigurationProxy.setLite(lite.toString());
                            }
                            if (cacheManagerJndiName != null) {
                                hazelcastRuntimeConfigurationProxy.setCacheManagerJNDIName(cacheManagerJndiName);
                            }
                            if (cachingProviderJndiName != null) {
                                hazelcastRuntimeConfigurationProxy.setCachingProviderJNDIName(cachingProviderJndiName);
                            }
                            if (executorPoolSize != null) {
                                hazelcastRuntimeConfigurationProxy.setExecutorPoolSize(executorPoolSize);
                            }
                            if (executorQueueCapacity != null) {
                                hazelcastRuntimeConfigurationProxy.setExecutorQueueCapacity(executorQueueCapacity);
                            }
                            if (scheduledExecutorPoolSize != null) {
                                hazelcastRuntimeConfigurationProxy.setScheduledExecutorPoolSize(scheduledExecutorPoolSize);
                            }
                            if (scheduledExecutorQueueCapacity != null) {
                                hazelcastRuntimeConfigurationProxy.setScheduledExecutorQueueCapacity(scheduledExecutorQueueCapacity);
                            }
                            if (memberName != null) {
                                hazelcastRuntimeConfigurationProxy.setMemberName(memberName);
                            }
                            if (memberGroup != null) {
                                hazelcastRuntimeConfigurationProxy.setMemberGroup(memberGroup);
                            }
                            if (publicAddress != null) {
                                hazelcastRuntimeConfigurationProxy.setPublicAddress(publicAddress);
                            }
                            if (configSpecificDataGridStartPort != null) {
                                if (!configToApply.isDas()) {
                                    hazelcastRuntimeConfigurationProxy.setConfigSpecificDataGridStartPort(configSpecificDataGridStartPort);
                                }
                            }
                            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                            return null;
                        }
                    }, nodeConfiguration);
                }
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                Throwable cause = ex.getCause();
                if (cause != null) {
                    actionReport.setMessage(ex.getCause().getMessage());
                } else {
                    actionReport.setMessage(ex.getMessage());
                }
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (dynamic) {
                boolean isEnabled = false;
                if (enabled != null) {
                    isEnabled = enabled;
                } else {
                    isEnabled = hazelcastCore.isEnabled();
                }
                // this command runs on all instances so they can update their configuration.
                if ("domain".equals(target)) {
                    hazelcastCore.setEnabled(isEnabled);
                } else {
                    for (Server targetServer : targetUtil.getInstances(target)    ) {
                        if (server.getInstanceName().equals(targetServer.getName())) {
                            hazelcastCore.setEnabled(isEnabled);
                        }
                    }
                }
            }

            if (encryptDatagrid != null && encryptDatagrid) {
                checkForDatagridKey(actionReport);
            }
        }
    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {

        // for all affected targets restart hazelcast. 
        // However do in turn to prevent a major data loss
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;
        inv = runner.getCommandInvocation("restart-hazelcast", subReport, context.getSubject());

        List<Server> serversAffected = targetUtil.getInstances(target);
        for (Server server : serversAffected) {
            ParameterMap params = new ParameterMap();
            params.add("target", server.getName());
            inv.parameters(params);
            inv.execute();
            // swallow the offline warning as it is not a problem
            if (subReport.hasWarnings()) {
                subReport.setMessage("");
            }
        }
    }

    private boolean validate(ActionReport actionReport) {
        boolean result = false;
        if (startPort != null) {
            try {
                int port = Integer.parseInt(startPort);
                if (port < 0 || port > Short.MAX_VALUE * 2) {
                    actionReport.failure(logger, "start port must be greater than zero or less than " + Short.MAX_VALUE * 2 + 1);
                    return result;
                }
            } catch (NumberFormatException nfe) {
                actionReport.failure(logger, "startPort is not a valid integer", nfe);
                return result;
            }
        }

        if (multicastPort != null) {
            try {
                int port = Integer.parseInt(multicastPort);
                if (port < 0 || port > Short.MAX_VALUE * 2) {
                    actionReport.failure(logger, "multicast port must be greater than zero or less than " + Short.MAX_VALUE * 2 + 1);
                    return result;
                }
            } catch (NumberFormatException nfe) {
                actionReport.failure(logger, "multicast is not a valid integer", nfe);
                return result;
            }
        }
        if ((multiCastGroup != null)) {
            InetAddress address;
            try {
                address = InetAddress.getByName(multiCastGroup);
                if (!address.isMulticastAddress()) {
                    actionReport.failure(logger, multiCastGroup + " is not a valid multicast address ");
                    return result;
                }
            } catch (UnknownHostException ex) {
                actionReport.failure(logger, multiCastGroup + " is not a valid multicast address ", ex);
                return result;
            }

        }

        return true;
    }

    @Override
    public String getTarget(ParameterMap pm) {
        String result = pm.getOne("target");
        if (result == null) {
            result = target;
        }
        return result;
    }

    private void checkForDatagridKey(ActionReport actionReport) {
        File datagridKey = new File(server.getConfigDirPath().getPath() + File.separator + "datagrid-key");
        if (!datagridKey.exists()) {
            actionReport.setActionExitCode(ActionReport.ExitCode.WARNING);
            actionReport.appendMessage("Could not find datagrid-key in domain config directory. Please ensure" +
                    " that you generate one before restarting the domain.");
        }
    }
}
