/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2022-2025 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
package fish.payara;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.testng.Assert.assertEquals;

public class JCacheRestTest {

    private final GenericContainer<?>[] nodes = new GenericContainer<?>[3];
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private Network network;

    @BeforeClass
    public void setUp() throws Exception {
        network = Network.newNetwork();
        DockerImageName payaraImg = DockerImageName.parse("payara/micro:6.2025.8");

        for (int instanceIndex = 0; instanceIndex < 3; instanceIndex++) {
            nodes[instanceIndex] = new GenericContainer<>(payaraImg)
                    .withNetwork(network)
                    .withNetworkAliases("node" + instanceIndex)
                    .withExposedPorts(8080)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource("jcache-rest.war"),
                            "/opt/payara/deployments/jcache-rest.war" // just copy the WAR here
                    )
                    .withReuse(true);

            nodes[instanceIndex].start();
        }

        // wait for nodes to start
        Thread.sleep(5000);
    }

    @Test
    public void testJCacheCluster() throws Exception {
        System.out.println("STARTING JCACHE CLUSTER TEST...");
        System.out.println("Initializing: Waiting for cluster to stabilize...");
        Thread.sleep(5000);

        // Test 1: Store on node 1 and verify on all nodes
        System.out.println("TEST 1: Single Node Write");
        String key1 = "foo";
        String value1 = "bar";

        System.out.println("Storing: " + key1 + " = " + value1 + " on node 1");
        put(nodes[0], key1, value1);

        System.out.println("Waiting: 5 seconds for replication...");
        Thread.sleep(5000);

        // Verify on all nodes
        System.out.println("Verifying: key '" + key1 + "' on all nodes");
        for (int instanceIndex = 0; instanceIndex < nodes.length; instanceIndex++) {
            String nodeName = String.format("Node %d", instanceIndex + 1);
            String actualValue = get(nodes[instanceIndex], key1);
            boolean success = value1.equals(actualValue);
            System.out.println("  " + (success ? "✓" : "✗") + " Verified: " + key1 + " = " + actualValue + " (expected: " + value1 + ")");
            assertEquals(actualValue, value1,
                    String.format("Value mismatch for key '%s' on %s", key1, nodeName));
        }

        // Test 2: Store on node 2 and verify on all nodes
        System.out.println("TEST 2: Second Node Write");
        String key2 = "baz";
        String value2 = "qux";

        System.out.println("Storing: " + key2 + " = " + value2 + " on node 2");
        put(nodes[1], key2, value2);

        System.out.println("Waiting: 5 seconds for replication...");
        Thread.sleep(5000);

        // Verify on all nodes
        System.out.println("Verifying: key '" + key2 + "' on all nodes");
        for (int instanceIndex = 0; instanceIndex < nodes.length; instanceIndex++) {
            String nodeName = String.format("Node %d", instanceIndex + 1);
            String actualValue = get(nodes[instanceIndex], key2);
            boolean success = value2.equals(actualValue);
            System.out.println("  " + (success ? "✓" : "✗") + " Verified: " + key2 + " = " + actualValue + " (expected: " + value2 + ")");
            assertEquals(actualValue, value2,
                    String.format("Value mismatch for key '%s' on %s", key2, nodeName));
        }

        // Test 3: Store on node 3 and verify on all nodes
        System.out.println("TEST 3: Third Node Write");
        String key3 = "hello";
        String value3 = "world";

        System.out.println("Storing: " + key3 + " = " + value3 + " on node 3");
        put(nodes[2], key3, value3);

        System.out.println("Waiting: 5 seconds for replication...");
        Thread.sleep(5000);

        // Verify on all nodes
        System.out.println("Verifying: key '" + key3 + "' on all nodes");
        for (int instanceIndex = 0; instanceIndex < nodes.length; instanceIndex++) {
            String nodeName = String.format("Node %d", instanceIndex + 1);
            String actualValue = get(nodes[instanceIndex], key3);
            boolean success = value3.equals(actualValue);
            System.out.println("  " + (success ? "✓" : "✗") + " Verified: " + key3 + " = " + actualValue + " (expected: " + value3 + ")");
            assertEquals(actualValue, value3,
                    String.format("Value mismatch for key '%s' on %s", key3, nodeName));
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        System.out.println("STOPPING DOWN JCACHE CLUSTER TEST...");

        for (var node : nodes) {
            if (node != null && node.isRunning()) {
                node.close();
            }

            if (network != null) {
                network.close();
            }
        }
    }

    private String baseUrl(GenericContainer<?> node) {
        return "http://" + node.getHost() + ":" + node.getMappedPort(8080);
    }

    private void put(GenericContainer<?> node, String key, String value) throws Exception {
        String url = baseUrl(node) + "/jcache-rest/webresources/cache?key=" + key;
        System.out.println("\n[PUT] Node: " + node.getContainerInfo().getName() +
                " | URL: " + url +
                " | Key: " + key +
                " | Value: " + value);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString("\"" + value + "\""))
                .build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        System.out.println("[PUT] Status: " + response.statusCode());
    }

    private String get(GenericContainer<?> node, String key) throws Exception {
        String url = baseUrl(node) + "/jcache-rest/webresources/cache?key=" + key;
        System.out.println("\n[GET] Node: " + node.getContainerInfo().getName() +
                " | URL: " + url +
                " | Key: " + key);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // Parse the JSON string to remove the extra quotes
        String result = responseBody;
        if (responseBody != null && responseBody.startsWith("\"") && responseBody.endsWith("\"")) {
            result = responseBody.substring(1, responseBody.length() - 1);
        }

        System.out.println("[GET] Status: " + response.statusCode() +
                " | Raw response: " + responseBody +
                " | Parsed value: " + result);
        return result;
    }
}
