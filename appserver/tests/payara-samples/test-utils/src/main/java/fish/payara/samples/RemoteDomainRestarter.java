/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples;

import java.io.IOException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Restart domain only once, only when setting it up, and not any more
 * And only if the server thinks it needs a restart.
 *
 * Restarting via rest because on a remote node, domain directories may
 * not be accessible at all. After restart, we wait for the domain to restart until returning.
 *
 * @author lprimak
 */
public class RemoteDomainRestarter {
    public static boolean restart() {
        // first, try remote domain
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String adminHost = System.getProperty("payara.adminHost", "localhost");
            String domainURL = String.format("http://%s:4848/__asadmin/", adminHost);
            HttpGet restartCommand = new HttpGet(domainURL.concat("restart-domain"));
            HttpGet restartRequired = new HttpGet(domainURL.concat("_get-restart-required"));
            try (CloseableHttpResponse httpresponse = httpclient.execute(restartCommand)) {
                if (httpresponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    detectWhenRestartCompletes(httpclient, restartRequired);
                }
            }
        } catch (IOException | InterruptedException ex) {
            return false;
        }
        return true;
    }

    private static void detectWhenRestartCompletes(final CloseableHttpClient httpclient,
            HttpGet restartRequired) throws InterruptedException {
        Thread.sleep(3 * 1000);
        for (int ii = 0; ii < 60 * 100; ++ii) {
            Thread.sleep(10);
            try (CloseableHttpResponse waitingOnRestart = httpclient.execute(restartRequired)){
                if (waitingOnRestart.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    break;
                }
            } catch (IOException ex) {
                // this exception is expected, as the server is being restarted
            }
        }
    }
}
