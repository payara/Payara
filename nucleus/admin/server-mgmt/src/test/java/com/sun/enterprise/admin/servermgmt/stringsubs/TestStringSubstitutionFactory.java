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
package com.sun.enterprise.admin.servermgmt.stringsubs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.enterprise.admin.servermgmt.stringsubs.impl.AttributePreprocessorImpl;
import com.sun.enterprise.admin.servermgmt.stringsubs.impl.SubstituableFactoryImpl;
import com.sun.enterprise.admin.servermgmt.stringsubs.impl.TestStringSubstitutionEngine;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.FileEntry;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Group;

/**
 * Unit Test class for {@link StringSubstitutionFactory}.
 */
public class TestStringSubstitutionFactory {

    private final static String _stringSubsPath = TestStringSubstitutionEngine.class.getPackage().getName().replace(".", "/")
            + "/stringsubs.xml";
    private Map<String, String> _substitutionMap = new HashMap<String, String>();
    private static final String _testFileName = "testStringSubs.txt";
    private static final String _testArchiveName = "testStringSubsArchive.jar";
    private static final String VALID_GROUP_ID = "valid_group";
    private String _testFileDirPath = null;

    @BeforeClass
    public void init() throws Exception {
        URL url = TestStringSubstitutionFactory.class.getClassLoader().getResource(_stringSubsPath);
        _testFileDirPath = new File(url.getPath()).getParentFile().getAbsolutePath();
        _substitutionMap.put("JAVA", "REPLACED_JAVA");
        _substitutionMap.put("JAVA_HOME", "REPLACED_JAVA_HOME");
        _substitutionMap.put("MW_HOME", "REPLACED_MW_HOME");
        _substitutionMap.put("TEST_FILE_DIR_PATH", _testFileDirPath);
    }

    /**
     * Test String substitution for invalid stream.
     */
    @Test
    public void testStringSubstitutorInvalidStream() {
        StringBuffer pathBuffer = new StringBuffer();
        pathBuffer.append(TestStringSubstitutionFactory.class.getPackage().getName().replace(".", "/"));
        pathBuffer.append(File.separator);
        pathBuffer.append(this.getClass().getSimpleName());
        pathBuffer.append(".class");
        InputStream invalidStream = TestStringSubstitutionFactory.class.getClassLoader().getResourceAsStream(pathBuffer.toString());
        try {
            StringSubstitutionFactory.createStringSubstitutor(invalidStream);
        }
        catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("No exception thrown for invalid stream.");
    }

    /**
     * Test String substitution for null stream.
     */
    @Test
    public void testStringSubstitutorNullStream() {
        try {
            StringSubstitutionFactory.createStringSubstitutor(null);
        } catch (StringSubstitutionException e) {
            return;
        }
        Assert.fail("No exception thrown for null stream.");
    }

    /**
     * Test String substitution for valid stream.
     */
    @Test
    public void testStringSubstitutorValidStream() {
        InputStream invalidStream = TestStringSubstitutionFactory.class.getClassLoader().
                getResourceAsStream(_stringSubsPath);
        try {
            StringSubstitutor substitutor = StringSubstitutionFactory.createStringSubstitutor(invalidStream);
            substitutor.setAttributePreprocessor(new AttributePreprocessorImpl(_substitutionMap));
            backUpTestFile();
            substitutor.substituteAll();
            for (Group group : substitutor.getStringSubsDefinition().getGroup()) {
                if (group.getId().equals(VALID_GROUP_ID)) {
                    validateSubstitutedArchiveEntries(group);
                    for (FileEntry fileEntry : group.getFileEntry()) {
                        if (fileEntry.getName().equalsIgnoreCase(_testFileName) &&
                                !validateTestFile(new File(fileEntry.getName()))) {
                            Assert.fail("Substitution failed in the test file.");
                            break;
                        }
                    }
                }
            }
            restoreTestFile();
        } catch (StringSubstitutionException e) {
            Assert.fail("Exception occurred during string substitution process.", e);
        }
    }

    /**
     * Validate if the substitution occurred properly in the test file.
     */
    private boolean validateTestFile(File testFile) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(testFile)));
            String afterSubstitutionLine = null;
            int i = 0;
            while ((afterSubstitutionLine = reader.readLine()) != null) {
                switch (i++) {
                    case 0:
                        if (!afterSubstitutionLine.equals("Substitute REPLACED_JAVA_HOME REPLACED_JAVA @MW_")) {
                            return false;
                        }
                        break;
                    case 1:
                        if (!afterSubstitutionLine.equals("HOME@")) {
                            return false;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validate all the substitutable archive entries.
     */
    private void validateSubstitutedArchiveEntries(Group group) {
        List<? extends Substitutable> substituables = new SubstituableFactoryImpl().getArchiveEntrySubstitutable(group.getArchive().get(0));
        for (Substitutable substituable : substituables) {
            Assert.assertTrue(validateTestFile(new File(substituable.getName())));
            substituable.finish();
        }
    }

    /**
     * Restore the archive by performing reverse substitution.
     */
    private void backUpTestFile() {
        try {
            File testDir = new File(_testFileDirPath);
            if (testDir.isDirectory()) {
                for (File file : testDir.listFiles()) {
                    if (file.getName().endsWith(_testArchiveName)
                            || file.getName().endsWith(_testFileName)) {
                        copy(file, new File(file.getAbsolutePath() + ".bkp") ,true);
                    }
                }
            }
        } catch (Exception e) {
            Assert.fail("Error occured while restoring the archive after subsitution.", e);
        }
    }

    private void restoreTestFile() {
        try {
            File testDir = new File(_testFileDirPath);
            if (testDir.isDirectory()) {
                for (File file : testDir.listFiles()) {
                    if (file.getName().endsWith(_testArchiveName)
                            || file.getName().endsWith(_testFileName)) {
                        file.delete();
                    }
                }
                for (File file : testDir.listFiles()) {
                    if (file.getAbsolutePath().endsWith(".bkp")) {
                        file.renameTo(new File(file.getAbsolutePath().replace(".bkp", "")));
                    }
                }
            }
        } catch (Exception e) {
            Assert.fail("Error occured while restoring the archive after subsitution.", e);
        }
    }

    /**
     * Copies a file.
     *
     * @param from the file to copy from.
     * @param to   the file to copy to.
     * @param mkdirs if parent directory should be created.
     * @throws IOException when an error occurs while trying to copy the file.
     */
    public void copy(File from, File to, boolean mkdirs)
            throws IOException {
        if (from == null) {
            throw new NullPointerException("The source file was null.");
        }

        if (to == null) {
            throw new NullPointerException("The destination file was null.");
        }

        if (from.isDirectory()) {
            throw new IOException("The source file was a directory, FileCopy does not support directory copies.");
        }

        if (to.isDirectory()) {
            to = new File(to, from.getName());
        }

        if (mkdirs) {
            File parent = to.getParentFile();
            if (parent != null)
            {
                parent.mkdirs();
            }
        }

        FileInputStream in = new FileInputStream(from);
        FileChannel fin = in.getChannel();
        FileOutputStream out = new FileOutputStream(to);
        FileChannel fout = out.getChannel();

        long size = fin.size();
        long position = 0;
        while (position < size) {
            position += fin.transferTo(position, 1000, fout);
        }
        fin.close();
        in.close();
        fout.close();
        out.close();
    }
}