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

import static fish.payara.arquillian.container.payara.clientutils.PayaraClient.ADMINSERVER;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.deployment.Validate;

/**
 * The common set of properties for a Payara container.
 * Extracted from the Configuration class for the remote Payara container.
 *
 * @author Vineet Reynolds
 */
public class CommonPayaraConfiguration implements ContainerConfiguration {

    protected String adminHost = "localhost";
    protected int adminPort = 4848;

    protected boolean adminHttps;
    private boolean authorisation;
    private String adminUser;
    private String adminPassword;
    private String target = ADMINSERVER;
    private String libraries;
    private String properties;
    private String type;

    public CommonPayaraConfiguration() {
        super();
    }

    public String getAdminHost() {
        return adminHost;
    }

    /**
     * @param adminHost
     *     Glassfish Admin Server (DAS) host address. Used to build the URL for the REST request.
     */
    public void setAdminHost(String adminHost) {
        this.adminHost = adminHost;
    }

    public int getAdminPort() {
        return adminPort;
    }

    /**
     * @param adminPort
     *     Glassfish Admin Console port. Used to build the URL for the REST request.
     */
    public void setAdminPort(int adminPort) {
        this.adminPort = adminPort;
    }

    public boolean isAdminHttps() {
        return adminHttps;
    }

    /**
     * @param adminHttps
     *     Flag indicating the administration url uses a secure connection. Used to build the URL for the REST
     *     request.
     */
    public void setAdminHttps(boolean adminHttps) {
        this.adminHttps = adminHttps;
    }

    public boolean isAuthorisation() {
        return authorisation;
    }

    /**
     * @param authorisation
     *     Flag indicating the remote server requires an admin user and password.
     */
    public void setAuthorisation(boolean authorisation) {
        this.authorisation = authorisation;
    }

    public String getAdminUser() {
        return adminUser;
    }

    /**
     * @param adminUser
     *     Authorised admin user in the remote glassfish admin realm
     */
    public void setAdminUser(String adminUser) {
        this.setAuthorisation(true);
        this.adminUser = adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * @param adminPassword
     *     Authorised admin user password
     */
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getTarget() {
        return target;
    }

    /**
     * @param target
     *     Specifies the target to which you are  deploying.
     *     <p>
     *     Valid values are:
     *     server
     *     Deploys the component to the default Admin Server instance.
     *     This is the default value.
     *     instance_name
     *     Deploys the component to  a  particular  stand-alone
     *     sever instance.
     *     cluster_name
     *     Deploys the component to every  server  instance  in
     *     the cluster. (Though Arquillion use only one instance
     *     to run the test case.)
     *     <p>
     *     The domain name as a target is not a reasonable deployment
     *     senarion in case of testing.
     */
    public void setTarget(String target) {
        this.target = target;
    }

    public String getLibraries() {
        return libraries;
    }

    /**
     * @param library
     *     A comma-separated list of library JAR files. Specify the
     *     library  JAR  files by their relative or absolute paths.
     *     Specify relative paths relative to domain-dir/lib/applibs.
     *     <p>
     *     The libraries are made available to the application in
     *     the order specified.
     */
    public void setLibraries(String library) {
        this.libraries = library;
    }

    public String getProperties() {
        return properties;
    }

    /**
     * @param properties
     *     Optional keyword-value  pairs  that  specify  additional
     *     properties  for the deployment. The available properties
     *     are determined by the implementation  of  the  component
     *     that  is  being deployed or redeployed.
     */
    public void setProperties(String properties) {
        this.properties = properties;
    }

    public String getType() {
        return this.type;
    }

    /**
     * @param type
     *     The packaging archive type of the component that is
     *     being deployed. Only possible values is: osgi
     *     <p>
     *     The component is packaged as an OSGi Alliance bundle.
     *     The type option is optional. If the component is packaged
     *     as a regular archive, omit this option.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Validates if current configuration is valid, that is if all required
     * properties are set and have correct values
     */
    public void validate() throws ConfigurationException {
        if (isAuthorisation()) {
            Validate.notNull(getAdminUser(), "adminUser must be specified to use authorisation");
            Validate.notNull(getAdminPassword(), "adminPassword must be specified to use authorisation");
        }
    }
}