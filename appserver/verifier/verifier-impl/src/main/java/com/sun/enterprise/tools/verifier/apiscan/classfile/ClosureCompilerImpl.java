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

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.File;

/**
 * This is single most important class of the apiscan package. This class is
 * used to compute the complete closure of a set of classes. It uses {@link
 * ClassFile} to find out dependencies of a class. For each references class
 * name, it loads the corresponding ClassFile using the ClassFileLoader passed
 * to it in constructor. Then it recurssively computes the dependencies until it
 * visits either Java primitive classes or the class name matches the exclude
 * list. Example of using this class is given below...
 * <blockquote><pre>
 * String classpath="your own classpath";
 * ClassFileLoader cfl=ClassFileLoaderFactory.newInstance(new
 * Object[]{classpath});
 * ClosureCompilerImpl cc=new ClosureCompilerImpl(cfl);
 * cc.addExcludePattern("java.");//exclude all classes that start with java.
 * Most of the J2SE classes will be excluded thus.
  * cc.addExcludePattern("javax.");//Most of the J2EE classes can be excluded
 * like this.
 * cc.addExcludePackage("org.omg.CORBA");//Exclude classes whose package name is
 * org.omg.CORBA
 * cc.addExcludeClass("mypackage.Foo");//Exclude class whose name is
 * myPackage.Foo
 * boolean successful=cc.buildClosure("a.b.MyEjb");
 * successful=cc.buildClosure("a.b.YourEjb") && successful; //note the order of
 * &&.
 * Collection closure=cc.getClosure();//now closure contains the union of
 * closure for the two classes.
 * Map failed=cc.getFailed();//now failure contains the union of failures for
 * the two classes.
 * cc.reset();//clear the results collected so far so that we can start afresh.
 * //Now you can again start computing closure of another set of classes.
 * //The excluded stuff are still valid.
 * </pre></blockquote>
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class ClosureCompilerImpl implements ClosureCompiler {

   /*
    * BRIDGE design pattern.
    * This is an abstraction. It delegates to an implementation.
    * It is bound to an implementation at runtime. See the constructor.
    */

    /**
     * an implementation to whom this abstraction delegates.
     * refer to bridge design pattern.
     */
    private ClosureCompilerImplBase imp;
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    private static Logger logger = Logger.getLogger("apiscan.classfile", resourceBundleName); // NOI18N
    private static final String myClassName = "ClosureCompilerImpl"; // NOI18N

    /**
     * @param loader the ClassFileLoader that is used to load the referenced
     *               classes.
     */
    public ClosureCompilerImpl(ClassFileLoader loader) {
        /*
         * See how it binds to a runtime implementation
         * TODO: Ideally we should introduce an AbstractFactory for
         * both ClassFileLoader & ClassClosureCompiler product types.
         */
        if(loader instanceof ASMClassFileLoader){
            imp = new ASMClosureCompilerImpl(loader);
        } else if(loader instanceof BCELClassFileLoader ||
                loader instanceof BCELClassFileLoader1) {
            imp = new BCELClosureCompilerImpl(loader);
        } else {
            throw new RuntimeException("Unknown loader type [" + loader + "]");
        }
    }

    /**
     * I don't expect this constructor to be used. Only defined for
     * testing purpose.
     * @param imp the implementation in the bridge design pattern.
     */
    public ClosureCompilerImpl(ClosureCompilerImplBase imp) {
        this.imp = imp;
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
        imp.addExcludedClass(className);
    }

    //takes in external format, i.e. java.util
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
        imp.addExcludedPackage(pkgName);
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
        imp.addExcludedPattern(pattern);
    }

    //See corresponding method of ClosureCompiler for javadocs
    public boolean buildClosure(String className) {
        logger.entering(myClassName, "buildClosure", className); // NOI18N
        return imp.buildClosure(className);
    }

    /**
     * @param jar whose classes it will try to build closure of. This is a
     *            convenience method which iterates over all the entries in a
     *            jar file and computes their closure.
     */
    public boolean buildClosure(java.util.jar.JarFile jar) throws IOException {
        return imp.buildClosure(jar);
    }

    //See corresponding method of ClosureCompiler for javadocs
    public Collection getClosure() {
        return imp.getClosure();
    }

    //See corresponding method of ClosureCompiler for javadocs
    public Map getFailed() {
        return imp.getFailed();
    }

    /**
     * Reset the closure for next closure computation.
     * Clear the internal cache. It includes the result it has collected since
     * last reset(). But it does not clear the excludedd list. If you want to
     * reset the excluded list, create a new ClosureCompiler.
     */
    public void reset() {
        imp.reset();
    }

    public Collection<String> getNativeMethods() {
        return imp.getNativeMethods();
    }

    public String toString() {
        return imp.toString();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println(
                    "Usage : java " + com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl.class.getName() + // NOI18N
                    " <classpath> <external class name(s)>"); // NOI18N
            System.out.println("Example: to find the closure of " + // NOI18N
                    "mypkg.MySessionBean which is packaged in myejb.jar run\n" + // NOI18N
                    " java " + com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl.class.getName() + // NOI18N
                    " path_to_j2ee.jar"+File.pathSeparator+"path_to_myejb.jar"+ // NOI18N
                    " mypkg.MySessionBean"); // NOI18N
            System.exit(1);
        }

        String cp=args[0];
        System.out.println("Using classpath " + cp); // NOI18N
        ClassFileLoader cfl = ClassFileLoaderFactory.newInstance(
                new Object[]{cp});
        ClosureCompilerImpl closure = new ClosureCompilerImpl(cfl);
        closure.addExcludedPattern("java."); // NOI18N
        for (int i = 1; i < args.length; i++) {
            String clsName = args[i];
            System.out.println("Building closure for " + clsName); // NOI18N
            closure.reset();
            closure.buildClosure(clsName);
            System.out.println("The closure is [" + closure+"\n]"); // NOI18N
        }
    }

}
