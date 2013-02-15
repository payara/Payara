/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.uberjar.builder.instanceroot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class InstanceRootBuilderUtil {

    private static final Logger logger = Logger.getLogger("embedded-glassfish");
    private static String resourceroot = "glassfish4/glassfish/domains/domain1/";

    public static void buildInstanceRoot(String instanceroot, String configFileURI) throws Exception {
        ClassLoader cl = InstanceRootBuilderUtil.class.getClassLoader();
        String resourceName = resourceroot;
        URL resource = cl.getResource(resourceName);
        URLConnection urlConn = resource.openConnection();
        if (urlConn instanceof JarURLConnection) {
            JarFile jarFile = ((JarURLConnection) urlConn).getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entryName.indexOf(resourceName) != -1 && !entryName.endsWith("/")) {
                    copy(cl.getResourceAsStream(entryName), instanceroot,
                            entryName.substring(entryName.indexOf(resourceName) + resourceName.length()));
                }
            }
            jarFile.close();
        }
        if (configFileURI != null) {
            URI configFile = URI.create(configFileURI);
            copy(configFile.toURL().openConnection().getInputStream(), instanceroot,
                    "config/domain.xml", true);
        }
    }

    public static void copy(InputStream stream, String destDir, String path) {
        copy(stream, destDir, path, false);
    }

    public static void copy(InputStream stream, String destDir, String path, boolean overwrite) {

        if (stream != null) {
            try {
                File f = new File(destDir, path);
                // create directory.
                if (!f.exists() || overwrite) {
                    f.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(new File(destDir, path));
                    byte[] data = new byte[2048];
                    int count = 0;
                    while ((count = stream.read(data)) != -1) {
                        fos.write(data, 0, count);
                    }
                    logger.fine("Created " + f);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (Exception ex) {
                }
            }
        }
    }

}
