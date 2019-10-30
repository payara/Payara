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

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.junit.DockerITest;
import fish.payara.test.containers.tools.junit.DockerITestExtension;

import java.io.StringReader;
import java.util.Arrays;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TODO should be in separate module for cluster environment.
 *
 * @author David Matejcek
 */
@ExtendWith(DockerITestExtension.class)
public class TempNodesITest extends DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(TempNodesITest.class);

//    private static PayaraServerContainer domain;
//    private static PayaraServerContainer node;
//    private static PayaraServerContainerConfiguration nodeCfg;


    /**
     * FIXME: use simpliar manager to only unzip payara, not managing domain - it is not used.
     * @throws Exception
     */
    @BeforeAll
    public static void initComputers() throws Exception {
        LOG.debug("initComputers()");
//        final DockerEnvironment environment = getDockerEnvironment();
//        domain = environment.getPayaraContainer();
//        final Network network = environment.getNetwork();
//        final PayaraServerContainerConfiguration mainCfg = environment.getConfiguration().getPayaraServerConfiguration();
//        nodeCfg = new PayaraServerContainerConfiguration();
//        nodeCfg.setAdminPort(mainCfg.getAdminPort());
//        nodeCfg.setDownloadedDockerImageName(mainCfg.getDownloadedDockerImageName());
//        nodeCfg.setJdkPackageId(mainCfg.getJdkPackageId());
//        nodeCfg.setHost("payara-test-node1");
//        nodeCfg.setHttpPort(mainCfg.getHttpPort());
//        nodeCfg.setHttpsPort(mainCfg.getHttpsPort());
//        final File payaraZipFile = mainCfg.getPayaraZipFile();
//        final File sharedNodeDir = new File(mainCfg.getMainApplicationDirectory().getParentFile(), "shared-node");
//        sharedNodeDir.mkdirs();
//        Files.copy(payaraZipFile.toPath(), new File(sharedNodeDir, payaraZipFile.getName()).toPath(),
//            StandardCopyOption.REPLACE_EXISTING);
//        nodeCfg.setMainApplicationDirectory(sharedNodeDir);
//        nodeCfg.setPomFile(mainCfg.getPomFile());
//        nodeCfg.setPreparationTimeout(mainCfg.getPreparationTimeout());
//        nodeCfg.setSystemMemory(mainCfg.getSystemMemory());
//        nodeCfg.setTestOutputDirectory(mainCfg.getTestOutputDirectory());
//        nodeCfg.setXms(mainCfg.getXms());
//        nodeCfg.setXmx(mainCfg.getXmx());
//        nodeCfg.setXss(mainCfg.getXss());
//
//        final PayaraServerDockerImageManager manager = new PayaraServerDockerImageManager(network, nodeCfg);
//        manager.prepareImage(false);
//        node = manager.start();
//        node.asLocalAdmin("stop-domain");
    }


    @Test
    public void testTemporaryDockerNodes() throws Exception {
        // 1) create docker node
        // 2) create instance on docker node
        // 3) delete instance on docker node
        // 4) verify that nothing left
        final PayaraServerContainer domain = getDockerEnvironment().getPayaraContainer();
        final String createResponse = domain.docker("containers/create", getCreateContainerJSon(domain));
        assertThat("curl create container output", createResponse, stringContainsInOrder("HTTP/1.1 201 Created"));
        final String containerId = getDockerId(createResponse);
        assertNotNull(containerId, "containerId");

        final String startResponse = domain.docker("containers/" + containerId + "/start", "");
        assertThat("curl start container output", startResponse, stringContainsInOrder("HTTP/1.1 204 No Content"));

        // FIXME: only simpliest request in cycle, the rest after that.
        // note: took 2 seconds but only 2 iterations on my computer. asadmin is slow (I know about unsuccessful redundant classloading, but not sure if in all cases)!!!
        final long start = System.currentTimeMillis();
        while (true) {
            try {
                assertAll( //
                    () -> {
                        final String listNodesResponse = domain.asAdmin("list-nodes");
                        assertAll( //
                            () -> assertNotNull(listNodesResponse, "listNodesResponse"), //
                            () -> assertThat("listNodesResponse", listNodesResponse.split("\n"), arrayWithSize(2)) //
                        );
                    }, //
                    () -> {
                        final String listInstancesResponse = domain.asAdmin("list-instances");
                        assertAll( //
                            () -> assertNotNull(listInstancesResponse, "listInstancesResponse"), //
                            () -> assertThat("listInstancesResponse", listInstancesResponse.split("\n"),
                                arrayWithSize(1)) //
                        );
                    });
                break;
            } catch (final AssertionError e) {
                if (System.currentTimeMillis() - start > 10000L) {
                    throw e;
                }
                Thread.sleep(100L);
            }
        }

        // FIXME: I need the right name of the instance. Reliably. To be fixed after using GenericContainer for the temp instance.
//        domain.asAdmin("delete-instance", dockerInstance1);

//        domain.asAdmin("create-node-docker", "--nodehost", nodeCfg.getHost(), "--dockerPasswordFile",
//            nodeCfg.getPasswordFileInDocker().getAbsolutePath(), "--dockerport", "2376", "DockerNode1");

//        domain.asAdmin("create-instance", "--autoname", "DockerTempInstance1");
//        asadmin create-node-docker --nodehost localhost --dockerPasswordFile /opt/passwordfile.txt --dockerport 2376 DockerInstance1
//        asadmin create-instance --node DockerNode1 dockerInstance1

        // FIXME: don't forget to delete created containers - now done manually
        // docker container prune; docker network prune
        // - should be also in documentation, I had 30 dead containers here, same for networks :D )
    }


    private String getCreateContainerJSon(final PayaraServerContainer domain) {
        final PayaraServerContainerConfiguration mainCfg = getDockerEnvironment().getConfiguration().getPayaraServerConfiguration();
        return "{\n" //
                + "\"Image\": \"payara/server-node:latest\",\n" //
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


    private String getDockerId(final String createContainerResponse) throws Exception {
        final String[] lines = createContainerResponse.split("\n");
        final Pattern pattern = Pattern.compile(Pattern.quote("{\"Id\":\"") + ".*");

        final String line = Arrays.stream(lines).filter(ln -> pattern.matcher(ln).matches()).findAny().orElse(null);
        try (JsonReader jr = Json.createReader(new StringReader(line))) {
            final JsonObject jo = jr.readObject();
            return jo.getString("Id");
        }
    }
}
