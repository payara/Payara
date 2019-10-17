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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import static org.testcontainers.utility.MountableFile.forClasspathResource;

/**
 * Payara docker image manager. Prepares image and starts the container.
 *
 * @author David Matějček
 */
public class PayaraServerDockerImageManager
    extends JavaDockerImageManager<PayaraServerContainer, PayaraServerContainerConfiguration> {

    private static final String PAYARA_DOCKER_IMAGE_STARTED = "xxxxxxxxxxx PAYARA DOCKER IMAGE STARTED xxxxxxxxxxx";


    /**
     * Creates the manager with Docker's default network.
     *
     * @param cfg
     */
    public PayaraServerDockerImageManager(final PayaraServerContainerConfiguration cfg) {
        this(null, cfg);
    }


    /**
     * Creates the manager.
     *
     * @param network - nullable
     * @param cfg
     */
    public PayaraServerDockerImageManager(final Network network, final PayaraServerContainerConfiguration cfg) {
        super(network, cfg);
    }


    @Override
    protected String getLoggerId() {
        return "D-PAYARA";
    }


    @Override
    protected String getRootDependencyId() {
        return "fish.payara.distributions:payara:zip";
    }


    @Override
    protected String getWebContextToCheck() {
        return "/";
    }


    @Override
    protected PayaraServerContainer createNewContainer() {
        return new PayaraServerContainer(getNameOfPreparedImage(), getConfiguration());
    }


    /**
     * @return {@link JavaDockerImageManager#getCommand()} enhanced by asadmin commands to allow
     *         external secure access to the admin port.
     */
    @Override
    protected StringBuilder getCommand() {
        final StringBuilder command = super.getCommand();
        if (getConfiguration().isJaCoCoEnabled()) {
            // FIXME: to lib/domain directory!
            command.append(" && unzip -o ").append(getConfiguration().getMainApplicationDirectoryInDocker())
                .append("/org.jacoco.agent-").append(getConfiguration().getJaCoCoVersion()).append(".jar")
                .append(" \"jacocoagent.jar\" -d ").append(getConfiguration().getMainApplicationDirectoryInDocker()); //
        }
        command.append(" && ls -la ").append(
            new File(getConfiguration().getMainApplicationDirectoryInDocker(), "glassfish/domains/domain1/config")); //

        final File asadmin = getConfiguration().getAsadminFileInDocker();
        command.append(" && ").append(asadmin) //
            .append(" --user admin --passwordfile ").append(getConfiguration().getPasswordFileForChangeInDocker()) //
            .append(" change-admin-password");
        command.append(" && ").append(asadmin).append(" start-domain domain1");
        command.append(" && ").append(asadmin) //
            .append(" --user admin --passwordfile ").append(getConfiguration().getPasswordFileInDocker()) //
            .append(" enable-secure-admin");
        command.append(" && ").append(asadmin).append(" restart-domain domain1");
        command.append(" && echo '" + PAYARA_DOCKER_IMAGE_STARTED + "'");
        command.append(" && sleep infinity"); //
        return command;
    }


    @Override
    protected WaitStrategy getWaitStrategy() {
        return Wait.forLogMessage(".*" + PAYARA_DOCKER_IMAGE_STARTED + ".*", 1)
            .withStartupTimeout(Duration.ofSeconds(getConfiguration().getStartupTimeout()));
    }


    @Override
    protected Map<String, MountableFile> getFilesToCopy() {
        final Map<String, MountableFile> files = super.getFilesToCopy();
        files.put(getConfiguration().getPasswordFileForChangeInDocker().getAbsolutePath(),
            forClasspathResource("server-side/passwordfile-change.txt"));
        files.put(getConfiguration().getPasswordFileInDocker().getAbsolutePath(),
            forClasspathResource("server-side/passwordfile.txt"));
        return files;
    }


    @Override
    protected List<NetworkTarget> getTargetsToCheck() {
        final List<NetworkTarget> targets = new ArrayList<>();
        // TODO: targets that would be checked on startup to be available to telnet
        return targets;
    }
}
