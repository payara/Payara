/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.docker.instance;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.SshConnector;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import fish.payara.docker.DockerConstants;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;

public class JsonRequestConstructorTest {

    @Test
    public void testNoExtraProperties() {
        Node node = new NodeStub();
        Server server = new ServerStub();

        JsonObject actual = JsonRequestConstructor.constructJsonRequest(
                new Properties(), node, server, "localhost", "4848");

        JsonObject expected = Json.createObjectBuilder()
                .add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage())
                .add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                        .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                        .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                        .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                        .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                        .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"))
                .add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                        .add(DockerConstants.PAYARA_DAS_HOST + "=" + "localhost")
                        .add(DockerConstants.PAYARA_DAS_PORT + "=" + "4848")
                        .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                        .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                        .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()))
                .build();

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testHostConfig() {
        Node node = new NodeStub();
        Server server = new ServerStub();
        Properties properties = new Properties();
        properties.put("HostConfig.Wibble", "Wobble");

        JsonObject actual = JsonRequestConstructor.constructJsonRequest(
                properties, node, server, "localhost", "4848");

        JsonObject expected = Json.createObjectBuilder()
                .add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage())
                .add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                        .add("Wibble", "Wobble")
                        .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                        .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                        .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                        .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                        .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"))
                .add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                        .add(DockerConstants.PAYARA_DAS_HOST + "=" + "localhost")
                        .add(DockerConstants.PAYARA_DAS_PORT + "=" + "4848")
                        .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                        .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                        .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()))
                .build();

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testEnvConfig() {
        Node node = new NodeStub();
        Server server = new ServerStub();
        Properties properties = new Properties();
        properties.put("Env", "[Wibble:Wobble|Humpty:Dumpty]");

        JsonObject actual = JsonRequestConstructor.constructJsonRequest(
                properties, node, server, "localhost", "4848");

        JsonObject expected = Json.createObjectBuilder()
                .add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage())
                .add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                        .add("Wibble=Wobble")
                        .add("Humpty=Dumpty")
                        .add(DockerConstants.PAYARA_DAS_HOST + "=" + "localhost")
                        .add(DockerConstants.PAYARA_DAS_PORT + "=" + "4848")
                        .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                        .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                        .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()))
                .add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                        .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                        .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                        .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                        .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                        .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"))
                .build();

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testNestedProperties() {
        Node node = new NodeStub();
        Server server = new ServerStub();
        Properties properties = new Properties();
        properties.put("Wibbly.Wobbly.Timey.Wimey", "Testy");
        properties.put("Wibbly.Wobbly.Timey.Whiney", "Westy");
        properties.put("Wibbly.Wobbly.Bobbly", "[Nibbly:Nubbly,Bibbly:Bubbly|Tibbly:Tobbly,Giggly:Goggly]");
        properties.put("Wibbly.Wubbly", "[Bibbly|Bobbly]");
        properties.put("Lovely", "Jabbly");

        JsonObject actual = JsonRequestConstructor.constructJsonRequest(
                properties, node, server, "localhost", "4848");

        JsonObject expected = Json.createObjectBuilder()
                .add(DockerConstants.DOCKER_IMAGE_KEY, node.getDockerImage())
                .add("Wibbly", Json.createObjectBuilder()
                        .add("Wobbly", Json.createObjectBuilder()
                                .add("Bobbly", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("Nibbly", "Nubbly")
                                                .add("Bibbly", "Bubbly"))
                                        .add(Json.createObjectBuilder()
                                                .add("Tibbly", "Tobbly")
                                                .add("Giggly", "Goggly")))
                                .add("Timey", Json.createObjectBuilder()
                                        .add("Whiney", "Westy")
                                        .add("Wimey", "Testy")))
                        .add("Wubbly", Json.createArrayBuilder()
                                .add("Bibbly")
                                .add("Bobbly")))
                .add("Lovely", "Jabbly")
                .add(DockerConstants.DOCKER_HOST_CONFIG_KEY, Json.createObjectBuilder()
                        .add(DockerConstants.DOCKER_MOUNTS_KEY, Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add(DockerConstants.DOCKER_MOUNTS_TYPE_KEY, "bind")
                                        .add(DockerConstants.DOCKER_MOUNTS_SOURCE_KEY, node.getDockerPasswordFile())
                                        .add(DockerConstants.DOCKER_MOUNTS_TARGET_KEY, DockerConstants.PAYARA_PASSWORD_FILE)
                                        .add(DockerConstants.DOCKER_MOUNTS_READONLY_KEY, true)))
                        .add(DockerConstants.DOCKER_NETWORK_MODE_KEY, "host"))
                .add(DockerConstants.DOCKER_CONTAINER_ENV, Json.createArrayBuilder()
                        .add(DockerConstants.PAYARA_DAS_HOST + "=" + "localhost")
                        .add(DockerConstants.PAYARA_DAS_PORT + "=" + "4848")
                        .add(DockerConstants.PAYARA_NODE_NAME + "=" + node.getName())
                        .add(DockerConstants.PAYARA_CONFIG_NAME + "=" + server.getConfigRef())
                        .add(DockerConstants.PAYARA_INSTANCE_NAME + "=" + server.getName()))
                .build();

        Assert.assertEquals(expected.toString(), actual.toString());
    }

    class NodeStub implements Node {

        @Override
        public void setName(String value) throws PropertyVetoException {

        }

        @Override
        public String getName() {
            return "Docky1";
        }

        @Override
        public String getNodeDir() {
            return null;
        }

        @Override
        public void setNodeDir(String value) throws PropertyVetoException {

        }

        @Override
        public String getNodeHost() {
            return null;
        }

        @Override
        public void setNodeHost(String value) throws PropertyVetoException {

        }

        @Override
        public String getInstallDir() {
            return null;
        }

        @Override
        public void setInstallDir(String value) throws PropertyVetoException {

        }

        @Override
        public String getType() {
            return null;
        }

        @Override
        public void setType(String value) throws PropertyVetoException {

        }

        @Override
        public String getWindowsDomain() {
            return null;
        }

        @Override
        public void setWindowsDomain(String value) throws PropertyVetoException {

        }

        @Override
        public String getFreeze() {
            return null;
        }

        @Override
        public void setFreeze(String value) throws PropertyVetoException {

        }

        @Override
        public SshConnector getSshConnector() {
            return null;
        }

        @Override
        public void setSshConnector(SshConnector connector) {

        }

        @Override
        public String getDockerPasswordFile() {
            return "/opt/payara/testing/passwordfile.txt";
        }

        @Override
        public void setDockerPasswordFile(String dockerPasswordFile) {

        }

        @Override
        public String getDockerImage() {
            return "payaraserver-node";
        }

        @Override
        public void setDockerImage(String dockerImage) {

        }

        @Override
        public String getDockerPort() {
            return null;
        }

        @Override
        public void setDockerPort(String dockerPort) {

        }

        @Override
        public String getUseTls() {
            return null;
        }

        @Override
        public void setUseTls(String value) {

        }

        @Override
        public String getInstallDirUnixStyle() {
            return null;
        }

        @Override
        public String getNodeDirUnixStyle() {
            return null;
        }

        @Override
        public String getNodeDirAbsolute() {
            return null;
        }

        @Override
        public String getNodeDirAbsoluteUnixStyle() {
            return null;
        }

        @Override
        public boolean nodeInUse() {
            return false;
        }

        @Override
        public boolean isDefaultLocalNode() {
            return false;
        }

        @Override
        public boolean isLocal() {
            return false;
        }

        @Override
        public boolean instanceCreationAllowed() {
            return false;
        }

        @Override
        public List<ResourceRef> getResourceRef() {
            return null;
        }

        @Override
        public List<ApplicationRef> getApplicationRef() {
            return null;
        }

        @Override
        public String getReference() {
            return null;
        }

        @Override
        public boolean isCluster() {
            return false;
        }

        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public boolean isInstance() {
            return false;
        }

        @Override
        public boolean isDas() {
            return false;
        }

        @Override
        public boolean isDeploymentGroup() {
            return false;
        }

        @Override
        public ConfigBeanProxy getParent() {
            return null;
        }

        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            return null;
        }

        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
            return null;
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) throws TransactionFailure {
            return null;
        }
    }

    class ServerStub implements Server {

        @Override
        public void setName(String value) throws PropertyVetoException {

        }

        @Override
        public String getName() {
            return "Insty1";
        }

        @Override
        public String getConfigRef() {
            return "Insty1-config";
        }

        @Override
        public void setConfigRef(String value) throws PropertyVetoException {

        }

        @Override
        public String getNodeAgentRef() {
            return null;
        }

        @Override
        public void setNodeAgentRef(String value) throws PropertyVetoException {

        }

        @Override
        public void setNodeRef(String value) throws PropertyVetoException {

        }

        @Override
        public String getNodeRef() {
            return null;
        }

        @Override
        public String getLbWeight() {
            return null;
        }

        @Override
        public void setLbWeight(String value) throws PropertyVetoException {

        }

        @Override
        public String getDockerContainerId() {
            return null;
        }

        @Override
        public void setDockerContainerId(String dockerContainerId) throws PropertyVetoException {

        }

        @Override
        public List<SystemProperty> getSystemProperty() {
            return null;
        }

        @Override
        public SystemProperty getSystemProperty(String name) {
            return null;
        }

        @Override
        public String getSystemPropertyValue(String name) {
            return null;
        }

        @Override
        public boolean containsProperty(String name) {
            return false;
        }

        @Override
        public List<Property> getProperty() {
            return null;
        }

        @Override
        public Property addProperty(Property property) {
            return null;
        }

        @Override
        public Property lookupProperty(String name) {
            return null;
        }

        @Override
        public Property removeProperty(String name) {
            return null;
        }

        @Override
        public Property removeProperty(Property removeMe) {
            return null;
        }

        @Override
        public Property getProperty(String name) {
            return null;
        }

        @Override
        public String getPropertyValue(String name) {
            return null;
        }

        @Override
        public String getPropertyValue(String name, String defaultValue) {
            return null;
        }

        @Override
        public String getReference() {
            return null;
        }

        @Override
        public ResourceRef getResourceRef(String name) {
            return null;
        }

        @Override
        public boolean isResourceRefExists(String refName) {
            return false;
        }

        @Override
        public void deleteResourceRef(String name) throws TransactionFailure {

        }

        @Override
        public void createResourceRef(String enabled, String refName) throws TransactionFailure {

        }

        @Override
        public ApplicationRef getApplicationRef(String appName) {
            return null;
        }

        @Override
        public Cluster getCluster() {
            return null;
        }

        @Override
        public List<DeploymentGroup> getDeploymentGroup() {
            return null;
        }

        @Override
        public boolean isCluster() {
            return false;
        }

        @Override
        public boolean isServer() {
            return false;
        }

        @Override
        public boolean isDas() {
            return false;
        }

        @Override
        public boolean isDeploymentGroup() {
            return false;
        }

        @Override
        public boolean isInstance() {
            return false;
        }

        @Override
        public String getAdminHost() {
            return null;
        }

        @Override
        public int getAdminPort() {
            return 0;
        }

        @Override
        public Config getConfig() {
            return null;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public List<ResourceRef> getResourceRef() {
            return null;
        }

        @Override
        public List<ApplicationRef> getApplicationRef() {
            return null;
        }

        @Override
        public ConfigBeanProxy getParent() {
            return null;
        }

        @Override
        public <T extends ConfigBeanProxy> T getParent(Class<T> type) {
            return null;
        }

        @Override
        public <T extends ConfigBeanProxy> T createChild(Class<T> type) throws TransactionFailure {
            return null;
        }

        @Override
        public ConfigBeanProxy deepCopy(ConfigBeanProxy parent) throws TransactionFailure {
            return null;
        }
    }
}

