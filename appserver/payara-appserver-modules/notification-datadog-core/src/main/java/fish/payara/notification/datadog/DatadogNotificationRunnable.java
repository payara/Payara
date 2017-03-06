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
package fish.payara.notification.datadog;

import com.fasterxml.jackson.databind.ObjectMapper;
import fish.payara.nucleus.notification.service.NotificationRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
public class DatadogNotificationRunnable extends NotificationRunnable<DatadogMessageQueue, DatadogNotifierConfigurationExecutionOptions> {

    private static Logger logger = Logger.getLogger(DatadogNotificationRunnable.class.getCanonicalName());

    public DatadogNotificationRunnable(DatadogMessageQueue queue, DatadogNotifierConfigurationExecutionOptions executionOptions) {
        this.queue = queue;
        this.executionOptions = executionOptions;
    }

    @Override
    public void run() {
        while (queue.size() > 0) {
            String formattedURL = MessageFormat.format(DATADOG_RESOURCE, executionOptions.getKey());
            String fullURL = DATADOG_ENDPOINT + formattedURL;
            try {
                URL url = new URL(fullURL);
                HttpURLConnection connection = createConnection(url,
                        new NotificationRunnable.Header(HEADER_CONTENTTYPE, ACCEPT_TYPE_JSON));

                DatadogMessage message = queue.getMessage();
                try(OutputStream outputStream = connection.getOutputStream()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.writeValue(outputStream, message);

                    if (connection.getResponseCode() < 200 && connection.getResponseCode() >= 300) {
                        logger.log(Level.SEVERE, "Error occurred while connecting Datadog. " +
                                "Check your parameters. HTTP response code: " + connection.getResponseCode());
                    }
                }
            }
            catch (MalformedURLException e) {
                logger.log(Level.SEVERE, "Error occurred while accessing URL: " + fullURL, e);
            }
            catch (ProtocolException e) {
                logger.log(Level.SEVERE, "Specified URL is not accepting protocol defined: " + HTTP_METHOD_POST, e);
            }
            catch (UnknownHostException e) {
                logger.log(Level.SEVERE, "Check your network connection. Cannot access URL: " + fullURL, e);
            }
            catch (ConnectException e) {
                logger.log(Level.SEVERE, "Error occurred while connecting URL: " + fullURL, e);
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "IO Error while accessing URL: " + fullURL, e);
            }
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        logger.log(Level.SEVERE, "Error occurred consuming datadog messages from queue", e);
    }
}
