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
package fish.payara.test.containers.tst.tempnodes;

import fish.payara.test.containers.tools.container.AsadminCommandException;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.junit.DockerITest;
import fish.payara.test.containers.tools.junit.DockerITestExtension;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TODO should be in separate module for cluster environment.
 *
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
public class DockerNodesITest extends DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(DockerNodesITest.class);

    private static final String INSTANCE_STATUS_RUNNING = "running";
    private static final String INSTANCE_STATUS_STOPPED = "not running";

    private static Set<DockerContainerId> containersToPreserve;

    /**
     * FIXME: use simpliar manager to only unzip payara, not managing domain - it is not used.
     * @throws Exception
     */
    @BeforeAll
    public static void init() throws Exception {
        LOG.debug("init()");
        containersToPreserve = getDockerIds();
        LOG.warn("These containers existed before the test and will be preserved: \n  {}", containersToPreserve);
    }


    @AfterAll
    public static void deleteDeadContainers() throws Exception {
        final PayaraServerContainer domain = getDockerEnvironment().getPayaraContainer();
        final Set<DockerContainerId> containersAfterTests = getDockerIds();
        for (DockerContainerId id : containersAfterTests) {
            if (containersToPreserve.contains(id)) {
                LOG.info("Tolerating container already existing before this test: {}", id);
                continue;
            }
            domain.docker("DELETE", "containers/" + id.id + "?v=true&force=true&link=true", "");
        }
    }


    @Test
    public void testTemporaryDockerNodeLifeCycle() throws Exception {
        final PayaraServerContainer domain = getDockerEnvironment().getPayaraContainer();
        final String createResponse = domain.docker("containers/create", getCreateContainerJSon(domain));
        assertThat("curl create container output", createResponse, stringContainsInOrder("HTTP/1.1 201 Created"));
        final String containerId = getDockerId(createResponse);
        assertNotNull(containerId, "containerId");

        final String startResponse = domain.docker("containers/" + containerId + "/start", "");
        assertThat("curl start container output", startResponse, stringContainsInOrder("HTTP/1.1 204 No Content"));

        // FIXME: took 2 seconds but only 2 iterations on my computer. asadmin is slow (I know about unsuccessful redundant classloading, but not sure if in all cases)!!!
        final Executable listRunningInstance = getListInstanceActionToWaitFor(domain, INSTANCE_STATUS_RUNNING);
        waitFor(listRunningInstance, 60 * 1000L);

        final String listInstancesResponse = domain.asAdmin("list-instances");
        assertAll( //
            () -> assertNotNull(listInstancesResponse, "listInstancesResponse"), //
            () -> assertThat("listInstancesResponse", listInstancesResponse.split("\n"), arrayWithSize(1)) //
        );
        final String listNodesResponse = domain.asAdmin("list-nodes");
        assertAll( //
            () -> assertNotNull(listNodesResponse, "listNodesResponse"), //
            // temporary nodes are not listed!
            () -> assertThat("listNodesResponse", listNodesResponse.split("\n"), arrayWithSize(1)) //
        );

        final String dockerInstanceName = parseInstanceNameAndStatus(listInstancesResponse).get(0)[0];

        try {
            final String deleteInstanceResponse = domain.asAdmin("delete-instance", dockerInstanceName);
            fail("Expected error, but received this response: " + deleteInstanceResponse);
        } catch (final AsadminCommandException e) {
            assertThat("e.message", e.getMessage(),
                stringContainsInOrder("Instance " + dockerInstanceName + " must be stopped before it can be deleted."));
        }
        final String stopInstanceResponse = domain.asAdmin("stop-instance", dockerInstanceName);
        assertEquals("The instance, " + dockerInstanceName + ", is stopped.", stopInstanceResponse.trim(), "e.message");

        final String listInstancesResponseAfterStop = domain.asAdmin("list-instances");
        assertThat("listInstancesResponseAfterStop", listInstancesResponseAfterStop,
            stringContainsInOrder("Nothing to list."));

        final String dockerDeleteResponse = //
            domain.docker("DELETE", "containers/" + containerId + "?v=true&force=true", "");
        assertThat("dockerDeleteResponse", dockerDeleteResponse, stringContainsInOrder("HTTP/1.1 204 No Content"));

//        domain.asAdmin("create-node-docker", "--nodehost", nodeCfg.getHost(), "--dockerPasswordFile",
//            nodeCfg.getPasswordFileInDocker().getAbsolutePath(), "--dockerport", "2376", "DockerNode1");

//        domain.asAdmin("create-instance", "--autoname", "DockerTempInstance1");
//        asadmin create-node-docker --nodehost localhost --dockerPasswordFile /opt/passwordfile.txt --dockerport 2376 DockerInstance1
//        asadmin create-instance --node DockerNode1 dockerInstance1

        // FIXME: don't forget to delete created containers - now done manually
        // docker container prune; docker network prune
        // - should be also in documentation, I had 30 dead containers here, same for networks :D )
    }


    private Executable getListInstanceActionToWaitFor(final PayaraServerContainer domain, final String instanceStatus) {
        return () -> {
            final String listInstancesResponse = domain.asAdmin("list-instances");
            assertThat("listInstancesResponse", listInstancesResponse, not(stringContainsInOrder("Nothing to list.")));
            final List<String[]> nameAndStatus = parseInstanceNameAndStatus(listInstancesResponse);
            assertAll( //
                () -> assertThat("nameAndStatus", nameAndStatus, hasSize(1)),
                () -> assertThat("nameAndStatus[0][1]", nameAndStatus.get(0)[1], equalTo(instanceStatus)));
        };
    }


    /**
     * @param listInstancesResponse reponse format: "[name] [status]  \n"
     */
    private List<String[]> parseInstanceNameAndStatus(final String listInstancesResponse) {
        final Pattern pattern = Pattern.compile("[ ]+");
        return Arrays.stream(listInstancesResponse.split("\n")).map(line -> pattern.split(line.trim(), 2))
            .collect(Collectors.toList());
    }

    private void waitFor(final Executable executable, final long timeoutInMillis) throws Exception {
        final long start = System.currentTimeMillis();
        while (true) {
            try {
                executable.execute();
                return;
            } catch (final AssertionError e) {
                if (System.currentTimeMillis() - start > timeoutInMillis) {
                    throw e;
                }
                LOG.warn("Nope. Waiting ...");
                Thread.sleep(100L);
            } catch (final Throwable e) {
                fail(e);
            }
        }
    }


    private String getCreateContainerJSon(final PayaraServerContainer domain) {
        final PayaraServerContainerConfiguration mainCfg = getDockerEnvironment().getConfiguration().getPayaraServerConfiguration();
        return "{\n" //
            + "\"Image\": \"payara/server-node:" + getTestConfiguration().getPayaraServerNodeTag() + "\",\n" //
                + "\"HostConfig\": {\n" //
                + "    \"Mounts\": [\n" //
                + "      {\n" //
                + "        \"Type\": \"bind\",\n" //
                + "        \"Source\": \"" + mainCfg.getPasswordFile().getAbsolutePath() + "\",\n" //
                + "        \"Target\": \"/opt/payara/passwords/passwordfile.txt\",\n" //
                + "        \"ReadOnly\": true\n" //
                + "      }\n" //
                + "    ],\n" //
                + "    \"NetworkMode\": \"" + domain.getNetwork().getId() + "\",\n"
                + "    \"ExtraHosts\": [\n" //
                + "      \"" + mainCfg.getHost() + ":" + domain.getVirtualNetworkIpAddress() + "\"\n"
                + "    ]\n"
                + "  },\n" //
                + "\"Env\": [\n" //
                + "  \"PAYARA_DAS_HOST=" + mainCfg.getHost() + "\",\n" //
                + "  \"PAYARA_DAS_PORT=" + mainCfg.getAdminPort() + "\",\n" //
                + "  \"PAYARA_NODE_NAME=TempNode1\"\n" //
                + "]\n" //
                + "}";
    }


    private static String getDockerId(final String createContainerResponse) throws Exception {
        final String[] lines = createContainerResponse.split("\n");
        final Pattern pattern = Pattern.compile(Pattern.quote("{\"Id\":\"") + ".*");
        final String line = Arrays.stream(lines).filter(ln -> pattern.matcher(ln).matches()).findAny().orElse(null);
        try (JsonReader jr = Json.createReader(new StringReader(line))) {
            final JsonObject jo = jr.readObject();
            return jo.getString("Id");
        }
    }

    private static Set<DockerContainerId> getDockerIds() throws Exception {
        final PayaraServerContainer domain = getDockerEnvironment().getPayaraContainer();
        final String listJson = domain.docker("GET", "containers/json?all=true", "");
        final String[] lines = listJson.split("\n");
        final Pattern pattern = Pattern.compile(Pattern.quote("[{\"Id\":\"") + ".*");
        final String line = Arrays.stream(lines).filter(ln -> pattern.matcher(ln).matches()).findAny().orElse(null);
        if (line == null) {
            return new HashSet<>();
        }
        final Function<JsonObject, DockerContainerId> converter = o -> {
            LOG.debug("getDockerIds(o={})", o);
            return new DockerContainerId(o.getString("Id"), o.getJsonArray("Names").get(0).toString());
        };
        try (JsonReader jr = Json.createReader(new StringReader(line))) {
            final JsonArray jsonArray = jr.readArray();
            final Predicate<JsonObject> filter = v -> v.getString("Image").startsWith("payara/server-node:");
            return jsonArray.stream().map(JsonValue::asJsonObject).filter(filter).map(converter::apply).collect(Collectors.toSet());
        }
    }


    private static final class DockerContainerId {
        public final String id;
        public final String name;

        public DockerContainerId(final String id, final String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[id=" + id + ", name=" + name + "]";
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.id, this.name);
        }

        @Override
        public boolean equals(final Object o) {
            if (o == null || !DockerContainerId.class.isInstance(o)) {
                return false;
            }
            if (o == this) {
                return true;
            }
            final DockerContainerId another = (DockerContainerId) o;
            return Objects.equals(this.id, another.id) && Objects.equals(this.name, another.name);
        }
    }
}
