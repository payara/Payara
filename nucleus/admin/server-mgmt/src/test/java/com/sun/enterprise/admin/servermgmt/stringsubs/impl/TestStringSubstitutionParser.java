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
package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.File;
import java.io.InputStream;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutionException;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.StringsubsDefinition;

/**
 * Unit test class to test {@link StringSubstitutionParser} functionality.
 */
public class TestStringSubstitutionParser {

    // Test string-subs.xml file.
    private final String _stringSubsPath = TestStringSubstitutionParser.class.getPackage().
            getName().replace(".", File.separator) + File.separator + "stringsubs.xml";
    private InputStream _configStream = null;

    @BeforeClass
    public void init() {
        _configStream = TestStringSubstitutionParser.class.getClassLoader().getResourceAsStream(_stringSubsPath);  
    }

    @Test
    public void testParserWithNullInput() {
        try {
            StringSubstitutionParser.parse(null);
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Failed, Parser is allowing null stream to parse.");
    }

    /**
     * Test string subs XML parsing.
     */
    @Test
    public void testParser() {
        StringsubsDefinition def = null;
        try {
            def = StringSubstitutionParser.parse(_configStream);
        } catch (StringSubstitutionException e) {
            Assert.fail("Failed to parse xml : " + _stringSubsPath, e);
        }
        Assert.assertNotNull(def);
        Assert.assertNotNull(def.getComponent());
        Assert.assertNotNull(def.getVersion());
        Assert.assertFalse(def.getChangePair().isEmpty());
    }
}