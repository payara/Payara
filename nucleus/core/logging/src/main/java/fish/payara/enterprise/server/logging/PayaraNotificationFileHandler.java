/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.enterprise.server.logging;

import com.sun.enterprise.server.logging.GFFileHandler;

import java.io.File;
import java.util.logging.LogManager;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * Service class that is created and initialised by @{code fish.payara.nucleus.notification.log.LogNotifierService}
 * The lifecycle of the bean is not managed by HK2 in order to prevent notification.log file creation upon domain start.
 *
 * @author mertcaliskan
 */
public class PayaraNotificationFileHandler extends GFFileHandler {

    private static final String NOTIFICATION_FILENAME = "notification.log";

    @Override
    protected String evaluateFileName() {
        String cname = getClass().getName();
        LogManager manager = LogManager.getLogManager();

        logFileProperty = manager.getProperty(cname + ".file");
        if (logFileProperty == null || logFileProperty.trim().equals("")) {
            logFileProperty = env.getInstanceRoot().getAbsolutePath() + File.separator + LOGS_DIR + File.separator
                    + NOTIFICATION_FILENAME;
        }

        return TranslatedConfigView.expandValue(logFileProperty);
    }
}