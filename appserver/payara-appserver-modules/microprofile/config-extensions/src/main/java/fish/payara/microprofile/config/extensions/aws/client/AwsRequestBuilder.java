/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.config.extensions.aws.client;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;

public class AwsRequestBuilder {

    private static final Logger LOGGER = Logger.getLogger(AwsRequestBuilder.class.getName());

    private final Client client;

    private String serviceName;
    private String region;
    private String method;
    private String action;
    private String version;
    private String contentType;
    private JsonObject data;

    public AwsRequestBuilder(String accessKey, String secretKey) {
        this.client = ClientBuilder.newBuilder()
                .register(new AwsAuthFeature(accessKey, secretKey))
                .register(new LoggingFeature(LOGGER, Level.FINE, Verbosity.PAYLOAD_ANY, 100))
                .build();
    }

    public AwsRequestBuilder serviceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    public AwsRequestBuilder region(String region) {
        this.region = region;
        return this;
    }

    public AwsRequestBuilder method(String method) {
        this.method = method;
        return this;
    }

    public AwsRequestBuilder action(String action) {
        this.action = action;
        return this;
    }

    public AwsRequestBuilder version(String version) {
        this.version = version;
        return this;
    }
    
    public AwsRequestBuilder ContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public AwsRequestBuilder data(JsonObject data) {
        this.data = data;
        return this;
    }

    public Invocation build() {
        final Date creationTime = new Date();
        final String query = MessageFormat.format("Action={0}&Version={1}", action, version);
        final String payload = data.toString();
        final String host = MessageFormat.format("{0}.{1}.amazonaws.com", serviceName.toLowerCase(), region);

        final String endpoint = "https://" + host;
        final String serviceNameValue = serviceName.equals("DynamoDB") ? serviceName + "_20120810" : serviceName;
        final String xAmzTarget = serviceNameValue + "." + action;

        final String xAmzContentSha256 = AuthUtils.generateHex(payload);

        return client.target(endpoint + "?" + query)
                .request()
                .header("X-Amz-Content-Sha256", xAmzContentSha256)
                .header("X-Amz-Date", AuthUtils.getTimestamp(creationTime))
                .header("X-Amz-Target", xAmzTarget)
                .build(method, Entity.entity(payload, contentType));
    }

    public static AwsRequestBuilder builder(String accessKey, String secretKey) {
        return new AwsRequestBuilder(accessKey, secretKey);
    }

}
