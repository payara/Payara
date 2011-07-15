/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.annotation.impl;

import com.sun.enterprise.deployment.annotation.impl.*;
import com.sun.enterprise.deployment.*;
import org.glassfish.apf.impl.AnnotationUtils;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: mk
 * Date: Mar 26, 2008
 * Time: 11:12:42 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
@Scoped(PerLookup.class)
public class EjbInWarScanner
    extends EjbJarScanner {

    /**
     * This scanner will scan the archiveFile for annotation processing.
     * @param archiveFile
     * @param classLoader
     */
    public void process(File archiveFile, EjbBundleDescriptor desc,
        ClassLoader classLoader) throws IOException {
        if (AnnotationUtils.getLogger().isLoggable(Level.FINE)) {
            AnnotationUtils.getLogger().fine("archiveFile is " + archiveFile);
            AnnotationUtils.getLogger().fine("classLoader is " + classLoader);
        }
        this.archiveFile = archiveFile;
        this.classLoader = classLoader;

        if (!archiveFile.isDirectory()) {
            // on client side
            return;
        }

        // add WEB-INF/classes
        File webinf = new File(archiveFile, "WEB-INF");
        File classes = new File(webinf, "classes");
        if (classes.exists()) {
            addScanDirectory(classes);
        }

        // add WEB-INF/lib
        File lib = new File(webinf, "lib");
        if (lib.exists()) {
            File[] jarFiles = lib.listFiles(new FileFilter() {
                 public boolean accept(File pathname) {
                     return (pathname.getAbsolutePath().endsWith(".jar"));
                 }
            });

            if (jarFiles != null && jarFiles.length > 0) {
                for (File jarFile : jarFiles) {
                    // support exploded jar file
                    if (jarFile.isDirectory()) {
                        addScanDirectory(jarFile);
                    } else {
                        addScanJar(jarFile);
                    }
                }
            }
        }


        // always add session beans, message driven beans,
        // interceptor classes that are defined in ejb-jar.xml r
        // regardless of they have annotation or not
        for (Iterator ejbs = desc.getEjbs().iterator(); ejbs.hasNext();) {
            EjbDescriptor ejbDesc = (EjbDescriptor)ejbs.next();
            if (ejbDesc instanceof EjbSessionDescriptor ||
                ejbDesc instanceof EjbMessageBeanDescriptor) {
                addScanClassName(ejbDesc.getEjbClassName());
            }
        }

        for (Iterator interceptors = desc.getInterceptors().iterator();
            interceptors.hasNext();) {
            EjbInterceptor interceptor =
                (EjbInterceptor)interceptors.next();
            addScanClassName(interceptor.getInterceptorClassName());
        }

    }

}
