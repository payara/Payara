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
package fish.payara.nucleus.notification.log;

import static java.lang.Boolean.valueOf;
import static java.lang.String.format;

import java.util.logging.Handler;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.enterprise.server.logging.PayaraNotificationFileHandler;
import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;

/**
 * @author mertcaliskan
 */
@Service(name = "log-notifier")
@RunLevel(StartupRunLevel.VAL)
public class LogNotifier extends PayaraConfiguredNotifier<LogNotifierConfiguration> {

    @Inject
    private ServiceLocator habitat;

    private PayaraNotificationFileHandler handler;

    private Logger logger = Logger.getLogger(LogNotifier.class.getCanonicalName());

    @Override
    public void bootstrap() {
        if (Boolean.valueOf(configuration.getUseSeparateLogFile())) {
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

    @Override
    public void destroy() {
        handler = null;
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = habitat.createAndInitialize(PayaraNotificationFileHandler.class);
        }
        return handler;
    }

    @Override
    public void handleNotification(PayaraNotification event) {
        if (valueOf(configuration.getEnabled())) {
            if (event.getSubject() != null) {
                logger.info(format("%s - %s", event.getSubject(), event.getMessage()));
            } else {
                logger.info(event.getMessage());
            }
        }
    }

}