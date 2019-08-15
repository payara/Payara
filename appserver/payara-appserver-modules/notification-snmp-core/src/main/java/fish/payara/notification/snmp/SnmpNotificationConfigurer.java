/*
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.notification.admin.BaseNotificationConfigurer;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.beans.PropertyVetoException;

/**
 * @author mertcaliskan
 */
@Service(name = "notification-snmp-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "notification-snmp-configure",
                description = "Configures SNMP Notification Service")
})
public class SnmpNotificationConfigurer extends BaseNotificationConfigurer<SnmpNotifierConfiguration, SnmpNotifierService> {

    @Param(name = "community", defaultValue = "public", optional = true)
    private String community;

    @Param(name = "oid", defaultValue = ".1.3.6.1.2.1.1.8", optional = true)
    private String oid;

    @Param(name = "version", defaultValue = "v2c", optional = true, acceptableValues = "v1,v2c")
    private String version;

    @Param(name = "hostName")
    private String hostName;

    @Param(name = "port", defaultValue = "162", optional = true)
    private Integer port;

    @Override
    protected void applyValues(SnmpNotifierConfiguration configuration) throws PropertyVetoException {
        if(this.enabled != null) {
            configuration.enabled(this.enabled);
        }
        if(this.noisy != null) {
            configuration.noisy(this.noisy);
        }
        if(StringUtils.ok(community)) {
            configuration.setCommunity(community);
        }
        if(StringUtils.ok(oid)) {
            configuration.setOid(oid);
        }
        if(StringUtils.ok(version)) {
            configuration.setVersion(version);
        }
        if(StringUtils.ok(hostName)) {
            configuration.setHost(hostName);
        }
        if(port != null) {
            configuration.setPort(String.valueOf(port));
        }
    }

    @Override
    protected String getHealthCheckNotifierCommandName() {
        return "healthcheck-snmp-notifier-configure";
    }

    @Override
    protected String getRequestTracingNotifierCommandName() {
        return "requesttracing-snmp-notifier-configure";
    }
}