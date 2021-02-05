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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

/**
 * Payara docker image manager. Prepares image and starts the container.
 *
 * @author David Matějček
 */
public class PayaraServerDockerImageManager
    extends JavaDockerImageManager<PayaraServerContainer, PayaraServerContainerConfiguration> {

    private static final String PAYARA_DOCKER_IMAGE_STARTED = "xxxxxxxxxxx PAYARA DOCKER IMAGE STARTED xxxxxxxxxxx";

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
        final PayaraServerContainerConfiguration cfg = getConfiguration();
        command.append(" && ls -la ").append(cfg.getMainApplicationDirectoryInDocker());
        command.append(" && ls -la ").append(cfg.getPayaraDomainDirectoryInDocker().getParentFile());
        if (cfg.isJaCoCoEnabled()) {
            // FIXME: apply to payara's use case, probably move to lib or domain directory!
            command.append(" && unzip -o ").append(cfg.getMainApplicationDirectoryInDocker())
                .append("/org.jacoco.agent-").append(cfg.getJaCoCoVersion()).append(".jar")
                .append(" \"jacocoagent.jar\" -d ").append(cfg.getMainApplicationDirectoryInDocker()); //
        }
        command.append(" && (cp ").append(getSourceLoggingProperties()) //
            .append(' ').append(cfg.getPayaraLoggingPropertiesInDocker()).append(" || true)");
        command.append(" && ls -la ").append(new File(cfg.getPayaraDomainDirectoryInDocker(), "config"));

        final File asadmin = cfg.getAsadminFileInDocker();
        command.append(" && ").append(asadmin).append(" start-domain ").append(cfg.getPayaraDomainName());
        command.append(" && echo '" + PAYARA_DOCKER_IMAGE_STARTED + "'");
        command.append(" && tail -F ").append(cfg.getPayaraServerLogInDocker()); //
        return command;
    }


    private String getSourceLoggingProperties() {
        return "/logging-" + (getConfiguration().isNewLoggingImplementation() ? "new" : "old") + ".properties";
    }


    @Override
    protected List<Integer> getExposedInternalPorts() {
        return Arrays.asList(TestablePayaraPort.getAllPossiblePortValues());
    }


    @Override
    protected WaitStrategy getWaitStrategy() {
        return Wait.forLogMessage(".*" + PAYARA_DOCKER_IMAGE_STARTED + ".*", 1)
            .withStartupTimeout(Duration.ofSeconds(getConfiguration().getStartupTimeout()));
    }


    // TODO: refactor - these files are used only in concrete tests, so those tests should use customized manager
    @Override
    protected Map<String, MountableFile> getFilesToCopy() {
        final Map<String, MountableFile> files = super.getFilesToCopy();
        addFileToCopyIfExists("server-side/passwordfile.txt", //
            getConfiguration().getPasswordFileInDocker(), 0777, files);
        addFileToCopyIfExists("server-side/passwordfile-user.txt", //
            getConfiguration().getPasswordFileForUserInDocker(), 0777, files);
        // we cannot copy file into payara directories before they would be created,
        // so we will separate this action into two steps:
        // 1) share file with the container
        // 2) copy shared file after we unzip the payara server to the correct place.
        addFileToCopyIfExists("server-side/logging-old.properties", "/logging-old.properties", 0777, files);
        addFileToCopyIfExists("server-side/logging-new.properties", "/logging-new.properties", 0777, files);
        addFileToCopyIfExists("server-side/logging-everything-old.properties", "/logging-everything-old.properties",
            0777, files);
        addFileToCopyIfExists("server-side/logging-everything-new.properties", "/logging-everything-new.properties",
            0777, files);

        return files;
    }


    @Override
    protected List<NetworkTarget> getTargetsToCheck() {
        final List<NetworkTarget> targets = new ArrayList<>();
        // TODO: targets that would be checked on startup to be available to telnet
        return targets;
    }
}
