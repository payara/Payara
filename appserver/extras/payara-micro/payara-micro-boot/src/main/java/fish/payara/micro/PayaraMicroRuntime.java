/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro;

import fish.payara.micro.event.PayaraClusteredCDIEvent;
import fish.payara.micro.event.PayaraClusterListener;
import fish.payara.micro.event.CDIEventListener;
import fish.payara.micro.data.InstanceDescriptor;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 *
 * @author steve
 */
public interface PayaraMicroRuntime {

    public void addCDIEventListener(CDIEventListener listener);

    public void addClusterListener(PayaraClusterListener listener);

    /**
     * Deploy from an InputStream which can load the Java EE archive
     * @param name The name of the deployment
     * @param contextRoot The context root to deploy the application to
     * @param is InputStream to load the war through
     * @return true if deployment was successful
     */
    public boolean deploy(String name, String contextRoot, InputStream is);

    /**
     * Deploy from an InputStream which can load the Java EE archive
     * @param name The name of the deployment and the context root of the deployment if a war file
     * @param is InputStream to load the war through
     * @return true if deployment was successful
     */
    public boolean deploy(String name, InputStream is);

    /**
     * Deploys a new archive to a running Payara Micro instance
     * @param name The name to give the application once deployed
     * @param contextRoot The context root to give the application
     * @param war A File object representing the archive to deploy, it can be an exploded directory
     * @return
     */
    public boolean deploy(String name, String contextRoot, File war);

    /**
     *  Deploys a new archive to a running Payara Micro instance
     * @param war A File object representing the archive to deploy, it can be an exploded directory
     * @return true if the file deployed successfully
     */
    public boolean deploy(File war);

    /**
     * Returns a collection if instance descriptors for all the Payara Micros in the cluster
     * @return
     */
    public Collection<InstanceDescriptor> getClusteredPayaras();

    /**
     * Returns the names of the deployed applications
     * @return a collection of names or null if there was a problem
     */
    public Collection<String> getDeployedApplicationNames();

    /**
     * Returns the instance name
     * @return
     */
    public String getInstanceName();

    public InstanceDescriptor getLocalDescriptor();

    public void publishCDIEvent(PayaraClusteredCDIEvent event);

    public void removeCDIEventListener(CDIEventListener listener);

    public void removeClusterListener(PayaraClusterListener listener);

    /**
     * Runs an asadmin command on all members of the Payara Micro Cluster
     * Functionally equivalent to the run method of the ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param command The name of the asadmin command to run
     * @param args The parameters to the command
     * @return
     */
    public Map<InstanceDescriptor, Future<? extends ClusterCommandResult>> run(String command, String... args);

    /**
     * Runs an asadmin command on specified  members of the Payara Micro Cluster
     * Functionally equivalent to the run method of the ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param command The name of the asadmin command to run
     * @param args The parameters to the command
     * @return
     */
    public Map<InstanceDescriptor, Future<? extends ClusterCommandResult>> run(Collection<InstanceDescriptor> members, String command, String... args);

    /**
     * Runs a Callable object on all members of the Payara Micro Cluster
     * Functionally equivalent to the run method on ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param <T> The Type of the Callable
     * @param callable The Callable object to run
     * @return
     */
    public <T extends Serializable> Map<InstanceDescriptor, Future<T>> run(Callable<T> callable);

    /**
     * Runs a Callable object on specified members of the Payara Micro Cluster
     * Functionally equivalent to the run method on ClusterCommandRunner passing in
     * all cluster members obtained from getClusteredPayaras()
     * @param <T> The Type of the Callable
     * @param members The collection of members to run the callable on
     * @param callable The Callable object to run
     * @return
     */
    public <T extends Serializable> Map<InstanceDescriptor, Future<T>> run(Collection<InstanceDescriptor> members, Callable<T> callable);

    /**
     * Stops and then shuts down the Payara Micro Server
     *
     * @throws BootstrapException
     */
    public void shutdown() throws BootstrapException;

    /**
     * Undeploys the named application
     * @param name Name of the application to undeploy
     */
    public void undeploy(String name);
    
}
