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
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import org.glassfish.apf.Scanner;
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
import java.util.logging.LogRecord;
import java.net.URL;
import java.util.concurrent.*;

import org.glassfish.logging.annotation.LogMessageInfo;

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

    public static final Logger deplLogger = com.sun.enterprise.deployment.util.DOLUtils.deplLogger;

  @LogMessageInfo(message = "Exception caught during annotation scanning.", cause="An exception was caught that indicates that the annotation is incorrect.", action="Correct the annotation.", level="SEVERE")
      private static final String ANNOTATION_SCANNING_EXCEPTION = "AS-DEPLOYMENT-00005";

  @LogMessageInfo(message = "Adding {0} since {1} is annotated with {2}.", level="INFO")
      private static final String ANNOTATION_ADDED = "AS-DEPLOYMENT-00006";

  @LogMessageInfo(message = "Adding {0} since it is implementing {1}.", level="INFO")
      private static final String INTERFACE_ADDED = "AS-DEPLOYMENT-00007";

  @LogMessageInfo(message = "Inconsistent type definition.  {0} is neither an annotation nor an interface.", cause="The annotation is incorrect.", action="Correct the annotation.", level="SEVERE")
      private static final String INCORRECT_ANNOTATION = "AS-DEPLOYMENT-00008";

  @LogMessageInfo(message = "The exception {0} occurred while examining the jar at file path:  {1}.", level="WARNING")
      private static final String JAR_EXCEPTION = "AS-DEPLOYMENT-00009";

  @LogMessageInfo(message = "No classloader can be found to use", cause="The archive being processed is not correct.", action="Examine the archive to determine what is incorrect.", level="SEVERE")
      private static final String NO_CLASSLOADER = "AS-DEPLOYMENT-00010";

  @LogMessageInfo(message = "Error in annotation processing: {0}.", level="WARNING")
      private static final String ANNOTATION_ERROR = "AS-DEPLOYMENT-00011";

  @LogMessageInfo(message = "Cannot load {0}  reason : {1}.", level="WARNING")
    private static final String CLASSLOADING_ERROR = "AS-DEPLOYMENT-00012";

  @LogMessageInfo(message = "An exception was caught during library jar processing:  {0}.", level="WARNING")
    private static final String LIBRARY_JAR_ERROR = "AS-DEPLOYMENT-00013";

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
            deplLogger.log(Level.SEVERE,
                           ANNOTATION_SCANNING_EXCEPTION,
                           e);
            return;
        }
        Level logLevel = (System.getProperty("glassfish.deployment.dump.scanning")!=null?Level.INFO:Level.FINE);
        boolean shouldLog = deplLogger.isLoggable(logLevel);
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
                          if (Level.INFO.equals(logLevel)) {
                            deplLogger.log(Level.INFO,
                                           ANNOTATION_ADDED,
                                           new Object[] { t.getName(),
                                                          ae.getName(),
                                                          at.getName() });
                          } else {
                            deplLogger.log(Level.FINE, "Adding " + t.getName()
                                           + " since " + ae.getName() + " is annotated with " + at.getName());
                          }
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
                      if (Level.INFO.equals(logLevel)) {
                        deplLogger.log(Level.INFO,
                                       INTERFACE_ADDED,
                                       new Object[] { cm.getName(),
                                                      im.getName() });
                      } else {
                        deplLogger.log(Level.FINE,
                                       "Adding " + cm.getName()
                                       + " since it is implementing " + im.getName());
                      }
                    }
                    entries.add(cm.getName());
                }
            } else {
                deplLogger.log(Level.SEVERE,
                               INCORRECT_ANNOTATION,
                               annotation);
            }
        }
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.log(Level.FINE,
                           "Done with results");
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
            deplLogger.log(Level.WARNING,
                           JAR_EXCEPTION,
                           new Object[] { ze.getMessage(),
                                          jarFile.getPath() });
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
            deplLogger.log(Level.SEVERE,
                           NO_CLASSLOADER);
            return elements;
        }        

        for (String className : entries) {
            if (deplLogger.isLoggable(Level.FINE)) {
                deplLogger.fine("Getting " + className);
            }
            try {                
                elements.add(classLoader.loadClass(className));
            } catch (NoClassDefFoundError err) {
                deplLogger.log(Level.WARNING,
                               ANNOTATION_ERROR,
                               err);
            } catch(ClassNotFoundException cnfe) {
              LogRecord lr = new LogRecord(Level.WARNING, CLASSLOADING_ERROR);
              Object args[] = { className,
                                cnfe.getMessage() };
              lr.setParameters(args);
              lr.setThrown(cnfe);
              deplLogger.log(lr);
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
            deplLogger.log(Level.WARNING,
                           LIBRARY_JAR_ERROR,
                           ex.getMessage());
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
            ParsingContext pc = new ParsingContext.Builder().logger(deplLogger).executorService(getExecutorService()).build();
            parser = new Parser(pc);
            needScanAnnotation = true;
        }
        classParser = parser;
    }
}
