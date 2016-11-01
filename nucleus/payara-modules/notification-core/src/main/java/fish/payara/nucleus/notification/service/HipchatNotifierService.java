/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.notification.service;

import com.google.common.eventbus.Subscribe;
import fish.payara.nucleus.notification.configuration.HipchatNotifier;
import fish.payara.nucleus.notification.configuration.HipchatNotifierConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.HipchatNotificationEvent;
import fish.payara.nucleus.notification.domain.execoptions.HipchatNotifierConfigurationExecutionOptions;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author mertcaliskan
 */
@Service(name = "service-hipchat")
@RunLevel(StartupRunLevel.VAL)
public class HipchatNotifierService extends BaseNotifierService<HipchatNotificationEvent, HipchatNotifier, HipchatNotifierConfiguration> {

    private Logger logger = Logger.getLogger(HipchatNotifierService.class.getCanonicalName());

    private static final String HTTP_PROTOCOL = "POST";
    private static final String ACCEPT_TYPE = "text/plain";
    private static final String ENDPOINT = "https://api.hipchat.com";
    private static final String RESOURCE = "/v2/room/{0}/notification?auth_token={1}";

    @PostConstruct
    void postConstruct() {
        register(NotifierType.HIPCHAT, HipchatNotifier.class, HipchatNotifierConfiguration.class, this);
    }

    @Override
    @Subscribe
    public void handleNotification(HipchatNotificationEvent event) {

        HipchatNotifierConfigurationExecutionOptions executionOptions =
                (HipchatNotifierConfigurationExecutionOptions) getNotifierConfigurationExecutionOptions();
        String urlStr = MessageFormat.format(RESOURCE, executionOptions.getRoomName(), executionOptions.getToken());
        try {
            URL url = new URL(concatenateEndpoint(urlStr));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod(HTTP_PROTOCOL);
            conn.setRequestProperty("Content-Type", ACCEPT_TYPE);
            conn.connect();
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write((event.getUserMessage() + " - " + event.getMessage()).getBytes());
            outputStream.flush();
            outputStream.close();
            if (conn.getResponseCode() != 204) {
                logger.log(Level.SEVERE,
                        "Error occurred while connecting HipChat. Check your room name and token. http response code",
                        conn.getResponseCode());
            }
        }
        catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Error occurred while accessing URL: " + concatenateEndpoint(urlStr), e);
        }
        catch (ProtocolException e) {
            logger.log(Level.SEVERE, "Specified URL is not accepting protocol defined: " + HTTP_PROTOCOL, e);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "IO Error while accessing URL: " + concatenateEndpoint(urlStr), e);
        }
    }

    private String concatenateEndpoint(String urlStr) {
        return ENDPOINT + urlStr;
    }
}