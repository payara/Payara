/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.apf.jandex;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.JandexIndexer;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Indexer;
import org.jvnet.hk2.annotations.Service;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JandexIndexReader implements JandexIndexer {
    private static final String JANDEX_INDEX_METADATA_KEY = JandexIndexer.class.getName() + ".index";

    @Override
    public void index(DeploymentContext deploymentContext) throws IOException {
        if (getIndexMap(deploymentContext) == null) {
            Index index = getIndexFromArchive(deploymentContext.getSource());
            if (index == null) {
                index = indexArchive(deploymentContext);
            }
            Map<String, Index> indexMap = new HashMap<>();
            indexMap.put(deploymentContext.getSource().getName(), index);
            deploymentContext.addTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, indexMap);
        }
    }

    @Override
    public void reindex(DeploymentContext deploymentContext) throws IOException {
        deploymentContext.removeTransientAppMetaData(JANDEX_INDEX_METADATA_KEY);
        index(deploymentContext);
    }

    @Override
    public Index getRootIndex(DeploymentContext deploymentContext) {
        return getIndexMap(deploymentContext).get(deploymentContext.getSource().getName());
    }

    @Override
    public Index getIndexFromArchive(ReadableArchive archive) throws IOException {
        Index index = readIndex(archive, "META-INF/jandex.idx");
        if (index == null) {
            index = readIndex(archive, "WEB-INF/classes/META-INF/jandex.idx");
        }
        return index;
    }

    @Override
    public boolean isJakartaEEApplication(DeploymentContext deploymentContext) throws IOException {
        // Check if the application contains any Jakarta EE specific annotations or classes
        return getRootIndex(deploymentContext).getKnownClasses().stream()
                .flatMap(clazz -> clazz.annotations().stream())
                .anyMatch(annotation -> annotation.name().toString().startsWith("jakarta."));
    }

    @Override
    public boolean hasAnyAnnotations(DeploymentContext deploymentContext, List<URI> uris, String... annotations) {
        var indexMap = getIndexMap(deploymentContext);
        if (indexMap == null) {
            return false;
        }
        for (URI uri : uris) {
            Index index = indexMap.get(uri.toString());
            if (index != null) {
                for (String annotation : annotations) {
                    if (!index.getAnnotations(annotation).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Index> getIndexMap(DeploymentContext deploymentContext) {
        return deploymentContext.getTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, Map.class);
    }

    private Index readIndex(ReadableArchive archive, String path) throws IOException {
        try (InputStream stream = archive.getEntry(path)) {
            if (stream != null) {
                return new IndexReader(stream).read();
            }
        }
        return null;
    }

    private static Index indexArchive(DeploymentContext deploymentContext) throws IOException {
        Indexer indexer = new Indexer();
        StringBuilder errors = new StringBuilder();
        deploymentContext.getSource().entries().asIterator().forEachRemaining(entry -> {
            if (entry.endsWith(".class")) {
                try (InputStream stream = deploymentContext.getSource().getEntry(entry)) {
                    if (stream != null) {
                        indexer.index(stream);
                    }
                } catch (IOException e) {
                    if (errors.length() == 0) {
                        errors.append(String.format("Unable to index %s from archive %s ", entry, deploymentContext.getSource().getName()));
                    }
                }
            }
        });
        if (errors.length() > 0) {
            throw new IOException(errors.toString());
        }
        return indexer.complete();
    }
}
