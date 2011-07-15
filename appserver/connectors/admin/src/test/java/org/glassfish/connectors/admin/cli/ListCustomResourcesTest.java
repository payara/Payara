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

import com.sun.enterprise.config.serverbeans.CustomResource;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.tests.utils.ConfigApiTest;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.DomDocument;

import java.util.List;

public class ListCustomResourcesTest extends ConfigApiTest {

    private Habitat habitat = getHabitat();
    private AdminCommandContext context ;
    private CommandRunner cr;
    private int origNum;
    private ParameterMap parameters;


    public DomDocument getDocument(Habitat habitat) {
        return new TestDocument(habitat);
    }

    public String getFileName() {
        return "DomainTest";
    }

    @Before
    public void setUp() {
        parameters = new ParameterMap();
        cr = habitat.getComponent(CommandRunner.class);
        context = new AdminCommandContext(
                LogDomains.getLogger(ListCustomResourcesTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        Resources resources = habitat.getComponent(Domain.class).getResources();
        assertTrue(resources != null);
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                origNum = origNum + 1;
            }
        }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of execute method, of class ListCustomResources.
     * list-custom-resources
     */
    @Test
    public void testExecuteSuccessListOriginal() {
        ListCustomResources listCommand = habitat.getComponent(ListCustomResources.class);
        cr.getCommandInvocation("list-custom-resources", context.getActionReport()).parameters(parameters).execute(listCommand);
        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        if (origNum == 0) {
            //Nothing to list.
        } else {
            assertEquals(origNum, list.size());
        }
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }

    /**
     * Test of execute method, of class ListCustomResources.
     * create-custom-resource ---restype=topic --factoryclass=javax.naming.spi.ObjectFactory
     * Resource1
     * list-custom-resources
     */
    @Test
    public void testExecuteSuccessListResource1() {

        CreateCustomResource createCommand = habitat.getComponent(CreateCustomResource.class);
        assertTrue(createCommand != null);
        parameters.set("restype", "topic");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "custom_resource1");
        cr.getCommandInvocation("create-custom-resource", context.getActionReport()).parameters(parameters).execute(createCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        parameters = new ParameterMap();
        ListCustomResources listCommand = habitat.getComponent(ListCustomResources.class);
        cr.getCommandInvocation("list-custom-resources", context.getActionReport()).parameters(parameters).execute(listCommand);

        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum + 1, list.size());
        List<String> listStr = new java.util.ArrayList<String>();
        for (MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertTrue(listStr.contains("custom_resource1"));
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }

    /**
     * Test of execute method, of class ListCustomResources.
     * delete-custom-resource Resource1
     * list-Custom-resources
     */
    @Test
    public void testExecuteSuccessListNoResource1() {

        DeleteCustomResource deleteCommand = habitat.getComponent(DeleteCustomResource.class);
        assertTrue(deleteCommand != null);
        parameters.set("jndi_name", "custom_resource1");
        cr.getCommandInvocation("delete-custom-resource", context.getActionReport()).parameters(parameters).execute(deleteCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        parameters = new ParameterMap();
        ListCustomResources listCommand = habitat.getComponent(ListCustomResources.class);
        cr.getCommandInvocation("list-custom-resources", context.getActionReport()).parameters(parameters).execute(listCommand);

        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        if ((origNum - 1) == 0) {
            //Nothing to list.
        } else {
            assertEquals(origNum - 1, list.size());
        }
        List<String> listStr = new java.util.ArrayList<String>();
        for (MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertFalse(listStr.contains("custom_resource1"));
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
    }
}
