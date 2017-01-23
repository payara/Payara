/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014-2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import java.beans.PropertyVetoException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
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
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-hazelcast-configuration",
            description = "Set Hazelcast Configuration")
})
public class SetHazelcastConfiguration implements AdminCommand {

    @Inject
    protected Logger logger;

    @Inject
    protected HazelcastCore hazelcast;

    @Inject
    protected Target targetUtil;
     
    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;
    
    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "hazelcastConfigurationFile", shortName = "f", optional = true)
    private String configFile;

    @Param(name = "startPort", optional = true)
    private String startPort;

    @Param(name = "multicastGroup", shortName = "g", optional = true)
    private String multiCastGroup;

    @Param(name = "multicastPort", optional = true)
    private String multicastPort;
        
    @Param(name = "clusterName", optional = true)
    private String hzClusterName;

    @Param(name = "clusterPassword", optional = true)
    private String hzClusterPassword;    

    @Param(name = "jndiName", shortName = "j", optional = true)
    private String jndiName;
    
    @Param(name = "licenseKey", shortName = "lk", optional = true)
    private String licenseKey;
    
    @Param(name = "lite", optional = true, defaultValue = "false")
    private Boolean lite;

    @Param(name = "hostawareParitioning", optional = true, defaultValue = "false")
    private Boolean hostawarePartitioning;    
    
    @Inject
    ServiceLocator serviceLocator;

    @Override
    public void execute(AdminCommandContext context) {

        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (!validate(actionReport)) {
            return;
        }

        Config config = targetUtil.getConfig(target);
        HazelcastRuntimeConfiguration hazelcastRuntimeConfiguration = config.getExtensionByType(HazelcastRuntimeConfiguration.class);
        if (hazelcastRuntimeConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<HazelcastRuntimeConfiguration>() {
                    @Override
                    public Object run(final HazelcastRuntimeConfiguration hazelcastRuntimeConfigurationProxy) throws PropertyVetoException, TransactionFailure {
                        if (startPort != null) {
                            hazelcastRuntimeConfigurationProxy.setStartPort(startPort);
                        }
                        if (multiCastGroup != null) {
                            hazelcastRuntimeConfigurationProxy.setMulticastGroup(multiCastGroup);
                        }
                        if (multicastPort != null) {
                            hazelcastRuntimeConfigurationProxy.setMulticastPort(multicastPort);
                        }
                        if (jndiName != null) {
                            hazelcastRuntimeConfigurationProxy.setJNDIName(jndiName);
                        }
                        if (enabled != null) {
                            hazelcastRuntimeConfigurationProxy.setEnabled(enabled.toString());
                        }
                        if (configFile != null) {
                            hazelcastRuntimeConfigurationProxy.setHazelcastConfigurationFile(configFile);
                        }
                        if (lite != null) {
                            hazelcastRuntimeConfigurationProxy.setLite(lite.toString());
                        }
                        
                        if (hostawarePartitioning != null) {
                            hazelcastRuntimeConfigurationProxy.setHostAwarePartitioning(hostawarePartitioning.toString());
                        }
                        
                        if (hzClusterName != null) {
                            hazelcastRuntimeConfigurationProxy.setClusterGroupName(hzClusterName);
                        }
                        if (hzClusterPassword != null) {
                            hazelcastRuntimeConfigurationProxy.setClusterGroupPassword(hzClusterPassword);
                        }
                        if (licenseKey != null){
                            hazelcastRuntimeConfigurationProxy.setLicenseKey(licenseKey);
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return null;
                    }

                }, hazelcastRuntimeConfiguration);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            if (dynamic) {
                enableOnTarget(actionReport, theContext, enabled);
            }

        }

    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv;
           
        if (target.equals("server-config")) {
            inv = runner.getCommandInvocation("__enable-hazelcast-internal-on-das", subReport, context.getSubject());
        } else {
            inv = runner.getCommandInvocation("__enable-hazelcast-internal-on-instance", subReport, context.getSubject());
        }

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }

    }

    private boolean validate(ActionReport actionReport) {
        boolean result = false;
        if (startPort != null) {
            try {
                int port = Integer.parseInt(startPort);
                if (port < 0 || port > Short.MAX_VALUE * 2) {
                    actionReport.failure(logger, "start port must be greater than zero or less than " + Short.MAX_VALUE*2+1);
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
                    actionReport.failure(logger, "multicast port must be greater than zero or less than " + Short.MAX_VALUE*2+1);
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
                actionReport.failure(logger, multiCastGroup+" is not a valid multicast address ", ex);
                return result;
            }

        }

        return true;
    }
}
