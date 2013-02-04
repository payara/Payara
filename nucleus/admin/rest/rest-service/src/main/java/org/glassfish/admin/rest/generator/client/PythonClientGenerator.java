/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.generator.client;

import org.glassfish.admin.rest.utils.Util;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigModel;

import com.sun.appserv.server.util.Version;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.glassfish.admin.rest.RestLogging;

public class PythonClientGenerator extends ClientGenerator {
    private File baseDirectory;
    private static String MSG_INSTALL =
            "To install the egg into your Python environment:  sudo easy_install " + ARTIFACT_NAME + "-VERSION-egg.zip";

    public PythonClientGenerator(ServiceLocator habitat) {
        super(habitat);
        baseDirectory = Util.createTempDirectory();
        messages.add(MSG_INSTALL.replace("VERSION", versionString));
    }

    @Override
    public Map<String, URI> getArtifact() {
        ZipOutputStream zip = null;
        Map<String, URI> artifacts = new HashMap<String, URI>();
        try {
            String ZIP_BASE_DIR = "glassfish-rest-client-VERSION".replace("VERSION", Version.getVersionNumber());
            String ZIP_GF_PACKAGE_DIR = ZIP_BASE_DIR + "/glassfish";
            String ZIP_REST_PACKAGE_DIR = ZIP_GF_PACKAGE_DIR + "/rest";

            File zipDir = Util.createTempDirectory();
            File zipFile = new File(zipDir, ARTIFACT_NAME + "-" + versionString + "-egg.zip");
            if (!zipFile.createNewFile()) {
                throw new RuntimeException("Unable to create new file"); //i18n
            }
            zipFile.deleteOnExit();
            zip = new ZipOutputStream(new FileOutputStream(zipFile));
            
            add(ZIP_GF_PACKAGE_DIR, "__init__.py", new ByteArrayInputStream("".getBytes()), zip);
            //add(ZIP_BASE_DIR, "PKG-INFO", new ByteArrayInputStream(getFileContents("PKG-INFO").getBytes()), zip);
            add(ZIP_BASE_DIR, "setup.py", new ByteArrayInputStream(getFileContents("setup.py").getBytes()), zip);
            addFileFromClasspath(ZIP_REST_PACKAGE_DIR, "__init__.py", zip);
            addFileFromClasspath(ZIP_REST_PACKAGE_DIR, "connection.py", zip);
            addFileFromClasspath(ZIP_REST_PACKAGE_DIR, "restclient.py", zip);
            addFileFromClasspath(ZIP_REST_PACKAGE_DIR, "restresponse.py", zip);
            addFileFromClasspath(ZIP_REST_PACKAGE_DIR, "restclientbase.py", zip);
            for (File file : baseDirectory.listFiles()) {
                add(ZIP_REST_PACKAGE_DIR, file, zip);
            }
        
            artifacts.put(zipFile.getName(), zipFile.toURI());
            Util.deleteDirectory(baseDirectory);
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ex) {
                    RestLogging.restLogger.log(Level.SEVERE, null, ex);
                }
            }
        }
        
        return artifacts;
    }

    @Override
    public ClientClassWriter getClassWriter(ConfigModel model, String className, Class parent) {
         return new PythonClientClassWriter(model, className, parent, baseDirectory);
    }

    private String getFileContents(String fileName) {
        String contents = new Scanner(getClass().getClassLoader().getResourceAsStream("/client/python/" + fileName)).useDelimiter("\\Z").next();

        return contents.replace("VERSION", Version.getVersionNumber());
    }
    
    private void addFileFromClasspath(String targetDir, String fileName, ZipOutputStream zip) throws IOException {
        add(targetDir, fileName, getClass().getClassLoader().getResourceAsStream("/client/python/" + fileName), zip);
    }
    
    private void add(String dirInZip, String nameInZip, InputStream source, ZipOutputStream target) throws IOException {
        try {
            String sourcePath = dirInZip + "/" + nameInZip;

            ZipEntry entry = new ZipEntry(sourcePath);
            target.putNextEntry(entry);

            byte[] buffer = new byte[1024];
            while (true) {
                int count = source.read(buffer);
                if (count == -1) {
                    break;
                }
                target.write(buffer, 0, count);
            }
            target.closeEntry();
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    private void add(String dirInZip, File source, ZipOutputStream target) throws IOException {
        add(dirInZip, source.getName(), new BufferedInputStream(new FileInputStream(source)), target);
    }
}
