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
package fish.payara.notification.hipchat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.glassfish.api.StartupRunLevel;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import fish.payara.internal.notification.PayaraConfiguredNotifier;
import fish.payara.internal.notification.PayaraNotification;

/**
 * @author mertcaliskan
 */
@Service(name = "hipchat-notifier")
@RunLevel(StartupRunLevel.VAL)
public class HipchatNotifier extends PayaraConfiguredNotifier<HipchatNotifierConfiguration> {

    private static final Logger LOGGER = Logger.getLogger(HipchatNotifier.class.getName());

    private static final String HIPCHAT_ENDPOINT = "https://api.hipchat.com";
    private static final String HIPCHAT_RESOURCE = "/v2/room/{0}/notification?auth_token={1}";

    private URL url;

    @Override
    public void handleNotification(PayaraNotification event) {
        if (url == null) {
            LOGGER.fine("Hipchat notifier received notification, but no URL was available.");
            return;
        }

        final String message = event.getMessage();
        final String subject = String.format("%s. (host: %s, server: %s, domain: %s, instance: %s)",
                event.getSubject(),
                event.getHostName(),
                event.getServerName(),
                event.getDomainName(),
                event.getInstanceName());

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod(HttpMethod.POST);
            connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            connection.connect();

            try (OutputStream outputStream = connection.getOutputStream()) {

                final String text = subject + "\n" + message;

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(text);
                }

                outputStream.write(text.getBytes(Charsets.UTF8_CHARSET));

                if (connection.getResponseCode() != 204) {
                    LOGGER.log(Level.SEVERE,
                            "Error occurred while connecting Hipchat. Check your room name and token. HTTP response code: "
                                    + connection.getResponseCode());
                } else {
                    LOGGER.log(Level.FINE, "Message successfully sent");
                }
            }
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while accessing URL: " + url.toString(), e);
        } catch (ProtocolException e) {
            LOGGER.log(Level.SEVERE, "Specified URL is not accepting protocol defined: " + HttpMethod.POST, e);
        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, "Check your network connection. Cannot access URL: " + url.toString(), e);
        } catch (ConnectException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while connecting URL: " + url.toString(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO Error while accessing URL: " + url.toString(), e);
        }
    }

    @Override
    public void bootstrap() {
        String formattedURL = MessageFormat.format(HIPCHAT_RESOURCE, configuration.getRoomName(), configuration.getToken());
        String fullURL = HIPCHAT_ENDPOINT + formattedURL;
        try {
            this.url = new URL(fullURL);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, "Error occurred while accessing URL: " + fullURL, e);
        }
    }

    @Override
    public void destroy() {
        this.url = null;
    }

}