/*
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
// Portions Copyright [2016-2017] [Payara Foundation]
package fish.payara.arquillian.container.payara.clientutils;

import java.util.Map;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;

/**
 * @author Z.Paulovics
 */
public interface PayaraClient {

    /**
     * Admin Server key for the REST request.
     */
    public static final String ADMINSERVER = "server";

    /**
     * Start-up the server
     * 
     * <p>
     * <ul>
     *   <li>   Get the node addresses list associated with the target
     *   <li>    Check the status of the target server instance
     *   <li>    In case of cluster tries to fund an instance which has RUNNING status
     * </ul>
     *
     */
    public void startUp();

    /**
     * Do deploy an application defined by a multipart form's data to the target server or cluster
     * of Payara
     *
     * @param name - name of the application form - a form of MediaType.MULTIPART_FORM_DATA_TYPE
     *
     * @return subComponents - a map of SubComponents of the application
     */
    public HTTPContext doDeploy(String name, FormDataMultiPart form) throws DeploymentException;

    /**
     * Do undeploy the application
     *
     * @param name - application name
     *
     * @return responseMap
     */
    public Map<String, Object> doUndeploy(String name, FormDataMultiPart form);

    /**
     * Verify whether the Domain Administration Server is running.
     */
    public boolean isDASRunning();
}
