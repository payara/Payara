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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Analyzes dependency declaration and see if there's anything redundantly declared.
 *
 * <p>
 * The common issue we have is that often distribution modules like PE and nucleus
 * end up containing tons of modules, which makes it unclear as to what functionalities
 * are really added.
 * </p>
 *
 * <p>
 * This mojo is intended to be used interactively to solve this problem.
 * The mojo will list up all the dependencies that are declared explicitly in POM,
 * yet it's also available through transitive dependencies.
 * </p>
 *
 * <p>
 * It is then a human's job to decide if the explicit dependency is justified;
 * for exmaple, if your module X depends on another module Y, and both just
 * so happens to rely on the same utility code Z, then you still want to keep
 * that dependency explicit, because Y might stop depending on Z any time.
 * On the other hand, if you depend on JSP, then the dependency to servlet
 * is implified in JSP, so your explicit dependency to servle is probably unnecessary.
 * </p>
 *
 * @goal analyze-dependency
 * @requiresProject
 * @requiresDependencyResolution runtime
 *
 * @author Kohsuke Kawaguchi
 */
public class AnalyzeMojo extends AbstractGlassfishMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        Map<String,Dep> ndd = determineAllNonDirectDependencies();

        boolean foundIssue = false;

        // find duplicate
        for( Dependency d : (List<Dependency>)project.getDependencies() ) {
            Dep dep = ndd.get(toKey(d));
            if(dep!=null) {
                getLog().warn("Dependency "+d+" is redundant. It's included through"+dep.trail);
                foundIssue = true;
            }
        }

        if(!foundIssue)
            getLog().info("No redundant dependency found");
    }

    /**
     * List up all non-direct dependencies
     */
    private Map<String,Dep> determineAllNonDirectDependencies() throws MojoExecutionException {
        Map<String,Dep> m = new HashMap<String, Dep>();

        for( Artifact a : (Set<Artifact>)project.getArtifacts()) {
            try {
                MavenProject p = loadPom(a);
                for( Dependency d : (List<Dependency>)p.getDependencies() ) {
                    if(d.isOptional())
                        // it makes sense for distribution to include a maven module
                        // that's declared as optional in its transitive dependencies 
                        continue;
                    if(d.getScope()!=null && d.getScope().equals("test"))
                        // we can safely ignore test scope dependency from computation
                        continue;
                    m.put(toKey(d),new Dep(d,a));
                }
            } catch (ProjectBuildingException e) {
                throw new MojoExecutionException("Failed to resolve "+a,e);
            }
        }

        return m;
    }

    private String toKey(Dependency d) {
        return d.getGroupId()+':'+d.getArtifactId();
    }

    private static class Dep {
        final List<String> trail;

        public Dep(Dependency d, Artifact a) {
            trail = a.getDependencyTrail();
        }
    }
}
