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
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.admin.AdminCommandContext;
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


public class ListIiopListenersTest extends org.glassfish.tests.utils.ConfigApiTest {

    private ServiceLocator services;
    private int origNum;
    private ParameterMap parameters;
    private CommandRunner cr;
    private AdminCommandContext context;

    public String getFileName() {
        return "DomainTest";
    }

    public DomDocument getDocument(ServiceLocator services) {
        return new TestDocument(services);
    }

    @Before
    public void setUp() {
        services = getHabitat();
        IiopService iiopService = services.getService(IiopService.class);
        parameters = new ParameterMap();
        cr = services.getService(CommandRunner.class);
        context = new AdminCommandContextImpl(
                LogDomains.getLogger(ListIiopListenersTest.class, LogDomains.ADMIN_LOGGER),
                new PropsFileActionReporter());
        List<IiopListener> listenerList = iiopService.getIiopListener();
        origNum = listenerList.size();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of execute method, of class ListIiopListeners.
     * list-iiop-listeners
     */
    @Test
    public void testExecuteSuccessListOriginal() {
        ListIiopListeners listCommand = services.getService(ListIiopListeners.class);
        cr.getCommandInvocation("list-iiop-listeners", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);
        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum, list.size());
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
    }

    /**
     * Test of execute method, of class ListIiopListeners.
     * list-iiop-listeners server
     */
    @Test
    public void testExecuteSuccessValidTargetOperand() {
        ListIiopListeners listCommand = services.getService(ListIiopListeners.class);
        parameters.set("DEFAULT", "server");
        cr.getCommandInvocation("list-iiop-listeners", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);               
        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum, list.size());
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
    }

    /**
     * Test of execute method, of class ListIiopListeners.
     * asadmin create-iiop-listener --listeneraddress localhost
     * --iiopport 4440 listener
     * list-iiop-listeners
     * delete-iiop-listener listener
     */
    @Test
    public void testExecuteSuccessListListener() {
        parameters.set("listeneraddress", "localhost");
        parameters.set("iiopport", "4440");
        parameters.set("listener_id", "listener");
        CreateIiopListener createCommand = services.getService(CreateIiopListener.class);
        cr.getCommandInvocation("create-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(createCommand);
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
        parameters = new ParameterMap();
        ListIiopListeners listCommand = services.getService(ListIiopListeners.class);
        cr.getCommandInvocation("list-iiop-listeners", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);               
        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum + 1, list.size());
        List<String> listStr = new ArrayList<String>();
        for (MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertTrue(listStr.contains("listener"));
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
        parameters = new ParameterMap();
        parameters.set("listener_id", "listener");
        DeleteIiopListener deleteCommand = services.getService(DeleteIiopListener.class);
        cr.getCommandInvocation("delete-iiop-listener", context.getActionReport(), adminSubject()).parameters(parameters).execute(deleteCommand);               
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
    }

    /**
     * Test of execute method, of class ListIiopListener.
     * list-iiop-listeners
     */
    @Test
    public void testExecuteSuccessListNoListener() {       
        parameters = new ParameterMap();
        ListIiopListeners listCommand = services.getService(ListIiopListeners.class);
        cr.getCommandInvocation("list-iiop-listeners", context.getActionReport(), adminSubject()).parameters(parameters).execute(listCommand);    
        CreateIiopListenerTest.checkActionReport(context.getActionReport());
        List<MessagePart> list = context.getActionReport().getTopMessagePart().getChildren();
        assertEquals(origNum, list.size());
        List<String> listStr = new ArrayList<String>();
        for (MessagePart mp : list) {
            listStr.add(mp.getMessage());
        }
        assertFalse(listStr.contains("listener"));
    }
}
