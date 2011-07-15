/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

/*
 * DeploymentFactoryManager.java
 *
 * Created on January 28, 2002, 6:24 PM
 */

package javax.enterprise.deploy.shared.factories;


import java.util.Vector;
import java.util.Iterator;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

/**
 * The DeploymentFactoryManager class is a central registry for
 * Java EE DeploymentFactory objects.  The DeploymentFactoryManager
 * retains references to DeploymentFactory objects loaded by
 * a tool.  A DeploymentFactory object provides a reference to 
 * a DeploymentManager.
 *
 * The DeploymentFactoryManager has been implemented as a singleton.
 * A tool gets a reference to the DeploymentFactoryManager via the 
 * getInstance method.
 *
 * The DeploymentFactoryManager can return two types of 
 * DeploymentManagers, a connected DeploymentManager and a 
 * disconnected DeploymentManager.  The connected DeploymentManager
 * provides access to any product resources that may be required
 * for configurations and deployment.  The method to retrieve a
 * connected DeploymentManager is getDeploymentManager. This method
 * provides parameters for user name and password that the  product 
 * may require for user authentication.  A disconnected DeploymentManager
 * does not provide access to a running Java EE product. The method
 * to retrieve a disconnected DeploymentManager is 
 * getDisconnectedDeploymentManager.  A disconnected DeploymentManager
 * does not need user authentication information.
 */
public final class DeploymentFactoryManager {
    
    private Vector deploymentFactories = null;
    
    // Singleton instance
    private static DeploymentFactoryManager deploymentFactoryManager = null;
    
    /** Creates new RIDeploymentFactoryManager */
    private DeploymentFactoryManager() {
        deploymentFactories = new Vector();
    }
    /**
     * Retrieve the Singleton DeploymentFactoryManager
     * @return DeploymentFactoryManager instance
     *
     */
    public static DeploymentFactoryManager getInstance() {
        if(deploymentFactoryManager == null){
            deploymentFactoryManager = new DeploymentFactoryManager();
        }
        return deploymentFactoryManager;
    }
       
    /**
     * Retrieve the lists of currently registered DeploymentFactories.
     *
     * @return the list of DeploymentFactory objects or an empty array 
     * 		if there are none.
     */
    public DeploymentFactory[] getDeploymentFactories() {
        Vector deploymentFactoriesSnapShot = null;
        synchronized(this){
            deploymentFactoriesSnapShot = 
				(Vector)this.deploymentFactories.clone();
        }
        DeploymentFactory[] factoriesArray = 
			new DeploymentFactory[deploymentFactoriesSnapShot.size()];
        deploymentFactoriesSnapShot.copyInto(factoriesArray);
        return factoriesArray;
    }
    
    /**
     * Retrieves a DeploymentManager instance to use for deployment.
     * The caller provides a URI and optional username and password,
     * and all registered DeploymentFactories will be checked.  The
     * first one to understand the URI provided will attempt to
     * initiate a server connection and return a ready DeploymentManager
     * instance.
     *
     * @param uri The uri to check
     * @param username An optional username (may be <tt>null</tt> if
     *        no authentication is required for this platform).
     * @param password An optional password (may be <tt>null</yy> if
     *        no authentication is required for this platform).
     * @return A ready DeploymentManager instance.
     * @throws DeploymentManagerCreationException
     *         Occurs when the factory appropriate to the specified URI
     *         was unable to initialize a DeploymentManager instance
     *         (server down, unable to authenticate, etc.).
     */
    public DeploymentManager getDeploymentManager(String uri, String username,
		 String password) throws DeploymentManagerCreationException{
        try{
            DeploymentFactory[] factories = this.getDeploymentFactories();
            for(int factoryIndex=0; factoryIndex < factories.length; 
				factoryIndex++){
                if(factories[factoryIndex].handlesURI(uri)){
                    return factories[factoryIndex].getDeploymentManager(uri,
							username,password);
                }
            }
            // No available factory supports the provided url.
            throw new DeploymentManagerCreationException("URL ["+uri+
				"] not supported by any available factories");
        }catch(Throwable t){
            throw new DeploymentManagerCreationException(
				"Could not get DeploymentManager");
        }
    }
    
    /**
     * Registers a DeploymentFactory so it will be able to handle
     * requests.
     */
    public void registerDeploymentFactory(DeploymentFactory factory){
        this.deploymentFactories.add(factory);
    }
    
    /**
     * Return a <tt>disconnected</tt> DeploymentManager instance.
     *
     * @param uri identifier of the disconnected DeploymentManager to
     *             return.
     * @return A DeploymentManager instance.
     * @throws DeploymentDriverException  occurs if the DeploymentManager
     *         could not be created.
     */
    public DeploymentManager getDisconnectedDeploymentManager(String uri) 
               throws DeploymentManagerCreationException {
        try{
            DeploymentFactory[] factories = this.getDeploymentFactories();
            for(int factoryIndex=0; factoryIndex < factories.length; 
				factoryIndex++){
                if(factories[factoryIndex].handlesURI(uri)){
                    return factories[factoryIndex].getDisconnectedDeploymentManager(uri);
                }
            }
            // No available factory supports the provided url.
            throw new DeploymentManagerCreationException("URL ["+uri+
				"] not supported by any available factories");
        }catch(Throwable t){
            throw new DeploymentManagerCreationException(
				"Could not get DeploymentManager");
        }
    }
}
