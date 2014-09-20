/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.grizzly.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.glassfish.grizzly.http.server.HttpHandler;

import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.junit.Assert;
import org.jvnet.hk2.config.Dom;

public class BaseTestGrizzlyConfig {
    protected String getContent(URLConnection connection) {
        try {
            InputStream inputStream = connection.getInputStream();
            InputStreamReader reader = new InputStreamReader(inputStream);
            try {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    builder.append(buffer, 0, read);
                }
                return builder.toString();
            } finally {
                reader.close();
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        return "";
    }

    protected void addStaticHttpHandler(GenericGrizzlyListener listener, int count) {
        final String name = "target/tmp/"
                + Dom.convertName(getClass().getSimpleName()) + count;
        File dir = new File(name);
        dir.mkdirs();
        dir.deleteOnExit();
        
        FileWriter writer;
        try {
            final File file = new File(dir, "index.html");
            file.deleteOnExit();
            
            writer = new FileWriter(file);
            try {
                writer.write("<html><body>You've found the server on port " + listener.getPort() + "</body></html>");
                writer.flush();
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        
        final List<HttpServerFilter> httpServerFilters = listener.getFilters(HttpServerFilter.class);

        for (HttpServerFilter httpServerFilter : httpServerFilters) {
            httpServerFilter.setHttpHandler(new StaticHttpHandler(name));
        }
    }

    protected void setHttpHandler(GenericGrizzlyListener listener, HttpHandler handler) {
        final List<HttpServerFilter> httpServerFilters = listener.getFilters(HttpServerFilter.class);

        for (HttpServerFilter httpServerFilter : httpServerFilters) {
            httpServerFilter.setHttpHandler(handler);
        }
    }

    public SSLSocketFactory getSSLSocketFactory() throws IOException {
        try {
            //---------------------------------
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                    }
                }
            };
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            //---------------------------------
            return sc.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }
}
