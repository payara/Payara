/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

package com.sun.enterprise.connectors.connector.module;

import org.glassfish.internal.deployment.GenericSniffer;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.deployment.annotation.introspection.EjbComponentAnnotationScanner;
import com.sun.enterprise.deployment.annotation.introspection.ResourceAdapterAnnotationScanner;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Singleton;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.lang.annotation.Annotation;

/**
 * Sniffer for detecting resource-adapter modules
 *
 * @author Jagadish Ramu
 */
@Service(name = ConnectorConstants.CONNECTOR_MODULE)
@Scoped(Singleton.class)
public class ConnectorSniffer extends GenericSniffer {

    @Inject
    private Logger logger;

    private static final Class[]  connectorAnnotations = new Class[] {
            javax.resource.spi.Connector.class };

    public ConnectorSniffer() {
        super(ConnectorConstants.CONNECTOR_MODULE, "META-INF/ra.xml", null);
    }

    final String[] containerNames = {"com.sun.enterprise.connectors.module.ConnectorContainer"};

    /**
     * Sets up the container libraries so that any imported bundle from the
     * connector jar file will now be known to the module subsystem
     *
     * @param containerHome is where the container implementation resides
     * @param logger        the logger to use
     * @throws java.io.IOException exception if something goes sour
     */
    @Override
    public Module[] setup(String containerHome, Logger logger) throws IOException {
        // do nothing, we are embedded in GFv3 for now
        return null;
    }

    /**
     * Returns the list of Containers that this Sniffer enables.
     * <p/>
     * The runtime will look up each container implementing
     * using the names provided in the habitat.
     *
     * @return list of container names known to the habitat for this sniffer
     */
    public String[] getContainersNames() {
        return containerNames;
    }

    /**
     * Returns the Module type
     *
     * @return the container name
     */
    public String getModuleType() {
        return ConnectorConstants.CONNECTOR_MODULE;
    }

    /**
     * Returns the list of annotations types that this sniffer is interested in.
     * If an application bundle contains at least one class annotated with
     * one of the returned annotations, the deployment process will not
     * call the handles method but will invoke the containers deployers as if
     * the handles method had been called and returned true.
     *
     * @return list of annotations this sniffer is interested in or an empty array
     */
    @Override
    public Class<? extends Annotation>[] getAnnotationTypes() {
        return connectorAnnotations;
    }

    /**
     * @return whether this sniffer should be visible to user
     *
     */
    public boolean isUserVisible() {
        return true;
    }

    /**
     * @return the set of the sniffers that should not co-exist for the
     * same module. For example, ejb and appclient sniffers should not
     * be returned in the sniffer list for a certain module.
     * This method will be used to validate and filter the retrieved sniffer
     * lists for a certain module
     *
     */
    public String[] getIncompatibleSnifferTypes() {
        return new String[] {"ejb", "web"};
    }

    private static final List<String> deploymentConfigurationPaths =
            initDeploymentConfigurationPaths();

    private static List<String> initDeploymentConfigurationPaths() {
        final List<String> result = new ArrayList<String>();
        result.add("META-INF/ra.xml");
        result.add("META-INF/sun-ra.xml");
        result.add("META-INF/weblogic-ra.xml");
        return result;
    }

    /**
     * Returns the descriptor paths that might exist in a connector app.
     *
     * @return list of the deployment descriptor paths
     */
    @Override
    protected List<String> getDeploymentConfigurationPaths() {
        return deploymentConfigurationPaths;
    }
}
