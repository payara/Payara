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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.web.WebCheckMgrImpl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.EventObject;

/** 
 * Singleton Utility class to load war archive and get class loader
 * 
 * @author Jerome Dochez
 * @author Sheetal Vartak
 * @version 1.0
 */
public class WebTestsUtil implements VerifierEventsListener {

    protected final String listenerClassPath = "WEB-INF/classes";   
    protected final String libraryClassPath = "WEB-INF/lib";   
    
    private final String separator= System.getProperty("file.separator");
    private static File warFile = new File(System.getProperty("java.io.tmpdir"), "listenertmp");       
    private static WebTestsUtil util = null;
    private static ClassLoader cl = null; 
    
    
    /** 
     * <p>
     * Get the unique instance for this class
     * </p>
     */
    public static WebTestsUtil getUtil(ClassLoader cLoader) {
    
        if (util==null) {
	    util = new WebTestsUtil();
	    WebCheckMgrImpl.addVerifierEventsListener(util);
	    cl = cLoader;
        }    
        return util;
    }
    
    
    private void deleteDirectory(String oneDir) {
        
        File[] listOfFiles;
        File cleanDir;

        cleanDir = new File(oneDir);
        if (!cleanDir.exists())  // Nothing to do.  Return; 
            return;

        listOfFiles = cleanDir.listFiles();
        if(listOfFiles != null) {       
            for(int countFiles = 0; countFiles < listOfFiles.length; countFiles++) {                    
                if (listOfFiles[countFiles].isFile()) {
                    listOfFiles[countFiles].delete();
                } else { // It is a directory
                    String nextCleanDir =  cleanDir + separator + listOfFiles[countFiles].getName();
                    File newCleanDir = new File(nextCleanDir);
                    deleteDirectory(newCleanDir.getAbsolutePath());
                }                    
            }// End for loop
        } // End if statement            
 
        cleanDir.delete();
    }
    
    /**
     * <p>
     * Individual test completion notification event
     * </p>
     * @param e event object which source is the result of the individual test
     */
    public void testFinished(EventObject e) {
        // do nothing, we don't care
    }
    
    /**
     * <p>
     * Notification that all tests pertinent to a verifier check manager have 
     * been completed
     * </p>
     * @param e event object which source is the check manager for the
     * completed tests
     */
    public void allTestsFinished(EventObject e) {
        // remove tmp files
        if ((warFile != null) && (warFile.exists())) {
            deleteDirectory(warFile.getAbsolutePath());
        }
        warFile=null;
        util=null;
        cl=null;
        WebCheckMgrImpl.removeVerifierEventsListener(this);        
    }

    private ClassLoader getClassLoader() {
	return cl;
    }   
    
    /**
     * <p>
     * load a class from the war archive file
     * </p>
     * @param className the class to load
     * @return the class object if it can be loaded
     */
    public Class loadClass(String className) throws Throwable {

        if ((warFile==null || !warFile.exists()) 
            && (getClassLoader() == null)) {
            throw new ClassNotFoundException();
        }
	    Class c = getClassLoader().loadClass(className);
	    return c;
    }
}
