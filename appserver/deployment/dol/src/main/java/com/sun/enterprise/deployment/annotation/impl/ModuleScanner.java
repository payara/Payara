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

package com.sun.enterprise.deployment.annotation.impl;

import com.sun.enterprise.deployment.annotation.introspection.ClassFile;
import com.sun.enterprise.deployment.annotation.introspection.ConstantPoolInfo;
import com.sun.enterprise.deployment.annotation.introspection.DefaultAnnotationScanner;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.logging.LogDomains;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.glassfish.apf.Scanner;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.apf.impl.JavaEEScanner;
import org.glassfish.hk2.classmodel.reflect.*;
import javax.inject.Inject;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.util.concurrent.*;


/**
 * This is an abstract class of the Scanner interface for J2EE module.
 *
 * @author Shing Wai Chan
 */
public abstract class ModuleScanner<T> extends JavaEEScanner implements Scanner<T> {

    private static final int DEFAULT_ENTRY_BUFFER_SIZE = 8192;

    @Inject
    DefaultAnnotationScanner defaultScanner;

    protected File archiveFile = null;
    protected ClassLoader classLoader = null;
    protected Parser classParser = null;

    private Set<URI> scannedURI = new HashSet<URI>();

    private boolean needScanAnnotation = false;

    private static ExecutorService executorService = null;
    
    private Set<String> entries = new HashSet<String>();

    protected Logger logger = LogDomains.getLogger(DeploymentUtils.class, 
        LogDomains.DPL_LOGGER);

    public void process(ReadableArchive archiveFile,
            T bundleDesc, ClassLoader classLoader, Parser parser) throws IOException {
        File file = new File(archiveFile.getURI());
        setParser(parser);
        process(file, bundleDesc, classLoader);
        completeProcess(bundleDesc, archiveFile);
        calculateResults();
    }

    /**
     * Performs all additional work after the "process" method has finished.
     * <p>
     * This is a separate method from "process" so that the app client scanner can invoke
     * it from its overriding process method.  All post-processing logic needs to be
     * collected in this one place.
     *
     * @param bundleDescr
     * @param archive
     * @throws IOException
     */
    protected void completeProcess(T bundleDescr, ReadableArchive archive) throws IOException {
        addLibraryJars(bundleDescr, archive);
    }

    protected void calculateResults() {
        try {
            classParser.awaitTermination();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Annotation scanning interrupted", e);
            return;
        }
        Level logLevel = (System.getProperty("glassfish.deployment.dump.scanning")!=null?Level.INFO:Level.FINE);
        boolean shouldLog = logger.isLoggable(logLevel);
        ParsingContext context = classParser.getContext();
        for (String annotation: defaultScanner.getAnnotations()) {
            Type type = context.getTypes().getBy(annotation);

            // we never found anyone using that type
            if (type==null) continue;

            // is it an annotation
            if (type instanceof AnnotationType) {
                AnnotationType at = (AnnotationType) type;
                for (AnnotatedElement ae : at.allAnnotatedTypes()) {
                    // if it is a member (field, method), let's retrieve the declaring type
                    // otherwise, use the annotated type directly.
                    Type t = (ae instanceof Member?((Member) ae).getDeclaringType():(Type) ae);
                    if (t.wasDefinedIn(scannedURI)) {
                        if (shouldLog) {
                            logger.log(logLevel, "Adding " + t.getName()
                                    + " since " + ae.getName() + " is annotated with " + at.getName());
                        }
                        entries.add(t.getName());
                    }
                }

            } else
            // or is it an interface ?
            if (type instanceof InterfaceModel) {
                InterfaceModel im = (InterfaceModel) type;
                for (ClassModel cm : im.allImplementations()) {
                    if (shouldLog) {
                        logger.log(logLevel, "Adding " + cm.getName()
                                + " since it is implementing " + im.getName());
                    }
                    entries.add(cm.getName());
                }
            } else {
                logger.log(Level.SEVERE, "Inconsistent type definition, " + annotation +
                        " is neither an annotation nor an interface");
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Done with results");
        }

    }

    /**
     * This add extra className to be scanned.
     * @param className
     */
    protected void addScanClassName(String className) {
        if (className!=null && className.length()!=0)
            entries.add(className);
    }

    /**
     * This add all classes in given jarFile to be scanned.
     * @param jarFile
     */
    protected void addScanJar(File jarFile) throws IOException {
        try {
            /*
             * An app might refer to a non-existent JAR in its Class-Path.  Java
             * SE accepts that silently, and so will GlassFish.
             */
            if ( ! jarFile.exists()) {
                return;
            }
            scannedURI.add(jarFile.toURI());
            if (needScanAnnotation) {
                classParser.parse(jarFile, null);
            }
        } catch (ZipException ze) {
            logger.log(Level.WARNING, ze.getMessage() +  ": file path: " + jarFile.getPath());
        }
    }
    
    /**
     * This add all classes in given jarFile to be scanned.
     * @param jarURI
     */
    protected void addScanURI(final URI jarURI) throws IOException {
        addScanJar(new File(jarURI));
    }

    /**
     * This will include all class in directory to be scanned.
     * param directory
     */
    protected void addScanDirectory(File directory) throws IOException {
        scannedURI.add(directory.toURI());
        if (needScanAnnotation) {
            classParser.parse(directory, null);
        }
    } 
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Set<Class> getElements() {
        Set<Class> elements = new HashSet<Class>();
        if (getClassLoader() == null) {
            AnnotationUtils.getLogger().severe("Class loader null");
            return elements;
        }        

        for (String className : entries) {
            if (AnnotationUtils.getLogger().isLoggable(Level.FINE)) {
                AnnotationUtils.getLogger().fine("Getting " + className);
            }
            try {                
                elements.add(classLoader.loadClass(className));
            } catch (NoClassDefFoundError err) {
                AnnotationUtils.getLogger().log(Level.WARNING, "Error in annotation processing: " + err);
            } catch(ClassNotFoundException cnfe) {
                AnnotationUtils.getLogger().log(Level.WARNING, "Cannot load " + className + " reason : " + cnfe.getMessage(), cnfe);
            }
        }
        return elements;
    }

    protected void addLibraryJars(T bundleDesc, 
        ReadableArchive moduleArchive) {
        List<URI> libraryURIs = new ArrayList<URI>(); 
        try {
            if (bundleDesc instanceof BundleDescriptor) {
                libraryURIs = DOLUtils.getLibraryJarURIs((BundleDescriptor)bundleDesc, moduleArchive);
            }

            for (URI uri : libraryURIs) {
                File libFile = new File(uri);;
                if (libFile.isFile()) {
                    addScanJar(libFile);
                } else if (libFile.isDirectory()) {
                    addScanDirectory(libFile);
                }
            }
        } catch (Exception ex) {
            // we log a warning and proceed for any problems in 
            // adding library jars to the scan list
            logger.log(Level.WARNING, ex.getMessage());
        }       
    }

    @Override
    public Types getTypes() {
        return classParser.getContext().getTypes();
    }

    protected synchronized ExecutorService getExecutorService() {
        if (executorService != null) {
            return executorService;
        }
        Runtime runtime = Runtime.getRuntime();
       int nrOfProcessors = runtime.availableProcessors();
        executorService = Executors.newFixedThreadPool(nrOfProcessors, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("dol-jar-scanner");
                t.setDaemon(true);
                t.setContextClassLoader(getClass().getClassLoader());
                return t;
            }
        });
        return executorService;
    }

    protected void setParser(Parser parser) {
        if (parser == null) {
            // if the passed in parser is null, it means no annotation scanning
            // has been done yet, we need to construct a new parser
            // and do the annotation scanning here
            ParsingContext pc = new ParsingContext.Builder().logger(logger).executorService(getExecutorService()).build();
            parser = new Parser(pc);
            needScanAnnotation = true;
        }
        classParser = parser;
    }
}
