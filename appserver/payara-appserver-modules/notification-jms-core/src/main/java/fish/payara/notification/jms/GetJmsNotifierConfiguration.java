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
package fish.payara.notification.jms;

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
@Service(name = "get-jms-notifier-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "get-jms-notifier-configuration",
                description = "Lists JMS Notifier Configuration")
})
public class GetJmsNotifierConfiguration extends BaseGetNotifierConfiguration<JmsNotifierConfiguration> {


    @Override
    protected String listConfiguration(JmsNotifierConfiguration configuration) {
        String headers[] = {"Enabled", "Context Factory Class", "Connection Factory Name", "Queue Name", "URL", "Username", "Password"};
        ColumnFormatter columnFormatter = new ColumnFormatter(headers);
        Object values[] = new Object[7];

        values[0] = configuration.getEnabled();
        values[1] = configuration.getContextFactoryClass();
        values[2] = configuration.getConnectionFactoryName();
        values[3] = configuration.getQueueName();
        values[4] = configuration.getUrl();
        values[5] = configuration.getUsername();
        values[6] = configuration.getPassword();

        columnFormatter.addRow(values);
        return columnFormatter.toString();
    }

    @Override
    protected Map<String, Object> getNotifierConfiguration(JmsNotifierConfiguration configuration) {
        Map<String, Object> map = new HashMap<>(4);
        
        map.put("enabled", configuration.getEnabled());
        map.put("contextFactoryClass", configuration.getContextFactoryClass());
        map.put("connectionFactoryName", configuration.getConnectionFactoryName());
        map.put("queueName", configuration.getQueueName());
        map.put("url", configuration.getUrl());
        map.put("username", configuration.getUsername());
        map.put("password", configuration.getPassword());
        
        return map;
    }
}
