/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.installer.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openinstaller.util.ClassUtils;

/**
 * File/Directory related operations.
 * @author sathyan
 */
public class FileUtils {

    /* Check if the given dir/file is a Symbolic link or not.
     * @param dirName File/Directory to verify.
     * @return true/false.
     */
    /* LOGGING */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    static public boolean isSymLink(File dirName) throws IOException {
        boolean symLink = true;
        LOGGER.log(Level.FINEST, dirName.getAbsolutePath());
        if (OSUtils.isWindows()) {
            symLink =
                    dirName.getAbsolutePath().equalsIgnoreCase(dirName.getCanonicalPath());
        } else {
            symLink =
                    dirName.getAbsolutePath().equals(dirName.getCanonicalPath());

        }
        return symLink;
    }

    /* Recursively delete the given directory and its contents.
     * @param objName File/Directory root to delete.
     */
    static public void deleteDirectory(File objName) throws IOException {

        File filesList[] = objName.listFiles();

        for (File eachFile : filesList) {
            LOGGER.log(Level.FINEST, eachFile.getAbsolutePath());
            if (eachFile.isDirectory()) {
                if (isSymLink(eachFile)) {
                    deleteDirectory(eachFile);
                } else {
                    eachFile.delete();
                }
            }

            if (eachFile.isFile()) {
                eachFile.delete();
            }
        }
        objName.delete();
    }

    /* Assign execute permissions on specified file/directory.
     * @param tFile File object.
     */
    public static void setExecutable(String fileName) throws SecurityException {
        LOGGER.log(Level.FINEST, fileName);
        if (isFileExist(fileName)) {
            File tFile = new File(fileName);
            tFile.setExecutable(true);
        }
    }


    /* Assign execute permissions for all files in specified directory 
     * @param  dirName content directory.
     */
    public static void setAllFilesExecutable(String dirName) throws SecurityException {
        LOGGER.log(Level.FINEST,dirName);
        if (isFileExist(dirName)) {
            File tDir = new File(dirName);
            if (tDir.isDirectory()) {
                File filesList[] = tDir.listFiles();

                for (File eachFile : filesList) {
                     LOGGER.log(Level.FINEST, eachFile.getAbsolutePath());
                     if (eachFile.isFile()) {
                         eachFile.setExecutable(true);
                     }
                }
            }
        }
    }

    /* return true/false if the given file exists.
     * @param fileName file/directory to check.
     * @return true/false.
     */
    public static boolean isFileExist(String fileName) {
        LOGGER.log(Level.FINEST, fileName);
        return new File(fileName).exists();
    }

    /* Delete the given file.
     * @param fileName file/directory to check.
     * @return true/false.
     */
    public static boolean deleteFile(String fileName) {
        LOGGER.log(Level.FINEST, fileName);
        if (!isFileExist(fileName)) {
            return false;
        }
        return new File(fileName).delete();

    }

    /* Create directory.
     * @param pathToCreate Directory path to create.
     * @return true/false.
     */
    public static boolean createDirectory(String pathToCreate) {
        boolean retStatus = true;
        LOGGER.log(Level.FINEST, pathToCreate);
        try {
            File dirPath = new File(pathToCreate);
            dirPath.mkdirs();
        } catch (Exception e) {
            /* Cannot create, could possible be SecurityException */
            retStatus = false;
        }
        return retStatus;
    }
}
