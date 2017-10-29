/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.hazelcast.HazelcastRuntimeConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
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

    @Override
    public void execute(AdminCommandContext context) {
       
        HazelcastRuntimeConfiguration runtimeConfiguration = domain.getExtensionByType(HazelcastRuntimeConfiguration.class);
        final ActionReport actionReport = context.getActionReport();
        String headers[] = {"Configuration File","Enabled","Start Port","MulticastGroup","MulticastPort","JNDIName","Lite Member",
                            "Cluster Name","Cluster Password", "License Key", "Host Aware Paritioning","Das Host","Das Port","Tcpip Members",
                            "Cluster Mode", "Member Name", "Member Group", "Interfaces", "Cache Manager JNDI Name", "Caching Provider JNDI Name",
                            "Executor Pool Size", "Executor Queue Capacity", "Scheduled Executor Pool Size", "Scheduled Executor Queue Capacity"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[24];
        values[0] = runtimeConfiguration.getHazelcastConfigurationFile();
        values[1] = runtimeConfiguration.getEnabled();
        values[2] = runtimeConfiguration.getStartPort();
        values[3] = runtimeConfiguration.getMulticastGroup();
        values[4] = runtimeConfiguration.getMulticastPort();
        values[5] = runtimeConfiguration.getJNDIName();
        values[6] = runtimeConfiguration.getLite();
        values[7] = runtimeConfiguration.getClusterGroupName();
        values[8] = runtimeConfiguration.getClusterGroupPassword();
        values[9] = runtimeConfiguration.getLicenseKey();
        values[10] = runtimeConfiguration.getHostAwarePartitioning();
        values[11] = runtimeConfiguration.getDasHost();
        values[12] = runtimeConfiguration.getDasPort();
        values[13] = runtimeConfiguration.getTcpipMembers();
        values[14] = runtimeConfiguration.getDiscoveryMode();
        values[15] = runtimeConfiguration.getMemberName();
        values[16] = runtimeConfiguration.getMemberGroup();
        values[17] = runtimeConfiguration.getInterface();
        values[18] = runtimeConfiguration.getCacheManagerJNDIName();
        values[19] = runtimeConfiguration.getCachingProviderJNDIName();
        values[20] = runtimeConfiguration.getExecutorPoolSize();
        values[21] = runtimeConfiguration.getExecutorQueueCapacity();
        values[22] = runtimeConfiguration.getScheduledExecutorPoolSize();
        values[23] = runtimeConfiguration.getScheduledExecutorQueueCapacity();
        
        columnFormatter.addRow(values);
        
        Map<String, Object> map = new HashMap<>(24);
        Properties extraProps = new Properties();
        map.put("hazelcastConfigurationFile", values[0]);
        map.put("enabled", values[1]);
        map.put("startPort", values[2]);
        map.put("multicastGroup", values[3]);
        map.put("multicastPort", values[4]);
        map.put("jndiName", values[5]);
        map.put("lite", values[6]);
        map.put("clusterName", values[7]);
        map.put("clusterPassword", values[8]);
        map.put("licenseKey", values[9]);
        map.put("hostAwareParitioning", values[10]);
        map.put("dasHost", values[11]);
        map.put("dasPort", values[12]);
        map.put("tcpipMembers", values[13]);
        map.put("clusterMode", values[14]);
        map.put("memberName", values[15]);
        map.put("memberGroup", values[16]);
        map.put("interfaces", values[17]);
        map.put("cacheManagerJndiName", values[18]);
        map.put("cachingProviderJndiName", values[19]);
        map.put("executorPoolSize", values[20]);
        map.put("executorQueueCapacity", values[21]);
        map.put("scheduledExecutorPoolSize", values[22]);
        map.put("scheduledExecutorQueueCapacity", values[23]);

        extraProps.put("getHazelcastConfiguration",map);
                
        actionReport.setExtraProperties(extraProps);
        
        actionReport.setMessage(columnFormatter.toString());
        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
