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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.ServletFilter;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompiler;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.util.WebArchiveLoadableHelper;
import com.sun.enterprise.deploy.shared.FileArchive;
import org.glassfish.web.deployment.descriptor.ErrorPageDescriptor;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;

import java.util.*;
import java.io.File;

/**
 * A j2ee archive should be self sufficient and should not depend on any classes to be 
 * available at runtime.
 * The test checks whether all the classes found in the web archive are loadable and the
 * classes that are referenced inside their code are also loadable within the jar. 
 * 
 * @author Vikas Awasthi
 */
public class WebArchiveClassesLoadable extends WebTest implements WebCheck { 
    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String archiveUri = getAbstractArchiveUri(descriptor);
        
        Iterator entries;
        try{
            entries=getClassNames(descriptor).iterator();
        } catch(Exception e) {
//            e.printStackTrace();
            result.failed(smh.getLocalString(getClass().getName() + ".exception",
                                             "Error: [ {0} ] exception while loading the archive [ {1} ].",
                                              new Object[] {e, descriptor.getName()}));
            return result;
        }
        
        boolean allPassed = true;
        ClosureCompiler closureCompiler=getVerifierContext().getClosureCompiler();

        // org.apache.jasper takes care of internal JSP stuff
        ((ClosureCompilerImpl)closureCompiler).addExcludedPattern("org.apache.jasper");

        // DefaultServlet takes care of the default servlet in GlassFish.
        // For some reason, for every web app, this is returned as a component
        ((ClosureCompilerImpl)closureCompiler).addExcludedClass("org.apache.catalina.servlets.DefaultServlet");
        if(getVerifierContext().isAppserverMode())
        	((ClosureCompilerImpl)closureCompiler).addExcludedPattern("com.sun.enterprise");

        while (entries.hasNext()) {
                String className=(String)entries.next();
                boolean status=closureCompiler.buildClosure(className);
                allPassed=status && allPassed;
        }
        if (allPassed) {
            result.setStatus(Result.PASSED);
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                (getClass().getName() + ".passed",
                "All the classes are loadable within [ {0} ] without any linkage error.",
                new Object[] {archiveUri}));
        } else {
            result.setStatus(Result.FAILED);
            addErrorDetails(result, compName);
            result.addErrorDetails(WebArchiveLoadableHelper.getFailedResults(closureCompiler, getVerifierContext().getOutDir()));
            result.addErrorDetails(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.loadableError",
                    "Please either bundle the above mentioned classes in the application " +
                    "or use optional packaging support for them."));
        }
        return result;
    }  
    
    /**
     * Looks for Servlet classes, ServletFilter classes, Listener classes and 
     * Exception classes in the webBundleDescriptor. The closure is computed
     * starting from these classes. 
     * @param descriptor
     * @return returns a list of class names in the form that can be used in 
     * classloader.load()
     * @throws Exception
     */ 
    private List getClassNames(WebBundleDescriptor descriptor) throws Exception{
        final List<String> results=new LinkedList<String>();
        for(Object obj : descriptor.getServletDescriptors()) {
            String servletClassName = (WebComponentDescriptor.class.cast(obj))
                    .getWebComponentImplementation();
            results.add(servletClassName);
        }
        
        for (Object obj : descriptor.getServletFilterDescriptors()) {
            String filterClassName = (ServletFilter.class.cast(obj)).getClassName();
            results.add(filterClassName);
        }
        
        for (Object obj : descriptor.getAppListenerDescriptors()) {
            String listenerClassName = (AppListenerDescriptor.class.cast(obj)).getListener();
            results.add(listenerClassName);
        }
        
        results.addAll(getVerifierContext().getFacesConfigDescriptor().getManagedBeanClasses());
        
        Enumeration en = ((WebBundleDescriptorImpl)descriptor).getErrorPageDescriptors();
        while (en.hasMoreElements()) {
            ErrorPageDescriptor errorPageDescriptor = (ErrorPageDescriptor) en.nextElement();
            String exceptionType = errorPageDescriptor.getExceptionType();
            if (exceptionType != null && !exceptionType.equals(""))
                results.add(exceptionType);
        }
        
        File file = getVerifierContext().getOutDir();
        if(!file.exists())
            return results;

        FileArchive arch= new FileArchive();
        arch.open(file.toURI());
        Enumeration entries = arch.entries();
        while(entries.hasMoreElements()){
            String name=(String)entries.nextElement();
            if(name.startsWith("org/apache/jsp") && name.endsWith(".class"))
                results.add(name.substring(0, name.lastIndexOf(".")).replace('/','.'));
        }
        return results;
    }
    
}
