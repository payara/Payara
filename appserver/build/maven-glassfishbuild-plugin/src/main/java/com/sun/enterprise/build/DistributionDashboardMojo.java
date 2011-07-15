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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create a wiki style document listing interesting information
 *
 * @goal dashboard
 * @phase package
 * @requiresProject
 * @requiresDependencyResolution runtime
 *
 *
 * @author Jerome Dochez
 */
public class DistributionDashboardMojo extends AbstractGlassfishMojo {


    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * @component
     */
    protected ArtifactFactory artifactFactory;

    /**
     * The directory where the final image will be created.
     *
     * @parameter expression="${outputDirectory}" default-value="${project.build.directory}"
     */
    protected File outputDirectory;

    /**
     * The file name of the created distribution image.
     *
     * @parameter expression="${finalName}" default-value="${project.build.finalName}.txt"
     */
    protected String finalName;

    /**
     * The generator
     *
     * @parameter default-value="com.sun.enterprise.build.DashboardWikiGenerator"
     */
    protected String generatorName;

    /**
     * Mojo execution
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        // find all maven modules
        Set<Artifact> ourArtifacts = findArtifactsOfScope(project.getArtifacts(), "compile");

        initXRefs(ourArtifacts);

        outputDirectory.mkdirs();

        // create a zip file
        File target = new File(outputDirectory, finalName);

        getLog().info("Creating the wiki page with " + generatorName);

        // create our generator
        Object o;
        try {
            Class c = Class.forName(generatorName);
            o = c.newInstance();
        } catch(ClassNotFoundException e) {
            getLog().error("Generator " + generatorName + " not found, aborting");
            return;
        } catch(Exception e) {
            getLog().error("Cannot instantiate generator " + generatorName);
            return;
        }
        DistributionVisitor generator = DistributionVisitor.class.cast(o);
        try {

            Map<String, List<Artifact>> modules = new HashMap<String, List<Artifact>>();
            List<Artifact> libraries = new ArrayList<Artifact>();

            for(Artifact a : ourArtifacts) {

                if ("pom".equals(a.getType()) || "xml".equals(a.getType())) {
                    continue;
                }
                if (isModule(a)) {
                    // our group key is xx.yy (like org.glassfish)
                    String key = a.getGroupId().substring(0, a.getGroupId().lastIndexOf('.'));
                    List<Artifact> values = modules.get(key);
                    if (values==null) {
                        values = new ArrayList<Artifact>();
                        modules.put(key, values);
                    }

                    values.add(a);
                } else {
                    libraries.add(a);
                }
            }
            PrintWriter writer = new PrintWriter(new FileWriter(target));
            generator.beginDistribution(writer, project);
            generator.beginCategory("Modules");

            // for modules we create a table per group.
            for (Map.Entry<String, List<Artifact>> item : modules.entrySet()) {
                if (!item.getValue().isEmpty()) {
                    dumpArtifacts(generator, item.getValue(), false);
                }
            }
            generator.endCategory();

            if (!libraries.isEmpty()) {
                generator.beginCategory("Libraries");
                dumpArtifacts(generator, libraries, true);
                generator.endCategory();
            }


            generator.endDistribution();
            writer.close();
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage());
        }



        getLog().info("done...");

        project.getArtifact().setFile(target);
    }

    private void dumpArtifacts(DistributionVisitor generator, List<Artifact> artifacts, boolean showGroup) throws IOException {

        generator.beginGroup(artifacts.get(0).getGroupId(), showGroup);
        
        for (Artifact module : artifacts) {

            generator.beginArtifact(module);

            // size
            generator.addSize(module.getFile().length());

            try {

                MavenProject moduleProject = null;
                try {
                    moduleProject = loadPom(module);
                } catch(Exception e) {
                    getLog().error("Cannot load pom for " + module.getId());
                }
                if (moduleProject != null) {

                    // get transitive list of repos, project first.
                    Map<String, ArtifactRepository> repos = new HashMap<String, ArtifactRepository>();
                    for (ArtifactRepository repo : (List<ArtifactRepository>) project.getRemoteArtifactRepositories()) {
                        repos.put(repo.getUrl(), repo);
                    }
                    for (ArtifactRepository repo : (List<ArtifactRepository>) moduleProject.getRemoteArtifactRepositories()) {
                        repos.put(repo.getUrl(), repo);
                    }                    

                    // repository
                    boolean found = false;
                    if (moduleProject.getDistributionManagementArtifactRepository()!=null) {
                        String publishRepoID = moduleProject.getDistributionManagementArtifactRepository().getId();

                        for (ArtifactRepository repo : repos.values()) {
                            if (repo.getId().equals(publishRepoID)) {
                                // this is where we download it from
                                generator.addRepository(repo);
                                found = true;
                                break;
                            }
                        }
                    }


                    if (!found) {

                        for (ArtifactRepository repo : repos.values()) {
                            String tentativeLocation = repo.getUrl() + "/" + repo.pathOf(module);
                            URL tentativeURL = new URL(tentativeLocation);
                            URLConnection connection = tentativeURL.openConnection();
                            connection.connect();
                            if (connection instanceof HttpURLConnection) {
                                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    generator.addRepository(repo);
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {
                        generator.addRepository(null);
                    }
           
                    // developers

                    generator.addDevelopers((List<Developer>) moduleProject.getDevelopers());

                    // mailing-lists
                    generator.addMailingLists((List<MailingList>) moduleProject.getMailingLists());

                    // imports
                    generator.addImports(filterImports((List<Dependency>) moduleProject.getModel().getDependencies()));

                    // importedBy
                    generator.addImportedBy(getImporting(module));

                    // scm
                    if (moduleProject.getScm() == null) {
                        MavenProject parentProject = moduleProject;
                        while(parentProject.hasParent()) {
                            parentProject = parentProject.getParent();
                        }
                        generator.addSCM(parentProject.getModel().getScm());
                    } else {
                        generator.addSCM(moduleProject.getModel().getScm());
                    }

                    // licenses
                    generator.addLicenses((List<License>) moduleProject.getLicenses());
                }

            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            generator.endArtifact();
        }
        generator.endGroup();
    }

    private List<Dependency> filterImports(List<Dependency> dependencies) {
        List<Dependency> result = new ArrayList<Dependency>();
        for (Dependency artifact : dependencies) {
            if (artifact.getScope()==null) {
                result.add(artifact);
            } else {
                if (artifact.getScope().equals("test")) {
                    continue;
                }
                if (artifact.getScope().equals("compile") && artifact.isOptional()) {
                    continue;
                }
                result.add(artifact);
            }
        }
        return result;
    }

    protected Map<String, List<Artifact>> dependentsXRef = null;

    protected void initXRefs(Set<Artifact> modules) {

        dependentsXRef =  new HashMap<String, List<Artifact>>();

        for (Artifact module : modules) {

            System.out.println("Importing module " + module.getId());
            MavenProject moduleProject = null;
            try {
                moduleProject = loadPom(module);
            } catch(Exception e) {
                getLog().error("cannot load " + module.getId());
            }
            if (moduleProject!=null) {
                List<Dependency> moduleDeps = moduleProject.getModel().getDependencies();
                if (moduleDeps != null) {
                    for (Dependency moduleDep : moduleDeps) {
                        String depKey = moduleDep.getGroupId() + ":" + moduleDep.getArtifactId();
                        List<Artifact> importing = dependentsXRef.get(depKey);
                        if (importing==null) {
                            importing = new ArrayList<Artifact>();
                            dependentsXRef.put(depKey, importing);
                        }
                        importing.add(module);
                    }
                }
            }
        }
    }

    protected List<Artifact> getImporting(Artifact module) {
        if (dependentsXRef==null) {
            return null;
        }
        return dependentsXRef.get(module.getGroupId() + ":" + module.getArtifactId());
    }

    protected final Pattern scmURLPattern = Pattern.compile("scm:([^:]*):(.*)");

    protected String getSCMType(String scmConnection) {

        Matcher m = scmURLPattern.matcher(scmConnection);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    protected String getSCMSpecficPart(String scmConnection) {

        Matcher m = scmURLPattern.matcher(scmConnection);
        if (m.matches()) {
            return m.group(2);
        }
        return null;
    }

}
