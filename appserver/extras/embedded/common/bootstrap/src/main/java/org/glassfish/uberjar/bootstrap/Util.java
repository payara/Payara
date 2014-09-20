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

package org.glassfish.uberjar.bootstrap;

import org.glassfish.embeddable.GlassFish;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@dev.java.net
 */

public class Util {

    private static Logger logger = Logger.getLogger("embedded-glassfish");

    public static Logger getLogger() {
        return logger;
    }

    // serverId:GlassFish map
    private final static Map<String, GlassFish> gfMap =
            new HashMap<String, GlassFish>();

    public static synchronized void addServer(String serverId, GlassFish glassfish) {
        gfMap.put(serverId, glassfish);
    }


    public static synchronized void removeServer(String serverId) {
        gfMap.remove(serverId);
    }

    public static GlassFish getServer(String serverId) {
        return gfMap.get(serverId);
    }

    public static URI whichJar(Class clazz) {
        logger.finer("ResourceName = " + clazz.getName().replace(".", "/") + ".class");
        URL url = clazz.getClassLoader().getResource(
                clazz.getName().replace(".", "/") + ".class");
        logger.finer("url = " + url);
        if (url != null) {
            URLConnection con = null;
            try {
                con = url.openConnection();
                logger.finer("con = " + con);
                if (con instanceof JarURLConnection) {
                    return JarURLConnection.class.cast(con).getJarFileURL().toURI();
                }

            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
        }
        return null;
    }

    public static boolean isUber(URI uri) {
        String uriString = uri.toString();
        String jarFileName = uriString.substring(uriString.lastIndexOf("/") + 1);
        return jarFileName.indexOf("glassfish-embedded") != -1 ? true : false;
    }

    private static String MODULES_DIR_PREFIX = "modules";
    private static String MODULES_DIR_SUFFIX = "_jar/";
    private static final String JARFILE_URL_PREFIX = "jar:file:";
    private static final String JARENTRY_PREFIX = "!/";

    public static List<URL> getModuleJarURLs(File modulesJarFile) {
        List<URL> moduleJarURLs = new ArrayList();
        try {
            JarFile modulesJar = new JarFile(modulesJarFile);
            Enumeration<JarEntry> entries = modulesJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    continue;
                }
                if (!entry.getName().startsWith(MODULES_DIR_PREFIX) ||
                        !entry.getName().endsWith(MODULES_DIR_SUFFIX)) {
                    continue;
                }
                moduleJarURLs.add(new URL(JARFILE_URL_PREFIX + modulesJar.getName() +
                        JARENTRY_PREFIX + entry.getName()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return moduleJarURLs;
    }

    static void copyWithoutClose(InputStream in, FileOutputStream out, long size) throws IOException {

        ReadableByteChannel inChannel = Channels.newChannel(in);
        FileChannel outChannel = out.getChannel();
        outChannel.transferFrom(inChannel, 0, size);

    }

    static void copy(InputStream in, FileOutputStream out, long size) throws IOException {

        try {
            copyWithoutClose(in, out, size);
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }
    
}
