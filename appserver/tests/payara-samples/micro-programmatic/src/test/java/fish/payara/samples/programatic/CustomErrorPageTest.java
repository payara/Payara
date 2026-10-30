/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.programatic;

import fish.payara.micro.ClusterCommandResult;
import fish.payara.micro.PayaraMicro;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class CustomErrorPageTest {

    private static final String HOST_NAME = System.getProperty("payara.adminHost", "localhost");
    private static final String CUSTOM_ERROR_PAGE_REPSONSE = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Page Not Found</title>\n"
            + "</head>\n"
            + "<body>\n"
            + "    <h1>Oops! This page does not exist.</h1>\n"
            + "</body>\n"
            + "</html>\n";

    private static final String ERROR_PAGE_REPSONSE = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Payara Micro 6.2025.5- Error report</title><style type=\"text/css\"><!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;}  B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} P{font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}A {color : black;}HR {color : #525D76;}--></style> </head><body><h1>HTTP Status 404 - </h1><hr/><p><b>type</b> Status report </p><p><b>message </b></p><p><b>description </b>The requested resource is not available.</p><hr/><h3>Payara Micro 6.2025.5</h3></body></html>";


    @Test
    public void customErrorPageIsSet() throws Exception {
        PayaraMicro micro = PayaraMicro.getInstance();
        micro.setPreBootHandler(t -> {
            ClusterCommandResult result = t.run("set", "configs.config.server-config.http-service.virtual-server.server.property.send-error_1=\"code=404 path=src/test/resources/custom-404.html reason=not_found\"");
            Assert.assertEquals(ClusterCommandResult.ExitStatus.SUCCESS, result.getExitStatus());
            Assert.assertNull(result.getFailureCause());
        });
        micro.setHttpAutoBind(true);
        micro.bootStrap();
        Assert.assertEquals(CUSTOM_ERROR_PAGE_REPSONSE, download());
        micro.shutdown();
    }

    @Test
    public void defaultErrorPage() throws Exception {
        PayaraMicro micro = PayaraMicro.getInstance();
        micro.setHttpAutoBind(true);
        micro.bootStrap();
        Assert.assertEquals(ERROR_PAGE_REPSONSE, download());
        micro.shutdown();
    }

    private String download() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + CustomErrorPageTest.HOST_NAME + ":8080" + "/"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

}
