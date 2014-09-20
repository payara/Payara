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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.resources.config.CustomResource;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import org.glassfish.tests.utils.ConfigApiTest;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

public class DeleteCustomResourceTest extends ConfigApiTest {
    private ServiceLocator habitat;
    private Resources resources;
    private ParameterMap parameters;
    private AdminCommandContext context;
    private CommandRunner cr;

    @Override
    public DomDocument getDocument(ServiceLocator habitat) {
        return new TestDocument(habitat);
    }

    public String getFileName() {
        return "DomainTest";
    }


    @Before
    public void setUp() {
        habitat = getHabitat();
        resources = habitat.<Domain>getService(Domain.class).getResources();
        parameters = new ParameterMap();
        cr = habitat.getService(CommandRunner.class);
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(DeleteCustomResourceTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
    }

    @After
    public void tearDown() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<Resources>() {
            public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                Resource target = null;
                for (Resource resource : param.getResources()) {
                    if (resource instanceof CustomResource) {
                        CustomResource r = (CustomResource) resource;
                        if (r.getJndiName().equals("sample_custom_resource")) {
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
        parameters = new ParameterMap();
    }

    /**
     * Test of execute method, of class DeleteCustomResource.
     * delete-custom-resource sample_custom_resource
     */
    @Test
    public void testExecuteSuccessDefaultTarget() {
        org.glassfish.resources.admin.cli.CreateCustomResource createCommand = habitat.getService(org.glassfish.resources.admin.cli.CreateCustomResource.class);
        assertTrue(createCommand != null);
        parameters.set("restype", "topic");
        parameters.set("factoryclass", "javax.naming.spi.ObjectFactory");
        parameters.set("jndi_name", "sample_custom_resource");
        cr.getCommandInvocation("create-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(createCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());

        parameters = new ParameterMap();
        org.glassfish.resources.admin.cli.DeleteCustomResource deleteCommand = habitat.getService(org.glassfish.resources.admin.cli.DeleteCustomResource.class);
        assertTrue(deleteCommand != null);
        parameters.set("jndi_name", "sample_custom_resource");
        cr.getCommandInvocation("delete-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        boolean isDeleted = true;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof CustomResource) {
                CustomResource jr = (CustomResource) resource;
                if (jr.getJndiName().equals("sample_custom_resource")) {
                    isDeleted = false;
                    logger.fine("CustomResource config bean sample_custom_resource is deleted.");
                    break;
                }
            }
        }
        assertTrue(isDeleted);
        logger.fine("msg: " + context.getActionReport().getMessage());
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        Servers servers = habitat.getService(Servers.class);
        boolean isRefDeleted = true;
        for (Server server : servers.getServer()) {
            if (server.getName().equals(SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)) {
                for (ResourceRef ref : server.getResourceRef()) {
                    if (ref.getRef().equals("sample_custom_resource")) {
                        isRefDeleted = false;
                        break;
                    }
                }
            }
        }
        assertTrue(isRefDeleted);
    }

    /**
     * Test of execute method, of class DeleteCustomResource.
     * delete-custom-resource doesnotexist
     */
    @Test
    public void testExecuteFailDoesNotExist() {
        org.glassfish.resources.admin.cli.DeleteCustomResource deleteCommand = habitat.getService(org.glassfish.resources.admin.cli.DeleteCustomResource.class);
        assertTrue(deleteCommand != null);
        parameters.set("jndi_name", "doesnotexist");
        cr.getCommandInvocation("delete-custom-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

}
