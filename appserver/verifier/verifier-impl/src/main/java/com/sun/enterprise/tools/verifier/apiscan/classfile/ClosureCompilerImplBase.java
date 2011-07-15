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

package com.sun.enterprise.tools.verifier.apiscan.classfile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the base class for classes that actually implement ClosureCompiler
 * interface, i.e. {@link BCELClosureCompilerImpl} and
 * {@link ASMClosureCompilerImpl}.
 * It contains common implementation for above classes.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class ClosureCompilerImplBase implements ClosureCompiler {

    protected ClassFileLoader loader;

    protected HashSet<String> excludedClasses = new HashSet<String>();

    protected HashSet<String> excludedPackages = new HashSet<String>();

    protected HashSet<String> excludedPatterns = new HashSet<String>();

    protected HashSet<String> visitedClasses = new HashSet<String>();

    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    protected static final Logger logger = Logger.getLogger("apiscan.classfile", resourceBundleName); // NOI18N

    // used for logging
    private static final String myClassName = "ClosureCompilerImplBase"; // NOI18N

    /**
     * @param loader the ClassFileLoader that is used to load the referenced
     *               classes.
     */
    protected ClosureCompilerImplBase(ClassFileLoader loader) {
        this.loader = loader;
    }

    /**
     * @param className the class name to be excluded from closure
     *                     computation. It is in the external class name format
     *                     (i.e. java.util.Map$Entry instead of java.util.Map.Entry).
     *                     When the closure compiler sees a class matches this
     *                     name, it does not try to compute its closure any
     *                     more. It merely adds this name to the closure. So the
     *                     final closure will contain this class name, but not
     *                     its dependencies.
     */
    public void addExcludedClass(String className) {
        excludedClasses.add(className);
    }

    /**
     * @param pkgName the package name of classes to be excluded from
     *                     closure computation. It is in the external format
     *                     (i.e. java.lang (See no trailing '.'). When the
     *                     closure compiler sees a class whose package name
     *                     matches this name, it does not try to compute the
     *                     closure of that class any more. It merely adds that
     *                     class name to the closure. So the final closure will
     *                     contain that class name, but not its dependencies.
     */
    public void addExcludedPackage(String pkgName) {
        excludedPackages.add(pkgName);
    }

    /**
     * @param pattern the pattern for the names of classes to be excluded from
     *                closure computation. It is in the external format (i.e.
     *                org.apache.). When the closure compiler sees a class whose
     *                name begins with this pattern, it does not try to compute
     *                the closure of that class any more. It merely adds that
     *                class name to the closure. So the final closure will
     *                contain that class name, but not its dependencies. Among
     *                all the excluded list, it is given the lowest priority in
     *                search order.
     */
    public void addExcludedPattern(String pattern) {
        excludedPatterns.add(pattern);
    }

    /**
     * @param jar whose classes it will try to build closure of. This is a
     *            convenience method which iterates over all the entries in a
     *            jar file and computes their closure.
     */
    public boolean buildClosure(java.util.jar.JarFile jar) throws IOException {
        boolean result = true;
        for (java.util.Enumeration entries = jar.entries();
             entries.hasMoreElements();) {
            String clsName = ((java.util.jar.JarEntry) entries.nextElement()).getName();
            if (clsName.endsWith(".class")) { // NOI18N
                String externalClsName = clsName.substring(0,
                        clsName.lastIndexOf(".class")) // NOI18N
                        .replace('/', '.');
                boolean newresult = this.buildClosure(externalClsName);
                result = newresult && result;
            }
        }//for all jar entries
        return result;
    }

    public Collection<String> getNativeMethods() {
        throw new UnsupportedOperationException();
    }

    /**
     * @param className name of class in external format.
     * @return
     */
    protected boolean needToBuildClosure(String className) {
        boolean result = true;
        if (visitedClasses.contains(className))
            result = false;
        else if (excludedClasses.contains(className)) {
            result = false;
        } else if (excludedPackages.contains(getPackageName(className))) {
            result = false;
        } else {
            for (Iterator i = excludedPatterns.iterator(); i.hasNext();) {
                String pattern = (String) i.next();
                if (className.startsWith(pattern)) {
                    result = false;
                    break;
                }
            }
        }
        logger.logp(Level.FINEST, myClassName, "needToBuildClosure", // NOI18N
                className + " " + result); // NOI18N
        return result;
    }

    /**
     * @param className name of class in external format.
     * @return package name in dotted format, e.g. java.lang for java.lang.void
     */
    protected static String getPackageName(String className) {
        int idx = className.lastIndexOf('.');
        if (idx != -1) {
            return className.substring(0, idx);
        } else
            return "";
    }

}
