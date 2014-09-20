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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.stringsubs.AttributePreprocessor;
import com.sun.enterprise.admin.servermgmt.stringsubs.StringSubstitutionException;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.StringsubsDefinition;

/**
 * Unit test class to test {@link StringSubstitutionEngine} functionality.
 */
public class TestStringSubstitutionEngine {

    private final static String _stringSubsPath = TestStringSubstitutionEngine.class.getPackage().
            getName().replace(".", "/") + "/stringsubs.xml";
    private static final String GROUP_WITHOUT_CHANGE_PAIR = "group_without_change_pair";
    private static final String GROUP_WITHOUT_FILES = "group_without_files";
    private static final String GROUP_WITH_INVALID_FILE_PATHS = "group_invalid_file_paths";
    private static final String _testFileName = "testStringSubs.txt";
    private static Map<String, String> _substitutionRestoreMap = new HashMap<String, String>();

    private File _testFile = null;
    private String _archiveDirPath = null;
    private StringSubstitutionEngine _engine;

    @BeforeClass
    public void init() throws Exception {
        InputStream configStream = TestStringSubstitutionEngine.class.getClassLoader().
                getResourceAsStream(_stringSubsPath);
        URL url = TestStringSubstitutionEngine.class.getClassLoader().getResource(_stringSubsPath);
        _archiveDirPath = new File(url.getPath()).getParentFile().getAbsolutePath();

        Map<String, String> lookUpMap = new HashMap<String, String>();
        lookUpMap.put("ORACLE_HOME", "REPLACED_ORACLE_HOME");
        lookUpMap.put("MW_HOME", "REPLACED_MW_HOME");
        _substitutionRestoreMap.put("REPLACED_ORACLE_HOME", "@ORACLE_HOME@");
        _substitutionRestoreMap.put("REPLACED_MW_HOME", "@MW_HOME@");
        _engine = new StringSubstitutionEngine(configStream);
        _engine.setAttributePreprocessor(new CustomAttributePreprocessor(lookUpMap));
    }

    /**
     * Test the engine initialization for invalid stream.
     */
    @Test
    public void testInitializationForInvalidStream() {
        try {
            new StringSubstitutionEngine(null);
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Allowing to parse the invalid stringsubs.xml stream.");
    }

    /**
     * Test the loaded string-subs.xml object.
     */
    @Test
    public void testXMLLoading() {
        StringsubsDefinition def = _engine.getStringSubsDefinition();
        Assert.assertNotNull(def);
        Assert.assertNotNull(def.getComponent());
        Assert.assertEquals(def.getComponent().size(), 2);
        Assert.assertNotNull(def.getVersion());
        Assert.assertFalse(def.getChangePair().isEmpty());
    }

    /**
     * Test substitution for null Component.
     */
    @Test
    public void testSubstitutionForNullComponent() {
        try {
            _engine.substituteComponents(null);
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Allowing to peform substitution for null Component.");
    }

    /**
     * Test substitution for empty Component.
     */
    @Test
    public void testSubstitutionForEmptyComponent() {
        try {
            _engine.substituteComponents(new ArrayList<String>(1));
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Allowing to peform substitution for empty Component.");
    }

    /**
     * Test substitution for invalid Component.
     */
    @Test
    public void testSubstitutionForInvalidComponent() {
        List<String> componentIDs = new ArrayList<String>(1);
        componentIDs.add("invalidComponent");
        try {
            _engine.substituteComponents(componentIDs);
        }
        catch (StringSubstitutionException e) {
            Assert.fail("Throwing exception if invalid Component id is passed for susbtitution.");
            return;
        }
    }

    /**
     * Test substitution for invalid Component.
     */
    @Test
    public void testSubstitutionForComponentWithoutGroup() {
        List<String> componentIDs = new ArrayList<String>(1);
        componentIDs.add("component_without_group");
        try {
            _engine.substituteComponents(componentIDs);
        } catch (StringSubstitutionException e) {
            Assert.fail("Throwing exception if invalid Component id is passed for susbtitution.");
            return;
        }
    }

    /**
     * Test substitution for null Group.
     */
    @Test
    public void testSubstitutionForNullGroup() {
        try {
            _engine.substituteGroups(null);
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Allowing to peform substitution for null Groups.");
    }

    /**
     * Test substitution for empty Group.
     */
    @Test
    public void testSubstitutionForEmptyGroup() {
        try {
            _engine.substituteGroups(new ArrayList<String>(1));
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("Allowing to peform substitution for empty Groups.");
    }

    /**
     * Test substitution for invalid Group.
     */
    @Test
    public void testSubstitutionForInvalidGroup() {
        List<String> groupIDs = new ArrayList<String>(1);
        groupIDs.add("invalidGroup");
        try {
            _engine.substituteGroups(groupIDs);
        } catch (StringSubstitutionException e) {
            Assert.fail("Throwing exception if invalid Group id is passed for susbtitution.");
            return;
        }
    }

    /**
     * Test substitution for Group without any change pair.
     */
    @Test
    public void testSubstitutionForGroupWithoutChangePair() {
        List<String> groupIDs = new ArrayList<String>(1);
        groupIDs.add(GROUP_WITHOUT_CHANGE_PAIR);
        try {
            createTextFile();
            _engine.substituteGroups(groupIDs);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(_testFile)));
                String afterSubstitutionLine = null;
                int i = 0;
                while ((afterSubstitutionLine = reader.readLine()) != null) {
                    switch (i++)
                    {
                        case 0:
                            Assert.assertTrue(afterSubstitutionLine.equals("@ORACLE_HOME@ First word in first line"));
                            break;
                        case 1:
                            Assert.assertTrue(afterSubstitutionLine.equals("Second line last word @MW_HOME@"));
                            break;
                        default:
                            Assert.fail("Substitution happening for a Group without change pair.");
                            break;
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (Exception e) {
            Assert.fail("Throwing exception if Group without change pair undergo substitution.");
            return;
        }
    }

    /**
     * Test substitution for Group pointing to invalid file paths.
     */
    @Test
    public void testSubstitutionForGroupInvalidFilePath() {
        List<String> groupIDs = new ArrayList<String>(1);
        groupIDs.add(GROUP_WITH_INVALID_FILE_PATHS);
        try {
            _engine.substituteGroups(groupIDs);
        } catch (StringSubstitutionException e) {
            Assert.fail("Throwing exception if Group having invalid file paths undergo substitution.");
            return;
        }
    }

    /**
     * Test substitution for empty Group.
     */
    @Test
    public void testSubstitutionForGroupWithoutFiles() {
        List<String> groupIDs = new ArrayList<String>(1);
        groupIDs.add(GROUP_WITHOUT_FILES);
        try {
            _engine.substituteGroups(groupIDs);
        } catch (StringSubstitutionException e) {
            Assert.fail("Throwing exception if Group without any substitutable entries undergo substitution.");
            return;
        }
    }

    /**
     * Creates text file.
     */
    private void createTextFile() {
        BufferedWriter writer = null;
        try {
            if (_testFile != null) {
                destroy();
            }
            _testFile = new File(_testFileName);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_testFile)));
            writer.write("@ORACLE_HOME@ First word in first line");
            writer.newLine();
            writer.write("Second line last word @MW_HOME@");
            writer.close();
        } catch (Exception e) {
            Assert.fail("Not able to create test Text file", e);
        }
    }

    /**
     * Custom implementation of {@link AttributePreprocessor}.
     */
    private class CustomAttributePreprocessor extends AttributePreprocessorImpl {
        private String testFilePath;

        CustomAttributePreprocessor(Map<String, String> lookUpMap) {
            super(lookUpMap);
            if (_testFile == null) {
                createTextFile();
            }
            testFilePath = _testFile.getAbsolutePath().replace(File.separator + _testFileName, "");
            lookUpMap.put("ARCHIVE_DIR", _archiveDirPath);
            lookUpMap.put("TEST_FILE_DIR", testFilePath);
        }
    }

    /**
     * Delete test file after test case executions.
     */
    @AfterTest
    public void destroy() {
        if (_testFile != null && _testFile.exists()) {
            if(!_testFile.delete()) {
                System.out.println("Not able to delete the temp file : " + _testFile.getAbsolutePath());
                _testFile.deleteOnExit();
            }
        }
        _testFile = null;
    }
}