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
 * EjbClosure.java
 *
 * Created on August 23, 2004, 2:05 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.stdapis;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoader;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClassFileLoaderFactory;
import com.sun.enterprise.tools.verifier.apiscan.classfile.ClosureCompilerImpl;
import com.sun.enterprise.tools.verifier.apiscan.packaging.ClassPathBuilder;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class EjbClosureCompiler extends ClosureCompilerImpl {
    private static Logger logger = Logger.getLogger("apiscan.stdapis"); // NOI18N
    private static final String myClassName = "EjbClosureCompiler"; // NOI18N
    private String specVersion;

    /**
     * Creates a new instance of EjbClosure
     */
    public EjbClosureCompiler(String specVersion, ClassFileLoader cfl) {
        super(cfl);
        logger.entering(myClassName, "init<>", specVersion); // NOI18N
        this.specVersion = specVersion;
        addStandardAPIs();
    }

    //this method adds APIs specific to versions.
    protected void addStandardAPIs() {
        String apiName = "ejb_jar_" + specVersion; // NOI18N
        Collection classes = APIRepository.Instance().getClassesFor(apiName);
        for (Iterator i = classes.iterator(); i.hasNext();) {
            addExcludedClass((String) i.next());
        }
        Collection pkgs = APIRepository.Instance().getPackagesFor(apiName);
        for (Iterator i = pkgs.iterator(); i.hasNext();) {
            addExcludedPackage((String) i.next());
        }
        Collection patterns = APIRepository.Instance().getPatternsFor(apiName);
        for (Iterator i = patterns.iterator(); i.hasNext();) {
            addExcludedPattern((String) i.next());
        }
    }

    public static void main(String[] args) {
        Handler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        Logger.getLogger("apiscan").addHandler(h); // NOI18N
        Logger.getLogger("apiscan").setLevel(Level.ALL); // NOI18N

        int j = 0;
        String pcp = "";
        String specVer = "";
        for (j = 0; j < args.length; ++j) {
            if (args[j].equals("-prefixClassPath")) { // NOI18N
                pcp = args[++j];
                continue;
            } else if (args[j].equals("-specVer")) { // NOI18N
                specVer = args[++j];
                continue;
            }
        }
        if (args.length < 5) {
            System.out.println(
                    "Usage: " + EjbClosureCompiler.class.getName() + // NOI18N
                    " <-prefixClassPath> <prefix classpath> <-specVer> <something like ejb_2.1> <jar file name(s)>"); // NOI18N
            return;
        }

        for (int i = 4; i < args.length; ++i) {
            try {
                JarFile jar = new JarFile(args[i]);
                String cp = pcp + File.pathSeparator +
                        ClassPathBuilder.buildClassPathForJar(jar);
                System.out.println("Using CLASSPATH " + cp); // NOI18N
                ClassFileLoader cfl = ClassFileLoaderFactory.newInstance(
                        new Object[]{cp});
                EjbClosureCompiler ejbClosureCompiler = new EjbClosureCompiler(
                        specVer, cfl);
                boolean result = ejbClosureCompiler.buildClosure(jar);
                jar.close();
                System.out.println("The closure is [\n"); // NOI18N
                System.out.println(ejbClosureCompiler);
                if (result) {
                    System.out.println("Did not find any non-standard APIs "); // NOI18N
                } else {
                    System.out.println("Found non-standard APIs for " + // NOI18N
                            args[i]);
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            System.out.println("\n]"); // NOI18N
        }//args[i]
    }

}
