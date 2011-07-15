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
 * AugmentationTest.java
 *
 * Create on April 19, 2001
 */

package com.sun.jdo.spi.persistence.support.sqlstore.utility;


import java.lang.reflect.*;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintStream;

// note the use of the public vs. the internal interfaces by this class
import com.sun.jdo.api.persistence.support.PersistenceManager;
import com.sun.jdo.spi.persistence.support.sqlstore.PersistenceCapable;
import com.sun.jdo.spi.persistence.support.sqlstore.StateManager;

/**
 * Utility class for testing a class file for correct augmentation.
 */
public class AugmentationTest
{
    static boolean debug = false;
    static final PrintStream out = System.out;

    static final void affirm(boolean cond)
    {
        if (debug && !cond)
            throw new RuntimeException("affirmion failed.");
    }

    static final void affirm(Object obj)
    {
        if (debug && obj == null)
            throw new RuntimeException("affirmion failed: obj = null");
    }

    static String toString(int mods,
                           Class type,
                           String name)
    {
        final StringBuffer s = new StringBuffer();
        s.append(Modifier.toString(mods));
        s.append(" ");
        s.append(type.getName());
        s.append(" ");
        s.append(name);
        return s.toString();
    }

    static String toString(int mods,
                           String name,
                           Class[] params)
    {
        final StringBuffer s = new StringBuffer();
        s.append(Modifier.toString(mods));
        s.append(" ");
        s.append(name);
        s.append("(");
        final int j = params.length - 1;
        for (int i = 0; i <= j; i++) {
            s.append(params[i].getName());
            if (i < j)
                s.append(",");
        }
        s.append(")");
        return s.toString();
    }

    static String toString(int mods,
                           Class result,
                           String name,
                           Class[] params)
    {
        final StringBuffer s = new StringBuffer();
        s.append(Modifier.toString(mods));
        s.append(" ");
        s.append(result.getName());
        s.append(" ");
        s.append(name);
        s.append("(");
        final int j = params.length - 1;
        for (int i = 0; i <= j; i++) {
            s.append(params[i].getName());
            if (i < j)
                s.append(",");
        }
        s.append(")");
        return s.toString();
    }

    public final int AFFIRMATIVE = 1;
    public final int NEGATIVE = 0;
    public final int ERROR = -1;

    boolean verbose;
    boolean requirePC;
    List classes;
    String className;
    Class classClass;

    public AugmentationTest()
    {}

    final void println()
    {
        out.println();
    }

    final void println(String msg)
    {
        out.println(msg);
    }

    final void verbose()
    {
        if (verbose)
            out.println();
    }

    final void verbose(String msg)
    {
        if (verbose)
            out.println(msg);
    }

    public int testLoadingClass()
    {
        verbose();
        verbose("Test loading class: " + className + " ...");

        try {
            classClass = Class.forName(className);
            verbose("+++ loaded class");
            return AFFIRMATIVE;
        } catch (LinkageError err) {
            println("!!! ERROR: linkage error when loading class: "
                    + className);
            println("    error: " + err);
            println("!!! failed loading class");
            return ERROR;
        } catch (ClassNotFoundException ex) {
            println("!!! ERROR: class not found: " + className);
            println("    exception: " + ex);
            println("!!! failed loading class");
            return ERROR;
        }
    }

    int implementsInterface(Class intf)
    {
        final Class[] interfaces = classClass.getInterfaces();
        for (int i = interfaces.length - 1; i >= 0; i--) {
            if (interfaces[i].equals(intf)) {
                verbose("+++ implements interface: " + intf.getName());
                return AFFIRMATIVE;
            }
        }
        verbose("--- not implementing interface: " + intf.getName());
        return NEGATIVE;
    }

    int hasField(int mods,
                 Class type,
                 String name)
    {
        try {
            final Field field = classClass.getField(name);

            if (field.getModifiers() != mods
                || !field.getType().equals(type)) {
                println("!!! ERROR: field declaration: ");
                println("    expected: " + toString(mods, type, name));
                println("    found:    " + field.toString());
                return ERROR;
            }

            verbose("+++ has field: " + field.toString());
            return AFFIRMATIVE;

        } catch (NoSuchFieldException ex) {
            verbose("--- no field: " + toString(mods, type, name));
            return NEGATIVE;
        }
    }

    int hasConstructor(int mods,
                       Class[] params)
    {
        try {
            final Constructor ctor = classClass.getConstructor(params);

            if (ctor.getModifiers() != mods) {
                println("!!! ERROR: constructor declaration: ");
                println("    expected: " + toString(mods, className, params));
                println("    found:    " + ctor.toString());
                return ERROR;
            }

            verbose("+++ has constructor: " + ctor.toString());
            return AFFIRMATIVE;

        } catch (NoSuchMethodException ex) {
            verbose("--- no constructor: "
                    + toString(mods, className, params));
            return NEGATIVE;
        }
    }

    int hasMethod(int mods,
                  Class result,
                  String name,
                  Class[] params)
    {
        try {
            final Method method = classClass.getMethod(name, params);

            if (method.getModifiers() != mods
                || !method.getReturnType().equals(result)) {
                println("!!! ERROR: method declaration: ");
                println("    expected: " + toString(mods, result, name, params));
                println("    found:    " + method.toString());
                return ERROR;
            }

            verbose("+++ has method: " + method.toString());
            return AFFIRMATIVE;

        } catch (NoSuchMethodException ex) {
            verbose("--- no method: "
                    + toString(mods, result, name, params));
            return NEGATIVE;
        }
    }

    public int hasGenericAugmentation()
    {
        affirm(ERROR < NEGATIVE && NEGATIVE < AFFIRMATIVE);

        verbose();
        verbose("Check for \"generic\" augmentation ...");
        affirm(classClass);

        final int nofFeatures = 15;
        final int[] r = new int[nofFeatures];
        {
            int i = 0;
            r[i++] = implementsInterface(PersistenceCapable.class);

            r[i++] = hasField(Modifier.PUBLIC | Modifier.TRANSIENT,
                              StateManager.class,
                              "jdoStateManager");
            r[i++] = hasField(Modifier.PUBLIC | Modifier.TRANSIENT,
                              byte.class,
                              "jdoFlags");

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               StateManager.class,
                               "jdoGetStateManager",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               void.class,
                               "jdoSetStateManager",
                               new Class[]{StateManager.class});

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               byte.class,
                               "jdoGetFlags",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               void.class,
                               "jdoSetFlags",
                               new Class[]{byte.class});

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               PersistenceManager.class,
                               "jdoGetPersistenceManager",
                               new Class[]{});

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               Object.class,
                               "jdoGetObjectId",
                               new Class[]{});

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               boolean.class,
                               "jdoIsDirty",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               boolean.class,
                               "jdoIsTransactional",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               boolean.class,
                               "jdoIsPersistent",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               boolean.class,
                               "jdoIsNew",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               boolean.class,
                               "jdoIsDeleted",
                               new Class[]{});

            r[i++] = hasMethod(Modifier.PUBLIC | Modifier.FINAL,
                               void.class,
                               "jdoMakeDirty",
                               new Class[]{String.class});
            affirm(i == nofFeatures);
        }

        int res = 0;
        for (int i = 0; i < nofFeatures; i ++) {
            final int j = r[i];
            affirm(ERROR <= j && j <= AFFIRMATIVE);

            if (j < res) {
                println("!!! ERROR: inconsistent \"generic\" augmentation of class: "
                      + className);
                return ERROR;
            }

            if (j > NEGATIVE)
                res = j;
        }

        if (res > NEGATIVE) {
            verbose("+++ has \"generic\" augmentation");
            return AFFIRMATIVE;
        }

        verbose("--- no \"generic\" augmentation");
        return NEGATIVE;
    }

    public int hasSpecificAugmentation()
    {
        affirm(ERROR < NEGATIVE && NEGATIVE < AFFIRMATIVE);

        verbose();
        verbose("Check for \"class-specific\" augmentation ...");
        affirm(classClass);

        final int nofFeatures = 5;
        final int[] r = new int[nofFeatures];
        {
            int i = 0;
            r[i++] = hasConstructor(Modifier.PUBLIC,
                                    new Class[]{StateManager.class});
            r[i++] = hasMethod(Modifier.PUBLIC,
                               Object.class,
                               "jdoGetField",
                               new Class[]{int.class});
            r[i++] = hasMethod(Modifier.PUBLIC,
                               void.class,
                               "jdoSetField",
                               new Class[]{int.class, Object.class});

            r[i++] = hasMethod(Modifier.PUBLIC,
                               void.class,
                               "jdoClear",
                               new Class[]{});
            r[i++] = hasMethod(Modifier.PUBLIC,
                               Object.class,
                               "jdoNewInstance",
                               new Class[]{StateManager.class});
            affirm(i == nofFeatures);
        }

        //@olsen: todo
        // + clone()

        int res = 0;
        for (int i = 0; i < nofFeatures; i++) {
            final int j = r[i];
            affirm(ERROR <= j && j <= AFFIRMATIVE);

            if (j < res) {
                println("!!! ERROR: inconsistent \"class-specific\" augmentation of class: "
                      + className);
                return ERROR;
            }

            if (j > NEGATIVE)
                res = j;
        }

        if (res > NEGATIVE) {
            verbose("+++ has \"class-specific\" augmentation");
            return AFFIRMATIVE;
        }

        verbose("--- no \"class-specific\" augmentation");
        return NEGATIVE;
    }

    static private final String[] transientPrefixes
        = {"java.",
           "javax.",
           "com.sun.jdo."};

    public int testPCFeasibility()
    {
        verbose();
        verbose("Test feasibility of class: " + className + " ...");

        int status = AFFIRMATIVE;

        final int mods = classClass.getModifiers();

        if (classClass.isPrimitive()) {
            println("!!! ERROR: specified class is primitive type");
            status = ERROR;
        }

        if (classClass.isArray()) {
            println("!!! ERROR: specified class is array");
            status = ERROR;
        }

        if (classClass.isInterface()) {
            println("!!! ERROR: specified class is interface");
            status = ERROR;
        }

        if (Modifier.isAbstract(mods)) {
            println("!!! ERROR: specified class is abstract");
            status = ERROR;
        }

        if (!Modifier.isPublic(mods)) {
            println("!!! ERROR: specified class is not public");
            status = ERROR;
        }

        //if (classClass.getDeclaringClass() != null
        //    && !isStatic(classClass.getModifiers())) {
        if (classClass.getDeclaringClass() != null) {
            println("!!! ERROR: specified class is inner class");
            status = ERROR;
        }

        if (Throwable.class.isAssignableFrom(classClass)) {
            println("!!! ERROR: specified class extends Throwable");
            status = ERROR;
        }

        // check for transient package prefixes
        // precludes SCO types from lang.*, math.*, util.*, sql.*
        for (int i = 0; i < transientPrefixes.length; i++) {
            final String typePrefix = transientPrefixes[i];
            if (className.startsWith(typePrefix)) {
                println("!!! ERROR: specified class starts with package prefix: "
                      + typePrefix);
                status = ERROR;
            }
        }

        //verbose("get superclass ...");
        final Class superClass = classClass.getSuperclass();
        if (superClass == null) {
            println("!!! ERROR: specified class doesn't have super class");
            status = ERROR;
        } else {
            try {
                //verbose("get superclass' default constructor ...");
                final Class[] params = new Class[]{};
                Constructor sctor = superClass.getConstructor(params);
            } catch (NoSuchMethodException ex) {
                println("!!! ERROR: super class '" + superClass.getName()
                      + "' doesn't provide default constructor");
                status = ERROR;
            }
        }

        verbose(status == AFFIRMATIVE
                ? "+++ is feasible for persistence-capability"
                : "!!! not feasible for persistence-capability");
        return status;
    }

    public int testJdoConstructor()
    {
        verbose();
        verbose("Test JDO constructor ...");
        affirm(classClass);

        try {
            //verbose("get jdo constructor ...");
            final Class[] params = new Class[]{StateManager.class};
            final Constructor ctor = classClass.getConstructor(params);

            //verbose("create new instance by jdo constructor ...");
            final Object[] args = new Object[]{null};
            final Object instance = ctor.newInstance(args);

            //verbose("cast instance to PersistenceCapable ...");
            PersistenceCapable pc = (PersistenceCapable)instance;

            //verbose("check jdoStateManager ...");
            if (pc.jdoGetStateManager() != null) {
                println("!!! ERROR: invokation of JDO constructor:");
                println("    pc.jdoStateManager != null");
                println("!!! failed testing JDO constructor");
                return ERROR;
            }

            //verbose("check jdoFlags ...");
            if (pc.jdoGetFlags() != 1) {
                println("!!! ERROR: invokation of JDO constructor:");
                println("    pc.jdoFlags != 0");
                println("!!! failed testing JDO constructor");
                return ERROR;
            }
        } catch (NoSuchMethodException ex) {
            println("!!! ERROR: no JDO constructor");
            println("!!! failed testing JDO constructor");
            return ERROR;
        } catch (InstantiationException ex) {
            println("!!! ERROR: invokation of JDO constructor:");
            println("    exception: " + ex);
            println("!!! failed testing JDO constructor");
            return ERROR;
        } catch (IllegalAccessException ex) {
            println("!!! ERROR: invokation of JDO constructor:");
            println("    exception: " + ex);
            println("!!! failed testing JDO constructor");
            return ERROR;
        } catch (InvocationTargetException ex) {
            println("!!! ERROR: invokation of JDO constructor:");
            println("    exception: " + ex);
            println("    nested:    " + ex.getTargetException());
            println("!!! failed testing JDO constructor");
            return ERROR;
        }

        verbose("+++ tested JDO constructor");
        return AFFIRMATIVE;
    }

    public int test(String className)
    {
        affirm(className);
        this.className = className;

        verbose();
        verbose("-------------------------------------------------------------------------------");
        verbose();
        verbose("Test class for augmentation: "
                + className + " ...");

        if (testLoadingClass() < AFFIRMATIVE) {
            return ERROR;
        }

        final int r0 = hasGenericAugmentation();
        final int r1 = hasSpecificAugmentation();

        if (r1 < NEGATIVE || r0 < NEGATIVE) {
            return ERROR;
        }
        affirm(r1 >= NEGATIVE && r0 >= NEGATIVE);

        if (r1 == NEGATIVE && r0 == NEGATIVE) {
            if (requirePC) {
                println();
                println("!!! ERROR: class not augmented: " + className);
                return ERROR;
            }

            println();
            println("--- not augmented: " + className);
            return NEGATIVE;
        }

        if (r0 == NEGATIVE) {
            println();
            println("!!! ERROR: class lacking \"generic\" augmentation: "
                    + className);
            return ERROR;
        }

        if (r1 == NEGATIVE) {
            println();
            println("!!! ERROR: class lacking \"class-specific\" augmentation: "
                    + className);
            return ERROR;
        }
        affirm(r1 > NEGATIVE && r0 > NEGATIVE);

        final int r2 = testPCFeasibility();
        if (r2 < AFFIRMATIVE) {
            return ERROR;
        }

        final int r3 = testJdoConstructor();
        if (r3 < AFFIRMATIVE) {
            return ERROR;
        }

        println();
        println("+++ augmented: " + className);
        return AFFIRMATIVE;
    }

    public int test(boolean verbose,
                    boolean requirePC,
                    List classes)
    {
        affirm(classes);
        this.verbose = verbose;
        this.requirePC = requirePC;

        final int all = classes.size();

        println();
        println("AugmentationTest: Testing classes for being enhanced for persistence-capability");

        int failed = 0;
        for (int i = 0; i < all; i++) {
            if (test((String)classes.get(i)) < NEGATIVE) {
                failed++;
            }
        }
        final int passed = all - failed;

        println();
        println("AugmentationTest: Summary:  TESTED: " + all
                + "  PASSED: " + passed
                + "  FAILED: " + failed);
        return failed;
    }


    /**
     * Prints usage message.
     */
    static void usage()
    {
        out.println();
        out.println("Usage: AugmentationTest <options> <classes>...");
        out.println();
        out.println("This class tests if classes have been correctly enhanced");
        out.println("for persistence-capability (\"augmented\").");
        out.println();
        out.println("Options include:");
        out.println("    -h, --help        print usage");
        out.println("    -v, --verbose     enable verbose output");
        out.println("    -pc, --requirePC  require all classes to be augmented");
        out.println();
        out.println("A non-zero value is returned in case of any errors.");
        out.println();
    }

    static public void main(String[] argv)
    {
        // parse args
        boolean verbose = false;
        boolean requirePC = false;
        List classes = new ArrayList();
        for (int i = 0; i < argv.length; i++) {
            String arg = argv[i];
            if (arg.equals("-h") || arg.equals("--help")) {
                usage();
                return;
            }
            if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
                continue;
            }
            if (arg.equals("-pc") || arg.equals("--requirePC")) {
                requirePC = true;
                continue;
            }
            if (arg.equals("--debug")) {
                debug = true;
                continue;
            }
            if (arg.startsWith("-")) {
                out.println();
                out.println("Unrecognized option: " + arg);
                usage();
                return;
            }
            classes.add(arg);
        }

        // check arguments
        if (classes.isEmpty()) {
            out.println();
            out.println("Missing classes argument");
            usage();
            return;
        }

        if (debug) {
            out.println("options:");
            out.println("    verbose = " + verbose);
            out.println("    requirePC = " + requirePC);
            out.print("    classes =");
            for (int i = 0; i < classes.size(); i++)
                out.print(" " + classes.get(i));
            out.println();
        }

        final AugmentationTest test = new AugmentationTest();
        final int r = test.test(verbose, requirePC, classes);
        System.exit(r);
    }
}
