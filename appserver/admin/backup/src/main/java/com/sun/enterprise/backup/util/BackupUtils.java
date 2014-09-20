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

package com.sun.enterprise.backup.util;

import com.sun.enterprise.util.OS;

import java.io.*;
import java.util.*;

public class BackupUtils {

    private BackupUtils() {
    }
    
    public static boolean protect(File f) {
        if(!f.exists())
            return true;

        try {
            File[]   files = null;
            boolean  ret = true;
            
            if(f.isDirectory()) {
                // chmod to rwx------
                // and chmod files inside dir to rw-------
                // 6580444 -- make any subdirs drwxr-xr-x (0755) otherwise we 
                // can't delete the whole tree as non-root for some reason.
                // notice that the original file, if a directory, WILL have 0700
                // this is exactly the way the permissions exist in the original
                // domain files.

                if (!f.setExecutable(true, true)) {
                    ret = false;
                }
                
                if (!f.setReadable(true, true)) {
                    ret = false;
                }

                if (!f.setWritable(true, true)) {
                    ret = false;
                }
                
                files = f.listFiles();
                
                if(files == null || files.length < 1)
                    return ret;
            } else {
                files = new File[] { f };
            }

            for(File file : files) {
                if(file.isDirectory()) {
                    if (!file.setExecutable(true, false)) {
                        ret = false;
                    }
                
                    if (!file.setReadable(true, false)) {
                        ret = false;
                    }

                    if (!file.setWritable(true, true)) {
                        ret = false;
                    }
                } else {
                    if (!file.setExecutable(false, false)) {
                        ret = false;
                    }
                
                    if (!file.setReadable(true, true)) {
                        ret = false;
                    }

                    if (!file.setWritable(true, true)) {
                        ret = false;
                    }
                }
            }
            return ret;
        } catch(Exception e) {
            return false;
        }
    }
    
    /**
     **/
    
    public static boolean makeExecutable(File f)
    {
        if(!OS.isUNIX())
            return true;	// no such thing in Windows...
        
        if(!f.exists())
            return true; // no harm, no foul...
        
        if(!f.isDirectory())
            return makeExecutable(new File[] { f} );
        
        // if we get here -- f is a directory
        
        return makeExecutable(f.listFiles());
    }
    
    private static boolean makeExecutable(File[] files) {

        boolean ret = true;

        // WBN October 2005
        // dirspace bugfix -- what if there is a space in the dirname?  trouble!
        // changed the argument to a File array
        
        // we are using a String here so that you can pass in a bunch
        // of space-separated filenames.  Doing it one at a time would be inefficient...
        // make it executable for ONLY the user
        
        // Jan 19, 2005 -- rolled back the fix for 6206176.  It has been decided
        // that this is not a bug but rather a security feature.
        
        
        // BUGFIX: 6206176
        // permissions changed from 744 to 755.
        // The reason is that if user 'A' does a restore then user 'A' will be the only
        // user allowed to start or stop a domain.  Whether or not a user is allowed to start a domain
        // needs to be based on the AppServer authentication mechanism (username-password) rather
        // than on the OS authentication mechanism.
        // This case actually is common:  user 'A' does the restore, root tries to start the restored domain.
        
        if(files == null || files.length <= 0)
            return true;
        
        for(File f : files) {
            if (!f.setExecutable(true, true)) {
                ret = false;
            }
                
            if (!f.setReadable(true, false)) {
                ret = false;
            }

            if (!f.setWritable(false, false)) {
                ret = false;
            }
        }

        return ret;
    }
}
