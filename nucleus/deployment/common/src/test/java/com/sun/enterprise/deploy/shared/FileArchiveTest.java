/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deploy.shared;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.MemoryHandler;
import com.sun.enterprise.util.OS;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import org.glassfish.tests.utils.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Tim Quinn
 */
public class FileArchiveTest {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    private final static String LINE_SEP = System.getProperty("line.separator");
    private final static String STALE_ENTRY = "oldLower/oldFile.txt";
    private final static String SUBARCHIVE_NAME = "subarch";

    private File archiveDir;
    private final Set<String> usualEntryNames =
            new HashSet<String>(Arrays.asList(new String[] {"sample.txt", "lower/other.txt"}));

    private final Set<String> usualExpectedEntryNames = initUsualExpectedEntryNames();
    private final Set<String> usualExpectedEntryNamesWithOverwrittenStaleEntry =
            initUsualExpectedEntryNamesWithOverwrittenStaleEntry();

    private final Set<String> usualSubarchiveEntryNames =
            new HashSet<String>(Arrays.asList(new String[] {"a.txt", "under/b.txt"}));

    private final Set<String> usualExpectedSubarchiveEntryNames = initUsualExpectedSubarchiveEntryNames();

    private Set<String> initUsualExpectedEntryNames() {
        final Set<String> expectedEntryNames = new HashSet<String>(usualEntryNames);
        expectedEntryNames.add("lower");
        return expectedEntryNames;
    }

    private Set<String> initUsualExpectedEntryNamesWithOverwrittenStaleEntry() {
        final Set<String> result = initUsualExpectedEntryNames();
        result.add(STALE_ENTRY);
        result.add("oldLower");
        return result;
    }

    private  Set<String> initUsualExpectedSubarchiveEntryNames() {
        final Set<String> result = new HashSet<String>(usualSubarchiveEntryNames);
        result.add("under");
        return result;
    }

    private static ServiceLocator habitat;
    private static ArchiveFactory archiveFactory;

    private static  RecordingHandler handler;
    private static MemoryHandler memoryHandler;
      
    public FileArchiveTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        habitat = Utils.getNewHabitat();
        archiveFactory = habitat.getService(ArchiveFactory.class);

        handler = new RecordingHandler();
        memoryHandler = new MemoryHandler(handler, 10, Level.ALL);
        deplLogger.addHandler(handler);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        archiveDir = tempDir();
    }

    @After
    public void tearDown() {
        if (archiveDir != null) {
            clean(archiveDir);
        }
        archiveDir = null;
    }

    private File tempDir() throws IOException {
        final File f = File.createTempFile("FileArch", "");
        f.delete();
        f.mkdir();
        return f;
    }

    private void clean(final File dir) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                clean(f);
            }
            if ( ! f.delete()) {
                f.deleteOnExit();
            }
        }
        if ( ! dir.delete()) {
            dir.deleteOnExit();
        };
    }


    private ReadableArchive createAndPopulateArchive(
            final Set<String> entryNames) throws Exception {

        WritableArchive instance = archiveFactory.createArchive(archiveDir.toURI()); //new FileArchive();
        instance.create(archiveDir.toURI());

        /*
         * Add some entries.
         */
        for (String entryName : entryNames) {
            instance.putNextEntry(entryName);
            instance.closeEntry();
        }

        instance.close();

        return archiveFactory.openArchive(archiveDir);
    }

    private ReadableArchive createAndPopulateSubarchive(
            final WritableArchive parent,
            final String subarchiveName,
            final Set<String> entryNames) throws Exception {
        final WritableArchive result = parent.createSubArchive(subarchiveName);
        for (String entryName : entryNames) {
            result.putNextEntry(entryName);
            result.closeEntry();
        }
        result.close();

        final ReadableArchive readableParent = archiveFactory.openArchive(parent.getURI());
        return readableParent.getSubArchive(subarchiveName);
    }

    private void createAndPopulateAndCheckArchive(
            final Set<String> entryNames) throws Exception {
        final ReadableArchive instance = createAndPopulateArchive(entryNames);
        
        checkArchive(instance, usualExpectedEntryNames);

    }

    private void checkArchive(final ReadableArchive instance,
            final Set<String> expectedEntryNames) {

        final Set<String> foundEntryNames = new HashSet<String>();

        for (Enumeration<String> e = instance.entries(); e.hasMoreElements(); ) {
            foundEntryNames.add(e.nextElement());
        }

        assertEquals("Missing or unexpected entry names reported", expectedEntryNames, foundEntryNames);
    }

    private void getListOfFiles(final FileArchive instance,
            final Set<String> expectedEntryNames,
            final Logger logger) {
        final List<String> foundEntryNames = new ArrayList<String>();

        instance.getListOfFiles(archiveDir, foundEntryNames, null, logger);

        assertEquals("Missing or unexpected entry names reported", expectedEntryNames, 
                new HashSet<String>(foundEntryNames));
    }

    private void getListOfFilesCheckForLogRecord(FileArchive instance, final Set<String> expectedEntryNames) throws IOException {
        handler.flush();
        getListOfFiles((FileArchive) instance, expectedEntryNames, deplLogger);
        if (handler.logRecords().size() != 1) {
            final StringBuilder sb = new StringBuilder();
            for (LogRecord record : handler.logRecords()) {
                sb.append(record.getLevel().getLocalizedName())
                        .append(": ")
                        .append(record.getMessage())
                        .append(LINE_SEP);
            }
            fail("Expected 1 log message but received " + handler.logRecords().size() + " as follows:" + LINE_SEP + sb.toString());
        }

        /*
         * We have a stale file under a stale directory.  Make sure a direct
         * request for the stale file fails.  (We know already from above that
         * getting the entries list triggers a warning about the skipped stale file.)
         */
        final InputStream is = instance.getEntry(STALE_ENTRY);
        assertNull("Incorrectly located stale FileArchive entry " + STALE_ENTRY, is);
    }



    /**
     * Computes the expected entry names for an archive which contains a subarchive.
     * <p>
     * The archive's entries method will report all the entries in the main
     * archive, plus the subarchive name, plus the entries in the subarchive.
     * @param expectedFromArchive entries from the main archive
     * @param subarchiveName name of the subarchive
     * @param expectedFromSubarchive entries in the subarchive
     * @return entry names that should be returned from the main archive's entries() method
     */
    private Set<String> expectedEntryNames(Set<String> expectedFromArchive, final String subarchiveName, Set<String>expectedFromSubarchive) {
        final Set<String> result = new HashSet<String>(expectedFromArchive);
        result.add(subarchiveName);
        for (String expectedSubarchEntryName : expectedFromSubarchive) {
            final StringBuilder path = new StringBuilder();
            path.append(subarchiveName).append("/");
            final String[] segments = expectedSubarchEntryName.split("/");
            for (int i = 0; i < segments.length; i++) {
                path.append(segments[i]);
                result.add(path.toString());
                if (i < segments.length) {
                    path.append("/");
                }
            }
        }
        return result;
    }

    @Test
    public void testSubarchive() throws Exception {

        System.out.println("testSubarchive");
        final ArchiveAndSubarchive archives = createAndPopulateArchiveAndSubarchive();
        

        checkArchive((FileArchive) archives.parent, archives.fullExpectedEntryNames);

        checkArchive((FileArchive) archives.subarchive, usualExpectedSubarchiveEntryNames);
    }

    @Test
    public void testSubArchiveCreateWithStaleEntry() throws Exception {
        System.out.println("testSubArchiveCreateWithStaleEntry");
        /*
         * Subarchives are a little tricky.  The marker file lives only at
         * the top level (because that's where undeployment puts it).  So
         * when a subarchive tests to see if an entry is valid it needs to
         * consult the marker file (if any) in the top-level owning archive.
         *
         * This test creates a directory structure containing a stale file
         * in a lower-level directory, creates the top-level marker file
         * as undeployment would, then creates an archive for the top level
         * and a subarchive for the lower-level directory (as the next
         * deployment would).  The archive and subarchive need to skip the
         * stale file.
         */

        /*
         * Create a file in the directory before creating the archive.
         */
        final File oldDir = new File(archiveDir, SUBARCHIVE_NAME);
        final File oldFile = new File(oldDir, STALE_ENTRY);
        oldFile.getParentFile().mkdirs();
        oldFile.createNewFile();

        /*
         * Mimic what undeployment does by creating a marker file for the
         * archive recording the pre-existing file.
         */
        FileArchive.StaleFileManager.Util.markDeletedArchive(archiveDir);

        /*
         * Now create the archive and subarchive on top of the directories
         * which already exist and contain the stale file and directory.
         */
        final ArchiveAndSubarchive archives = createAndPopulateArchiveAndSubarchive();

        checkArchive((FileArchive) archives.parent, archives.fullExpectedEntryNames);

        checkArchive((FileArchive) archives.subarchive, usualExpectedSubarchiveEntryNames);

        getListOfFilesCheckForLogRecord((FileArchive) archives.parent, archives.fullExpectedEntryNames);
        
    }

    private static class ArchiveAndSubarchive {
        ReadableArchive parent;
        ReadableArchive subarchive;
        Set<String> fullExpectedEntryNames;
    }

    private ArchiveAndSubarchive createAndPopulateArchiveAndSubarchive() throws Exception {
        final ArchiveAndSubarchive result = new ArchiveAndSubarchive();
        result.parent = createAndPopulateArchive(usualEntryNames);
        result.subarchive = createAndPopulateSubarchive(
                (FileArchive) result.parent,
                SUBARCHIVE_NAME,
                usualSubarchiveEntryNames);
        result.fullExpectedEntryNames = expectedEntryNames(
                usualExpectedEntryNames, SUBARCHIVE_NAME, usualSubarchiveEntryNames);

        return result;
    }

    /**
     * Test of open method, of class FileArchive.
     */
    @Test
    public void testNormalCreate() throws Exception {
        System.out.println("testNormalCreate");

        createAndPopulateAndCheckArchive(usualEntryNames);
    }

    @Test
    public void testCreateWithOlderLeftoverEntry() throws Exception {
        System.out.println("testCreateWithOlderLeftoverEntry");
        final ReadableArchive instance = createWithOlderLeftoverEntry(usualEntryNames);

        getListOfFilesCheckForLogRecord((FileArchive) instance, usualExpectedEntryNames);

        
    }

    @Test
    public void testCreateWithOlderLeftoverEntryWhichIsCreatedAgain() throws Exception {
        System.out.println("testCreateWithOlderLeftoverEntryWhichIsCreatedAgain");
        final FileArchive instance = (FileArchive) createWithOlderLeftoverEntry(usualEntryNames);

        /*
         * Now add the stale entry explicitly which should make it valid.
         */
        final OutputStream os = instance.putNextEntry(STALE_ENTRY);
        os.write("No longer stale!".getBytes());
        os.close();

        checkArchive(instance, usualExpectedEntryNamesWithOverwrittenStaleEntry);
    }

    private ReadableArchive createWithOlderLeftoverEntry(final Set<String> entryNames) throws Exception {

        /*
         * Create a file in the directory before creating the archive.
         */
        final File oldFile = new File(archiveDir, STALE_ENTRY);
        oldFile.getParentFile().mkdirs();
        oldFile.createNewFile();

        /*
         * Mimic what undeployment does by creating a marker file for the
         * archive recording the pre-existing file.
         */
        FileArchive.StaleFileManager.Util.markDeletedArchive(archiveDir);

        /*
         * Now create the archive.  The archive should not see the old file.
         */
        return createAndPopulateArchive(entryNames);
    }

    @Test
    public void testCreateWithOlderLeftoverEntryAndThenOpen() throws Exception {
        if (! OS.isWindows()) {
            System.out.println("Skipping (as successful) testCreateWithOlderLeftoverEntryAndThenOpen because this is not a Windows system");
            return;
        }
        System.out.println("testCreateWithOlderLeftoverEntryAndThenOpen");
        createWithOlderLeftoverEntry(usualEntryNames);
        final FileArchive openedArchive = new FileArchive();
        openedArchive.open(archiveDir.toURI());
        System.err.println("A WARNING should appear next");
        checkArchive(openedArchive, usualExpectedEntryNames);
    }

    @Test
    public void testOpenWithPreexistingDir() throws Exception {
        System.out.println("testOpenWithPreexistingDir");
        createPreexistingDir();
        final FileArchive openedArchive = new FileArchive();
        openedArchive.open(archiveDir.toURI());
        checkArchive(openedArchive, usualExpectedEntryNames);
    }

    private void createPreexistingDir() throws IOException {
         for (String entryName : usualEntryNames) {
             final File f = fileForPath(archiveDir, entryName);
             final File parentDir = f.getParentFile();
             if(parentDir != null) {
                 parentDir.mkdirs();
             }
             try {
                 f.createNewFile();
             } catch (Exception ex) {
                 throw new IOException(f.getAbsolutePath(), ex);
             }
         }
     }

    private File fileForPath(File anchor, final String path) {
         final String[] interveningDirNames = path.split("/");
         File interveningDir = anchor;
         for (int i = 0; i < interveningDirNames.length - 1; i++) {
             String name = interveningDirNames[i];
             interveningDir = new File(interveningDir, name + "/");
         }
         return new File(interveningDir,interveningDirNames[interveningDirNames.length - 1]);
     }

    @Ignore
    @Test
    public void testInaccessibleDirectoryInFileArchive() throws Exception {
        final String vendorURLText = System.getProperty("java.vendor.url");
        if (vendorURLText != null && vendorURLText.contains("ibm")) {
            /*
             * The IBM Java implementation seems not to work correctly with
             * File.setReadable.  So report this test
             */
            System.out.println("Skipping testInaccessibleDirectoryInFileArchive (as successful) because the Java vendor seems to be IBM");
            return;
        }
        /*
         * FileArchive will log a warning if it cannot list the files in the
         * directory. Here's the message key it will use.
         */
        final String EXPECTED_LOG_KEY = "enterprise.deployment.nullFileList";
        System.out.println("testInaccessibleDirectoryInFileArchive");
        
        final FileArchive archive = (FileArchive) createAndPopulateArchive(usualEntryNames);

        /*
         * Now make the lower-level directory impossible to execute - therefore
         * the attempt to list the files should fail.
         */
        final File lower = new File(archiveDir, "lower");
        lower.setExecutable(false, false);
        final boolean canRead = lower.setReadable(false, false);
        if ( ! canRead) {
            /*
             * If we cannot change the permissions then the test will fail.
             * We'd like to dynamically ignore this test but that's very involved
             * and requirea a custom test runner and notifier.  So we just
             * say the test passes.
             */
            return;
        }

        /*
         * Try to list the files.  This should fail with our logger getting
         * one record.
         */
        final Vector<String> fileList = new Vector<String>();
        handler.flush();
        archive.getListOfFiles(lower, fileList, null /* embeddedArchives */, deplLogger);

        List<LogRecord> logRecords = handler.logRecords();
        if (logRecords.isEmpty()) {
            fail("FileArchive logged no message about being unable to list files; expected " + EXPECTED_LOG_KEY);
        }
        assertEquals("FileArchive did not log expected message (re: being unable to list files)",
                        EXPECTED_LOG_KEY, logRecords.get(0).getMessage());
        /*
         * Change the protection back.
         */
        lower.setExecutable(true, false);
        lower.setReadable(true, false);
        handler.flush();

        archive.getListOfFiles(lower, fileList, null, deplLogger);
        assertTrue("FileArchive was incorrectly unable to list files; error key in log record:" +
                (logRecords.isEmpty() ? "" : logRecords.get(0).getMessage()),
                logRecords.isEmpty());
        
    }

    private static class RecordingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<LogRecord>();

        public void close() {
            records.clear();
        }

        public void flush() {
            records.clear();
        }

        public void publish(LogRecord record) {
            records.add(record);
        }

        List<LogRecord> logRecords() {
            return records;
        }
    }
}

