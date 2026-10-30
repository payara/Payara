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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2026] [Payara Foundation and/or its affiliates.]
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license.

package com.sun.enterprise.deployment.annotation.impl;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.annotation.introspection.DefaultAnnotationScanner;
import com.sun.enterprise.deployment.util.DOLUtils;
import jakarta.inject.Inject;
import org.glassfish.apf.Scanner;
import org.glassfish.apf.impl.JavaEEScanner;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jboss.jandex.AnnotationInstance;
import org.jvnet.hk2.annotations.Optional;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This is an abstract class of the Scanner interface for J2EE module.
 * <p>
 * Subclasses register the archives making up the module ({@link #addScanDirectory(File)},
 * {@link #addScanJar(File)}); the classes to process are then looked up in the Jandex
 * indexes of those archives, see {@link JandexIndexer}.
 *
 * @author Shing Wai Chan
 */
public abstract class ModuleScanner<T> extends JavaEEScanner implements Scanner<T> {

    protected File archiveFile = null;
    protected ClassLoader classLoader = null;
    protected final Set<URI> scannedURI = new LinkedHashSet<>();
    protected final Set<String> entries = new HashSet<>();
    private Collection<Index> indexes;

    @Inject
    DefaultAnnotationScanner defaultScanner;

    @Inject
    @Optional
    JandexIndexer jandexIndexer;

    @Inject
    @Optional
    ArchiveFactory archiveFactory;

    public static final Logger deplLogger = DOLUtils.deplLogger;

    @LogMessageInfo(message = "Adding {0} since {1} is annotated with {2}.", level="INFO")
    private static final String ANNOTATION_ADDED = "AS-DEPLOYMENT-00006";

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

    public void process(ReadableArchive archiveFile, T bundleDesc, ClassLoader classLoader) throws IOException {
        File file = new File(archiveFile.getURI());
        process(file, bundleDesc, classLoader);
        completeProcess(bundleDesc, archiveFile);
        calculateResults(bundleDesc);
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

    /**
     * Collects the classes of the registered archives that use any of the annotations
     * handled by the deployment layer.
     */
    protected void calculateResults(T bundleDesc) {
        Level logLevel = System.getProperty("glassfish.deployment.dump.scanning") != null ? Level.INFO : Level.FINE;
        boolean isFullAttribute = bundleDesc instanceof BundleDescriptor
                && ((BundleDescriptor) bundleDesc).isFullAttribute();
        Set<String> annotationsToProcess = defaultScanner.getAnnotations(isFullAttribute);
        for (Index index : getIndexes()) {
            for (String annotation : annotationsToProcess) {
                for (AnnotationInstance instance : index.getIndex().getAnnotations(annotation)) {
                    String className = Index.getClassName(instance);
                    if (className != null && entries.add(className) && deplLogger.isLoggable(logLevel)) {
                        deplLogger.log(logLevel, ANNOTATION_ADDED, new Object[]{className, instance.target(), annotation});
                    }
                }
            }
        }
    }

    /**
     * @return the Jandex indexes of the archives registered with {@link #addScanDirectory(File)},
     * {@link #addScanJar(File)} and {@link #addScanURI(URI)}
     */
    protected Collection<Index> getIndexes() {
        if (indexes == null) {
            indexes = new ArrayList<>();
            DeploymentContext deploymentContext = DeploymentUtils.getCurrentDeploymentContext();
            if (jandexIndexer != null && deploymentContext != null) {
                indexes.addAll(jandexIndexer.getIndexesByURI(deploymentContext, scannedURI).values());
            } else if (archiveFactory != null) {
                // no deployment in progress, e.g. in the application client container: index directly
                for (URI uri : scannedURI) {
                    try (ReadableArchive archive = archiveFactory.openArchive(uri)) {
                        indexes.add(new Index(JandexIndexer.indexArchive(archive, entry -> true)));
                    } catch (IOException e) {
                        deplLogger.log(Level.WARNING, JAR_EXCEPTION, new Object[]{e.getMessage(), uri});
                    }
                }
            }
        }
        return indexes;
    }

    /**
     * This add extra className to be scanned.
     * @param className
     */
    protected void addScanClassName(String className) {
        if (className != null && !className.isEmpty()) {
            entries.add(className);
        }
    }

    /**
     * This add all classes in given jarFile to be scanned.
     * @param jarFile
     */
    protected void addScanJar(File jarFile) throws IOException {
        /*
         * An app might refer to a non-existent JAR in its Class-Path.  Java
         * SE accepts that silently, and so will GlassFish.
         */
        if (jarFile.exists()) {
            scannedURI.add(jarFile.toURI());
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
     * @param directory
     */
    protected void addScanDirectory(File directory) throws IOException {
        scannedURI.add(directory.toURI());
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Set<Class> getElements() {
        return getElements(entries);
    }

    @Override
    public Set<Class> getElements(Set<String> classNames) {
        Set<Class> elements = new HashSet<>();
        if (getClassLoader() == null) {
            deplLogger.log(Level.SEVERE, NO_CLASSLOADER);
            return elements;
        }

        for (String className : classNames) {
            if (deplLogger.isLoggable(Level.FINE)) {
                deplLogger.log(Level.FINE, "Getting {0}", className);
            }
            try {
                elements.add(classLoader.loadClass(className));
            } catch (NoClassDefFoundError err) {
                deplLogger.log(Level.WARNING, ANNOTATION_ERROR, err);
            } catch (ClassNotFoundException cnfe) {
                LogRecord lr = new LogRecord(Level.WARNING, CLASSLOADING_ERROR);
                Object args[] = { className, cnfe.getMessage() };
                lr.setParameters(args);
                lr.setThrown(cnfe);
                deplLogger.log(lr);
            }
        }
        return elements;
    }

    /**
     *
     * @param bundleDesc
     * @param moduleArchive
     */
    protected void addLibraryJars(T bundleDesc, ReadableArchive moduleArchive) {
        List<URI> libraryURIs = new ArrayList<>();
        try {
            if (bundleDesc instanceof BundleDescriptor) {
                libraryURIs = DOLUtils.getLibraryJarURIs((BundleDescriptor)bundleDesc, moduleArchive);
            }

            for (URI uri : libraryURIs) {
                File libFile = new File(uri);
                if (libFile.isFile()) {
                    addScanJar(libFile);
                } else if (libFile.isDirectory()) {
                    addScanDirectory(libFile);
                }
            }
        } catch (Exception ex) {
            // we log a warning and proceed for any problems in
            // adding library jars to the scan list
            deplLogger.log(Level.WARNING, LIBRARY_JAR_ERROR, ex.getMessage());
        }
    }
}
