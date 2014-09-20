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

/*
 * AllJSPsMustBeCompilable.java
 *
 * Created on August 27, 2004, 11:57 AM
 */

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.net.URI;

import org.apache.jasper.JspC;
import org.apache.jasper.JasperException;
/**
 *
 * @author  ss141213
 */
public class AllJSPsMustBeCompilable extends WebTest implements WebCheck{

    public Result check(WebBundleDescriptor descriptor) {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        // initialize good result
        addGoodDetails(result, compName);
        result.addGoodDetails(smh.getLocalString(getClass().getName() + ".passed",
                                "All JSPs are compilable."));
        // set default status to PASSED
        result.setStatus(Result.PASSED);
        // initialize error results.
        addErrorDetails(result, compName);
        result.addErrorDetails(smh.getLocalString(getClass().getName() + ".exception",
                "Error: Some JSPs bundled inside [ {0} ] could not be compiled. See details below.",
                new Object[] {descriptor.getName()}));
        for(JasperException e : compile(descriptor)){
            result.failed(formatMessage(descriptor, e.toString()));
        }
        return result;
    }

    protected List<JasperException> compile(WebBundleDescriptor descriptor) {
        String archiveUri = getAbstractArchiveUri(descriptor);
        File outDir=getVerifierContext().getOutDir();
        logger.log(Level.INFO, "Compiling JSPs in [ " +new File(archiveUri).getName()+ " ]");
        JspC jspc=new JspC();
        jspc.setUriroot(new File(URI.create(archiveUri)).getAbsolutePath());
        jspc.setCompile(true);
        jspc.setOutputDir(outDir.getAbsolutePath());
        jspc.setFailOnError(false);
        setClassPath(jspc);
        jspc.setSchemaResourcePrefix("/schemas/");
        jspc.setDtdResourcePrefix("/dtds/");
        if (logger.isLoggable(Level.FINEST)) 
            jspc.setVerbose(1);
        try {
            jspc.execute();
        } catch(JasperException je) {
            List<JasperException> errors = jspc.getJSPCompilationErrors();
            errors.add(je);
            return errors;
        }
        return jspc.getJSPCompilationErrors();
    }

    /**
     * @param jspc
     */
    private void setClassPath(JspC jspc) {
        /*
        ClassPath settings of JspC is tricky.
        JspC has two classpath settings, viz: classPath and systemClassPath.
        systemClassPath defaults to the value set in java.class.path
        system property. It is not clear how they differ.
        From the JspC code, it looks like that JspC uses systemClassPath to
        construct what they call a systemCL and it uses classPath to construct
        what they call loader whose parent is set as systemCL. loader is set as
        ThreadContextClassLoader during jspc. More over, when JspC invokes
        javac, it combines classPath and systemClassPath sets that as
        javac classpath. So, it does not really matter what goes in which variable
        as long as the order is preserved.
        There are three different modes:
        1. standalone portable mode, 2. standalone appserver mode and
        3. deployment backend.
        In all the cases, we only set the classPath. systemClassPath is
        internally set correctly using java.class.path variable in JspC.
        Any way our WebArchiveClassesLoadable will take care of
        proprietary class linking.
        */
        String cp = getVerifierContext().getClassPath();
        logger.log(Level.FINE, "JSPC classpath "+ cp);
        jspc.setClassPath(cp);
    }

    private String formatMessage(WebBundleDescriptor descriptor, String message) {
        if (message == null || descriptor == null) return null;
        String formattedMessage = message;
        String archiveUri = getAbstractArchiveUri(descriptor);
        int index = message.indexOf(archiveUri);
        if(index != -1) {
            formattedMessage = message.substring(index + archiveUri.length() + 1);
        }
        return formattedMessage;
    }

}
