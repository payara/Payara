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

import java.io.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.lang.System;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.Vector;

public class Filter {

    static Vector<String> myMessageIds = new Vector();
    static Vector<String> myMissingMessageIds = new Vector();


    public Collection<File> listFiles(
            File directory,
            FilenameFilter filter,
            boolean recurse) {

        Vector<File> files = new Vector<File>();

        // Get files / directories in the directory
        File[] entries = directory.listFiles();

        // Go over entries
        for (File entry : entries) {

            // If there is no filter or the filter accepts the
            // file / directory, add it to the list
            if (filter == null || filter.accept(directory, entry.getName())) {
                files.add(entry);
            }

            // If the file is a directory and the recurse flag
            // is set, recurse into the directory
            if (recurse && entry.isDirectory()) {
                files.addAll(listFiles(entry, filter, recurse));
            }
        }

        // Return collection of files
        return files;
    }

    public void readFileToGetMessageIds(File file) {
        try {

            //System.out.println("\n\n..." + file.getAbsolutePath());

            BufferedReader rdr =
                    new BufferedReader(
                            new InputStreamReader(
                                    new java.io.FileInputStream(file)));
            String line = null;

            while ((line = rdr.readLine()) != null) {
                //System.out.println("\n..."+line);
                if (line.contains(":")
                        && line.contains("=")
                        && !line.startsWith("#")
                        && !line.contains(".cause")
                        && !line.contains(".check")) {
                    StringTokenizer st = new StringTokenizer(line, "=");
                    if (st.hasMoreTokens()) {
                        st.nextToken();
                        String tempString = st.nextToken().trim();
                        if (tempString.contains(":")) {
                            String id = tempString.substring(0, tempString.indexOf(":")).trim();
                            //System.out.println("Ids=" + id);
                            if (id.length() < 10 && !id.contains(" ") && id.matches("[a-zA-Z0-9]*")) {
                                myMessageIds.add(id);
                            }
                        }
                    }
                }
            }
            rdr.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception during readFileToGetMessageIds: " + e);
        }


    }

    public void readFileToGetMissingDiagnosticInfo(File file) {
        try {

            Vector<String> idVector = new Vector();
            //System.out.println("\n\n..." + file.getAbsolutePath());

            BufferedReader rdr =
                    new BufferedReader(
                            new InputStreamReader(
                                    new java.io.FileInputStream(file)));
            String line = null;

            while ((line = rdr.readLine()) != null) {
                //System.out.println("\n..."+line);
                if (line.contains(":")
                        && line.contains("=")
                        && !line.startsWith("#")
                        && !line.contains(".cause")
                        && !line.contains(".check")) {
                    StringTokenizer st = new StringTokenizer(line, "=");
                    if (st.hasMoreTokens()) {
                        st.nextToken();
                        String tempString = st.nextToken().trim();
                        if (tempString.contains(":")) {
                            String id = tempString.substring(0, tempString.indexOf(":")).trim();
                            //System.out.println("Ids=" + id);
                            if (id.length() < 10 && !id.contains(" ") && id.matches("[a-zA-Z0-9]*")) {
                                myMissingMessageIds.add(id);
                            }
                        }
                    }
                }
            }

            rdr.close();

            for (String id : myMissingMessageIds) {
                rdr =
                        new BufferedReader(
                                new InputStreamReader(
                                        new java.io.FileInputStream(file)));
                line = null;
                while ((line = rdr.readLine()) != null) {
                    if (!line.contains("#")
                            && (line.contains(id + ".diag.cause") || line.contains(id + ".diag.check"))) {
                        idVector.add(id);
                    }
                }
                rdr.close();
            }

            for (String id : idVector) {
                myMissingMessageIds.remove(id);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception during readFileToGetMissingDiagnosticInfo: " + e);
        }


    }

    public void writeKeyToFile(Vector<String> ids, String fileName) {

        try {
            FileWriter fw = new FileWriter(new File(fileName));
            for (String id : ids) {
                fw.write(id + "\n");
            }
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception during Writing File");
        }

    }


    public static void main(String args[]) {

        boolean masterIds = false;
        boolean missingIds = false;
        boolean isGfHome = true;
        String gfHome = "";

        if (args.length < 2 || args[0]==null || args[1] ==null || ("${glassfish_home}").equals(args[0]) || ("${options}").equals(args[1]) 
				|| ("").equals(args[0]) || ("").equals(args[1]) ) {
            System.out.println("Missing Inputs...\n");
            System.out.println("Usage: java Filter <glassfish home> <missingId/masterId>\n");
            System.out.println("Help: missingId for generate list of Ids which has diagnostic info missing.\n");
            System.out.println("Help: masterId for generate master list of Ids.\n");
        } else {

            gfHome = args[0];

            File tempFile = new File(gfHome);
            if (tempFile == null || !tempFile.isDirectory()) {
                isGfHome = false;
            }

            if (args[1].equals("missingId")) {
                missingIds = true;
            }

            if (args[1].equals("masterId")) {
                masterIds = true;
            }

			if (!masterIds && !missingIds) {
				System.out.println("Missing Inputs...<missingId/masterId> value is missing as second argument.");
			} else
            if (!isGfHome) {
                System.out.println("Missing Inputs...<glassfish home> value is not proper as first argument.");                
            } else {

                FilenameFilter fnm = new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.equals("LogStrings.properties");
                    }
                };

                Collection<File> myCollection = new Filter().listFiles(new File(args[0]), fnm, true);
                //System.out.println(myCollection.size());

                if (masterIds) {
                    for (File myFile : myCollection) {
                        new Filter().readFileToGetMessageIds(myFile);
                    }
                    new Filter().writeKeyToFile(myMessageIds, "master-list-message-ids.txt");
                }                

                if (missingIds) {
                    for (File myFile : myCollection) {
                        new Filter().readFileToGetMissingDiagnosticInfo(myFile);
                    }
                    new Filter().writeKeyToFile(myMissingMessageIds, "missing-diagnostic-message-ids.txt");
                }
            }
        }

    }

}

