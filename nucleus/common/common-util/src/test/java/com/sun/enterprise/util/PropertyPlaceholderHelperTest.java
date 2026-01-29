/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package com.sun.enterprise.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Zdeněk Soukup
 */
public class PropertyPlaceholderHelperTest {

    private Map<String, String> env = new HashMap<>();

    public PropertyPlaceholderHelperTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        env.put("testEnvironmentHandlers", "java.util.logging.ConsoleHandler");
        env.put("testEnvironmentHandlerServices", "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler");
    }

    @After
    public void tearDown() {
        env = null;
    }

    /**
     * Test of getPropertyValue method, of class PropertyPlaceholderHelper.
     *
     */
    @Test
    public void testGetPropertyValueFromEnv() {
        System.out.println("getPropertyValueFromEnv");
        String loggingProperty1 = "testEnvironmentHandlers";
        String loggingProperty2 = "testEnvironmentHandlerServices";

        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        String expResult1 = "java.util.logging.ConsoleHandler";
        String result1 = propertyPlaceholderHelper.getPropertyValue(loggingProperty1);
        assertEquals(expResult1, result1);

        propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        String expResult2 = "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler";
        String result2 = propertyPlaceholderHelper.getPropertyValue(loggingProperty2);
        assertEquals(expResult2, result2);
    }

    /**
     * Test of replacePropertiesPlaceholder method, of class
     *
     */
    @Test
    public void testReplacePropertiesPlaceholder() {
        System.out.println("replacePropertiesPlaceholder");
        Properties props = new Properties();
        props.setProperty("testEnvironmentHandlersProperties", "${ENV=testEnvironmentHandlers}");
        props.setProperty("testEnvironmentHandlerServicesProperties", "${ENV=testEnvironmentHandlerServices}");

        Properties expResultProps = new Properties();
        expResultProps.setProperty("testEnvironmentHandlersProperties", "java.util.logging.ConsoleHandler");
        expResultProps.setProperty("testEnvironmentHandlerServicesProperties", "com.sun.enterprise.server.logging.GFFileHandler,com.sun.enterprise.server.logging.SyslogHandler");

        PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper(env, PropertyPlaceholderHelper.ENV_REGEX);
        Properties result = propertyPlaceholderHelper.replacePropertiesPlaceholder(props);

        assertEquals(expResultProps.getProperty("testEnvironmentHandlersProperties"), result.getProperty("testEnvironmentHandlersProperties"));
        assertEquals(expResultProps.getProperty("testEnvironmentHandlerServicesProperties"), result.getProperty("testEnvironmentHandlerServicesProperties"));
    }

}