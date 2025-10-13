/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2024] Payara Foundation and/or affiliates

package org.glassfish.config.support;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.Utility;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.glassfish.config.support.Constants.*;

/**
 * It is incredibly complex and difficult to do "perfect-parsing" when the elements
 * aren't in the right order.  These 3 elements:
 * clusters
 * servers
 * configs
 * Need to be in that exact order.  If they aren't in that order we MUST do a reparse
 * with the streaming parser.  As of July 6,2010 the clusters ALWAYS appears after the
 * other two so we have to do a reparse 100% of the time anyways.
 * Now (July 2010, GF 3.1)a new wrinkle has been added.  Each instance wants to
 * have information about all other servers in its cluster.
 * This makes the parsing with the old method so difficult and complex that I came up
 * with a new plan:
 * ALWAYS parse twice.  The first time through should be VERY fast because we are skipping
 * almost everything.  I'm just picking out the minimal "look-ahead" info and saving it
 * for the final parse.
 * @author Byron Nevins
 */
class DomainXmlPreParser {

    static class DomainXmlPreParserException extends Exception {

        DomainXmlPreParserException(Throwable t) {
            super(t);
        }

        private DomainXmlPreParserException(String s) {
            super(s);
        }
    }
    
    private XMLStreamReader reader;
    private List<ClusterData> clusters = new LinkedList<>();
    private List<DeploymentGroupData> deploymentGroups = new LinkedList<>();
    private List<String> configNames = new LinkedList<>();
    private Map<String, String> mapServerConfig = new HashMap<>();
    private ClusterData cluster;
    private DeploymentGroupData deploymentGroup; // Refers to the FIRST deployment group for this instance.
    private String instanceName;
    private String serverConfigRef;
    private boolean valid = false;
    private boolean validDG = false;
    
    private static final boolean DEBUG = Boolean.parseBoolean(Utility.getEnvOrProp("AS_DEBUG"));
    private static final Logger LOGGER = Logger.getLogger("org.glassfish.config.support");

    DomainXmlPreParser(URL domainXml, XMLInputFactory xif, String instanceNameIn) throws DomainXmlPreParserException {
        if (domainXml == null || xif == null || !StringUtils.ok(instanceNameIn))
            throw new IllegalArgumentException();

        InputStream in = null;

        try {
            instanceName = instanceNameIn;
            in = domainXml.openStream();
            reader = xif.createXMLStreamReader(domainXml.toExternalForm(), in);
            parse();
            postProcess();
            validate();
        } catch (DomainXmlPreParserException e) {
            throw e;
        } catch (Exception e2) {
            throw new DomainXmlPreParserException(e2);
        } finally {
            cleanup(in);
        }
    }

    final String getClusterName() {
        if(!valid) {
            return null;
        }
        return cluster.name;
    }

    final List<String> getServerNames() {
        if(!valid) {
            return null;
        }
        return cluster.serverRefs;
    }

    final List<String> getDGServerNames() {
        if(!validDG) {
            return Collections.emptyList();
        }

        return deploymentGroups
            .stream()
            .filter(groupData -> groupData.dgServerRefs.contains(instanceName))
            .flatMap(groupData -> groupData.dgServerRefs.stream())
            .collect(Collectors.toList());
    }

    public Map<String, String> getMapServerConfig() {
        return this.mapServerConfig;
    }

    final String getConfigName() {
        if(!valid)
            return null;

        return cluster.configRef;
    }


    ///////////////////////////////////////////////////////////////////////////
    ///////   Everything below here is private   //////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    private void parse() throws XMLStreamException {
        while (reader.hasNext()) {
            if (reader.next() == START_ELEMENT) {
                handleElement();
            }
        }
    }

    private void cleanup(InputStream in) {
        // this code is so ugly that it lives here!!
        try {
            if (reader != null)
                reader.close();
            if (in != null)
                in.close();
            reader = null;
        }
        catch (Exception ex) {
            // ignore!
        }
    }

    private void postProcess() {
        // our instance is in either zero or one cluster.  Find it and set it.
        for (ClusterData cd : clusters) {
            for (String serverName : cd.serverRefs) {
                if (instanceName.equals(serverName)) {
                    cluster = cd;
                    return;
                }
            }
        }
        // if we get here that means the instance either 
        // does not exist or it is stand-alone
        cluster = new ClusterData();
        cluster.configRef = serverConfigRef;
        cluster.serverRefs.add(instanceName);

        // our instance is in either zero or one dg. Find it and set it.
        for (DeploymentGroupData dgData : deploymentGroups) {
            for (String serverName : dgData.dgServerRefs) {
                if (instanceName.equals(serverName)) {
                    deploymentGroup = dgData;
                    return;
                }
            }
        }
        // if we get here that means the instance either
        // does not exist or it is stand-alone
        deploymentGroup = new DeploymentGroupData();
        deploymentGroup.dgServerRefs.add(instanceName);
    }

    private void validate() throws DomainXmlPreParserException {
        // 1. confirm that the server was located
        if (serverConfigRef == null) {
            throw new DomainXmlPreParserException(Strings.get("dxpp.serverNotFound", instanceName));
        }
        
        // 2. config-ref of server matches config-ref of cluster
        if (!serverConfigRef.equals(cluster.configRef)) {
            throw new DomainXmlPreParserException(Strings.get("dxpp.configrefnotmatch", instanceName, cluster.name));
        }
        
        if(!configNames.contains(serverConfigRef)){
            throw new DomainXmlPreParserException(Strings.get("dxpp.confignotfound", instanceName, serverConfigRef));
        }

        valid = true;

        if (deploymentGroup != null) {
            validDG = true;
        }
    }

    private void handleElement() throws XMLStreamException {
        String name = reader.getLocalName();

        if (!StringUtils.ok(name))
            return;

        switch (name) {
            case SERVERS:
                handleServers();
                break;
            case CLUSTERS:
                handleClusters();
                break;
            case DEPLOYMENT_GROUPS:
                handleDeploymentGroups();
                break;
            case CONFIGS:
                handleConfigs();
                break;
            default:
                break;
        }
    }

    private void handleServers() throws XMLStreamException {
        // we are pointed at the servers element
        printf("FOUND SERVERS");

        while (skipToStartButNotPast(SERVER, SERVERS)) {
            handleServer();
        }
    }

    private void handleServer() {
        String name = getName();
        String configRef = getConfigRef();

        printf("SERVER: " + name + ", ref= " + configRef);
        mapServerConfig.put(name, configRef);

        if (instanceName.equals(name)) {
            serverConfigRef = configRef;
        }
    }

    private void handleDeploymentGroups() throws XMLStreamException {
        // we are pointed at the servers element
        printf("FOUND DEPLOYMENT GROUPS");

        while (skipToStartButNotPast(DEPLOYMENT_GROUP, DEPLOYMENT_GROUPS)) {
            handleDeploymentGroup();
        }
    }

    private void handleClusters() throws XMLStreamException {
        // we are pointed at the servers element
        printf("FOUND CLUSTERS");

        while (skipToStartButNotPast(CLUSTER, CLUSTERS)) {
            handleCluster();
        }
    }

    private void handleCluster() throws XMLStreamException {
        ClusterData cd = new ClusterData();
        cd.name = getName();
        cd.configRef = getConfigRef();
        handleServerRefs(cd);
        clusters.add(cd);
        printf(cd.toString());
    }

    private void handleDeploymentGroup() throws XMLStreamException {
        DeploymentGroupData deploymentGroupData = new DeploymentGroupData();
        deploymentGroupData.name = getName();
        handleDGServerRefs(deploymentGroupData);
        deploymentGroups.add(deploymentGroupData);
        printf(deploymentGroupData.toString());
    }

    private void handleServerRefs(ClusterData cd) throws XMLStreamException {

        while (skipToStartButNotPast(SERVER_REF, CLUSTER)) {
            cd.serverRefs.add(reader.getAttributeValue(null, REF));
        }
    }

    private void handleDGServerRefs(DeploymentGroupData dgData) throws XMLStreamException {
        while (skipToStartButNotPast(DG_SERVER_REF, DEPLOYMENT_GROUP)) {
            dgData.dgServerRefs.add(reader.getAttributeValue(null, REF));
        }
    }

    private void handleConfigs() throws XMLStreamException {
        // we are pointed at the configs element
        printf("FOUND CONFIGS");

        while (skipToStartButNotPast(CONFIG, CONFIGS)) {
            handleConfig();
        }
    }

    private void handleConfig() {
        String name = reader.getAttributeValue(null, NAME);
        printf("CONFIG: " + name);
        configNames.add(name);
    }

    private boolean skipToStartButNotPast(String startName, String stopName) throws XMLStreamException {
        if (!StringUtils.ok(startName) || !StringUtils.ok(stopName)) {
            throw new IllegalArgumentException();
        }
        while (reader.hasNext()) {
            reader.next();
            // getLocalName() will throw an exception in many states.  Be careful!!
            if (reader.isStartElement() && startName.equals(reader.getLocalName())) {
                return true;
            }
            if (reader.isEndElement() && stopName.equals(reader.getLocalName())) {
                return false;
            }
        }
        return false;
    }

    private String getName() {
        return reader.getAttributeValue(null, NAME);
    }

    private String getConfigRef() {
        return reader.getAttributeValue(null, CONFIG_REF);
    }

    private static void printf(String message) {
        if (DEBUG)
            LOGGER.log(Level.INFO, message);
    }

    private static class ClusterData {
        String name;
        String configRef;
        List<String> serverRefs = new ArrayList<>();

        @Override
        public String toString() {
            return "Cluster:name=" + name + ", config-ref=" + configRef + ", server-refs = " + serverRefs;
        }
    }

    private static class DeploymentGroupData {
        String name;
        List<String> dgServerRefs = new ArrayList<>();

        @Override
        public String toString() {
            return "DeploymentGroup:name=" + name + ", dg-server-refs = " + dgServerRefs;
        }
    }
}
