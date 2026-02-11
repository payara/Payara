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

package fish.payara.admin.cluster;

import com.sun.enterprise.config.serverbeans.Node;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.hk2.api.ServiceLocator;

import com.trilead.ssh2.SCPClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for list-node-versions command.
 *
 * Uses glassfish-version.properties as the version source,
 * - CONFIG nodes: read local install dirs.
 * - SSH nodes: SFTP + SCP download properties file + parse locally and find  version.
 */
public final class ListNodeVersionHelper {

    private static Logger logger;

    private static final String[] CURRENT_VERSION_FILES = {
            "glassfish/config/branding/glassfish-version.properties"
    };

    private static final String[] STAGED_VERSION_FILES = {
            "glassfish/config.new/branding/glassfish-version.properties"
    };

    private static final String[] OLD_VERSION_FILES = {
            "glassfish/config.old/branding/glassfish-version.properties"
    };

    private static final Pattern VERSION_PATTERN =
            Pattern.compile(".*_version=([0-9]+)");

    private ListNodeVersionHelper() {
    }

    public static NodeVersionInfo collect(ServiceLocator habitat, Node node, Logger log) {
        logger = log;

        String installDir = node.getInstallDir();
        logger.log(Level.FINER, "Collecting versions for node='%s' type='%s'" + node.getName(), node.getType());

        try {
            if ("CONFIG".equals(node.getType())) {
                String current = readLocalRoot(installDir, CURRENT_VERSION_FILES, "");
                String staged  = readLocalRoot(installDir, STAGED_VERSION_FILES, " (staged)");
                String old     = readLocalRoot(installDir, OLD_VERSION_FILES, " (old)");
                return new NodeVersionInfo(current, staged, old);
            }

            if ("SSH".equals(node.getType())) {
                File tempDir = Files.createTempDirectory("list-node-versions-").toFile();
                try {
                    String current = readSshRoot(habitat, node, tempDir, installDir, CURRENT_VERSION_FILES, "");
                    String staged  = readSshRoot(habitat, node, tempDir, installDir, STAGED_VERSION_FILES, " (staged)");
                    String old     = readSshRoot(habitat, node, tempDir, installDir, OLD_VERSION_FILES, " (old)");
                    return new NodeVersionInfo(current, staged, old);
                } finally {
                    deleteDirQuietly(tempDir);
                }
            }
            return new NodeVersionInfo("unknown", "", "");

        } catch (Exception e) {
            logger.log(Level.FINER,
                    "Failed to collect versions for node " + node.getName(), e);
            return new NodeVersionInfo("unknown", "", "");
        }
    }

    /**
     * Local reading
     */

    private static String readLocalRoot(String installRoot, String[] relPaths, String suffix) {
        if (installRoot == null || installRoot.isBlank()) return "";

        for (String rel : relPaths) {
            Path p = Paths.get(installRoot, rel);
            if (Files.isRegularFile(p)) {
                try {
                    String v = readVersionFromProperties(p);
                    return v.isBlank() ? "" : v + suffix;
                } catch (IOException ignored) {
                }
            }
        }
        return "";
    }

    /**
     * Remote reading
     */

    private static String readSshRoot(ServiceLocator habitat, Node node, File tempDir, String installRoot, String[] relPaths, String suffix) throws Exception {

        SSHLauncher sshL = habitat.getService(SSHLauncher.class);
        sshL.init(node, logger);

        for (String rel : relPaths) {
            String remote = joinRemote(installRoot, rel);

            try {
                logger.log(Level.FINER, "Node='{0}' attempting to download {1}", new Object[]{node.getName(), remote});

                SCPClient scp = sshL.getSCPClient();
                scp.get(remote, tempDir.getAbsolutePath());

                File local = new File(tempDir, "glassfish-version.properties");
                if (!local.exists()) {
                    logger.log(Level.FINER, "Node='{0}' SCP succeeded but file not found locally", node.getName());
                    continue;
                }

                String v = readVersionFromProperties(local.toPath());
                return v.isBlank() ? "" : v + suffix;
            } catch (IOException ex) {
                logger.log(Level.FINER, "Node='{0}' unable to read {1}: {2}", new Object[]{node.getName(), remote, ex.getMessage()});
            } finally {
                try { tempDir.delete(); } catch (Exception ignored) {}
            }
        }
        return "";
    }

    private static String joinRemote(String root, String rel) {
        String r = root.replace('\\', '/');
        return r.endsWith("/") ? r + rel : r + "/" + rel;
    }

    private static String readVersionFromProperties(Path file) throws IOException {
        String major = null;
        String minor = null;
        String patch = null;

        for (String line : Files.readAllLines(file)) {
            Matcher m = VERSION_PATTERN.matcher(line);
            if (!m.find()) continue;

            if (line.startsWith("major")) {
                major = m.group(1);
            } else if (line.startsWith("minor")) {
                minor = m.group(1);
            } else if (line.startsWith("update")) {
                patch = m.group(1);
            }
        }

        if (major == null || minor == null || patch == null) {
            return "";
        }

        return major + "." + minor + "." + patch;
    }

    /**
     * Cleanup Helper after using files
     */

    private static void deleteDirQuietly(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                try { f.delete(); } catch (Exception ignored) {}
            }
        }
        try { dir.delete(); } catch (Exception ignored) {}
    }
}