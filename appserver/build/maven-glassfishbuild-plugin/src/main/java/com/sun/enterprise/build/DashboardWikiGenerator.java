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
import org.apache.maven.model.*;
import org.apache.maven.project.MavenProject;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.repository.ArtifactRepository;


/*
 * Implementation of the DistributionVisitor interface for wiki
 *
 * @author Jerome Dochez
 */
public class DashboardWikiGenerator implements DistributionVisitor {

    String category = null;
    PrintWriter writer = null;
    boolean groupInTable = false;
    Artifact module;
    MavenProject distribution;
    List<String> footNotes = new ArrayList<String>();

    /**
     * Sets the writer associated with his generator
     *
     * @param writer the writer associated with this generator
     * @param distribution the distribution's project
     */
    public void beginDistribution(PrintWriter writer, MavenProject distribution) {
        this.writer = writer;
        this.distribution = distribution;

        writer.append("!!! Dashboard for the ").append(distribution.getArtifactId()).append(":").append(
                distribution.getVersion()).println(" Distribution");

    }


    /**
     * Start a new category for the dashboard, usually this is represented
     * by a separate table but the underlying format will decide.
     *
     * @param categoryName name of the new category
     */
    public void beginCategory(String categoryName) {
        writer.println("!! " + categoryName + " List");
    }

    /**
     * Start a new group of modules or librairies.
     *
     * @param groupName the new group name
     * @param inTable true if the group should be displayed as a column
     * or as a subtitle.
     */
    public void beginGroup(String groupName, boolean inTable) {
        this.groupInTable = inTable;
        if (!groupInTable) {
            writer.append("! Group : ").println(groupName);
        }
        writer.println("%%sortable");
        if (groupInTable) {
            writer.append("|| group-id");
        }
        writer.println("|| module-id || version || size || repository || lead || mailing-lists || imports || importedBy || SCM || license");
    }

    /**
     * Start a new module/library artifact.
     *
     * @param artifact artifact for the module
     */
    public void beginArtifact(Artifact artifact) {

        this.module=artifact;
        if (groupInTable) {
            writer.write("|" + artifact.getGroupId());
        }
        writer.append("| <a name=\"").append(artifact.getArtifactId()).append("\"/>").append(
                artifact.getArtifactId()).append("|").append(artifact.getVersion());

    }

    /**
     * Adds the module's size
     * @param size module's size
     */
    public void addSize(long size) {
        writer.append("| ").append(String.valueOf(size / 1024)).append(" KB");
    }

    /**
     * Adds the module's repository
     * @param repository module's repository
     */
    public void addRepository(ArtifactRepository repository) {
        if (repository!=null) {
                writer.append(" | [").append(repository.getId()).append("|").append(repository.getUrl()).append(
                    "/").append(artifactToPath(module)).append("]");
            } else {
                writer.write(" | unknown");
            }
    }

    /**
     * Adds the list of developers of the module
     * @param devs module's developers
     */
    public void addDevelopers(List<Developer> devs) {

        boolean foundLead=false;

        for (Developer dev : devs) {
            for (String role : (List<String>) dev.getRoles()) {
                if ("lead".equalsIgnoreCase(role)) {
                    foundLead = true;
                    if (dev.getUrl()!=null) {
                        writer.append("| [").append(dev.getName()).append("|").append(dev.getUrl()).append("]");
                    } else {
                        writer.append("|").append(dev.getName());
                    }
                    break;
                }
            }
        }
        if (!foundLead) {
            writer.write("| no owner");
        }

    }

    /**
     * Adds the list of mailing-lists associated with the module
     *
     * @param mailingLists mailing-lists for the module
     */
    public void addMailingLists(List<MailingList> mailingLists) {
        if (mailingLists.isEmpty()) {
            writer.write("| none");
        } else {
            writer.write("|");
            for (MailingList mailingList : mailingLists) {
                writer.append(" [").append(mailingList.getName()).append(
                        "|").append(mailingList.getArchive()).append("]\\\\");
            }
        }
    }

    /**
     * Adds the list of imported modules by this module
     * @param dependencies module's dependencies
     */
    public void addImports(List<Dependency> dependencies) {
        writer.write(" | ");
        if (dependencies != null) {
            for (Dependency moduleDep : dependencies) {
                writer.append("<a href=#").append(moduleDep.getArtifactId()).append(">").append(
                    moduleDep.getArtifactId()).append(":").append(moduleDep.getVersion()).append("</a>\\\\");
            }
        }

    }

    /**
     * Adds the list of modules importing the current module
     *
     * @param importers module's importers
     */
    public void addImportedBy(List<Artifact> importers) {
        writer.write(" | ");
        if (importers == null || importers.isEmpty()) {
            writer.append(distribution.getArtifactId()).append(":").append(distribution.getVersion());
        } else {
            for (Artifact importer : importers) {
                writer.write(" <a href=#" + importer.getArtifactId() + ">" + importer.getArtifactId() + ":" + importer.getVersion() + "</a>\\\\");
            }
        }
    }

    /**
     * Adds the source code management information
     *
     * @param scm module's SCM
     */
    public void addSCM(Scm scm) {
        if (scm!=null && getSCMType(scm.getConnection())!=null) {
            String scmType = getSCMType(scm.getConnection());
            if (scmType.equals("hg")) {
                String scmSpecificPart = getSCMSpecficPart(scm.getConnection());
                writer.append("| [").append(scmType).append("|").append(scmSpecificPart).append("]");
            } else {
                String group = module.getGroupId().substring(module.getGroupId().lastIndexOf('.')+1);
                String subProjectScm = module.getArtifactId() + "/" + group + "/" +
                        module.getArtifactId();
                int insertionIndex;
                String proposedScm = scm.getConnection().substring(4);
                if (proposedScm.endsWith(subProjectScm)) {
                    proposedScm = proposedScm.substring(0, proposedScm.length()-subProjectScm.length());
                }
                if (footNotes.contains(proposedScm)) {
                    insertionIndex = footNotes.indexOf(proposedScm);
                } else {
                    footNotes.add(proposedScm);
                    insertionIndex = footNotes.size();
                }
                writer.write("|[" + insertionIndex + "]");
            }
        } else {
            writer.write("| unknown");
        }
    }

    /**
     * Adds the licenses governing the module
     * @param licenses module's licenses
     */
    public void addLicenses(List<License> licenses) {
        writer.write("|");
        if (licenses == null || licenses.isEmpty()) {
            writer.write("unknown");
        } else {
            for (License license : licenses) {
                writer.append("[").append(license.getName()).append("|").append(license.getUrl()).append("]\\\\");
            }
        }
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
        writer.println("%%");
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
        int index = 1;
        for (String footNote : footNotes) {
            writer.append("[#").append(String.valueOf(index++)).append("]").append(footNote).append("\\\\").println();
        }
        writer.println();
        writer.append("Generated by ").append(System.getProperty("user.name")).append(
                " on ").append(Calendar.getInstance().getTime().toString());

    }

    private String artifactToPath(Artifact a) {
        return a.getGroupId().replace('.', '/') + "/" + a.getArtifactId() + "/" + a.getVersion();
    }    

    protected final Pattern scmURLPattern = Pattern.compile("scm:([^:]*):(.*)");

    protected String getSCMType(String scmConnection) {

        if (scmConnection!=null) {
            Matcher m = scmURLPattern.matcher(scmConnection);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    protected String getSCMSpecficPart(String scmConnection) {

        if (scmConnection!=null) {
            Matcher m = scmURLPattern.matcher(scmConnection);
            if (m.matches()) {
                return m.group(2);
            }
        }
        return null;
    }
}


