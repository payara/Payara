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
package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.container.MySQLContainerConfiguration;
import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;

import org.testcontainers.shaded.org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringStyle;

/**
 * Configuration of the docker environment.
 * <p>
 * Note: items in each subconfiguration should respect relations between containers, especially
 * ports and hostnames
 *
 * @author David Matějček
 */
public class DockerEnvironmentConfiguration {

    private boolean forceNewPayaraServer;
    private boolean forceNewMySQLServer;
    private boolean isUseMySqlContainer;

    private PayaraServerContainerConfiguration payaraServerConfiguration;
    private MySQLContainerConfiguration mySqlConfiguration;


    /**
     * @return true to delete prepared docker image and to create it again
     */
    public boolean isForceNewPayaraServer() {
        return this.forceNewPayaraServer;
    }


    /**
     * @param forceNewPayaraServer true to delete prepared docker image and to create it again
     */
    public void setForceNewPayaraServer(final boolean forceNewPayaraServer) {
        this.forceNewPayaraServer = forceNewPayaraServer;
    }


    /**
     * @return {@link PayaraServerContainerConfiguration} - configuration of the Payara Docker
     *         container
     */
    public PayaraServerContainerConfiguration getPayaraServerConfiguration() {
        return this.payaraServerConfiguration;
    }


    /**
     * @param payaraServerConfiguration - configuration of the Payara Docker container
     */
    public void setPayaraServerConfiguration(final PayaraServerContainerConfiguration payaraServerConfiguration) {
        this.payaraServerConfiguration = payaraServerConfiguration;
    }


    public void setUseMySqlContainer(boolean isUseMySqlContainer) {
        this.isUseMySqlContainer = isUseMySqlContainer;
    }


    public boolean isUseMySqlContainer() {
        return this.isUseMySqlContainer;
    }


    /**
     * @return true to delete prepared docker image and to create it again
     */
    public boolean isForceNewMySQLServer() {
        return this.forceNewMySQLServer;
    }


    /**
     * @param forceNewMySQLServer true to delete prepared docker image and to create it again
     */
    public void setForceNewMySQLServer(final boolean forceNewMySQLServer) {
        this.forceNewMySQLServer = forceNewMySQLServer;
    }


    /**
     * @return {@link MySQLContainerConfiguration} - configuration of the MySQL Docker
     *         container
     */
    public MySQLContainerConfiguration getMySQLServerConfiguration() {
        return this.mySqlConfiguration;
    }


    /**
     * @param mySqlConfiguration - configuration of the MySQL Docker container
     */
    public void setMySQLServerConfiguration(final MySQLContainerConfiguration mySqlConfiguration) {
        this.mySqlConfiguration = mySqlConfiguration;
    }

    /**
     * @return timeout of preparation of all container images in secodns.
     */
    public long getPreparationTimeout() {
        return this.payaraServerConfiguration.getPreparationTimeout();
    }


    /**
     * Returns all properties - each property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
