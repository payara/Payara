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

package org.glassfish.tests.paas.enable_disable_test;

import junit.framework.Assert;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.Deployer;
import org.glassfish.resources.util.ResourceUtil;
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
 * @author Shalini M
 */

public class EnableDisableTest {

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
				+ "/target/enable-disable-sample.war"); // TODO :: use
																// mvn apis to
																// get the
																// archive
																// location.
		Assert.assertTrue(archive.exists());

		Deployer deployer = null;
		String appName = null;
		try {
			deployer = glassfish.getDeployer();
			appName = deployer.deploy(archive);

			System.err.println("Deployed [" + appName + "]");
			Assert.assertNotNull(appName);

            CommandResult result = null;
			CommandRunner commandRunner = glassfish.getCommandRunner();
            {
                result = commandRunner.run("list-services","appname="+appName, "output=STATE");
                System.out.println("\nlist-services command output [ "
                        + result.getOutput() + "]");

                boolean notRunning = result.getOutput().toLowerCase().contains("notrunning");
                Assert.assertTrue(!notRunning);
                boolean stopped =result.getOutput().toLowerCase().contains("stopped");
                Assert.assertTrue(!stopped);
            }

			// 3. Access the app to make sure PaaS app is correctly provisioned.
			String HTTP_PORT = (System.getProperty("http.port") != null) ? System
					.getProperty("http.port") : "28080";

			get("http://localhost:" + HTTP_PORT
					+ "/enable-disable-sample/EnableDisableServlet",
					"Customer ID");

            {
                result = commandRunner.run("disable", appName);
                System.out.println("disable "+ appName + " output : " + result.getOutput());
                System.out.println("disable "+ appName + " status : " + result.getExitStatus());
                Assert.assertEquals(result.getExitStatus(), CommandResult.ExitStatus.SUCCESS);

                result = commandRunner.run("list-services","appname="+appName, "output=STATE");
                System.out.println("list-services --appname=["+appName+"] : status : " + result.getExitStatus());
                System.out.println("\nlist-services command output [ "
                        + result.getOutput() + "]");
                boolean notRunning = result.getOutput().toLowerCase().contains("notrunning");
                boolean stopped =result.getOutput().toLowerCase().contains("stopped");
                Assert.assertTrue(stopped || notRunning);
            }

            {
                result = commandRunner.run("enable", appName);
                System.out.println("enable "+ appName + " output : " + result.getOutput());
                System.out.println("enable "+ appName + " status : " + result.getExitStatus());
                Assert.assertEquals(result.getExitStatus(), CommandResult.ExitStatus.SUCCESS);

                result = commandRunner.run("list-services","appname="+appName, "output=STATE");
                System.out.println("list-services --appname=["+appName+"] : status : " + result.getExitStatus());
                System.out.println("\nlist-services command output [ "
                        + result.getOutput() + "]");
                boolean notRunning = result.getOutput().toLowerCase().contains("notrunning");
                Assert.assertTrue(!notRunning);
                boolean stopped =result.getOutput().toLowerCase().contains("stopped");
                Assert.assertTrue(!stopped);
            }

            get("http://localhost:" + HTTP_PORT
                    + "/enable-disable-sample/EnableDisableServlet",
                    "Customer ID");

			// 4. Undeploy the PaaS application .
		} finally {
			if (appName != null) {
				deployer.undeploy(appName);
				System.out.println("Destroying the resources created");
				System.err.println("Undeployed [" + appName + "]");
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

}
