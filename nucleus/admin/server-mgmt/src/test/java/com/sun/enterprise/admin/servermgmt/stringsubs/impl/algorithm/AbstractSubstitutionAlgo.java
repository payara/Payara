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
package com.sun.enterprise.admin.servermgmt.stringsubs.impl.algorithm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.stringsubs.Substitutable;
import com.sun.enterprise.admin.servermgmt.stringsubs.SubstitutionAlgorithm;
import com.sun.enterprise.admin.servermgmt.stringsubs.impl.LargeFileSubstitutionHandler;
import com.sun.enterprise.admin.servermgmt.stringsubs.impl.SmallFileSubstitutionHandler;

/**
 * Abstract class to test substitution algorithm. Derived classes will
 * provide the implementation of {@link SubstitutionAlgorithm} use to
 * execute the test cases, by defining the abstract method
 * {@link AbstractSubstitutionAlgo#getAlgorithm(Map)}
 */
public abstract class AbstractSubstitutionAlgo
{
    private String _testFileName = "testStringSubs.txt";
    private File _testFile;
    private SubstitutionAlgorithm _algorithm;

    /**
     * Create test file used as a input file for string substitution.
     */
    @BeforeClass
    public void init() {
        Map<String, String> substitutionMap = new HashMap<String, String>();
        substitutionMap.put("line", "replacedLine");
        substitutionMap.put("file", "testFile");
        substitutionMap.put("HTTP_PORT", "8080");
        substitutionMap.put("HTTPS_PORT", "8443");
        _algorithm = getAlgorithm(substitutionMap);
    }

    /**
     * Gets the substitution algorithm.
     *
     * @return Algorithm to perform substitution.
     */
    protected abstract SubstitutionAlgorithm getAlgorithm(Map<String, String> substitutionMap);

    /**
     * Test the {@link SubstitutionAlgorithm} instance for null map.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSubstitutionForNullMap() {
        getAlgorithm(null);
    }

    /**
     * Test the {@link SubstitutionAlgorithm} instance for empty map.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSubstitutionForEmptyMap() {
        getAlgorithm(new HashMap<String, String>());
    }

    /**
     * Test substitution for small text file.
     */
    @Test
    public void testSmallTextFileSubstitution() {
        createTextFile();
        Substitutable resolver = null;
        try {
            resolver = new SmallFileSubstitutionHandler(_testFile); 
            _algorithm.substitute(resolver);
            resolver.finish();
        } catch (Exception e) {
            Assert.fail("Test case execution failed", e);
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(_testFile)));
            String afterSubstitutionLine = null;
            int i = 0;
            while ((afterSubstitutionLine = reader.readLine()) != null) {
                switch (i++)
                {
                    case 0:
                        Assert.assertEquals(afterSubstitutionLine, "First replacedLine in testFile repeat First replacedLine in testFile");
                        break;
                    case 1:
                        Assert.assertEquals(afterSubstitutionLine, "Second replacedLine in testFile");
                        break;
                    default:
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Assert.fail("Not able to read test file");
        } finally {
            _testFile.delete();
        }
    }

    /**
     * Test substitution for small XML file.
     */
    @Test
    public void testSmallXMLFileSubstitution() {
        String fileName = _testFileName.replace(".txt", ".xml");
        createXMLFile(fileName);
        Substitutable resolver = null; 
        try {
            resolver = new SmallFileSubstitutionHandler(new File(fileName)); 
            _algorithm.substitute(resolver);
            resolver.finish();
        } catch (Exception e) {
            Assert.fail("Test case failed", e);
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(_testFile)));
            String afterSubstitutionLine = null;
            int i = 0;
            while ((afterSubstitutionLine = reader.readLine()) != null) {
                switch (i++)
                {
                    case 1:
                        Assert.assertEquals(afterSubstitutionLine, 
                                "<port name=\"http\" value=\"8080\"></port>");
                        break;
                    case 2:
                        Assert.assertEquals(afterSubstitutionLine, 
                                "<port name=\"https\" value=\"8443\"></port>");
                        break;
                    default:
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Assert.fail("Not able to read test file.", e);
        } finally {
            _testFile.delete();
        }
    }

    /**
     * Test substitution for large text file.
     */
    //@Test
    //TODO: Test case failing on hudson, Test case execution create temporary file
    // to perform substitution.
    public void testLargeTextFileSubstitution() {
        createTextFile();
        Substitutable resolver = null; 
        try {
            resolver = new LargeFileSubstitutionHandler(_testFile); 
            _algorithm.substitute(resolver);
            resolver.finish();
        } catch (Exception e) {
            Assert.fail("Test case failed : " + e.getMessage());
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(_testFileName))));
        } catch (FileNotFoundException e) {
            Assert.fail("Not able to locate test file : " + _testFileName, e);
        }
        String afterSubstitutionLine = null;
        try {
            int i = 0;
            while ((afterSubstitutionLine = reader.readLine()) != null) {
                switch (i++)
                {
                    case 0:
                        Assert.assertEquals(afterSubstitutionLine,
                                "First replacedLine in testFile repeat First replacedLine in testFile");
                        break;
                    case 1:
                        Assert.assertEquals(afterSubstitutionLine,
                                "Second replacedLine in testFile");
                        break;
                    default:
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Assert.fail("Not able to read test file");
        } finally {
            _testFile.delete();
        }
    }

    /**
     * Test substitution for large XML file.
     */
    //@Test
    //TODO: Test case failing on hudson, Test case execution create temporary file
    // to perform substitution.
    public void testLargeXMLFileSubstitution() {
        String fileName = _testFileName.replace(".txt", ".xml");
        createXMLFile(fileName);
        Substitutable resolver = null; 
        try {
            resolver = new LargeFileSubstitutionHandler(_testFile);
            _algorithm.substitute(resolver);
            resolver.finish();
        } catch (Exception e) {
            Assert.fail("Test case failed : " + e.getMessage());
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fileName))));
        } catch (FileNotFoundException e) {
            Assert.fail("Test case failed : " + e.getMessage());
        }
        String afterSubstitutionLine = null;
        try {
            int i = 0;
            while ((afterSubstitutionLine = reader.readLine()) != null) {
                switch (i++)
                {
                    case 1:
                        Assert.assertEquals(afterSubstitutionLine,
                                "<port name=\"http\" value=\"8080\"></port>");
                        break;
                    case 2:
                        Assert.assertEquals(afterSubstitutionLine,
                                "<port name=\"https\" value=\"8443\"></port>");
                        break;
                    default:
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Assert.fail("Not able to read test file");
        } finally {
            _testFile.delete();
        }
    }

    /**
     * Delete test file after test case executions.
     */
    @AfterTest
    public void destroy() {
        if (_testFile != null && _testFile.exists()) {
            if(!_testFile.delete())  {
                System.out.println("Not able to delete the temp file : " + _testFile.getAbsolutePath());
            }
        }
    }

    /**
     * Creates text file.
     */
    private void createTextFile() {
        BufferedWriter writer = null;
        try {
            _testFile = new File(_testFileName);
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_testFile)));
            writer.write("First line in file repeat First line in file");
            writer.newLine();
            writer.write("Second line in file");
            writer.close();
        } catch (Exception e) {
            Assert.fail("Not able to create test Text file : " + _testFile.getAbsolutePath() + e.getMessage());
        }
    }

    /**
     * Creates XML file.
     */
    private void createXMLFile(String fileName) {
        _testFile = new File(fileName);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_testFile)));
            writer.write(" <ports>");
            writer.newLine();
            writer.write("<port name=\"http\" value=\"HTTP_PORT\"></port>");
            writer.newLine();
            writer.write("<port name=\"https\" value=\"HTTPS_PORT\"></port>");
            writer.newLine();
            writer.write("</ports>");
            writer.close();
        } catch (Exception e) {
            Assert.fail("Not able to create test XML file : " + _testFile.getAbsolutePath() + e.getMessage());
        }
    }
}
