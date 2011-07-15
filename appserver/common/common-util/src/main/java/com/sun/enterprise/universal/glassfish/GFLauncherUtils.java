/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.glassfish;

import com.sun.enterprise.util.net.NetUtils;
import static com.sun.enterprise.util.SystemPropertyConstants.*;
import com.sun.enterprise.universal.io.SmartFile;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Package private static utility methods
 * @author bnevins
 */
public class GFLauncherUtils {

    private GFLauncherUtils() {
    // all static methods
    }

    public static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    public static boolean safeExists(File f) {
        return f != null && f.exists();
    }

    public static boolean safeIsDirectory(File f) {
        return f != null && f.isDirectory();
    }

    public static synchronized File getInstallDir() {
        // if it is already set as a System Property -- skip the huge block below
        setInstallDirFromSystemProp();

        if(installDir == null)
        {
            String resourceName = GFLauncherUtils.class.getName().replace('.', '/') + ".class";
            URL resource = GFLauncherUtils.class.getClassLoader().getResource(resourceName);

            if (resource == null) {
                return null;
            }

            if (!resource.getProtocol().equals("jar")) {
                return null;
            }

            try {
                JarURLConnection c = (JarURLConnection) resource.openConnection();
                URL jarFile = c.getJarFileURL();
                
                // important to sanitize it!
                // unreported bug:
                // JDK does this:
                // the parent of "/foo/." is "/foo", not "/" !
               
                
                File f = SmartFile.sanitize(new File(jarFile.toURI()));

                f = f.getParentFile();  // <install>/modules

                if (f == null) {
                    return null;
                }

                f = f.getParentFile(); // <install>/

                if (f == null) {
                    return null;
                }

                installDir = SmartFile.sanitize(f);
            }
            catch (Exception e) {
                installDir = null;
            }
        }
        return installDir;
    }

    public static boolean isWindows() {
        String osname = System.getProperty("os.name");

        if (osname == null || osname.length() <= 0) {
            return false;
        }

        // case insensitive compare...
        osname = osname.toLowerCase(Locale.ENGLISH);

        if (osname.indexOf("windows") >= 0) {
            return true;
        }

        return false;
    }

    /**
     * This method returns the fully qualified name of the host.  If
     * the name can't be resolved (on windows if there isn't a domain specified), just 
     * host name is returned
     *
     * @deprecated
     * @return 
     * @throws UnknownHostException so it can be handled on a case by case basis
     */
    @Deprecated
    public static String getCanonicalHostName() throws UnknownHostException {
        return NetUtils.getCanonicalHostName();
    }

    public static String replace(String s, String token, String replace) {
        if (s == null || s.length() <= 0 || token == null || token.length() <= 0) {
            return s;
        }

        int index = s.indexOf(token);

        if (index < 0) {
            return s;
        }

        int tokenLength = token.length();
        String ret = s.substring(0, index);
        ret += replace;
        ret += s.substring(index + tokenLength);

        return ret;
    }

    /**
     * Makes an educated guess on whether an arbitrary string is a relative path.
     * If the string really is a path, it should be 100% accurate.
     * If it is an arbitrary string like, say, "hello/./world", then it will say
     * that it is a relative path.
     * @param path the path to check
     * @return true if the path is probably relative
     */
    public static boolean isRelativePath(String path) {
        if (!ok(path)) {
            return false;
        }
        else if (path.startsWith(".")) {
            return true;
        }
        else if (path.indexOf("/.") >= 0) {
            return true;
        }
        else if (path.indexOf("\\.") >= 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Convert a classpath like string, e.g. c:/a;c:/b, and convert to List of File
     * @param cp The classpath-like string
     * @return the list of File
     */
    public static List<File> stringToFiles(String cp)
    {
        List<File> list = new ArrayList<File>();

        if(ok(cp)) {
            cp = SmartFile.sanitizePaths(cp);
            String[] ss = cp.split(File.pathSeparator);

            for(String s : ss)
                list.add(new File(s));
        }
        
        return list;
    }
    
    public static String fileListToPathString(List<File> files) {
        StringBuilder sb = new StringBuilder();
        boolean firstFile = true;

        for (File f : files) {
            if(firstFile) {
                firstFile = false;
            }
            else {
                sb.append(File.pathSeparatorChar);
            }
            // let's use forward slashes for neatness...
            sb.append(f.getPath().replace('\\', '/'));
        }
        return sb.toString();
    }

    private static void setInstallDirFromSystemProp() {
        // https://embedded-glassfish.dev.java.net/issues/show_bug.cgi?id=54
        //
        // For instance if we are called from an Embedded Server then InstallRoot
        // may already be set as a System Prop -- and the jar we are running from has
        // nothing whatever to do with the concept of an "install root".
        // In that case we certainly do not want to wipe out the already set install root.

        // if anything is not kosher simply return w/o setting installDir to anything.
        String installRootDirName = System.getProperty(INSTALL_ROOT_PROPERTY);

        if(!ok(installRootDirName))
            return;

        File f = SmartFile.sanitize(new File(installRootDirName));

        if(f.isDirectory()) {
            installDir = f;
        }
    }

    private static File installDir;
}

