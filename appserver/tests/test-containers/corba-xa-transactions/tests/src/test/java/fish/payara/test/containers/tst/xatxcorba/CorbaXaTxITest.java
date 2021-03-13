/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.test.containers.tst.xatxcorba;

import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network.Ipam;
import com.github.dockerjava.api.model.Network.Ipam.Config;
import com.github.dockerjava.api.model.Ulimit;

import fish.payara.test.containers.tools.container.AsadminCommandExecutor;
import fish.payara.test.containers.tools.container.Deployer;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.container.TestablePayaraPort;
import fish.payara.test.containers.tools.properties.Properties;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import static org.jboss.shrinkwrap.api.ShrinkWrap.createFromZipFile;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author David Matejcek
 */
@Testcontainers
public class CorbaXaTxITest {

    private static final Logger LOG = LoggerFactory.getLogger(CorbaXaTxITest.class);
    private static final Logger LOG_DAS1 = LoggerFactory.getLogger("DAS1");
    private static final Logger LOG_DAS2 = LoggerFactory.getLogger("DAS2");

    private static final String HOSTNAME_DAS1 = "hostname-das1";
    private static final String HOSTNAME_DAS2 = "hostname-das2";
    private static final Integer DASC_ADMIN_PORT = 2048;
    private static final Integer DASC_HTTP_PORT = 2080;
    private static final Integer DASC_ORB_PORT = 2037;

    private static final Properties TEST_CFG = new Properties("test.properties");
    private static final PayaraServerContainerConfiguration CFG_DAS1 = new PayaraServerContainerConfiguration();
    private static final PayaraServerContainerConfiguration CFG_DAS2 = new PayaraServerContainerConfiguration();
    private static final String PAYARA_DOCKER_IMAGE_STARTED = "xxxxxxxxxxx PAYARA DOCKER IMAGE STARTED xxxxxxxxxxx";


    static {
        final File parentOfShared = TEST_CFG.getFile("docker.payara.sharedDirectory");
        final boolean newLogging = TEST_CFG.getBoolean("docker.payara.cfg.logging.newImplementation", true);
        CFG_DAS1.setMainApplicationDirectory(new File(parentOfShared, "dasA"));
        CFG_DAS1.setHost(HOSTNAME_DAS1);
        CFG_DAS1.setPayaraDirectoryName("payara");
        CFG_DAS1.setPayaraDomainName("domainA");
        CFG_DAS1.setNewLoggingImplementation(newLogging);
        CFG_DAS2.setMainApplicationDirectory(new File(parentOfShared, "dasBC"));
        CFG_DAS2.setHost(HOSTNAME_DAS2);
        CFG_DAS2.setPayaraDirectoryName("payara");
        CFG_DAS2.setPayaraDomainName("domainB");
        CFG_DAS2.setNewLoggingImplementation(newLogging);
    }

    private static final Network NET = Network.builder().createNetworkCmdModifier(cmd -> {
        final Config config = new Config().withGateway("172.234.254.1").withSubnet("172.234.254.0/16")
            .withIpRange("172.234.254.0/24");
        final Ipam ipam = new Ipam().withConfig(config);
        cmd.withIpam(ipam);
        cmd.withDriver("bridge");
    }).build();


    @Container
    private final PayaraServerContainer dasA = createContainer(CFG_DAS1, LOG_DAS1);
    @Container
    private final PayaraServerContainer dasBC = createContainer(CFG_DAS2, LOG_DAS2, DASC_ADMIN_PORT, DASC_HTTP_PORT);


    @BeforeEach
    public void deployApplication() throws Throwable {
        assertAll( //
            () -> assertTrue(dasA.isRunning(), "server is running"), //
            () -> assertTrue(dasBC.isRunning(), "server is running") //
        );
        final AsadminCommandExecutor asadminC = new AsadminCommandExecutor(dasBC, "--terse", "--user", "admin",
            "--passwordfile", CFG_DAS2.getPasswordFileInDocker().getAbsolutePath(), "--port",
            DASC_ADMIN_PORT.toString());

        dasA.asAdmin("set-config-property", "--propertyname=fish.payara.test.containers.app.xatxcorba.packagewrapper.remote.host",
            "--propertyvalue=" + HOSTNAME_DAS2);
        dasA.asAdmin("set-config-property", "--propertyname=fish.payara.test.containers.app.xatxcorba.packagewrapper.remote.port",
            "--propertyvalue=" + 3700);

        dasBC.asAdmin("set-config-property", "--propertyname=fish.payara.test.containers.app.xatxcorba.packager.remote.host",
            "--propertyvalue=" + HOSTNAME_DAS2);
        dasBC.asAdmin("set-config-property", "--propertyname=fish.payara.test.containers.app.xatxcorba.packager.remote.port",
            "--propertyvalue=" + DASC_ORB_PORT);

        asadminC.exec("create-domain", "--portbase", "2000", "domainC");
        dasBC.execInContainer("cp", getSourceLoggingProperties(CFG_DAS2),
            CFG_DAS2.getPayaraMainDirectoryInDocker().getAbsolutePath()
                + "/glassfish/domains/domainC/config/logging.properties");
        asadminC.exec("start-domain", "domainC");
        asadminC.exec("enable-secure-admin");
        asadminC.exec("stop-domain", "domainC");
        asadminC.exec("start-domain", "domainC");
        assertAll( //
            () -> assertNotNull(dasA.getMappedPort(4848), "dasA admin port"), //
            () -> assertNotNull(dasA.getMappedPort(8080), "dasA http port"), //
            () -> assertNotNull(dasBC.getMappedPort(4848), "dasB admin port"), //
            () -> assertNotNull(dasBC.getMappedPort(8080), "dasB http port"), //
            () -> assertNotNull(dasBC.getMappedPort(DASC_ADMIN_PORT), "domainC admin port"), //
            () -> assertNotNull(dasBC.getMappedPort(DASC_HTTP_PORT), "domainC http port") //
        );
        final EnterpriseArchive earA = createFromZipFile(EnterpriseArchive.class, TEST_CFG.getFile("test.earA"));
        LOG.info(earA.toString(true));
        dasA.deploy("/", earA);
        final EnterpriseArchive earB = createFromZipFile(EnterpriseArchive.class, TEST_CFG.getFile("test.earB"));
        LOG.info(earB.toString(true));
        dasBC.deploy("/", earB);

        final EnterpriseArchive earC = createFromZipFile(EnterpriseArchive.class, TEST_CFG.getFile("test.earC"));
        LOG.info(earC.toString(true));
        final Deployer deployer = new Deployer(dasBC.getContainerIpAddress(), dasBC.getMappedPort(DASC_ADMIN_PORT));
        deployer.deploy("/", earC);
    }


    @AfterEach
    public void log() throws Throwable {
//        final ExecResult resultA = dasBC.execInContainer(StandardCharsets.UTF_8, "ls -la /proc/*/fd");
//        LOG.info("dasBC notify: \n {}", resultA.getStdout());
//        Thread.sleep(600000L);
//        final ExecResult resultA = dasA.execInContainer(StandardCharsets.UTF_8, "cat",
//            dasA.getConfiguration().getPayaraServerLogInDocker().getAbsolutePath());
//        LOG.info("domainA log: \n {}", resultA.getStdout());
//        final ExecResult resultB = dasBC.execInContainer(StandardCharsets.UTF_8, "cat",
//            dasBC.getConfiguration().getPayaraServerLogInDocker().getAbsolutePath());
//        LOG.info("domainB log: \n {}", resultB.getStdout());
        final ExecResult resultC = dasBC.execInContainer(StandardCharsets.UTF_8, "tail", "-n", "20",
            CFG_DAS2.getPayaraMainDirectoryInDocker().getAbsolutePath() + "/glassfish/domains/domainC/logs/server.log");
        LOG.info("domainC log: \n {}", resultC.getStdout());
    }


    @Test
    public void test() throws Throwable {
        final WebTarget target = dasA.getAnonymousBasicWebTarget();
        final Response response1 = target.path("/cross-service/packager/1").request().post(Entity.text(""));
        assertEquals(204, response1.getStatus(), "Servlet response status code");
    }


    private static PayaraServerContainer createContainer(final PayaraServerContainerConfiguration cfg,
        final Logger logger, final int... additionalExposedPorts) {
        cfg.getMainApplicationDirectory().mkdirs();
        final PayaraServerContainer container = new PayaraServerContainer(
            TEST_CFG.getString("docker.payara.image.base"), cfg);
        container.withFileSystemBind( //
                cfg.getMainApplicationDirectory().getAbsolutePath(),
                cfg.getMainApplicationDirectoryInDocker().getAbsolutePath(), BindMode.READ_WRITE);
        container.withCopyFileToContainer(
            MountableFile.forClasspathResource("server-side/logging-old.properties", 0777), "/logging-old.properties");
        container.withCopyFileToContainer(
            MountableFile.forClasspathResource("server-side/logging-new.properties", 0777), "/logging-new.properties");
        container //
        .withNetwork(NET).withNetworkMode("bridge").withNetworkAliases(cfg.getHost()) //
        .withExposedPorts(TestablePayaraPort.getFullServerPortValues()) //
        .withCreateContainerCmdModifier(cmd -> {
            cmd.getHostConfig().withMemory(cfg.getSystemMemoryInBytes()); //
            cmd.getHostConfig().withUlimits(new Ulimit[] {new Ulimit("nofile", 4096L, 8192L)}); //
            cmd.withUser("payara");
            cmd.withHostName(cfg.getHost());
            final HostConfig hostConfig = cmd.getHostConfig();
            hostConfig.withMemorySwappiness(0L);
        })
        .withLogConsumer(new Slf4jLogConsumer(logger))
        .waitingFor(Wait.forLogMessage(".*xxxxxxxxxxx PAYARA DOCKER IMAGE STARTED xxxxxxxxxxx.*", 1)
            .withStartupTimeout(Duration.ofSeconds(60L)));

        if (additionalExposedPorts != null) {
            container.addExposedPorts(additionalExposedPorts);
        }
        container.withCommand("/bin/sh", "-c", getCommand(cfg).toString()); //
        return container;
    }

    /**
     * @return command to be executed (printing some info about network from the container point of
     *         view, fixing eclipse generated jar files, and environment.
     */
    private static StringBuilder getCommand(final PayaraServerContainerConfiguration cfg) {
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
//        for (final NetworkTarget hostAndPort : getTargetsToCheck()) {
//            command.append(" && nc -v -z -w 1 ") //
//                .append(hostAndPort.getHost()).append(' ').append(hostAndPort.getPort());
//        }
        command.append(" && cat /etc/hosts && cat /etc/resolv.conf && hostname && netstat -r -n && netstat -ln");
        command.append(" && java -version");
        // useful to have access to the application state after the container is stopped.
        command.append(" && pwd");
        command.append(" && (rm -rf /host-shared/payara || true)");
        command.append(" && mv /opt/payara/appserver /host-shared/payara");
        command.append(" && ln -s /host-shared/payara /opt/payara/appserver");
        command.append(" && ln -s ").append(" /opt/payara/passwordFile ")
            .append(cfg.getPasswordFileInDocker().getAbsolutePath());

        command.append(" && ls -la ").append(cfg.getMainApplicationDirectoryInDocker());
        command.append(" && ls -la ").append(cfg.getPayaraDomainDirectoryInDocker().getParentFile());
        if (cfg.isJaCoCoEnabled()) {
            // FIXME: apply to payara's use case, probably move to lib or domain directory!
            command.append(" && unzip -o ").append(cfg.getMainApplicationDirectoryInDocker())
                .append("/org.jacoco.agent-").append(cfg.getJaCoCoVersion()).append(".jar")
                .append(" \"jacocoagent.jar\" -d ").append(cfg.getMainApplicationDirectoryInDocker()); //
        }

        final File asadmin = cfg.getAsadminFileInDocker();
        command.append(" && ").append(asadmin).append(" --user admin --passwordfile ")
            .append("/opt/payara/passwordFile").append(" create-domain ").append(cfg.getPayaraDomainName());
        command.append(" && cp ").append(getSourceLoggingProperties(cfg)) //
        .append(' ').append(cfg.getPayaraLoggingPropertiesInDocker());
        command.append(" && ").append(asadmin).append(" start-domain ").append(cfg.getPayaraDomainName());
        command.append(" && ").append(asadmin).append(" --user admin --passwordfile ")
            .append("/opt/payara/passwordFile").append(" enable-secure-admin");
        command.append(" && ").append(asadmin).append(" restart-domain ").append(cfg.getPayaraDomainName());
        command.append(" && ls -la ").append(new File(cfg.getPayaraDomainDirectoryInDocker(), "config"));
        command.append(" && echo '" + PAYARA_DOCKER_IMAGE_STARTED + "'");
        command.append(" && tail -F ").append(cfg.getPayaraServerLogInDocker()); //

        return command;
    }


    private static String getSourceLoggingProperties(final PayaraServerContainerConfiguration cfg) {
        return "/logging-" + (cfg.isNewLoggingImplementation() ? "new" : "old") + ".properties";
    }
}
