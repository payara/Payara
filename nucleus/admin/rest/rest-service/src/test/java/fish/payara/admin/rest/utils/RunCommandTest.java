/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.admin.rest.utils;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;

import javax.security.auth.Subject;

import com.sun.enterprise.admin.report.DoNothingActionReporter;

import fish.payara.audit.AdminAuditConfiguration;
import fish.payara.audit.AdminAuditService;
import fish.payara.audit.admin.GetAdminAuditServiceConfiguration;
import fish.payara.nucleus.notification.NotificationService;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.domain.EventSource;
import fish.payara.nucleus.notification.domain.NotificationEvent;
import fish.payara.nucleus.notification.domain.NotifierExecutionOptionsFactoryStore;
import fish.payara.nucleus.notification.log.LogNotificationEvent;
import fish.payara.nucleus.notification.service.NotificationEventFactoryStore;
import java.beans.PropertyVetoException;


import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandEventBroker;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandParameters;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.ProgressStatus;

import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.runlevel.RunLevelContext;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.runlevel.internal.AsyncRunLevelContext;
import org.glassfish.hk2.runlevel.internal.RunLevelControllerImpl;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.Target;

import org.junit.Before;
import org.junit.Test;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.testng.Assert;

import sun.security.acl.PrincipalImpl;

/**
 * Test for running a command to check that is is processed properly.
 * This tests the admin audit service.
 * @see ResourceUtil#runCommand(java.lang.String, org.glassfish.api.admin.ParameterMap, javax.security.auth.Subject) 
 * @author jonathan coustick
 */
public class RunCommandTest {
    
    private ServiceLocator serviceLocator;
    
    private List<NotificationEvent> events;
    
    @Before
    public void setUp() {
        serviceLocator = ServiceLocatorFactory.getInstance().create("testServiceLocator");
        ServiceLocatorUtilities.addOneConstant(serviceLocator, new TestNotificationService());
        ServiceLocatorUtilities.addOneConstant(serviceLocator, this);
        ServiceLocatorUtilities.addClasses(serviceLocator, AdminAuditService.class, GetAdminAuditServiceConfiguration.class, TestConfiguration.class,
                Target.class, NotificationEventFactoryStore.class, NotifierExecutionOptionsFactoryStore.class, TestCommandRunner.class);
        
        Globals.setDefaultHabitat(serviceLocator);
        
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.addActiveDescriptor(RunLevelControllerImpl.class);
        config.addActiveDescriptor(AsyncRunLevelContext.class);
        config.addActiveDescriptor(RunLevelContext.class);
            
        config.commit();
        
        serviceLocator.getService(RunLevelController.class).proceedTo(StartupRunLevel.VAL);
        serviceLocator.getService(NotificationEventFactoryStore.class).register(NotifierType.LOG, new fish.payara.nucleus.notification.log.LogNotificationEventFactory() {
            @Override
            public LogNotificationEvent buildNotificationEvent(Level level, String subject, String message, Object[] parameters) {
                LogNotificationEvent event = new LogNotificationEvent();
                Assert.assertEquals(Level.WARNING, level);
                event.setLevel(level);
                Assert.assertNull(parameters);
                event.setParameters(parameters);
                event.setMessage(message);
                Assert.assertEquals("AUDIT", subject);
                event.setSubject(subject);
                return event;
            }
        });
        
        
        events = new LinkedList<>();
    }
    
    @Test
    public void testAdminAudit() {
        Subject testSubject = new Subject();
        testSubject.getPrincipals().add(new PrincipalImpl("testuser"));
        RestActionReporter commandResult = ResourceUtil.runCommand("get-admin-audit-configuration", new ParameterMap(), testSubject);
        Assert.assertTrue(commandResult.isSuccess());
        
        
    }
    
    @Service
    @ContractsProvided(NotificationService.class)
    class TestNotificationService extends NotificationService {
        
        @Override
        public void notify(EventSource source, NotificationEvent notificationEvent) {
            if (!source.equals(EventSource.AUDIT)) {
                Assert.fail("Recieved non-audit message, was of type" + source.getValue());
            }
            events.add(notificationEvent);
            
        }
        
    }

    @Service
    @ContractsProvided(AdminAuditConfiguration.class)
    class TestConfiguration implements AdminAuditConfiguration {

        @Inject
        public TestConfiguration() {
            
        }
        
        
        @Override
        public String getEnabled() {
            return Boolean.TRUE.toString();
        }

        @Override
        public void enabled(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getAuditLevel() {
            return "INTERNAL";
        }

        @Override
        public void setAuditLevel(String value) throws PropertyVetoException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public List<Notifier> getNotifierList() {
            return new ArrayList<>();
        }

        @Override
        public <T extends Notifier> T getNotifierByType(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ConfigBeanProxy getParent() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) throws TransactionFailure {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
 
    @Service
    @ContractsProvided(CommandRunner.class)
    class TestCommandRunner implements CommandRunner {
        
        @Inject
        public TestCommandRunner() {
            
        }

        @Override
        public ActionReport getActionReport(String name) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CommandModel getModel(String name, Logger logger) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CommandModel getModel(String scope, String name, Logger logger) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BufferedReader getHelp(CommandModel model) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean validateCommandModelETag(AdminCommand command, String eTag) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean validateCommandModelETag(CommandModel model, String eTag) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public AdminCommand getCommand(String commandName, ActionReport report, Logger logger) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public AdminCommand getCommand(String scope, String commandName, ActionReport report, Logger logger) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CommandInvocation getCommandInvocation(String name, ActionReport report, Subject subject) {
            return new CommandInvocation() {
                @Override
                public CommandInvocation parameters(CommandParameters params) {
                    return this;
                }

                @Override
                public CommandInvocation parameters(ParameterMap params) {
                    return this;
                }

                @Override
                public CommandInvocation inbound(Payload.Inbound inbound) {
                   return this;
                }

                @Override
                public CommandInvocation outbound(Payload.Outbound outbound) {
                    return this;
                }

                @Override
                public CommandInvocation listener(String nameRegexp, AdminCommandEventBroker.AdminCommandListener listener) {
                    return this;
                }

                @Override
                public CommandInvocation progressStatusChild(ProgressStatus ps) {
                    return this;
                }

                @Override
                public CommandInvocation managedJob() {
                    return this;
                }

                @Override
                public ActionReport report() {
                    return new DoNothingActionReporter();
                }

                @Override
                public void execute() {
                    // do nothing
                }

                @Override
                public void execute(AdminCommand command) {
                    //do nothing
                }
            };
        }

        @Override
        public CommandInvocation getCommandInvocation(String scope, String name, ActionReport report, Subject subject) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CommandInvocation getCommandInvocation(String name, ActionReport report, Subject subject, boolean isNotify) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public CommandInvocation getCommandInvocation(String scope, String name, ActionReport report, Subject subject, boolean isNotify) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
}
