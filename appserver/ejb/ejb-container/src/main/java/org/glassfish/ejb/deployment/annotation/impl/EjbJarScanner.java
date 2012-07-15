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

package org.glassfish.ejb.deployment.annotation.impl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import com.sun.enterprise.deployment.EjbBundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.annotation.impl.ModuleScanner;
import org.glassfish.apf.impl.AnnotationUtils;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Implementation of the Scanner interface for Ejb jar.
 *
 * @author Shing Wai Chan
 */
@Service(name="ejb")
@PerLookup
public class EjbJarScanner extends ModuleScanner<EjbBundleDescriptor> {

    @Override
    public void process(File af, EjbBundleDescriptor desc, ClassLoader cl)
            throws IOException {
        this.archiveFile = af;
        this.classLoader = cl;

        if (AnnotationUtils.getLogger().isLoggable(Level.FINE)) {
            AnnotationUtils.getLogger().fine("archiveFile is " + archiveFile);
            AnnotationUtils.getLogger().fine("classLoader is " + classLoader);
        }

        if (!archiveFile.isDirectory()) return ; // in app client jar

        addScanDirectories();
        addClassesFromDescriptor(desc);
    }

    protected void addScanDirectories() throws IOException {
        addScanDirectory(archiveFile);
    }

    protected void addClassesFromDescriptor(EjbBundleDescriptor desc) {
        // always add session beans, message driven beans,
        // interceptor classes that are defined in ejb-jar.xml
        // regardless of they have annotation or not
        for (EjbDescriptor ejbDesc : desc.getEjbs()) {
            if (ejbDesc instanceof EjbSessionDescriptor || 
                ejbDesc instanceof EjbMessageBeanDescriptor) {
                addScanClassName(ejbDesc.getEjbClassName());
            }
        }

        for (EjbInterceptor ei : desc.getInterceptors()) {
            addScanClassName(ei.getInterceptorClassName());
        }
    }
}
