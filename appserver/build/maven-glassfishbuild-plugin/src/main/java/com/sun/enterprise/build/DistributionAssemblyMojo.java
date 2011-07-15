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

import com.sun.enterprise.module.ManifestConstants;
import com.sun.enterprise.module.common_impl.Jar;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;

/**
 * Creates a glassfish distribution image.
 *
 * @goal assemble
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 *
 * @author Kohsuke Kawaguchi
 */
public class DistributionAssemblyMojo extends AbstractGlassfishMojo {

    /**
     * The directory where the final image will be created.
     *
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}"
     */
    protected File outputDirectory;

    /**
     * The file name of the created distribution image.
     *
     * @parameter expression="${finalName}" default-value="${project.build.finalName}.zip"
     */
    protected String finalName;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Set artifacts = project.getArtifacts();

        Set<Artifact> images = findArtifactsOfType(artifacts,"zip");
        Artifact baseImage = findBaseImage(images);

        // find all maven modules
        Set<Artifact> modules = findArtifactsOfScope(artifacts, "runtime");

        outputDirectory.mkdirs();

        // create a zip file
        Zip zip = new Zip();
        zip.setProject(new Project());
        File target = new File(outputDirectory, finalName);
        zip.setDestFile(target);

        // add the base image jar as <zipgroupfileset>
        ZipFileSet zfs = new ZipFileSet();
        zfs.setSrc(baseImage.getFile());
        zfs.setDirMode("755");
        zfs.setFileMode("644"); // work around for http://issues.apache.org/bugzilla/show_bug.cgi?id=42122
        zip.addZipfileset(zfs);

        // then put all modules
        for (Artifact a : modules) {
            zfs = new ZipFileSet();
            zfs.setFile(a.getFile());
            zfs.setPrefix("glassfish/modules");
            zip.addZipfileset(zfs);
        }

        getLog().info("Creating the distribution");
        long time = System.currentTimeMillis();
        zip.execute();
        getLog().info("Packaging took "+(System.currentTimeMillis()-time)+"ms");

        project.getArtifact().setFile(target);
        // normally I shouldn't have to do this. Maven is supposed to pick up
        // the glassfish-distribution artifact handler definition from components.xml
        // and use that.
        // but because of what seems like an ordering issue, I can't get this work.
        // ArtifactHandlerManager just don't get glassfish-distribution ArtifactHandler.
        // so to make this work, I'm overwriting artifact handler here as a work around.
        // This may be somewhat unsafe, as other processing could have already
        // happened with the old incorrect artifact handler, but at least this
        // seems to make the deploy/install phase work.
        project.getArtifact().setArtifactHandler(new DistributionArtifactHandler());
    }

    /**
     * Finds the base image ".zip" file from dependency list.
     *
     * <p>
     * The interesting case is let's say where we are building pe, in which
     * case we see both pe-base and nucleus-base (through nucleus.)
     * So we look for one with the shortest dependency path.
     */
    private Artifact findBaseImage(Set<Artifact> images) throws MojoExecutionException {
        if(images.isEmpty())
            throw new MojoExecutionException("No base image zip dependency is given");

        Set<Artifact> shortest = new HashSet<Artifact>();
        int shortestLen = Integer.MAX_VALUE;

        for (Artifact a : images) {
            int l = a.getDependencyTrail().size();
            if(l<shortestLen) {
                shortest.clear();
                shortestLen = l;
            }
            if(l==shortestLen)
                shortest.add(a);
        }

        if(shortest.size()>1)
            throw new MojoExecutionException("More than one base image zip dependency is specified: "+shortest);

        return shortest.iterator().next();
    }
}
