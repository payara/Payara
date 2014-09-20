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

package org.glassfish.tests.weball;

import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.internal.embedded.*;
import org.glassfish.api.embedded.web.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.glassfish.api.admin.ServerEnvironment;

/**
 * @author Vivek Pandey
 */
public class WebAllTest {
    private static Server server = null;
    private static Port http=null;
    
    @BeforeClass
    public static void setup() throws IOException {

        //create directory 'glassfish' inside target so that it gets cleaned by itself
        EmbeddedFileSystem.Builder fsBuilder = new EmbeddedFileSystem.Builder();
        String p = System.getProperty("buildDir");
        File root = new File(p).getParentFile();
        root =new File(root, "glassfish");
        //If web container requires docroot to be there may be it should be automatically created by embedded API
        new File(root, "docroot").mkdirs();

        EmbeddedFileSystem fs = fsBuilder.instanceRoot(root).build();
        Server.Builder builder = new Server.Builder("WebAllTest");
        builder.embeddedFileSystem(fs);                
        server = builder.build();
        server.getHabitat().getService(NetworkConfig.class,
                ServerEnvironment.DEFAULT_INSTANCE_NAME);
        http = server.createPort(8080);
        Assert.assertNotNull("Failed to create port 8080!", http);
        ContainerBuilder b = server.createConfig(ContainerBuilder.Type.web);
        EmbeddedWebContainer embedded = (EmbeddedWebContainer) b.create(server);
        embedded.setConfiguration((WebBuilder)b);
        embedded.bind(http, "http");

    }

    @Test
    public void testWeb() throws Exception {
        System.out.println("Starting Web " + server);
        ContainerBuilder b = server.createConfig(ContainerBuilder.Type.web);

        System.out.println("builder is " + b);
        server.addContainer(b);
        EmbeddedDeployer deployer = server.getDeployer();
        System.out.println("Added Web");

        String p = System.getProperty("buildDir");
        System.out.println("Root is " + p);
        ScatteredArchive.Builder builder = new ScatteredArchive.Builder("sampleweb", new File(p));
        builder.resources(new File(p));
        builder.addClassPath((new File(p)).toURL());
        DeployCommandParameters dp = new DeployCommandParameters(new File(p));

        System.out.println("Deploying " + p);
        String appName = deployer.deploy(builder.buildWar(), dp);
        Assert.assertNotNull("Deployment failed!", appName);

        URL servlet = new URL("http://localhost:8080/classes/hello");
        URLConnection yc = servlet.openConnection();
        BufferedReader in = new BufferedReader(
                                new InputStreamReader(
                                yc.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null){
            sb.append(inputLine);
        }
        in.close();
        System.out.println(inputLine);

        Assert.assertEquals("Hello World!", sb.toString());

        deployer.undeploy(appName, null);
    }
}
