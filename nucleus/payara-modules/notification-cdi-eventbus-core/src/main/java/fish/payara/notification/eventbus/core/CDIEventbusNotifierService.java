/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.notification.eventbus.core;

import com.sun.enterprise.util.Utility;
import fish.payara.micro.cdi.Outbound;
import fish.payara.nucleus.notification.configuration.CDIEventbusNotifier;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.enterprise.inject.spi.CDI;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.logging.LogLevel;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.glassfish.internal.data.ApplicationRegistry;

/**
 * @author mertcaliskan
 */
@Service(name = "service-cdieventbus")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class CDIEventbusNotifierService extends BaseNotifierService<CDIEventbusNotificationEvent,
        CDIEventbusNotifier,
        CDIEventbusNotifierConfiguration> {

    private CDIEventbusNotifierConfigurationExecutionOptions executionOptions;
    private @Inject ApplicationRegistry appRegistry;
    private static final Logger log = Logger.getLogger(CDIEventbusNotifierService.class.getName());

    @Override
    public void handleNotification(@SubscribeTo NotificationEvent event) {
        if (event instanceof CDIEventbusNotificationEvent && executionOptions != null && executionOptions.isEnabled()) {
            CDIEventbusMessageImpl message = new CDIEventbusMessageImpl((CDIEventbusNotificationEvent) event, event.getSubject(), event.getMessage());
            for(String appName : appRegistry.getAllApplicationNames()) {
                ClassLoader oldCL = null;
                try {
                    ClassLoader appCl = appRegistry.get(appName).getAppClassLoader();
                    if(appCl != null) {
                        oldCL = Utility.setContextClassLoader(appCl);
                        CDI.current();
                        doHandleNotification((CDIEventbusNotificationEvent) event, message);
                    }
                }
                catch(IllegalStateException e) {
                    log.log(LogLevel.FINE, "CDIEventbusNotifierService.handleNotification: not a CDI application", e);
                }
                finally {
                    Utility.setContextClassLoader(oldCL);
                }
            }
        }
    }

    private void doHandleNotification(final CDIEventbusNotificationEvent event, CDIEventbusMessageImpl message) {
        CDI.current().getBeanManager().fireEvent(message, new Outbound() {
            @Override
            public String eventName() {
                return "";
            }

            @Override
            public boolean loopBack() {
                return executionOptions.getLoopBack();
            }

            @Override
            public String[] instanceName() {
                return new String[] { event.getInstanceName() };
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Outbound.class;
            }
        });
    }

    @Override
    public void bootstrap() {
        register(NotifierType.CDIEVENTBUS, CDIEventbusNotifier.class, CDIEventbusNotifierConfiguration.class);

        executionOptions = (CDIEventbusNotifierConfigurationExecutionOptions) getNotifierConfigurationExecutionOptions();
    }

    @Override
    public void shutdown() {
        reset(this);
    }
}
