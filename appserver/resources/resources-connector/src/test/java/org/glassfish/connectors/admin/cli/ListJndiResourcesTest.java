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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.DomDocument;

import java.util.ArrayList;
import java.util.List;
import org.glassfish.tests.utils.ConfigApiTest;

public class ListJndiResourcesTest extends ConfigApiTest {

    private ServiceLocator habitat;
    private int origNum = 0;
    private ParameterMap parameters;
    AdminCommandContext context;
    CommandRunner cr;

    public DomDocument getDocument(ServiceLocator habitat) {
        return new TestDocument(habitat);
    }

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setUp() {
        habitat = getHabitat();
        cr = habitat.getService(CommandRunner.class);
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(ListJndiResourcesTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        parameters = new ParameterMap();
        Resources resources = habitat.<Domain>getService(Domain.class).getResources();
        for (Resource resource : resources.getResources()) {
            if (resource instanceof org.glassfish.resources.config.ExternalJndiResource) {
                origNum = origNum + 1;
            }
        }
    }

    @After
    public void tearDown() {

    }

    /**
     * Test of execute method, of class ListJndiResources.
     * list-jndi-resources
     */

    @Test
    public void testExecuteSuccessListOriginal() {
        org.glassfish.resources.admin.cli.ListJndiResources listCommand = habitat.getService(org.glassfish.resources.admin.cli.ListJndiResources.class);
        cr.getCommandInvocation("list-jndi-resources", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);
        List<ActionReport.MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        if (origNum == 0) {
            //Nothing to list.
        } else {
            assertEquals(origNum, list.size());
        }
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }

/**
     * Test of execute method, of class ListJndiResources.
     * create-jndi-resource ---restype=topic --factoryclass=javax.naming.spi.ObjectFactory --jndilookupname=sample_jndi
     * resource
     * list-jndi-resources
     */

    @Test
    public void testExecuteSuccessListResource() {
        createJndiResource();
        parameters = new ParameterMap();
        org.glassfish.resources.admin.cli.ListJndiResources listCommand = habitat.getService(org.glassfish.resources.admin.cli.ListJndiResources.class);
        cr.getCommandInvocation("list-jndi-resources", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);
        List<ActionReport.MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum + 1, list.size());
        List<String> listStr = new ArrayList<String>();
        for (ActionReport.MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertTrue(listStr.contains("resource"));
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        deleteJndiResource();
    }


    private void createJndiResource() {
        parameters = new ParameterMap();
        parameters.set("restype", "topic");
        parameters.set("jndilookupname", "sample_jndi");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "resource");
        org.glassfish.resources.admin.cli.CreateJndiResource createCommand = habitat.getService(org.glassfish.resources.admin.cli.CreateJndiResource.class);
        cr.getCommandInvocation("create-jndi-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(createCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }

    private void deleteJndiResource() {
        parameters = new ParameterMap();
        parameters.set("jndi_name", "resource");
        org.glassfish.resources.admin.cli.DeleteJndiResource deleteCommand = habitat.getService(org.glassfish.resources.admin.cli.DeleteJndiResource.class);
        cr.getCommandInvocation("delete-jndi-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }
    /**
     * Test of execute method, of class ListJndiResource.
     * delete-jndi-resource resource
     * list-jndi-resources
     */
    @Test
    public void testExecuteSuccessListNoResource() {

        createJndiResource();

        parameters = new ParameterMap();
        org.glassfish.resources.admin.cli.ListJndiResources listCommand = habitat.getService(org.glassfish.resources.admin.cli.ListJndiResources.class);
        cr.getCommandInvocation("list-jndi-resources", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);

        List<ActionReport.MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum + 1, list.size());
        origNum = origNum + 1; //as we newly created a resource after test "setup".


        deleteJndiResource();

        ParameterMap parameters = new ParameterMap();
        listCommand = habitat.getService(org.glassfish.resources.admin.cli.ListJndiResources.class);
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(ListJndiResourcesTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        cr.getCommandInvocation("list-jndi-resources", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);

        list = context.getActionReport().getTopMessagePart().getChildren();

        if ((origNum - 1) == 0) {
            //Nothing to list.
        } else {
            assertEquals(origNum - 1, list.size());
        }
        List<String> listStr = new ArrayList<String>();
        for (ActionReport.MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertFalse(listStr.contains("resource"));
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }
}
