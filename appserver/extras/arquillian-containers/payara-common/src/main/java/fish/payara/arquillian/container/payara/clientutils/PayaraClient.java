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
