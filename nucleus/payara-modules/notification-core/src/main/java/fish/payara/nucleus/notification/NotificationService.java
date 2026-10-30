/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;

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
import org.glassfish.hk2.extras.ExtrasUtilities;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.Globals;
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

import fish.payara.internal.notification.NotifierManager;
import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;
import fish.payara.internal.notification.PayaraNotifier;
import fish.payara.internal.notification.PayaraNotifierConfiguration;
import fish.payara.internal.notification.admin.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;

/**
 * Main service class that received {@link #notify(PayaraNotification)} HK2
 * events, and distributes them to notifiers discovered by the service.
 *
 * @author mertcaliskan
 * @author Matthew Gill
 */
@Service(name = "notification-service")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class NotificationService implements NotifierManager, EventListener, ConfigListener {

    private static final Logger logger = Logger.getLogger(NotificationService.class.getName());

    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    private NotificationServiceConfiguration configuration;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Events events;

    @Inject
    private Transactions transactions;

    private final Set<NotifierHandler> notifiers;

    private boolean isInstance;

    private boolean enabled;

    public NotificationService() {
        this.notifiers = new HashSet<>();
        // done in constructor to ensure that topics are valid for injection
        ExtrasUtilities.enableTopicDistribution(Globals.getDefaultHabitat());
    }

    @PostConstruct
    void initialize() {
        // Get the config if it's not been injected
        if (configuration == null) {
            configuration = serviceLocator.getService(NotificationServiceConfiguration.class);
        }

        // Register an event listener
        if (events != null) {
            events.register(this);
        }

        // Is this service running on an instance?
        final ServerEnvironment env = serviceLocator.getService(ServerEnvironment.class);
        isInstance = env.isInstance();

        // Find and register all notifier services
        final List<ServiceHandle<PayaraNotifier>> notifierHandles = serviceLocator.getAllServiceHandles(PayaraNotifier.class);
        for (ServiceHandle<PayaraNotifier> handle : notifierHandles) {
            NotifierHandler handler;
            final boolean isNotifierConfigurable = handle
                    .getActiveDescriptor()
                    .getAdvertisedContracts()
                    .contains(PayaraConfiguredNotifier.class.getName());
            if (isNotifierConfigurable) {
                PayaraNotifierConfiguration notifierConfig = getOrCreateNotifierConfiguration(handle);
                handler = new NotifierHandler(handle, notifierConfig);
            } else {
                handler = new NotifierHandler(handle);
            }
            notifiers.add(handler);
        }
    }

    @PreDestroy
    void destroy() {
        notifiers.clear();

        if (events != null) {
            events.unregister(this);
        }
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(EventTypes.SERVER_READY)) {
            bootstrapNotificationService();
        }
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            shutdownNotificationService();
        }
        transactions.addListenerForType(NotificationServiceConfiguration.class, this);
    }

    public void shutdownNotificationService() {
        notifiers.forEach(NotifierHandler::destroy);
    }

    public void bootstrapNotificationService() {
        if (configuration != null) {
            final boolean wasEnabled = this.enabled;
            this.enabled = Boolean.valueOf(configuration.getEnabled());

            if (this.enabled) {

                // Configure the log notifier by default
                final List<PayaraNotifierConfiguration> notifierConfigurations = configuration.getNotifierConfigurationList();
                if (notifierConfigurations != null && notifierConfigurations.isEmpty()) {
                    try {
                        ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                            @Override
                            public Object run(final NotificationServiceConfiguration configurationProxy)
                                    throws PropertyVetoException, TransactionFailure {
                                LogNotifierConfiguration notifierConfiguration = configurationProxy.createChild(LogNotifierConfiguration.class);
                                notifierConfiguration.enabled(true);
                                configurationProxy.getNotifierConfigurationList().add(notifierConfiguration);
                                return configurationProxy;
                            }
                        }, configuration);
                    } catch (TransactionFailure e) {
                        logger.log(Level.SEVERE, "Error occurred while setting initial log notifier configuration", e);
                    }
                }

                // Bootstrap each notifier
                notifiers.forEach(NotifierHandler::bootstrap);
    
                logger.info("Payara Notification Service bootstrapped.");
            } else if (wasEnabled) {
                shutdownNotificationService();
            }
        }
    }

    public void notify(@SubscribeTo PayaraNotification event) {
        if (enabled) {
            final List<String> blacklist = event.getNotifierBlacklist();
            final List<String> whitelist = event.getNotifierWhitelist();

            for (NotifierHandler handler : notifiers) {
                final String notifierName = handler.getName();

                if (blacklist != null && blacklist.contains(notifierName)) {
                    continue;
                }
                if (whitelist != null && !whitelist.contains(notifierName)) {
                    continue;
                }

                handler.accept(event);
            }
        }
    }

    @Override
    public void reconfigureNotifier(PayaraNotifierConfiguration configuration) {
        if (!enabled || configuration == null) {
            return;
        }
        for (NotifierHandler handler : notifiers) {
            final PayaraNotifierConfiguration notifierConfig = handler.getConfig();
            if (notifierConfig != null && notifierConfig.getClass().isAssignableFrom(configuration.getClass())) {
                handler.reconfigure();
                return;
            }
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        boolean isCurrentInstanceMatchTarget = false;
        if (isInstance) {
            isCurrentInstanceMatchTarget = true;
        } else {
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

                    if (changedType.equals(NotificationServiceConfiguration.class)) {
                        configuration = (NotificationServiceConfiguration) changedInstance;
                    }
                    return null;
                }
            }, logger);
        }
        return null;
    }

    private PayaraNotifierConfiguration getOrCreateNotifierConfiguration(ServiceHandle<?> handle) {
        final Class<PayaraNotifierConfiguration> configClass = NotifierUtils.getConfigurationClass(handle.getActiveDescriptor().getImplementationClass());
        if (configuration.getNotifierConfigurationByType(configClass) == null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<NotificationServiceConfiguration>() {
                    @Override
                    public Object run(final NotificationServiceConfiguration configurationProxy)
                            throws PropertyVetoException, TransactionFailure {
                        final PayaraNotifierConfiguration config = configurationProxy.createChild(configClass);
                        configurationProxy.getNotifierConfigurationList().add(config);
                        return config;
                    }
                }, configuration);
            } catch (TransactionFailure e) {
                logger.log(Level.SEVERE, "Error occurred while setting initial notifier configuration", e);
            }
        }
        return configuration.getNotifierConfigurationByType(configClass);
    }
}