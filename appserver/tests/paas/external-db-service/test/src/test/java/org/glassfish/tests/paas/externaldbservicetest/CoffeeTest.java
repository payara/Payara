/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.paas.externaldbservicetest;

import junit.framework.Assert;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.Deployer;
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
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Shalini M
 */

public class CoffeeTest {

    @Test
    public void test() throws Exception {

        // 1. Bootstrap GlassFish DAS in embedded mode.
        GlassFishProperties glassFishProperties = new GlassFishProperties();
        glassFishProperties.setInstanceRoot(System.getenv("S1AS_HOME")
                + "/domains/domain1");
        glassFishProperties.setConfigFileReadOnly(false);
        GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish(
                glassFishProperties);
        PrintStream sysout = System.out;
        glassfish.start();
        System.setOut(sysout);

        // 2. Deploy the PaaS application.
        File archive = new File(System.getProperty("basedir")
                + "/target/external_db_service_test.war");
        // TODO :: use mvn apis to get the archive location.
        Assert.assertTrue(archive.exists());

        Deployer deployer = null;
        String appName = null;
        CommandRunner commandRunner = glassfish.getCommandRunner();
        try {

            //2.1. Generate and run Derby VM
            CommandResult createDerbyVMResult = commandRunner.run(
                    "create-derby-vm", "--servicecharacteristics", "service-type=Database");
            System.out.println("\ncreate-derby-vm command output [ " +
                    createDerbyVMResult.getOutput() + "]");

	    String derbyIP = (createDerbyVMResult.getOutput().split("\n"))[1];

            System.out.println("Derby IP = " + derbyIP);

            //2.2. Create the shared DB service
            CommandResult createExternalServiceResult = commandRunner.run(
                    "create-external-service", "--configuration", "ip-address=" + derbyIP + ":databasename=foo:connectionAttributes=;'create=true':port=1527:user=APP:password=APP:host=" + derbyIP + ":classname=org.apache.derby.jdbc.ClientXADataSource:resourcetype=javax.sql.XADataSource",
		    "--servicetype",
                    "Database", "coffee-service");
            System.out.println("\ncreate-external-service command output [ " +
                    createExternalServiceResult.getOutput() + "]");

            //2.3. List services to check for the shared service
            CommandResult listSharedServicesResult = commandRunner.run(
                    "list-services");
            System.out.println("\nlist-services command output [ "
                    + listSharedServicesResult.getOutput() + "]");

            //2.4. Deploy app
            deployer = glassfish.getDeployer();
            appName = deployer.deploy(archive);

            System.err.println("Deployed [" + appName + "]");
            Assert.assertNotNull(appName);


            CommandResult result = commandRunner.run("list-services");
            System.out.println("\nlist-services command output [ "
                    + result.getOutput() + "]");

            // 3. Access the app to make sure PaaS app is correctly provisioned.
            String HTTP_PORT = (System.getProperty("http.port") != null) ? System
                    .getProperty("http.port") : "28080";

            String instanceIP = getLBIPAddress(glassfish);
            get("http://" + instanceIP + ":" + HTTP_PORT
                    + "/external_db_service_test/CoffeeServlet",
                    "Coffee ID");

            // 4. Undeploy the PaaS application .
        } finally {
            if (appName != null) {
                deployer.undeploy(appName);
                System.err.println("Undeployed [" + appName + "]");
                System.out.println("Destroying the resources created");
                //4.1. Delete External DB Service.
                CommandResult deleteResult = commandRunner.run(
                        "delete-external-service", "coffee-service");
                System.out.println("\ndelete-external-service command output [" +
                    deleteResult.getOutput() + "]");
                //4.2. Delete generated derby VM
                CommandResult deleteDerbyVMResult = commandRunner.run(
                    "delete-derby-vm");
                System.out.println("\ndelete-derby-vm command output [ " +
                    deleteDerbyVMResult.getOutput() + "]");
            }
        }

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
        System.out.println("\n***** SUCCESS **** Found [" + result
                + "] in the response.*****\n");
    }

    private String getLBIPAddress(GlassFish glassfish) {
        String lbIP = null;
        String IPAddressPattern = "IP-ADDRESS\\s*\n*(.*)\\s*\n(([01]?\\d*|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
                + "([0-9]?\\d\\d?|2[0-4]\\d|25[0-5]))";
        try {
            CommandRunner commandRunner = glassfish.getCommandRunner();
            String result = commandRunner
                    .run("list-services", "--type", "LB",
                            "--output", "IP-ADDRESS").getOutput().toString();
            if (result.contains("Nothing to list.")) {
                result = commandRunner
                        .run("list-services", "--type", "JavaEE", "--output",
                                "IP-ADDRESS").getOutput().toString();

                Pattern p = Pattern.compile(IPAddressPattern);
                Matcher m = p.matcher(result);
                if (m.find()) {
                    lbIP = m.group(2);
                } else {
                    lbIP = "localhost";
                }
            } else {
                Pattern p = Pattern.compile(IPAddressPattern);
                Matcher m = p.matcher(result);
                if (m.find()) {
                    lbIP = m.group(2);
                } else {
                    lbIP = "localhost";
                }

            }

        } catch (Exception e) {
            System.out.println("Regex has thrown an exception "
                    + e.getMessage());
            return "localhost";
        }
        return lbIP;
    }
}
