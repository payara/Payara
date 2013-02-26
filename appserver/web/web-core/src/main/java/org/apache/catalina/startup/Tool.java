/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;


import org.apache.catalina.core.StandardServer;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>General purpose wrapper for command line tools that should execute in an
 * environment with the common class loader environment set up by Catalina.
 * This should be executed from a command line script that conforms to
 * the following requirements:</p>
 * <ul>
 * <li>Passes the <code>catalina.home</code> system property configured with
 *     the pathname of the Tomcat installation directory.</li>
 * <li>Sets the system classpath to include <code>bootstrap.jar</code> and
 *     <code>$JAVA_HOME/lib/tools.jar</code>.</li>
 * </ul>
 *
 * <p>The command line to execute the tool looks like:</p>
 * <pre>
 *   java -classpath $CLASSPATH org.apache.catalina.startup.Tool \
 *     ${options} ${classname} ${arguments}
 * </pre>
 *
 * <p>with the following replacement contents:
 * <ul>
 * <li><strong>${options}</strong> - Command line options for this Tool wrapper.
 *     The following options are supported:
 *     <ul>
 *     <li><em>-ant</em> : Set the <code>ant.home</code> system property
 *         to corresponding to the value of <code>catalina.home</code>
 *         (useful when your command line tool runs Ant).</li>
 *     <li><em>-common</em> : Add <code>common/classes</code> and
 *         <code>common/lib</codE) to the class loader repositories.</li>
 *     <li><em>-debug</em> : Enable debugging messages from this wrapper.</li>
 *     <li><em>-server</em> : Add <code>server/classes</code> and
 *         <code>server/lib</code> to the class loader repositories.</li>
 *     <li><em>-shared</em> : Add <code>shared/classes</code> and
 *         <code>shared/lib</code> to the class loader repositories.</li>
 *     </ul>
 * <li><strong>${classname}</strong> - Fully qualified Java class name of the
 *     application's main class.</li>
 * <li><strong>${arguments}</strong> - Command line arguments to be passed to
 *     the application's <code>main()</code> method.</li>
 * </ul>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2006/03/12 01:27:07 $
 */

public final class Tool {

    @LogMessageInfo(
            message = "Must set 'catalina.home' system property",
            level = "SEVERE",
            cause = "Did not set 'catalina.home'",
            action = "Verify that 'catalina.home' was passed"
    )
    public static final String MUST_SET_SYS_PROPERTY = "AS-WEB-CORE-00470";

    @LogMessageInfo(
            message = "Class loader creation threw exception",
            level = "SEVERE",
            cause = "Could not create a new class loader",
            action = "Verify directory paths"
    )
    public static final String CLASS_LOADER_CREATION_EXCEPTION = "AS-WEB-CORE-00471";

    @LogMessageInfo(
            message = "Exception creating instance of {0}",
            level = "SEVERE",
            cause = "Could not load application class",
            action = "Verify the class name"
    )
    public static final String CREATING_INSTANCE_EXCEPTION = "AS-WEB-CORE-00472";

    @LogMessageInfo(
            message = "Exception locating main() method",
            level = "SEVERE",
            cause = "Could not locate the static main() method of the application class",
            action = "Verify the access permission"
    )
    public static final String LOCATING_MAIN_METHOD_EXCEPTION = "AS-WEB-CORE-00473";

    @LogMessageInfo(
            message = "Exception calling main() method",
            level = "SEVERE",
            cause = "Could not invoke main() method",
            action = "Verify the underlying method is inaccessible, and parameter values"
    )
    public static final String CALLING_MAIN_METHOD_EXCEPTION = "AS-WEB-CORE-00474";

    @LogMessageInfo(
            message = "Usage:  java org.apache.catalina.startup.Tool [<options>] <class> [<arguments>]",
            level = "INFO"
    )
    public static final String USAGE_INFO = "AS-WEB-CORE-00475";


    // ------------------------------------------------------- Static Variables

    /**
     * Set <code>ant.home</code> system property?
     */
    private static boolean ant = false;


    /**
     * The pathname of our installation base directory.
     */
    private static String catalinaHome = System.getProperty("catalina.home");


    /**
     * Include common classes in the repositories?
     */
    private static boolean common = false;


    /**
     * Enable debugging detail messages?
     *
    private static boolean debug = false;
    */

    private static final Logger log = StandardServer.log;

    private static final ResourceBundle rb = log.getResourceBundle();

    /**
     * Include server classes in the repositories?
     */
    private static boolean server = false;


    /**
     * Include shared classes in the repositories?
     */
    private static boolean shared = false;


    // ----------------------------------------------------------- Main Program


    /**
     * The main program for the bootstrap.
     *
     * @param args Command line arguments to be processed
     */
    public static void main(String args[]) {

        // Verify that "catalina.home" was passed.
        if (catalinaHome == null) {
            log.log(Level.SEVERE, MUST_SET_SYS_PROPERTY);
            System.exit(1);
        }

        // Process command line options
        int index = 0;
        while (true) {
            if (index == args.length) {
                usage();
                System.exit(1);
            }
            if ("-ant".equals(args[index]))
                ant = true;
            else if ("-common".equals(args[index]))
                common = true;
            //else if ("-debug".equals(args[index]))
            //    debug = true;
            else if ("-server".equals(args[index]))
                server = true;
            else if ("-shared".equals(args[index]))
                shared = true;
            else
                break;
            index++;
        }
        if (index > args.length) {
            usage();
            System.exit(1);
        }

        // Set "ant.home" if requested
        if (ant)
            System.setProperty("ant.home", catalinaHome);

        // Construct the class loader we will be using
        ClassLoader classLoader = null;
        try {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Constructing class loader");
                ClassLoaderFactory.setDebug(1);
            }
            ArrayList<File> packed = new ArrayList<File>();
            ArrayList<File> unpacked = new ArrayList<File>();
            unpacked.add(new File(catalinaHome, "classes"));
            packed.add(new File(catalinaHome, "lib"));
            if (common) {
                unpacked.add(new File(catalinaHome,
                                      "common" + File.separator + "classes"));
                packed.add(new File(catalinaHome,
                                    "common" + File.separator + "lib"));
            }
            if (server) {
                unpacked.add(new File(catalinaHome,
                                      "server" + File.separator + "classes"));
                packed.add(new File(catalinaHome,
                                    "server" + File.separator + "lib"));
            }
            if (shared) {
                unpacked.add(new File(catalinaHome,
                                      "shared" + File.separator + "classes"));
                packed.add(new File(catalinaHome,
                                    "shared" + File.separator + "lib"));
            }
            classLoader =
                ClassLoaderFactory.createClassLoader
                (unpacked.toArray(new File[unpacked.size()]),
                 packed.toArray(new File[packed.size()]),
                 null);
        } catch (Throwable t) {
            log.log(Level.SEVERE, CLASS_LOADER_CREATION_EXCEPTION, t);
            System.exit(1);
        }
        Thread.currentThread().setContextClassLoader(classLoader);

        // Load our application class
        Class<?> clazz = null;
        String className = args[index++];
        try {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Loading application class " + className);
            clazz = classLoader.loadClass(className);
        } catch (Throwable t) {
            String msg = MessageFormat.format(rb.getString(CREATING_INSTANCE_EXCEPTION),
                                              className);
            log.log(Level.SEVERE, msg, t);
            System.exit(1);
        }

        // Locate the static main() method of the application class
        Method method = null;
        String params[] = new String[args.length - index];
        System.arraycopy(args, index, params, 0, params.length);
        try {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Identifying main() method");
            String methodName = "main";
            Class paramTypes[] = new Class[1];
            paramTypes[0] = params.getClass();
            method = clazz.getMethod(methodName, paramTypes);
        } catch (Throwable t) {
            log.log(Level.SEVERE, LOCATING_MAIN_METHOD_EXCEPTION, t);
            System.exit(1);
        }

        // Invoke the main method of the application class
        try {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "Calling main() method");
            Object paramValues[] = new Object[1];
            paramValues[0] = params;
            method.invoke(null, paramValues);
        } catch (Throwable t) {
            log.log(Level.SEVERE, CALLING_MAIN_METHOD_EXCEPTION, t);
            System.exit(1);
        }

    }


    /**
     * Display usage information about this tool.
     */
    private static void usage() {

        if (log.isLoggable(Level.INFO)) {
            log.log(Level.INFO, USAGE_INFO);
        }

    }


}
