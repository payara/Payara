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
package fish.payara.test.containers.tools.container;

import java.io.File;
import java.nio.file.Paths;

/**
 * Configuration for the {@link PayaraServerContainer}
 *
 * @author David Matějček
 */
// FIXME: extract paths and make them accessible from the container (but configuration not, because it cannot be changed on running container)
public class PayaraServerContainerConfiguration extends JavaContainerConfiguration {

    private static final String JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE = "jacoco-docker-payara-server.exec";

    private String payaraDirectoryName;
    private String payaraDomainName;
    private NetworkTarget dockerHostAndPort;
    private boolean newLoggingImplementation = true;

    /**
     * Name of the root payara directory
     *
     * @return payara5/payara41/...
     */
    public String getPayaraDirectoryName() {
        return payaraDirectoryName;
    }


    /**
     * Sets the name of the root payara directory (payara5/payara41/...)
     *
     * @param payaraDirectoryName root payara directory
     */
    public void setPayaraDirectoryName(final String payaraDirectoryName) {
        this.payaraDirectoryName = payaraDirectoryName;
    }


    /**
     * @return name of the domain existing in the container
     */
    public String getPayaraDomainName() {
        return payaraDomainName;
    }


    /**
     * @param payaraDomainName name of the domain existing in the container
     */
    public void setPayaraDomainName(final String payaraDomainName) {
        this.payaraDomainName = payaraDomainName;
    }


    /**
     * @param dockerHostAndPort {@link NetworkTarget} of the docker rest service
     */
    public void setDockerHostAndPort(final NetworkTarget dockerHostAndPort) {
        this.dockerHostAndPort = dockerHostAndPort;
    }


    /**
     * @return {@link NetworkTarget} of the docker rest service
     */
    public NetworkTarget getDockerHostAndPort() {
        return this.dockerHostAndPort;
    }


    /**
     * @return directory containing domain directory
     */
    public File getPayaraDomainDirectory() {
        return getPayaraMainDirectory().toPath()
            .resolve(Paths.get("glassfish", "domains", getPayaraDomainName())).toFile();
    }


    /**
     * @return directory containing domain directory in docker container
     */
    public File getPayaraDomainDirectoryInDocker() {
        return getPayaraMainDirectoryInDocker().toPath()
            .resolve(Paths.get("glassfish", "domains", getPayaraDomainName())).toFile();
    }


    /**
     * @return directory containing domain lib directory
     */
    public File getPayaraDomainLibDirectory() {
        return new File(getPayaraDomainDirectory(), "lib");
    }

    /**
     * @return directory containing domain lib directory in docker container
     */
    public File getPayaraDomainLibDirectoryInDocker() {
        return new File(getPayaraDomainDirectoryInDocker(), "lib");
    }


    /**
     * @return main payara directory in docker container
     */
    public File getPayaraMainDirectoryInDocker() {
        return new File(getMainApplicationDirectoryInDocker(), getPayaraDirectoryName());
    }


    /**
     * @return main payara directory on the host system.
     */
    public File getPayaraMainDirectory() {
        return new File(getMainApplicationDirectory(), getPayaraDirectoryName());
    }


    /**
     * @return logging.properties in docker
     */
    public File getPayaraLoggingPropertiesInDocker() {
        return getPayaraDomainDirectoryInDocker().toPath().resolve(Paths.get("config", "logging.properties")).toFile();
    }


    /**
     * @return server.log in docker
     */
    public File getPayaraServerLogInDocker() {
        return getPayaraDomainDirectoryInDocker().toPath().resolve(Paths.get("logs", "server.log")).toFile();
    }


    /**
     * @return output file in docker container (path to the
     *         {@value #JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE})
     */
    @Override
    public File getJaCoCoReportFileInDocker() {
        return new File(getJaCoCoReportDirectoryInDocker(), JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE);
    }


    /**
     * @return absolute path to the asadmin command
     */
    public File getAsadminFileInDocker() {
        return new File(getPayaraMainDirectoryInDocker(), "/bin/asadmin");
    }


    /**
     * @return absolute path to the passwordfile.txt
     */
    public File getPasswordFile() {
        return new File(getMainApplicationDirectory(), "passwordfile.txt");
    }


    /**
     * @return absolute path to the passwordfile.txt in docker container
     */
    public File getPasswordFileInDocker() {
        return new File(getMainApplicationDirectoryInDocker(), "passwordfile.txt");
    }


    /**
     * @return absolute path to the passwordfile-user.txt in docker container
     */
    public File getPasswordFileForUserInDocker() {
        return new File(getMainApplicationDirectoryInDocker(), "passwordfile-user.txt");
    }


    /**
     * @return which logging implementation to use. Depends on Payara version used.
     */
    public boolean isNewLoggingImplementation() {
        return newLoggingImplementation;
    }


    /**
     * @param newLoggingImplementation use new or old class names? Depends on Payara version used.
     */
    public void setNewLoggingImplementation(boolean newLoggingImplementation) {
        this.newLoggingImplementation = newLoggingImplementation;
    }
}
