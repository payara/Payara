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

package org.glassfish.tests.paas.lazysharedservice;


import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.embeddable.*;
import org.glassfish.internal.api.Globals;
import org.junit.Assert;
import org.junit.Test;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sandhya Kripalani
 */

public class LazySharedServiceTest {

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

        // 2. Deploy the PaaS-bookstore application.
        File archive = new File(System.getProperty("basedir")
                + "/target/lazy-shared-service-test.war"); // TODO :: use mvn apis to get the
        // archive location.
        org.junit.Assert.assertTrue(archive.exists());

        Deployer deployer = null;
        String appName = null;
        try {

            //3.Create the shared services first, as these services will be referenced by the application
            createSharedServices();

            String instanceIP = getLBIPAddress(glassfish);

            //Check to see if the LB service is not provisioned
            {
                int responseCode=getResponseCode("http://"+instanceIP+":50080");
                junit.framework.Assert.assertTrue(responseCode==404);
            }

            //Check to see if the DB service is not provisioned
            {
                ServiceLocator habitat = Globals.getDefaultHabitat();
                org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
                ActionReport report = habitat.getService(ActionReport.class);

                org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("ping-connection-pool", report);
                ParameterMap parameterMap = new ParameterMap();
                parameterMap.add("DEFAULT","jdbc/lazy_init_shared_service");
                invocation.parameters(parameterMap).execute();
                System.out.println("Message while trying to ping an uncreated jdbc pool :: "+report.getTopMessagePart().getMessage());
                Assert.assertTrue(report.hasFailures());
            }

            deployer = glassfish.getDeployer();
            appName = deployer.deploy(archive);

            System.err.println("Deployed [" + appName + "]");
            Assert.assertNotNull(appName);

            //4. View the status of the shared services to see if they were provisioned after deployment of the app.
            checkStatusOfSharedServices();

            CommandRunner commandRunner = glassfish.getCommandRunner();
            CommandResult result = commandRunner.run("list-services");
            System.out.println("\nlist-services command output [ "
                    + result.getOutput() + "]");

            // 5. Access the app to make sure PaaS-lazy-shared-service-test app is correctly
            // provisioned.

            String HTTP_PORT = (System.getProperty("http.port") != null) ? System
                    .getProperty("http.port") : "28080";

            get("http://" + instanceIP + ":" + HTTP_PORT
                    + "/lazy-shared-service-test/BasicDBPaaSServlet", "SYSFILES");



            // 6. Undeploy the Zoo catalogue application .

        } finally {
            if (appName != null) {
                deployer.undeploy(appName);
                System.err.println("Undeployed [" + appName + "]");
                deleteSharedService();
                try {
                    boolean undeployClean = false;
                    CommandResult commandResult = glassfish.getCommandRunner()
                            .run("list-services");
                    System.out.println(commandResult.getOutput().toString());
                    if (commandResult.getOutput().contains("Nothing to list")) {
                        undeployClean = true;
                    }
                    Assert.assertTrue(undeployClean);
                } catch (Exception e) {
                    System.err
                            .println("Couldn't varify whether undeploy succeeded");
                }
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

    private void createSharedServices() {

        System.out.println("################### Trying to Create Shared Service #######################");
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);

        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("create-shared-service", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("servicetype","JavaEE");
        parameterMap.add("characteristics","service-type=JavaEE");
        parameterMap.add("configuration","min.clustersize=2:max.clustersize=4");
        parameterMap.add("initmode","lazy");
        parameterMap.add("DEFAULT","my-shared-gf-service");
        invocation.parameters(parameterMap).execute();

        System.out.println("Created shared service 'my-shared-gf-service' :" + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());


        //Create shared service of type Database
        // asadmin create-shared-service --characteristics service-type=Database --configuration database.name=my-shared-db-service --servicetype Database my-shared-db-service
        parameterMap=new ParameterMap();
        parameterMap.add("servicetype", "Database");
        parameterMap.add("characteristics", "service-type=Database");
        parameterMap.add("configuration", "database.name=my-shared-db-service");
        parameterMap.add("initmode","lazy");
        parameterMap.add("DEFAULT", "my-shared-db-service");

        invocation.parameters(parameterMap).execute();

        System.out.println("Created shared service 'my-shared-db-service' :" + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());

        // Create shared service of type LB
        //asadmin create-shared-service --template LBNative --configuration http-port=50080:https-port=50081:ssl-enabled=true --servicetype LB my-shared-lb-service
        parameterMap = new ParameterMap();
        parameterMap.add("servicetype", "LB");
        parameterMap.add("characteristics", "service-type=LB");
        parameterMap.add("configuration", "http-port=50080:https-port=50081:ssl-enabled=true");
        parameterMap.add("initmode","lazy");
        parameterMap.add("DEFAULT", "my-shared-lb-service");
        invocation.parameters(parameterMap).execute();

        System.out.println("Created shared service 'my-shared-lb-service' :" + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());

        //List the services and check the status of both the services - it should be 'RUNNING'
        invocation = commandRunner.getCommandInvocation("list-services", report);
        parameterMap = new ParameterMap();
        parameterMap.add("scope", "shared");
        parameterMap.add("output", "service-name,state");
        invocation.parameters(parameterMap).execute();

        boolean serviceUninitialized = false;
        List<Map<String, String>> list = (List<Map<String, String>>) report.getExtraProperties().get("list");
        for (Map<String, String> map : list) {
            serviceUninitialized = false;
            String state = map.get("STATE");
            if ("UNINITIALIZED".equalsIgnoreCase(state)) {
                serviceUninitialized = true;
            }else{
                break;
            }
        }
        Assert.assertTrue(serviceUninitialized);//check if the shared services are in UNINITIALIZED state as the services should be provisioned lazily..

    }

    private void checkStatusOfSharedServices() {

        System.out.println("$$$$$$$$$$$$$ Checking Status of Shared Services $$$$$$$$$$$$$$$");
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);

        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation =  commandRunner.getCommandInvocation("list-services", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("scope", "shared");
        parameterMap.add("output", "service-name,state");
        invocation.parameters(parameterMap).execute();

        boolean sharedServicesRunning = false;
        List<Map<String, String>> list = (List<Map<String, String>>) report.getExtraProperties().get("list");
        for (Map<String, String> map : list) {
            sharedServicesRunning = false;
            String state = map.get("STATE");
            if ("RUNNING".equalsIgnoreCase(state)) {
                sharedServicesRunning = true;
            }else{
                break;
            }
        }
        Assert.assertTrue(sharedServicesRunning);//check if the shared services are running.

    }

    public int getResponseCode(String urlString) {
        HttpURLConnection huc;

        try {
            URL u = new URL(urlString);
            huc =  (HttpURLConnection)  u.openConnection();
            huc.setRequestMethod("GET");
            huc.connect();
        } catch (ConnectException e){
            System.out.println("Expected Failure Msg while trying to connect to Unprovisioned LB service : ");
            e.printStackTrace();
           return 404;
        }catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return 200;
    }



    private void deleteSharedService() {
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);
        //Try stopping a shared service, referenced by the app. Should 'FAIL'

        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("delete-shared-service", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-lb-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());

        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-db-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());

        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-gf-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());

    }

}
