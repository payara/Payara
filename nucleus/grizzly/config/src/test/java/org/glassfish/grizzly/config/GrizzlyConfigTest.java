/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.config.dom.NetworkAddressValidator;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created Jan 5, 2009
 *
 * @author <a href="mailto:justin.d.lee@oracle.com">Justin Lee</a>
 */
@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
public class GrizzlyConfigTest extends BaseTestGrizzlyConfig {
    @Test
    public void processConfig() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config.xml");
            grizzlyConfig.setupNetwork();
            int count = 0;
            for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
                addStaticHttpHandler((GenericGrizzlyListener) listener, count++);
            }
            final String content = getContent(new URL("http://localhost:38082").openConnection());
            final String content2 = getContent(new URL("http://localhost:38083").openConnection());
            final String content3 = getContent(new URL("http://localhost:38084").openConnection());
            Assert.assertEquals("<html><body>You've found the server on port 38082</body></html>", content);
            Assert.assertEquals("<html><body>You've found the server on port 38083</body></html>", content2);
            Assert.assertEquals("<html><body>You've found the server on port 38084</body></html>", content3);
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void references() throws IOException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config.xml");
            final List<NetworkListener> list = grizzlyConfig.getConfig().getNetworkListeners().getNetworkListener();
            final NetworkListener listener = list.get(0);
            boolean found = false;
            for (NetworkListener ref : listener.findProtocol().findNetworkListeners()) {
                found |= ref.getName().equals(listener.getName());
            }
            assertTrue("Should find the NetworkListener in the list of references from Protocol", found);
            found = false;
            for (NetworkListener ref : listener.findTransport().findNetworkListeners()) {
                found |= ref.getName().equals(listener.getName());
            }
            assertTrue("Should find the NetworkListener in the list of references from Transport", found);
            found = false;
            for (NetworkListener ref : listener.findThreadPool().findNetworkListeners()) {
                found |= ref.getName().equals(listener.getName());
            }
            assertTrue("Should find the NetworkListener in the list of references from ThreadPool", found);
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void defaults() throws IOException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config.xml");
            final ThreadPool threadPool = grizzlyConfig.getConfig().getNetworkListeners().getThreadPool().get(0);
            Assert.assertEquals("5", threadPool.getMaxThreadPoolSize());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void testDefaultBufferConfiguration() throws Exception {
        GrizzlyConfig grizzlyConfig = null;
        try {
            configure();
            grizzlyConfig = new GrizzlyConfig("grizzly-config.xml");
            grizzlyConfig.setupNetwork();
            final String bufferType = grizzlyConfig.getConfig().getNetworkListeners().getNetworkListener().get(0).findTransport().getByteBufferType();
            Assert.assertEquals("heap", bufferType);
            GenericGrizzlyListener genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-1");
            MemoryManager mm = genericGrizzlyListener.getTransport().getMemoryManager();
            assertEquals(HeapMemoryManager.class.getName(), mm.getClass().getName());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void testSelectionKeyHandlerConfiguration() throws Exception {
        GrizzlyConfig grizzlyConfig = null;
        try {
            configure();
            grizzlyConfig = new GrizzlyConfig("grizzly-config-skh.xml");
            grizzlyConfig.setupNetwork();
            final String bufferType = grizzlyConfig.getConfig().getNetworkListeners().getNetworkListener().get(0).findTransport().getByteBufferType();
            GenericGrizzlyListener genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-1");
            NIOTransport transport = (NIOTransport) genericGrizzlyListener.getTransport();
            assertNotSame(DummySelectionKeyHandler.class.getName(), transport.getSelectionKeyHandler().getClass().getName());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void testDirectBufferConfiguration() throws Exception {
        GrizzlyConfig grizzlyConfig = null;
        try {
            configure();
            grizzlyConfig = new GrizzlyConfig("grizzly-direct-buffer.xml");
            grizzlyConfig.setupNetwork();
            final String bufferType = grizzlyConfig.getConfig().getNetworkListeners().getNetworkListener().get(0).findTransport().getByteBufferType();
            Assert.assertEquals("direct", bufferType);
            GenericGrizzlyListener genericGrizzlyListener =
                                           (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-1");
            MemoryManager mm = genericGrizzlyListener.getTransport().getMemoryManager();
            assertEquals(ByteBufferManager.class.getName(), mm.getClass().getName());
            assertTrue(((ByteBufferManager) mm).isDirect());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void testSocketBufferConfiguration() throws Exception {
        GrizzlyConfig grizzlyConfig = null;
        try {
            configure();
            grizzlyConfig = new GrizzlyConfig("grizzly-config-socket.xml");
            grizzlyConfig.setupNetwork();
            GenericGrizzlyListener genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-1");
            Transport t = genericGrizzlyListener.getTransport();

            assertEquals(-1, t.getReadBufferSize());
            assertEquals(-1, t.getWriteBufferSize());

            genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-2");
            t = genericGrizzlyListener.getTransport();
            assertEquals(8192, t.getReadBufferSize());
            assertEquals(-1, t.getWriteBufferSize());

            genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-3");
            t = genericGrizzlyListener.getTransport();
            assertEquals(-1, t.getReadBufferSize());
            assertEquals(8000, t.getWriteBufferSize());

            genericGrizzlyListener =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-4");
            t = genericGrizzlyListener.getTransport();
            assertEquals(6000, t.getReadBufferSize());
            assertEquals(5000, t.getWriteBufferSize());
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdownNetwork();
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void ssl() throws URISyntaxException, IOException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            configure();
            grizzlyConfig = new GrizzlyConfig("grizzly-config-ssl.xml");
            grizzlyConfig.setupNetwork();
            int count = 0;
            for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
                addStaticHttpHandler((GenericGrizzlyListener) listener, count++);
            }
            
            Assert.assertEquals("<html><body>You've found the server on port 38082</body></html>",
                    getContent(new URL("https://localhost:38082").openConnection()));
            Assert.assertEquals("<html><body>You've found the server on port 38083</body></html>",
                    getContent(new URL("https://localhost:38083").openConnection()));
            Assert.assertEquals("<html><body>You've found the server on port 38084</body></html>",
                    getContent(new URL("https://localhost:38084").openConnection()));
            Assert.assertEquals("<html><body>You've found the server on port 38085</body></html>",
                    getContent(new URL("https://localhost:38085").openConnection()));
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    private void configure() throws URISyntaxException {
        ClassLoader cl = getClass().getClassLoader();
        System
            .setProperty("javax.net.ssl.trustStore", new File(cl.getResource("cacerts.jks").toURI()).getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        System
            .setProperty("javax.net.ssl.keyStore", new File(cl.getResource("keystore.jks").toURI()).getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
    }

//    @Test(expected = GrizzlyConfigException.class)
    public void badConfig() throws IOException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-bad.xml");
            grizzlyConfig.setupNetwork();
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    public void timeoutDisabled() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-timeout-disabled.xml");
            grizzlyConfig.setupNetwork();
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    @Test
    public void testNetworkAddressValidator() {
        NetworkAddressValidator validator = new NetworkAddressValidator();
        assertTrue(validator.isValid("${SOME_PROP}", null));
        assertFalse(validator.isValid("$SOME_PROP}", null));
        assertFalse(validator.isValid("${SOME_PROP", null));
        assertFalse(validator.isValid("{SOME_PROP}", null));
        assertTrue(validator.isValid("127.0.0.1", null));
        assertFalse(validator.isValid("1271.2.1.3", null));
        assertTrue(validator.isValid("::1", null));
        assertFalse(validator.isValid(":1", null));
    }
    
    @Test
    public void ioStrategySet() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-io-strategies.xml");
            grizzlyConfig.setupNetwork();
            GenericGrizzlyListener genericGrizzlyListener1 =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-1");
            
            Assert.assertEquals(SameThreadIOStrategy.class, genericGrizzlyListener1.getTransport().getIOStrategy().getClass());

            GenericGrizzlyListener genericGrizzlyListener2 =
                    (GenericGrizzlyListener) getListener(grizzlyConfig, "http-listener-2");
            
            Assert.assertEquals(WorkerThreadIOStrategy.class, genericGrizzlyListener2.getTransport().getIOStrategy().getClass());
            
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }
    
    @Test
    public void schemeOverride() throws IOException, InstantiationException {
        GrizzlyConfig grizzlyConfig = null;
        try {
            grizzlyConfig = new GrizzlyConfig("grizzly-config-scheme-override.xml");
            grizzlyConfig.setupNetwork();
            for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
                setHttpHandler((GenericGrizzlyListener) listener, new HttpHandler() {

                    @Override
                    public void service(Request request, Response response) throws Exception {
                        response.getWriter().write(request.getScheme());
                    }
                });
            }
            
            final String content = getContent(new URL("http://localhost:38082").openConnection());
            final String content2 = getContent(new URL("http://localhost:38083").openConnection());
            
            Assert.assertEquals("http", content);
            Assert.assertEquals("https", content2);
        } finally {
            if (grizzlyConfig != null) {
                grizzlyConfig.shutdown();
            }
        }
    }

    private static GrizzlyListener getListener(GrizzlyConfig grizzlyConfig,
            String listenerName) {
        for (GrizzlyListener listener : grizzlyConfig.getListeners()) {
            GenericGrizzlyListener genericGrizzlyListener = (GenericGrizzlyListener) listener;
            if (listenerName.equals(genericGrizzlyListener.getName())) {
                return listener;
            }
        }
        
        return null;
    }
}
