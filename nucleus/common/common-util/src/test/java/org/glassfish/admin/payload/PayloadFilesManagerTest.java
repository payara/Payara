/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.payload;

import com.sun.enterprise.util.io.FileUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.Payload.Inbound;
import org.glassfish.api.admin.Payload.Outbound;
import org.glassfish.api.admin.Payload.Part;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tjquinn
 */
public class PayloadFilesManagerTest {

    private Logger defaultLogger = Logger.getAnonymousLogger();

    public PayloadFilesManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getOutputFileURI method, of class PayloadFilesManager.
     */
    @Test
    public void testGetOutputFileURI() throws Exception {
        ////System.out.println("getOutputFileURI");

        PayloadFilesManager.Temp instance = new PayloadFilesManager.Temp(Logger.getAnonymousLogger());
        try {
            String originalPath = "way/over/there/myApp.ear";
            Part testPart = PayloadImpl.Part.newInstance("text/plain", originalPath, null, "random content");
            URI result = instance.getOutputFileURI(testPart, testPart.getName());
            ////System.out.println("  " + originalPath + " -> " + result);
            assertTrue(result.toASCIIString().endsWith("/myApp.ear"));
        } finally {
            instance.cleanup();
        }

    }

    @Test
    public void testBraces() throws Exception {
        ////System.out.println("testBraces");

        final File tmpDir = File.createTempFile("gfpayl{braces}", "tmp");
        tmpDir.delete();
        tmpDir.mkdir();

        try {
            final PayloadFilesManager instance = new PayloadFilesManager.Perm(tmpDir,
                    null, Logger.getAnonymousLogger());
            final String originalPath = "some/path";
            final Part testPart = PayloadImpl.Part.newInstance("text/plain", originalPath, null, "random content");
            final URI result = instance.getOutputFileURI(testPart, testPart.getName());
            ////System.out.println("  " + originalPath + " -> " + result);
            assertFalse(result.toASCIIString().contains("{"));
        } finally {
            PayloadFilesManagerTest.cleanup(tmpDir);
        }
    }

    @Test
    public void testDiffFilesFromSamePath() throws Exception {
        new CommonTempTest() {

            @Override
            protected void addParts(Outbound ob,
                    PayloadFilesManager instance) throws Exception {
                final Properties props = fileXferProps();
                ob.addPart("text/plain", "dir/x.txt", props, "sample data");
                ob.addPart("text/plain", "dir/y.txt", props, "y content in same temp dir as dir/x.txt");
            }

            @Override
            protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                List<File> files = instance.processParts(ib);
                File parent = null;
                boolean success = true;
                // Make sure all files have the same parent - since in this test
                // they all came from the same path originally.
                for (File f : files) {
                    ////System.out.println("  " + f.toURI().toASCIIString());
                    if (parent == null) {
                        parent = f.getParentFile();
                    } else {
                        success &= (parent.equals(f.getParentFile()));
                    }
                }
                assertTrue("Failed because the temp files should have had the same parent", success);
            }

        }.run("diffFilesFromSamePath");
    }

    @Test
    public void testSameFilesInDiffPaths() throws Exception {
        new CommonTempTest() {

            @Override
            protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                final Properties props = fileXferProps();
                ob.addPart("text/plain", "here/x.txt", props, "data from here");
                ob.addPart("text/plain", "elsewhere/x.txt", props, "data from elsewhere");
            }

            @Override
            protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                List<File> files = instance.processParts(ib);
                boolean success = true;
                String fileName = null;
                List<File> parents = new ArrayList<File>();
                for (File f : files) {
                    if (fileName == null) {
                        fileName= f.getName();
                    } else {
                        success &= (f.getName().equals(fileName)) && ( ! parents.contains(f.getParentFile()));
                    }
                    ////System.out.println("  " + f.toURI().toASCIIString());
                }
                assertTrue("Failed because temp file names did not match or at least two had a parent in common", success);
            }

        }.run("sameFilesInDiffPaths");
    }

    @Test
    public void testLeadingSlashes() throws Exception {


        new CommonTempTest() {
            private static final String originalPath = "/here/x.txt";
            private final File originalFile = new File(originalPath);

            @Override
            protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                final Properties props = fileXferProps();
                ob.addPart("application/octet-stream", originalPath, props, "data from here");
            }

            @Override
            protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                List<File> files = instance.processParts(ib);
                ////System.out.println("  Original: " + originalFile.toURI().toASCIIString());

                for (File f : files) {
                    ////System.out.println("  Temp file: " + f.toURI().toASCIIString());
                    if (f.equals(originalFile)) {
                        fail("Temp file was created at original top-level path; should have been in a temp dir");
                    }
                }
            }
        }.run("testLeadingSlashes");
    }

    @Test
    public void testPathlessFile() throws Exception {
        new CommonTempTest() {

            @Override
            protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                final Properties props = fileXferProps();
                ob.addPart("application/octet-stream", "flat.txt", props, "flat data");
                ob.addPart("text/plain", "x/other.txt", props, "one level down");
            }

            @Override
            protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                List<File> files = instance.processParts(ib);
                boolean success = true;
                for (File f : files) {
                    if (f.getName().equals("flat.txt")) {
                        success &= (f.getParentFile().equals(instance.getTargetDir()));
                    }
                    ////System.out.println("  " + f.toURI().toASCIIString());
                }
                ////System.out.println("  Done");
                assertTrue("Flat file was not deposited in top-level temp directory", success);
            }
        }.run("testPathlessFile");
    }

//    @Test
//    public void testWindowsPath() throws Exception {
//        //System.out.println("testWindowsPath");
//        testForBadChars("C:\\Program Files\\someDir");
//    }
//
//    @Test
//    public void testNonWindowsPath() throws Exception {
//        //System.out.println("testNonWindowsPath");
//        testForBadChars("/Users/whoever/someDir");
//
//    }

    @Test
    public void simplePermanentTransferTest() throws Exception {
        final String FILE_A_PREFIX = "";
        final String FILE_A_NAME = "fileA.txt";
        final String FILE_B_PREFIX = "x/y/z";
        final String FILE_B_NAME = "fileB.txt";

        final Set<File> desiredResults = new HashSet<File>();

        /*
         * Create a directory into which we'll transfer some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            origDir.mkdir();

            final File fileA = new File(origDir, FILE_A_NAME);
            writeFile(fileA, "This is File A", "and another line");
            desiredResults.add(new File(targetDir.toURI().resolve(FILE_A_PREFIX + FILE_A_NAME)));

            final File fileB = new File(origDir, FILE_B_NAME);
            desiredResults.add(new File(targetDir.toURI().resolve(FILE_B_PREFIX + FILE_B_NAME)));
            writeFile(fileB, "Here is File B", "which has an", "additional line");
        

            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(FILE_A_PREFIX + fileA.getName()),
                            "test-xfer",
                            fileA);
                    ob.attachFile(
                            "text/plain",
                            URI.create(FILE_B_PREFIX + fileB.getName()),
                            "test-xfer",
                            fileB);
                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    instance.processParts(ib);

                    final Set<File> missing = new HashSet<File>();
                    for (File f : desiredResults) {
                        if ( ! f.exists()) {
                            missing.add(f);
                        }
                    }
                    assertEquals("Unexpected missing files after extraction", Collections.EMPTY_SET, missing);

                }

                @Override
                protected void cleanup() {
                    for (File f : desiredResults) {
                        f.delete();
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentTransferTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void simplePermanentTransferAndRemovalTest() throws Exception {
        final String FILE_A_PREFIX = "";
        final String FILE_A_NAME = "fileA.txt";
        final String FILE_B_PREFIX = "x/y/z/";
        final String FILE_B_NAME = "fileB.txt";
        final String FILE_C_PREFIX = FILE_B_PREFIX;
        final String FILE_C_NAME = "fileC.txt";

        final Set<File> desiredPresent = new HashSet<File>();
        final Set<File> desiredAbsent = new HashSet<File>();

        /*
         * Create a directory into which we'll transfer some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            origDir.mkdir();

            final File fileA = new File(origDir, FILE_A_NAME);
            writeFile(fileA, "This is File A", "and another line");
            desiredPresent.add(new File(targetDir.toURI().resolve(FILE_A_PREFIX + FILE_A_NAME)));

            /*
             * In this test result, we want file B to be absent after we transfer
             * it (with files A and C) and then use a second PayloadFilesManager
             * to request B's removal.
             */
            final File fileB = new File(origDir, FILE_B_NAME);
            desiredAbsent.add(new File(targetDir.toURI().resolve(FILE_B_PREFIX + FILE_B_NAME)));
            writeFile(fileB, "Here is File B", "which has an", "additional line");

            final File fileC = new File(origDir, FILE_C_NAME);
            desiredPresent.add(new File(targetDir.toURI().resolve(FILE_C_PREFIX + FILE_C_NAME)));
            writeFile(fileC, "Here is File C", "which has an", "additional line", "even beyond what fileB has");


            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(FILE_A_PREFIX + fileA.getName()),
                            "test-xfer",
                            fileA);
                    ob.attachFile(
                            "text/plain",
                            URI.create(FILE_B_PREFIX + fileB.getName()),
                            "test-xfer",
                            fileB);
                    ob.attachFile(
                            "text/plain",
                            URI.create(FILE_C_PREFIX + fileC.getName()),
                            "test-xfer",
                            fileC);
                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    instance.processParts(ib);

                    /*
                     * Now ask another PayloadFilesManager to remove one of the
                     * just-extracted files.
                     */

                    Payload.Outbound ob = PayloadImpl.Outbound.newInstance();
                    ob.requestFileRemoval(
                            URI.create(FILE_B_PREFIX + FILE_B_NAME),
                            "removeTest" /* dataRequestName */,
                            null /* props */);

                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ob.writeTo(baos);
                    baos.close();

                    final PayloadFilesManager remover =
                            new PayloadFilesManager.Perm(instance.getTargetDir(), null,
                            Logger.getAnonymousLogger());

                    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    Payload.Inbound removerIB = PayloadImpl.Inbound.newInstance("application/zip", bais);

                    remover.processParts(removerIB);

                    final Set<File> missing = new HashSet<File>();
                    for (File f : desiredPresent) {
                        if ( ! f.exists()) {
                            missing.add(f);
                        }
                    }
                    assertEquals("Unexpected missing files after extraction", Collections.EMPTY_SET, missing);

                    final Set<File> unexpectedlyPresent = new HashSet<File>();
                    for (File f : desiredAbsent) {
                        if (f.exists()) {
                            unexpectedlyPresent.add(f);
                        }
                    }
                    assertEquals("Unexpected files remain after removal request",
                            Collections.EMPTY_SET, unexpectedlyPresent);
                }

                @Override
                protected void cleanup() {
                    for (File f : desiredPresent) {
                        f.delete();
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentTransferAndRemovalTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void simplePermanentDirWithNoSlashRemovalTest() throws Exception {
        final String DIR = "x/";
        final String DIR_WITH_NO_SLASH = "x";
        
        final String FILE_A_PREFIX = DIR;
        final String FILE_A_NAME = "fileA.txt";

        final Set<File> desiredAbsent = new HashSet<File>();
        /*
         * Create a directory into which we'll copy some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();
        origDir.mkdir();

        final File dir = new File(origDir, DIR);
        dir.mkdir();
        
        final File fileA = new File(dir, FILE_A_NAME);

        writeFile(fileA, "This is FileA", "with two lines of content");

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            desiredAbsent.add(new File(targetDir.toURI().resolve(FILE_A_PREFIX + FILE_A_NAME)));

            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(DIR),
                            "test-xfer",
                            dir,
                            true /* isRecursive */);

                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    instance.processParts(ib);

//                    listDir("After creation, before deletion", myTargetDir);
                    /*
                     * Now ask another PayloadFilesManager to remove a directory
                     * recursively.
                     */

                    Payload.Outbound ob = PayloadImpl.Outbound.newInstance();
                    ob.requestFileRemoval(
                            URI.create(DIR_WITH_NO_SLASH),
                            "removeTest" /* dataRequestName */,
                            null /* props */,
                            true /* isRecursive */);

                    ob.requestFileRemoval(
                            URI.create("notThere"),
                            "removeTest",
                            null,
                            true);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ob.writeTo(baos);
                    baos.close();

                    final PayloadFilesManager remover =
                            new PayloadFilesManager.Perm(instance.getTargetDir(), null,
                            debugLogger());

                    final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                    Payload.Inbound removerIB = PayloadImpl.Inbound.newInstance("application/zip", bais);

                    remover.processParts(removerIB);

//                    listDir("After deletion" , myTargetDir);

                    final Set<File> unexpectedlyPresent = new HashSet<File>();
                    for (File f : desiredAbsent) {
                        if (f.exists()) {
                            unexpectedlyPresent.add(f);
                        }
                    }
                    assertEquals("Unexpected files remain after removal request",
                            Collections.EMPTY_SET, unexpectedlyPresent);
                }

                @Override
                protected void cleanup() {
                    for (File f : desiredAbsent) {
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentDirWithNoSlashRemovalTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void recursiveReplacementTest() throws Exception {

        /*
         * Populate the target directory with a subdirectory containing a file,
         * then replace the subdirectory via a replacement request in a Payload.
         */
        final String DIR = "x/";

        final String FILE_A_PREFIX = DIR;
        final String FILE_A_NAME = "fileA.txt";

        final String FILE_B_PREFIX = DIR;
        final String FILE_B_NAME = "fileB.txt";

        final Set<File> desiredAbsent = new HashSet<File>();
        final Set<File> desiredPresent = new HashSet<File>();

        final File targetDir = File.createTempFile("tgt", "");
        targetDir.delete();
        targetDir.mkdir();

        final File dir = new File(targetDir, DIR);
        dir.mkdir();

        final File fileA = new File(dir, FILE_A_NAME);
        writeFile(fileA, "This is FileA", "with two lines of content");

        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();
        origDir.mkdir();

        final File origSubDir = new File(origDir, DIR);
        origSubDir.mkdirs();
        final File fileB = new File(origSubDir, FILE_B_NAME);
        writeFile(fileB, "This is FileB", "with yet another", "line of content");


        try {
            desiredPresent.add(new File(targetDir.toURI().resolve(FILE_B_PREFIX + FILE_B_NAME)));
            desiredAbsent.add(new File(targetDir.toURI().resolve(FILE_A_PREFIX + FILE_A_NAME)));

            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.requestFileReplacement(
                            "application/octet-stream",
                            URI.create(DIR),
                            "test-xfer",
                            null, /* props */
                            origSubDir,
                            true /* isRecursive */);

                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {

                    listDir("After creation, before deletion", myTargetDir);

                    /*
                     * Process files.
                     */
                    instance.processParts(ib);

                    listDir("After deletion" , myTargetDir);

                    final Set<File> unexpectedlyPresent = new HashSet<File>();
                    for (File f : desiredAbsent) {
                        if (f.exists()) {
                            unexpectedlyPresent.add(f);
                        }
                    }
                    assertEquals("Unexpected files remain after replacement request",
                            Collections.EMPTY_SET, unexpectedlyPresent);

                    final Set<File> unexpectedlyAbsent = new HashSet<File>();
                    for (File f : desiredPresent) {
                        if ( ! f.exists()) {
                            unexpectedlyAbsent.add(f);
                        }
                    }
                    assertEquals("Unexpected files absent after replacement request",
                            Collections.EMPTY_SET, unexpectedlyAbsent);
                }

                @Override
                protected void cleanup() {
                    for (File f : desiredAbsent) {
                        if (f.exists()) {
                            f.delete();
                        }
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("replacementTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    private static void listDir(final String title, final File dir) {
        ////System.out.println(title);
        listDir(dir);
        ////System.out.println();
    }

    private static void listDir(final File dir) {
        if ( ! dir.exists()) {
            //System.out.println("Directory  " + dir.getAbsolutePath() + " does not exist");
        } else {
            for (File f : dir.listFiles()) {
                //System.out.println((f.isDirectory() ? "dir " : "    ") + f.getAbsolutePath());
                if (f.isDirectory()) {
                    listDir(f);
                }
            }
        }
    }

    @Test
    public void simplePermanentTransferDirTest() throws Exception {
        final String DIR = "x/y/z/";
        final String FILE_PREFIX = "x/y/z/";
        final String FILE_NAME = "fileB.txt";

        final Set<File> desiredResults = new HashSet<File>();

        /*
         * Create a directory into which we'll transfer some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            origDir.mkdir();

            /*
             * Add the directory first, then add a file in the directory.  That
             * will let us check to make sure the PayloadFileManager set the
             * lastModified time on the directory correctly.
             */
            final URI dirURI = URI.create(DIR);
            final File dir = new File(origDir, DIR);
            dir.mkdirs();
            desiredResults.add(dir);

            final File file = new File(dir, FILE_NAME);
            desiredResults.add(new File(targetDir.toURI().resolve(FILE_PREFIX + FILE_NAME)));
            writeFile(file, "Here is the File", "which has an", "additional line");
            final long dirCreationTime = dir.lastModified();


            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(DIR),
                            "test-xfer",
                            dir);
                    ob.attachFile(
                            "text/plain",
                            URI.create(FILE_PREFIX + file.getName()),
                            "test-xfer",
                            file);
                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    instance.processParts(ib);

                    final URI extractedDirURI = myTargetDir.toURI().resolve(dirURI);
                    final File extractedDir = new File(extractedDirURI);
                    final long extractedLastModified = extractedDir.lastModified();

                    assertEquals("Directory lastModified mismatch after extraction",
                            dirCreationTime, extractedLastModified);

                }

                @Override
                protected void cleanup() {
                    for (File f : desiredResults) {
                        f.delete();
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentTransferDirTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void simplePermanentRecursiveTransferTest() throws Exception {
        final String DIR = "x/";
        final String Y_SUBDIR = "y/";
        final String Z_SUBDIR = "z/";
        final String FILE_A_PREFIX = DIR + Y_SUBDIR;
        final String FILE_A_NAME = "fileA.txt";

        final String FILE_B_PREFIX = DIR + Y_SUBDIR + Z_SUBDIR;
        final String FILE_B_NAME = "fileB.txt";

        final Set<File> desiredResults = new HashSet<File>();

        /*
         * Create a directory into which we'll copy some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();
        origDir.mkdir();

        final File dir = new File(origDir, DIR);
        dir.mkdir();
        final File ySubdir = new File(dir, Y_SUBDIR);
        ySubdir.mkdir();
        final File zSubdir = new File(dir, Y_SUBDIR + Z_SUBDIR);
        zSubdir.mkdir();

        final File fileA = new File(ySubdir, FILE_A_NAME);
        final File fileB = new File(zSubdir, FILE_B_NAME);

        writeFile(fileA, "This is FileA", "with two lines of content");
        writeFile(fileB, "This is FileB", "with a" , "third line");

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            desiredResults.add(new File(targetDir.toURI().resolve(FILE_A_PREFIX + FILE_A_NAME)));
            desiredResults.add(new File(targetDir.toURI().resolve(FILE_B_PREFIX + FILE_B_NAME)));

            /*
             * Add the original directory recursively.
             */


            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create("x/"),
                            "test-xfer",
                            dir,
                            true /* isRecursive */);

                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    final List<File> files = instance.processParts(ib);

                    final Set<File> missing = new HashSet<File>();
                    for (File f : desiredResults) {
                        if ( ! f.exists()) {
                            missing.add(f);
                        }
                    }
                    assertEquals("Unexpected missing files after extraction", Collections.EMPTY_SET, missing);


                }

                @Override
                protected void cleanup() {
                    for (File f : desiredResults) {
                        f.delete();
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentRecursiveTransferTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void simplePermanentRecursiveTransferDirOnlyTest() throws Exception {
        final String DIR = "x/";
        final String Y_SUBDIR = "y/";
        final String Z_SUBDIR = "z/";

        final Set<File> desiredResults = new HashSet<File>();

        /*
         * Create a directory into which we'll copy some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();
        origDir.mkdir();

        final File dir = new File(origDir, DIR);
        dir.mkdir();
        final File ySubdir = new File(dir, Y_SUBDIR);
        ySubdir.mkdir();
        final File zSubdir = new File(dir, Y_SUBDIR + Z_SUBDIR);
        zSubdir.mkdir();

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            desiredResults.add(new File(targetDir.toURI().resolve(DIR)));
            desiredResults.add(new File(targetDir.toURI().resolve(DIR + Y_SUBDIR)));
            desiredResults.add(new File(targetDir.toURI().resolve(DIR + Y_SUBDIR + Z_SUBDIR)));

            /*
             * Add the original directory recursively.
             */


            new CommonPermTest() {

                @Override
                protected CommonPermTest init(File targetDir) {
                    super.init(targetDir);

                    return this;
                }

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(DIR),
                            "test-xfer",
                            dir,
                            true /* isRecursive */);

                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    instance.processParts(ib);

                    final Set<File> missing = new HashSet<File>();
                    for (File f : desiredResults) {
                        if ( ! f.exists()) {
                            missing.add(f);
                        }
                    }
                    assertEquals("Unexpected missing files after extraction", Collections.EMPTY_SET, missing);


                }

                @Override
                protected void cleanup() {
                    for (File f : desiredResults) {
                        f.delete();
                    }
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.init(targetDir).run("simplePermanentRecursiveTransferDirOnlyTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    @Test
    public void simpleTempRecursiveTransferDirOnlyTest() throws Exception {
        final String DIR = "x/";
        final String Y_SUBDIR = "y/";
        final String Z_SUBDIR = "z/";

        final List<String> desiredResultsNamePrefixes = new ArrayList<String>();

        /*
         * Create a directory into which we'll copy some small files.
         */
        final File origDir = File.createTempFile("pfm", "");
        origDir.delete();
        origDir.mkdir();

        final File dir = new File(origDir, DIR);
        dir.mkdir();
        final File ySubdir = new File(dir, Y_SUBDIR);
        ySubdir.mkdir();
        final File zSubdir = new File(ySubdir, Z_SUBDIR);
        zSubdir.mkdir();

        File targetDir = null;

        try {
            /*
             * Choose the directory into which we want the PayloadFilesManager to
             * deliver the files.
             */

            targetDir = File.createTempFile("tgt", "");
            targetDir.delete();
            targetDir.mkdir();

            desiredResultsNamePrefixes.add("/x");
            desiredResultsNamePrefixes.add("/x/y");
            desiredResultsNamePrefixes.add("/x/y/z");

            /*
             * Add the original directory recursively.
             */


            new CommonTempTest() {

                @Override
                protected void addParts(Outbound ob, PayloadFilesManager instance) throws Exception {
                    ob.attachFile(
                            "application/octet-stream",
                            URI.create(DIR),
                            "test-xfer",
                            dir,
                            true /* isRecursive */);

                }

                @Override
                protected void checkResults(Inbound ib, PayloadFilesManager instance) throws Exception {
                    /*
                     * Extract files to where we want them.
                     */
                    final List<File> files = instance.processParts(ib);

                  checkNextFile:
                    for (File f : files) {

                        for (ListIterator<String> it = desiredResultsNamePrefixes.listIterator(); it.hasNext();) {
                            final String desiredPrefix = it.next().replace("/", File.separator);
                            if (f.getPath().contains(desiredPrefix)) {
                                it.remove();
                                continue checkNextFile;
                            }
                        }
                    }

                    assertEquals("Unexpected missing files after extraction", Collections.EMPTY_LIST, desiredResultsNamePrefixes);


                }

                @Override
                protected void doCleanup() {
                    PayloadFilesManagerTest.cleanup(origDir);
                }

            }.run("simpleTempRecursiveTransferDirOnlyTest");
        } finally {
            if (targetDir != null) {
                FileUtils.whack(targetDir);
            }
        }
    }

    private static void cleanup(final File... files) {
        boolean ok = true;
        for (File f : files) {
            /*
             * If this is a directory we've been asked to clean up then
             * clean it recursively.
             */
            if (f.isDirectory()) {
                if ( ! FileUtils.whack(f)) {
                    System.err.println("** Could not whack " + f.getAbsolutePath());
                    ok = false;
                }
            } else {
                if ( f.exists() && ! f.delete()) {
                    System.err.println("** Could not clean up " + f.getAbsolutePath());
                    ok = false;
                    f.deleteOnExit();
                }
            }
        }
        if ( ! ok) {
            new Exception().printStackTrace();
        }
    }
    
    private void writeFile(final File file, final String... content) throws FileNotFoundException {
        PrintStream ps = new PrintStream(file);
        for (String s : content) {
            ps.println(s);
        }
        ps.close();
    }

    private Properties fileXferProps() {
        final Properties props = new Properties();
        props.setProperty("data-request-type", "file-xfer");
        return props;
    }

//    private void testForBadChars(String initialPath) {
//        URI uri = null;
//        URI targetDirURI = null;
//        try {
//            PayloadFilesManager.Temp instance = new PayloadFilesManager.Temp(Logger.getAnonymousLogger());
//            uri = URI.create(initialPath.replace(File.separator, "/"));
//            targetDirURI = instance.getTargetDir().toURI();
//
//            //System.out.println("  " + initialPath + " -> " + uri.toASCIIString());
//            String uriString = targetDirURI.relativize(uri).toASCIIString();
//
//            // trim the trailing slash for the directory
//            if (uriString.endsWith("/")) {
//                uriString = uriString.substring(0, uriString.length() - 1);
//            }
//            assertFalse("path " + uriString + " still contains bad character(s)",
//                    uriString.contains("/") ||
//                    uriString.contains("\\") ||
//                    (uriString.contains(":") && File.separatorChar == '\\'));        } catch (Exception e) {
//            fail("unexpected exception " + e.getMessage());
//        }
//    }
    
    private abstract class CommonTest {
        protected final static String payloadType = "application/zip";

        protected abstract void addParts(final Payload.Outbound ob,
                final PayloadFilesManager instance) throws Exception;

        protected abstract void checkResults(final Payload.Inbound ib,
                final PayloadFilesManager instance) throws Exception;

        protected abstract PayloadFilesManager instance() throws IOException;

        protected abstract void cleanup();

        public void run(String testName) throws Exception {
            File tempZipFile = null;

            //System.out.println(testName);


            try {
                tempZipFile = File.createTempFile("testzip", ".zip");
                Payload.Outbound ob = PayloadImpl.Outbound.newInstance();

                addParts(ob, instance());

                OutputStream os;
                ob.writeTo(os = new BufferedOutputStream(new FileOutputStream(tempZipFile)));
                os.close();

                Payload.Inbound ib = PayloadImpl.Inbound.newInstance(payloadType, new BufferedInputStream(new FileInputStream(tempZipFile)));

                checkResults(ib, instance());

                cleanup();
            } finally {
                if (tempZipFile != null) {
                    tempZipFile.delete();
                }
            }

        }
    }
    
    private abstract class CommonTempTest extends CommonTest {

        private List<PayloadFilesManager.Temp> tempInstances = new ArrayList<PayloadFilesManager.Temp>();

        @Override
        protected PayloadFilesManager instance() throws IOException {
            final PayloadFilesManager.Temp tempInstance = new PayloadFilesManager.Temp(Logger.getAnonymousLogger());
            tempInstances.add(tempInstance);
//            System.err.println("** Temp recording " + tempInstance.getTargetDir().getAbsolutePath());
            return tempInstance;
        }

        @Override
        protected void cleanup() {
            doCleanup();
            for (PayloadFilesManager.Temp tempInstance : tempInstances) {
//                System.err.println("** Temp cleaning " + tempInstance.getTargetDir().getAbsolutePath());
                tempInstance.cleanup();
            }
        }

        protected void doCleanup() {}
    }

    private abstract class CommonPermTest extends CommonTest {
        private PayloadFilesManager.Perm permInstance;
        protected File myTargetDir;

        protected CommonPermTest init(final File targetDir) {
            permInstance = new PayloadFilesManager.Perm(targetDir, null, debugLogger());
            myTargetDir = targetDir;
//            System.err.println("** Perm creating " + permInstance.getTargetDir().getAbsolutePath());
            return this;
        }

        @Override
        protected PayloadFilesManager instance() throws IOException {
            return permInstance;
        }
    }

    /**
     * Makes it easy to turn on or off FINE-level logging in the PayloadFilesManager
     * during the tests.
     * <p>
     * Uncomment the comment lines below to turn on FINE logging for the test.
     *
     *
     * @return
     */
    private static Logger debugLogger() {
        final Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.FINE);
        logger.addHandler(new Handler() {

                        {
//                            this.setLevel(Level.INFO);
                            this.setLevel(Level.FINE);
                        }

                        @Override
                        public void publish(LogRecord record) {
                            //System.out.println(record.getMessage());
                        }

                        @Override
                        public void flush() {
                            //System.out.flush();
                        }

                        @Override
                        public void close() throws SecurityException {
                            // no-op
                        }
                    });
        return logger;
    }

}
