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

package javax.enterprise.deploy.model;

import javax.enterprise.deploy.shared.ModuleType;

/**
 * J2eeApplicationObject is an interface that represents a Java EE 
 * application (EAR); it maintains a DeployableObject for each 
 * module in the archive.
 */
public interface J2eeApplicationObject extends DeployableObject {
    
    /**
     * Return the DeployableObject of the specified URI designator.
     * @param uri Describes where to get the module from.
     * @return the DeployableObject describing the j2ee module at this uri
     *        or 'null' if there is not match.
     */
    public DeployableObject getDeployableObject(String uri);
    
    /**
     * Return the all DeployableObjects of the specified type.
     * @param type The type of module to return.
     * @return the list of DeployableObjects describing the j2ee module 
     *         at this uri or 'null' if there are no matches.
     */
    public DeployableObject[] getDeployableObjects(ModuleType type);
    
    /**
     * Return the all DeployableObjects in this application.
     * @return the DeployableObject describing the j2ee module at this uri
     *          or 'null' if there are no matches.
     */
    public DeployableObject[] getDeployableObjects();
        
    /**
     * Return the list of URIs of the designated module type.
     * @param type The type of module to return.
     * @return the Uris of the contained modules or 'null' if there
     *       are no matches.
     */
    public String[] getModuleUris(ModuleType type);
    
    /**
     * Return the list of URIs for all modules in the application.
     * @return the Uris of the contained modules or 'null' if there
     *       are no matches.
     */
    public String[] getModuleUris();
    
    /**
     * Return a list of DDBean based upon an XPath; all
     * deployment descriptors of the specified type are searched.
     *
     * @param type The type of deployment descriptor to query.
     * @param xpath An XPath string referring to a location in the
     *         deployment descriptor
     * @return The list of DDBeans or 'null' of there are no matches.
     */   
   public DDBean[] getChildBean(ModuleType type, String xpath);
   
   /**
    * Return the text value from the XPath; search only the 
    * deployment descriptors of the specified type. 
    *
    * @param type The type of deployment descriptor to query.
    * @param xpath An xpath string referring to a location in the
    *             deployment descriptor
    * @return The text values of this xpath or 'null' if there are no
    *          matches.
    */   
   public String[] getText(ModuleType type, String xpath);
   
   /**
    * Register a listener for changes in XPath that are related
    * to this deployableObject.
    *
    * @param type The type of deployment descriptor to query.
    * @param xpath The xpath to listen for.
    * @param xpl The listener.
    */   
   public void addXpathListener(ModuleType type, String xpath, 
		XpathListener xpl);
   
   /** 
    * Unregister the listener for an XPath.
    *
    * @param type The type of deployment descriptor to query.
    * @param xpath he XPath to listen for
    * @param xpl The listener
    */   
   public void removeXpathListener(ModuleType type, String xpath, 
		XpathListener xpl);
   
}

