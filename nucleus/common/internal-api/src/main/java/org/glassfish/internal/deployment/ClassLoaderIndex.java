/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2026] Payara Foundation and/or its affiliates. All rights reserved.
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

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.ModuleInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A Jandex index that resolves classes by name on demand from the class files a class loader can see,
 * without defining them. Combined with the indexes of a deployment's archives it completes class
 * hierarchies and annotation definitions that live in the server or in libraries outside the
 * deployment (superclasses, implemented interfaces, annotation types and their default values).
 * <p>
 * Class files served from the server's bundles are indexed once per server lifetime and shared by
 * all instances; those served from files or jars belong to the deployment and are indexed per instance.
 * Only {@link #getClassByName(DotName)} and {@link #getKnownClasses()} yield results; the aggregate
 * queries (subclasses, implementors, annotation usages) answer for the deployment's own indexes only.
 */
public final class ClassLoaderIndex implements IndexView {

    private static final Logger LOGGER = Logger.getLogger(ClassLoaderIndex.class.getName());
    private static final String PACKAGE_INFO = "package-info";

    /** classes of the server's bundles, whose URLs identify the bundle revision and so the exact class file */
    private static final ConcurrentMap<String, Optional<ClassInfo>> SERVER_CLASSES = new ConcurrentHashMap<>();

    private final ClassLoader classLoader;
    private final ConcurrentMap<DotName, Optional<ClassInfo>> classes = new ConcurrentHashMap<>();

    public ClassLoaderIndex(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader);
    }

    @Override
    public ClassInfo getClassByName(DotName className) {
        String name = className.toString();
        if (name.equals(PACKAGE_INFO) || name.endsWith("." + PACKAGE_INFO)) {
            // package annotations belong to the archive that defines the package, never to a library
            return null;
        }
        return classes.computeIfAbsent(className, this::resolve).orElse(null);
    }

    private Optional<ClassInfo> resolve(DotName className) {
        String resource = className.toString().replace('.', '/') + ".class";
        // the classes of the deployment itself are indexed, so what is asked for here lives in the
        // server or in shared libraries: ask the parent loaders first, which spares the module class
        // loader's own (expensive, directory-walking) lookup for everything they can serve
        ClassLoader parent = classLoader.getParent();
        URL shared = parent == null ? null : parent.getResource(resource);
        URL url = shared != null ? shared : classLoader.getResource(resource);
        if (url == null) {
            return Optional.empty();
        }
        if (isDeploymentResource(url)) {
            return index(className, url);
        }
        return SERVER_CLASSES.computeIfAbsent(url.toString(), key -> index(className, url));
    }

    private static boolean isDeploymentResource(URL url) {
        String protocol = url.getProtocol();
        return "file".equals(protocol) || "jar".equals(protocol);
    }

    private static Optional<ClassInfo> index(DotName className, URL url) {
        try (InputStream stream = url.openStream()) {
            Indexer indexer = new Indexer();
            indexer.index(stream);
            return Optional.ofNullable(indexer.complete().getClassByName(className));
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.FINE, e, () -> "Unable to index " + url);
            return Optional.empty();
        }
    }

    @Override
    public Collection<ClassInfo> getKnownClasses() {
        return classes.values().stream().flatMap(Optional::stream).collect(Collectors.toList());
    }

    @Override
    public Collection<ClassInfo> getKnownDirectSubclasses(DotName className) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getAllKnownSubclasses(DotName className) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getKnownDirectSubinterfaces(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getAllKnownSubinterfaces(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getKnownDirectImplementations(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getAllKnownImplementations(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getKnownDirectImplementors(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getAllKnownImplementors(DotName interfaceName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<AnnotationInstance> getAnnotations(DotName annotationName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName annotationName, IndexView index) {
        return Collections.emptyList();
    }

    @Override
    public Collection<AnnotationInstance> getAnnotationsWithRepeatable(DotName annotationName, DotName containerName) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ModuleInfo> getKnownModules() {
        return Collections.emptyList();
    }

    @Override
    public ModuleInfo getModuleByName(DotName moduleName) {
        return null;
    }

    @Override
    public Collection<ClassInfo> getKnownUsers(DotName className) {
        return Collections.emptyList();
    }

    @Override
    public Collection<ClassInfo> getClassesInPackage(DotName packageName) {
        return Collections.emptyList();
    }

    @Override
    public Set<DotName> getSubpackages(DotName packageName) {
        return Collections.emptySet();
    }

    @Override
    public String toString() {
        return "ClassLoaderIndex[" + classLoader + "]";
    }
}
