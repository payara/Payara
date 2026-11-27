/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package com.sun.enterprise.server.logging.logviewer.backend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests the log file name validation used by {@link LogFilter} and
 * {@link LogFilterForInstance} to prevent path traversal (FISH-14151).
 */
public class LogFilterFileNameTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File logsDir;
    private File configDir;

    @Before
    public void setUp() throws IOException {
        File domainDir = temporaryFolder.newFolder("domain1");
        logsDir = new File(domainDir, "logs");
        configDir = new File(domainDir, "config");
        logsDir.mkdirs();
        configDir.mkdirs();
        new File(logsDir, "server.log").createNewFile();
        new File(logsDir, "server.log_2026-11-10T10-00-00").createNewFile();
        new File(configDir, "domain.xml").createNewFile();
    }

    @Test
    public void sanitizeAcceptsBareFileNames() {
        assertEquals("server.log", LogFilter.sanitizeLogFileName("server.log"));
        assertEquals("server.log", LogFilter.sanitizeLogFileName("  server.log  "));
        assertEquals("server.log_2026-11-10T10-00-00",
                LogFilter.sanitizeLogFileName("server.log_2026-11-10T10-00-00"));
        assertEquals("..server.log", LogFilter.sanitizeLogFileName("..server.log"));
    }

    @Test
    public void sanitizeRejectsUnsafeNames() {
        assertNull(LogFilter.sanitizeLogFileName(null));
        assertNull(LogFilter.sanitizeLogFileName(""));
        assertNull(LogFilter.sanitizeLogFileName("   "));
        assertNull(LogFilter.sanitizeLogFileName("."));
        assertNull(LogFilter.sanitizeLogFileName(".."));
        assertNull(LogFilter.sanitizeLogFileName("../config/domain.xml"));
        assertNull(LogFilter.sanitizeLogFileName("..\\config\\domain.xml"));
        assertNull(LogFilter.sanitizeLogFileName("/etc/passwd"));
        assertNull(LogFilter.sanitizeLogFileName("C:\\Windows\\win.ini"));
        assertNull(LogFilter.sanitizeLogFileName("../../../../../../etc/passwd"));
        assertNull(LogFilter.sanitizeLogFileName("${com.sun.aas.instanceRoot}/logs/server.log"));
        assertNull(LogFilter.sanitizeLogFileName("server.log\0.txt"));
    }

    @Test
    public void resolveReturnsFileInsideLogDirectory() throws IOException {
        assertEquals(new File(logsDir, "server.log").getCanonicalFile(),
                LogFilter.resolveLogFileInDirectory(logsDir, "server.log"));
        assertEquals(new File(logsDir, "server.log_2026-11-10T10-00-00").getCanonicalFile(),
                LogFilter.resolveLogFileInDirectory(logsDir, "server.log_2026-11-10T10-00-00"));
    }

    @Test
    public void resolveRejectsTraversal() {
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, "../config/domain.xml"));
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, "../../../../../../../../etc/passwd"));
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, new File(configDir, "domain.xml").getAbsolutePath()));
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, ".."));
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, null));
        assertNull(LogFilter.resolveLogFileInDirectory(null, "server.log"));
    }

    @Test
    public void resolveRejectsSymlinkEscapingLogDirectory() throws IOException {
        Path link = new File(logsDir, "evil.log").toPath();
        try {
            Files.createSymbolicLink(link, new File(configDir, "domain.xml").toPath());
        } catch (UnsupportedOperationException | IOException e) {
            Assume.assumeNoException("Symbolic links not supported on this platform", e);
        }
        assertNull(LogFilter.resolveLogFileInDirectory(logsDir, "evil.log"));
    }
}
