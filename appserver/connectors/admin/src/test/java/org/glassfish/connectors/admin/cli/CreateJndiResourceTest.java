/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.tests.utils.ConfigApiTest;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;


public class CreateJndiResourceTest extends ConfigApiTest {

    private Habitat habitat;
    private Resources resources;
    private ParameterMap parameters;
    private AdminCommandContext context;
    private CommandRunner cr;
    private Server server;

    public DomDocument getDocument(Habitat habitat) {
        return new TestDocument(habitat);
    }

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setUp() {
        habitat = getHabitat();
        resources = habitat.getComponent(Domain.class).getResources();
        server = habitat.getComponent(Server.class);
        parameters = new ParameterMap();
        context = new AdminCommandContext(
                LogDomains.getLogger(CreateJndiResourceTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        cr = habitat.getComponent(CommandRunner.class);
    }

    @After
    public void tearDown() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<Resources>() {
            public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                Resource target = null;
                for (Resource resource : param.getResources()) {
                    if (resource instanceof BindableResource) {
                        BindableResource r = (BindableResource) resource;
                        if (r.getJndiName().equals("sample_jndi_resource") ||
                                r.getJndiName().equals("dupRes")) {
                            target = resource;
                            break;
                        }
                    }
                }
                if (target != null) {
                    param.getResources().remove(target);
                }
                return null;
            }
        }, resources);

/*
        ParameterMap refParameters = new ParameterMap();

        refParameters.set("reference_name", "sample_jndi_resource");
        CreateJndiResource command = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("delete-resource-ref", context.getActionReport()).parameters(refParameters).execute(command);

        refParameters.set("reference_name", "dupRes");
        cr.getCommandInvocation("delete-resource-ref", context.getActionReport()).parameters(refParameters).execute(command);
*/

    }

    /**
     * Test of execute method, of class CreateJndiResource.
     * asadmin create-jndi-resource --restype=queue --factoryclass=sampleClass --jndilookupname=sample_jndi
     * sample_jndi_resource
     */
    @Test
    public void testExecuteSuccess() {
        parameters.set("jndilookupname", "sample_jndi");
        parameters.set("restype", "queue");
        parameters.set("factoryclass", "sampleClass");
        parameters.set("jndi_name", "sample_jndi_resource");
        CreateJndiResource command = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command);

        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof ExternalJndiResource) {
                ExternalJndiResource r = (ExternalJndiResource) resource;
                if (r.getJndiName().equals("sample_jndi_resource")) {
                    assertEquals("queue", r.getResType());
                    assertEquals("sample_jndi", r.getJndiLookupName());
                    assertEquals("sampleClass", r.getFactoryClass());
                    assertEquals("true", r.getEnabled());
                    isCreated = true;
                    logger.fine("Jndi Resource config bean sample_jndi_resource is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
        Servers servers = habitat.getComponent(Servers.class);
        boolean isRefCreated = false;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("sample_jndi_resource")) {
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
     * Test of execute method, of class CreateJndiResource.
     * asadmin create-jndi-resource --restype=queue --factoryclass=sampleClass --jndilookupname=sample_jndi
     * dupRes
     * asadmin create-jndi-resource --restype=queue --factoryclass=sampleClass --jndilookupname=sample_jndi
     * dupRes
     */
    @Test
    public void testExecuteFailDuplicateResource() {
        parameters.set("jndilookupname", "sample_jndi");
        parameters.set("restype", "queue");
        parameters.set("factoryclass", "sampleClass");
        parameters.set("jndi_name", "dupRes");
        CreateJndiResource command1 = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command1);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof BindableResource) {
                BindableResource jr = (BindableResource) resource;
                if (jr.getJndiName().equals("dupRes")) {
                    isCreated = true;
                    logger.fine("Jndi Resource config bean dupRes is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);

        CreateJndiResource command2 = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command2);
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        int numDupRes = 0;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof BindableResource) {
                BindableResource jr = (BindableResource) resource;
                if (jr.getJndiName().equals("dupRes")) {
                    numDupRes = numDupRes + 1;
                }
            }
        }
        assertEquals(1, numDupRes);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateJndiResource.
     * asadmin create-jndi-resource --restype=queue --factoryclass=sampleClass --jndilookupname=sample_jndi
     * --enabled=false --description=External JNDI Resource
     * sample_jndi_resource
     */
    @Test
    public void testExecuteWithOptionalValuesSet() {
        parameters.set("jndilookupname", "sample_jndi");
        parameters.set("restype", "queue");
        parameters.set("factoryclass", "sampleClass");
        parameters.set("enabled", "false");
        parameters.set("description", "External JNDI Resource");
        parameters.set("jndi_name", "sample_jndi_resource");
        CreateJndiResource command = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof ExternalJndiResource) {
                ExternalJndiResource r = (ExternalJndiResource) resource;
                if (r.getJndiName().equals("sample_jndi_resource")) {
                    assertEquals("queue", r.getResType());
                    assertEquals("sampleClass", r.getFactoryClass());
                    assertEquals("sample_jndi", r.getJndiLookupName());
                    //expect enabled for the resource to be true as resource-ref's enabled
                    //would be set to false
                    assertEquals("true", r.getEnabled());
                    assertEquals("External JNDI Resource", r.getDescription());
                    isCreated = true;
                    logger.fine("Jndi Resource config bean sample_jndi_resource is created.");
                    break;
                }
            }
        }
        assertTrue(isCreated);
/*
        ResourceRef ref = server.getResourceRef("sample_jndi_resource");
        assertTrue(ref != null);
        assertEquals("false", ref.getEnabled());
*/

        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateJndiResource.
     * asadmin create-jndi-resource --factoryclass=sampleClass --jndilookupname=sample_jndi
     * sample_jndi_resource
     */
    @Test
    public void testExecuteFailInvalidResType() {
        parameters.set("factoryclass", "sampleClass");
        parameters.set("jndilookupname", "sample_jndi");
        parameters.set("jndi_name", "sample_jndi_resource");
        CreateJndiResource command = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command);
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
    }

    /**
     * Test of execute method, of class CreateJndiResource.
     * asadmin create-jndi-resource --factoryclass=sampleClass --restype=queue
     * sample_jndi_resource
     */
    @Test
    public void testExecuteFailInvalidJndiLookupName() {
        parameters.set("factoryclass", "sampleClass");
        parameters.set("restype", "queue");
        parameters.set("jndi_name", "sample_jndi_resource");
        CreateJndiResource command = habitat.getComponent(CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport()).parameters(parameters).execute(command);
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
    }
}
