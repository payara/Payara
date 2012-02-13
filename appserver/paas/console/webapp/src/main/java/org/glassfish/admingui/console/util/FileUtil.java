/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.console.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author anilam
 */
public class FileUtil {

    static public File inputStreamToFile(InputStream inputStream, String origFileName) throws IOException {

          /* We don't want to use a random tmpfile name, just use the same file name as uploaded.
           * Otherwise, OE will use this random filename to create the services, adding all the random number.
           */
//        int index = origFileName.indexOf(".");
//        String suffix = null;
//        if (index > 0) {
//            suffix = origFileName.substring(index);
//        }
//        String prefix = origFileName.substring(0, index);
//        File tmpFile = File.createTempFile("gf-" + prefix, suffix);
//        tmpFile.deleteOnExit();


        String tmpdir = System.getProperty("java.io.tmpdir");
        File tmpFile = new File(tmpdir, origFileName);
        if (tmpFile.exists()){
            if (!tmpFile.delete()){
                System.out.println("tmpFile cannot be deleted: " + tmpFile.getAbsolutePath());
            }
            tmpFile = new File(tmpdir, origFileName);
        }
        tmpFile.deleteOnExit();
          
        OutputStream out = new FileOutputStream(tmpFile);
        byte buf[] = new byte[4096];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        System.out.println("\ntmp is created." + tmpFile.getAbsolutePath());
        return tmpFile;
    }


}
