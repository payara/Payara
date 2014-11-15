/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.hazelcast.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import java.beans.PropertyVetoException;
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
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
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
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
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

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Inject
    protected Target targetUtil;
    
    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "hazelcastConfigurationFile", shortName ="f", optional = true)
    private String configFile;

    @Param(name = "startPort", shortName = "p", optional = true)
    private String startPort;

    @Param(name = "multicastGroup", shortName = "g", optional = true)
    private String multiCastGroup;

    @Param(name = "multicastPort", shortName = "mp", optional = true)
    private String multicastPort;

    @Param(name = "jndiName", shortName = "j", optional = true)
    private String jndiName;

    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
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
                            hazelcastRuntimeConfigurationProxy.setEnabled(enabled);
                        }
                        if (configFile != null) {
                            hazelcastRuntimeConfigurationProxy.setHazelcastConfigurationFile(configFile);
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

        }

    }
}
