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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created Jan 5, 2009
 *
 * @author <a href="mailto:justin.d.lee@oracle.com">Justin Lee</a>
 */
public class PUGrizzlyConfigTest extends BaseTestGrizzlyConfig {
    static int count;

    @Test
    public void puConfig() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;
        
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-pu.xml");
            grizzlyConfig.setupNetwork();
            for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
                addStaticHttpHandler((GenericGrizzlyListener) listener, count++);
            }
            final String httpContent = getContent(new URL("http://localhost:38082").openConnection());
            Assert.assertEquals("<html><body>You've found the server on port 38082</body></html>", httpContent);

            final String xProtocolContent = getXProtocolContent("localhost", 38082);
            Assert.assertEquals("X-Protocol-Response", xProtocolContent);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
            }
        }
    }

    @Test
    public void puHttpHttpsSamePortConfig() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;

        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-pu-http-https-same-port.xml");
            grizzlyConfig.setupNetwork();
            for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
                addStaticHttpHandler((GenericGrizzlyListener) listener, count++);
            }
            final String httpContent1 = getContent(new URL("http://localhost:38082").openConnection());
            Assert.assertEquals("<html><body>You've found the server on port 38082</body></html>", httpContent1);

            HttpsURLConnection.setDefaultSSLSocketFactory(getSSLSocketFactory());
            final String httpContent2 = getContent(new URL("https://localhost:38082").openConnection());
            Assert.assertEquals("<html><body>You've found the server on port 38082</body></html>", httpContent2);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
            }
        }
    }

    @Test
    public void wrongPuConfigLoop() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;

        boolean isIllegalState = false;
        
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-pu-loop.xml");
            grizzlyConfig.setupNetwork();
        } catch (IllegalStateException e) {
            isIllegalState = true;
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
            }
        }

        Assert.assertTrue("Double http definition should throw IllegalStateException", isIllegalState);
    }
        
//    private String getContent(URLConnection connection) throws IOException {
//        final InputStream inputStream = connection.getInputStream();
//        StringBuilder builder;
//        InputStreamReader reader = new InputStreamReader(inputStream);
//        try {
//            builder = new StringBuilder();
//            char[] buffer = new char[1024];
//            int read;
//            while ((read = reader.read(buffer)) != -1) {
//                builder.append(buffer, 0, read);
//            }
//        } finally {
//            reader.close();
//        }
//        return builder.toString();
//    }
//
//    private void addStaticHttpHandler(GenericGrizzlyListener listener, int count) throws IOException {
//        final String name = System.getProperty("java.io.tmpdir", "/tmp") + "/grizzly-config-root" + count;
//        File dir = new File(name);
//        dir.mkdirs();
//        final FileWriter writer = new FileWriter(new File(dir, "index.html"));
//        try {
//            writer.write("<html><body>You've found the server on port " + listener.getPort() + "</body></html>");
//        } finally {
//            writer.flush();
//            writer.close();
//        }
//
//        final List<HttpServerFilter> httpServerFilters = listener.getFilters(HttpServerFilter.class);
//
//        for (HttpServerFilter httpServerFilter : httpServerFilters) {
//            httpServerFilter.setHttpHandler(new StaticHttpHandler(name));
//        }
//    }

    @SuppressWarnings({"SocketOpenedButNotSafelyClosed"})
    private String getXProtocolContent(String host, int port) throws IOException {
        Socket s = null;
        OutputStream os = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        
        try {
            s = new Socket(host, port);
            os = s.getOutputStream();
            os.write("X-protocol".getBytes());
            os.flush();


            is = s.getInputStream();
            baos = new ByteArrayOutputStream();
            int b;
            while ((b = is.read()) != -1) {
                baos.write(b);
            }
        } finally {
            close(os);
            close(is);
            close(baos);
            if (s != null) {
                try {
                    s.close();
                } catch (IOException e) {}
            }
        }

        return new String(baos.toByteArray());
    }

    private void close(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {}
        }
    }
}
