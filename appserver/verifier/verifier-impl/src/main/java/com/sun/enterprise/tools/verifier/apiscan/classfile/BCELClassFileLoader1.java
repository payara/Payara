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
 * Created on August 17, 2004, 9:28 AM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.sun.org.apache.bcel.internal.util.ClassPath;

/**
 * Yet another factory for {@link BCELClassFile}. This is not a public class, as
 * I expect users to use {@link ClassFileLoaderFactory} interface. It differs
 * from {@link BCELClassFileLoader} in the sense that it loads classfiles using
 * bcel ClassPath class. Known Issues: Currently it ignores any INDEX
 * information if available in a Jar file. This is because BCEL provided class
 * ClassPath does not understand INDEX info. We should add this feature in
 * future.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class BCELClassFileLoader1 implements ClassFileLoader {

    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    private static Logger logger = Logger.getLogger("apiscan.classfile", resourceBundleName); // NOI18N
    private ClassPath cp;

    /**
     * Creates a new instance of BCELClassFileLoader User should use {link
     * ClassFileLoaderFactory} to create new instance of a loader.
     *
     * @param classPath represents the search path that is used by this loader.
     *                  Please note that, it does not read the manifest entries
     *                  for any jar file specified in the classpath, so if the
     *                  jar files have optional package dependency, that must be
     *                  taken care of in the classpath by ther caller.
     */
    public BCELClassFileLoader1(String classPath) {
        logger.entering("BCELClassFileLoader1", "<init>(String)", classPath); // NOI18N
        cp = new ClassPath(classPath);
    }

    //See ClassFileLoader for description of this method.
    public ClassFile load(String externalClassName) throws IOException {
        logger.entering("BCELClassFileLoader1", "load", externalClassName); // NOI18N
        //BCEL library expects me to pass in internal form.
        String internalClassName = externalClassName.replace('.', '/');
        //don't call getInputStream as it first tries to load using the
        // getClass().getClassLoader().getResourceAsStream()
        //return cp.getInputStream(extClassName);
        InputStream is = cp.getClassFile(internalClassName, ".class") // NOI18N
                .getInputStream();
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
            throw new IOException(
                    externalClassName + ".class represents " +
                    cf.getName() +
                    ". Perhaps your package name is incorrect or you passed the" +
                    " name using internal form instead of using external form.");
        }
    }
}
