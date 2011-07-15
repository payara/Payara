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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.*;
import org.apache.maven.project.MavenProject;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Oct 9, 2007
 * Time: 8:35:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ContributorsWikiGenerator implements DistributionVisitor {

    PrintWriter writer = null;
    MavenProject distribution;
    Artifact currentArtifact;
    Map<String, Developer> contributors = new HashMap<String, Developer>();
    Map<String, List<Artifact>> contributorProjects = new HashMap<String, List<Artifact>>();

    /**
     * Sets the writer associated with his generator
     *
     * @param writer       the writer associated with this generator
     * @param distribution the distribution's project
     */
    public void beginDistribution(PrintWriter writer, MavenProject distribution) {
        this.writer = writer;
        this.distribution = distribution;

        writer.append("!!! Contributors list for the ").append(distribution.getArtifactId()).append(":").append(
                distribution.getVersion()).println(" Distribution");

        writer.println("%%sortable");
        writer.println("|| module-id || version || lead || developers");

    }

    /**
     * Start a new category for the dashboard, usually this is represented
     * by a separate table but the underlying format will decide.
     *
     * @param categoryName name of the new category
     */
    public void beginCategory(String categoryName) {

    }

    /**
     * Start a new group of modules or librairies.
     *
     * @param groupName the new group name
     * @param inTable   true if the group should be displayed as a column
     *                  or as a subtitle.
     */
    public void beginGroup(String groupName, boolean inTable) {
    }

    /**
     * Start a new module/library artifact.
     *
     * @param artifact artifact for the module
     */
    public void beginArtifact(Artifact artifact) {
        writer.append("| <a name=\"").append(artifact.getArtifactId()).append("\"/>").append(
                artifact.getGroupId()+":"+artifact.getArtifactId()).append("|").append(artifact.getVersion());
        currentArtifact = artifact;
    }

    /**
     * Adds the module's size
     *
     * @param size module's size
     */
    public void addSize(long size) {
    }

    /**
     * Adds the module's repository
     *
     * @param repository module's repository
     */
    public void addRepository(ArtifactRepository repository) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private void addDeveloperXRef(Developer dev, Artifact artifact) {
        List<Artifact> artifacts = this.contributorProjects.get(dev.getName());        
        if (artifacts==null) {
            artifacts = new ArrayList<Artifact>();
            contributorProjects.put(dev.getName(), artifacts);
        }
        if (!artifacts.contains(artifact)) {
            artifacts.add(artifact);
        }
        contributors.put(dev.getName(), dev);
    }

    /**
     * Adds the list of developers of the module
     *
     * @param devs module's developers
     */
    public void addDevelopers(List<Developer> devs) {
        boolean foundLead=false;
        for (Developer dev : devs) {
            for (String role : (List<String>) dev.getRoles()) {
                if ("lead".equalsIgnoreCase(role)) {
                    foundLead = true;
                    String title = dev.getName();
                    if (dev.getOrganization()!=null && !dev.getOrganization().startsWith("Sun")) {
                        title = title + "(" + dev.getOrganization() + ")";
                    }
                    if (dev.getUrl()!=null) {
                        writer.append("| [").append(title).append("|").append(dev.getUrl()).append("]");
                    } else {
                        writer.append("|").append(title);
                    }
                    addDeveloperXRef(dev, currentArtifact);
                    break;
                }
            }
        }
        if (!foundLead) {
            writer.append("| Unknown");
        }
        boolean foundDev=false;
        writer.append("| ");
        for (Developer dev : devs) {
            for (String role : (List<String>) dev.getRoles()) {
                if ("developer".equalsIgnoreCase(role)) {
                    foundDev=true;
                    String title = dev.getName();
                    if (dev.getOrganization()!=null && !dev.getOrganization().startsWith("Sun")) {
                        title = title + "(" + dev.getOrganization() + ")";
                    }
                    if (dev.getUrl()!=null) {
                        writer.append("[").append(title).append("|").append(dev.getUrl()).append("]");
                    } else {
                        writer.append(title);
                    }
                    writer.append("\\\\");
                    addDeveloperXRef(dev, currentArtifact);
                }
            }
        }
        if (!foundDev) {
            writer.append("Unknown");
        }        
    }

    /**
     * Adds the list of mailing-lists associated with the module
     *
     * @param lists mailing-lists for the module
     */
    public void addMailingLists(List<MailingList> lists) {
    }

    /**
     * Adds the list of imported modules by this module
     *
     * @param dependencies module's dependencies
     */
    public void addImports(List<Dependency> dependencies) {
    }

    /**
     * Adds the list of modules importing the current module
     *
     * @param importers module's importers
     */
    public void addImportedBy(List<Artifact> importers) {
    }

    /**
     * Adds the source code management information
     *
     * @param scm module's SCM
     */
    public void addSCM(Scm scm) {
    }

    /**
     * Adds the licenses governing the module
     *
     * @param licenses module's licenses
     */
    public void addLicenses(List<License> licenses) {
    }

    /**
     * End of the current module's model
     */
    public void endArtifact() {
        writer.println();        
    }

    /**
     * End of the current group
     */
    public void endGroup() {
    }

    /**
     * End of the current category
     */
    public void endCategory() {
    }

    /**
     * End of the distribution
     */
    public void endDistribution() {
        writer.println();

        writer.append("!! List of project per contributors ").append(distribution.getArtifactId()).append(":").append(
                distribution.getVersion()).println(" Distribution");

        writer.append("! Sun Contributors");
        writer.println("%%sortable");
        writer.println("|| name || module-id");
        for (Map.Entry<String, List<Artifact>> entry : contributorProjects.entrySet()) {
            String devName = entry.getKey();
            Developer dev = contributors.get(devName);
            if (dev.getOrganization()!=null && !dev.getOrganization().startsWith("Sun"))
                continue;
            writer.append("| " + devName + " | ");
            for (Artifact module : entry.getValue()) {
                writer.append("<a href=#").append(module.getArtifactId()).append(">").append(
                    module.getGroupId()).append(":").append(module.getArtifactId()).append("</a>\\\\");                
            }
            writer.println("");
        }
        writer.append("! External Contributors");
        writer.println("%%sortable");
        writer.println("|| name || company || module-id");
        for (Map.Entry<String, List<Artifact>> entry : contributorProjects.entrySet()) {
            String devName = entry.getKey();
            Developer dev = contributors.get(devName);
            if (dev.getOrganization()!=null && dev.getOrganization().startsWith("Sun"))
                continue;
            writer.append("| " + devName + " | " + dev.getOrganization() + " | ");
            for (Artifact module : entry.getValue()) {
                writer.append("<a href=#").append(module.getArtifactId()).append(">").append(
                    module.getGroupId()).append(":").append(module.getArtifactId()).append("</a>\\\\");
            }
            writer.println("");
        }
        writer.append("Generated by ").append(System.getProperty("user.name")).append(
                " on ").append(Calendar.getInstance().getTime().toString());
        
    }
}
