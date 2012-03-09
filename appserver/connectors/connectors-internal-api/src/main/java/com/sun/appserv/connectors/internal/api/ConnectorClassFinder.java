/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2010,2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.connectors.internal.api;

import com.sun.enterprise.loader.ASURLClassLoader;
import org.glassfish.internal.api.DelegatingClassLoader;

/**
 * connector-class-finder to provide a class from its .rar
 *
 * @author Jagadish Ramu
 */
public class ConnectorClassFinder extends ASURLClassLoader implements DelegatingClassLoader.ClassFinder {

        private final DelegatingClassLoader.ClassFinder librariesClassFinder;
        private volatile String raName;

    public ConnectorClassFinder(ClassLoader parent, String raName,
                                              DelegatingClassLoader.ClassFinder finder){
            super(parent);
            this.raName = raName;
            
            // There should be better approach to skip libraries Classloader when none specified.
            // casting to DelegatingClassLoader is not a clean approach
            DelegatingClassLoader.ClassFinder libcf = null;
            if(finder!= null && (finder instanceof DelegatingClassLoader)){
                if(((DelegatingClassLoader)finder).getDelegates().size() > 0){
                    libcf = finder;
                }
            }
            this.librariesClassFinder = libcf;
        }

    public Class<?> findClass(String name) throws ClassNotFoundException {
            Class c = null;

            if(librariesClassFinder != null){
                try{
                    c = librariesClassFinder.findClass(name);
                }catch(ClassNotFoundException cnfe){
                    //ignore
                }
                if(c != null){
                    return c;
                }
            }
            return super.findClass(name);
        }

        public Class<?> findExistingClass(String name) {
            if(librariesClassFinder != null){
                Class claz = librariesClassFinder.findExistingClass(name);
                if(claz != null){
                    return claz;
                }
            }
            return super.findLoadedClass(name);
        }

        public String getResourceAdapterName(){
            return raName;
        }

        public void setResourceAdapterName(String raName){
            this.raName = raName;
        }
    }
