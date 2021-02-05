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

import fish.payara.test.containers.tools.container.MySQLDockerImageManager;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerDockerImageManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;

/**
 * Manages docker environment for tests.
 *
 * @author David Matějček
 */
// TODO: refactor and enhance.
// transform to be used this way as an alternative to @TestContainers to be able to manipulate whole
// environment inside tests:
// - DockerITestExtension
//     - is responsible for the DockerEnvironment's lifecycle; extensible.
//     - injects the environment
// - DockerEnvironment -> annotation with arguments
//     - impl = class extending DockerEnvironmentSkeleton
//     - lifecycle = enum: method/class/jvm - not mandatory, jvm by default
// - DockerEnvironmentSkeleton
//      - abstract class based on current DockerEnvironment
//      - the test environment; access to containers, manipulating them.
// OR ********
// - DELETE it, transform all those managers to Dockerfiles and use @TestContainers annotation
// - but then the same container environment could not be reused by several test classes
public class DockerEnvironment implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DockerEnvironment.class);

    private static DockerEnvironment environment;

    /** Docker network */
    private final Network network;
    private final PayaraServerContainer payaraContainer;
    private final MySQLContainer<?> mySQLContainer;

    private final LocalDateTime startupTime;

    private final DockerEnvironmentConfiguration cfg;

    private final List<DockerEnvironmentAfterCloseHandler> afterCloseHandlers = new ArrayList<>();

    /**
     * Creates new singleton instance or throws ISE if it already exists.
     *
     * @param cfg configuration of the docker environment
     * @param afterCloseHandler
     * @return new instance.
     * @throws Exception
     */
    public static synchronized DockerEnvironment createEnvironment(final DockerEnvironmentConfiguration cfg,
        final DockerEnvironmentAfterCloseHandler afterCloseHandler) throws Exception {
        createEnvironment(cfg);
        environment.addAfterCloseHandler(afterCloseHandler);
        return environment;
    }


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
        LOG.info("DockerEnvironment initialized: {}", environment);
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
        this.cfg = cfg;
        this.network = Network.newNetwork();

        final List<CompletableFuture<Void>> parallelFutures = new ArrayList<>();
        // port of the docker service on the host, some tests use it.
        Testcontainers.exposeHostPorts(cfg.getPayaraServerConfiguration().getDockerHostAndPort().getPort());

        final PayaraServerDockerImageManager payaraServerMgr = //
            new PayaraServerDockerImageManager(this.network, cfg.getPayaraServerConfiguration());
        parallelFutures.add(CompletableFuture.runAsync(() -> payaraServerMgr.prepareImage(cfg.isForceNewPayaraServer())));


        final MySQLDockerImageManager mysqlMgr;
        if (cfg.isUseMySqlContainer()) {
            mysqlMgr = new MySQLDockerImageManager(this.network, cfg.getMySQLServerConfiguration());
            parallelFutures.add(CompletableFuture.runAsync(() -> mysqlMgr.prepareImage(cfg.isForceNewMySQLServer())));
        } else if (cfg.getMySQLServerConfiguration().getPort() > 0) {
            Testcontainers.exposeHostPorts(cfg.getMySQLServerConfiguration().getPort());
            mysqlMgr = null;
        } else {
            LOG.debug("MySQL docker is not configured.");
            mysqlMgr = null;
        }

        try {
            CompletableFuture.allOf(parallelFutures.toArray(new CompletableFuture[parallelFutures.size()]))
                .get(cfg.getPreparationTimeout(), TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException("Could not initialize docker environment!", e);
        }

        // STEP2: Create and start containers sequentionally (respect dependencies!)
        if (mysqlMgr == null) {
            this.mySQLContainer = null;
        } else {
            this.mySQLContainer = mysqlMgr.start();
        }
        this.payaraContainer = payaraServerMgr.start();
        logPayaraContainerStarted(this.payaraContainer);
        this.startupTime = LocalDateTime.now();
    }


    /**
     * @return configuration of the environment, which was provided in constructor.
     */
    public DockerEnvironmentConfiguration getConfiguration() {
        return this.cfg;
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
     * @param handler handler to be called as a last instruction in the {@link #close()} method.
     */
    public void addAfterCloseHandler(final DockerEnvironmentAfterCloseHandler handler) {
        this.afterCloseHandlers.add(handler);
    }


    /**
     * Resets the logging to lighter (benchmarks) or heavier (functionalities) version.
     *
     * @param newRegimeIsBenchmark
     */
    public void reconfigureLogging(final boolean newRegimeIsBenchmark) {
        final File configDir = new File(cfg.getPayaraServerConfiguration().getPayaraDomainDirectoryInDocker(), "config");
        this.payaraContainer.reconfigureLogging(
            new File(configDir, newRegimeIsBenchmark ? "logging-benchmark.properties" : "logging.properties"));
    }


    /**
     * @return initialized docker container with the Payara domain inside.
     */
    public PayaraServerContainer getPayaraContainer() {
        return this.payaraContainer;
    }


    /**
     * @return initialized docker container with the MySQL server inside.
     */
    public MySQLContainer<?> getMySqlcontainer() {
        return this.mySQLContainer;
    }


    @Override
    public void close() {
        if (!this.payaraContainer.isRunning()) {
            return;
        }
        LOG.info("Closing docker containers ...");
        closeSilently(this.payaraContainer);
        closeSilently(this.network);
        environment = null;
        this.afterCloseHandlers.stream().forEach(DockerEnvironmentAfterCloseHandler::afterClose);
    }


    private static void logPayaraContainerStarted(final PayaraServerContainer container) {
        LOG.info("\n" //
            + "========================================\n"
            + "{}(name: '{}') started, you can use this urls:\n"
            + "{}\n"
            + "{}\n"
            + "{}\n"
            + "========================================", //
            container.getClass().getSimpleName(), container.getContainerInfo().getName(), //
            container.getAdminUrl(), //
            container.getHttpUrl(), //
            container.getHttpsUrl());
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

    /**
     * Reaction to the closure of the {@link DockerEnvironment} instance. IE if the reference was
     * used, it should be reset.
     */
    @FunctionalInterface
    public interface DockerEnvironmentAfterCloseHandler {

        /**
         * Reaction to the closure of the {@link DockerEnvironment} instance.
         */
        void afterClose();
    }
}
