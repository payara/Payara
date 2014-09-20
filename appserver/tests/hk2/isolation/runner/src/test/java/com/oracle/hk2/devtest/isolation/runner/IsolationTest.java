/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.hk2.devtest.isolation.runner;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.glassfish.tests.utils.NucleusStartStopTest;
import org.glassfish.tests.utils.NucleusTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Ensures that different apps get different service locators
 * 
 * @author jwells
 *
 */
public class IsolationTest extends NucleusStartStopTest {
    private final static String ISO1_WAR = "isolation/web/iso1/target/hk2-isolation-web-iso1.war";
    private final static String ISO1_APP_NAME = "hk2-isolation-web-iso1";
    private final static String ISO1_URL = "http://localhost:8080/hk2-isolation-web-iso1/iso1";
    
    private final static String ISO2_WAR = "isolation/web/iso2/target/hk2-isolation-web-iso2.war";
    private final static String ISO2_APP_NAME = "hk2-isolation-web-iso2";
    private final static String ISO2_URL = "http://localhost:8080/hk2-isolation-web-iso2/iso2";
    
    private final static String SOURCE_HOME = System.getProperty("source.home", "$");
    private final static String SOURCE_HOME_ISO1_WAR = "/appserver/tests/hk2/" + ISO1_WAR;
    private final static String SOURCE_HOME_ISO2_WAR = "/appserver/tests/hk2/" + ISO2_WAR;
    
    private static final String SERVLET_CONTEXT_LOCATOR = "ServletContextLocator";
    private static final String JNDI_APP_LOCATOR = "JndiAppLocator";
    
    private boolean deployed1;
    private boolean deployed2;
    
    private Map<String, String> getNames(String rawHTML) {
        Map<String, String> retVal = new HashMap<String, String>();
        
        StringTokenizer st = new StringTokenizer(rawHTML, "\n");
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            
            int equalsIndex = line.indexOf('=');
            if (equalsIndex < 0) continue;  // Skip lines that do not have = in it
            
            String key = line.substring(0, equalsIndex);
            String value = line.substring(equalsIndex + 1, line.length());
            
            retVal.put(key, value);
        }
        
        return retVal;
    }
    
    private String getName(String rawHTML, String key) {
        Map<String, String> names = getNames(rawHTML);
        
        return names.get(key);
    }
    
    @BeforeTest
    public void beforeTest() {
        String iso1War = ISO1_WAR;
        String iso2War = ISO2_WAR;
        
        if (!SOURCE_HOME.startsWith("$")) {
            iso1War = SOURCE_HOME + SOURCE_HOME_ISO1_WAR;
            iso2War = SOURCE_HOME + SOURCE_HOME_ISO2_WAR;
        }
        
        deployed1 = NucleusTestUtils.nadmin("deploy", iso1War);
        deployed2 = NucleusTestUtils.nadmin("deploy", iso2War);
        
        Assert.assertTrue(deployed1);
        Assert.assertTrue(deployed2);
    }
    
    @AfterTest
    public void afterTest() {
        if (deployed1) {
            deployed1 = false;
            NucleusTestUtils.nadmin("undeploy", ISO1_APP_NAME);
        }
        if (deployed2) {
            deployed2 = false;
            NucleusTestUtils.nadmin("undeploy", ISO2_APP_NAME);
        }
    }
    
    /**
     * Ensures that the service locators in two web-apps are different
     */
    @Test(enabled=false)
    public void testWebAppsAreIsolated() {
        String fromURL1 = NucleusTestUtils.getURL(ISO1_URL);
        String fromURL2 = NucleusTestUtils.getURL(ISO2_URL);
            
        String iso1Name = getName(fromURL1, SERVLET_CONTEXT_LOCATOR);
        String iso2Name = getName(fromURL2, SERVLET_CONTEXT_LOCATOR);
            
        Assert.assertNotEquals(iso1Name, iso2Name);
    }
    
    /**
     * Ensures that the application service locators in two web-apps are different
     */
    @Test
    public void testWebAppsApplicationServiceLocatorsAreIsolated() {
        String fromURL1 = NucleusTestUtils.getURL(ISO1_URL);
        String fromURL2 = NucleusTestUtils.getURL(ISO2_URL);
            
        String iso1Name = getName(fromURL1, JNDI_APP_LOCATOR);
        String iso2Name = getName(fromURL2, JNDI_APP_LOCATOR);
            
        Assert.assertNotEquals(iso1Name, iso2Name);
    }
}
