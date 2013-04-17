/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import org.jboss.weld.resources.spi.ResourceLoader;
import org.jboss.weld.resources.spi.ResourceLoadingException;
import org.jvnet.hk2.annotations.Service;

/**
 * This is implementation of ResourceLoader interface. One instance of this class
 * is created for each bean deployment archive. This class ensures that resource
 * is loaded using class loader for that bean deployment archive.
 * 
 * This was needed to fix issue : http://java.net/jira/browse/GLASSFISH-17396
 *
 * @author kshitiz
 */
@Service
public class ResourceLoaderImpl implements ResourceLoader{

    private ClassLoader classLoader;

    public ResourceLoaderImpl(ClassLoader cl) {
        classLoader = cl;
    }

    @Override
    public Class<?> classForName(String name) {
        ClassLoader cl = getClassLoader();
        try {
            if (cl != null) {
                return cl.loadClass(name);
            } else {
                return Class.forName(name);
            }
        } catch (ClassNotFoundException e) {
            throw new ResourceLoadingException("Error loading class " + name, e);
        } catch (NoClassDefFoundError e) {
            throw new ResourceLoadingException("Error loading class " + name, e);
        } catch (TypeNotPresentException e) {
            throw new ResourceLoadingException("Error loading class " + name, e);
        }
    }

    @Override
    public URL getResource(String name) {
        ClassLoader cl = getClassLoader();
        if (cl != null) {
            return cl.getResource(name);
        } else {
            return ResourceLoaderImpl.class.getResource(name);
        }
    }

    @Override
    public Collection<URL> getResources(String name) {
        ClassLoader cl = getClassLoader();
        try {
            if (cl != null) {
                return getCollection(cl.getResources(name));
            } else {
                return getCollection((getClass().getClassLoader().getResources(name)));
            }
        } catch (IOException e) {
            throw new ResourceLoadingException("Error loading resource " + name, e);
        }
    }

    @Override
    public void cleanup() {
    }

    private ClassLoader getClassLoader(){
        if(classLoader != null){
            return classLoader;
        }
        return Thread.currentThread().getContextClassLoader();
    }

    private Collection<URL> getCollection(Enumeration<URL> resources) {
        ArrayList<URL> urls = new ArrayList<URL>();
        while(resources.hasMoreElements()){
            urls.add(resources.nextElement());
        }
        return urls;
    }

}
