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

package org.glassfish.deployapi.config;

import java.io.*;
import java.util.Iterator;

import javax.enterprise.deploy.spi.DeploymentConfiguration;
import javax.enterprise.deploy.model.DeployableObject;
import javax.enterprise.deploy.model.J2eeApplicationObject;
import javax.enterprise.deploy.model.DDBeanRoot;
import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.DConfigBeanRoot;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.exceptions.ConfigurationException;
import javax.enterprise.deploy.spi.exceptions.BeanNotFoundException;

import org.glassfish.deployapi.config.DConfigBeanRootFactoryImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * A container for all the server-specific configuration information for a 
 * single top-level J2EE module.  The DeploymentConfiguration  object could 
 * represent a single stand alone module or an EAR file that contains several 
 * sub-modules.
 *
 * @author Jerome Dochez
 */
public class SunDeploymentConfiguration implements DeploymentConfiguration {
    
    private final DeployableObject deployObject;
    private DeploymentManager deploymentManager=null;
    
    protected static final LocalStringManagerImpl localStrings =
	  new LocalStringManagerImpl(SunDeploymentConfiguration.class);
    
    /** Creates a new instance of SunDeploymentConfiguration */
    public SunDeploymentConfiguration(DeployableObject deployObject) throws ConfigurationException {
        this.deployObject = deployObject;
    }
        
    /**
     * Returns the top level configuration bean, DConfigBeanRoot,
     * associated with the deployment descriptor represented by
     * the designated DDBeanRoot bean.
     *
     * @param bean The top level bean that represents the 
     *       associated deployment descriptor.
     * @return the DConfigBeanRoot for editing the server-specific 
     *           properties required by the module.
     * @throws ConfigurationException reports errors in generating 
     *           a configuration bean
     */        
    public DConfigBeanRoot getDConfigBeanRoot(DDBeanRoot dDBeanRoot) throws ConfigurationException {
        
        return null;
    }    
    
    /**
     * Returns an object that provides access to
     * the deployment descriptor data and classes
     * of a J2EE module.
     * @return DeployableObject
     */
    public DeployableObject getDeployableObject() {
        return deployObject;
    }
    
    /**
     * Remove the root DConfigBean and all its children.
     *
     * @param bean the top leve DConfigBean to remove.
     * @throws BeanNotFoundException  the bean provides is
     *      not in this beans child list.
     */    
    public void removeDConfigBean(DConfigBeanRoot dConfigBeanRoot) throws BeanNotFoundException {
    }
    
    /** 
     * Restore from disk to a full set of configuration beans previously
     * stored.
     * @param inputArchive The input stream from which to restore 
     *       the Configuration.
     * @throws ConfigurationException reports errors in generating 
     *           a configuration bean
     */    
    public void restore(InputStream inputStream) throws ConfigurationException {
    }
    
    /**
     * Restore from disk to instantated objects all the DConfigBeans 
     * associated with a specific deployment descriptor. The beans
     * may be fully or partially configured.
     * @param inputArchive The input stream for the file from which the 
     *         DConfigBeans should be restored.
     * @param bean The DDBeanRoot bean associated with the 
     *         deployment descriptor file.
     * @return The top most parent configuration bean, DConfigBeanRoot
     * @throws ConfigurationException reports errors in generating 
     *           a configuration bean
     */    
    public DConfigBeanRoot restoreDConfigBean(InputStream inputStream, DDBeanRoot dDBeanRoot) throws ConfigurationException {
        return null;
    }
    
    /** 
     * Save to disk the current set configuration beans created for
     * this deployable module. 
     * It is recommended the file format be XML.
     *
     * @param outputArchive The output stream to which to save the 
     *        Configuration.
     * @throws ConfigurationException
     */    
    public void save(OutputStream outputStream) throws ConfigurationException {
    }
    
    /**
     * Save to disk all the configuration beans associated with 
     * a particular deployment descriptor file.  The saved data  
     * may be fully or partially configured DConfigBeans. The 
     * output file format is recommended to be XML.
     * @param outputArchive The output stream to which the DConfigBeans 
     *        should be saved.
     * @param bean The top level bean, DConfigBeanRoot, from which to be save.
     * @throws ConfigurationException reports errors in generating 
     *           a configuration bean
     */    
    public void saveDConfigBean(OutputStream outputStream, DConfigBeanRoot dConfigBeanRoot) throws ConfigurationException {
    }
           
    /**
     * sets the deployment manager
     */
    public void setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
    }
    
    /**
     * @return the deployment manager
     */
    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }
}
