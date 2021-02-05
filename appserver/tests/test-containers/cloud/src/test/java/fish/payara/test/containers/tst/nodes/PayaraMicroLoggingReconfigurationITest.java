/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above. However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.test.containers.tst.nodes;

import com.github.dockerjava.api.model.Network.Ipam;
import com.github.dockerjava.api.model.Network.Ipam.Config;
import com.sun.enterprise.server.logging.ODLLogFormatter;

import fish.payara.test.containers.tools.container.DasCfg;
import fish.payara.test.containers.tools.container.PayaraMicroContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.TestablePayaraPort;
import fish.payara.test.containers.tools.junit.WaitForExecutable;
import fish.payara.test.containers.tools.log4j.EventCollectorAppender;
import fish.payara.test.containers.tools.properties.Properties;
import fish.payara.test.containers.tools.rs.RestClientCache;
import fish.payara.test.containers.tst.log.war.LoggingRestEndpoint;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.spi.LoggingEvent;
import org.hamcrest.core.StringContains;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TODO: Split to some appropriate tests using the knowledge gained here.
 *       Reason: Micro does not support set-log-x commands.
 *
 * @author David Matejcek
 */
@Testcontainers
@Disabled("Command is not supported on micro these days")
public class PayaraMicroLoggingReconfigurationITest {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraMicroLoggingReconfigurationITest.class);
    private static final Logger LOG_DAS = LoggerFactory.getLogger("DAS");
    private static final Logger LOG_MICRO = LoggerFactory.getLogger("MIC");

    private static final String WEBAPP_NAME = PayaraMicroLoggingReconfigurationITest.class.getSimpleName() + "WebApp";
    private static final Properties TEST_CFG = new Properties("test.properties");
    private static final DasCfg CFG_DAS = new DasCfg("hostname-das", TEST_CFG.getString("docker.payara.tag"));
    private static final DasCfg CFG_MIC = new DasCfg("hostname-micro", TEST_CFG.getString("docker.payara.tag"));
    private static final Class<LoggingRestEndpoint> APP_CLASS = LoggingRestEndpoint.class;

    // FIXME: filtering messages
    private static final Set<String> MESSAGES_FOR_FILTER = Arrays
        .stream(new String[] {"STDERR:   akjaskjf" //
        }).collect(Collectors.toSet());

    private static final Network NET = Network.builder().createNetworkCmdModifier(cmd -> {
        final Config config = new Config().withGateway("172.234.254.1").withSubnet("172.234.254.0/16")
            .withIpRange("172.234.254.0/24");
        final Ipam ipam = new Ipam().withConfig(config);
        cmd.withIpam(ipam);
        cmd.withDriver("bridge");
    }).build();
    private static final RestClientCache RS_CLIENTS = new RestClientCache();

    private static File warFileOnHost;
    private static File warFileInMicro;

    @Container
    private final PayaraServerContainer das = new PayaraServerContainer(CFG_DAS) //
            .withNetwork(NET).withNetworkMode("bridge").withNetworkAliases(CFG_DAS.getHost()) //
            .withExposedPorts(TestablePayaraPort.getFullServerPortValues()) //
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withHostName(CFG_DAS.getHost());
            })
            .withLogConsumer(new Slf4jLogConsumer(LOG_DAS))
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30L)));

    @Container
    private final PayaraMicroContainer micro = new PayaraMicroContainer(CFG_MIC) //
        .withNetwork(NET).withNetworkMode("bridge").withNetworkAliases(CFG_MIC.getHost()) //
        .withExposedPorts(TestablePayaraPort.getMicroPortValues()).withLogConsumer(new Slf4jLogConsumer(LOG_MICRO))
        .withFileSystemBind(warFileOnHost.getAbsolutePath(), warFileInMicro.getAbsolutePath())
        .withCommand( //
            "--clustermode", "dns:" + CFG_DAS.getHost() + ":4900," + CFG_MIC.getHost() + ":6900",
            "--hzPublicAddress", CFG_MIC.getHost(),
            "--name", "MicroTest", "--deploy", warFileInMicro.getAbsolutePath(), "--contextRoot", "/logging")
        .waitingFor(Wait.forLogMessage(".*Payara Micro.+ ready.+\\n", 1).withStartupTimeout(Duration.ofSeconds(30L)));

    private EventCollectorAppender microLog;

    @BeforeAll
    public static void createApplication() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class) //
            .addPackages(true, APP_CLASS.getPackage()) //
            .addAsWebResource(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
        ;
        LOG.info(war.toString(true));
        warFileOnHost = new File(TEST_CFG.getFile("build.directory"), WEBAPP_NAME + ".war");
        war.as(ZipExporter.class).exportTo(warFileOnHost, true);
        warFileInMicro = new File("/opt/payara/", warFileOnHost.getName());
    }


    @BeforeEach
    public void addLogCollector() {
        final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("MIC");
        assertNotNull(logger, "MIC logger was not found");
        final Predicate<LoggingEvent> filter = event -> {
            final Predicate<? super String> predicate = msgPart -> {
                final String message = event.getMessage().toString();
                return message.contains(msgPart);
            };
            return MESSAGES_FOR_FILTER.stream().anyMatch(predicate);
        };
        microLog = new EventCollectorAppender().withCapacity(20).withEventFilter(filter);
        logger.addAppender(microLog);
    }


    @AfterEach
    public void resetLogCollector() {
        assertThat("log collector size", microLog.getSize(), equalTo(0));
        microLog.clearCache();
    }


    @AfterAll
    public static void close() {
        RS_CLIENTS.close();
        NET.close();
    }


    @Test
    public void testSetLogLevels() throws Throwable {
        das.asAdmin("set-hazelcast-configuration", "--clustermode", "dns", "--dnsmembers",
            CFG_DAS.getHost() + ":4900," + CFG_MIC.getHost() + ":6900");
        // FIXME: if there are other tests running before this, das has also another IP (why?)
        WaitForExecutable.waitFor(
            () -> assertThat(das.asAdmin("list-hazelcast-cluster-members"), StringContains.containsString(WEBAPP_NAME)),
            15000L);

        final String microExplicitTarget = getExplicitTarget();

        callServlet();
        // TODO replace with waiting for log
        Thread.sleep(100L);
        // TODO: check log

        final String setLogLevelResult = das.asAdmin("send-asadmin-command", "--explicitTarget=" + microExplicitTarget, //
            "--logOutput=true",
            "--command=set-log-levels", LoggingRestEndpoint.class.getName() + "=FINE");
        assertEquals("Command executed successfully", setLogLevelResult);

        callServlet();
        // TODO: check log

        final String setLogAttrResult = das.asAdmin("send-asadmin-command", "--explicitTarget=" + microExplicitTarget,
            "--command=set-log-attributes", "handlers=java.util.logging.ConsoleHandler"
                + ":java.util.logging.ConsoleHandler.formatter=" + ODLLogFormatter.class.getName()
                + ":" + ODLLogFormatter.class.getName() + ".ansiColor=true");
        assertEquals("Command executed successfully", setLogAttrResult);
        callServlet();
        // TODO: check log
    }


    private void callServlet() throws Exception {
        final Client client = RS_CLIENTS.getNonPreemptiveClient(true, null, null);
        final WebTarget target = client.target(micro.getHttpUrl().toURI()).path("logging").path("sample");
        final Response response1 = target.request().post(Entity.entity("", MediaType.TEXT_PLAIN_TYPE));
        assertEquals(204, response1.getStatus(), "Servlet response status code");
    }


    private String getExplicitTarget() throws Throwable {
        final String microClusterMemberResponse = das.asAdmin("list-hazelcast-cluster-members", "--type", "micro");
        assertThat(microClusterMemberResponse, StringContains.containsString(WEBAPP_NAME));
        final String[] lines = microClusterMemberResponse.split("\n");
        assertEquals(2, lines.length, "list-hazelcast-cluster-members - lines: " + Arrays.toString(lines));
        final String[] columns = lines[lines.length - 1].split("[ ]+");
        assertEquals(12, columns.length, "list-hazelcast-cluster-members - columns: " + Arrays.toString(columns));
        // column 4, skip '/' prefix
        final String microIpAddress = columns[3].substring(1);
        return microIpAddress + ":6900:MicroTest";
    }
}
