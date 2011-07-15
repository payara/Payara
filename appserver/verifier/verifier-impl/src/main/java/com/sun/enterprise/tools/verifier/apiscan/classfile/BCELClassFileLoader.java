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

/*
 * BCELClassFileLoader.java
 *
 * Created on September 7, 2004, 5:41 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * *This is a factory for {@link BCELClassFile}. This is not a public class, as
 * I expect users to use {@link ClassFileLoaderFactory} interface. This class
 * internally uses the the standard Java ClassLoader to load the resource and
 * construct BCELClassFile object out of it.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class BCELClassFileLoader implements ClassFileLoader {

    private ClassLoader cl;
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    private static Logger logger = Logger.getLogger("apiscan.classfile", resourceBundleName); // NOI18N
    private final static String myClassName = "BCELClassFileLoader"; // NOI18N

    /**
     * Creates a new instance of BCELClassFileLoader.
     *
     * @param cp that will be used to create a new java.net.URLClassLoader. In
     *           subsequent load operations, this classloader will be used.
     */
    public BCELClassFileLoader(String cp) {
        ArrayList<URL> urls = new ArrayList<URL>();
        for (StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
             st.hasMoreTokens();) {
            String entry = st.nextToken();
            try {
                // toURI().toURL() takes care of all the escape characters
                // in the absolutePath. The toURI() method encodes all escape
                // characters. Later URLClassLoader decodes and regenerates 
                // correct escape characters.
                urls.add(new File(entry).toURI().toURL());
            } catch (MalformedURLException e) {
                logger.logp(Level.WARNING, myClassName, "init<>", getClass().getName() + ".exception1", new Object[]{entry});
                logger.log(Level.WARNING, "", e);
            }
        }
        //We do not want system class loader or even extension class loadera s our parent.
        //We want only boot class loader as our parent. Boot class loader is represented as null.
        cl = new URLClassLoader((URL[]) urls.toArray(new URL[0]), null);
    }

    /**
     * Creates a new instance of BCELClassFileLoader.
     *
     * @param cl is the classloader that will be used in subsequent load
     *           operations.
     */
    public BCELClassFileLoader(ClassLoader cl) {
        this.cl = cl;
    }

    //See corresponding method of ClassFileLoader
    public ClassFile load(String externalClassName) throws IOException {
        logger.entering("BCELClassFileLoader", "load", externalClassName); // NOI18N
        //URLClassLoader library expects me to pass in internal form.
        String internalClassName = externalClassName.replace('.', '/');
        URL resource = cl.getResource(internalClassName + ".class");
        //URLClassLoader returns null if resource is not found.
        if(resource == null)
            throw new IOException("Not able to load " + internalClassName + ".class");
        URLConnection urlcon = resource.openConnection();
        urlcon.setUseCaches(false); // bug 6328564
        InputStream is = urlcon.getInputStream();
        try {
            ClassFile cf = new BCELClassFile(is, internalClassName + ".class"); // NOI18N
            matchClassSignature(cf, externalClassName);
            return cf;
        } finally {
            is.close();
        }
    }

    //This method is neede to be protected against users who are passing us
    //internal class names instead of external class names or
    //when the file actually represents some other class, but it isnot 
    //available in in proper package hierarchy.
    private void matchClassSignature(ClassFile cf, String externalClassName)
            throws IOException {
        String nameOfLoadedClass = cf.getName();
        if (!nameOfLoadedClass.equals(externalClassName)) {
            throw new IOException(externalClassName + ".class represents " +
                    cf.getName() +
                    ". Perhaps your package name is incorrect or you passed the " +
                    "name using internal form instead of using external form.");
        }
    }
}
