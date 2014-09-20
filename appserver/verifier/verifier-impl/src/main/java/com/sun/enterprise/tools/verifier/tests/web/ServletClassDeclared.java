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

import java.util.*;
import java.lang.reflect.Modifier;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deploy.shared.FileArchive;

/**
 * All Servlet class of an war bundle should be declared in the deployment
 * descriptor for portability
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class ServletClassDeclared extends WebTest implements WebCheck { 

    final String servletClassPath = "WEB-INF/classes";
    
    /** 
     *  All Servlet class of an war bundle should be declared in the deployment
     * 
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {
        
	Result result = getInitializedResult();
        // See bug #6332745
        if(getVerifierContext().getJavaEEVersion().compareTo(SpecVersionMapper.JavaEEVersion_5) >=0){
            result.setStatus(Result.NOT_APPLICABLE);
            return result;
        }
//	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneWarning = false;
        boolean foundOne=false;
        
//        File f =  Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
        result = loadWarFile(descriptor);
        
//        ZipFile zip = null;
        FileArchive arch = null;
        Enumeration entries= null;
        //ZipEntry entry;
        Object entry;

        try {
//            if (f == null) {
              String uri = getAbstractArchiveUri(descriptor);
              try {
                 arch = new FileArchive();
                 arch.open(uri);
                 entries = arch.entries();
               }catch (Exception e) { throw e; }
//            }
//            else {
//              zip = new ZipFile(f);
//              entries = zip.entries();
//            }
        } catch(Exception e) {
            e.printStackTrace();
	    result.failed(smh.getLocalString
				 (getClass().getName() + ".exception",
                                 "IOException while loading the war file [ {0} ]",
				  new Object[] {descriptor.getName()}));
            
            return result;
        }
        while (entries.hasMoreElements()) {
            entry  = entries.nextElement();
//            if (f == null) {
            String name = (String)entry;
//            }
//            else {
//               name = ((ZipEntry)entry).getName();
//            }
            if (name.startsWith(servletClassPath)) {
                if (name.endsWith(".class")) {
                    String classEntryName = name.substring(0, name.length()-".class".length());
                    classEntryName = classEntryName.substring(servletClassPath.length()+1, classEntryName.length());
                    String className = classEntryName.replace('/','.');
                    Class servletClass = loadClass(result, className);
                    if (!Modifier.isAbstract(servletClass.getModifiers()) &&
                            isImplementorOf(servletClass, "javax.servlet.Servlet")) {
                        foundOne=true;
                        // let's find out if this servlet has associated deployment descriptors...
                        Set servlets = descriptor.getServletDescriptors();
                        boolean foundDD = false;
                        for (Iterator itr = servlets.iterator();itr.hasNext();) {
                            WebComponentDescriptor servlet = (WebComponentDescriptor)itr.next();
                            String servletClassName = servlet.getWebComponentImplementation();
                            if (servletClassName.equals(className)) {
                                foundDD=true;
                                break;
                            }
                        }
                        if (foundDD) {
                            result.addGoodDetails(smh.getLocalString
                                (getClass().getName() + ".passed",
                                "Servlet class [ {0} ] found in war file is defined in the Deployement Descriptors",
                                new Object[] {className}));
                        } else {
                            oneWarning=true;                            
                            result.addWarningDetails(smh.getLocalString
                                (getClass().getName() + ".warning",
                                "Servlet class [ {0} ] found in war file is not defined in the Deployement Descriptors",
                                new Object[] {className}));
                        }
                    }
                }
            }
        }
        if (!foundOne) {
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no servlet implementation within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
        } else {
            if (oneWarning) {
                result.setStatus(Result.WARNING);
            } else {
                result.setStatus(Result.PASSED);
            }
        }
        return result;
    }
}
