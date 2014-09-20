/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

public class FileClassLoader extends ClassLoader {
    String codebase;
    Hashtable cache = new Hashtable();

    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(FileClassLoader.class);

    public FileClassLoader(String codebase)
    {
	this.codebase = codebase;
    }

    private byte[] loadClassData(String name)
	throws IOException
    {
        // load the class data from the codebase
	String sep = System.getProperty("file.separator");
	String c = name.replace('.', sep.charAt(0)) + ".class";
	File file = new File(codebase + sep + c);
	if (!file.exists()) {
	    File wf = new File(codebase + sep + "WEB-INF" + sep + "classes" + sep + c);
	    if (wf.exists()) {
		file = wf;
	    }
	}
	FileInputStream fis = null;
        byte[] buf = null;
        try {
          fis = new FileInputStream(file);
          int avail = fis.available();
          buf = new byte[avail];
          fis.read(buf);
        } finally {
          if (fis != null)
            fis.close();
        }
	return buf;
    }
    
    String getClassName(File f) throws IOException, ClassFormatError {
	FileInputStream fis = null;
        byte[] buf = null;
        int avail = 0;
        try {
          fis = new FileInputStream(f);
          avail = fis.available();
          buf = new byte[avail];
          fis.read(buf);
        } finally {
          if (fis != null)
            fis.close();
        }
	Class c = super.defineClass(null, buf, 0, avail);
	return c.getName();
    }

    /**
     * @exception ClassNotFoundException if class load fails
     */
    public synchronized Class loadClass(String name, boolean resolve) 
	throws ClassNotFoundException
    {
        Class c = (Class)cache.get(name);
        if (c == null) {
	    try { 
                byte data[] = loadClassData(name);
                c = defineClass(null,data, 0, data.length);
                if( !name.equals(c.getName()) ) {
                    throw new ClassNotFoundException(localStrings.getLocalString("classloader.wrongpackage", "", new Object[] { name, c.getName() }));
                }
	    }
	    catch ( Exception ex ) {
		// Try to use default classloader. If this fails, 
		// then a ClassNotFoundException is thrown.
		c = Class.forName(name);
	    }
            cache.put(name, c);
        }
        if (resolve)
            resolveClass(c);
        return c;
    }

    public String toString()
    {
	return "FileClassLoader: Codebase = "+codebase+"\n";
    }
}
