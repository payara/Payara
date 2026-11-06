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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2020-2026] Payara Foundation and/or affiliates
package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.JandexIndexer.Index;
import org.jboss.jandex.Indexer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Detects the presence of annotations in an archive without loading classes: through the Jandex
 * indexes of the deployment in progress, or by indexing the archive directly where there is none
 * (the application client container).
 *
 * @author Jerome Dochez
 */
public class AnnotationDetector {

    /** the component-defining annotations of enterprise beans */
    public static final List<String> EJB_COMPONENT_ANNOTATIONS = List.of(
            "jakarta.ejb.Stateless", "jakarta.ejb.Stateful", "jakarta.ejb.MessageDriven", "jakarta.ejb.Singleton");

    /** the annotations of a resource adapter */
    public static final List<String> CONNECTOR_ANNOTATIONS = List.of(
            "jakarta.resource.spi.Connector", "jakarta.resource.spi.AdministeredObject",
            "jakarta.resource.spi.Activation", "jakarta.resource.spi.AuthenticationMechanism",
            "jakarta.resource.spi.ConfigProperty", "jakarta.resource.spi.ConnectionDefinition",
            "jakarta.resource.spi.ConnectionDefinitions", "jakarta.resource.spi.SecurityPermission");

    /** the annotations referring to a persistence unit */
    public static final List<String> PERSISTENCE_UNIT_ANNOTATIONS = List.of(
            "jakarta.persistence.PersistenceUnit", "jakarta.persistence.PersistenceUnits");

    private final List<String> annotationNames;

    public AnnotationDetector(Collection<String> annotationNames) {
        this.annotationNames = new ArrayList<>(annotationNames);
    }

    /**
     * @return whether a class of the archive or of the libraries it refers to carries one of the
     * annotations
     */
    public boolean hasAnnotationInArchive(ReadableArchive archive) throws IOException {
        List<URI> others = additionalArchives(archive);
        try {
            others.addAll(DOLUtils.getLibraryJarURIs(null, archive));
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, e.getMessage(), e);
        }
        DeploymentContext deploymentContext = DeploymentUtils.getCurrentDeploymentContext();
        if (deploymentContext != null) {
            List<URI> uris = new ArrayList<>();
            uris.add(archive.getURI());
            uris.addAll(others);
            return Globals.getDefaultHabitat().getService(JandexIndexer.class)
                    .hasAnyAnnotations(deploymentContext, uris, annotationNames.toArray(new String[0]));
        }
        // no deployment in progress, e.g. in the application client container: index directly
        if (hasAnnotations(archive)) {
            return true;
        }
        ArchiveFactory archiveFactory = Globals.getDefaultHabitat().getService(ArchiveFactory.class);
        for (URI uri : others) {
            try (ReadableArchive other = archiveFactory.openArchive(uri)) {
                if (hasAnnotations(other)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return whether the class file at the entry carries one of the annotations
     */
    public boolean containsAnnotation(ReadableArchive archive, String entryName) throws IOException {
        try (InputStream stream = archive.getEntry(entryName)) {
            if (stream == null) {
                return false;
            }
            Indexer indexer = new Indexer();
            indexer.index(stream);
            return new Index(indexer.complete()).hasAnnotations(annotationNames);
        }
    }

    /**
     * @return archives to detect in addition to the archive itself and its libraries
     */
    protected List<URI> additionalArchives(ReadableArchive archive) throws IOException {
        return new ArrayList<>();
    }

    private boolean hasAnnotations(ReadableArchive archive) throws IOException {
        return new Index(JandexIndexer.indexArchive(archive, entry -> true)).hasAnnotations(annotationNames);
    }
}
