/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2014-2022] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.hazelcast.HazelcastConfigSpecificConfiguration;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jakarta.inject.Inject;
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
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

/**
 * Asadmin command to get the current configuration of Hazelcast
 * @since 4.1.151
 */
@Service(name = "get-hazelcast-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.hazelcast.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-hazelcast-configuration",
            description = "List Hazelcast Configuration")
})
public class GetHazelcastConfiguration implements AdminCommand {
    @Inject
    private Domain domain;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "checkencrypted", optional = true)
    private boolean checkEncrypted;

    @Override
    public void execute(AdminCommandContext context) {

        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        HazelcastConfigSpecificConfiguration nodeConfiguration = config.getExtensionByType(HazelcastConfigSpecificConfiguration.class);
        HazelcastRuntimeConfiguration runtimeConfiguration = domain.getExtensionByType(HazelcastRuntimeConfiguration.class);

        final ActionReport actionReport = context.getActionReport();

        if (checkEncrypted) {
            if (!Boolean.parseBoolean(runtimeConfiguration.getDatagridEncryptionEnabled())) {
                actionReport.setMessage("Data Grid Not Encrypted");
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            } else {
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
            return;
        }
        String headers[] = {"Property Name","PropertyValue","Scope"};

        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        columnFormatter.addRow(new Object[]{"Configuration File",runtimeConfiguration.getHazelcastConfigurationFile(),"Domain"});
        columnFormatter.addRow(new Object[]{"Interfaces",runtimeConfiguration.getInterface(),"Domain"});
        columnFormatter.addRow(new Object[]{"Auto Increment Port", runtimeConfiguration.getAutoIncrementPort(), "Domain"});
        columnFormatter.addRow(new Object[]{"Start Port",runtimeConfiguration.getStartPort(),"Domain"});
        columnFormatter.addRow(new Object[]{"Cluster Name",runtimeConfiguration.getClusterGroupName(),"Domain"});
        columnFormatter.addRow(new Object[]{"License Key",runtimeConfiguration.getLicenseKey(),"Domain"});
        columnFormatter.addRow(new Object[]{"Host Aware Partitioning",runtimeConfiguration.getHostAwarePartitioning(),"Domain"});
        columnFormatter.addRow(new Object[]{"DAS Public Address",runtimeConfiguration.getDASPublicAddress(),"Domain"});
        columnFormatter.addRow(new Object[]{"DAS Bind Address",runtimeConfiguration.getDASBindAddress(),"Domain"});
        columnFormatter.addRow(new Object[]{"DAS Port",runtimeConfiguration.getDasPort(),"Domain"});
        columnFormatter.addRow(new Object[]{"Cluster Mode",runtimeConfiguration.getDiscoveryMode(),"Domain"});
        columnFormatter.addRow(new Object[]{"Tcpip Members",runtimeConfiguration.getTcpipMembers(),"Domain"});
        columnFormatter.addRow(new Object[]{"DNS Members",runtimeConfiguration.getDnsMembers(),"Domain"});
        columnFormatter.addRow(new Object[]{"MulticastGroup",runtimeConfiguration.getMulticastGroup(),"Domain"});
        columnFormatter.addRow(new Object[]{"MulticastPort",runtimeConfiguration.getMulticastPort(),"Domain"});
        columnFormatter.addRow(new Object[]{"Kubernetes Namespace",runtimeConfiguration.getKubernetesNamespace(),"Domain"});
        columnFormatter.addRow(new Object[]{"Kubernetes Service Name",runtimeConfiguration.getKubernetesServiceName(),"Domain"});
        columnFormatter.addRow(new Object[]{"Encrypt Datagrid", runtimeConfiguration.getDatagridEncryptionEnabled(), "Domain"});
        columnFormatter.addRow(new Object[]{"Enabled",nodeConfiguration.getEnabled(),"Config"});
        columnFormatter.addRow(new Object[]{"JNDIName",nodeConfiguration.getJNDIName(),"Config"});
        columnFormatter.addRow(new Object[]{"Cache Manager JNDI Name",nodeConfiguration.getCacheManagerJNDIName(),"Config"});
        columnFormatter.addRow(new Object[]{"Caching Provider JNDI Name",nodeConfiguration.getCachingProviderJNDIName(),"Config"});
        columnFormatter.addRow(new Object[]{"Lite Member",nodeConfiguration.getLite(),"Config"});
        columnFormatter.addRow(new Object[]{"Member Name",nodeConfiguration.getMemberName(),"Config"});
        columnFormatter.addRow(new Object[]{"Member Group",nodeConfiguration.getMemberGroup(),"Config"});
        columnFormatter.addRow(new Object[]{"Executor Pool Size",nodeConfiguration.getExecutorPoolSize(),"Config"});
        columnFormatter.addRow(new Object[]{"Executor Queue Capacity",nodeConfiguration.getExecutorQueueCapacity(),"Config"});
        columnFormatter.addRow(new Object[]{"Scheduled Executor Pool Size",nodeConfiguration.getScheduledExecutorPoolSize(),"Config"});
        columnFormatter.addRow(new Object[]{"Scheduled Executor Queue Capacity",nodeConfiguration.getScheduledExecutorQueueCapacity(),"Config"});
        columnFormatter.addRow(new Object[]{"Public Address",nodeConfiguration.getPublicAddress(),"Config"});
        columnFormatter.addRow(new Object[]{"Config Specific Data Grid Start Port",nodeConfiguration.getConfigSpecificDataGridStartPort(),"Config"});

        Map<String, Object> map = new HashMap<>(26);
        Properties extraProps = new Properties();
        map.put("hazelcastConfigurationFile", runtimeConfiguration.getHazelcastConfigurationFile());
        map.put("enabled", nodeConfiguration.getEnabled());
        map.put("autoIncrementPort", runtimeConfiguration.getAutoIncrementPort());
        map.put("startPort", runtimeConfiguration.getStartPort());
        map.put("multicastGroup", runtimeConfiguration.getMulticastGroup());
        map.put("multicastPort", runtimeConfiguration.getMulticastPort());
        map.put("jndiName", nodeConfiguration.getJNDIName());
        map.put("lite", nodeConfiguration.getLite());
        map.put("clusterName", runtimeConfiguration.getClusterGroupName());
        map.put("licenseKey", runtimeConfiguration.getLicenseKey());
        map.put("hostAwarePartitioning", runtimeConfiguration.getHostAwarePartitioning());
        map.put("dasPublicAddress", runtimeConfiguration.getDASPublicAddress());
        map.put("dasBindAddress", runtimeConfiguration.getDASBindAddress());
        map.put("dasPort", runtimeConfiguration.getDasPort());
        map.put("tcpipMembers", runtimeConfiguration.getTcpipMembers());
        map.put("dnsMembers", runtimeConfiguration.getDnsMembers());
        map.put("clusterMode", runtimeConfiguration.getDiscoveryMode());
        map.put("memberName", nodeConfiguration.getMemberName());
        map.put("memberGroup", nodeConfiguration.getMemberGroup());
        map.put("interfaces", runtimeConfiguration.getInterface());
        map.put("cacheManagerJndiName", nodeConfiguration.getCacheManagerJNDIName());
        map.put("cachingProviderJndiName", nodeConfiguration.getCachingProviderJNDIName());
        map.put("executorPoolSize", nodeConfiguration.getExecutorPoolSize());
        map.put("executorQueueCapacity", nodeConfiguration.getExecutorQueueCapacity());
        map.put("scheduledExecutorPoolSize", nodeConfiguration.getScheduledExecutorPoolSize());
        map.put("scheduledExecutorQueueCapacity", nodeConfiguration.getScheduledExecutorQueueCapacity());
        map.put("publicAddress", nodeConfiguration.getPublicAddress());
        map.put("kubernetesNamespace", runtimeConfiguration.getKubernetesNamespace());
        map.put("kubernetesServiceName", runtimeConfiguration.getKubernetesServiceName());
        map.put("configSpecificDataGridStartPort",nodeConfiguration.getConfigSpecificDataGridStartPort());
        map.put("encryptDatagrid", runtimeConfiguration.getDatagridEncryptionEnabled());

        extraProps.put("getHazelcastConfiguration",map);

        actionReport.setExtraProperties(extraProps);

        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
