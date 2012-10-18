/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.paas.enabledisable;

import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sandhya Kripalani
 */

public class ExtSharedServiceEnableDisableTest {

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
                + "/target/ext-shared-service-enable-disable-test.war"); // TODO :: use mvn apis to get the
        // archive location.
        Assert.assertTrue(archive.exists());

        //Obtaining the IP address of the DAS
        String ip_address="127.0.0.1";
        try{
        Enumeration netint_enum= NetworkInterface.getNetworkInterfaces();
        for (Iterator it = Collections.list(netint_enum).iterator(); it.hasNext();) {
                NetworkInterface netint = (NetworkInterface) it.next();
            if(netint.getName().equals("virbr0")){
                Enumeration inetAddresses=netint.getInetAddresses();
                if(inetAddresses.hasMoreElements())
                {
                    InetAddress inetAddress=(InetAddress)inetAddresses.nextElement();
                    ip_address=inetAddress.toString();
                    ip_address=ip_address.substring(1,ip_address.length());
                    break;
                }

            }
            }
        }catch(SocketException socketException){
            socketException.printStackTrace();
        }

        Deployer deployer = null;
        String appName = null;

        try {

            {
                //start-database
                ServiceLocator habitat = Globals.getDefaultHabitat();
                ServerContext serverContext = habitat.getService(ServerContext.class);
                String[] startdbArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                        File.separator + "bin" + File.separator + "asadmin" + (OS.isWindows() ? ".bat" : ""), "start-database",
                        "--dbhome" , serverContext.getInstallRoot().getAbsolutePath() + File.separator + "databases","--dbhost",ip_address};
                ProcessExecutor startDatabase = new ProcessExecutor(startdbArgs);

                try {
                    startDatabase.execute();
                } catch (ExecException e) {
                    e.printStackTrace();
                }
            }

            //Create the shared & external services first, as these services will be referenced by the application
            createSharedAndExternalServices(ip_address);

            deployer = glassfish.getDeployer();
            appName = deployer.deploy(archive);

            System.err.println("Deployed [" + appName + "]");
            Assert.assertNotNull(appName);

            CommandRunner commandRunner = glassfish.getCommandRunner();
            CommandResult result = commandRunner.run("list-services");
            System.out.println("\nlist-services command output [ "
                    + result.getOutput() + "]");

            // 3. Access the app to make sure PaaS-ext-shared-service-enable-disable-test app is correctly
            // provisioned.

            String HTTP_PORT = (System.getProperty("http.port") != null) ? System
                    .getProperty("http.port") : "28080";

            String instanceIP = getLBIPAddress(glassfish);

            get("http://" + instanceIP + ":" + HTTP_PORT
                    + "/ext-shared-service-enable-disable-test/list", "Here is a list of animals in the zoo.");

            testSharedAndExternalService();


            // 4. Access the app to make sure PaaS-ext-shared-service-enable-disable-test app is correctly
            // provisioned after running Shared-Services test

            get("http://" + instanceIP + ":" + HTTP_PORT
                    + "/ext-shared-service-enable-disable-test/list", "Here is a list of animals in the zoo.");

            // 5. Undeploy the Zoo catalogue application .

        } finally {
            if (appName != null) {
                deployer.undeploy(appName);
                System.err.println("Undeployed [" + appName + "]");
                deleteSharedAndExternalService();
                {
                    //stop-database
                    ServiceLocator habitat = Globals.getDefaultHabitat();
                    ServerContext serverContext = habitat.getService(ServerContext.class);
                    String[] stopDbArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                            File.separator + "bin" + File.separator + "asadmin" + (OS.isWindows() ? ".bat" : ""), "stop-database","--dbhost",ip_address};
                    ProcessExecutor stopDatabase = new ProcessExecutor(stopDbArgs);

                    try {
                        stopDatabase.execute();
                    } catch (ExecException e) {
                        e.printStackTrace();
                    }
                }
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

    private void createSharedAndExternalServices(String ipAddress_DAS) {

        System.out.println("################### Trying to Create External and Shared Service #######################");
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);

        //Created external service of type JavaEE
        //asadmin create-shared-service --servicetype=JavaEE --configuration min.clustersize=2:max.clustersize=4 --characteristics service-type=JavaEE my-shared-gf-service.
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("create-shared-service", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("servicetype","JavaEE");
        parameterMap.add("characteristics","service-type=JavaEE");
        parameterMap.add("configuration","min.clustersize=2:max.clustersize=4");
        parameterMap.add("DEFAULT","my-shared-gf-service");
        invocation.parameters(parameterMap).execute();

        System.out.println("Created shared service 'my-shared-gf-service' :" + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());


        //Create external service of type Database
        // asadmin create-external-service --servicetype=Database --configuration ip-address=127.0.0.1:databasename=sun-appserv-samples:port=1527:user=APP:password=APP:host=127.0.0.1:classname=org.apache.derby.jdbc.ClientXADataSource:resourcetype=javax.sql.XADataSource my-external-db-service
        invocation = commandRunner.getCommandInvocation("create-external-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("servicetype", "Database");
        parameterMap.add("configuration", "ip-address="+ipAddress_DAS+":databasename=sun-appserv-samples:connectionAttributes=;'create=true':port=1527:user=APP:password=APP:host="+ipAddress_DAS+":classname=org.apache.derby.jdbc.ClientXADataSource:resourcetype=javax.sql.XADataSource");
        //parameterMap.add("configuration", "ip-address=127.0.0.1:databasename=${com.sun.aas.installRoot}/databases/sun-appserv-samples:port=1527:user=APP:password=APP:connectionAttributes=;'create\\=true':host=127.0.0.1:classname=org.apache.derby.jdbc.EmbeddedXADataSource:resourcetype=javax.sql.XADataSource");
        parameterMap.add("DEFAULT", "my-external-db-service");

        invocation.parameters(parameterMap).execute();
        System.out.println("Created external service 'my-external-db-service' :" + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());



        // Create shared service of type LB
        //asadmin create-shared-service --characteristics service-type=LB --configuration http-port=50080:https-port=50081:ssl-enabled=true --servicetype LB my-shared-lb-service
        invocation = commandRunner.getCommandInvocation("create-shared-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("servicetype", "LB");
        parameterMap.add("characteristics", "service-type=LB");
        parameterMap.add("configuration", "http-port=50080:https-port=50081:ssl-enabled=true");
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

            boolean sharedServiceStarted = false;
            List<Map<String, String>> list = (List<Map<String, String>>) report.getExtraProperties().get("list");
            for (Map<String, String> map : list) {
                sharedServiceStarted = false;
                String state = map.get("STATE");
                if ("RUNNING".equalsIgnoreCase(state)) {
                    sharedServiceStarted = true;
                }else{
                    break;
                }
            }
            Assert.assertTrue(sharedServiceStarted);//check if the shared services are started.

    }

    private void testSharedAndExternalService() {

        System.out.println("$$$$$$$$$$$$$ TEST SHARED AND EXTERNAL SERVICES $$$$$$$$$$$$$$$");
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);

       //Disable the application and try stopping  the shared service. Command should succeed
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("disable", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "ext-shared-service-enable-disable-test");
        invocation.parameters(parameterMap).execute();

        System.out.println("Disabled application ext-shared-service-enable-disable-test: " + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());


        invocation = commandRunner.getCommandInvocation("stop-shared-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-lb-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());
        System.out.println("MSG: " + report.getMessage());


        //try deleting a external service when an app is using it. it should 'FAIL'
        invocation = commandRunner.getCommandInvocation("delete-external-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-external-db-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertTrue(report.hasFailures());
        System.out.println("Expected Failure Msg: " + report.getMessage());

        invocation = commandRunner.getCommandInvocation("stop-shared-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-gf-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());
        System.out.println("MSG: " + report.getMessage());


        //List the services and check the status of both the services - it should be 'STOPPED'
        parameterMap = new ParameterMap();
        parameterMap.add("scope", "shared");
        parameterMap.add("output", "service-name,state");
        invocation = commandRunner.getCommandInvocation("list-services", report);
        invocation.parameters(parameterMap).execute();

        boolean sharedServiceStopped = false;
        List<Map<String, String>> list = (List<Map<String, String>>) report.getExtraProperties().get("list");
        for (Map<String, String> map : list) {
        sharedServiceStopped = false;
            String state = map.get("STATE");
            if ("STOPPED".equalsIgnoreCase(state)) {
                sharedServiceStopped = true;
            } else {
                sharedServiceStopped = false;
                break;
            }
        }
        Assert.assertTrue(sharedServiceStopped);//check if the shared services are stopped

        // Start the shared services.
        report = habitat.getService(ActionReport.class);
        invocation = commandRunner.getCommandInvocation("start-shared-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-gf-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());
        System.out.println("MSG: " + report.getMessage());

        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-lb-service");
        invocation.parameters(parameterMap).execute();

        Assert.assertFalse(report.hasFailures());
        System.out.println("MSG: " + report.getMessage());

        //List the services and check the status of both the services - it should be 'STARTED'
        parameterMap = new ParameterMap();
        parameterMap.add("scope", "shared");
        parameterMap.add("output", "service-name,state");
        invocation = commandRunner.getCommandInvocation("list-services", report);
        invocation.parameters(parameterMap).execute();

        boolean sharedServiceStarted = false;
        list = (List<Map<String, String>>) report.getExtraProperties().get("list");
        for (Map<String, String> map : list) {
            sharedServiceStarted = false;
            String state = map.get("STATE");
            if ("STARTED".equalsIgnoreCase(state) || "RUNNING".equalsIgnoreCase(state)) {
                sharedServiceStarted = true;
            } else {
                break;
            }
        }
        Assert.assertTrue(sharedServiceStarted);//check if the shared services are started.

        //Enable the application and try stopping  accessing
        invocation = commandRunner.getCommandInvocation("enable", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "ext-shared-service-enable-disable-test");
        invocation.parameters(parameterMap).execute();

        System.out.println("Enabled application ext-shared-service-enable-disable-test: " + !report.hasFailures());
        Assert.assertFalse(report.hasFailures());

        {
            //List the services and check the status of both the services - it should be 'RUNNING'
            invocation = commandRunner.getCommandInvocation("list-services", report);
            parameterMap = new ParameterMap();
            parameterMap.add("scope", "shared");
            parameterMap.add("output", "service-name,state");
            invocation.parameters(parameterMap).execute();

            sharedServiceStarted = false;
            list = (List<Map<String, String>>) report.getExtraProperties().get("list");
            for (Map<String, String> map : list) {
                sharedServiceStarted = false;
                String state = map.get("STATE");
                if ("RUNNING".equalsIgnoreCase(state)) {
                    sharedServiceStarted = true;
                }else{
                    break;
                }
            }
            Assert.assertTrue(sharedServiceStarted);//check if the shared services are started.
        }

    }

    private void deleteSharedAndExternalService() {
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);
        ActionReport report = habitat.getService(ActionReport.class);

        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation =
        commandRunner.getCommandInvocation("delete-shared-service", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-lb-service");
        invocation.parameters(parameterMap).execute();
        Assert.assertFalse(report.hasFailures());

        invocation = commandRunner.getCommandInvocation("delete-external-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-external-db-service");
        invocation.parameters(parameterMap).execute();
        Assert.assertFalse(report.hasFailures());

        invocation =commandRunner.getCommandInvocation("delete-shared-service", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "my-shared-gf-service");
        invocation.parameters(parameterMap).execute();
        Assert.assertFalse(report.hasFailures());
    }



}
