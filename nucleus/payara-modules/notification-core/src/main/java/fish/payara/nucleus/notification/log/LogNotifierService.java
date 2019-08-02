/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
package fish.payara.nucleus.notification.log;

import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Logger;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

/**
 * @author mertcaliskan
 */
@Service(name = "service-log")
@RunLevel(StartupRunLevel.VAL)
@MessageReceiver
public class LogNotifierService extends BaseNotifierService<LogNotificationEvent, LogNotifier, LogNotifierConfiguration> {

    @Inject
    private ServiceLocator habitat;

    PayaraNotificationFileHandler handler;

    private Logger logger = Logger.getLogger(LogNotifierService.class.getCanonicalName());
    LogNotifierConfigurationExecutionOptions execOptions;

    @Override
    public void bootstrap() {
        register(NotifierType.LOG, LogNotifier.class, LogNotifierConfiguration.class);

        execOptions = (LogNotifierConfigurationExecutionOptions) getNotifierConfigurationExecutionOptions();

        if (execOptions.getUseSeparateLogFile()) {
            Handler[] existingHandlers = logger.getHandlers();
            for (Handler handler : existingHandlers) {
                logger.removeHandler(handler);
            }
            logger.addHandler(getHandler());
            logger.setUseParentHandlers(false);
        }
        else {
            if (handler != null) {
                logger.removeHandler(getHandler());
            }
            logger.setUseParentHandlers(true);
        }
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = habitat.createAndInitialize(PayaraNotificationFileHandler.class);
        }
        return handler;
    }

    @Override
    public void shutdown() {
        reset(this);
    }

    @PreDestroy
    void preDestory() {
        handler = null;
    }

    @Override
    public void handleNotification(@SubscribeTo NotificationEvent event) {
        if (event instanceof LogNotificationEvent) {
            LogNotificationEvent logEvent = (LogNotificationEvent) event;
            if (execOptions != null && execOptions.isEnabled()) {
                if (logEvent.getParameters() != null && logEvent.getParameters().length > 0) {
                    String formattedText = MessageFormat.format(logEvent.getMessage(), logEvent.getParameters());
                    logger.log(logEvent.getLevel(), logEvent.getSubject() != null
                            ? logEvent.getSubject() + " - " + formattedText : formattedText);
                } else {
                    logger.log(logEvent.getLevel(), logEvent.getSubject() != null
                            ? logEvent.getSubject() + " - " + logEvent.getMessage() : logEvent.getMessage());
                }
            }
        }
    }
}