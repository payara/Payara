/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package org.glassfish.web.loader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.naming.resources.FileDirContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebappClassLoaderTest {

    private static CyclicBarrier lock;
    private static ExecutorService executor;
    private static File junitJarFile;

    @BeforeClass
    public static void setup() throws URISyntaxException {
        // Run 3 methods at the same time, and make the pool large enough to increase
        // the chance of a race condition
        lock = new CyclicBarrier(3);
        executor = Executors.newFixedThreadPool(60);

        // Fetch any JAR to use for classloading
        junitJarFile = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    @AfterClass
    public static void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void check_findResourceInternalFromJars_thread_safety() throws Exception {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        final WebappClassLoader webappClassLoader = new WebappClassLoader(classLoader, null);
        webappClassLoader.start();
        webappClassLoader.setResources(new FileDirContext());

        CompletableFuture<Exception> result = new CompletableFuture<>();

        // Create the tasks, and have them each run at the same time
        // using the cyclic barrier
        Runnable lookupTask = waitAndDo(lock, result, () -> lookup(classLoader, webappClassLoader));
        Runnable addTask = waitAndDo(lock, result, () -> add(classLoader, webappClassLoader));
        Runnable closeTask = waitAndDo(lock, result, () -> webappClassLoader.closeJARs(true));

        try {
            // Run the methods at the same time
            for (int i = 0; i < 100; i++) {
                executor.execute(addTask);
                executor.execute(lookupTask);
                executor.execute(closeTask);
            }
            // Check to see if any completed exceptionally
            Exception ex = result.get(200, TimeUnit.MILLISECONDS);
            if (ex != null) {
                throw ex;
            }
        } catch (TimeoutException ex) {
            // Success!
        } finally {
            webappClassLoader.close();
        }
    }

    private void add(ClassLoader realClassLoader, WebappClassLoader webappClassLoader) throws IOException {
        List<JarFile> jarFiles = findJarFiles(realClassLoader);

        for (JarFile j : jarFiles) {
            try {
                webappClassLoader.addJar(junitJarFile.getName(), j, junitJarFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void lookup(ClassLoader realClassLoader, WebappClassLoader webappClassLoader) throws Exception {
        for (JarFile jarFile : findJarFiles(realClassLoader)) {
            for (JarEntry entry : Collections.list(jarFile.entries())) {
                webappClassLoader.findResource(entry.getName());
                // System.out.println("Looked up " + resourceEntry);
                Thread.sleep(0, 100);
            }
        }
    }

    private List<JarFile> findJarFiles(ClassLoader realClassLoader) throws IOException {
        List<JarFile> jarFiles = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            jarFiles.add(new JarFile(junitJarFile));
        }
        return jarFiles;
    }

    /**
     * Generate a task that will wait on the passed cyclic barrier before running
     * the passed task. Record the result in the passed future
     * 
     * @param lock   the lock to wait on before execution
     * @param result where to store any encountered exceptions
     * @param task   the task to run
     * @return a new task
     */
    private static Runnable waitAndDo(final CyclicBarrier lock, final CompletableFuture<Exception> result,
            final ExceptionalRunnable task) {
        return () -> {
            try {
                lock.await();
                task.run();
            } catch (Exception ex) {
                result.complete(ex);
            }
        };
    }

    /**
     * A runnable interface that allows exceptions
     */
    @FunctionalInterface
    private static interface ExceptionalRunnable {
        void run() throws Exception;
    }
}