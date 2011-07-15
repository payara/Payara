/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.build;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Cvs;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @goal checkout
 * @aggregator
 * @description Updates all projects in a multi project build.
 *              This is useful for users who have adopted the 
 *              flat project structure where the aggregator
 *              project is a sibling of the sub projects
 *              rather than sitting in the parent directory.
 */
public class CheckoutMojo extends AbstractMojo {
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    ///**
    // * Optional path to cvs executable. Useful for overriding
    // * to use the specific one or troubleshooting.
    // *
    // * @parameter expression="${cvsExe}"
    // */
    //private File cvsExe;

    public void execute() throws MojoExecutionException {
        try {
            // infer the repository connection configuration
            String module = FileUtils.readFileToString(new File("./CVS/Repository")).trim();
            String cvsroot = FileUtils.readFileToString(new File("./CVS/Root")).trim();

            getLog().info("CVSROOT="+cvsroot);


            String currentTag = null;

            File tagFile = new File("./CVS/Tag");
            if(tagFile.exists()) {
                currentTag = FileUtils.readFileToString(tagFile).trim();
                if(currentTag.startsWith("T")) // the format is Ttagname
                    currentTag = currentTag.substring(1);
            }
            getLog().info("tag="+currentTag);

            Set<String> modulesChecked = new HashSet<String>();
            for( String modulePath : (List<String>)project.getModules() ) {
                // modulePath is the relative path from bootstrap to each module like "../foobar"
                getLog().debug("module path = " + modulePath);
                
                // need to rework sub-modules sub directories...
                int i = modulePath.indexOf("/");
                if (modulePath.startsWith("..") && i != -1) {
                    i = modulePath.indexOf("/", i + 1);
                    if (i != -1) {
                        modulePath = modulePath.substring(0, i);
                    }
                }
                getLog().debug("o = " + modulePath);
                if (!modulesChecked.add(modulePath)) {
                    getLog().debug(modulePath + " already done");
                    continue;
                }

                Cvs cvs = new Cvs();
                cvs.setTaskName("cvs");
                cvs.setProject(createAntProject());

                File moduleDir = new File(project.getBasedir(), modulePath).getCanonicalFile();

                if (new File(moduleDir,"CVS").exists()) {
                    getLog().info("update  " + moduleDir);
                    cvs.setCommand("update");
                    cvs.setDest(moduleDir);
                } else {
                    getLog().info("checkout " + moduleDir);
                    // setTag method generates "-rTAG" without space in it,
                    // and some users suspect that that's contributing a failure in some environment
                    // (specifically cygwin on Windows)
                    cvs.setCommand(currentTag==null ? "checkout" : "checkout -r "+currentTag);
                    // set the directory to align with the current workspace layout.
                    cvs.setDest(project.getBasedir().getParentFile().getParentFile());
                    cvs.setCvsRoot(cvsroot);
                    cvs.setPackage(canonicalize(module+"/"+ modulePath));

                }
                cvs.execute();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot run checkout command : ", e);
        }
    }

    /**
     * Take string like "foo/bar/../zot" and get rid of ".." (which
     * will make the string "foo/zot".
     */
    private String canonicalize(String s) {
        List<String> r = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(s,"/");
        while(tokens.hasMoreTokens()) {
            String t = tokens.nextToken();
            if(t.equals("."))   continue;   // ignore
            if(t.equals("..")) {
                r.remove(r.size()-1); // bar/.. cancels out
                continue;
            }
            r.add(t);
        }

        StringBuilder buf = new StringBuilder();
        for (String t : r) {
            if(buf.length()>0)  buf.append('/');
            buf.append(t);
        }
        return buf.toString();
    }

    private Project createAntProject() {
        Project project = new Project();

        DefaultLogger antLogger = new DefaultLogger();
        antLogger.setOutputPrintStream( System.out );
        antLogger.setErrorPrintStream( System.err );
        antLogger.setMessageOutputLevel( getLog().isDebugEnabled() ? Project.MSG_DEBUG : Project.MSG_INFO );
        project.addBuildListener(antLogger);
        return project;
    }
}
