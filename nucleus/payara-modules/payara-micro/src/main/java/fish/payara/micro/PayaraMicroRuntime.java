/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.micro;

import fish.payara.micro.services.PayaraMicroInstance;
import fish.payara.micro.services.data.InstanceDescriptor;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;

/**
 * This class represents a running Payara Micro server and enables you to
 * manipulate a running server.
 *
 * All the methods can throw Illegal State Exception if the Payara Micro Server
 * has been shutdown
 *
 * @author steve
 */
public class PayaraMicroRuntime {
    private static final Logger logger = Logger.getLogger(PayaraMicroRuntime.class.getCanonicalName());

    private final GlassFish runtime;
    private final PayaraMicroInstance instanceService;
    private final String instanceName;

    PayaraMicroRuntime(String instanceName, GlassFish runtime) {
        this.runtime = runtime;
        this.instanceName = instanceName;
        try {
            instanceService = runtime.getService(PayaraMicroInstance.class, "payara-micro-instance");
            instanceService.setInstanceName(instanceName);
        } catch (GlassFishException ex) {
            throw new IllegalStateException("Unable to retrieve the embedded Payara Micro HK2 service. Something bad has happened", ex);
        }
    }

    /**
     * Stops and then shuts down the Payara Micro Server
     *
     * @throws BootstrapException
     */
    public void shutdown() throws BootstrapException {
        checkState();
        try {
            runtime.dispose();
        } catch (GlassFishException ex) {
            throw new BootstrapException("Unable to stop Payara Micro", ex);
        }
    }
    
    public Collection<InstanceDescriptor> getClusteredPayaras() {
        return instanceService.getClusteredPayaras();
    }
    
    /**
     * Returns the names of the deployed applications
     * @return a collection of names or null if there was a problem
     */
    public Collection<String> getDeployedApplicationNames() {
        checkState();
        try {
            return runtime.getDeployer().getDeployedApplications();
        } catch (GlassFishException ex) {
            logger.log(Level.SEVERE, "There was a problem obtaining the names of the applications", ex);
            return null;
        }
    }
    
    public ClusterCommandRunner getCommandRunner() {
        checkState();
        try {
            return new ClusterCommandRunner(runtime.getCommandRunner(), instanceService);
        } catch (GlassFishException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    /**
     * Deploy from an InputStream which can load the Java EE archive
     * @param name The name of the deployment
     * @param is InputStream to load the war through
     * @return true if deployment was successful
     */
    public boolean deploy(String name, InputStream is) {
        checkState();
        boolean result = false;
        try{
            runtime.getDeployer().deploy(is,"--availabilityenabled=true","--name",name);
            result = true;
        }catch (GlassFishException gfe) {
                logger.log(Level.WARNING, "Failed to deploy archive ", gfe);            
        }
        return result;
    }

    /**
     *  Deploys a new archive to a running Payara Micro instance
     * @param war A File object representing the archive to deploy
     * @return true if the file deployed successfully
     */
    public boolean deploy(File war) {
        checkState();
        boolean result = false;
        if (war.exists() && war.isFile() && war.canRead()) {
            try {
                runtime.getDeployer().deploy(war, "--availabilityenabled=true");
                result = true;
            } catch (GlassFishException ex) {
                logger.log(Level.WARNING, "Failed to deploy archive ", ex);
            }
        } else {
            logger.log(Level.WARNING, "{0} is not a valid deployment", war.getAbsolutePath());
        }
        return result;
    }

    private void checkState() {
        try {
            if (!(runtime.getStatus().equals(Status.STARTED))) {
                throw new IllegalStateException("Payara Micro is not Running");
            }
        } catch (GlassFishException ex) {
            throw new IllegalStateException("Payara Micro is not Running");
        }
    }

}
