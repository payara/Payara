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
// Portions Copyright [2016-2025] [Payara Foundation and/or its affiliates.]

package com.sun.enterprise.deployment.annotation.impl;

import com.sun.enterprise.deployment.annotation.introspection.DefaultAnnotationScanner;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import java.util.zip.ZipException;
import org.glassfish.apf.Scanner;
import org.glassfish.apf.impl.JavaEEScanner;
import org.glassfish.hk2.classmodel.reflect.*;
import jakarta.inject.Inject;
import org.glassfish.api.deployment.archive.ReadableArchive;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This is an abstract class of the Scanner interface for J2EE module.
 *
 * @author Shing Wai Chan
 */
public abstract class ModuleScanner<T> extends JavaEEScanner implements Scanner<T> {

    private static final int DEFAULT_ENTRY_BUFFER_SIZE = 8192;

    protected File archiveFile = null;
    protected ClassLoader classLoader = null;
    protected Parser classParser = null;

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
        throw new UnsupportedOperationException("No longer supported.");
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

    protected void calculateResults(T bundleDesc) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    /**
     * This add extra className to be scanned.
     * @param className
     */
    protected void addScanClassName(String className) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    /**
     * This add all classes in given jarFile to be scanned.
     * @param jarFile
     */
    protected void addScanJar(File jarFile) throws IOException {
        throw new UnsupportedOperationException("No longer supported.");
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
        throw new UnsupportedOperationException("No longer supported.");
    }
    
    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public Set<Class> getElements() {
        throw new UnsupportedOperationException("No longer supported.");
    }

    @Override
    public Set<Class> getElements(Set<String> classNames) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    /**
     * 
     * @param bundleDesc
     * @param moduleArchive 
     */
    protected void addLibraryJars(T bundleDesc, 
        ReadableArchive moduleArchive) {
        throw new UnsupportedOperationException("No longer supported.");
    }

    @Override
    public Types getTypes() {
        throw new UnsupportedOperationException("No longer supported.");
    }

    protected void setParser(Parser parser) {
        throw new UnsupportedOperationException("No longer supported.");
    }
}
