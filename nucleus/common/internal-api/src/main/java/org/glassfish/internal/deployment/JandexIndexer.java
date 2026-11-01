/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025-2026] Payara Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.internal.deployment;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Jandex-based class and annotation index of a deployment, replacing HK2 class-model
 * ({@code Types}/{@code Parser}) scanning.
 * <p>
 * One {@link Index} is kept per archive (the root archive, every nested jar, war or rar,
 * expanded EAR modules, shared WAR libraries and external libraries), keyed by the archive URI
 * and stored in the transient metadata of the (root) deployment context. Indexes of library
 * jars are cached on disk in the instance directory, so repeated deployments and server
 * restarts do not re-index unchanged libraries.
 */
@Contract
public interface JandexIndexer {

    /**
     * Index of a single archive together with memoized, content-dependent facts about it.
     */
    final class Index {
        private final org.jboss.jandex.Index index;
        private volatile Boolean cdiEnablingAnnotations;
        private volatile Boolean validBeanArchive;

        public Index(org.jboss.jandex.Index index) {
            this.index = Objects.requireNonNull(index);
        }

        public org.jboss.jandex.Index getIndex() {
            return index;
        }

        /**
         * @return true if any element in this archive is annotated with one of the given annotations
         */
        public boolean hasAnnotations(String... annotationNames) {
            return hasAnnotations(Arrays.asList(annotationNames));
        }

        public boolean hasAnnotations(Collection<String> annotationNames) {
            return annotationNames.stream().anyMatch(name -> !index.getAnnotations(DotName.createSimple(name)).isEmpty());
        }

        /**
         * @return names of the classes that declare, or whose members or parameters declare,
         * any of the given annotations
         */
        public Set<String> getAnnotatedClassNames(Collection<String> annotationNames) {
            Set<String> result = new HashSet<>();
            for (String annotationName : annotationNames) {
                for (AnnotationInstance instance : index.getAnnotations(DotName.createSimple(annotationName))) {
                    String className = getClassName(instance);
                    if (className != null) {
                        result.add(className);
                    }
                }
            }
            return result;
        }

        /**
         * @return true if any class in this archive refers to the given class
         */
        public boolean references(String className) {
            return !index.getKnownUsers(DotName.createSimple(className)).isEmpty();
        }

        public Set<String> getKnownClassNames() {
            return index.getKnownClasses().stream().map(classInfo -> classInfo.name().toString())
                    .collect(Collectors.toSet());
        }

        /**
         * Memoized: does this archive contain CDI bean-defining annotations
         */
        public boolean hasCDIEnablingAnnotations(BooleanSupplier compute) {
            Boolean result = cdiEnablingAnnotations;
            if (result == null) {
                result = compute.getAsBoolean();
                cdiEnablingAnnotations = result;
            }
            return result;
        }

        /**
         * Memoized: is this archive a valid bean archive with respect to CDI extensions and beans.xml
         */
        public boolean isValidBeanArchive(BooleanSupplier compute) {
            Boolean result = validBeanArchive;
            if (result == null) {
                result = compute.getAsBoolean();
                validBeanArchive = result;
            }
            return result;
        }

        /**
         * @return name of the class declaring the annotated element, or null if the target is unknown
         */
        public static String getClassName(AnnotationInstance instance) {
            ClassInfo declaringClass = getDeclaringClass(instance.target());
            return declaringClass == null ? null : declaringClass.name().toString();
        }

        public static ClassInfo getDeclaringClass(AnnotationTarget target) {
            if (target == null) {
                return null;
            }
            switch (target.kind()) {
                case CLASS:
                    return target.asClass();
                case FIELD:
                    return target.asField().declaringClass();
                case METHOD:
                    return target.asMethod().declaringClass();
                case METHOD_PARAMETER:
                    return target.asMethodParameter().method().declaringClass();
                case RECORD_COMPONENT:
                    return target.asRecordComponent().declaringClass();
                case TYPE:
                    return getDeclaringClass(target.asType().enclosingTarget());
                default:
                    return null;
            }
        }
    }

    /**
     * Index the deployment's source archive, its nested archives and libraries, unless already indexed.
     */
    void index(DeploymentContext deploymentContext) throws IOException;

    /**
     * @return index of the deployment context's source archive, or null if it was not indexed
     */
    Index getRootIndex(DeploymentContext deploymentContext);

    /**
     * @return all indexes of the deployment (also for sub-module contexts), keyed by archive URI; empty if not indexed
     */
    Map<String, Index> getAllIndexes(DeploymentContext deploymentContext);

    /**
     * Resolve the index for an archive URI. A directory inside an indexed archive (e.g. WEB-INF/classes)
     * resolves to the index of the enclosing archive; an archive that was not indexed yet is indexed on demand.
     *
     * @return the index, or null if the archive cannot be read
     */
    Index getIndex(DeploymentContext deploymentContext, URI archive);

    /**
     * @return indexes for the given archive URIs (see {@link #getIndex}), keyed by the normalized archive URI
     */
    Map<String, Index> getIndexesByURI(DeploymentContext deploymentContext, Collection<URI> archives);

    /**
     * @return true if any of the archives contains one of the annotations
     */
    boolean hasAnyAnnotations(DeploymentContext deploymentContext, Collection<URI> archives, String... annotationNames);

    /**
     * Index an archive outside of a deployment, e.g. to detect its type before a deployment context
     * exists. The index is remembered, so a deployment of the same archive reuses it instead of
     * indexing again.
     */
    Index indexArchive(ReadableArchive archive) throws IOException;

    /**
     * Whether a class of the archive, of an archive nested at its top level or of a library on its
     * manifest {@code Class-Path} carries one of the annotations. Uses the deployment's indexes
     * when a deployment is in progress, otherwise {@link #indexArchive(ReadableArchive)}.
     */
    boolean hasAnnotations(ReadableArchive archive, Collection<String> annotationNames) throws IOException;

    default boolean hasAnnotations(ReadableArchive archive, Class<?>... annotationTypes) throws IOException {
        return hasAnnotations(archive, Arrays.stream(annotationTypes).map(Class::getName).collect(Collectors.toList()));
    }

    /**
     * @return names of the classes in the whole deployment annotated with any of the given annotations
     */
    default Set<String> getAnnotatedClassNames(DeploymentContext deploymentContext, String... annotationNames) {
        List<String> names = Arrays.asList(annotationNames);
        Set<String> result = new HashSet<>();
        for (Index index : getAllIndexes(deploymentContext).values()) {
            result.addAll(index.getAnnotatedClassNames(names));
        }
        return result;
    }

    /**
     * @return true if any class in the deployment refers to one of the given classes
     */
    default boolean references(DeploymentContext deploymentContext, String... classNames) {
        return getAllIndexes(deploymentContext).values().stream()
                .anyMatch(index -> Arrays.stream(classNames).anyMatch(index::references));
    }

    /**
     * @return a view over all indexes of the deployment, for class hierarchy queries spanning archives
     */
    default IndexView getCompositeIndex(DeploymentContext deploymentContext) {
        return compositeIndex(getAllIndexes(deploymentContext).values());
    }

    static IndexView compositeIndex(Collection<Index> indexes) {
        return CompositeIndex.create(indexes.stream().map(Index::getIndex).collect(Collectors.<IndexView>toList()));
    }

    /**
     * @return a view over the indexes completed by the classes the class loader can see, resolved on
     * demand without loading them: superclasses and interfaces in the server or in libraries,
     * annotation types and their defaults (see {@link ClassLoaderIndex})
     */
    static IndexView compositeIndex(Collection<Index> indexes, ClassLoader classLoader) {
        List<IndexView> views = indexes.stream().map(Index::getIndex).collect(Collectors.<IndexView>toList());
        views.add(new ClassLoaderIndex(classLoader));
        return CompositeIndex.create(views);
    }

    /**
     * Index the class files of an archive. Nested archives are not descended into.
     *
     * @param entryFilter selects the class file entries to index
     */
    static org.jboss.jandex.Index indexArchive(ReadableArchive archive, Predicate<String> entryFilter) throws IOException {
        Indexer indexer = new Indexer();
        for (Enumeration<String> entries = archive.entries(); entries.hasMoreElements();) {
            String entry = entries.nextElement();
            if (entry.endsWith(".class") && !entry.startsWith("META-INF/") && entryFilter.test(entry)) {
                try (InputStream stream = archive.getEntry(entry)) {
                    if (stream != null) {
                        indexer.index(stream);
                    }
                } catch (IOException | RuntimeException e) {
                    Logger.getLogger(JandexIndexer.class.getName()).log(Level.WARNING,
                            e, () -> "Unable to index " + entry + " in " + archive.getURI());
                }
            }
        }
        return indexer.complete();
    }
}
