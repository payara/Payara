/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.resources.config.CustomResource;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import org.glassfish.tests.utils.ConfigApiTest;
import org.glassfish.tests.utils.Utils;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.TransactionFailure;


public class CreateCustomResourceTest extends ConfigApiTest {

    ServiceLocator habitat = Utils.instance.getHabitat(this);
    private ParameterMap parameters;
    private Resources resources = null;
    private org.glassfish.resources.admin.cli.CreateCustomResource command = null;
    private AdminCommandContext context = null;
    private CommandRunner cr = null;
    
    @Override
    public DomDocument getDocument(ServiceLocator habitat) {
        return new TestDocument(habitat);
    }

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setUp() {
        parameters = new ParameterMap();
        resources = habitat.<Domain>getService(Domain.class).getResources();
        assertTrue(resources != null);
        command = habitat.getService(org.glassfish.resources.admin.cli.CreateCustomResource.class);
        assertTrue(command != null);
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(CreateCustomResourceTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        cr = habitat.getService(CommandRunner.class);
    }

    @After
    public void tearDown() throws TransactionFailure {
        org.glassfish.resources.admin.cli.DeleteCustomResource deleteCommand = habitat.getService(org.glassfish.resources.admin.cli.DeleteCustomResource.class);
        parameters = new ParameterMap();
        parameters.set("jndi_name", "sample_custom_resource");
        cr.getCommandInvocation("delete-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);
        parameters = new ParameterMap();
        parameters.set("jndi_name", "dupRes");
        cr.getCommandInvocation("delete-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);
    }

    /**
     * Test of execute method, of class CreateCustomResource.
     * asadmin create-custom-resource --restype=topic --factoryclass=javax.naming.spi.ObjectFactory
     * sample_custom_resource
     */
    @Test
    public void testExecuteSuccess() {
        parameters.set("restype", "topic");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "sample_custom_resource");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);

        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                CustomResource r = (CustomResource) resource;
                if (r.getJndiName().equals("sample_custom_resource")) {
                    assertEquals("topic", r.getResType());
                    assertEquals("javax.naming.spi.ObjectFactory", r.getFactoryClass());
                    assertEquals("true", r.getEnabled());
                    isCreated = true;
                    logger.fine("Custom Resource config bean sample_custom_resource is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);

        logger.fine("msg: " + context.getActionReport().getMessage());

        // Check resource-ref created
        Servers servers = habitat.getService(Servers.class);
        boolean isRefCreated = false;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("sample_custom_resource")) {
                        assertEquals("true", ref.getEnabled());
                        isRefCreated = true;
                        break;
                    }
                }
            }
        }
        assertTrue(isRefCreated);
    }

    /**
     * Test of execute method, of class CreateCustomResource.
     * asadmin create-custom-resource --restype=topic --factoryclass=javax.naming.spi.ObjectFactory
     * dupRes
     * asadmin create-custom-resource --restype=topic --factoryclass=javax.naming.spi.ObjectFactory
     * dupRes
     */
    @Test
    public void testExecuteFailDuplicateResource() {
        parameters.set("restype", "topic");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "dupRes");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);

        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                CustomResource jr = (CustomResource) resource;
                if (jr.getJndiName().equals("dupRes")) {
                    isCreated = true;
                    logger.fine("Custom Resource config bean dupRes is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);

        //Try to create a duplicate resource dupRes. Get a new instance of the command.
        org.glassfish.resources.admin.cli.CreateCustomResource command2 = habitat.getService(org.glassfish.resources.admin.cli.CreateCustomResource.class);
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command2);

        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());

        //Check that the 2nd resource was NOT created
        int numDupRes = 0;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                CustomResource jr = (CustomResource) resource;
                if (jr.getJndiName().equals("dupRes")) {
                    numDupRes = numDupRes + 1;
                }
            }
        }
        assertEquals(1, numDupRes);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }


    /**
     * Test of execute method, of class CreateCustomResource.
     * asadmin create-custom-resource --restype=topic --factoryclass=javax.naming.spi.ObjectFactory
     * --enabled=false --description=Administered Object sample_custom_resource
     */
    @Test
    public void testExecuteWithOptionalValuesSet() {
        parameters.set("restype", "topic");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("enabled", "false");
        parameters.set("description", "Administered Object");
        parameters.set("jndi_name", "sample_custom_resource");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);

        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                CustomResource r = (CustomResource) resource;
                if (r.getJndiName().equals("sample_custom_resource")) {
                    assertEquals("topic", r.getResType());
                    assertEquals("javax.naming.spi.ObjectFactory", r.getFactoryClass());
                    //expect enabled for the resource to be true as resource-ref's enabled
                    //would be set to false
                    assertEquals("true", r.getEnabled());
                    assertEquals("Administered Object", r.getDescription());
                    isCreated = true;
                    logger.fine("Custom Resource config bean sample_custom_resource is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateCustomResource.
     * asadmin create-custom-resource --factoryclass=javax.naming.spi.ObjectFactory
     * sample_custom_resource
     */
    @Test
    public void testExecuteFailInvalidResType() throws TransactionFailure {
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "sample_custom_resource");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);

        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
    }


}
