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

package org.glassfish.appclient.client.jws.boot;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

/**
 * Class Path manager for Java Web Start-aware ACC running under Java runtime 1.6.
 *
 * @author tjquinn
 */
public class ClassPathManager16 extends ClassPathManager {
    
    /** Class object for the JNLPClassLoader class */
    private Class jnlpClassLoaderClass;

    /** Method object for the getJarFile method on JNLPClassLoader - only in 1.6 and later */
    private Method getJarFileMethod;

    /**
     *Returns a new instance of the class path manager for use under Java 1.6
     *@param loader the Java Web Start-provided class loader
     */
    protected ClassPathManager16(ClassLoader loader, boolean keepJWSClassLoader) {
        super(loader, keepJWSClassLoader);
        try {
            prepareIntrospectionInfo();
        } catch (Throwable thr) {
            throw new RuntimeException(thr);
        }
    }

    /**
     *Prepares the reflection-related private variables for later use in 
     *locating classes in JARs.
     *@throws ClassNotFoundException if the JNLPClassLoader class cannot be found
     *@throws NoSuchMethodException if the getJarFile method cannot be found
     */
    private void prepareIntrospectionInfo() throws ClassNotFoundException, NoSuchMethodException {
        jnlpClassLoaderClass = getJNLPClassLoader().loadClass("com.sun.jnlp.JNLPClassLoader");
        getJarFileMethod = jnlpClassLoaderClass.getDeclaredMethod("getJarFile", URL.class);
        getJarFileMethod.setAccessible(true);
    }

    public ClassLoader getParentClassLoader() {
        return (keepJWSClassLoader() ? getJnlpClassLoader() : getJNLPClassLoader().getParent());
    }

    public File findContainingJar(URL resourceURL) throws IllegalArgumentException, URISyntaxException, MalformedURLException, IllegalAccessException, InvocationTargetException {
        File result = null;
        if (resourceURL != null) {
            /*
             *The URL will be similar to http://host:port/...path-in-server-namespace!resource-spec
             *Extract the part preceding the ! and ask the Java Web Start loader to
             *find the locally-cached JAR file corresponding to that part of the URL.
             */
            URI resourceURI = resourceURL.toURI();
            String ssp = resourceURI.getSchemeSpecificPart();
            String jarOnlySSP = ssp.substring(0, ssp.indexOf('!'));

            URL jarOnlyURL = new URL(jarOnlySSP).toURI().toURL();

            /*
             *Use introspection to invoke the method.  This avoids complications
             *in building the app server under Java 1.5 in which the JNLPClassLoader
             *does not provide the getJarFile method.
             */
            JarFile jarFile = (JarFile) getJarFileMethod.invoke(getJNLPClassLoader(), jarOnlyURL);
            if (jarFile == null) {
                throw new IllegalArgumentException(resourceURL.toExternalForm());
            }
            result = new File(jarFile.getName());
        }
        return result;
    }
}
