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
import java.net.URL;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.stringsubs.Substitutable;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.FileEntry;

/**
 * Unit test for {@link FileEntryFactory} functionality.
 */
public class TestFileEntryFactory {

    private static final String _qualifiedClassName = TestFileEntryFactory.class.getName().replace('.', '/') + ".class";
    private FileEntryFactory _factory;
    private File _classFile;

    @BeforeClass
    public void init() {
        URL url = TestFileEntryFactory.class.getClassLoader().getResource(_qualifiedClassName);
        _factory = new FileEntryFactory();
        _classFile = new File(url.getPath());
    }

    /**
     * Test get file by mentioning the path of an directory.
     */
    @Test
    public void testGetFileFromDir() {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setName(_classFile.getParentFile().getAbsolutePath());
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        Assert.assertTrue(!substitutables.isEmpty());
        boolean fileFound = false;
        for (Substitutable substitutable : substitutables) {
            if (substitutable.getName().endsWith(_classFile.getAbsolutePath())) {
                fileFound = true;
                break;
            }
        }
        Assert.assertTrue(fileFound);
    }

    /**
     * Test get file by mentioning the absolute path of an file.
     */
    @Test
    public void testGetFile() {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setName(_classFile.getAbsolutePath());
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        Assert.assertTrue(!substitutables.isEmpty());
        Assert.assertTrue(substitutables.size() == 1);
        Assert.assertTrue(substitutables.get(0).getName().equals(_classFile.getAbsolutePath()));
    }

    /**
     * Test get file by using wild card.
     */
    @Test
    public void testGetFilesUsingWildCard() {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setName(_classFile.getParentFile().getAbsolutePath() + File.separator + "Test*");
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        Assert.assertTrue(!substitutables.isEmpty());
        boolean validResult = true;
        for (Substitutable substitutable : substitutables) {
            if (!(new File(substitutable.getName())).getName().startsWith("Test")) {
                validResult = false;
                break;
            }
        }
        Assert.assertTrue(validResult);
    }

    /**
     * Test get file by using wild card in between file path.
     */
    @Test
    public void testGetFilesUsingWildCardBetweenPath() {
        FileEntry fileEntry = new FileEntry();
        File parentFile = _classFile.getParentFile();
        File grandParentFile = parentFile.getParentFile();
        if (grandParentFile == null || !grandParentFile.exists()) {
            return;
        }
        String className = this.getClass().getSimpleName() + ".class";
        fileEntry.setName(grandParentFile.getAbsolutePath() + File.separator + "*" + File.separator + className);
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        Assert.assertTrue(!substitutables.isEmpty());
        Assert.assertTrue(substitutables.size() == 1);
        Assert.assertTrue((new File(substitutables.get(0).getName())).getName().equals(className));
    }

    /**
     * Test get file by using regex pattern.
     */
    @Test
    public void testGetFilesUsingRegex() {
        FileEntry fileEntry = new FileEntry();
        if (!_classFile.exists()) {
            Assert.fail("Not able to locate Test class :" + TestFileEntryFactory.class.getSimpleName());
        }
        fileEntry.setName(_classFile.getParentFile().getAbsolutePath() + File.separator + "(.*+)");
        fileEntry.setRegex("yes");
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        boolean fileFound = false;
        for (Substitutable substitutable : substitutables) {
            if (substitutable.getName().endsWith(_classFile.getAbsolutePath())) {
                fileFound = true;
                break;
            }
        }
        Assert.assertTrue(fileFound);
    }

    /**
     * Test get files for invalid file name.
     */
    @Test
    public void testGetFileInvalidInput() {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setName(_classFile.getAbsolutePath() + File.separator + "zzzzzzzzz.class");
        List<Substitutable> substitutables = _factory.getFileElements(fileEntry);
        Assert.assertTrue(substitutables.isEmpty());
    }
}