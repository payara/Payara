/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

import java.io.InputStream;
import java.util.logging.Logger;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import fish.payara.arquillian.container.payara.clientutils.PayaraClient;
import fish.payara.arquillian.container.payara.clientutils.PayaraClientException;
import fish.payara.arquillian.container.payara.clientutils.PayaraClientService;

/**
 * A class to aid in deployment and undeployment of archives involving a GlassFish container.
 * This class encapsulates the operations involving the GlassFishClient class.
 * Extracted from the GlassFish 3.1 remote container.
 *
 * @param <C>
 *     A class of type {@link CommonPayaraConfiguration}
 *
 * @author Vineet Reynolds
 */
public class CommonPayaraManager<C extends CommonPayaraConfiguration> {

    private static final Logger log = Logger.getLogger(CommonPayaraManager.class.getName());

    private static final String DELETE_OPERATION = "__deleteoperation";

    private C configuration;

    private PayaraClient payaraClient;

    private String deploymentName;

    public CommonPayaraManager(C configuration) {
        this.configuration = configuration;

        // Start up the PayaraClient service layer
        payaraClient = new PayaraClientService(configuration);
    }

    public void start() throws LifecycleException {
        try {
            payaraClient.startUp();
        } catch (PayaraClientException e) {
            log.severe(e.getMessage());
            throw new LifecycleException(e.getMessage());
        }
    }

    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        final String archiveName = archive.getName();

        final ProtocolMetaData protocolMetaData = new ProtocolMetaData();

        try {
            InputStream deployment = archive.as(ZipExporter.class).exportAsInputStream();

            // Build up the POST form to send to Payara
            final FormDataMultiPart form = new FormDataMultiPart();
            form.bodyPart(new StreamDataBodyPart("id", deployment, archiveName));

            deploymentName = createDeploymentName(archiveName);
            addDeployFormFields(deploymentName, form);

            // Do Deploy the application on the remote Payara
            HTTPContext httpContext = payaraClient.doDeploy(deploymentName, form);
            protocolMetaData.addContext(httpContext);
        } catch (PayaraClientException e) {
            throw new DeploymentException("Could not deploy " + archiveName, e);
        }
        
        return protocolMetaData;
    }

    public void undeploy(Archive<?> archive) throws DeploymentException {

        if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        deploymentName = createDeploymentName(archive.getName());
        
        try {
            // Build up the POST form to send to Payara
            FormDataMultiPart form = new FormDataMultiPart();
            form.field("target", configuration.getTarget(), TEXT_PLAIN_TYPE);
            form.field("operation", DELETE_OPERATION, TEXT_PLAIN_TYPE);
            
            payaraClient.doUndeploy(deploymentName, form);
        } catch (PayaraClientException e) {
            throw new DeploymentException("Could not undeploy " + archive.getName(), e);
        }
    }

    public boolean isDASRunning() {
        return payaraClient.isDASRunning();
    }

    private String createDeploymentName(String archiveName) {
        String correctedName = archiveName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }
        
        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }
        
        return correctedName;
    }

    private void addDeployFormFields(String name, FormDataMultiPart deployform) {

        // Add the name field, the name is the archive filename without extension
        deployform.field("name", name, TEXT_PLAIN_TYPE);

        // Add the target field (the default is "server" - Admin Server)
        deployform.field("target", configuration.getTarget(), TEXT_PLAIN_TYPE);

        // Add the libraries field (optional)
        if (configuration.getLibraries() != null) {
            deployform.field("libraries", configuration.getLibraries(), TEXT_PLAIN_TYPE);
        }

        // Add the properties field (optional)
        if (configuration.getProperties() != null) {
            deployform.field("properties", configuration.getProperties(), TEXT_PLAIN_TYPE);
        }

        // Add the type field (optional, the only valid value is "osgi", other values are ommited)
        if (configuration.getType() != null && "osgi".equals(configuration.getType())) {
            deployform.field("type", configuration.getType(), TEXT_PLAIN_TYPE);
        }
    }
}
