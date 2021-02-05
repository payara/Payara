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
package fish.payara.test.containers.tools.container;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ulimit;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import static java.util.Objects.requireNonNull;
import static org.testcontainers.containers.BindMode.READ_WRITE;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

/**
 * Docker image manager usable to run Java applications. Prepares image and starts the container.
 *
 * @param <T> Supported successor of the {@link GenericContainer}
 * @param <C> Supported {@link JavaContainerConfiguration} type
 * @author David Matějček
 */
public abstract class JavaDockerImageManager<T extends GenericContainer<T>, C extends JavaContainerConfiguration>
    extends DockerImageManager {

    private static final Logger LOG = LoggerFactory.getLogger(JavaDockerImageManager.class);
    // TODO see getCommand()
//    private static final String REPACKED_JAR_NAMEADDON = "";
    private static final String USERNAME = "payara";

    private final C cfg;


    /**
     * Creates the manager.
     *
     * @param network can be null
     * @param cfg mandatory
     */
    public JavaDockerImageManager(final Network network, final C cfg) {
        super(cfg.getDownloadedDockerImageName(), network);
        this.cfg = requireNonNull(cfg, "cfg");
    }


    /**
     * @return ID of the containr's logger (stdout) mapped into host's SLF4J+LOG4J
     */
    protected abstract String getLoggerId();


    /**
     * @return maven id in form groupId:artifactId
     */
    protected abstract String getRootDependencyId();


    /**
     * WARN: don't give the image directly to a container, it would be changed or deleted
     * (depends on settigs)
     *
     * @return new stopped container of the type T
     */
    protected abstract T createNewContainer();


    /**
     * @return each host and port must be reachable from the container inside.
     */
    protected abstract List<NetworkTarget> getTargetsToCheck();


    /**
     * @return list of internal ports which will be mapped to host port numbers.
     */
    protected abstract List<Integer> getExposedInternalPorts();


    /**
     * @return {@link WaitStrategy} to be used to detect that the container started successfully.
     */
    protected abstract WaitStrategy getWaitStrategy();


    /**
     * @return configuration given in constructor
     */
    protected C getConfiguration() {
        return this.cfg;
    }


    @Override
    public void prepareImage(final boolean forceNew) {
        getConfiguration().getMainApplicationDirectory().mkdirs();
        getConfiguration().getMainApplicationDirectory().setWritable(true, false);
        super.prepareImage(forceNew);
    }


    @Override
    public T start() {
        LOG.debug("Creating and starting container from image {} ...", getNameOfPreparedImage());
        try {
            final T container = createNewContainer(); //
            configureContainer(container);
            startContainer(container);
            LOG.info("Payara server container started.");
            return container;
        } catch (final Exception e) {
            throw new IllegalStateException("Could not install Payara Server Docker container!", e);
        }
    }


    /**
     * Configures the container before it will be started.
     * Network, exposed ports, time zone, locale, copyied files (see {@link #getFilesToCopy()}, and
     * finally command to be executed (see {@link #getCommand()}) method.
     *
     * @param container container to configure
     */
    protected void configureContainer(final T container) {
        container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(getLoggerId())));
        for (final Entry<String, MountableFile> file : getFilesToCopy().entrySet()) {
            container.withCopyFileToContainer(file.getValue(), file.getKey());
        }
        container.withFileSystemBind( //
            this.cfg.getMainApplicationDirectory().getAbsolutePath(),
            this.cfg.getMainApplicationDirectoryInDocker().getAbsolutePath(), READ_WRITE);
        if (this.cfg.isJaCoCoEnabled()) {
            final File jaCoCoReportDirectory = requireNonNull(getConfiguration().getJaCoCoReportDirectory(),
                "configuration.jaCoCoReportDirectory");
            if (!jaCoCoReportDirectory.isDirectory() && !jaCoCoReportDirectory.mkdirs()) {
                throw new IllegalStateException(
                    "Cannot create JaCoCo output directory needed for filesystem binding.");
            }
            container.withFileSystemBind(this.cfg.getJaCoCoReportDirectory().getAbsolutePath(),
                this.cfg.getJaCoCoReportDirectoryInDocker().getAbsolutePath(), READ_WRITE);
        }
        container.withNetwork(getNetwork()); //
        container.withExposedPorts(getExposedInternalPorts().stream().toArray(Integer[]::new)); //
        container.withEnv("TZ", "UTC").withEnv("LC_ALL", "en_US.UTF-8"); //
        container.withCreateContainerCmdModifier(cmd -> {
            // see https://github.com/zpapez/docker-java/wiki
            cmd.getHostConfig().withMemory(this.cfg.getSystemMemoryInBytes()); //
            cmd.getHostConfig().withUlimits(new Ulimit[] {new Ulimit("nofile", 4096L, 8192L)}); //
            cmd.withHostName(this.cfg.getHost());
            cmd.withUser(USERNAME);
            final HostConfig hostConfig = cmd.getHostConfig();
            hostConfig.withMemorySwappiness(0L);
//            cmd.getHostConfig().withCpuQuota(30_000L).withCpuPeriod(200_000L); // 100_000/150_000
        }); //
        container.withCommand("/bin/sh", "-c", getCommand().toString()); //
    }


    /**
     * Prepares a map of files that should be replicated into the container.
     *
     * @return map of in-container absolute paths and {@link MountableFile} instances
     */
    protected Map<String, MountableFile> getFilesToCopy() {
        final Map<String, MountableFile> files = new HashMap<>();
        addFileToCopyIfExists("server-side/fixEclipseJars.sh", "/usr/local/bin/fixEclipseJars.sh", 0777, files);
        return files;
    }


    /**
     * Searches for the hostPath with current classloader and if it is found, adds it to the files
     * as a {@link MountableFile} map under the key dockerPath.
     *
     * @param hostPath - used to find the resource
     * @param dockerPath - absolute path in docker container
     * @param mode - mode of the file in docker container
     * @param files - files to be mounted.
     */
    protected void addFileToCopyIfExists(final String hostPath, final File dockerPath, final int mode,
        final Map<String, MountableFile> files) {
        addFileToCopyIfExists(hostPath, dockerPath.getAbsolutePath(), mode, files);
    }


    /**
     * Searches for the hostPath with current classloader and if it is found, adds it to the files
     * as a {@link MountableFile} map under the key dockerPath.
     *
     * @param hostPath - used to find the resource
     * @param dockerPath - absolute path in docker container
     * @param mode - mode of the file in docker container
     * @param files - files to be mounted.
     */
    protected void addFileToCopyIfExists(final String hostPath, final String dockerPath, final int mode,
        final Map<String, MountableFile> files) {
        final MountableFile resource;
        try {
             resource = forClasspathResource(hostPath, mode);
        } catch (final IllegalArgumentException e) {
            LOG.warn("Resource not added: {}", hostPath);
            return;
        }
        files.put(dockerPath, resource);
    }


    /**
     * @return command to be executed (printing some info about network from the container point of
     *         view, fixing eclipse generated jar files, and environment.
     */
    protected StringBuilder getCommand() {
        final StringBuilder command = new StringBuilder();
        command.append("echo \"***************** Useful informations about this container *****************\"");
        command.append(" && set -x");
        command.append(" && export LANG=\"en_US.UTF-8\"").append(" && export LANGUAGE=\"en_US.UTF-8\"");
        command.append(" && (env | sort) && locale");
        command.append(" && lsb_release -a");
        command.append(" && ulimit -a");
        // TODO: move to docker image for testing
        // TODO: apply to Payara's use case
//        command.append(" && fixEclipseJars.sh ").append(this.cfg.getMainApplicationDirectoryInDocker()).append("/*")
//            .append(' ').append(REPACKED_JAR_NAMEADDON);
        for (final NetworkTarget hostAndPort : getTargetsToCheck()) {
            command.append(" && nc -v -z -w 1 ") //
                .append(hostAndPort.getHost()).append(' ').append(hostAndPort.getPort());
        }
        command.append(" && cat /etc/hosts && cat /etc/resolv.conf");
        command.append(" && hostname && netstat -r -n && netstat -ln");
        command.append(" && java -version");
        // useful to have access to the application state after the container is stopped.
        command.append(" && (rm -rf /host-shared/payara || true)");
        command.append(" && mv /opt/payara/appserver /host-shared/payara");
        command.append(" && ln -s /host-shared/payara /opt/payara/appserver");

        return command;
    }


    private void startContainer(final T container) {
        LOG.debug("startContainer(container={})", container);
        container.waitingFor(getWaitStrategy());
        container.start();
    }
}
