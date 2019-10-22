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

import fish.payara.test.containers.tools.properties.Properties;

import java.io.File;
import java.net.SocketTimeoutException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Test configuration, see test.properties (filtered by Maven).
 *
 * @author David Matějček
 */
public final class TestConfiguration {

    private static final TestConfiguration CONFIGURATION = new TestConfiguration();

    private final File buildDirectory;
    private final File classDirectory;
    private final File payaraDirectory;

    private final String payaraHost;
    private final int payaraPort;
    private final String payaraUsername;
    private final String payaraPassword;
    private final int jerseyClientConnectionTimeout;
    private final int jerseyClientReadTimeout;



    /**
     * @return instance loaded from the test.properties.
     */
    public static TestConfiguration getInstance() {
        return CONFIGURATION;
    }


    /**
     * Generic constructor. Uses {@link Properties} instance to get values of it's attributes.
     */
    private TestConfiguration() {
        final Properties properties = new Properties("test.properties");

        this.buildDirectory = properties.getFile("build.directory");
        this.classDirectory = properties.getFile("class.directory");

        this.payaraDirectory = new File(properties.getFile("docker.payara.sharedDirectory"), "payara5");
        this.payaraHost = properties.getString("docker.payara.host");
        this.payaraPort = properties.getInt("docker.payara.port", 0);
        this.payaraUsername = properties.getString("docker.payara.username");
        this.payaraPassword = properties.getString("docker.payara.password");

        this.jerseyClientConnectionTimeout = properties.getInt("benchmark.client.timeoutInMillis.connect", 0);
        this.jerseyClientReadTimeout = properties.getInt("benchmark.client.timeoutInMillis.read", 0);
    }


    /**
     * @return internal hostname of the payara docker container
     */
    public String getPayaraHost() {
        return this.payaraHost;
    }


    /**
     * @return internal port of the application in the docker container.
     */
    public int getPayaraPort() {
        return this.payaraPort;
    }


    /**
     * @return username valid to login into the application in the docker container.
     */
    public String getPayaraUsername() {
        return this.payaraUsername;
    }


    /**
     * @return password valid to login into the application in the docker container.
     */
    public String getPayaraPassword() {
        return this.payaraPassword;
    }


    /**
     * @return path to the target directory
     */
    public File getBuildDirectory() {
        return this.buildDirectory;
    }


    /**
     * @return path to the directory with compiled classes and resources
     */
    public File getClassDirectory() {
        return this.classDirectory;
    }


    /**
     * @return path to the Payara directory
     */
    public File getPayaraDirectory() {
        return this.payaraDirectory;
    }


    /**
     * @return path to the benchmark results.
     */
    public File getBenchmarkOutputDirectory() {
        return getBuildDirectory();
    }


    /**
     * @return time in millis; short time causes {@link SocketTimeoutException}
     *         with the "Connection timeout" message.
     */
    public int getJerseyClientConnectionTimeout() {
        return this.jerseyClientConnectionTimeout;
    }


    /**
     * @return time in millis; short time causes {@link SocketTimeoutException}
     *         with the "Read timed out" message.
     */
    public int getJerseyClientReadTimeout() {
        return this.jerseyClientReadTimeout;
    }


    /**
     * Returns all properties - one property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
