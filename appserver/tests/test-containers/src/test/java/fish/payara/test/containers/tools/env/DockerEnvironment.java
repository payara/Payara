/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerDockerImageManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Manages docker environment for tests.
 *
 * @author David Matějček
 */
public class DockerEnvironment implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DockerEnvironment.class);

    private static DockerEnvironment environment;

    /** Docker network */
    private final Network network;
    private final PayaraServerContainer payaraContainer;

    private final LocalDateTime startupTime;


    /**
     * Creates new singleton instance or throws ISE if it already exists.
     *
     * @param cfg configuration of the docker environment
     * @return new instance.
     * @throws Exception
     */
    public static synchronized DockerEnvironment createEnvironment(final DockerEnvironmentConfiguration cfg)
        throws Exception {
        if (environment != null) {
            throw new IllegalStateException("Environment already created.");
        }

        environment = new DockerEnvironment(cfg);
        return environment;
    }


    /**
     * @return singleton instance.
     */
    public static DockerEnvironment getInstance() {
        return environment;
    }


    /**
     * Initializes the whole environment as configured.
     *
     * @param cfg
     * @throws Exception
     */
    protected DockerEnvironment(final DockerEnvironmentConfiguration cfg) throws Exception {

        // STEP1: Create images (they will be cached)
        LOG.info("Using docker environment configuration:\n{}", cfg);
        this.network = Network.newNetwork();

        final List<CompletableFuture<Void>> parallelFutures = new ArrayList<>();

        final PayaraServerDockerImageManager fbServerMgr = //
            new PayaraServerDockerImageManager(this.network, cfg.getPayaraServerConfiguration());
        parallelFutures.add(CompletableFuture.runAsync(() -> fbServerMgr.prepareImage(cfg.isForceNewPayaraServer())));

        try {
            CompletableFuture.allOf(parallelFutures.toArray(new CompletableFuture[parallelFutures.size()]))
                .get(cfg.getPreparationTimeout(), TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException("Could not initialize docker environment!", e);
        }

        // STEP2: Create and start containers sequentionally (respect dependencies!)
        this.payaraContainer = fbServerMgr.start();
        logContainerStarted(this.payaraContainer);
        this.startupTime = LocalDateTime.now();
    }


    /**
     * @return time when the environment has been completely initialized.
     */
    public LocalDateTime getStartupTime() {
        return this.startupTime;
    }


    /**
     * @return initialized internal network of the docker environment.
     */
    public Network getNetwork() {
        return this.network;
    }


    /**
     * Resets the logging to lighter (benchmarks) or heavier (functionalities) version.
     *
     * @param fileInContainer
     */
    public void reconfigureLogging(final File fileInContainer) {
        this.payaraContainer.reconfigureLogging(fileInContainer);
    }


    /**
     * @return basic URI of applications, for example: http://host:port/basicContext
     */
    public URI getBaseUri() {
        return this.payaraContainer.getBaseUri();
    }


    @Override
    public void close() {
        if (!this.payaraContainer.isRunning()) {
            return;
        }
        LOG.info("Closing docker containers ...");
        try {
            final ExecResult result = this.payaraContainer.execInContainer("killall", "-v", "java");
            LOG.info("killall output: \n OUT: {}\n ERR: {}", result.getStdout(), result.getStderr());
            Thread.sleep(5000L);
        } catch (final IOException | InterruptedException e) {
            LOG.error("Could not shutdown the server nicely.", e);
        }
        closeSilently(this.payaraContainer);
        closeSilently(this.network);
    }


    private static void logContainerStarted(final GenericContainer<?> container) {
        LOG.info("\n" //
            + "========================================\n"
            + "{}(name: '{}') started, you can use this urls:\n"
            + "https://{}:{}\n"
            + "http://{}:{}\n"
            + "========================================", //
            container.getClass().getSimpleName(), container.getContainerInfo().getName(),
            container.getContainerIpAddress(),
            container.getMappedPort(4848),
            container.getContainerIpAddress(),
            container.getMappedPort(8080));
    }


    private static void closeSilently(final AutoCloseable closeable) {
        LOG.trace("closeSilently(closeable={})", closeable);
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final Exception e) {
            LOG.warn("Close method caused an exception.", e);
        }
    }
}
