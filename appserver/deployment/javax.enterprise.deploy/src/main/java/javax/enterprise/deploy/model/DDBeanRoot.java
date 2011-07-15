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
 * An interface that represents the root of a standard deployment 
 * descriptor.  A DDBeanRoot is a type of DDBean.
 *
 * @author gfink
 */
public interface DDBeanRoot extends DDBean {
    
    /**
     * Return the ModuleType of deployment descriptor.
     *
     * @return The ModuleType of deployment descriptor
     */   
   public ModuleType getType();
   
   /**
    * Return the containing DeployableObject
    * @return The DeployableObject that contains this
    *           deployment descriptor
    */   
   public DeployableObject getDeployableObject();
   
   /**
    * A convenience method to return the DTD version number.
    * The DeployableObject has this information.
    * @return a string containing the DTD version number
    *
    * This method is being deprecated. Two DD data formats
    * are being used, DTD and XML Schema.  DDBeanRoot.getDDBeanRootVersion
    * should be used in its place.
    *
    * @deprecated As of version 1.1 replaced by 
    * DDBeanRoot.getDDBeanRootVersion()
    */
   public String getModuleDTDVersion();

   /** 
    * Returns the version number of an XML instance document.
    * This method is replacing the methods DDBeanRoot.getModuleDTDVersion 
    * and DeployableObject.getModuleDTDVersion. This method returns 
    * the version number of any Java EE XML instance document. 
    *
    * @return a string that is the version number of the XML instance 
    *   document. Null is returned if no version number can be found. 
    */ 
   public String getDDBeanRootVersion(); 

    /**
     * Return the XPath for this standard bean.
     * The root XPath is "/".
     * @return "/" this is the root standard bean.
     */
    public String getXpath();

    /**
     * Returns the filename relative to the root of the module 
     * of the XML instance document this DDBeanRoot represents. 
     *
     * @return String the filename relative to the root of the module 
     */ 
   public String getFilename(); 
}

