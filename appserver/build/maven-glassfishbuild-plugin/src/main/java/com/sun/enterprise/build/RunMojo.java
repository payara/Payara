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

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.impl.HK2Factory;
import com.sun.enterprise.module.common_impl.AbstractFactory;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.module.bootstrap.Main;
import com.sun.enterprise.module.maven.MavenProjectRepository;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Expand;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes Glassfish by the current module (and all the other modules needed
 * for a particular distribution of GF.)
 *
 * @goal run
 * @phase compile
 * @requiresProject
 * @requiresDependencyResolution runtime
 * @aggregator
 *
 * @author Kohsuke Kawaguchi
 */
public class RunMojo extends DistributionAssemblyMojo {
    /**
     * Distribution of Glassfish to be used as a basis.
     * Either this or &lt;distributions> is required.
     *
     * @parameter
     */
    protected ArtifactInfo distribution;

    /**
     * Same as &lt;distribution> but allows you to specify multiple values.
     *
     * @parameter
     */
    protected ArtifactInfo[] distributions;

    /**
     * @component
     */
    protected ArtifactResolver artifactResolver;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * @component
     */
    protected MavenProjectBuilder projectBuilder;

    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @parameter expression="${localRepository}"
     * @required
     */
    protected ArtifactRepository localRepository;

    /**
     * The root directory of the launched module system.
     *
     * If unspecified, the base installation image from {@link #distribution}
     * will be used.
     *
     * @parameter expression="${glassfish.home}"
     */
    private File rootDir;

    /**
     * Command-line options to be passed to {@link StartupContext}.
     *
     * @parameter
     */
    private String[] args = new String[0];

    /**
     * @parameter expression="${session}"
     */
    private MavenSession session;

    public void execute() throws MojoExecutionException, MojoFailureException {
        configLogger();
        
        List<Artifact> dist = new ArrayList<Artifact>();
        try {
            if(distribution!=null)
                dist.add(resolve(distribution));
            if(distributions!=null)
                for (ArtifactInfo a : distributions)
                    dist.add(resolve(a));
            if(dist.isEmpty())
                throw new MojoExecutionException("Either <distribution> or <distributions> is required");
        } catch (ArtifactResolutionException e1) {
            throw new MojoExecutionException("Error attempting to download the distribution POM", e1);
        } catch (ArtifactNotFoundException e11) {
            throw new MojoExecutionException("Distribution POM not found", e11);
        }

        List<MavenProject> distPoms = new ArrayList<MavenProject>();

        // normally we decide the module list by looking at <distribution> and <distributions>,
        // but if the user is developing a single hk2-jar, repeating the same info in <distributions>
        // would violate the DRY principle. So treat that case as a special and allow the module
        // to be in the mix.
        //
        // TODO: we could also conceivably include child <module>s.
        if(project.getPackaging().equals("hk2-jar"))
            distPoms.add(project);

        try {
            for (Artifact a : dist) {
                MavenProject distPom = projectBuilder.buildFromRepository(a, project.getRemoteArtifactRepositories(), localRepository);
                distPom.setFile(a.getFile()); // maven doesn't seem to set this. shouldn't it?
                distPoms.add(distPom);

                // resolve transitive dependencies
                try {
                    if (distPom.getDependencyArtifacts() == null )
                        distPom.setDependencyArtifacts( distPom.createArtifacts( artifactFactory, null, null ) );

                    ArtifactResolutionResult result = artifactResolver.resolveTransitively(
                        distPom.getDependencyArtifacts(),
                        a, localRepository,
                        project.getRemoteArtifactRepositories(),
                        artifactMetadataSource, new ScopeArtifactFilter("runtime") );
                    distPom.setArtifacts( result.getArtifacts() );

                    // download any missing artifacts
                    for (Object dependency : distPom.getArtifacts())
                        artifactResolver.resolve( (Artifact)dependency, project.getRemoteArtifactRepositories(), localRepository );
                } catch (ArtifactResolutionException e) {
                    throw new MojoExecutionException("Failed to resolve dependencies of distribution POM",e);
                } catch (ArtifactNotFoundException e) {
                    throw new MojoExecutionException("Failed to resolve dependencies of distribution POM",e);
                } catch (InvalidDependencyVersionException e) {
                    throw new MojoExecutionException("Failed to resolve dependencies of distribution POM",e);
                }

            }
        } catch (ProjectBuildingException e12) {
            throw new MojoExecutionException("Unable to parse distribution POM", e12);
        }

        // do we have rootDir ?
        if(rootDir==null) {
            // where's the base installation image?
            Artifact baseImage = findBaseImage(distPoms);
            rootDir = new File(new File(session.getExecutionRootDirectory()),"target/glassfish3/glassfish");
            if(!rootDir.exists()) {
                getLog().info(
                    String.format("Extracting %1s to %2s as the installation base image",
                        baseImage.getFile(), rootDir));
                rootDir.mkdirs();

                try {
                    Expand exp = new Expand();
                    exp.setProject(new Project());
                    exp.setSrc(baseImage.getFile());
                    exp.setDest(rootDir.getParentFile());
                    exp.execute();
                } catch (BuildException e) {
                    throw new MojoExecutionException("Failed to extract "+baseImage.getFile());
                }
            } else {
                getLog().info("Using existing glassfish installation image at "+rootDir);
            }
        }

        assert rootDir!=null;

        try {
            Properties p = com.sun.enterprise.module.bootstrap.ArgumentManager.argsToMap(args);
            p.setProperty("com.sun.aas.installRoot", rootDir.getAbsolutePath());
            StartupContext context = new StartupContext(p);

            HK2Factory.initialize();
            ModulesRegistry mr = createModuleRegistry(distPoms);
            mr.setParentClassLoader(this.getClass().getClassLoader());
            Collection<Module> modules = mr.getModules("org.glassfish.core:glassfish");
            if (modules.size() == 1) {
                Module mainModule = modules.iterator().next();
                try {
                    Class mainClass = mainModule.getClassLoader().loadClass("com.sun.enterprise.glassfish.bootstrap.ASMainHK2");
                    Object instance = mainClass.newInstance();
                    Main mainInstance = Main.class.cast(instance);
                    mainInstance.launch(mr, context);
                } catch (Exception e) {
                    throw new MojoExecutionException("Exception while loading or running glassfish main class", e);
                }
            } else {
                throw new MojoExecutionException("Cannot find glassfish main module org.glassfish.core:glassfish");
            }

            // TODO: what's the orderly shutdown sequence of Glassfish?
            // block forever for now.
            Object x = new Object();
            synchronized(x) {
                x.wait();
            }

        } catch (IOException e) {
            throw new MojoExecutionException("Failed to boot up the module system",e);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Failed to boot up the module system",e);
        }
    }

    private Artifact resolve(ArtifactInfo distribution) throws ArtifactResolutionException, ArtifactNotFoundException {
        Artifact dist = distribution.toArtifact(artifactFactory);
        artifactResolver.resolve(dist,
                    project.getRemoteArtifactRepositories(), localRepository);
        return dist;
    }

    /**
     * Finds the base installation image artifact from the distribution POM,
     * or throw an exception if fails.
     */
    private Artifact findBaseImage(List<MavenProject> distPoms) throws MojoExecutionException {
        for (MavenProject dist : distPoms) {
            for (Object o : dist.getArtifacts()) {
                Artifact a = (Artifact) o;
                String type = a.getType();
                if(type!=null && type.equals("zip"))
                    return a;
            }
        }

        throw new MojoExecutionException("No base image found");
    }

    /**
     * Creates a fully configured module registry.
     */
    protected ModulesRegistry createModuleRegistry(List<MavenProject> groups) throws IOException {
        ModulesRegistry r = AbstractFactory.getInstance().createModulesRegistry();
        r.setParentClassLoader(this.getClass().getClassLoader());
        for (MavenProject group : groups) {
            // one repository for loading all the modules from distribution
            MavenProjectRepository lib = new MavenProjectRepository(group,artifactResolver,localRepository,artifactFactory);
            r.addRepository(lib);
            lib.initialize();
        }

        return r;
    }

    /**
     * Added logging configuration.
     */
    private void configLogger() {
        Properties props = System.getProperties();
        for (Entry<Object,Object> e : props.entrySet()) {
            String key = e.getKey().toString();

            if(key.startsWith("logging.")) {
                Level value = Level.parse(e.getValue().toString());

                key = key.substring(8);
                Logger logger = Logger.getLogger(key);
                logger.setLevel(value);

                // the default root ConsoleHandler only logs messages above INFO,
                // so if we want to log more detailed levels, we need to install
                // a separate handler.
                if(value.intValue() < Level.INFO.intValue()) {
                    ConsoleHandler h = new ConsoleHandler();
                    h.setLevel(value);
                    logger.addHandler(h);
                }
            }
        }
    }
}
