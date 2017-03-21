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
import fish.payara.nucleus.notification.service.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mertcaliskan
 */
public class Main {

    protected static final String HTTP_METHOD_POST = "POST";
    protected static final String ACCEPT_TYPE_JSON = "application/json";

    static final String ENDPOINT = "https://app.datadoghq.com/api/v1/events?api_key=0c132491b27df330b0bc80595fd9624c";

    public static void main(String[] args) throws IOException {
        URL url = new URL(ENDPOINT);
        HttpURLConnection connection = createConnection(url, ACCEPT_TYPE_JSON);
        DatadogMessage message = new DatadogMessage("title", "message");
        try(OutputStream outputStream = connection.getOutputStream()) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(outputStream, message);

            if (connection.getResponseCode() != 200) {
                System.out.println("Error occurred while connecting Datadog. " + "Check your tokens. HTTP response code: " + connection.getResponseCode());
            }
        }
    }

    private static HttpURLConnection createConnection(URL url, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(HTTP_METHOD_POST);
        connection.setRequestProperty("Content-Type", contentType);
        connection.connect();
        return connection;
    }

    public static class DatadogMessage extends Message {

        private String title;
        private String text;
        private String priority = "Normal";
        private List<String> tags;

        public DatadogMessage(String subject, String message) {
            this.title = subject;
            this.subject = message;
            this.text = message;
            this.tags = Arrays.asList("HealthCheck");
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }

        public String getPriority() {
            return priority;
        }

        public List<String> getTags() {
            return tags;
        }
    }
}
