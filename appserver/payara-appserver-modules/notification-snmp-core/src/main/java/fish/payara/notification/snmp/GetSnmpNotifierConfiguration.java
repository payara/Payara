/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.snmp;

import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.notification.admin.BaseGetNotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * @author mertcaliskan
 */
@Service(name = "get-snmp-notifier-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "get-snmp-notifier-configuration",
                description = "Lists SNMP Notifier Configuration")
})
public class GetSnmpNotifierConfiguration extends BaseGetNotifierConfiguration<SnmpNotifierConfiguration> {

    @Override
    protected String listConfiguration(SnmpNotifierConfiguration configuration) {
        String headers[] = {"Enabled", "Community", "OID", "Version", "Host", "Port"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[6];

        values[0] = configuration.getEnabled();
        values[1] = configuration.getCommunity();
        values[2] = configuration.getOid();
        values[3] = configuration.getVersion();
        values[4] = configuration.getHost();
        values[5] = configuration.getPort();

        columnFormatter.addRow(values);
        return columnFormatter.toString();
    }

    @Override
    protected Map<String, Object> getNotifierConfiguration(SnmpNotifierConfiguration configuration) {
        Map<String, Object> map = new HashMap<>(6);
        
        map.put("enabled", configuration.getEnabled());
        map.put("community", configuration.getCommunity());
        map.put("oid", configuration.getOid());
        map.put("version", configuration.getVersion());
        map.put("hostName", configuration.getHost());
        map.put("port", configuration.getPort());
        
        return map;
    }
}
