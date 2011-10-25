/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.embeddable.*;
import org.glassfish.embeddable.web.*;  
import org.glassfish.embeddable.web.config.*;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for VirtualServerConfig#setHostNames
 * 
 * @author Amy Roh
 */
public class EmbeddedVirtualServerHostNameTest {

    static GlassFish glassfish;
    static WebContainer embedded;
    static String contextRoot = "host";
    static int newPort = 9090;
    
    @BeforeClass
    public static void setupServer() throws GlassFishException {

        GlassFishRuntime runtime = GlassFishRuntime.bootstrap();
        GlassFishProperties props = new GlassFishProperties();
        props.setPort("http-listener", 8080);
        GlassFish glassfish = runtime.newGlassFish(props);
        glassfish.start();
        embedded = glassfish.getService(WebContainer.class);

    }
    
    @Test
    public void test() throws Exception {

        String virtualServerId = "example";
        File root = new File("target/classes");
        VirtualServer virtualServer = embedded.createVirtualServer(virtualServerId, root);

        VirtualServerConfig config = new VirtualServerConfig();
        config.setHostNames("example.com");
        virtualServer.setConfig(config);
        embedded.addVirtualServer(virtualServer);

        VirtualServer vs = embedded.getVirtualServer(virtualServerId);
        Assert.assertEquals(virtualServerId,vs.getID());

        Context context = (Context) embedded.createContext(root);
        embedded.addContext(context, contextRoot);

        // curl -i -H 'Host: example.com' http://localhost:8080/
        URL servlet = new URL("http://localhost:8080/"+contextRoot+"/hello");
        URLConnection yc = servlet.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null){
            sb.append(inputLine);
        }
        in.close();
        System.out.println(inputLine);
        Assert.assertEquals("Hello World!", sb.toString());

    } 

    @AfterClass
    public static void shutdownServer() throws GlassFishException {
        System.out.println("Stopping server " + glassfish);
        if (glassfish != null) {
            glassfish.stop();
            glassfish.dispose();
            glassfish = null;
        }
    }
    
}
