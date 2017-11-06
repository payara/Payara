/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 // Portions Copyright [2016] [Payara Foundation and/or its affiliates]

package org.glassfish.javaee.full.deployment;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.sun.enterprise.loader.ASURLClassLoader;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Classloader that is responsible to load the ear libraries (lib/*.jar etc)
 *
 */
public class EarLibClassLoader extends ASURLClassLoader
{
    public EarLibClassLoader(URL[] urls, ClassLoader classLoader) {
        super(classLoader); 
        enableCurrentBeforeParent();
        for (URL url : urls) {
            super.addURL(url);
        }
    }

    /**
     * The below loads services from META-INF from the libraries,
     * so we want to take these from the EAR libraries,
     * this does similar to what WebappClassLoader does
     * 
     * @param name
     * @return set of resources URLSs
     * @throws IOException 
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> localResources = super.getResources(name);
        Enumeration<URL> parentResources = getParent().getResources(name);
        
        Enumeration<URL> combinedResources = Iterators.asEnumeration(Iterators.concat(
                Lists.transform(currentBeforeParentEnabled?
                        ImmutableList.of(localResources, parentResources) :
                        ImmutableList.of(parentResources, localResources),
                        new Function<Enumeration<URL>, Iterator<URL>>() {
                    @Override
                    public Iterator<URL> apply(Enumeration<URL> enumeration) {
                        return Iterators.forEnumeration(enumeration);
                    }
                }).iterator()
        ));
        return combinedResources;
    }

    @Override
    protected String getClassLoaderName() {
        return "EarLibClassLoader";
    }
}
