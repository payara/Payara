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

package org.glassfish.tests.paas.basic;

import junit.framework.Assert;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author bhavanishankar@dev.java.net
 */

public class BasicPaaSTest {

    @Test
    public void test() throws Exception {

        // 1. Create the config file required for non-virtual deployment.
        createCloudConfigFile();


        // 2. Bootstrap GlassFish DAS in embedded mode.
        GlassFishProperties glassFishProperties = new GlassFishProperties();
        glassFishProperties.setInstanceRoot(System.getenv("S1AS_HOME") + "/domains/domain1");
        glassFishProperties.setConfigFileReadOnly(false);
        GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(glassFishProperties);
        PrintStream sysout = System.out;
        glassfish.start();
        System.setOut(sysout);

        // 3. Deploy the PaaS application.
        File archive = new File(System.getProperty("basedir") +
                "/target/basic_paas_sample.war"); // TODO :: use mvn apis to get the archive location.
        Assert.assertTrue(archive.exists());
        CommandRunner commandRunner = glassfish.getCommandRunner();
        CommandResult result = commandRunner.run("cloud-deploy",
                archive.getAbsolutePath());
        System.err.println("Deployed basic_paas_sample, command result = ["
                + result.getOutput() + "]");
        Assert.assertNull(result.getFailureCause());

        // 4. Access the app to make sure PaaS app is correctly provisioned.
        get("http://localhost:28080/basic_paas_sample/BasicPaaSServlet",
                "Request headers from the request:");

        // 5. Undeploy the PaaS application . TODO :: use cloud-undeploy??
        result = commandRunner.run("undeploy", "basic_paas_sample");
        System.err.println("Undeployed basic_paas_sample, command result = [" +
                result.getOutput() + "]");
        // TODO :: stop-cluster and unprovisioning should be done automatically
        // by the orchestrator during undeploy. For now, we need to do it manually.
        commandRunner.run("stop-cluster", "mycluster");

        // 6. Stop the GlassFish DAS
        glassfish.dispose();

    }

    private void createCloudConfigFile() throws Exception {
        File configFile = new File(System.getenv("S1AS_HOME") + "/config/cloud-config.properties");
        Properties configProps = new Properties();
        configProps.setProperty("APPLICATION_SERVER_PROVIDER", "GLASSFISH");
        configProps.setProperty("GF_PORT", "4848");
        configProps.setProperty("GF_TARGET", "server");
        configProps.setProperty("GF_INSTALL_DIR", System.getenv("S1AS_HOME") + File.separator + "..");
        // TODO :: remove all AWS properties later.
        configProps.setProperty("AWS_INSTANCE_USERNAME", System.getProperty("user.name"));
        configProps.setProperty("AWS_KEYPAIR", System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa");
        configProps.setProperty("AWS_LOCAL_KEYPAIR_LOCATION", System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "id_rsa");
        configProps.store(new FileOutputStream(configFile), null);
    }

    private void get(String urlStr, String result) throws Exception {
        URL url = new URL(urlStr);
        URLConnection yc = url.openConnection();
        System.out.println("\nURLConnection [" + yc + "] : ");
        BufferedReader in = new BufferedReader(new InputStreamReader(
                yc.getInputStream()));
        String line = null;
        boolean found = false;
        while ((line = in.readLine()) != null) {
            System.out.println(line);
            if (line.indexOf(result) != -1) {
                found = true;
            }
        }
        Assert.assertTrue(found);
        System.out.println("\n***** SUCCESS **** Found [" + result + "] in the response.*****\n");
    }

    void printContents(URI jarURI) throws IOException {
        JarFile jarfile = new JarFile(new File(jarURI));
        System.out.println("\n\n[" + jarURI + "] contents : \n");
        Enumeration<JarEntry> entries = jarfile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            System.out.println(entry.getSize() + "\t" + new Date(entry.getTime()) +
                    "\t" + entry.getName());
        }
        System.out.println("");
    }
}
