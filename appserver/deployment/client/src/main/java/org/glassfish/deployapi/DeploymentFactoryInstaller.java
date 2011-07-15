/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployapi;

import java.io.*;
import java.util.jar.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

import org.glassfish.deployment.common.DeploymentUtils;
import com.sun.enterprise.util.shared.ArchivistUtils;
import com.sun.logging.LogDomains;

/**
 * This singleton object is responsible to resolving all the 
 * DeploymentManagerFactory installed in the RI and register
 * them to the DeploymentManagerFactory
 *
 * @author Jerome Dochez
 */
public class DeploymentFactoryInstaller {
    
    private static DeploymentFactoryInstaller dfInstaller = null;
    
    private final String J2EE_DEPLOYMENT_MANAGER_REPOSITORY = "lib" + File.separator + "deployment";
    private static final String J2EE_DEPLOYMENT_MANAGER = "J2EE-DeploymentFactory-Implementation-Class";
    private static final String J2EE_HOME = "com.sun.enterprise.home";
    
    private static final Logger sLogger=LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    /** Creates a single instance of DeploymentManagerFactoryResolver */
    private DeploymentFactoryInstaller() {
    }
    
    public static DeploymentFactoryInstaller getInstaller() {
    
        if (dfInstaller==null) {
            dfInstaller = new DeploymentFactoryInstaller();
            dfInstaller.initialize();
        }
        return dfInstaller;
    }
    
    /**
     * @return a list of installed deployment manager 
     * implementation archives
     */
    public File[] getListOfDeploymentFactoryFiles() {
        
        File repository = new File(System.getProperty("com.sun.aas.installRoot")+File.separator+
            J2EE_DEPLOYMENT_MANAGER_REPOSITORY);
        
        if (sLogger.isLoggable(Level.FINE)) {
            sLogger.fine("J2EE Deployment factory repository = " 
                    + repository.getAbsolutePath());
        }
        if (!repository.exists()) {
            sLogger.log(Level.SEVERE, "enterprise.deployment.backend.deplyomentManagerLoadFailure",
                new Object[] {"Cannot find any deployment manager"});
            return null;
        }
        
        return repository.listFiles();        
    }
    
    /**
     * Add a new deployment manager to our respository
     */
    public void addDeploymentFactory(File newDM) throws IOException {
        
        int number=1;
        // copy to the right location...
        File repository = new File(System.getProperty(J2EE_HOME)+File.separator+
            J2EE_DEPLOYMENT_MANAGER_REPOSITORY);
        File to = new File(repository, newDM.getName());
        while (to.exists()) {
            to = new File(repository, newDM.getName()+number);
            number++;
        }
        ArchivistUtils.copy(
            new BufferedInputStream(new FileInputStream(newDM)),
            new BufferedOutputStream(new FileOutputStream(to)));
        
        installDeploymentFactory(to);
    
    }
    
    
    protected void installDeploymentFactory(File installedDM) throws IOException {
        
        if (sLogger.isLoggable(Level.FINE)) {
            sLogger.fine("Installing Deployment factory = " 
                    + installedDM.getAbsolutePath());
        }
        
        // let's check first that we indeed have a valid 
        // deployment manager implementation
        
        /*
         *Declare the JarFile and Manifest but populate them inside the first try block.  This way the 
         *jar file can be closed right away to conserve resources.
         */
        JarFile jarFile = null;
        Manifest m = null;
        try {
            jarFile = new JarFile(installedDM);
            m = jarFile.getManifest();
        } finally {
            /*
             *The jarFile.close can throw IOException, but this method also throws that exception so there is no
             *need to catch it and wrap it here in the finally clause.
             */
            jarFile.close();
            jarFile = null;
        }
        String className = m.getMainAttributes().getValue(J2EE_DEPLOYMENT_MANAGER);
        URL[] urls = new URL[]{installedDM.toURI().toURL()};
        URLClassLoader urlClassLoader = new java.net.URLClassLoader(urls, getClass().getClassLoader());
        Class factory = null;
        try {
            factory=urlClassLoader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            sLogger.log(Level.SEVERE, "enterprise.deployment.backend.deplyomentManagerLoadFailure",
                new Object[] {"Unable to load declared DeploymentManagerFactory"});
            throw new IllegalArgumentException(className + " is not present in the " + installedDM.getName());
        }
        
        // Ok we have the class, let's instanciate it, check it and 
        // if everything is fine, register it to the DeploymentFactoryManager
        Object df = null;
        try {            
            df = factory.newInstance();
        } catch (Exception ie) {
            sLogger.log(Level.SEVERE, "enterprise.deployment.backend.deplyomentManagerLoadFailure",
                    new Object[]{className});
            sLogger.log(Level.SEVERE, "Error occurred", ie);
            throw new IllegalArgumentException("Cannot install " + installedDM.getName());
        }
        if (df instanceof DeploymentFactory) {
            DeploymentFactoryManager.getInstance().registerDeploymentFactory((DeploymentFactory) df);
        } else {
            throw new IllegalArgumentException("The " + className + 
                " declared as a DeploymentFactory does implement the DeploymentFactory interface");
        }
    }
    
    protected void initialize() {
                
        File[] elligibleFiles = getListOfDeploymentFactoryFiles();
        if (elligibleFiles==null) {
            return;
        }
           
        for (int i=0;i<elligibleFiles.length;i++) {
            try {
                installDeploymentFactory(elligibleFiles[i]);
            } catch(Exception ioe) {
                sLogger.log(Level.SEVERE, "enterprise.deployment.backend.deplyomentManagerLoadFailure",
                    new Object[] {elligibleFiles[i].getName()});
                sLogger.log(Level.SEVERE, "Error occurred", ioe);
            }
        }        
    }       
}
