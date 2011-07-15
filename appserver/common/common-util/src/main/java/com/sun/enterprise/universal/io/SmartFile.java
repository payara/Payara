/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.io;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.universal.glassfish.GFLauncherUtils;

/**
 * A class for sanitizing Files.
 * Note that the main reason for this class is that on non-Windows, 
 * getCanonicalXXX and getAbsoluteXXX might point at different files.  
 * If the file is a soft link then the Canonical will be the file that is linked to.
 * The Absolute will be the link file itself.
 * This method will give you the benefits of Canonical -- but will always point
 * at the link file itself. 
 * Windows is horribly complex compared to "everything else".  Windows does not have
 * the symbolic link issue -- so use getCanonicalXXX to do the work on Windows.
 * Windows will return paths with all forward slashes -- no backward slashes unless it
 * is the special Windows network address that starts with "\\"
 * <p>
 * I.e. It is just like getAbsoluteXXX -- but it removes all relative path 
 * elements from the path.
 * @author bnevins
 */
public class SmartFile {

    /**
     * Sanitize a File object -- remove all relative path portions, i.e. dots
     * e.g. "/xxx/yyy/././././../yyy"  --> /xxx/yyy on UNIX, perhaps C:/xxx/yyy on Windows
     * @param f The file to sanitize
     * @return THe sanitized File
     */
    public static File sanitize(File f) {
        SmartFile sf = new SmartFile(f);
        return new File(sf.path);
    }

    /**
     * Sanitize a path -- remove all relative path portions, i.e. dots
     * e.g. "/xxx/yyy/././././../yyy"  --> /xxx/yyy on UNIX, perhaps C:/xxx/yyy on Windows
     * Note that the main reason for this class is that on non-Windows, 
     * getCanonicalXXX and getAbsoluteXXX might point at different files.  
     * If the file is a soft link then the Canonical will be the file that is linked to.
     * The Absolute will be the link file itself.
     * This method will give you the benefits of Canonical -- but will always point
     * at the link file path itself. 
     * @param filename The path to sanitize
     * @return The sanitized path
     */
    public static String sanitize(String filename) {
        SmartFile sf = new SmartFile(filename);
        return sf.path;
    }

    /**
     * Sanitize a "Classpath-like" list of Paths.
     * @param pathsString A string of paths, each separated by File.pathSeparator
     * @return The sanitized paths
     */
    public static String sanitizePaths(String pathsString) {
        if (!ok(pathsString))
            return pathsString;

        try {
            String[] paths = pathsString.split(File.pathSeparator);
            StringBuilder sb = new StringBuilder();
            Set<String> pathsSet = new HashSet<String>();
            List<String> pathsList = new LinkedList<String>();

            for (int i = 0; i < paths.length; i++) {
                String path = paths[i];

                // ignore empty path elements.  E.g. "c:/foo;;;;;;;" should become "C:/foo"
                // not "c:/foo;thisdir;thisdir;thisdir etc"
                if (!ok(path))
                    continue;

                // pathsSet is only here for removing duplicates.  We need the
                // List to maintain the original order!
                path = SmartFile.sanitize(path);

                if (pathsSet.add(path))
                    pathsList.add(path);
            }

            boolean firstElement = true;
            for (String path : pathsList) {
                if (firstElement)
                    firstElement = false;
                else
                    sb.append(File.pathSeparator);

                sb.append(path);
            }
            return sb.toString();
        }
        catch (Exception e) {
            return pathsString;
        }
    }

    private SmartFile(File f) {
        if (f == null)
            throw new NullPointerException();

        convert(f.getAbsolutePath());
    }

    private SmartFile(String s) {
        if (s == null)
            throw new NullPointerException();

        // note that "" is a valid filename
        // IT 7500 get rid of quotes!!!
        s = StringUtils.removeEnclosingQuotes(s);
        convert(new File(s).getAbsolutePath());
    }

    private void convert(String oldPath) {
        if (GFLauncherUtils.isWindows())
            convertWindows(oldPath);
        else
            convertNix(oldPath);
    }

    /*
     * There is no symlink issue with getCanonical vs getAbsolute
     * so we do it the EASY way here...
     */
    private void convertWindows(String oldPath) {
        try {
            path = new File(oldPath).getCanonicalPath();
            if (!path.startsWith("\\")) // network address...
                path = path.replace('\\', '/');
        }
        catch (IOException ex) {
            // what to do?  This has never happened to me and I use File I/O
            //** a lot **
            path = oldPath.replace('\\', '/');
        }
    }

    private void convertNix(String oldPath) {
        // guarantee -- the beginning will not have "." or ".." 
        // (because of getAbsolutePath()...)
        char[] p = oldPath.toCharArray();

        int from, to;
        for (from = 0, to = 0; from < p.length; from++) {
            if (p[from] == '/' &&
                ((from + 3 < p.length &&
                  p[from+1] == '.' && p[from+2] == '.' && p[from+3] == '/') ||
                 (from + 3 == p.length &&
                  p[from+1] == '.' && p[from+2] == '.'))) {
                // remove the previous directory due to /../
                while (to > 0 && p[--to] != '/');
                from += 2;
            }
            else if (p[from] == '/' &&
                    ((from + 2 < p.length &&
                      p[from+1] == '.' && p[from+2] == '/') ||
                     (from + 2 == p.length &&
                      p[from+1] == '.'))) {
                // skip over /./
                from += 1;
            }
            else {
                p[to++] = p[from];
            }
        }
        if (to > 0 && p[to-1] == '/') to -= 1;
        path = new String(p, 0, to);
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    private String path;
}
