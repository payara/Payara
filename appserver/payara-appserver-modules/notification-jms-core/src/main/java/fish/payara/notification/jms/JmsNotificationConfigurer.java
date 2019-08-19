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
package fish.payara.notification.jms;

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
@Service(name = "notification-jms-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = NotificationServiceConfiguration.class,
                opType = RestEndpoint.OpType.POST,
                path = "notification-jms-configure",
                description = "Configures JMS Notification Service")
})
public class JmsNotificationConfigurer extends BaseNotificationConfigurer<JmsNotifierConfiguration, JmsNotifierService> {

    @Param(name = "contextFactoryClass")
    private String contextFactoryClass;

    @Param(name = "connectionFactoryName")
    private String connectionFactoryName;

    @Param(name = "queueName")
    private String queueName;

    @Param(name = "url")
    private String url;

    @Param(name = "username", optional = true)
    private String username;

    @Param(name = "password", optional = true)
    private String password;

    @Override
    public void execute(AdminCommandContext context) {
        super.execute(context);
    }

    @Override
    protected void applyValues(JmsNotifierConfiguration configuration) throws PropertyVetoException {
        if(this.enabled != null) {
            configuration.enabled(this.enabled);
        }
        if(this.noisy != null) {
            configuration.noisy(this.noisy);
        }
        if(StringUtils.ok(contextFactoryClass )) {
            configuration.setContextFactoryClass(contextFactoryClass);
        }
        if(StringUtils.ok(connectionFactoryName)) {
            configuration.setConnectionFactoryName(connectionFactoryName);
        }
        if(StringUtils.ok(queueName)) {
            configuration.setQueueName(queueName);
        }
        if(StringUtils.ok(url)) {
            configuration.setUrl(url);
        }
        if(StringUtils.ok(username)) {
            configuration.setUsername(username);
        }
        if(StringUtils.ok(password)) {
            configuration.setPassword(password);
        }
    }

    @Override
    protected String getHealthCheckNotifierCommandName() {
        return "healthcheck-jms-notifier-configure";
    }

    @Override
    protected String getRequestTracingNotifierCommandName() {
        return "requesttracing-jms-notifier-configure";
    }
}
