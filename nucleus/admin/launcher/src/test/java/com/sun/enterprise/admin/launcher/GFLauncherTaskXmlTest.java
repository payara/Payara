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
