/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Utility class for the substitutable files.
 */
public class SubstitutionFileUtil
{
    private static final Logger _logger = SLogger.getLogger();
            
    private static final LocalStringsImpl _strings = new LocalStringsImpl(SubstitutionFileUtil.class);

    private static final String INMEMORY_SUBSTITUTION_FILE_SIZE_IN_KB = "inmemory.substitution.file.size.in.kb";
    private static final int DEFAULT_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_KB = 10240;
    private static int PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES = 0;

    /**
     * The maximum copy byte count.
     */
    public static final int MAX_COPY_BYTE_COUNT = (64 * 1024 * 1024) - (32 * 1024);

    /**
     * Gets the maximum file size for which in-memory substitution can be performed.
     *
     * @return Max file size in bytes to perform in-memory substitution.
     */
    public static int getInMemorySubstitutionFileSizeInBytes() {
        if (PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES > 0) {
            return PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES;
        }
        try {
            PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES = Integer.parseInt(
                    StringSubstitutionProperties.getProperty(INMEMORY_SUBSTITUTION_FILE_SIZE_IN_KB)) * 1024;
        } catch (Exception e) {
            _logger.log(Level.INFO, SLogger.MISSING_MEMORY_FILE_SIZE);
            PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES = DEFAULT_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_KB;
        }
        return PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES > 0 ?
                PROVIDED_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_BYTES : DEFAULT_INMEMORY_SUBSTITUTION_FILE_SIZE_IN_KB;
    }

    /**
     * Create a directory with the given prefix.
     * 
     * @param prefix Prefix for the directory name.
     * @return An extraction directory.
     */
    public static File setupDir(String prefix) throws IOException {
        String extractBase = System.getProperty("user.dir");
        File extractDir = null;
        File extractBaseFile = new File(extractBase);
        if (!extractBaseFile.mkdirs()) {
            _logger.log(Level.WARNING, SLogger.DIR_CREATION_ERROR,
                    extractBaseFile.getAbsolutePath());
        }
        extractDir = File.createTempFile(prefix, null, extractBaseFile);
        // ensure it's a directory
        if (extractDir.delete()) {
        	if (_logger.isLoggable(Level.FINE)) {
        		_logger.log(Level.FINE, _strings.get("recreateDirectories", extractDir.getAbsolutePath()));
        	}
        }
        if (!extractDir.mkdirs()) {
            _logger.log(Level.WARNING, SLogger.DIR_CREATION_ERROR, extractDir.getAbsolutePath());
        }
        return extractDir;
    }

    /**
     * Delete's the given file, if the file is a directory then method will
     * recursively delete the content of it. 
     *
     * @param file File to delete.
     */
    public static void removeDir(File file) {
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for(File f : files) {
                removeDir(f);
            }
        }
        if (!file.delete()) {
        	if (_logger.isLoggable(Level.FINE)) {
        		_logger.log(Level.FINE, _strings.get("failureInFileDeletion", file.getAbsolutePath()));
        	}
        }
    }
}