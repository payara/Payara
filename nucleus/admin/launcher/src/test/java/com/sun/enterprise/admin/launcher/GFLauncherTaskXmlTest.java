/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package com.sun.enterprise.admin.launcher;

import org.glassfish.api.admin.RuntimeType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class GFLauncherTaskXmlTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String readXml(File f) throws IOException {
        byte[] bytes = Files.readAllBytes(f.toPath());
        // Strip BOM (FF FE) and decode as UTF-16LE
        return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }

    @Test
    public void writeTaskXml_noUserIdElement() throws Exception {
        File xml = tmp.newFile("task.xml");
        File batch = tmp.newFile("payara-start.bat");
        GFLauncher.writeTaskXml(xml, batch.getAbsolutePath());
        String content = readXml(xml);
        // UserId is omitted so schtasks resolves the user from /ru on the command line,
        // avoiding ERROR_NONE_MAPPED (1332) on accounts the XML validator can't resolve.
        assertFalse("UserId element must be absent", content.contains("<UserId>"));
    }

    @Test
    public void writeTaskXml_usesCmdExeNotWscript() throws Exception {
        File xml = tmp.newFile("task.xml");
        File batch = tmp.newFile("payara-start.bat");
        GFLauncher.writeTaskXml(xml, batch.getAbsolutePath());
        String content = readXml(xml);
        assertTrue("Expected cmd.exe command", content.contains("<Command>cmd.exe</Command>"));
        assertFalse("Should not reference wscript", content.toLowerCase().contains("wscript"));
    }

    @Test
    public void writeTaskXml_argumentsContainBatchPath() throws Exception {
        File xml = tmp.newFile("task.xml");
        File batch = tmp.newFile("payara-start.bat");
        GFLauncher.writeTaskXml(xml, batch.getAbsolutePath());
        String content = readXml(xml);
        String escapedBatch = batch.getAbsolutePath()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        assertTrue("Arguments should reference batch file",
            content.contains("/c &quot;" + escapedBatch + "&quot;"));
    }

    @Test
    public void writeTaskXml_bom_isUtf16Le() throws Exception {
        File xml = tmp.newFile("task.xml");
        File batch = tmp.newFile("payara-start.bat");
        GFLauncher.writeTaskXml(xml, batch.getAbsolutePath());
        byte[] bytes = Files.readAllBytes(xml.toPath());
        assertEquals("BOM first byte", (byte) 0xFF, bytes[0]);
        assertEquals("BOM second byte", (byte) 0xFE, bytes[1]);
    }

    @Test
    public void writeTaskXml_alwaysUsesPasswordLogon() throws Exception {
        File xml = tmp.newFile("task.xml");
        File batch = tmp.newFile("payara-start.bat");
        GFLauncher.writeTaskXml(xml, batch.getAbsolutePath());
        String content = readXml(xml);
        assertTrue("Expected Password logon type",
            content.contains("<LogonType>Password</LogonType>"));
        assertFalse("Should not contain InteractiveToken",
            content.contains("InteractiveToken"));
        assertFalse("Should not contain UserId element",
            content.contains("<UserId>"));
    }

    @Test
    public void gfLauncherInfo_isInstance_trueWhenInstanceNameSet() {
        GFLauncherInfo info = new GFLauncherInfo(RuntimeType.INSTANCE);
        info.setInstanceName("inst1");
        assertTrue("isInstance should be true when instanceName is set", info.isInstance());
        assertNull("windowsPassword should be null by default", info.getWindowsPassword());
    }

    @Test
    public void gfLauncherInfo_windowsPassword_roundTrips() {
        GFLauncherInfo info = new GFLauncherInfo(RuntimeType.INSTANCE);
        info.setWindowsPassword("s3cr3t");
        assertEquals("s3cr3t", info.getWindowsPassword());
    }

}
