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

import static fish.payara.microprofile.config.extensions.aws.client.AuthUtils.HmacSHA256;
import static fish.payara.microprofile.config.extensions.aws.client.AuthUtils.bytesToHex;
import static fish.payara.microprofile.config.extensions.aws.client.AuthUtils.generateHex;
import static fish.payara.microprofile.config.extensions.aws.client.AuthUtils.getSignatureKey;
import static java.text.MessageFormat.format;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AwsAuthFeature implements ClientRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(AwsAuthFeature.class.getName());

    private final String accessKey;
    private final String secretKey;

    public AwsAuthFeature(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public synchronized void filter(ClientRequestContext requestContext) throws IOException {
        MultivaluedMap<String, String> headers = requestContext.getStringHeaders();

        Context filterContext = new Context();
        filterContext.method = requestContext.getMethod();
        filterContext.query = requestContext.getUri().getQuery();
        filterContext.timestamp = headers.getFirst("X-Amz-Date");
        filterContext.date = filterContext.timestamp.split("T")[0];
        filterContext.host = requestContext.getUri().getHost();
        filterContext.region = filterContext.host.split("\\.")[1];
        filterContext.serviceName = filterContext.host.split("\\.")[0];
        filterContext.payload = requestContext.getEntity().toString().trim();
        filterContext.xAmzContentSha256 = requestContext.getHeaderString("X-Amz-Content-Sha256");
        filterContext.xAmzTarget = requestContext.getHeaderString("X-Amz-Target");
        filterContext.contentType = requestContext.getHeaderString("Content-Type");

        try {
            requestContext.getHeaders().putSingle("Authorization", generateAwsAuthSignature(filterContext));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to create Authorization header", ex);
        }
    }

    /**
     * Build string for Authorization header.
     *
     * @return
     */
    private String generateAwsAuthSignature(Context filterContext) throws Exception {
        String stringToSign = "";

        /* Step 2.1 Start with the algorithm designation, followed by a newline character. */
        stringToSign = "AWS4-HMAC-SHA256\n";

        /* Step 2.2 Append the request date value, followed by a newline character. */
        stringToSign += filterContext.timestamp + "\n";

        /* Step 2.3 Append the credential scope value, followed by a newline character. */
        stringToSign += filterContext.date + "/" + filterContext.region + "/" + filterContext.serviceName + "/aws4_request\n";

        /* Step 2.4 Append the hash of the canonical request that you created in Task 1: Create a Canonical Request for Signature Version 4. */
        stringToSign += generateHex(prepareCanonicalUrl(filterContext));

        if (LOGGER.isLoggable(Level.FINER)) {
            System.out.println("String to sign: " + stringToSign);
        }

        /* Step 3.1 Derive your signing key */
        byte[] signatureKey = getSignatureKey(secretKey, filterContext.date, filterContext.region, filterContext.serviceName);

        /* Step 3.2 Calculate the signature. */
        byte[] signature = HmacSHA256(signatureKey, stringToSign);

        /* Step 3.2.1 Encode signature (byte[]) to Hex */
        String strHexSignature = bytesToHex(signature);

        return format(
"AWS4-HMAC-SHA256 Credential={0}/{1}/{2}/{3}/aws4_request, SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date;x-amz-target, Signature={4}",
                accessKey,
                filterContext.date,
                filterContext.region,
                filterContext.serviceName,
                strHexSignature);
    }

    private String prepareCanonicalUrl(Context filterContext) {
        String canonicalURL = "";
    
        /* Step 1.1 Start with the HTTP request method (GET, PUT, POST, etc.), followed by a newline character. */
        canonicalURL += filterContext.method;
        canonicalURL += "\n";
    
        /* Step 1.2 Add the canonical URI parameter, followed by a newline character. */
        canonicalURL += "/";
        canonicalURL += "\n";
    
        /* Step 1.3 Add the canonical query string, followed by a newline character. */
        canonicalURL += filterContext.query;
        canonicalURL += "\n";
    
        /* Step 1.4 Add the canonical headers, followed by a newline character. */
        canonicalURL += format("content-type:{0}\n", filterContext.contentType);
        canonicalURL += format("host:{0}\n", filterContext.host);
        canonicalURL += format("x-amz-content-sha256:{0}\n", filterContext.xAmzContentSha256);
        canonicalURL += format("x-amz-date:{0}\n", filterContext.timestamp);
        canonicalURL += format("x-amz-target:{0}\n", filterContext.xAmzTarget);
    
        /* Step 1.5 Add the signed headers, followed by a newline character. */
        canonicalURL += "\ncontent-type;host;x-amz-content-sha256;x-amz-date;x-amz-target\n";
    
        /* Step 1.6 Use a hash (digest) function like SHA256 to create a hashed value from the payload in the body of the HTTP or HTTPS. */
        canonicalURL += AuthUtils.generateHex(filterContext.payload);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Canonical URL: " + canonicalURL);
        }
        return canonicalURL;
    }

    /**
     * Store the context of a request filter execution
     */
    private static class Context {
        private String method;
        private String query;
        private String timestamp;
        private String date;
        private String host;
        private String region;
        private String serviceName;
        private String payload;

        private String xAmzContentSha256;
        private String xAmzTarget;
        private String contentType;
    }
    
}
