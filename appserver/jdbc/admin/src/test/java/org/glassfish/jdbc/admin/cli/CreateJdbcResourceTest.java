/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbc.admin.cli;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.logging.LogDomains;

import java.beans.PropertyVetoException;

import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jdbc.config.JdbcResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import org.glassfish.api.ActionReport;
import org.glassfish.tests.utils.Utils;
import org.glassfish.tests.utils.ConfigApiTest;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Jennifer
 */
//@Ignore // temporarily disabled
public class CreateJdbcResourceTest extends ConfigApiTest {
    // Get Resources config bean
    ServiceLocator habitat = Utils.instance.getHabitat(this);
    private Resources resources = habitat.<Domain>getService(Domain.class).getResources();
    private CreateJdbcResource command = null;
    private ParameterMap parameters = new ParameterMap();
    private AdminCommandContext context = null;
    private CommandRunner cr = null;
    
    @Override
    public DomDocument getDocument(ServiceLocator habitat) {

        return new TestDocument(habitat);
    }    

    /**
     * Returns the DomainTest file name without the .xml extension to load the test configuration
     * from.
     *
     * @return the configuration file name
     */
    public String getFileName() {
        return "DomainTest";
    }    
    
    @Before
    public void setUp() {
        assertTrue(resources!=null);
        
        // Get an instance of the CreateJdbcResource command
        command = habitat.getService(CreateJdbcResource.class);
        assertTrue(command!=null);
        
        // Set the options and operand to pass to the command
        parameters.set("connectionpoolid", "DerbyPool");
        parameters.set("enabled", "true");
        parameters.set("description", "my resource");
        parameters.set("DEFAULT", "jdbc/foo");
        
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(CreateJdbcResourceTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        
        cr = habitat.getService(CommandRunner.class);
    }

    @After
    public void tearDown() throws TransactionFailure {
       // Delete the created resource
       ConfigSupport.apply(new SingleConfigCode<Resources>() {
            public Object run(Resources param) throws PropertyVetoException, TransactionFailure {
                Resource target = null;
                // TODO: this causes NoSuchElementException but really
                // it should have caused ConcurrentModificationException, because the iteration
                // and removal runs at the same time.
                for (Resource resource : param.getResources()) {
                    if (resource instanceof JdbcResource) {
                        JdbcResource jr = (JdbcResource)resource;
                        if (jr.getJndiName().equals("jdbc/foo")||
                            jr.getJndiName().equals("dupRes")||
                            jr.getJndiName().equals("jdbc/sun")||
                            jr.getJndiName().equals("jdbc/alldefaults")||
                            jr.getJndiName().equals("jdbc/junk")) {
                            target = resource;
                            break;
                        }
                    }
                }
                if (target!=null) {
                    param.getResources().remove(target);
                }
                return null;
            }                        
        }, resources);

        parameters = new ParameterMap();
    }
    
    /**
     * Test of execute method, of class CreateJdbcResource.
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool --enabled=true
     *        --description "my resource" jdbc/foo
     */
    @Test
    public void testExecuteSuccess() {
        // Set operand
        parameters.set("DEFAULT", "jdbc/foo");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/foo")) {
                    assertEquals("DerbyPool", jr.getPoolName());
                    assertEquals("true", jr.getEnabled());
                    assertEquals("my resource", jr.getDescription());
                    isCreated = true;
                    logger.fine("JdbcResource config bean jdbc/foo is created.");
                    continue;
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
                    if (ref.getRef().equals("jdbc/foo")) {
                        assertEquals("true", ref.getEnabled());
                        isRefCreated = true;
                        continue;
                    }
                }
            }
        }
        assertTrue(isRefCreated);
    }
    
    /**
     * Test of execute method, of class CreateJdbcResource.
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool jdbc/alldefaults
     */
    @Test
    public void testExecuteSuccessDefaultValues() {
        // Only pass the required option and operand
        parameters = new ParameterMap();
        parameters.set("connectionpoolid", "DerbyPool");
        parameters.set("DEFAULT", "jdbc/alldefaults");
        

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/alldefaults")) {
                    assertEquals("DerbyPool", jr.getPoolName());
                    assertEquals("true", jr.getEnabled());
                    assertNull(jr.getDescription());
                    isCreated = true;
                    logger.fine("JdbcResource config bean jdbc/alldefaults is created.");
                    continue;
                }
            }
        }       
        assertTrue(isCreated);
        
        logger.fine("msg: " + context.getActionReport().getMessage());    
    }

    /**
     * Test of execute method, of class CreateJdbcResource.
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool --enabled=true 
     *         --description "my resource" dupRes
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool --enabled=true 
     *         --description "my resource" dupRes
     */
    @Test
    public void testExecuteFailDuplicateResource() {
        // Set operand
        parameters.set("DEFAULT", "dupRes");

        //Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);

        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        //Check that the resource was created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("dupRes")) {
                    isCreated = true;
                    logger.fine("JdbcResource config bean dupRes is created.");
                    continue;
                }
            }
        }       
        assertTrue(isCreated);
        
        //Try to create a duplicate resource dupRes. Get a new instance of the command.
        CreateJdbcResource command2 = habitat.getService(CreateJdbcResource.class);
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command2);
        
        // Check the exit code is FAILURE
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        
        //Check that the 2nd resource was NOT created
        int numDupRes = 0;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("dupRes")) {
                    numDupRes = numDupRes + 1;
                }
            }
        }       
        assertEquals(1, numDupRes);
        
        // Check the error message
        assertEquals("A JdbcResource by name dupRes already exists with resource-ref in target server.", context.getActionReport().getMessage());
        logger.fine("msg: " + context.getActionReport().getMessage());
    }
    
    
    /**
     * Test of execute method, of class CreateJdbcResource when specified
     * connectionpoolid does not exist.
     * asadmin create-jdbc-resource --connectionpoolid xxxxxx --enabled=true 
     *         --description "my resource" jdbc/nopool
     */
    @Test
    public void testExecuteFailInvalidConnPoolId() {
        // Set invalid connectionpoolid
        parameters.set("connectionpoolid", "xxxxxx");
        parameters.set("DEFAULT", "jdbc/nopool");
        
        // Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        
        // Check the exit code is Failure
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        
        //Check that the resource was NOT created
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/nopool")) {
                    isCreated = true;
                    logger.fine("JdbcResource config bean jdbc/nopool is created.");
                }
            }
        }       
        assertFalse(isCreated);

        // Check the error message
        assertEquals("Attribute value (pool-name = xxxxxx) is not found in list of jdbc connection pools.",
        context.getActionReport().getMessage());
    }
    
    /**
     * Test of execute method, of class CreateJdbcResource when enabled set to junk
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool --enabled=junk 
     *         --description "my resource" jdbc/junk
     */
    @Ignore
    @Test
    public void testExecuteFailInvalidOptionEnabled() {
        // Set invalid enabled option value: --enabled junk
        parameters.set("enabled", "junk");
        parameters.set("DEFAULT", "jdbc/junk");
        
        // Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        
        // Check the exit code is Failure
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());

        // Don't check error message.  Error message being set by CommandRunnerImpl.
    }
    
    /**
     * Test of execute method, of class CreateJdbcResource when enabled has no value
     * asadmin create-jdbc-resource --connectionpoolid DerbyPool --enabled 
     *         --description "my resource" jdbc/sun
     */
    @Test
    public void testExecuteSuccessNoValueOptionEnabled() {
        // Set enabled without a value:  --enabled
        parameters.set("enabled", "");
        parameters.set("DEFAULT", "jdbc/sun");
        
        // Call CommandRunnerImpl.doCommand(..) to execute the command
        cr.getCommandInvocation("create-jdbc-resource", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        
        // Check the exit code is SUCCESS
        assertEquals(ActionReport.ExitCode.SUCCESS, context.getActionReport().getActionExitCode());
        
        // Check that the resource was created with enabled set to true
        boolean isCreated = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof JdbcResource) {
                JdbcResource jr = (JdbcResource)resource;
                if (jr.getJndiName().equals("jdbc/sun")) {
                    assertEquals("DerbyPool", jr.getPoolName());
                    assertEquals("true", jr.getEnabled());
                    assertEquals("my resource", jr.getDescription());
                    isCreated = true;
                    continue;
                }
            }
        }       
        assertTrue(isCreated);
        
        logger.fine("msg: " + context.getActionReport().getMessage());
    }
}
