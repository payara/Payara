/*
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.appclient.common;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * ClassPath utilities required by GlassFish clients.
 *
 * @author David Matejcek
 */
public class ClassPathUtils {

    private static final Function<Path, URL> TO_URL = p -> {
        try {
            return p.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Could not convert path to url: " + p, e);
        }
    };

    private static final Function<Path, Path> TO_REAL_PATH = p -> {
        try {
            return p.toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Could not resolve real path of: " + p, e);
        }
    };


    /**
     * @param clientJarFile
     * @return Main-Class attributer of the manifest file.
     */
    public static String getMainClass(File clientJarFile) {
        try (JarFile jarFile = new JarFile(clientJarFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return null;
            }
            Attributes mainAttributes = manifest.getMainAttributes();
            return mainAttributes.getValue("Main-Class");
        } catch (IOException e) {
            throw new IllegalStateException("Could not detect the main class from the manifest of " + clientJarFile, e);
        }
    }


    /**
     * @return java.class.path without gf-client.jar extended by env.APPCPATH. Never null.
     */
    public static URL[] getJavaClassPathForAppClient() {
        final Path gfClientJar = TO_REAL_PATH.apply(getGFClientJarPath());
        final List<Path> paths = convertClassPathToPaths(System.getProperty("java.class.path"));
        final List<URL> result = new ArrayList<>();
        for (Path path : paths) {
            if (!TO_REAL_PATH.apply(path).equals(gfClientJar)) {
                result.add(TO_URL.apply(path));
            }
        }
        result.addAll(convertClassPathToURLs(System.getenv("APPCPATH")));
        return result.toArray(new URL[result.size()]);
    }

    /**
     * @return {@link URL} to the gf-client.jar
     */
    public static URL getGFClientJarURL() {
        return TO_URL.apply(getGFClientJarPath());
    }


    /**
     * @return {@link Path} to the gf-client.jar
     */
    public static Path getGFClientJarPath() {
        try {
            Class<?> clazz = Class.forName("org.glassfish.appclient.client.acc.agent.AppClientContainerAgent");
            CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                throw new IllegalStateException("Unable to detect the gf-client.jar location,"
                    + " because the getCodeSource() or getLocation() method returned null."
                    + " That can happen ie. when you use the boot classloader"
                    + " or a classloader which doesn't use locations.");
            }
            return Path.of(codeSource.getLocation().toURI());
        } catch (ClassNotFoundException | URISyntaxException e) {
            throw new IllegalStateException("Could not detect the GlassFish lib directory.", e);
        }
    }


    /**
     * @param classPath files separated by {@link File#pathSeparator}
     * @return classPath as a list of {@link URL}
     */
    public static List<URL> convertClassPathToURLs(final String classPath) {
        return convertClassPathToPaths(classPath).stream().map(TO_URL).collect(Collectors.toList());
    }


    /**
     * @param classPath files separated by {@link File#pathSeparator}
     * @return classPath as a list of {@link Path}
     */
    public static List<Path> convertClassPathToPaths(final String classPath) {
        if (classPath == null || classPath.isBlank()) {
            return emptyList();
        }
        final List<Path> result = new ArrayList<>();
        try {
            for (String classPathElement : classPath.split(File.pathSeparator)) {
                result.add(new File(classPathElement.trim()).toPath());
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse the classpath: " + classPath, e);
        }
    }


    /**
     * @param url url describing a {@link Path}
     * @return string path
     */
    public static String convertToString(URL url) {
        try {
            return Path.of(url.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot convert to URI string: " + url, e);
        }
    }
}
