/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.http;

import com.sun.enterprise.util.JDK;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.net.ssl.SSLContext;

@WebServlet(value = "/hello")
public class TestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getOutputStream().print("Hello World");
        Response response = newClientBuilder().build()
                .target("https://google.com")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(new Object()));

        resp.getOutputStream().print("\n"+response.getStatus());
    }

    static boolean hasTLS13Support() {
        List<String> jvmOptions = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean openJSSEOption = jvmOptions.contains("-XX:+UseOpenJSSE");
        return JDK.isTls13Supported() || openJSSEOption && JDK.isOpenJSSEFlagRequired();
    }

    static ClientBuilder newClientBuilder() {
        SSLContext sslContext = null;
        try {
            if (!hasTLS13Support()) {
                sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
            }
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new IllegalStateException(ex);
        }

        ClientBuilder builder = ClientBuilder.newBuilder();
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        return builder;
    }
}
