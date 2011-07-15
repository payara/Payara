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
 * ClassFileLoaderFactory.java
 *
 * Created on August 24, 2004, 5:56 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A factory for ClassFileLoader so that we can control the creation of
 * ClassFileLoaders. More over, this factory can be dynamically configured by
 * setting the Java class name of the actual ClassFileLoader type in the system
 * property apiscan.ClassFileLoader. See newInstance() method.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ClassFileLoaderFactory {
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    private static Logger logger = Logger.getLogger("apiscan.classfile", resourceBundleName); // NOI18N
    private final static String myClassName = "ClassFileLoaderFactory"; // NOI18N
    /**
     * a factory method to create ClassFileLoader instance. It decides which
     * kind of loader class to instantioate based on the class name supplied by
     * the system property ClassFileLoader. If there is no such property set, it
     * defaults to {@link BCELClassFileLoader}
     *
     * @param args Search path to be used by the ClassFileLoader. Depending on
     *             the type of the ClassFileLoader requested, the semantics of
     *             this argument varies.
     * @throws RuntimeException If it could not instantiate the loader type
     *                          requested. The actual error is wrapped in this
     *                          exception.
     */
    public static ClassFileLoader newInstance(Object[] args)
            throws RuntimeException {
        logger.entering(myClassName, "newInstance", args); // NOI18N
        String loaderClassName = System.getProperty("apiscan.ClassFileLoader");
        if (loaderClassName == null) {
            loaderClassName =
                    com.sun.enterprise.tools.verifier.apiscan.classfile.BCELClassFileLoader.class.getName();
            logger.logp(Level.FINE, myClassName, "newInstance", // NOI18N
                    "System Property apiscan.ClassFileLoader is null, so defaulting to " + // NOI18N
                    loaderClassName);
        }
        try {
            Class clazz = Class.forName(loaderClassName);
            Object o = null;
            Constructor[] constrs = clazz.getConstructors();
            for (int i = 0; i < constrs.length; ++i) {
                try {
                    o = constrs[i].newInstance(args);
                } catch (IllegalArgumentException e) {
                 //try another constr as this argument did not match the reqd types for this constr.
                }
            }
            if (o == null) throw new Exception(
                    "Could not find a suitable constructor for this argument types.");
            logger.exiting(myClassName, "<init>", o); // NOI18N
            return (ClassFileLoader) o;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoaderFactory.exception1", e);
            throw new RuntimeException("Unable to instantiate a loader.", e);
        }
    }
}
