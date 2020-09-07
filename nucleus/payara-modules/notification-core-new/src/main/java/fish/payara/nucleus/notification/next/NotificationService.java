/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification.next;

import static fish.payara.nucleus.notification.next.admin.NotifierUtils.getNotifierName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.sun.enterprise.config.serverbeans.Config;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.Changed;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.NotProcessed;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transactions;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.internal.notification.PayaraNotifierConfiguration;
import fish.payara.internal.notification.admin.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.next.log.NewLogNotifierConfiguration;

/**
 * Main service class that provides {@link #notify(NotificationEvent)} method used by services, which needs disseminating notifications.
 *
 * @author mertcaliskan
 */
@Service(name = "new-notification-service")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class NotificationService implements EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private NotificationServiceConfiguration configuration;

    @Inject
    private Events events;

    @Inject
    private ServiceLocator habitat;

    @Inject
    Transactions transactions;

    @Inject
    ServerEnvironment env;

    private boolean enabled;

    @PostConstruct
    void postConstruct() {
        events.register(this);
        configuration = habitat.getService(NotificationServiceConfiguration.class);
    }

    public void event(Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapNotificationService();
        }
        transactions.addListenerForType(NotificationServiceConfiguration.class, this);
    }

    public void bootstrapNotificationService() {
        if (configuration != null) {
            this.enabled = Boolean.valueOf(configuration.getEnabled());

            if (this.enabled) {
                final List<PayaraNotifierConfiguration> notifierConfigurations = configuration.getNotifierConfigurationList();
                if (notifierConfigurations != null && notifierConfigurations.isEmpty()) {
                    try {
                        ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                            @Override
                            public Object run(final NotificationServiceConfiguration configurationProxy)
                                    throws PropertyVetoException, TransactionFailure {
                                NewLogNotifierConfiguration notifierConfiguration = configurationProxy.createChild(NewLogNotifierConfiguration.class);
                                notifierConfiguration.enabled(true);
                                configurationProxy.getNotifierConfigurationList().add(notifierConfiguration);
                                return configurationProxy;
                            }
                        }, configuration);
                    } catch (TransactionFailure e) {
                        logger.log(Level.SEVERE, "Error occurred while setting initial log notifier configuration", e);
                    }
                }
    
                logger.info("Payara Notification Service bootstrapped with configuration: " + configuration);
            }
        }
    }

    public void notify(@SubscribeTo PayaraNotification event) {
        if (enabled) {
            final List<ServiceHandle<PayaraNotifier>> notifierHandles = habitat.getAllServiceHandles(PayaraNotifier.class);

            final List<String> blacklist = event.getBlacklist();
            final List<String> whitelist = event.getWhitelist();

            for (ServiceHandle<PayaraNotifier> notifierHandle : notifierHandles) {
                final String notifierName = getNotifierName(notifierHandle.getActiveDescriptor());

                if (!blacklist.isEmpty() && blacklist.contains(notifierName)) {
                    continue;
                }
                if (!whitelist.isEmpty() && !whitelist.contains(notifierName)) {
                    continue;
                }
                notifierHandle.getService().handleNotification(event);
            }
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (env.isInstance()) {
            isCurrentInstanceMatchTarget = true;
        }
        else {
            for (PropertyChangeEvent pe : events) {
                ConfigBeanProxy proxy = (ConfigBeanProxy) pe.getSource();
                while (proxy != null && !(proxy instanceof Config)) {
                    proxy = proxy.getParent();
                }

                if (proxy != null && ((Config) proxy).isDas()) {
                    isCurrentInstanceMatchTarget = true;
                    break;
                }
            }
        }

        if (isCurrentInstanceMatchTarget) {
            return ConfigSupport.sortAndDispatch(events, new Changed() {

                @Override
                public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {

                    if(changedType.equals(NotificationServiceConfiguration.class)) {
                        configuration = (NotificationServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }
}