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
package com.oracle.cdi.devtests.cases.runner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.glassfish.tests.utils.NucleusStartStopTest;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.oracle.cdi.cases.devtests.multiejb1.MultiBeansXmlEjb1;
import com.oracle.cdi.cases.devtests.multiejb2.MultiBeansXmlEjb2;
import com.oracle.cdi.cases.devtests.predestroy.lib.PreDestroyConstants;

/**
 * 
 * @author jwells
 */
public class CasesTest extends NucleusStartStopTest {
    private final static String SOURCE_HOME = System.getProperty("source.home", "$");
    private final static String SOURCE_HOME_CDI = "/appserver/tests/cdi/";
    
    private Context context;
    
    @BeforeTest
    public void beforeTest() throws NamingException {
        context = new InitialContext();
    }
    
    private static String getDeployablePath(String endPath) {
        if (!SOURCE_HOME.startsWith("$")) {
            return SOURCE_HOME + SOURCE_HOME_CDI + endPath;
        }
        
        return endPath;
    }
    
    private static void deploy(String deployPath) {
        String dp = getDeployablePath(deployPath);
        
        boolean success = NucleusTestUtils.nadmin("deploy", dp);
        Assert.assertTrue(success);
    }
    
    private static void undeploy(String appName) {
        boolean success = NucleusTestUtils.nadmin("undeploy", appName);
        Assert.assertTrue(success);
    }
    
    private final static String MULTI_BEANS_XML_JAR = "cases/multiBeansXml/multiBeansApp/target/multiBeansApp.ear";
    private final static String MULTI_BEANS_XML_APP = "multiBeansApp";
    private final static String MULTI_BEANS_EJB1_JNDI = "java:global/multiBeansApp/ejb1/InterceptedEjb1!com.oracle.cdi.cases.devtests.multiejb1.MultiBeansXmlEjb1";
    private final static String MULTI_BEANS_EJB2_JNDI = "java:global/multiBeansApp/ejb2/InterceptedEjb2!com.oracle.cdi.cases.devtests.multiejb2.MultiBeansXmlEjb2";
    
    @Test
    public void testMultiBeansXml() throws NamingException {
        deploy(MULTI_BEANS_XML_JAR);
        try {
            {
                MultiBeansXmlEjb1 ejb1 = (MultiBeansXmlEjb1) context.lookup(MULTI_BEANS_EJB1_JNDI);
                Assert.assertNotNull(ejb1);
                
                List<String> ejb1List = ejb1.callMe(new LinkedList<String>());
                Assert.assertNotNull(ejb1List);
                
                Assert.assertEquals(2, ejb1List.size());
                
                String interceptor1 = ejb1List.get(0);
                String callMe1 = ejb1List.get(1);
                
                Assert.assertEquals(MultiBeansXmlEjb1.INTERCEPTOR1, interceptor1);
                Assert.assertEquals(MultiBeansXmlEjb1.CALL_ME1, callMe1);
            }
            
            {
                MultiBeansXmlEjb2 ejb2 = (MultiBeansXmlEjb2) context.lookup(MULTI_BEANS_EJB2_JNDI);
                Assert.assertNotNull(ejb2);
            
                List<String> ejb2List = ejb2.callMe(new LinkedList<String>());
                Assert.assertNotNull(ejb2List);
                
                Assert.assertEquals(2, ejb2List.size());
                
                String interceptor2 = ejb2List.get(0);
                String callMe2 = ejb2List.get(1);
                
                Assert.assertEquals(MultiBeansXmlEjb2.INTERCEPTOR2, interceptor2);
                Assert.assertEquals(MultiBeansXmlEjb2.CALL_ME2, callMe2);
            }
            
        }
        finally {
            undeploy(MULTI_BEANS_XML_APP);
        }
        
    }
    
    private final static String PRE_DESTROY_SCOPING_JAR = "cases/preDestroyScoping/ear/target/preDestroyScoping.ear";
    private final static String PRE_DESTROY_SCOPING_APP = "preDestroyScoping";
    private final static String BEAN_URL = "http://localhost:8080/web/bean";
    private final static String LOGOUT_BASE_URL = "http://localhost:8080/web/logout;jsessionid=";
    private final static String EVENTS_URL = "http://localhost:8080/web/events";
    
    private static List<String> getLines(String lines) {
        StringTokenizer st = new StringTokenizer(lines, "\n");
        
        List<String> retVal = new ArrayList<String>();
        
        while (st.hasMoreTokens()) {
            retVal.add(st.nextToken());
        }
        
        return retVal;
    }
    
    /**
     * This test case is from https://java.net/jira/browse/GLASSFISH-18435
     */
    @Test
    public void testPreDestroyScoping() {
        deploy(PRE_DESTROY_SCOPING_JAR);
        try {
            String lines = NucleusTestUtils.getURL(BEAN_URL);
            
            List<String> asArray = getLines(lines);
            Assert.assertEquals(asArray.size(), 2, "Did not get the proper number of strings from getURL return of " + lines);
            
            String uuid = asArray.get(1);
            
            NucleusTestUtils.getURL(LOGOUT_BASE_URL + uuid);
            
            lines = NucleusTestUtils.getURL(EVENTS_URL);
            
            List<String> results = getLines(lines);
            
            Assert.assertEquals(results.size(), 3);
            
            Assert.assertEquals(results.get(0), PreDestroyConstants.CREATED + asArray.get(0));
            Assert.assertEquals(results.get(1), PreDestroyConstants.PRODUCER_PRE_DESTROY_IN);
            Assert.assertEquals(results.get(2), PreDestroyConstants.EXPECTED_EXCEPTION);
        }
        finally {
            undeploy(PRE_DESTROY_SCOPING_APP);
        }
        
    }
}
