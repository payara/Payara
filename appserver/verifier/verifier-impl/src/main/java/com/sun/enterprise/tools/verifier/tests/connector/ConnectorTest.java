/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * ConnectorTest.java
 *
 * Created on September 20, 2000, 11:10 AM
 */

package com.sun.enterprise.tools.verifier.tests.connector;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
/**
 * Superclass for all connector tests, contains common services.
 *
 * @author  Jerome Dochez
 * @version 
 */
public abstract class ConnectorTest extends VerifierTest implements VerifierCheck, ConnectorCheck
{
        
    /**
     * <p>
     * run an individual test against the deployment descriptor for the 
     * archive the verifier is performing compliance tests against.
     * </p>
     *
     * @param descriptor deployment descriptor for the archive
     * @return result object containing the result of the individual test
     * performed
     */    
    public Result check(Descriptor descriptor) {
        return check((ConnectorDescriptor) descriptor);
    }
   
    /**
     * <p>
     * all connector tests should implement this method. it run an individual
     * test against the resource adapter deployment descriptor. 
     * </p>
     *
     * @param descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */    
    public abstract Result check(ConnectorDescriptor descriptor);     
    
    /**
     * <p>
     * Find a class implementating the interface in the jar files contained
     * in the connector rar file.
     * </p>
     * @param interfaceName the interface the class should implement
     * @return class implementing the interface or null if not present in the 
     * jar files
     */
    protected Class findImplementorOf(ConnectorDescriptor desc, String interfaceName) {
        /**
        * This is a little bit hectic but we have to go through all the 
        * jar files included in the rar file and load all classes implemented
        * in these jar files. For each class, we should look if the class
        * implements the requested interface "interfaceName"
        */
        
        // let's get the rar file
        try {
            String uri=getAbstractArchiveUri(desc);
            FileArchive arch = new FileArchive();
            arch.open(uri);
            for(Enumeration en = arch.entries();en.hasMoreElements();) {                    
                String entry = (String)en.nextElement();
                if (entry.endsWith(".jar")) {
                    // we found a jar file, let's load it
                    JarInputStream jis = new JarInputStream(arch.getEntry(entry));
                    try {
                        // Now we are going to iterate over the element of the jar file
                        ZipEntry ze = jis.getNextEntry();
                        while(ze!=null) {
                            String elementName = (String) ze.getName();
                            // Is this jar entry a java class file ?
                            if (elementName.endsWith(".class")) {
                                // we found a .class file let's load it and see if it does implement the interface
                                String className = elementName.substring(0, elementName.length()-".class".length()).replace('/','.');
                                //try {
                                ClassLoader jcl = getVerifierContext().getRarClassLoader();
                                Class c = Class.forName(className, false, jcl);

                                if (isImplementorOf(c, interfaceName))
                                    if(c.getSuperclass() != null)
                                        return c;

                            }
                            ze = jis.getNextEntry();
                        }
                    } catch(ClassNotFoundException cnfe) {
                        // We ignore this for now
                    } catch(NoClassDefFoundError cdnf) {
                        //continue to search other classes
                    } finally {
                        try {
                            if(jis != null)
                                jis.close();
                        } catch(IOException e) {}
                    }
                }
            }
        } catch(java.io.IOException ioe) {
            Verifier.debug(ioe);
        }
        return null;
    }
    
    /**
     * <p>
     * Check that a class overrides some methods defined in the java.lang.Object class
     * </p>
     *
     * @param clazz the implementation class
     * @param methodName the method name
     * @param parmTypes the method parameter types
     * @param methodSignature a human readable signature description of the method
     * @param result where to put the method lookup result
     * @return true if the method is overriden by the implementation class
     */
    protected boolean checkMethodImpl(Class clazz, String methodName, Class[] parmTypes, 
        String methodSignature, Result result) {
        
        Method m=null;
        Class c = clazz;
	    
        do {
            try {
                m = c.getDeclaredMethod(methodName, parmTypes);
            } catch(NoSuchMethodException nsme) {
            } catch(SecurityException se) {
            }
            c = c.getSuperclass();
        } while (m != null && c!=null && c != Object.class);            

        if (m==null) {
	    result.failed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.MethodOverride.failed", 
                "Warning: The class [ {0} ] does not override the method [ {1} ]",
                new Object[] {clazz.getName(), methodSignature }));                  
            return false;
        } else {
	    result.passed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.MethodOverride.passed", 
                "The class [ {0} ] overrides the method [ {1} ]",
                new Object[] {clazz.getName(), methodSignature }));                                       
            return true;
        }
    }
    
    /**
     * <p>
     * Look for an implementation of an interface in all the classes present 
     * in a jar file, setting the result object with the look up result
     * </p>
     *
     * @param interfaceName interface to look for an implementor
     * @param result where to put the look up result
     */
    protected boolean findImplementorOf(ConnectorDescriptor desc, String interfaceName, Result result) 
    {
        Class c = findImplementorOf(desc, interfaceName);
        if (c != null) {
	    result.passed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.findImplementor.passed", 
                "The class [ {0} ] implements the [ {1} ] interface",
                new Object[] {c.getName(), interfaceName}));    
            return true;
        } else {
	    result.failed(smh.getLocalString
	        ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.findImplementor.failed", 
                "Error: There is no implementation of the [ {0} ] provided",
                new Object[] {interfaceName}));        
            return false;
        }
    }
    
    /**
     * <p>
     * Check if a class or interface can be loaded from the archive file
     * </p>
     * 
     * @param className the class or interface name
     * @param result instance for test status
     * @return true if the class or interface can be loaded
     */
    protected boolean isClassLoadable(String className, Result result) {
        ClassLoader jcl = getVerifierContext().getClassLoader();
        try {
            Class.forName(className, false, jcl);
            result.passed(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.isClassLoadable.passed", 
                    "The class [ {0} ] is contained in the archive file",
                    new Object[] {className}));                
            return true;
        } catch(ClassNotFoundException cnfe) {
            result.failed(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.isClassLoadable.failed", 
                    "The class [ {0} ] is not contained in the archive file",
                    new Object[] {className}));                
            return true;
        }
    }

    protected String getAbstractArchiveUri(ConnectorDescriptor desc) {
        String archBase = getVerifierContext().getAbstractArchive().
                getURI().toString();
        final ModuleDescriptor moduleDescriptor = desc.getModuleDescriptor();
        if (moduleDescriptor.isStandalone()) {
            return archBase; // it must be a stand-alone module; no such physical dir exists
        } else {
            return archBase + "/" +
                    FileUtils.makeFriendlyFilename(moduleDescriptor.getArchiveUri());
        }
    }

}
