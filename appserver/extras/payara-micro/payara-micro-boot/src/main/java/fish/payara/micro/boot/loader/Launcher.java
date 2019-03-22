/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Modified 2017 Payara Foundation
package fish.payara.micro.boot.loader;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import fish.payara.micro.boot.loader.archive.Archive;
import fish.payara.micro.boot.loader.archive.JarFileArchive;

/**
 * Base class for launchers that can start an application with a fully
 * configured classpath backed by one or more {@link Archive}s.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public abstract class Launcher {

    /**
     * Launch the application. This method is the initial entry point that
     * should be called by a subclass
     * {@code public static void main(String[] args)} method.
     *
     * @param args the incoming arguments
     * @throws Exception if the application fails to launch
     */
    protected Object launch(String method, String[] args) throws Exception {
        boolean explode = true;
        String unpackDir = null;
        for (int i = 0; i < args.length; i++) {
            if ("--nested".equals(args[i].toLowerCase())) {
                explode = false;
            } else if ("--unpackdir".equals(args[i].toLowerCase()) || "--rootdir".equals(args[i].toLowerCase())) {
                if (args.length >= (i + 1)) {
                    unpackDir = args[i + 1];
                }
            }
        }

        ClassLoader classLoader;
        if (!explode) {
            classLoader = createClassLoader(getClassPathArchives());
            fish.payara.micro.boot.loader.jar.JarFile.registerUrlProtocolHandler();
        } else {
            if (unpackDir != null) {
                classLoader = new ExplodedURLClassloader(new File(unpackDir));
            } else {
                classLoader = new ExplodedURLClassloader();
            }
        }
        return launch(method, args, getMainClass(), classLoader);
    }

    /**
     * Create a classloader for the specified archives.
     *
     * @param archives the archives
     * @return the classloader
     * @throws Exception if the classloader cannot be created
     */
    protected ClassLoader createClassLoader(List<Archive> archives) throws Exception {
        List<URL> urls = new ArrayList<URL>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        return createClassLoader(urls.toArray(new URL[urls.size()]));
    }

    /**
     * Create a classloader for the specified URLs.
     *
     * @param urls the URLs
     * @return the classloader
     * @throws Exception if the classloader cannot be created
     */
    protected ClassLoader createClassLoader(URL[] urls) throws Exception {
        return new LaunchedURLClassLoader(urls, getClass().getClassLoader());
    }

    /**
     * Launch the application given the archive file and a fully configured
     * classloader.
     *
     * @param args the incoming arguments
     * @param mainClass the main class to run
     * @param classLoader the classloader
     * @throws Exception if the launch fails
     */
    protected Object launch(String method, String[] args, String mainClass, ClassLoader classLoader)
            throws Exception {
        Thread.currentThread().setContextClassLoader(classLoader);
        return createMainMethodRunner(mainClass, method, args, classLoader).run();
    }

    /**
     * Create the {@code MainMethodRunner} used to launch the application.
     *
     * @param mainClass the main class
     * @param args the incoming arguments
     * @param classLoader the classloader
     * @return the main method runner
     */
    protected MainMethodRunner createMainMethodRunner(String mainClass, String method, String[] args,
            ClassLoader classLoader) {
        return new MainMethodRunner(mainClass, args, method);
    }

    /**
     * Returns the main class that should be launched.
     *
     * @return the name of the main class
     * @throws Exception if the main class cannot be obtained
     */
    protected abstract String getMainClass() throws Exception;

    /**
     * Returns the archives that will be used to construct the class path.
     *
     * @return the class path archives
     * @throws Exception if the class path archives cannot be obtained
     */
    protected abstract List<Archive> getClassPathArchives() throws Exception;

    protected final Archive createArchive() throws Exception {
        ProtectionDomain protectionDomain = getClass().getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
        String path = (location == null ? null : location.getSchemeSpecificPart());
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException(
                    "Unable to determine code source archive from " + root);
        }
        return new JarFileArchive(root);
    }

}
