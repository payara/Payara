/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.embedded.app;

import static org.glassfish.embeddable.CommandResult.ExitStatus.SUCCESS;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.annotation.Resource;
import javax.naming.InitialContext;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that Payara 'asadmin' commands can be executed against an Embedded Payara instance using
 * the CommandRunner resource. The CommandRunner is exercised by performing commands to create a
 * Payara JDBC connection pool and a JDBC resource. The JDBC connection pool is tested by using the
 * CommandRunner to perform a ping. The JDBC resource is verified by performing a JNDI lookup.
 *
 * @author magnus.smith
 */
@RunWith(Arquillian.class)
public class AsAdminCommandTestCase {

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        return create(WebArchive.class)
                   .addClasses(
                       NoInterfaceEJB.class, 
                       NameProvider.class)
                   .addAsWebInfResource(INSTANCE, "beans.xml");
    }

    @Resource(mappedName = "org.glassfish.embeddable.CommandRunner")
    private CommandRunner commandRunner;

    @Test
    public void shouldBeAbleToIssueAsAdminCommand() throws Exception {
        assertNotNull("Verify that the asadmin CommandRunner resource is available", commandRunner);

        CommandResult result = commandRunner.run("create-jdbc-connection-pool", "--datasourceclassname=org.apache.derby.jdbc.EmbeddedXADataSource",
                "--restype=javax.sql.XADataSource",
                "--property=portNumber=1527:password=APP:user=APP" + ":serverName=localhost:databaseName=my_database" + ":connectionAttributes=create\\=true",
                "my_derby_pool");

        assertEquals("Verify 'create-jdbc-connection-pool' asadmin command", SUCCESS, result.getExitStatus());

        result = commandRunner.run("create-jdbc-resource", "--connectionpoolid", "my_derby_pool", "jdbc/my_database");

        assertEquals("Verify 'create-jdbc-resource' asadmin command", SUCCESS, result.getExitStatus());

        result = commandRunner.run("ping-connection-pool", "my_derby_pool");

        assertEquals("Verify asadmin command 'ping-connection-pool'", SUCCESS, result.getExitStatus());

        assertNotNull(new InitialContext().lookup("jdbc/my_database"));
    }
}
