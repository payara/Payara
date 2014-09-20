/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;

import java.io.File;

/**
 * Represents the configuration parameters required in order to create
 * and install a web module (web application) into the server runtime.
 */
public class WebModuleConfig {

    // ----------------------------------------------------- Instance Variables

    /**
     * The config bean containing the properties specified in the web-module
     * element in server.xml.
     */
    private Application _wmBean = null;

    /**
     * The parent directory under which the work directory for files generated
     * by the web application (i.e compiled JSP class files etc) resides.
     */
    private String _baseDir = null;

    /**
     * The work directory
     */
    private String workDir = null;
            
    /**
     * The directory under which the work directory for files generated
     * by the web application (i.e compiled JSP class files etc) resides.
     */
    private String _workDir = null;
    
    /**
     * The source directory for the web application
     */
    private File _dir = null;
    
    /**
     * The objectType property
     */
    private String _objectType = null;
    
    /**
     * The parent classloader for the web application.
     */
    private ClassLoader _parentLoader = null;

    /**
     * Deployment descriptor information about the web application.
     */
    private WebBundleDescriptorImpl _wbd = null;

    /** 
     * keep a list of virtual servers that this webmodule is associated with
     */
    private String _vsIDs;

    // START S1AS 6178005
    private String stubBaseDir;
    // END S1AS 6178005

    private ClassLoader _appClassLoader = null;

    private DeploymentContext deploymentContext;


    // ------------------------------------------------------------- Properties

    public ClassLoader getAppClassLoader() {
        return _appClassLoader;
    }

    public void setAppClassLoader(ClassLoader _appClassLoader) {
        this._appClassLoader = _appClassLoader;
    }
    
    /**
     * Set the elements of information specified in the web-module element
     * in server.xml.
     */
    public void setBean(Application wmBean) {
        _wmBean = wmBean;
    }


    /**
     * Return the configuration information specified in server.xml.
     */
    public Application getBean() {
        return _wmBean;
    }

    /**
     * Return the name of the web application (as specified in server.xml)
     *
     * @return [$appID:]$moduleID
     */
    public String getName() {
        String name = null;
        if (_wbd != null) {
            name = _wbd.getModuleID();
        }
        return name;
    }

    /**
     * Return the context path at which the web application is deployed.
     */
    public String getContextPath() {
        String ctxPath = null;
        if (_wbd != null) {
                ctxPath = _wbd.getContextRoot().trim();
                // Don't prefix a / if this web module is the default one
                // i.e. has an empty context-root
                if ((ctxPath.length() > 0) && !ctxPath.startsWith("/")) {
                    ctxPath = "/" + ctxPath;
                } else if (ctxPath.equals("/")) {
                    ctxPath = "";
                }
        }
        return ctxPath;
    }
    
    /**
     * Set the directory in which the web application is deployed.
     */
    public void setLocation(File sourceDir) {
        _dir = sourceDir;
    }

    /**
     * Return the directory in which the web application is deployed.
     */
    public File getLocation() {
        return _dir;
    }

    /**
     * Return the list of virtual servers to which the web application is
     * deployed.
     */
    public String getVirtualServers() {
        return _vsIDs;
    }

    /**
     * Return the list of virtual servers to which the web application is
     * deployed.
     */
    public void setVirtualServers(String virtualServers) {
        _vsIDs = virtualServers;
    }
    
    /**
     * Set the parent classloader for the web application.
     */
    public void setParentLoader(ClassLoader parentLoader) {
        _parentLoader = parentLoader;
    }

    /**
     * Return the parent classloader for the web application.
     */
    public ClassLoader getParentLoader() {
        return _parentLoader;
    }

    /**
     * Sets the deployment context for this web application.
     */
    public void setDeploymentContext(DeploymentContext deploymentContext) {
        synchronized (this) {
            this.deploymentContext = deploymentContext;
        }
    }

    /**
     * Gets the deployment context of this web application.
     */
    public DeploymentContext getDeploymentContext() {
        synchronized (this) {
            return deploymentContext;
        }
    }

    /**
     * Sets the work directory for this web application.
     */
    public synchronized void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    /**
     * Gets the work directory for this web application.
     *
     * The work directory is either
     *   generated/jsp/$appID/$moduleID
     * or
     *   generated/jsp/$moduleID
     */
    public synchronized String getWorkDir() {
        if (workDir == null) {
            if (deploymentContext != null &&
                    deploymentContext.getScratchDir(
                        "jsp") != null) {
                workDir = deploymentContext.getScratchDir("jsp").getPath();
            } else {
                workDir = getWebDir(_baseDir);
            }
        }
        return workDir;
    }

    // START S1AS 6178005
    /**
     * Gets the stub path of this web application.
     *
     * @return Stub path of this web application
     */
    public String getStubPath() {
        return getWebDir(stubBaseDir);
    }
    // END S1AS 6178005
    
    /**
     * Sets the parent of the work directory for this web application.
     *
     * The actual work directory is a subdirectory named after
     * the web application.
     *
     * @param baseDir The new base directory under which the actual work
     * directory will be created
     */
    public void setWorkDirBase(String baseDir) {
        synchronized (this) {
            _baseDir = baseDir;
        }
    }

    // START S1AS 6178005
    /**
     * Sets the base directory of this web application's stub path.
     *
     * @param stubBaseDir Stub path
     */
    public void setStubBaseDir(String stubBaseDir) {
        this.stubBaseDir = stubBaseDir;
    }
    // END S1AS 6178005

    /**
     * Return the object representation of the deployment descriptor specified
     * for the web application.
     */
    public WebBundleDescriptorImpl getDescriptor() {
        return _wbd;
    }

    /**
     * Set the deployment descriptor object describing the contents of the
     * web application.
     *
     * @param wbd The deployment descriptor object
     */
    public void setDescriptor(WebBundleDescriptorImpl wbd) {
        _wbd = wbd;
    }
        
    /**
     * Return the objectType property
     */
    public String getObjectType() {
        return _objectType;
    }

    /**
     * Set the objectType property.
     *
     * @param objectType objectType property
     */
    public void setObjectType(String objectType) {
        _objectType = objectType;
    }
        
    /*
     * Appends this web module's id to the given base directory path, and
     * returns it.
     *
     * @param baseDir Base directory path
     */
    private String getWebDir(String baseDir) {

        if (baseDir == null) {
            return null;
        }

        StringBuilder dir = new StringBuilder(baseDir);
        dir.append(File.separator);

        com.sun.enterprise.deployment.Application app = _wbd.getApplication();
        if (app != null && !app.isVirtual()) {
            dir.append(FileUtils.makeFriendlyFilename(
                app.getRegistrationName()));
            dir.append(File.separator);
            dir.append(FileUtils.makeFriendlyFilename(
                _wbd.getModuleDescriptor().getArchiveUri()));
        } else {
            dir.append(FileUtils.makeLegalNoBlankFileName(
                _wbd.getModuleID()));
        }

        return dir.toString();
    }
}
