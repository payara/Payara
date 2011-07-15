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
 * ClassPathBuilder.java
 *
 * Created on August 13, 2004, 6:52 PM
 */

package com.sun.enterprise.tools.verifier.apiscan.packaging;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a stand alone utility that recurssively computes the optional package
 * dependency of a jar file.  Then it makes a classpath out of the same 
 * information.
 *
 * @author Sanjeeb.Sahoo@Sun.COM 
 */
public class ClassPathBuilder {
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    public static final Logger logger = Logger.getLogger("apiscan.packaging", resourceBundleName); // NOI18N
    private static final String myClassName = "ClassPathBuilder"; // NOI18N
    
    private static final String thisClassName = "com.sun.enterprise.tools.verifier.apiscan.packaging.ClassPathBuilder"; // NOI18N

    public static String buildClassPathForJar(JarFile jar) throws IOException {
        return buildClassPathForJar(new Archive(jar));
    }

    public static String buildClassPathForJar(File file) throws IOException {
        return buildClassPathForJar(new Archive(file));
    }

    public static String buildClassPathForRar(File file) throws IOException {
        return buildClassPathForRar(new Archive(file));
    }

    //It adds the incoming jar file to the classpath.
    public static String buildClassPathForJar(Archive jar) throws IOException {
        logger.entering(myClassName, "buildClassPathForJar", jar); // NOI18N
        StringBuffer classpath = new StringBuffer(
                jar.getPath() + File.pathSeparator);

        Archive[] archives = jar.getBundledArchives();
        classpath.append(convertToClassPath(archives));
        //now resolve installed exts.
        classpath.append(File.pathSeparator).append(
                convertToClassPath(getInstalledArchivesForJar(jar)));
        String result = removeDuplicates(classpath).toString();
        logger.exiting(myClassName, "buildClassPathForJar", result); // NOI18N
        return result;
    }

    //It adds the incoming jar file to the classpath.
    public static String buildClassPathForRar(Archive rar) throws IOException {
        logger.entering(myClassName, "buildClassPathForRar", rar); // NOI18N
        final StringBuffer classpath = new StringBuffer();

        // all the jar's inside the rar should be available in the classpath
        new File(rar.getPath()).listFiles(new FileFilter() {
            public boolean accept(File file) {
                if (file.getName().endsWith(".jar") && file.isFile()) { // NOI18N
                    classpath.append(File.pathSeparator).append(file.getAbsolutePath());
                    return true;
                }
                return false;
            }
        });

        Archive[] archives = rar.getBundledArchives();
        classpath.append(convertToClassPath(archives));
        //now resolve installed exts.
        classpath.append(File.pathSeparator).append(
                convertToClassPath(getInstalledArchivesForJar(rar)));
        String result = removeDuplicates(classpath).toString();
        logger.exiting(myClassName, "buildClassPathForRar", result); // NOI18N
        return result;
    }

    private static Archive[] getInstalledArchivesForJar(Archive jar)
            throws IOException {
        ArrayList<Archive> list = new ArrayList<Archive>();
        ExtensionRef[] extRefs = jar.getExtensionRefs();
        Archive[] bundled = jar.getBundledArchives();
        Archive[] allOptPkgs = Archive.getAllOptPkgsInstalledInJRE();

        //first go over all thebundled ones.
        for (int i = 0; i < extRefs.length; ++i) {
            ExtensionRef ref = extRefs[i];
            logger.logp(Level.FINE, myClassName, "getInstalledArchivesForJar", // NOI18N
                    "Trying to find an optional package for \n" + ref); // NOI18N
            logger.logp(Level.FINE, myClassName, "getInstalledArchivesForJar", // NOI18N
                    "Searching in the bundled optional package list."); // NOI18N
            Archive satisfyingPkg = null;
            for (int j = 0; j < bundled.length; ++j) {
                try {
                    if (ref.isSatisfiedBy(bundled[j])) {
                        satisfyingPkg = bundled[j];
                        logger.logp(Level.INFO, myClassName,
                                "getInstalledArchivesForJar", // NOI18N
                                thisClassName + ".info1", new Object[]{satisfyingPkg.getPath()});
                        break;
                    }
                } catch (IOException e) {
                    logger.logp(Level.WARNING, myClassName,
                            "getInstalledArchivesForJar", // NOI18N
                            thisClassName + ".exception1", new Object[]{bundled[j].getPath()});
                    logger.log(Level.WARNING, "", e);
                }
            }
            logger.logp(Level.FINE, myClassName, "getInstalledArchivesForJar", // NOI18N
                    "Searching in the installed optional package list."); // NOI18N
            //now go over installed refs.
            if (satisfyingPkg == null) {
                for (int j = 0; j < allOptPkgs.length; ++j) {
                    try {
                        if (ref.isSatisfiedBy(allOptPkgs[j])) {
                            satisfyingPkg = allOptPkgs[j];
                            logger.logp(Level.FINE, myClassName,
                                    "buildClassPathForJar", // NOI18N
                                    "Found a matching installed optional package " + // NOI18N
                                    satisfyingPkg.getPath());
                            break;
                        }
                    } catch (IOException e) {
                        logger.logp(Level.WARNING, myClassName,
                                "getInstalledArchivesForJar", // NOI18N
                                thisClassName + ".exception1", new Object[]{allOptPkgs[j].getPath()});
                        logger.log(Level.WARNING, "", e);
                    }
                }//for
            }//if
            if (satisfyingPkg != null) {
                list.add(satisfyingPkg);
                //TODO We are not supporting if the satisfyingPkg depends on some optional package.
            } else {
                logger.logp(Level.WARNING, myClassName, "buildClassPathForEar", thisClassName + ".warning1", new Object[]{ref.toString()});// NOI18N
            }
        }//for each ref
        return (Archive[]) list.toArray(new Archive[0]);
    }

    public static String buildClassPathForWar(JarFile war) throws IOException {
        return buildClassPathForWar(new Archive(war));
    }

    public static String buildClassPathForWar(File file) throws IOException {
        return buildClassPathForWar(new Archive(file));
    }

    public static String buildClassPathForWar(Archive war) throws IOException {
        final StringBuffer cp = new StringBuffer();
        if (new File(war.getPath()).isDirectory()) {
            final String explodedDir = war.getPath();
            //As per section#9.5 of Servlet 2.4 spec, WEB-INF/classes must be
            // ahead of WEB-INF/lib/*.jar in class-path.
            cp.append(
                    explodedDir + File.separator + "WEB-INF" + File.separator +
                    "classes" +
                    File.separator);
            File[] jarFiles = new File(
                    explodedDir + File.separator + "WEB-INF" + File.separator +
                    "lib").listFiles(new FileFilter() {
                        public boolean accept(File file) {
                            if (file.getName().endsWith(".jar") &&
                                    file.isFile()) {
                                cp.append(File.pathSeparator).append(
                                        file.getAbsolutePath());
                                return true;
                            }
                            return false;
                        }
                    });

        } else {
            assert(false);//We must explode the war. TBD. Anyway never reached from verifier.
        }

        Archive[] archives = war.getBundledArchives();
        cp.append(File.pathSeparator).append(convertToClassPath(archives));

        //now resolve installed exts.
        cp.append(File.pathSeparator).append(
                convertToClassPath(getInstalledArchivesForJar(war)));
        String result = removeDuplicates(cp).toString();
        logger.exiting(myClassName, "buildClassPathForWar", result);
        return result;
    }

    public static String buildClassPathForEar(JarFile ear) throws IOException {
        return buildClassPathForEar(new Archive(ear));
    }

    public static String buildClassPathForEar(File fileOrDir)
            throws IOException {
        return buildClassPathForEar(new Archive(fileOrDir));
    }

    public static String buildClassPathForEar(Archive jar) throws IOException {
        logger.entering(myClassName, "buildClassPathForEar", jar);
        StringBuffer classpath = new StringBuffer();
        //See we do not care about bundled opt packages, as for an ear file
        //there should not be any Class-Path entry.
        ExtensionRef[] extRefs = jar.getExtensionRefs();
        Archive[] allOptPkgs = Archive.getAllOptPkgsInstalledInJRE();
        for (int i = 0; i < extRefs.length; ++i) {
            ExtensionRef ref = extRefs[i];
            logger.logp(Level.FINE, myClassName, "buildClassPathForEar",
                    "Finding an installed optional package matching extension ref\n" +
                    ref);
            Archive satisfyingPkg = null;
            for (int j = 0; j < allOptPkgs.length; ++j) {
                if (ref.isSatisfiedBy(allOptPkgs[j])) {
                    satisfyingPkg = allOptPkgs[j];
                    break;
                }
            }
            if (satisfyingPkg != null) {
                logger.logp(Level.FINE, myClassName, "buildClassPathForEar",
                        "Found an installed optional package " +
                        satisfyingPkg.getPath());
                if (classpath.length() != 0)
                    classpath.append(File.pathSeparator);
                classpath.append(satisfyingPkg.getPath());
                try {
                    String depCP = buildClassPathForJar(satisfyingPkg);
                    classpath.append(File.pathSeparator).append(depCP);
                } catch (IOException e) {
                    logger.logp(Level.WARNING, myClassName,
                            "buildClassPathForEar",
                            "Ignoring " + satisfyingPkg.getPath(), e);
                }
            } else {
                logger.logp(Level.WARNING, myClassName, "buildClassPathForEar",
                        "Could not find an installed optional package for \n" +
                        ref);
            }
        }//for each ref
        File[] archives = 
                new File(jar.getPath()).listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith("_rar");
                    }
                });
        for (File archive : archives) {
            String rarCP = buildClassPathForRar(archive);
            classpath.append(File.pathSeparator).append(rarCP);
        }
        String result = removeDuplicates(classpath).toString();
        logger.exiting(myClassName, "buildClassPath", result);
        return result;
    }

    private static StringBuffer convertToClassPath(Archive[] archives) {
        StringBuffer cp = new StringBuffer();
        for (int i = 0; i < archives.length; ++i) {
            if (i != 0) cp.append(File.pathSeparatorChar);
            cp.append(archives[i].getPath());
        }
        return cp;
    }

    private static StringBuffer removeDuplicates(StringBuffer cp) {
        ArrayList<String> tokens = new ArrayList<String>();
        for (StringTokenizer st = new StringTokenizer(cp.toString(),
                File.pathSeparator);
             st.hasMoreTokens();) {
            String next = st.nextToken();
            if (!tokens.contains(next)) tokens.add(next);
        }
        StringBuffer result = new StringBuffer();
        for (int j = 0; j < tokens.size(); ++j) {
            if (j != 0) result.append(File.pathSeparator);
            result.append(tokens.get(j));
        }
        return result;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println(
                    "Usage : java " + ClassPathBuilder.class.getName() +
                    " <path(s) to jar files>");
        }
        Logger logger = Logger.getLogger("apiscan");
        Handler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        logger.addHandler(h);
        logger.setLevel(Level.ALL);

        for (int i = 0; i < args.length; i++) {
            String jarFileName = args[i];
            try {
                System.out.println("Building CLASSPATH for " + jarFileName);
                String classPath;
                if (jarFileName.endsWith(".ear"))
                    classPath =
                            ClassPathBuilder.buildClassPathForEar(
                                    new Archive(new File(jarFileName)));
                else
                    classPath =
                            ClassPathBuilder.buildClassPathForJar(
                                    new Archive(new File(jarFileName)));
                System.out.println(
                        "CLASSPATH for For " + jarFileName + "\n [" + classPath +
                        "]");
            } catch (Exception e) {
                System.out.println(
                        "For " + jarFileName + " got the following exception");
                e.printStackTrace();
            }
        }
    }
}
