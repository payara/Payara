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

package javax.enterprise.deploy.spi.factories;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;

/**
 * The DeploymentFactory interface is a deployment driver for a    
 * Java EE plaform product.  It returns a DeploymentManager object
 * which represents a connection to a specific Java EE platform
 * product.
 *
 * <p> Each application server vendor must provide an implementation 
 *  of this class in order for the Java EE Deployment API to work 
 *  with their product.
 *
 * <p> The class implementing this interface should have a public
 * no-argument constructor, and it should be stateless (two instances
 * of the class should always behave the same).  It is suggested but
 * not required that the class have a static initializer that registers
 * an instance of the class with the DeploymentFactoryManager class.
 *
 * <p> A <tt>connected</tt> or <tt>disconnected</tt> DeploymentManager 
 * can be requested.  A DeploymentManager that runs connected to the 
 * platform can provide access to Java EE resources. A DeploymentManager 
 * that runs disconnected only provides module deployment configuration 
 * support. 
 *
 * @see javax.enterprise.deploy.shared.factories.DeploymentFactoryManager
 */
public interface DeploymentFactory 
{
    /**
     * Tests whether this factory can create a DeploymentManager
     * object based on the specificed URI.  This does not indicate
     * whether such an attempt will be successful, only whether the
     * factory can handle the uri. 
     * @param uri The uri to check
     * @return <tt>true</tt> if the factory can handle the uri.
     */
    public boolean handlesURI(String uri);

    /**
     * Return a <tt>connected</tt> DeploymentManager instance.
     *
     * @param uri The URI that specifies the connection parameters
     * @param username An optional username (may be <tt>null</tt> if
     *        no authentication is required for this platform).
     * @param password An optional password (may be <tt>null</yy> if
     *        no authentication is required for this platform).
     * @return A ready DeploymentManager instance.
     * @throws DeploymentManagerCreationException  occurs when a 
     *        DeploymentManager could not be returned (server down, 
     *        unable to authenticate, etc).
     */
    public DeploymentManager getDeploymentManager(String uri, 
            String username, String password) 
            throws DeploymentManagerCreationException;

    /**
     * Return a <tt>disconnected</tt> DeploymentManager instance. 
     *
     * @param uri the uri of the DeploymentManager to return.
     * @return A DeploymentManager <tt>disconnected</tt> instance.
     * @throws DeploymentManagerCreationException occurs if the 
     *         DeploymentManager could not be created.
     */
    public DeploymentManager getDisconnectedDeploymentManager(String uri) 
            throws DeploymentManagerCreationException;

    /**
     * Provide a string with the name of this vendor's DeploymentManager.
     * @return the name of the vendor's DeploymentManager.
     */
    public String getDisplayName();

    /**
     * Provide a string identifying version of this vendor's 
     * DeploymentManager.
     * @return the name of the vendor's DeploymentManager.
     */
    public String getProductVersion();
}
