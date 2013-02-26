/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;

import org.apache.catalina.Host;
import org.apache.catalina.core.StandardServer;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expand out a WAR in a Host's appBase.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @author Glenn L. Nielsen
 * @version $Revision: 1.3 $
 */

public class ExpandWar {

    private static final Logger log = StandardServer.log;
    private static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Application base directory {0} does not exist",
            level = "WARNING"
    )
    public static final String APP_NOT_EXIST_EXCEPTION = "AS-WEB-CORE-00444";

    @LogMessageInfo(
            message = "Unable to create the directory [{0}]",
            level = "WARNING"
    )
    public static final String UNABLE_CREATE_DIRECTORY_EXCEPTION = "AS-WEB-CORE-00445";

    @LogMessageInfo(
            message = "The archive [{0}] is malformed and will be ignored: an entry contains an illegal path [{1}]",
            level = "WARNING"
    )
    public static final String ARCHIVE_IS_MALFORMED_EXCEPTION = "AS-WEB-CORE-00446";

    @LogMessageInfo(
            message = "Failed to set last-modified time of the file {0}",
            level = "WARNING"
    )
    public static final String FAILED_SET_LAST_MODIFIED_TIME_EXCEPTION = "AS-WEB-CORE-00447";

    @LogMessageInfo(
            message = "Error copying {0} to {1}",
            level = "SEVERE",
            cause = "Could not copy file",
            action = "Verify if channel is not available for file transfer"
    )
    public static final String ERROR_COPYING_EXCEPTION = "AS-WEB-CORE-00448";

    @LogMessageInfo(
            message = "[{0}] could not be completely deleted. The presence of the remaining files may cause problems",
            level = "SEVERE",
            cause = "Could not completely delete specified directory",
            action = "Verify the access permission to specified directory"
    )
    public static final String DELETE_DIR_EXCEPTION = "AS-WEB-CORE-00449";

    /**
     * Expand the WAR file found at the specified URL into an unpacked
     * directory structure, and return the absolute pathname to the expanded
     * directory.
     *
     * @param host Host war is being installed for
     * @param war URL of the web application archive to be expanded
     *  (must start with "jar:")
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    public static String expand(Host host, URL war)
        throws IOException {

        // Calculate the directory name of the expanded directory
        String pathname = war.toString().replace('\\', '/');
        if (pathname.endsWith("!/")) {
            pathname = pathname.substring(0, pathname.length() - 2);
        }
        int period = pathname.lastIndexOf('.');
        if (period >= pathname.length() - 4)
            pathname = pathname.substring(0, period);
        int slash = pathname.lastIndexOf('/');
        if (slash >= 0) {
            pathname = pathname.substring(slash + 1);
        }
        return expand(host, war, pathname);

    }


    /**
     * Expand the WAR file found at the specified URL into an unpacked
     * directory structure, and return the absolute pathname to the expanded
     * directory.
     *
     * @param host Host war is being installed for
     * @param war URL of the web application archive to be expanded
     *  (must start with "jar:")
     * @param pathname Context path name for web application
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL or if the
     *            WAR file is invalid
     * @exception IOException if an input/output error was encountered
     *  during expansion
     */
    public static String expand(Host host, URL war, String pathname)
        throws IOException {

        // Make sure that there is no such directory already existing
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute()) {
            appBase = new File(System.getProperty("catalina.base"),
                               host.getAppBase());
        }
        if (!appBase.exists() || !appBase.isDirectory()) {
            String msg = MessageFormat.format(rb.getString(APP_NOT_EXIST_EXCEPTION),
                                              appBase.getAbsolutePath());
            throw new IOException(msg);
        }
        File docBase = new File(appBase, pathname);
        if (docBase.exists()) {
            // War file is already installed
            return (docBase.getAbsolutePath());
        }

        // Create the new document base directory
        if (!docBase.mkdir()) {
            String msg = MessageFormat.format(rb.getString(UNABLE_CREATE_DIRECTORY_EXCEPTION),
                                              docBase);
            throw new IOException(msg);
        }

        // Expand the WAR into the new document base directory
        String canonicalDocBasePrefix = docBase.getCanonicalPath();
        if (!canonicalDocBasePrefix.endsWith(File.separator)) {
            canonicalDocBasePrefix += File.separator;
        }
        JarURLConnection juc = (JarURLConnection) war.openConnection();
        juc.setUseCaches(false);
        JarFile jarFile = null;
        InputStream input = null;
        boolean success = false;
        try {
            jarFile = juc.getJarFile();
            Enumeration jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) jarEntries.nextElement();
                String name = jarEntry.getName();
                File expandedFile = new File(docBase, name);
                if (!expandedFile.getCanonicalPath().startsWith(
                        canonicalDocBasePrefix)) {
                    // Trying to expand outside the docBase
                    // Throw an exception to stop the deployment
                    String msg = MessageFormat.format(rb.getString(ARCHIVE_IS_MALFORMED_EXCEPTION),
                                                      new Object[] {war, name});
                    throw new IllegalArgumentException(msg);
                }
                int last = name.lastIndexOf('/');
                if (last >= 0) {
                    File parent = new File(docBase,
                                           name.substring(0, last));
                    if (!parent.mkdirs() && !parent.isDirectory()) {
                        String msg = MessageFormat.format(rb.getString(UNABLE_CREATE_DIRECTORY_EXCEPTION),
                                                          parent);
                        throw new IOException(msg);
                    }
                }
                if (name.endsWith("/")) {
                    continue;
                }
                input = jarFile.getInputStream(jarEntry);
                expand(input, expandedFile);
                long lastModified = jarEntry.getTime();
                if ((lastModified != -1) && (lastModified != 0)) {
                    if (!expandedFile.setLastModified(lastModified)) {
                        if (log.isLoggable(Level.WARNING)) {
                            log.log(Level.WARNING, FAILED_SET_LAST_MODIFIED_TIME_EXCEPTION,
                                    expandedFile.getAbsolutePath());
                        }
                    }
                }
                input.close();
                input = null;
            }
            success = true;
        } catch (IOException e) {
            throw e;
        } finally {
            if (!success) {
                // If something went wrong, delete expanded dir to keep things 
                // clean
                deleteDir(docBase);
            }
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable t) {
                    ;
                }
                input = null;
            }
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable t) {
                    ;
                }
                jarFile = null;
            }
        }

        // Return the absolute path to our new document base directory
        return (docBase.getAbsolutePath());

    }


    /**
     * Validate the WAR file found at the specified URL.
     *
     * @param host Host war is being installed for
     * @param war URL of the web application archive to be validated
     *  (must start with "jar:")
     * @param pathname Context path name for web application
     *
     * @exception IllegalArgumentException if this is not a "jar:" URL or if the
     *            WAR file is invalid
     * @exception IOException if an input/output error was encountered
     *            during validation
     */
    public static void validate(Host host, URL war, String pathname)
        throws IOException {

        // Make the appBase absolute
        File appBase = new File(host.getAppBase());
        if (!appBase.isAbsolute()) {
            appBase = new File(System.getProperty("catalina.base"),
                               host.getAppBase());
        }
        
        File docBase = new File(appBase, pathname);

        // Calculate the document base directory
        String canonicalDocBasePrefix = docBase.getCanonicalPath();
        if (!canonicalDocBasePrefix.endsWith(File.separator)) {
            canonicalDocBasePrefix += File.separator;
        }
        JarURLConnection juc = (JarURLConnection) war.openConnection();
        juc.setUseCaches(false);
        JarFile jarFile = null;
        try {
            jarFile = juc.getJarFile();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                String name = jarEntry.getName();
                File expandedFile = new File(docBase, name);
                if (!expandedFile.getCanonicalPath().startsWith(
                        canonicalDocBasePrefix)) {
                    // Entry located outside the docBase
                    // Throw an exception to stop the deployment
                    String msg = MessageFormat.format(rb.getString(ARCHIVE_IS_MALFORMED_EXCEPTION),
                                                    new Object[] {war, name});
                    throw new IllegalArgumentException(msg);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable t) {
                    // Ignore
                }
                jarFile = null;
            }
        }
    }


    /**
     * Copy the specified file or directory to the destination.
     *
     * @param src File object representing the source
     * @param dest File object representing the destination
     */
    public static boolean copy(File src, File dest) {
        
        boolean result = true;
        
        String files[] = null;
        if (src.isDirectory()) {
            files = src.list();
            result = dest.mkdir();
        } else {
            files = new String[1];
            files[0] = "";
        }
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; (i < files.length) && result; i++) {
            File fileSrc = new File(src, files[i]);
            File fileDest = new File(dest, files[i]);
            if (fileSrc.isDirectory()) {
                result = copy(fileSrc, fileDest);
            } else {
                FileChannel ic = null;
                FileChannel oc = null;
                try {
                    ic = (new FileInputStream(fileSrc)).getChannel();
                    oc = (new FileOutputStream(fileDest)).getChannel();
                    ic.transferTo(0, ic.size(), oc);
                } catch (IOException e) {
                    String msg = MessageFormat.format(rb.getString(ERROR_COPYING_EXCEPTION),
                                                      new Object[] {fileSrc, fileDest});
                    log.log(Level.SEVERE, msg, e);
                    result = false;
                } finally {
                    if (ic != null) {
                        try {
                            ic.close();
                        } catch (IOException e) {
                        }
                    }
                    if (oc != null) {
                        try {
                            oc.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
        return result;
        
    }
    
    
    /**
     * Delete the specified directory, including all of its contents and
     * sub-directories recursively. Any failure will be logged.
     *
     * @param dir File object representing the directory to be deleted
     */
    public static boolean delete(File dir) {
        // Log failure by default
        return delete(dir, true);
    }

    /**
     * Delete the specified directory, including all of its contents and
     * sub-directories recursively.
     *
     * @param dir File object representing the directory to be deleted
     * @param logFailure <code>true</code> if failure to delete the resource
     *                   should be logged
     */
    public static boolean delete(File dir, boolean logFailure) {
        boolean result;
        if (dir.isDirectory()) {
            result = deleteDir(dir, logFailure);
        } else {
            if (dir.exists()) {
                result = dir.delete();
            } else {
                result = true;
            }
        }
        if (logFailure && !result) {
            log.log(Level.SEVERE, DELETE_DIR_EXCEPTION, dir.getAbsolutePath());
        }
        return result;
     }

    
    /**
     * Delete the specified directory, including all of its contents and
     * sub-directories recursively. Any failure will be logged.
     *
     * @param dir File object representing the directory to be deleted
     */
    public static boolean deleteDir(File dir) {
        return deleteDir(dir, true);
    }

    /**
     * Delete the specified directory, including all of its contents and
     * sub-directories recursively.
     *
     * @param dir File object representing the directory to be deleted
     * @param logFailure <code>true</code> if failure to delete the resource
     *                   should be logged
     */
    public static boolean deleteDir(File dir, boolean logFailure) {

        String files[] = dir.list();
        if (files == null) {
            files = new String[0];
        }
        for (int i = 0; i < files.length; i++) {
            File file = new File(dir, files[i]);
            if (file.isDirectory()) {
                deleteDir(file, logFailure);
            } else {
                if (!file.delete() && logFailure) {
                    log.log(Level.SEVERE, DELETE_DIR_EXCEPTION, file.getAbsolutePath());
                }
            }
        }

        boolean result;
        if (dir.exists()) {
            result = dir.delete();
        } else {
            result = true;
        }
        
        if (logFailure && !result) {
            log.log(Level.SEVERE, DELETE_DIR_EXCEPTION, dir.getAbsolutePath());
        }
        
        return result;
    }


    /**
     * Expand the specified input stream into the specified directory, creating
     * a file named from the specified relative path.
     *
     * @param input InputStream to be copied
     * @param docBase Document base directory into which we are expanding
     * @param name Relative pathname of the file to be created
     *
     * @exception IOException if an input/output error occurs
     *
     * @deprecated
     */
    protected static void expand(InputStream input, File docBase, String name)
        throws IOException {

        File file = new File(docBase, name);
        expand(input, file);
    }

    /**
     * Expand the specified input stream into the specified file.
     *
     * @param input InputStream to be copied
     * @param file The file to be created
     *
     * @exception IOException if an input/output error occurs
     */
    private static void expand(InputStream input, File file)
        throws IOException {
        BufferedOutputStream output = null;
        try {
            output = 
                new BufferedOutputStream(new FileOutputStream(file));
            byte buffer[] = new byte[2048];
            while (true) {
                int n = input.read(buffer);
                if (n <= 0)
                    break;
                output.write(buffer, 0, n);
            }
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

    }


}
