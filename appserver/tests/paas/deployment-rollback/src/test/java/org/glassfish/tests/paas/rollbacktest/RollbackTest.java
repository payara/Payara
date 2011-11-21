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

package org.glassfish.tests.paas.rollbacktest;

import junit.framework.Assert;
import org.glassfish.embeddable.*;
import org.glassfish.paas.orchestrator.provisioning.util.FailureInducer;
import org.glassfish.paas.orchestrator.state.*;
import org.junit.Test;

import java.io.File;
import java.io.PrintStream;

public class RollbackTest {

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
				+ "/target/rollback-test-sample.war"); // TODO :: use mvn apis
														// to get the archive
														// location.
        Assert.assertTrue(archive.exists());

        Class[] states = {ServiceDependencyDiscoveryState.class, ProvisioningState.class, PreDeployAssociationState.class,
                PostDeployAssociationState.class, DeploymentCompletionState.class };

        for(Class state : states){
            String appName = state.getSimpleName();
            testRollback(glassfish, archive, appName, state);
        }
	}

    private void testRollback(GlassFish glassfish, File archive, String appName, Class state) throws GlassFishException {

        try {

            CommandRunner commandRunner = glassfish.getCommandRunner();
            try {
                System.out.println("Setting failure inducer with state : " + state.getSimpleName());
                FailureInducer.setFailureState(state);
                System.out.println("Archive absolute path : " + archive.getAbsolutePath());
                CommandResult result = commandRunner.run("deploy", "--name=" + appName, archive.getAbsolutePath());
                System.out.println("Deploy command result : " + result.getOutput());
                System.out.println("Deploy command exit-status : " + result.getExitStatus());
                System.out.println("Deploy command failure-cause : " + result.getFailureCause());

                validateResult(appName, commandRunner);

            } catch (Exception gfe) {
                System.out.println("Failure while deploying application [" + archive.getName() + "] " + gfe.getLocalizedMessage());
                gfe.printStackTrace();
                validateResult(appName, commandRunner);
            }
        } finally {
        }
    }

    private void validateResult(String appName, CommandRunner commandRunner) {
        CommandResult result;
        result = commandRunner.run("list-services", "--appname="+ appName );
        System.out.println("list-services --appname=["+appName+"] : status : " + result.getExitStatus());
        Assert.assertEquals(result.getExitStatus(), CommandResult.ExitStatus.FAILURE);

        result = commandRunner.run("list-services");
        System.out.println("list-services : status : " + result.getExitStatus());
        boolean containsNothingToList = result.getOutput().contains("Nothing to list");
        Assert.assertTrue(containsNothingToList);

        result = commandRunner.run("list-applications", "domain");
        boolean applicationFound = result.getOutput().contains(appName);
        Assert.assertTrue(!applicationFound);
    }
}
