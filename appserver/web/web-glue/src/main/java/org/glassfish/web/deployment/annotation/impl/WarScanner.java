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

package org.glassfish.web.deployment.annotation.impl;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.deployment.web.ServletFilter;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.classmodel.reflect.Parser;
import org.glassfish.hk2.classmodel.reflect.ParsingContext;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.web.deployment.descriptor.*;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;


/**
 * Implementation of the Scanner interface for war.
 *
 * @author Shing Wai Chan
 */
@Service(name="war")
@PerLookup
public class WarScanner extends ModuleScanner<WebBundleDescriptor> {
    protected boolean scanOtherLibraries = false;

    @Inject protected ClassLoaderHierarchy clh;

    public void setScanOtherLibraries(boolean scanOtherLibraries) {
        this.scanOtherLibraries = scanOtherLibraries;
    }

    public boolean isScanOtherLibraries() {
        return scanOtherLibraries;
    }

    @Override
    public void process(File archiveFile, WebBundleDescriptor webBundleDesc,
            ClassLoader classLoader) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    /**
     * This scanner will scan the archiveFile for annotation processing.
     * @param readableArchive the archive to process
     * @param webBundleDesc existing bundle descriptor to add to
     * @param classLoader classloader to load archive classes with.
     */
    @Override
    public void process(ReadableArchive readableArchive, WebBundleDescriptor webBundleDesc,
            ClassLoader classLoader, Parser parser) throws IOException {

        this.archiveFile =  new File(readableArchive.getURI()); 
        this.classLoader = classLoader;
        setParser(parser);

        if (AnnotationUtils.getLogger().isLoggable(Level.FINE)) {
            AnnotationUtils.getLogger().fine("archiveFile is " + archiveFile);
            AnnotationUtils.getLogger().fine("webBundle is " + webBundleDesc);
            AnnotationUtils.getLogger().fine("classLoader is " + classLoader);
        }

        if (!archiveFile.isDirectory()) {
            // on client side
            return;
        }

        if (isScanOtherLibraries()) {
            addLibraryJars(webBundleDesc, readableArchive);
            calculateResults();
            return;
        }

        File webinf = new File(archiveFile, "WEB-INF");
        
        if (webBundleDesc instanceof WebFragmentDescriptor) {
            WebFragmentDescriptor webFragmentDesc = (WebFragmentDescriptor)webBundleDesc;
            File lib = new File(webinf, "lib");
            if (lib.exists()) {
                File jarFile = new File(lib, webFragmentDesc.getJarName());
                if (jarFile.exists()) {
                    // support exploded jar file
                    if (jarFile.isDirectory()) {
                        addScanDirectory(jarFile);
                    } else {
                        addScanJar(jarFile);
                    }
                }
            }
        } else {
            File classes = new File(webinf, "classes");
            if (classes.exists()) {
                addScanDirectory(classes);   
            }
            scanXmlDefinedClassesIfNecessary(webBundleDesc);
        }
        calculateResults();
    }

    // This is not mandated by the spec. It is for WSIT.
    // We will also scan any servlets/filters/listeners classes specified
    // in web.xml additionally if those classes are not resided in the wars.
    private void scanXmlDefinedClassesIfNecessary(
            WebBundleDescriptor webBundleDesc) 
            throws IOException {

        ClassLoader commonCL = clh.getCommonClassLoader();

        for (Iterator webComponents =
            webBundleDesc.getWebComponentDescriptors().iterator();
            webComponents.hasNext();) {
            WebComponentDescriptor webCompDesc =
                (WebComponentDescriptor)webComponents.next();
            if (webCompDesc.isServlet()) {
                String servletName = webCompDesc.getWebComponentImplementation();
                if (isScan(servletName, commonCL)) {
                    addScanClassName(servletName);
                }
            }
        }

        Vector servletFilters = webBundleDesc.getServletFilters();
        for (int i = 0; i < servletFilters.size(); i++) {
            ServletFilter filter = (ServletFilter)servletFilters.elementAt(i);
            String filterName = filter.getClassName();
            if (isScan(filterName, commonCL)) {
                addScanClassName(filter.getClassName());
            }
        }

        Vector listeners = webBundleDesc.getAppListenerDescriptors();
        for (int j = 0; j < listeners.size(); j++) {
            AppListenerDescriptor listenerDesc =
                (AppListenerDescriptor) listeners.elementAt(j);
            String listenerName = listenerDesc.getListener();
            if (isScan(listenerName, commonCL)) {
                addScanClassName(listenerDesc.getListener());
            }
        }
    }

    private boolean isScan(String className, ClassLoader commonCL) throws IOException {
        boolean result = false;
        //XXX TBD ignore delegate in sun-web.xml in this moment
        String resourceName = "/" + className.replace(".", "/") + ".class";
        return (commonCL.getResource(resourceName) != null);
    }
}
 
