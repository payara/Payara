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

package org.glassfish.AnnotationProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.Diagnostic.Kind.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("org.glassfish.AnnotationProcessor.LogMessageInfo")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class LogMessageInfoAnnotationProcessor extends AbstractProcessor {

    public static final String RBFILE = "LogMessages.properties";
    HashMap<String, String> pkgMap = null;
    protected boolean debugging = true;

    @Override
    public boolean process (Set<? extends TypeElement> annotations, 
            RoundEnvironment env) {

        info("LogMessageInfoAnnotationProcessor Invoked.");

        if (!env.processingOver()) {
            Set<? extends Element> elements;
            String elementPackage = null;
            String prevElementPackage = null;
            LogResourceBundle lrb = null;

            // XXX: Should validate that if level is > INFO a cause and action 
            //      are manditory.

            // XXX: The annotation processor should try to detect the
            //      reuse of an existing log message id.
            //      Degree 1: processed during same build pass.
            //      Degree 2: processed during different builds.

            elements = env.getElementsAnnotatedWith(LogMessageInfo.class);
            Iterator<? extends Element> it = elements.iterator();

            while (it.hasNext()) {
                VariableElement element = (VariableElement)it.next();

                elementPackage = processingEnv.getElementUtils().getPackageOf(element).
                                                getQualifiedName().toString();

                debug("Annotated pkg: " + elementPackage);

                LogMessageInfo lmi = element.getAnnotation(LogMessageInfo.class);

                if (lmi.pkg() != null && !lmi.pkg().isEmpty()) {
                    info("LogMessageInfoAnnotationProcessor Invoked.");
                    info((String)element.getConstantValue() + 
                            ": Package overidden by " + lmi.pkg());
                    elementPackage = lmi.pkg();
                }

                // If the name of the package changed then we need to save
                // the currently open RB and open the RB appropriate to
                // this package.
                if (prevElementPackage != null && 
                        !prevElementPackage.equals(elementPackage)) {

                    StoreRB(lrb, prevElementPackage);

                    lrb = LoadRB(elementPackage);

                } else if (prevElementPackage == null) {
                    // Either we need to reuse the open RB or we need to 
                    // open/create a new RB.
                    lrb = LoadRB(elementPackage);
                }
                prevElementPackage = elementPackage;

                // Save the log message...
                lrb.put((String)element.getConstantValue(), lmi.message());
                // Save the message's comment if it has one...
                if (!lmi.comment().isEmpty()) {
                    lrb.putComment((String)element.getConstantValue(),
                            lmi.comment());
                }

                debug("Processing: " + (String)element.getConstantValue());

                // We are finsished.   Write the message bundle if needed.
                if (!it.hasNext()) {
                    StoreRB(lrb, prevElementPackage);
                }
            }
        }

        return true; // Claim the annotations
    }    

    /**
     * This method, given a pkg name will determine the path to the resource,
     * create the LogResourceBundle for that path and load any resources
     * from the existing resource bundle file.
     * 
     * @param pkg the package the resource bundle is relative
     * @return a LogResourceBundle
     */ 
    private LogResourceBundle LoadRB(String pkg) {

        String targetRBPath = null;
        LogResourceBundle lrb = null;
        BufferedReader bufferedReader = null;

        try {
            targetRBPath = getResourcePath(pkg);
        } catch (IllegalArgumentException e) {
            debug("LoadRB: Error: (directory instead of file): " + pkg,
                    e);
            error("Unable to load log message resource bundle for package: " + 
                    pkg, e);
        } catch (IOException e) {
            debug("LoadRB: Error: " + pkg, e);
            error("Unable to load log message resource bundle for package: " +
                    pkg, e);
        } 

        try {
            bufferedReader = new BufferedReader(new FileReader(targetRBPath));

            lrb = new LogResourceBundle().load(bufferedReader);
        } catch (FileNotFoundException e) {
            // Nothing there to load.  Return the empty resource bundle.
            return new LogResourceBundle();
        } catch (IOException e) {
            error("Unable to load log message resource bundle for package (load): " +
                    pkg, e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    error("Unable to load log message resource bundle for package: " +
                            pkg, e);
                }
            }
        }

        return lrb;
    }

    private void StoreRB(LogResourceBundle lrb, String pkg) {

        String targetRBPath = null;
        BufferedWriter bufferedWriter = null; 
        boolean propsWritten = false;

        try {
            targetRBPath = getResourcePath(pkg);
        } catch (IllegalArgumentException e) {
            debug("StoreRB: Error: (directory instead of file): " + pkg,
                    e);
            error("Unable to store log message resource bundle for package: " +
                    pkg, e);
        } catch (IOException e) {
            debug("StoreRB: Error: " + pkg, e);
            error("Unable to store log message resource bundle for package: " +
                    pkg, e);
        } 

        // We are switching packages so we need to store the prior RB.
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(targetRBPath));

            propsWritten = lrb.store(bufferedWriter);
        } catch (IOException e) {
            error("Unable to store log message resource bundle for package (store): " +
                    pkg, e);
        } finally {
            if (propsWritten) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    error("Unable to store log message resource bundle for package: " +
                            pkg, e);
                }
            }
        }
    }

    /**
     * We cache paths to the resource bundle because the compiler does not
     * allow us to call createResource() more than once for an object.
     * Note that in Java 7 getResource() throws FileNotFound if the
     * target resource does not exist.   The behavior is different in Java 6.
     * 
     * @param pkg
     * @return path to resource bundle relative to pkg 
     */
    private String getResourcePath(String pkg) throws IllegalArgumentException, 
            IOException {
        FileObject targetRBFile = null;

        if (pkgMap == null)
            pkgMap = new HashMap<String, String>();

        if (pkgMap.containsKey(pkg))
            return pkgMap.get(pkg);

        targetRBFile = processingEnv.getFiler().createResource(
            StandardLocation.CLASS_OUTPUT, pkg, RBFILE, 
            (javax.lang.model.element.Element[]) null);

        debug("getResroucePath: File path: " + targetRBFile.toUri().toString());

        pkgMap.put(pkg, targetRBFile.toUri().getPath());
        return targetRBFile.toUri().getPath();
    }

    protected void debug(String msg) {
        if (debugging)
            System.out.println(msg);
    }

    protected void debug(String msg, Throwable t) {
        if (debugging)
            System.out.println(msg + "Exception: " + t.getMessage());
    }

    protected void info(String msg) {
        debug(msg);
        processingEnv.getMessager().printMessage(Kind.NOTE, msg);
    }

    protected void warn(String msg) {
        processingEnv.getMessager().printMessage(Kind.WARNING, msg);
    }
    protected void warn(String msg, Throwable t) {
        String errMsg = msg + ": " + t.getMessage();

        processingEnv.getMessager().printMessage(Kind.WARNING, errMsg);
    }

    protected void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }
    protected void error(String msg, Throwable t) {
        String errMsg = msg + ": " + t.getMessage();

        processingEnv.getMessager().printMessage(Kind.ERROR, errMsg);
    }
}
