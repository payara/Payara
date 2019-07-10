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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package org.glassfish.orb.admin.cli;

import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
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
import java.util.List;

import junit.framework.Assert;


public class CreateIiopListenerTest extends org.glassfish.tests.utils.ConfigApiTest {

    private ServiceLocator services;
    private IiopService iiopService;
    private ParameterMap parameters;
    private AdminCommandContext context;
    private CommandRunner cr;
    
    public static void checkActionReport(ActionReport report) {
        if (ActionReport.ExitCode.SUCCESS.equals(report.getActionExitCode())) {
            return;
        }
        
        Throwable reason = report.getFailureCause();
        Assert.assertNotNull("Action failed with exit code " +
            report.getActionExitCode() + " and message " +
                report.getMessage(), reason);
        throw new AssertionError(reason);
    }

    @Override
    public String getFileName() {
        return "DomainTest";
    }

    public DomDocument getDocument(ServiceLocator services) {
        return new TestDocument(services);
    }

    @Before
    public void setUp() {
        services = getHabitat();
        iiopService = services.getService(IiopService.class);
        parameters = new ParameterMap();
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(CreateIiopListenerTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        cr = services.getService(CommandRunner.class);
    }

    @After
    public void tearDown() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<IiopService>() {
            public Object run(IiopService param) throws PropertyVetoException,
                    TransactionFailure {
                List<IiopListener> listenerList = param.getIiopListener();
                for (IiopListener listener : listenerList) {
                    String currListenerId = listener.getId();
                    if (currListenerId != null && currListenerId.equals
                            ("iiop_1")) {
                        listenerList.remove(listener);
                        break;
                    }
                }
                return listenerList;
            }
        }, iiopService);
        parameters = new ParameterMap();
    }
    
    

    /**
     * Test of execute method, of class CreateIiopListener.
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 --enabled=true --securityenabled=true iiop_1
     */
    @Test
    public void testExecuteSuccess() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        parameters.set("enabled", "true");
        parameters.set("securityenabled", "true");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("true", listener.getEnabled());
                assertEquals("4440", listener.getPort());
                assertEquals("true", listener.getSecurityEnabled());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateIiopListener.
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 iiop_1
     */

    @Test
    public void testExecuteSuccessDefaultValues() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("4440", listener.getPort());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }


    /**
     * Test of execute method, of class CreateIiopListener.
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 iiop_1
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 iiop_1
     */
    @Test
    public void testExecuteFailDuplicateListener() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        CreateIiopListener command1 = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command1);
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("4440", listener.getPort());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());

        CreateIiopListener command2 = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command2);               
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        int numDupRes = 0;
        listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                numDupRes = numDupRes + 1;
            }
        }
        assertEquals(1, numDupRes);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateIiopListener with same iiop port number
     * and listener address.
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 iiop_1
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 iiop_2
     */
    @Test
    public void testExecuteFailForSamePortAndListenerAddress() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("4440", listener.getPort());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());

        parameters = new ParameterMap();
        parameters.set("listener_id", "iiop_2");
        parameters.set("iiopport", "4440");
        parameters.set("listeneraddress", "localhost");
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
        logger.fine("msg: " + context.getActionReport().getMessage());

    }

    /**
     * Test of execute method, of class CreateIiopListener when enabled set to junk
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 --enabled=junk iiop_1
     */
    //@Test
    public void testExecuteFailInvalidOptionEnabled() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        parameters.set("enabled", "junk");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        assertEquals(ActionReport.ExitCode.FAILURE, context.getActionReport().getActionExitCode());
    }

    /**
     * Test of execute method, of class CreateIiopListener when enabled has no value
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 --enable iiop_1
     */
    @Test
    public void testExecuteSuccessNoValueOptionEnabled() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        parameters.set("enabled", "");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("true", listener.getEnabled());
                assertEquals("4440", listener.getPort());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }

    /**
     * Test of execute method, of class CreateIiopListener when enabled has no value
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 --securityenabled iiop_1
     */
    @Test
    public void testExecuteSuccessNoValueOptionSecurityEnabled() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "iiop_1");
        parameters.set("securityenabled", "");
        CreateIiopListener command = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(command);               
        checkActionReport(context.getActionReport());
        boolean isCreated = false;
        List<IiopListener> listenerList = iiopService.getIiopListener();
        for (IiopListener listener : listenerList) {
            if (listener.getId().equals("iiop_1")) {
                assertEquals("localhost", listener.getAddress());
                assertEquals("true", listener.getSecurityEnabled());
                assertEquals("4440", listener.getPort());
                isCreated = true;
                logger.fine("IIOPListener name iiop_1 is created.");
                break;
            }
        }
        assertTrue(isCreated);
        logger.fine("msg: " + context.getActionReport().getMessage());
    }
}
