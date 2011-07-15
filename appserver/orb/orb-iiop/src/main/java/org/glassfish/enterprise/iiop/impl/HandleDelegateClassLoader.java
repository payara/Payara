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

package org.glassfish.enterprise.iiop.impl;

import java.io.*;

public class HandleDelegateClassLoader
    extends ClassLoader
{
    
    public HandleDelegateClassLoader() {
        super();
    }
    
    protected Class findClass(String name)
        throws ClassNotFoundException
    {
        // This is called only if the class could not be loaded by
        // the parent class loader (see javadoc for loadClass methods).
        // Load the class from the current thread's context class loader.
        
        Class c = Thread.currentThread().getContextClassLoader().loadClass(name);
        
        return c;
    }
    
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (!name.equals("com.sun.enterprise.iiop.IIOPHandleDelegate")) {
            return super.loadClass(name, resolve);
        }
        
        Class handleDelClass = findLoadedClass(name);
        if (handleDelClass != null) {
            return handleDelClass;
        }
        
        try {
            // read the bytes for IIOPHandleDelegate.class
            ClassLoader resCl = Thread.currentThread().getContextClassLoader();
            if (Thread.currentThread().getContextClassLoader() == null)  {
                resCl = getSystemClassLoader();
            }
            InputStream is = resCl.getResourceAsStream("org/glassfish/enterprise/iiop/impl/IIOPHandleDelegate.class");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            byte[] buf = new byte[4096]; // currently IIOPHandleDelegate is < 4k
            int nread = 0;
            while ( (nread = is.read(buf, 0, buf.length)) != -1 ) {
                baos.write(buf, 0, nread);
            }
            baos.close();
            is.close();

            byte[] buf2 = baos.toByteArray();
            
            handleDelClass = defineClass(
            "org.glassfish.enterprise.iiop.impl.IIOPHandleDelegate",
            buf2, 0, buf2.length);
            
        } catch ( Exception ex ) {
            throw (ClassNotFoundException)new ClassNotFoundException(ex.getMessage()).initCause(ex);
        }
        
        if (resolve) {
            resolveClass(handleDelClass);
        }
        
        return handleDelClass;
    }
}
