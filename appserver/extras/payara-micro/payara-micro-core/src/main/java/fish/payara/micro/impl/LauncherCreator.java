/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */

package fish.payara.micro.impl;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

class LauncherCreator {
    public static final String LAUNCHER_JAR = "launch-micro.jar";
    private static final String MAIN_CLASS = RootDirLauncher.class.getName();

    private final File rootDir;
    private final URLClassLoader classLoader;
    private final String bootJarUrl = System.getProperty(RootDirLauncher.BOOT_JAR_URL);
    private String[] classpath;
    private Attributes bootManifestAttributes;

    LauncherCreator(File root, URLClassLoader microClassLoader) {
        this.rootDir = root;
        this.classLoader = microClassLoader;
    }

    public void buildLauncher() throws IOException {
        buildClasspath();
        buildLauncherJar();
        buildEnvFile();
    }


    /**
     * Collect the classpath entries into form suitable for Class-Path manifest atrribute
     */
    private void buildClasspath() {
        URI baseUri = rootDir.toURI();
        this.classpath = Stream.of(classLoader.getURLs())
                // filter out directories - it prevents OpenJDK CDS (JDK-8209385)
                // and root/runtime/ doesn't contain any direct resources anyway
                .filter(url -> !url.getPath().endsWith("/"))
                .map(url -> relativize(baseUri, url))
                .map(URI::toString)
                .toArray(String[]::new);
    }

    private static URI relativize(URI baseUri, URL url) {
        try {
            return baseUri.relativize(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot turn classpath entry into relative path "+url, e);
        }
    }


    /**
     * Create file with parameters to java executable when user cannot use java -jar
     * @throws IOException
     */
    private void buildEnvFile() throws IOException {
        try (PrintWriter envOut = new PrintWriter(new FileWriter(new File(rootDir, ".env")))) {
            envOut.print("MICRO_CLASSPATH=");
            envOut.println(
                    Stream.concat(Stream.of(LAUNCHER_JAR),Stream.of(classpath))
                            .map(LauncherCreator::uriToPath)
                            .collect(joining(File.pathSeparator)));
            // we escape opens so the file can be sourced into shell env easily.
            envOut.print("MICRO_OPENS=\"");
            envOut.print(Stream.concat(buildOpens(), buildExports()).collect(joining(" ")));
            envOut.println('"');
            envOut.print("MICRO_MAIN=");
            envOut.println(MAIN_CLASS);
        }
    }

    private static String uriToPath(String entry) {
        // additional libraries outside runtime are stored as file URIs
        // --class-path argument needs path
        return entry.startsWith("file:") ? new File(URI.create(entry)).getAbsolutePath() : entry;
    }

    private Stream<String> buildOpens() {
        return Stream.of(bootManifestAttributes.getValue("Add-Opens").split(" "))
                .map(open -> "--add-opens "+open+"=ALL-UNNAMED");
    }

    private Stream<String> buildExports() {
        return Stream.of(bootManifestAttributes.getValue("Add-Exports").split(" "))
                .map(export -> "--add-exports "+export+"=ALL-UNNAMED");
    }

    private void buildLauncherJar() throws IOException {
        if (bootJarUrl == null || !bootJarUrl.startsWith("file:")) {
            throw new IllegalArgumentException("Output launcher was not launched via Payara Micro Distribution artifact ("+bootJarUrl+" isn't one)");
        }

        try (JarFile bootJar = new JarFile(new File(URI.create(bootJarUrl)))) {
            this.bootManifestAttributes = bootJar.getManifest().getMainAttributes();
            Manifest mf = buildManifest();
            writeLauncher(bootJar, mf);
        }
    }

    private void writeLauncher(JarFile bootJar, Manifest mf) throws IOException {
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(new File(rootDir, LAUNCHER_JAR)), mf)) {

            // Boot classes and API are in boot jar and not in runtime directory, we need those
            // But we don't need MICRO-INF, we are already unpacked
            bootJar.stream().filter(entry -> entry.getName().startsWith("fish/"))
                    .forEach(entry -> {
                        try {
                            out.putNextEntry(entry);
                            try (InputStream input = bootJar.getInputStream(entry)) {
                                byte[] buffer = new byte[4096];
                                for (int read = 0; (read = input.read(buffer)) > 0; ) {
                                    out.write(buffer, 0, read);
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private Manifest buildManifest() {
        Manifest result = new Manifest();
        Attributes attr = result.getMainAttributes();
        attr.putAll(bootManifestAttributes);
        attr.putValue("Class-Path", Stream.of(this.classpath).collect(joining(" ")));
        attr.putValue("Main-Class", MAIN_CLASS);
        attr.remove("Start-Class");
        return result;
    }


}
