/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package com.sun.enterprise.tools.apt;

import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is processing the @{@link Service} annotation and generates
 * META-INF/services style text file for each interface annotated with @Contract
 *
 * @author Jerome Dochez
 *
 * @deprecated Not used.
 */
public class ServiceAnnotationProcessor extends AbstractProcessor {

    private boolean debug;
    private Map<String, ServiceFileInfo> serviceFiles = new HashMap<String, ServiceFileInfo>();

    /**
     * Creates a new instance of ServiceAnnotationProcessor
     */
    public ServiceAnnotationProcessor() {
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        debug = processingEnv.getOptions().containsKey("-Adebug"); // todo : bad
        if (debug) {
            printNote(processingEnv.getOptions().toString());
        }

        // load all existing meta-inf files...
        loadExistingMetaInfFiles();
    }

    /**
     * Loads all existing META-INF/services file from our destination directory
     * This is usuful because during incremental builds, not all source files
     * are recompiled, henve we cannot rewrite the META-INF/services file from
     * scratch each time, but append/remove entries from it as necessary.
     */
    protected void loadExistingMetaInfFiles() {
        String outDirectory = processingEnv.getOptions().get("-s");
        if (outDirectory==null) {
            outDirectory = System.getProperty("user.dir");
        }
        File outDir = new File(new File(outDirectory),"META-INF/services").getAbsoluteFile();
        if (debug) {
            printNote("Output dir is " + outDir.getAbsolutePath());
        }

        if (!outDir.exists()) {
            return;
        }
        File[] listFiles = outDir.listFiles();
        if (listFiles == null || listFiles.length == 0) return;
        for (File file : listFiles) {
            if(file.isDirectory())  continue;
            Set<String> entries = new HashSet<String>();
            FileReader reader = null;
            LineNumberReader lineReader = null;
            try {
                reader = new FileReader(file);
                lineReader = new LineNumberReader(reader);
                String line = lineReader.readLine();
                while (line != null) {
                    entries.add(line);
                    line = lineReader.readLine();
                }
            } catch (IOException e) {
                printError(e.getMessage());
            } finally {
                try {
                    if (lineReader != null) lineReader.close();
                    if (reader != null) reader.close();
                } catch(IOException ioe) {

                }
            }
            ServiceFileInfo info = new ServiceFileInfo(file.getName(), entries);
            serviceFiles.put(file.getName(), info);
        }
    }

    /**
     * Annotation processor entry point, we are using a visitor pattern the visit
     * only the class declaration.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ListClassVisitor listClassVisitor = new ListClassVisitor();

        Collection<TypeElement> classes = new ArrayList<TypeElement>();
        filterClasses(classes, roundEnv.getRootElements());
        for (TypeElement typeDecl : classes)
            typeDecl.accept(listClassVisitor, null);

        for (ServiceFileInfo info : serviceFiles.values()) {
            if (info.isDirty()) {
                if (debug) {
                    printNote("Creating META-INF/services " + info.getServiceName() + " file");
                }
                PrintWriter writer = new PrintWriter(info.getWriter());
                for (String implementor : info.getImplementors()) {
                    if (debug) {
                        printNote(" Implementor " + implementor);
                    }
                    writer.println(implementor);
                }
                writer.close();
            }
        }

        return true;
    }

    private void filterClasses(Collection<TypeElement> classes, Collection<? extends Element> elements) {
        for (Element element : elements) {
            if (element.getKind().equals(ElementKind.CLASS)) {
                classes.add((TypeElement) element);
                filterClasses(classes, ElementFilter.typesIn(element.getEnclosedElements()));
            }
        }
    }

    /**
     * Records the contract&lt;->service relationship.
     *
     * @param contractName
     *      FQCN of the contract type/annotation.
     * @param impl
     *      Implementation class.
     */
    private void createContractImplementation(String contractName, TypeElement impl) {
        ServiceFileInfo info;
        if (!serviceFiles.containsKey(contractName)) {
            info = new ServiceFileInfo(contractName, new HashSet<String>());
            serviceFiles.put(contractName, info);
        } else {
            info = serviceFiles.get(contractName);
        }

        if (info.getImplementors().add(impl.getQualifiedName().toString())) {
            try {
                info.createFile(processingEnv);
            } catch (IOException ioe) {
                printError(ioe.getMessage());
            }
        }
    }

    private void printNote(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    private void printError(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    /**
     * Invoked on all class declaration, use the cached META-INF/services
     * information and the mirror APIs to find out if classes need to be
     * added or removed from the generated service file.
     */
    private class ListClassVisitor extends SimpleElementVisitor6<Void, Void> {

        @Override
        public Void visitType(TypeElement element, Void aVoid) {
            if (debug) {
                printNote("Visiting " + element.getQualifiedName());
            }
            Service service = element.getAnnotation(Service.class);
            if (debug) {
                printNote("Service annotation = " + service);
            }
            if (service != null) {

                // look for contract in interfaces
                for (TypeMirror intf : element.getInterfaces()) {
                    checkContract((TypeElement) ((DeclaredType) intf).asElement(), element);
                }

                // look for contract in super classes
                TypeElement sd = element;
                while(sd.getSuperclass()!=null) {
                    sd = (TypeElement) ((DeclaredType) sd.getSuperclass()).asElement();
                    checkContract(sd, element);
                }
            } else {
                // we need to check if that class previously add an @Service annotation so
                // we remove the entry from the META-INF file
                for (ServiceFileInfo info : serviceFiles.values()) {
                    if (debug) {
                        printNote("Checking against " + info.getServiceName());
                    }
                    for (String implementor : info.getImplementors()) {
                        if (implementor.equals(element.getQualifiedName().toString())) {
                            if (debug) {
                                printNote("Need to remove " + implementor);
                            }
                            info.getImplementors().remove(implementor);
                            try {
                                info.createFile(processingEnv);
                            } catch(IOException ioe) {
                                printError(ioe.getMessage());
                            }
                            return null;
                        }
                    }
                }
            }

            // check for meta annotations
            for (AnnotationMirror a : element.getAnnotationMirrors()) {
                TypeElement atd = (TypeElement) a.getAnnotationType().asElement();
                Contract c = atd.getAnnotation(Contract.class);
                if(c!=null) {
                    // this is a contract annotation
                    createContractImplementation(atd.getQualifiedName().toString(), element);
                }
            }
            return null;
        }

        private void checkContract(TypeElement type, TypeElement impl) {
            Contract contract = type.getAnnotation(Contract.class);

            if (contract != null)
                createContractImplementation(type.getQualifiedName().toString(), impl);
        }
    }
}
