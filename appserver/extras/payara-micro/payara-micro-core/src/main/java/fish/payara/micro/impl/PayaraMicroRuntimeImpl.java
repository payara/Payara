/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 */
package fish.payara.micro.impl;

import fish.payara.asadmin.CommandResult;
import fish.payara.micro.BootstrapException;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.event.CDIEventListener;
import fish.payara.micro.event.PayaraClusterListener;
import fish.payara.micro.PayaraInstance;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.micro.event.PayaraClusteredCDIEvent;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFish.Status;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishRuntime;

/**
 * This class represents a running Payara Micro server and enables you to
 * manipulate a running server.
 *
 * All the methods can throw Illegal State Exception if the Payara Micro Server
 * has been shutdown
 *
 * @author steve
 */
public class PayaraMicroRuntimeImpl implements PayaraMicroRuntime  {
    private static final Logger logger = Logger.getLogger(PayaraMicroRuntimeImpl.class.getCanonicalName());

    private final GlassFish runtime;
    private final PayaraInstance instanceService;
    private final String instanceName;
    private final HashSet<PayaraClusterListener> listeners;

    PayaraMicroRuntimeImpl(GlassFish runtime) {
        this.runtime = runtime;
        this.listeners = new HashSet<>(10);
        try {
            instanceService = runtime.getService(PayaraInstance.class, "payara-instance");
            instanceName = instanceService.getInstanceName();
            instanceService.addBootstrapListener(new BootstrapListener(this));
        } catch (GlassFishException ex) {
            throw new IllegalStateException("Unable to retrieve the embedded Payara Micro HK2 service. Something bad has happened", ex);
        }
    }
    
    private GlassFishRuntime gfRuntime;
    
    PayaraMicroRuntimeImpl(GlassFish instance, GlassFishRuntime gfRuntime) {
        this(instance);
        this.gfRuntime = gfRuntime;
    }
    
    /**
     * Returns the instance name
     * @return 
     */
    @Override
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Stops and then shuts down the Payara Micro Server
     *
     * @throws BootstrapException
     */
    @Override
    public void shutdown() throws BootstrapException {
        checkState();
        try {
            //runtime.dispose();
            this.gfRuntime.shutdown();
        } catch (GlassFishException ex) {
            throw new BootstrapException("Unable to stop Payara Micro", ex);
        }
    }
    
    /**
     * Returns a collection if instance descriptors for all the Payara Micros in the cluster
     * @return 
     */
    @Override
    public Collection<InstanceDescriptor> getClusteredPayaras() {
        return instanceService.getClusteredPayaras();
    }
    
    /**
     * Returns the names of the deployed applications
     * @return a collection of names or null if there was a problem
     */
    @Override
    public Collection<String> getDeployedApplicationNames() {
        checkState();
        try {
            return runtime.getDeployer().getDeployedApplications();
        } catch (GlassFishException ex) {
            logger.log(Level.SEVERE, "There was a problem obtaining the names of the applications", ex);
            return null;
        }
    }
    
    /**
     * Runs an asadmin command on all members of the Payara Micro Cluster
     * Functionally equivalent to the run method of the ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param command The name of the asadmin command to run
     * @param args The parameters to the command
     * @return 
     */
    @Override
    public Map<InstanceDescriptor, Future<? extends CommandResult>> run (String command, String... args ) {
        
        // NEEDS TO HANDLE THE CASE FOR LOCAL RUNNING IF NO CLUSTER ENABLED
        
        Map<String,Future<CommandResult>> commandResult = instanceService.executeClusteredASAdmin(command, args);
        Map<InstanceDescriptor, Future<? extends CommandResult>> result = new HashMap<>(commandResult.size());
        for (Entry<String,Future<CommandResult>> entry : commandResult.entrySet()) {
            String uuid = entry.getKey();
            InstanceDescriptor id = instanceService.getDescriptor(uuid);
            if (id != null) {
                result.put(id, entry.getValue());
            }
        }
        return result;
    }

   
     /**
     * Runs an asadmin command on specified  members of the Payara Micro Cluster
     * Functionally equivalent to the run method of the ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param command The name of the asadmin command to run
     * @param args The parameters to the command
     * @return 
     */
    @Override
    public Map<InstanceDescriptor, Future<? extends CommandResult>> run (Collection<InstanceDescriptor> members, String command, String... args ) {
        
        HashSet<String> memberUUIDs = new HashSet<>(members.size());
        for (InstanceDescriptor member : members) {
            memberUUIDs.add(member.getMemberUUID());
        }
        
        Map<String,Future<CommandResult>> commandResult = instanceService.executeClusteredASAdmin(memberUUIDs,command, args);
        Map<InstanceDescriptor, Future<? extends CommandResult>> result = new HashMap<>(commandResult.size());
        for (Entry<String,Future<CommandResult>> entry : commandResult.entrySet()) {
            String uuid = entry.getKey();
            InstanceDescriptor id = instanceService.getDescriptor(uuid);
            if (id != null) {
                result.put(id, entry.getValue());
            }
        }
        return result;
    }
   
        /**
     * Runs a Callable object on all members of the Payara Micro Cluster
     * Functionally equivalent to the run method on ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras() 
     * @param <T> The Type of the Callable
     * @param callable The Callable object to run
     * @return 
     */
    @Override
    public <T extends Serializable> Map<InstanceDescriptor, Future<T>> run (Callable<T> callable) {
        
        // NEEDS TO HANDLE THE CASE FOR LOCAL RUNNING IF NO CLUSTER ENABLED
        
        Map<String, Future<T>> runCallable = instanceService.runCallable(callable);
        Map<InstanceDescriptor, Future<T>> result = new HashMap<>(runCallable.size());
        for (Entry<String, Future<T>> entry : runCallable.entrySet()) {
            String uuid = entry.getKey();
            InstanceDescriptor id = instanceService.getDescriptor(uuid);
            if (id != null) {
                result.put(id, entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * Runs a Callable object on specified members of the Payara Micro Cluster
     * Functionally equivalent to the run method on ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras() 
     * @param <T> The Type of the Callable
     * @param members The collection of members to run the callable on
     * @param callable The Callable object to run
     * @return 
     */
    @Override
    public <T extends Serializable> Map<InstanceDescriptor, Future<T>> run (Collection<InstanceDescriptor> members, Callable<T> callable) {
        
        HashSet<String> memberUUIDs = new HashSet<>(members.size());
        for (InstanceDescriptor member : members) {
            memberUUIDs.add(member.getMemberUUID());
        }    
        
        Map<String, Future<T>> runCallable = instanceService.runCallable(memberUUIDs,callable);
        Map<InstanceDescriptor, Future<T>> result = new HashMap<>(runCallable.size());
        for (Entry<String, Future<T>> entry : runCallable.entrySet()) {
            String uuid = entry.getKey();
            InstanceDescriptor id = instanceService.getDescriptor(uuid);
            if (id != null) {
                result.put(id, entry.getValue());
            }
        }
        return result;
    }
        /**
     * Deploy from an InputStream which can load the Java EE archive
     * @param name The name of the deployment
     * @param contextRoot The context root to deploy the application to
     * @param is InputStream to load the war through
     * @return true if deployment was successful
     */
    @Override
    public boolean deploy(String name, String contextRoot, InputStream is) {
        checkState();
        boolean result = false;
        try{
            runtime.getDeployer().deploy(is,"--availabilityenabled=true","--name",name, "--contextroot", contextRoot);
            result = true;
        }catch (GlassFishException gfe) {
                logger.log(Level.WARNING, "Failed to deploy archive ", gfe);            
        }
        return result;
    }
       
    /**
     * Deploy from an InputStream which can load the Java EE archive
     * @param name The name of the deployment and the context root of the deployment if a war file
     * @param is InputStream to load the war through
     * @return true if deployment was successful
     */
    @Override
    public boolean deploy(String name, InputStream is) {
        checkState();
        boolean result = false;
        try{
            runtime.getDeployer().deploy(is,"--availabilityenabled=true","--name",name, "--contextroot", name);
            result = true;
        }catch (GlassFishException gfe) {
                logger.log(Level.WARNING, "Failed to deploy archive ", gfe);            
        }
        return result;
    }
    
    /**
     * Deploys a new archive to a running Payara Micro instance
     * @param name The name to give the application once deployed
     * @param contextRoot The context root to give the application
     * @param war A File object representing the archive to deploy, it can be an exploded directory
     * @return 
     */
    @Override
    public boolean deploy(String name, String contextRoot, File war) {
        checkState();
        boolean result = false;
        if (war.exists() && (war.isFile() || war.isDirectory()) && war.canRead()) {
            try {
                runtime.getDeployer().deploy(war, "--availabilityenabled=true","--name",name, "--contextroot", contextRoot);
                result = true;
            } catch (GlassFishException ex) {
                logger.log(Level.WARNING, "Failed to deploy archive ", ex);
            }
        } else {
            logger.log(Level.WARNING, "{0} is not a valid deployment", war.getAbsolutePath());
        }
        return result;        
    }
    

    /**
     *  Deploys a new archive to a running Payara Micro instance
     * @param war A File object representing the archive to deploy, it can be an exploded directory
     * @return true if the file deployed successfully
     */
    @Override
    public boolean deploy(File war) {
        checkState();
        boolean result = false;
        if (war.exists() && (war.isFile() || war.isDirectory()) && war.canRead()) {
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
    
    /**
     * Undeploys the named application 
     * @param name Name of the application to undeploy
     */
    @Override
    public void undeploy(String name) {
        try {
            runtime.getDeployer().undeploy(name);
        } catch (GlassFishException ex) {
            logger.log(Level.WARNING, "Failed to undeploy application {0}", name);
        }
    }
    
    @Override
    public void removeClusterListener(PayaraClusterListener listener) {
        listeners.remove(listener);
    }
    
    
    @Override
    public void addClusterListener(PayaraClusterListener listener) {
        listeners.add(listener);
    }

    @Override
    public void publishCDIEvent(PayaraClusteredCDIEvent event) {
        this.instanceService.publishCDIEvent(event);
    }
    
    @Override
    public void addCDIEventListener(CDIEventListener listener) {
        this.instanceService.addCDIListener(listener);
    }
    
    @Override
    public InstanceDescriptor getLocalDescriptor( ) {
        return instanceService.getLocalDescriptor();
    }

    @Override
    public void removeCDIEventListener(CDIEventListener listener) {
        this.instanceService.removeCDIListener(listener);
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
    
    void memberAdded(InstanceDescriptor id) {
        for (PayaraClusterListener listener : listeners) {
            listener.memberAdded(id);
        }
    }

    void memberRemoved(InstanceDescriptor id) {
        for (PayaraClusterListener listener : listeners) {
            listener.memberRemoved(id);
        }
    }

}
