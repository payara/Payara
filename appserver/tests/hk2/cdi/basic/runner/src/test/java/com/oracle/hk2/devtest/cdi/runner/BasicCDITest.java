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
package com.oracle.hk2.devtest.cdi.runner;

import java.io.File;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tests.utils.NucleusStartStopTest;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.oracle.hk2.devtest.cdi.ejb1.BasicEjb;

/**
 * 
 * @author jwells
 *
 */
public class BasicCDITest extends NucleusStartStopTest {
    private final static String EJB1_JAR = "cdi/basic/ejb1/target/ejb1.jar";
    private final static String EJB1_APP_NAME = "ejb1";
    private final static String BASIC_EJB_JNDI_NAME = "java:global/ejb1/EjbInjectedWithServiceLocator!" +
      BasicEjb.class.getName();
    private final static String SOURCE_HOME = System.getProperty("source.home", "$");
    private final static String SOURCE_HOME_EJB = "/appserver/tests/hk2/" + EJB1_JAR;
    private final static String GLASSFISH_HOME = System.getProperty("glassfish.home");
    private final static String RELATIVE_FILE_PATH = "domains/domain1/config/destroyed-ejb1.txt";
    
    private boolean deployed1;
    private Context context;
    
    @BeforeTest
    public void beforeTest() throws NamingException {
        context = new InitialContext();
        
        String ejb1Jar = EJB1_JAR;
        if (!SOURCE_HOME.startsWith("$")) {
            ejb1Jar = SOURCE_HOME + SOURCE_HOME_EJB;
        }
        
        deployed1 = NucleusTestUtils.nadmin("deploy", ejb1Jar);
        Assert.assertTrue(deployed1);
        
        File destroyedFile = new File(GLASSFISH_HOME);
        destroyedFile = new File(destroyedFile, RELATIVE_FILE_PATH);
        
        if (destroyedFile.exists()) {
            Assert.assertTrue(destroyedFile.delete());
        }
    }
    
    @AfterTest
    public void afterTest() throws NamingException {
        if (deployed1) {
            NucleusTestUtils.nadmin("undeploy", EJB1_APP_NAME);
            deployed1 = false;
        }
        
        if (context != null) {
            context.close();
            context = null;
        }
        
        // After the undeployment the file indicating the proper destruction should be there
        File destroyedFile = new File(GLASSFISH_HOME);
        destroyedFile = new File(destroyedFile, RELATIVE_FILE_PATH);
        
        Assert.assertTrue(destroyedFile.exists());
    }
    
    /**
     * Ensures that a ServiceLocator can be injected into a CDI bean
     * @throws NamingException 
     */
    @Test
    public void testBasicHK2CDIInjection() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
            
        Assert.assertTrue(basic.cdiManagerInjected());
        Assert.assertTrue(basic.serviceLocatorInjected());
            
        basic.installHK2Service();
            
        Assert.assertTrue(basic.hk2ServiceInjectedWithEjb());
    }
    
    /**
     * Ensures that the ServiceLocator is available in all CDI events
     * 
     * @throws NamingException
     */
    @Test
    public void testCDIExtensionHasAccessToServiceLocatorViaJNDI() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
        
        basic.isServiceLocatorAvailableInAllCDIExtensionEvents();
    }
    
    /**
     * Tests that an HK2 service with its own scope works properly
     * 
     * @throws NamingException
     */
    @Test
    public void testCustomScopedHK2ServiceWorks() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
        
        basic.isEJBWithCustomHK2ScopeProperlyInjected();
    }
    
    /**
     * Tests that an application created post processor works
     * 
     * @throws NamingException
     */
    @Test
    public void testApplicationDefinedPostProcessorRuns() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
        
        basic.doesApplicationDefinedPopulatorPostProcessorRun();
    }
    
    /**
     * Tests that a service added via JIT resolution works
     * 
     * @throws NamingException
     */
    @Test
    public void testJITResolution() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
        
        basic.isServiceAddedWithJITResolverAdded();
    }
    
    /**
     * Tests that a service added via JIT resolution works
     * 
     * @throws NamingException
     */
    @Test
    public void testApplicationScopedCDIServiceIntoHK2Service() throws NamingException {
        BasicEjb basic = (BasicEjb) context.lookup(BASIC_EJB_JNDI_NAME);
        Assert.assertNotNull(basic);
        
        basic.checkApplicationScopedServiceInjectedIntoHk2Service();
    }
}
