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

package com.sun.enterprise.tools.verifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Reads the list of file names from the file cleandirs.txt and
 * calls deleteFile to recursively delete the directories.
 * NOTE : This independent class gets called only on W2K, where the
 * Normal cleanAll() call from doit() in verifier fails.
 */
public class TmpCleaner {

    private final static String TMPDIR = System.getProperty("java.io.tmpdir");

    public void run() {

        // read the file
        try {
            String cleandirs = TMPDIR + File.separator + "cleandirs.txt"; // NOI18N
            File tmpfile = new File(cleandirs);
            if (!tmpfile.exists())
                return;
            BufferedReader br = new BufferedReader(new FileReader(cleandirs));

            try {
                do {
                    String str = br.readLine();
                    String file = TMPDIR + File.separator + str;
                    File toDelete = new File(file);
                    deleteFile(toDelete);
                    toDelete.deleteOnExit();
                } while (br.ready());
            } catch (Exception e) {
            }


            br.close();
            File f = new File(cleandirs);
            f.delete();
        } catch (Exception e) {
        }
    }

    private void deleteFile(File p_file) {
        String FILE_SEPARATOR = System.getProperty("file.separator");
        // If it is a directory, empty it first
        if (p_file.isDirectory()) {
            String[] dirList = p_file.list();
            for (int i = 0; i < dirList.length; i++) {

                File aFile = new File(
                        p_file.getPath() + FILE_SEPARATOR + dirList[i]);
                if (aFile.isDirectory()) {
                    deleteFile(aFile);
                }
                aFile.delete();
            }
        }
        p_file.delete();
    }


    public static void main(String[] args) {
        try {
            TmpCleaner t = new TmpCleaner();
            System.gc();
            t.run();
        } catch (Exception e) {
        }
        System.exit(0);
    }

}
