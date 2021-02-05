/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.test.containers.tools.container;

import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import javax.enterprise.deploy.spi.Target;

import org.glassfish.deployment.client.DFDeploymentProperties;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DFProgressObject;
import org.glassfish.deployment.client.RemoteDeploymentFacility;
import org.glassfish.deployment.client.ServerConnectionIdentifier;
import org.glassfish.deployment.common.DeploymentException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSR-88 Deployer for docker containers.
 *
 * @author David Matejcek
 */
public class Deployer {
    private static final Logger LOG = LoggerFactory.getLogger(Deployer.class);

    private final String host;
    private final int port;

    /**
     * Creates instance of {@link Deployer}
     *
     * @param host
     * @param adminPort usually 4848
     */
    public Deployer(final String host, final int adminPort) {
        this.host = host;
        this.port = adminPort;
    }

    /**
     * Deploys the archive to the server on given context root
     * <p>
     * The target is always <code>server</code>
     *
     * @param contextRoot
     * @param archive - it's name except suffix will be used as an application name.
     * @return application name
     * @throws DeploymentException - deployment failed
     */
    public String deploy(final String contextRoot, final Archive<?> archive) throws DeploymentException {
        LOG.debug("deploy(contextRoot={}, archive.name={})", contextRoot, archive.getName());
        Objects.requireNonNull(archive, "archive must not be null");

        // TODO: implement as parameter + extension (different defaults - as visible here)
        final DFDeploymentProperties options = new DFDeploymentProperties();
        final String applicationName = createApplicationName(archive.getName());
        options.setName(applicationName);
        options.setEnabled(true);
        options.setForce(true);
        options.setUpload(true);
        options.setTarget("server");
        options.setContextRoot(contextRoot);
        final Properties props = new Properties();
        props.setProperty("keepSessions", "true");
        props.setProperty("implicitCdiEnabled", "true");
        options.setProperties(props);

        // TODO: extend to implement AutoCloseable
        final RemoteDeploymentFacility deployer = connectDeployer();
        try {
            final MemoryMappedArchive deployedArchive = toInMemoryArchive(archive);
            final Target[] targets = new Target[] {deployer.createTarget("server")};
            final DFProgressObject progressObject = deployer.deploy(targets, deployedArchive, null, options);
            final DFDeploymentStatus deploymentStatus = progressObject.waitFor();
            LOG.info("Deployment status: {}", deploymentStatus);
            if (deploymentStatus.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                throw new DeploymentException("Deployment failed! " + deploymentStatus.getAllStageMessages());
            }
            final List<String> modules = getModuleNames(applicationName, deployer);
            LOG.info("modules: {}", modules);
            return applicationName;
        } catch (final IOException e) {
            throw new DeploymentException("Deployment of application " + applicationName + " failed!", e);
        } finally {
            deployer.disconnect();
        }
    }


    /**
     * Undeploys the archive. This method is symetrical to {@link #deploy(String, Archive)}.
     * <p>
     * The target is always <code>server</code>
     *
     * @param archive
     * @throws DeploymentException - undeployment failed
     */
    public void undeploy(final Archive<?> archive) throws DeploymentException {
        final String applicationName = createApplicationName(archive.getName());
        undeploy(applicationName);
    }


    /**
     * Undeploys the archive.
     *
     * @param applicationName - application name returned by {@link #deploy(String, Archive)}.
     * @throws DeploymentException - undeployment failed
     */
    public void undeploy(final String applicationName) throws DeploymentException {
        LOG.debug("undeploy(applicationName={})", applicationName);
        Objects.requireNonNull(applicationName, "archive must not be null");
        final RemoteDeploymentFacility deployer = connectDeployer();
        try {
            final Target[] targets = new Target[] {deployer.createTarget("server")};
            final DFProgressObject progressObject = deployer.undeploy(targets, applicationName);
            final DFDeploymentStatus deploymentStatus = progressObject.waitFor();
            LOG.info("Deployment status: {}", deploymentStatus);
            if (deploymentStatus.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                throw new DeploymentException("Undeployment failed!" + deploymentStatus.getAllStageMessages());
            }
        } finally {
            deployer.disconnect();
        }
    }


    private String createApplicationName(final String archiveName) {
        String correctedName = archiveName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }

        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }

        return correctedName;
    }


    private MemoryMappedArchive toInMemoryArchive(final Archive<?> archive) {
        try (InputStream archiveStream = archive.as(ZipExporter.class).exportAsInputStream()) {
            return new MemoryMappedArchive(archiveStream);
        } catch (IOException e) {
            throw new DeploymentException("Deployment failed - cannot load the input archive!", e);
        }
    }


    private RemoteDeploymentFacility connectDeployer() {
        LOG.trace("connectDeployer()");
        final RemoteDeploymentFacility deployer = new RemoteDeploymentFacility();
        final ServerConnectionIdentifier sci = new ServerConnectionIdentifier();

        sci.setHostName(this.host);
        sci.setHostPort(this.port);
        // TODO: configure+filter from pom.xml
        sci.setUserName("admin");
        sci.setPassword("admin");

        deployer.connect(sci);
        return deployer;
    }


    private List<String> getModuleNames(final String applicationName, final RemoteDeploymentFacility deployer) {
        try {
            return deployer.getSubModuleInfoForJ2EEApplication(applicationName);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not get names of submodules", e);
        }
    }
}
