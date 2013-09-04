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
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Class to retrieve all the matching files for a given input path.
 * It also handles the processing of wild-card in the given path.
 */
final class FileLister {
    private static final Logger _log = SLogger.getLogger(); 
            
    private static final LocalStringsImpl _strings = new LocalStringsImpl(FileLister.class);

    final static String ASTERISK = "*";

    /**
     * Recursively find all files represented by path with wild-card character 
     * @param pathPattern path with wild-card character ASTERISK
     * @return List<File> all files whose paths match the pattern 
     */
    public List<File> getFiles(String pathPattern) {
        int asteriskIndex = pathPattern.indexOf(ASTERISK);
        // if input does not contain a wild-card character, return child files directly
        if (asteriskIndex < 0) {
            return getAllChildFiles(new File(pathPattern));
        }

        List<File> retrievedFiles = new LinkedList<File>();

        // try twice to handle '/' in windows
        // if the first try of parsing pathWithPattern fails, then replace all '/' 
        // with '\' and try again
        int numTries = 0;
        while (numTries < 2) {
            // path substring before the wild-card character
            String head = pathPattern.substring(0, asteriskIndex);
            // path substring after the wild-card character
            String tail = (asteriskIndex < pathPattern.length() - 1 ?
                    pathPattern.substring(asteriskIndex + 1) : "" );
            // get parent file of the head, add "temp" to handle input like /path/to/parent/* 
            File parent = (new File(head + "temp")).getParentFile();
            if (parent == null) {
            	if (_log.isLoggable(Level.FINEST)){
            		_log.log(Level.FINEST, _strings.get("parentFileNotSpecified"));
            	}
                parent = (new File(head + "temp").getAbsoluteFile()).getParentFile();
            }

            String pattern = pathPattern;
            // index of separator after the wild-card character
            int nextSeparator = -1;
            if (tail.length() > 0) {
                nextSeparator = pathPattern.indexOf(File.separator, asteriskIndex + 1);
            }
            // for input like /temp/bar*/var, create a filter with /temp/bar* 
            if (nextSeparator > asteriskIndex) {
                pattern = pathPattern.substring(0, nextSeparator);
            }
            WildCardFilenameFilter filter= new WildCardFilenameFilter(pattern);

            // get a filtered list of children 
            String childFileNames[] = parent.list(filter);
            if (childFileNames != null ) {
                for (String childName : childFileNames) {
                    String path = parent.getAbsolutePath() + File.separator + childName;
                    File file = new File(path);
                    // input ends with wild-card, e.g. /temp/*
                    if (nextSeparator < asteriskIndex) {
                        if (file.isFile()) {  // case of /path/to/childfile
                            retrievedFiles.add(file);
                        }
                        //TODO : Currently wild card character search do not allow to look for the
                        // matching files in sub-directories for e.g
                        // |-Directory
                        // | |-testFile1.txt
                        // | |-testDirectory
                        // | | |-testFile2.txt
                        // wild card search pattern 'Directory/test*' will retrieve only one file
                        // testFile1.txt to retrieve other file uncomment the below code.
                        /*else
            {
              retrievedFiles.addAll(getAllChildFiles(file));
            }*/
                    }
                    else { // input does not end with wild-card, e.g. /temp/*/bar
                        if (file.isDirectory()) { // file has to be a directory here
                            if (nextSeparator == pathPattern.length() - 1) { // case of /path/to/child/
                                retrievedFiles.addAll(getAllChildFiles(file));
                            }
                            else { // case of /path/to/child/blah
                                String newpattern = path + File.separator + pathPattern.substring(nextSeparator+1);
                                // recursively handle /path/to/child/blah
                                retrievedFiles.addAll(getFiles(newpattern));
                            }
                        } 
                        // do nothing if child is not a directory, which is impossible            
                    }
                }
            }

            // if retval is empty, and if the system's default file separator is '\' (windows),
            // then replace all '/' with '\' and try parsing the input again
            if (!retrievedFiles.isEmpty()) {
                break;
            }
            else if (File.separator.equals("\\") && pathPattern.contains("/")) {
                pathPattern = pathPattern.replace("/", File.separator);
                if (_log.isLoggable(Level.FINEST)) {
                	_log.log(Level.FINEST, "detected \"/\" in pathWithPattern on Windows, replace with \"\\\"");
                }
                numTries++;
            } else {
                break;
            }
        }
        return retrievedFiles;
    }

    /**
     * Gets the list of child files. If the given file is a directory then all the
     * files under directory and sub-directories will be retrieved recursively.
     * @param rootfile
     * @return List<File>
     */
    public List<File> getAllChildFiles(File rootfile) {
        List<File> retFiles = new LinkedList<File>();
        if (!rootfile.exists()) {
            // No operation, return empty list
            _log.log(Level.INFO, SLogger.INVALID_FILE_LOCATION, rootfile.getAbsolutePath());
        }
        else if (!rootfile.isDirectory()) {
            retFiles.add(rootfile);
        }
        else {
            File files[] = rootfile.listFiles();
            for (File file : files) {
                retFiles.addAll(getAllChildFiles(file));
            }
        }
        return retFiles;
    }

    /**
     * Custom filename filter to deal with wild-card character
     */
   private static class WildCardFilenameFilter implements FilenameFilter {
        private String _pattern;
        private boolean _endsWithWc;

        public WildCardFilenameFilter(String pattern) {
            _pattern = pattern;
            _endsWithWc = _pattern.endsWith(ASTERISK);
        }

        @Override
        public boolean accept(File dir, String name) {
            String fullpath = dir + File.separator + name;
            StringTokenizer tokenizedPattern = new StringTokenizer(_pattern, ASTERISK);
            while (tokenizedPattern.hasMoreTokens()) {
                String subpattern = tokenizedPattern.nextToken();
                int start = fullpath.indexOf(subpattern);
                if ( start < 0 ) {
                    return false;
                }
                fullpath = fullpath.substring(start + subpattern.length());
            }
            return fullpath.length() == 0 || _endsWithWc;
        }
    }
}
