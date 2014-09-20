/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.hk2.devtest.cdi.ear.runner;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tests.utils.NucleusStartStopTest;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.oracle.hk2.devtest.cdi.ear.ejb1.Ejb1Remote;
import com.oracle.hk2.devtest.cdi.ear.ejb2.Ejb2Remote;

/**
 * 
 * @author jwells
 *
 */
public class CDIEarTest extends NucleusStartStopTest {
    private final static String APP_JAR = "cdi/ear/app/target/app.ear";
    private final static String APP_NAME = "app";
    
    private final static String SOURCE_HOME = System.getProperty("source.home", "$");
    private final static String SOURCE_HOME_APP = "/appserver/tests/hk2/" + APP_JAR;
    
    private final static String EJB1_JNDI_NAME = "java:global/app/ejb1/Ejb1";
    private final static String EJB2_JNDI_NAME = "java:global/app/ejb2/Ejb2";
    
    private final static String WAR1_URL = "http://localhost:8080/war1/war1";
    private final static String WAR2_URL = "http://localhost:8080/war2/war2";
    
    private boolean deployed1;
    private Context context;
    
    @BeforeTest
    public void beforeTest() throws NamingException {
        context = new InitialContext();
        
        String appJar = APP_JAR;
        if (!SOURCE_HOME.startsWith("$")) {
            appJar = SOURCE_HOME + SOURCE_HOME_APP;
        }
        
        deployed1 = NucleusTestUtils.nadmin("deploy", appJar);
        Assert.assertTrue(deployed1);
    }
    
    @AfterTest
    public void afterTest() throws NamingException {
        if (deployed1) {
            NucleusTestUtils.nadmin("undeploy", APP_NAME);
            deployed1 = false;
        }
        
        if (context != null) {
            context.close();
            context = null;
        }
    }
    
    private Object lookupWithFiveSecondSleep(String jndiName) throws NamingException, InterruptedException {
        long sleepTime = 5L * 1000L;
        long interval = 100L;
        
        while (sleepTime > 0) {
            try {
                return context.lookup(jndiName);
            }
            catch (NamingException ne) {
                sleepTime -= interval;
                if (sleepTime <= 0) {
                    throw ne;
                }
                
                if ((sleepTime % 1000L) == 0) {
                    System.out.println("Sleeping another " + (sleepTime / 1000) + " seconds...");
                }
                
                Thread.sleep(interval);
            }
            
        }
        
        throw new AssertionError("Should never get here");
    }
    
    @Test
    public void testInjectFromLib1IntoEjb1() throws NamingException, InterruptedException {
        Ejb1Remote ejb1 = (Ejb1Remote) lookupWithFiveSecondSleep(EJB1_JNDI_NAME);
        
        ejb1.isLib1HK2ServiceAvailable(); 
    }
    
    @Test
    public void testInjectFromEjb1IntoEjb1() throws NamingException, InterruptedException {
        Ejb1Remote ejb1 = (Ejb1Remote) lookupWithFiveSecondSleep(EJB1_JNDI_NAME);
        
        ejb1.isEjb1HK2ServiceAvailable();
        
    }
    
    @Test
    public void testInjectedLib1Ejb1War1IntoWar1() {
        String fromWar1 = NucleusTestUtils.getURL(WAR1_URL);
        
        Assert.assertTrue(fromWar1.contains("success"),
                "Does not contain the word success: " + fromWar1);
    }
    
    @Test
    public void testInjectFromLib1IntoEjb2() throws NamingException, InterruptedException {
        Ejb2Remote ejb2 = (Ejb2Remote) lookupWithFiveSecondSleep(EJB2_JNDI_NAME);
        
        ejb2.isLib1HK2ServiceAvailable(); 
    }
    
    @Test
    public void testInjectFromEjb1IntoEjb2() throws NamingException, InterruptedException {
        Ejb2Remote ejb2 = (Ejb2Remote) lookupWithFiveSecondSleep(EJB2_JNDI_NAME);
        
        ejb2.isEjb1HK2ServiceAvailable(); 
    }
    
    @Test
    public void testInjectFromEjb2IntoEjb2() throws NamingException, InterruptedException {
        Ejb2Remote ejb2 = (Ejb2Remote) lookupWithFiveSecondSleep(EJB2_JNDI_NAME);
        
        ejb2.isEjb2HK2ServiceAvailable(); 
    }
    
    @Test
    public void testInjectedLib1Ejb1Ejb2War2IntoWar2() {
        String fromWar2 = NucleusTestUtils.getURL(WAR2_URL);
        
        Assert.assertTrue(fromWar2.contains("success"),
                "Does not contain the word success: " + fromWar2);
    }
}
