/*
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
 */
package fish.payara.arquillian.container.payara.managed;

import static org.jboss.arquillian.container.spi.client.deployment.Validate.notNull;

import java.io.File;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;


public class PayaraMicroContainerConfiguration implements ContainerConfiguration {

    private String microJar = System.getenv("MICRO_JAR");

    private boolean clusterEnabled = Boolean.parseBoolean(System.getenv("MICRO_CLUSTER_ENABLED"));

    private boolean outputToConsole = Boolean.parseBoolean(System.getenv().getOrDefault("MICRO_CONSOLE_OUTPUT", "true"))
            || System.getenv("MICRO_CONSOLE_OUTPUT").equals("");

    private boolean debug; // TODO

    public String getMicroJar() {
        return microJar;
    }

    public File getMicroJarFile() {
        return new File(getMicroJar());
    }

    /**
     * @param microJar the location of the Payara Micro Jar. This is a required field.
     */
    public void setMicroJar(String microJar) {
        this.microJar = microJar;
    }

    public boolean isClusterEnabled() {
        return clusterEnabled;
    }

    /**
     * @param clusterEnabled Enable clustering on the instance.
     * Disabled by default.
     */
    public void setClusterEnabled(boolean clusterEnabled) {
        this.clusterEnabled = clusterEnabled;
    }

    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    /**
     * @param outputToConsole Show the output of the admin commands on the console.
     * Enabled by default.
     */
    public void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * @param debug Flag to start the server in debug mode 
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Validates if current configuration is valid, that is if all required properties are set and
     * have correct values
     */
    public void validate() throws ConfigurationException {
        notNull(getMicroJar(), "The property microJar must be specified or the MICRO_JAR environment variable must be set");

        if (!getMicroJarFile().isFile()) {
            throw new IllegalArgumentException("Could not locate the Payra Micro jar file " + getMicroJar());
        }

    }
}
