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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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

package fish.payara.cluster.winrm;

import com.google.common.io.BaseEncoding;
import com.sun.enterprise.config.serverbeans.Node;
import io.cloudsoft.winrm4j.winrm.WinRmTool;
import io.cloudsoft.winrm4j.winrm.WinRmToolResponse;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service(name = "WinRMHelper")
@PerLookup
public class WinRMHelper {
    private WinRmTool winrm;

    public void init(String username, String password, String host, int port, String workingDirectory) {
        winrm = WinRmTool.Builder.builder(host, username, password)
                .port(port)
                .useHttps(false)
                .workingDirectory(workingDirectory)
                .build();
    }

    public void init(Node node) {
        init(
                node.getWinrmConnector().getWinrmUser(),
                node.getWinrmConnector().getWinrmPassword(),
                node.getNodeHost(),
                Integer.parseInt(node.getWinrmConnector().getWinrmPort()),
                node.getInstallDirUnixStyle()
        );
    }

    public void sendFile(File file, String destinationPath) throws IOException {
        int chunkSize = 1024;
        byte[] inputData = new byte[chunkSize];
        int bytesRead;
        int expectedFileSize = 0;

        try (InputStream stream = new FileInputStream(file)) {
            while ((bytesRead = stream.read(inputData)) > 0) {
                byte[] chunk;
                if (bytesRead == chunkSize) {
                    chunk = inputData;
                } else {
                    chunk = Arrays.copyOf(inputData, bytesRead);
                }
                winrm.executePs("If ((!(Test-Path " + destinationPath + ")) -or ((Get-Item '" + destinationPath + "').length -eq " +
                        expectedFileSize + ")) {Add-Content -Encoding Byte -path " + destinationPath +
                        " -value ([System.Convert]::FromBase64String(\"" + BaseEncoding.base64().encode(chunk) + "\"))}");
                expectedFileSize += bytesRead;
            }
        }
    }

    public WinRmTool getWinRM() {
        return winrm;
    }

    public void pingConnection() {
        executeCommand("echo ping");
    }

    public boolean exists(String path) {
        return false; // TODO: WinRM
    }

    public WinRmToolResponse makeDirectory(String path) {
        return executeCommand("mkdir " + path);
    }

    public WinRmToolResponse remove(String path) {
        return executeCommand("rd /S /Q " + path);
    }

    public List<PathEntry> ls(String directory) throws IOException {
        List<PathEntry> entries = new ArrayList<>();
        ByteArrayOutputStream directoryStream = new ByteArrayOutputStream();
        executePowershell("ls -n -Force -directory" + directory, directoryStream);
        String[] directoryOutput = directoryStream.toString(StandardCharsets.UTF_8).split("\n");

        for (String entry : directoryOutput) {
            entries.add(new PathEntry(entry, directory, true));
        }

        ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
        executePowershell("ls -n -Force -file " + directory, fileStream);
        String[] fileOutput = fileStream.toString(StandardCharsets.UTF_8).split("\n");

        for (String entry : fileOutput) {
            entries.add(new PathEntry(entry, directory, false));
        }

        return entries;
    }

    public boolean isDirectoryEmpty(String directory) throws IOException {
        return ls(directory).isEmpty();
    }

    public WinRmToolResponse executeCommand(String command) {
        return winrm.executeCommand(command);
    }

    public WinRmToolResponse executeCommand(String command, OutputStream output) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
            return winrm.executeCommand(command, writer, writer);
        }
    }

    public WinRmToolResponse executeCommand(String command, OutputStream standardOut, OutputStream errorOut) throws IOException {
        try (OutputStreamWriter stdOut = new OutputStreamWriter(standardOut);
             OutputStreamWriter errOut = new OutputStreamWriter(errorOut)) {

            return winrm.executeCommand(command, stdOut, errOut);
        }
    }

    public WinRmToolResponse executePowershell(String command) {
        return winrm.executePs(command);
    }

    public WinRmToolResponse executePowershell(String command, OutputStream output) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(output)) {
            return winrm.executePs(command, writer, writer);
        }
    }

    public WinRmToolResponse executePowershell(String command, OutputStream standardOut, OutputStream errorOut) throws IOException {
        try (OutputStreamWriter stdOut = new OutputStreamWriter(standardOut);
             OutputStreamWriter errOut = new OutputStreamWriter(errorOut)) {

            return winrm.executePs(command, stdOut, errOut);
        }
    }

    public static class PathEntry {
        private final String filename;
        private final String parent;
        private final boolean directory;

        private PathEntry(String filename, String parent, boolean directory) {
            this.filename = filename;
            this.parent = parent;
            this.directory = directory;
        }

        public String getFilename() {
            return filename;
        }

        public String getParent() {
            return parent;
        }

        public String getPath() {
            return parent + "/" + filename;
        }

        public boolean isDirectory() {
            return directory;
        }
    }
}
