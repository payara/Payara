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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.api.logging.LogHelper;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.stringsubs.Substitutable;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.FileEntry;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Factory class to get the files that has to undergo string substitution.
 */
class FileEntryFactory {

    private static final Logger _logger = SLogger.getLogger();
    private static final LocalStringsImpl _strings = new LocalStringsImpl(FileEntryFactory.class);

    /**
     * Create the {@link List} of {@link FileSubstitutionHandler} by processing the file path.
     * The path can point to a single file\directory  or can contain pattern or wild
     * card characters. A {@link FileSubstitutionHandler} point to a individual file eligible
     * for String substitution process.
     *
     * @param fileEntry
     * @return List of matching substitutable entries.
     */
    @SuppressWarnings("unchecked")
    List<Substitutable> getFileElements(FileEntry fileEntry)  {
        // TODO: Name attribute of file entry can not contain comma separated files.
        String pathEntries[] = fileEntry.getName().split(",");
        List<Substitutable> substituables = null;
        List<File> retrievedFiles  = null;
        for(String pathEntry : pathEntries) {
            String isRegex = fileEntry.getRegex();
            if(Boolean.getBoolean(isRegex) || "yes".equalsIgnoreCase(isRegex)) {
                File file = new File(pathEntry);
                File parentDir = file.getParentFile();
                if(parentDir == null || !parentDir.exists()) {
                    continue;
                }
                retrievedFiles = new ArrayList<File>();
                String expression = file.getName();
                String[] fileList = parentDir.list();
                Pattern pattern = Pattern.compile(expression);
                for(String fileName : fileList) {
                    Matcher matcher = pattern.matcher(fileName);
                    if(matcher.matches()) {
                        File matchingFile = new File(parentDir, fileName);
                        if(matchingFile.exists() && matchingFile.canRead() && matchingFile.canWrite()) {
                            retrievedFiles.add(matchingFile);
                        } else {
                        	if (_logger.isLoggable(Level.FINER)) {
                        		_logger.log(Level.FINER, _strings.get("skipFileFromSubstitution", matchingFile.getAbsolutePath()));
                        	}
                        }
                    }
                }
            }
            else {
                FileLister fileLocator = new FileLister();
                retrievedFiles = fileLocator.getFiles(fileEntry.getName());
            }
            if (retrievedFiles.isEmpty()) {
            	if (_logger.isLoggable(Level.FINER)) {
            		_logger.log(Level.FINER, _strings.get("noMatchedFile", pathEntry));
            	}
                continue;
            }
            if (substituables == null) {
                substituables = new ArrayList<Substitutable>(retrievedFiles.size());
            }
            for (File retrievedFile : retrievedFiles) {
                if (retrievedFile.exists()) {
                    try {
                        FileSubstitutionHandler substituable = retrievedFile.length() > SubstitutionFileUtil.getInMemorySubstitutionFileSizeInBytes() ?
                                new LargeFileSubstitutionHandler(retrievedFile) : new SmallFileSubstitutionHandler(retrievedFile);
                                substituables.add(substituable);
                    } catch (FileNotFoundException e) {
                    	LogHelper.log(_logger, Level.WARNING, SLogger.INVALID_FILE_LOCATION, e, retrievedFile);
                    }
                }
            }
        }
        return substituables == null ? Collections.EMPTY_LIST: substituables;
    }
}
