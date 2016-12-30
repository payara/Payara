/*
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.notification.service;

import fish.payara.nucleus.notification.domain.NotifierConfigurationExecutionOptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author mertcaliskan
 */
public abstract class NotificationRunnable<MQ extends MessageQueue, EO extends NotifierConfigurationExecutionOptions>
        implements Runnable, Thread.UncaughtExceptionHandler {

    protected static final String HTTP_METHOD_POST = "POST";
    protected static final String ACCEPT_TYPE_JSON = "application/json";
    protected static final String ACCEPT_TYPE_TEXT_PLAIN = "text/plain";

    protected static final String HIPCHAT_ENDPOINT = "https://api.hipchat.com";
    protected static final String HIPCHAT_RESOURCE = "/v2/room/{0}/notification?auth_token={1}";

    protected static final String SLACK_ENDPOINT = "https://hooks.slack.com/services";
    protected static final String SLACK_RESOURCE = "/{0}/{1}/{2}";

    protected MQ queue;
    protected EO executionOptions;

    protected HttpURLConnection createConnection(URL url, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(HTTP_METHOD_POST);
        connection.setRequestProperty("Content-Type", contentType);
        connection.connect();
        return connection;
    }
}
