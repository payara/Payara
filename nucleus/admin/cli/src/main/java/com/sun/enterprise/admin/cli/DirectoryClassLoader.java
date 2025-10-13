/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018 - 2019] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class loader that loads classes from all jar files
 * in a specified directory.
 */
public class DirectoryClassLoader extends URLClassLoader {

    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(DirectoryClassLoader.class);
    private static final int MAX_DEPTH = 5;
    private static final Comparator<Path> FILENAME_COMPARATOR = Comparator.comparing(Path::getFileName);
    private static final Function<Path, URL> MAPPER = p -> {
        try {
            return p.toUri().toURL();
        } catch (final Exception e) {
            throw new IllegalStateException(STRINGS.get("DirError", p));
        }
    };


    /**
     * Initializes a new instance by the jarsAndDirs, filtered and ordered by file names.
     *
     * @param jarsAndDirs
     * @param parent - parent has higher priority.
     */
    public DirectoryClassLoader(final Set<File> jarsAndDirs, final ClassLoader parent) {
        super(getJars(jarsAndDirs), parent);
    }


    /**
     * Create a DirectoryClassLoader to load from jar files in
     * the specified directory, with the specified parent class loader.
     *
     * @param dir the directory of jar files to load from
     * @param parent the parent class loader
     */
    public DirectoryClassLoader(final File dir, final ClassLoader parent) {
        super(getJars(dir), parent);
    }


    private static URL[] getJars(final Set<File> jarsAndDirs) {
        return getJars(jarsAndDirs.toArray(new File[jarsAndDirs.size()]));
    }


    private static URL[] getJars(final File... jarsAndDirs) {
        return Arrays.stream(jarsAndDirs).map(DirectoryClassLoader::getJarPaths).flatMap(Set::stream)
            .sorted(FILENAME_COMPARATOR).map(MAPPER).toArray(URL[]::new);
    }


    private static Set<Path> getJarPaths(final File dir) {
        try (Stream<Path> stream = Files.walk(dir.toPath(), MAX_DEPTH)) {
            return stream.filter(path -> !Files.isDirectory(path))
                .filter(path -> path.getFileName().toString().endsWith(".jar")).collect(Collectors.toSet());
        } catch (final IOException e) {
            throw new IllegalStateException(STRINGS.get("DirError", dir), e);
        }
    }


    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append(getClass().getName()).append('@').append(hashCode()).append("(\n");
        Arrays.stream(getURLs()).forEach(u -> b.append(u).append('\n'));
        b.append(')');
        return b.toString();
    }
}
