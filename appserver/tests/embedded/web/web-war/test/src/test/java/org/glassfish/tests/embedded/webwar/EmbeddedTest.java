/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded.webwar;

import java.util.*;
import org.junit.Test;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.internal.embedded.Server;
import org.glassfish.internal.embedded.LifecycleException;
import org.glassfish.internal.embedded.EmbeddedContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.internal.embedded.EmbeddedDeployer;
import org.glassfish.internal.embedded.Port;
import org.glassfish.internal.embedded.ScatteredArchive;
import org.glassfish.internal.embedded.ScatteredArchive.Builder;
import org.glassfish.internal.embedded.ContainerBuilder;
import org.glassfish.internal.embedded.admin.AdminInfo;
import org.glassfish.internal.embedded.admin.EmbeddedAdminContainer;
import org.glassfish.internal.embedded.admin.CommandExecution;
import org.glassfish.internal.embedded.admin.CommandParameters;
import org.glassfish.api.embedded.web.EmbeddedWebContainer;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.container.Sniffer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import org.glassfish.api.admin.ServerEnvironment;

public class EmbeddedTest {

    private static Port http=null;
    private static Server server = null;

    @BeforeClass
    public static void setup() {
        Server.Builder builder = new Server.Builder("build");

        server = builder.build();
        NetworkConfig nc = server.getHabitat().getService(NetworkConfig.class,
                ServerEnvironment.DEFAULT_INSTANCE_NAME);
        List<NetworkListener> listeners = nc.getNetworkListeners().getNetworkListener();
        System.out.println("Network listener size before creation " + listeners.size());
        for (NetworkListener nl : listeners) {
            System.out.println("Network listener " + nl.getPort());
        }
        try {
            http = server.createPort(8080);
            ContainerBuilder b = server.createConfig(ContainerBuilder.Type.web);
            server.addContainer(b);
            EmbeddedWebContainer embedded = (EmbeddedWebContainer) b.create(server);
            embedded.bind(http, "http");
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        listeners = nc.getNetworkListeners().getNetworkListener();
        System.out.println("Network listener size after creation " + listeners.size());
        Assert.assertTrue(listeners.size() == 1);
        for (NetworkListener nl : listeners) {
            System.out.println("Network listener " + nl.getPort());
        }
        Collection<NetworkListener> cnl = server.getHabitat().getAllServices(NetworkListener.class);
        System.out.println("Network listener size after creation " + cnl.size());
        for (NetworkListener nl : cnl) {
            System.out.println("Network listener " + nl.getPort());
        }

        server.addContainer(ContainerBuilder.Type.all);
    }    

    @Test
    public void testWeb() throws Exception {
        System.out.println("Starting Web " + server);
        ContainerBuilder b = server.createConfig(ContainerBuilder.Type.web);
        System.out.println("builder is " + b);
        server.addContainer(b);
        EmbeddedDeployer deployer = server.getDeployer();
        System.out.println("Added Web");

        String testClass = "org/glassfish/tests/embedded/webwar/EmbeddedTest.class";
        URL source = this.getClass().getClassLoader().getResource(testClass);
        String p = source.getPath().substring(0, source.getPath().length()-testClass.length()) +
            "../../../war/target/test-war.war";

        System.out.println("Root is " + p);
        DeployCommandParameters dp = new DeployCommandParameters(new File(p));

        System.out.println("Deploying " + p);
        String appName = null;
        try {
            appName = deployer.deploy(new File(p), dp);
            System.out.println("Deployed " + appName);
            Assert.assertTrue(appName != null);
            try {
                URL servlet = new URL("http://localhost:8080/test-war/");
                URLConnection yc = servlet.openConnection();
                BufferedReader in = new BufferedReader(
                                        new InputStreamReader(
                                        yc.getInputStream()));
                String inputLine = in.readLine();
                if (inputLine != null)
                    System.out.println(inputLine);
                Assert.assertEquals(inputLine.trim(), "filterMessage=213");
                in.close();
            } catch(Exception e) {
                e.printStackTrace();
                throw e;
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
        if (appName!=null)
            deployer.undeploy(appName, null);

    }

    public static void  close() throws LifecycleException {
        if (http!=null) {
            http.close();
            http=null;
        }
        System.out.println("Stopping server " + server);
        if (server!=null) {
            server.stop();
            server=null;
        }
    }
}
