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

package org.glassfish.tests.paas.basicsd;

import junit.framework.Assert;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.internal.api.Globals;
import org.glassfish.paas.orchestrator.provisioning.util.JSONUtil;
import org.junit.Test;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

/**
 * @author Sandhya Kripalani K
 */

public class BasicServiceDiscoveryPaaSTest {


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
                + "/target/basic_sd_paas_sample.war"); // TODO :: use mvn apis
        // to get the archive
        // location.
        Assert.assertTrue(archive.exists());

        Deployer deployer = null;
        String appName = null;
        try {

            deployer = glassfish.getDeployer();
            appName = deployer.deploy(archive);

            System.err.println("Deployed [" + appName + "]");
            Assert.assertNotNull(appName);

            CommandRunner commandRunner = glassfish.getCommandRunner();
            CommandResult result = commandRunner.run("list-services");
            System.out.println("\nlist-services command output [ "
                    + result.getOutput() + "]");

            // 3. Access the app to make sure PaaS app is correctly provisioned.

            String HTTP_PORT = (System.getProperty("http.port") != null) ? System
                    .getProperty("http.port") : "28080";

            testGetServiceMetadata(archive);
            testGenerateGFDeploymentPlan(archive);

            // 4. Undeploy the PaaS-DB application using undeploy.
        } finally {
            if (appName != null) {
                deployer.undeploy(appName);
                System.err.println("Undeployed [" + appName + "]");
                try {
                    boolean undeployClean = false;
                    CommandResult commandResult = glassfish.getCommandRunner()
                            .run("list-services");
                    if (commandResult.getOutput().contains("Nothing to list.")) {
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


     //Test for CLI : '_get-service-description''
    private void testGetServiceDescription(String appName, String serviceName) {

        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);

        ActionReport report = habitat.getService(ActionReport.class);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("appname", appName);
        parameterMap.add("DEFAULT", serviceName);

        boolean testPassed = false;
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("_get-service-description", report);
        invocation.parameters(parameterMap).execute();


        Map<String, Object> SDMap = (Map<String, Object>) report.getExtraProperties().get("list");
        if (serviceName.equalsIgnoreCase((String) SDMap.get("name"))) {
            if ("lazy".equalsIgnoreCase((String) SDMap.get("init-type"))) {
                Map<String, String> serviceCharacteristicsMap = (Map<String, String>) SDMap.get("characteristics");
                if ("JavaEE".equals(serviceCharacteristicsMap.get("service-type"))) {
                    Map<String, String> serviceConfigurationsMap = (Map<String, String>) SDMap.get("configurations");
                    String minclustersize = serviceConfigurationsMap.get("min.clustersize");
                    String maxclustersize = serviceConfigurationsMap.get("max.clustersize");
                    if (Integer.parseInt(minclustersize) == 1 && Integer.parseInt(maxclustersize) == 2) {
                        testPassed = true;
                    }
                }
            }
        }

        System.out.println("CLI 'get-service-description' test passed? :: " + testPassed);
        Assert.assertTrue(testPassed);
    }


    //Test for CLI : '_get-service-metadata''
    /*
        The war file contains the following service definition and service reference
          1.In glassfish-services.xml
        <glassfish-services>
            <service-description name="basic-db" init-type="lazy">
                <characteristics><characteristic name="service-type" value="JavaEE"/></characteristics>
                <configurations>
                    <configuration name="min.clustersize" value="1"/>
                    <configuration name="max.clustersize" value="2"/>
                </configurations>
            </service-description>
         </glassfish-services>
         <service-description name="db-service" init-type="lazy">
        <characteristics>
            <characteristic name="service-type" value="Database"/>
        </characteristics>
        <configurations>
            <configuration name="database.name" value=""/>
            <configuration name="database.init.sql" value=""/>
        </configurations>
    </service-description>

         2.In web.xml
         <resource-ref>
                <res-ref-name>jdbc/__basic_db_paas_sample</res-ref-name>
                <res-type>javax.sql.DataSource</res-type>
         </resource-ref>
     */

    private void testGetServiceMetadata(File archive) {

        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);

        ActionReport report = habitat.getService(ActionReport.class);
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("_get-service-metadata", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", System.getProperty("basedir") + "/target/basic_sd_paas_sample.war");
        boolean testPassed = false;
        invocation.parameters(parameterMap).execute();
        testPassed=!report.hasFailures();

        String appName = "basic_sd_paas_sample";
        String serviceName = "basic-sd";

        testGetServiceDescription(appName, serviceName);


        List<Map<String, Object>> serviceDescList = (List<Map<String, Object>>) report.getExtraProperties().get("list");

        Map<String, Object> serviceDescMap = serviceDescList.get(1);
        serviceName = (String) serviceDescMap.get("name");
        String init_type = (String) serviceDescMap.get("init-type");
        Map<String, String> svcCharacteristicMap = (Map<String, String>) serviceDescMap.get("characteristics");
        String serviceType = (String) svcCharacteristicMap.get("service-type");

        Map<String, String> svcConfigurationMap = (Map<String, String>) serviceDescMap.get("configurations");

        if (serviceName.equalsIgnoreCase("db-service")) {
            if ("lazy".equalsIgnoreCase(init_type)) {
                if ("Database".equals(serviceType)) {
                    Map<String, String> serviceConfigurationsMap = (Map<String, String>) serviceDescMap.get("configurations");
                    if (serviceConfigurationsMap.containsKey("database.init.sql") && serviceConfigurationsMap.containsKey("database.name")) {
                        testPassed = true;
                    }
                }
            }
        }

        System.out.println("CLI 'get-service-metadata' test passed? :: " + testPassed);
        Assert.assertTrue(testPassed);
    }


    //  Test CLI '_generate-glassfish-services-deployment-plan'

    private void testGenerateGFDeploymentPlan(File archive) {

        //Execute the '_get-service-metadata' command and obtain the SDs. Change the configuration of one of the SD.
        ServiceLocator habitat = Globals.getDefaultHabitat();
        org.glassfish.api.admin.CommandRunner commandRunner = habitat.getService(org.glassfish.api.admin.CommandRunner.class);

        ActionReport report = habitat.getService(ActionReport.class);
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("_get-service-metadata", report);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", System.getProperty("basedir") + "/target/basic_sd_paas_sample.war");
        boolean testPassed = false;
        invocation.parameters(parameterMap).execute();

        String newDBname = "UserDB";
        String newSQLFilename = "User.sql";
        String DATABASE_NAME_PROP = "database.name";
        String SQL_FILE_PROP = "database.init.sql";

        List<Map<String, Object>> serviceDescList = (List<Map<String, Object>>) report.getExtraProperties().get("list");
        for (Map<String, Object> serviceDescMap : serviceDescList) {
            if ("db-service".equals(serviceDescMap.get("name"))) {
                int index = serviceDescList.indexOf(serviceDescMap);
                Map<String, String> configurations = (Map<String, String>) serviceDescMap.get("configurations");
                configurations.put(DATABASE_NAME_PROP, newDBname);
                configurations.put(SQL_FILE_PROP, newSQLFilename);
                serviceDescMap.put("configurations", configurations);
                break;
            }
        }

        String modifiedServiceDesc = JSONUtil.javaToJSON(serviceDescList, 10);
        //System.out.println("Modified service description:: "+modifiedServiceDesc);
        parameterMap = new ParameterMap();
        parameterMap.add("archive", System.getProperty("basedir") + "/target/basic_sd_paas_sample.war");
        parameterMap.add("modifiedServiceDesc", modifiedServiceDesc);
        report = habitat.getService(ActionReport.class);
        invocation = commandRunner.getCommandInvocation("_generate-glassfish-services-deployment-plan", report);
        invocation.parameters(parameterMap).execute();

        String jarFilePath = (String) report.getExtraProperties().get("deployment-plan-file-path");

        report = habitat.getService(ActionReport.class);
        invocation = commandRunner.getCommandInvocation("undeploy", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", "basic_sd_paas_sample");
        invocation.parameters(parameterMap).execute();
        Assert.assertFalse(report.hasFailures());

        invocation = commandRunner.getCommandInvocation("deploy", report);
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", System.getProperty("basedir") + "/target/basic_sd_paas_sample.war");
        parameterMap.add("deploymentplan", jarFilePath);
        invocation.parameters(parameterMap).execute();

        System.out.println("App deployed with new plan ::" + !report.hasFailures());

        String appname = "basic_sd_paas_sample";
        String servicename = "db-service";
        parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", servicename);
        parameterMap.add("appname", appname);
        invocation = commandRunner.getCommandInvocation("_get-service-description", report);
        invocation.parameters(parameterMap).execute();

        Map<String, Object> SDMap = (Map<String, Object>) report.getExtraProperties().get("list");

        Map<String, String> serviceConfigurationsMap = (Map<String, String>) SDMap.get("configurations");
        String sql_filename = serviceConfigurationsMap.get(SQL_FILE_PROP);
        String db_name = serviceConfigurationsMap.get(DATABASE_NAME_PROP);
        if (newSQLFilename.equals(sql_filename) && newDBname.equals(db_name)) {
            testPassed = true;
        }


        System.out.println("CLI _generate-glassfish-services-deployment-plan passed " + testPassed);
        Assert.assertTrue(testPassed);

    }


}
