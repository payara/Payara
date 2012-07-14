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

package org.glassfish.javaee.full.deployment;

import java.util.*;

import com.sun.enterprise.loader.ASURLClassLoader;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.hk2.api.PreDestroy;

/**
 * Simplistic class loader which will delegate to each module class loader in the order
 * they were added to the instance
 *
 * @author Jerome Dochez
 */
public class EarClassLoader extends ASURLClassLoader
{

    private List<ClassLoaderHolder> moduleClassLoaders = new LinkedList<ClassLoaderHolder>();
    boolean isPreDestroyCalled = false;

    public EarClassLoader(ClassLoader classLoader) {
        super(classLoader); 
    }

    public void addModuleClassLoader(String moduleName, ClassLoader cl) {
        moduleClassLoaders.add(new ClassLoaderHolder(moduleName, cl));
    }

    public ClassLoader getModuleClassLoader(String moduleName) {
        for (ClassLoaderHolder clh : moduleClassLoaders) {
            if (moduleName.equals(clh.moduleName)) {
                return clh.loader;
            }
        }
        return null;
    }

    @Override
    public void preDestroy() {
        if (isPreDestroyCalled) {
            return;
        }

        try {
            for (ClassLoaderHolder clh : moduleClassLoaders) {
                // destroy all the module classloaders
                if ( !(clh.loader instanceof EarLibClassLoader) &&  
                     !(clh.loader instanceof EarClassLoader) && 
                     !isRARCL(clh.loader)) {
                    try {
                        PreDestroy.class.cast(clh.loader).preDestroy();
                    } catch (Exception e) {
                        // ignore, the class loader does not need to be 
                        // explicitly stopped.
                    }
                }
            }

            // destroy itself
            super.preDestroy();

            //now destroy embedded Connector CLs
            DelegatingClassLoader dcl = (DelegatingClassLoader)this.getParent();
            for(DelegatingClassLoader.ClassFinder cf : dcl.getDelegates()){
                try {
                    PreDestroy.class.cast(cf).preDestroy();
                } catch (Exception e) {
                    // ignore, the class loader does not need to be 
                    // explicitly stopped.
                }
            }

            // now destroy the EarLibClassLoader
            PreDestroy.class.cast(this.getParent().getParent()).preDestroy();

            moduleClassLoaders = null;
        } catch (Exception e) {
            // ignore, the class loader does not need to be explicitely stopped.
        }

        isPreDestroyCalled = true;
    }

    private boolean isRARCL(ClassLoader loader) {
        DelegatingClassLoader connectorCL = (DelegatingClassLoader) this.getParent();
        if (!(loader instanceof DelegatingClassLoader.ClassFinder)) {
            return false;
        }
        return connectorCL.getDelegates().contains((DelegatingClassLoader.ClassFinder)loader);
    }

    private static class ClassLoaderHolder {
        final ClassLoader loader;
        final String moduleName;

        private ClassLoaderHolder(String moduleName, ClassLoader loader) {
            this.loader = loader;
            this.moduleName = moduleName;
        }
    }

    @Override
    protected String getClassLoaderName() {
        return "EarClassLoader";
    }
}
