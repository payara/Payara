/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.Application;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.deployment.common.ApplicationConfigInfo;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.GenericHandler;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.web.LogFacade;
import org.glassfish.web.jsp.JSPCompiler;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.lang.String;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web module deployer.
 *
 * @author jluehe
 * @author Jerome Dochez
 */
@Service
public class WebDeployer extends JavaEEDeployer<WebContainer, WebApplication>{

    private static final Logger logger = LogFacade.getLogger();

    private static final ResourceBundle rb = logger.getResourceBundle();

    @Inject
    ServerContext sc;

    @Inject
    Domain domain;

    @Inject
    RequestDispatcher dispatcher;

    /**
     * Constructor
     */
    public WebDeployer() {
    }

    /**
     * Returns the meta data assocated with this Deployer
     *
     * @return the meta data for this Deployer
     */
    @Override
    public MetaData getMetaData() {
        return new MetaData(false,
                new Class[] {WebBundleDescriptorImpl.class}, new Class[] {Application.class});
    }

    public <V> V loadMetaData(Class<V> type, DeploymentContext dc) {

        WebBundleDescriptorImpl wbd = dc.getModuleMetaData(WebBundleDescriptorImpl.class);

        if (wbd.isStandalone()) {
            // the context root should be set using the following precedence
            // for standalone web module
            // 1. User specified value through DeployCommand
            // 2. Context root value specified through sun-web.xml
            // 3. Context root from last deployment if applicable
            // 4. The default context root which is the archive name 
            //    minus extension
            DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
            String contextRoot = params.contextroot;
            if(contextRoot==null) {
                contextRoot = wbd.getContextRoot();
                if("".equals(contextRoot))
                    contextRoot = null;
            }
            if(contextRoot==null) {
                contextRoot = params.previousContextRoot;
            }
            if(contextRoot==null)
                contextRoot = ((GenericHandler)dc.getArchiveHandler()).getDefaultApplicationNameFromArchiveName(dc.getOriginalSource());

            if (!contextRoot.startsWith("/")) {
                contextRoot = "/" + contextRoot;
            }
            wbd.setContextRoot(contextRoot);
            wbd.setName(params.name());

            // set the context root to deployment context props so this value
            // will be persisted in domain.xml
            dc.getAppProps().setProperty(ServerTags.CONTEXT_ROOT, contextRoot);
        } 

        return null;
    }

    private WebModuleConfig loadWebModuleConfig(DeploymentContext dc) {
        
        WebModuleConfig wmInfo = new WebModuleConfig();
        
        try {
            DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
            wmInfo.setDescriptor(dc.getModuleMetaData(WebBundleDescriptorImpl.class));
            wmInfo.setVirtualServers(params.virtualservers);
            wmInfo.setLocation(dc.getSourceDir());
            wmInfo.setObjectType(dc.getAppProps().getProperty(ServerTags.OBJECT_TYPE));
        } catch (Exception ex) {
            String msg = rb.getString(LogFacade.UNABLE_TO_LOAD_CONFIG);
            msg = MessageFormat.format(msg, wmInfo.getName());
            logger.log(Level.WARNING, msg, ex);
        }
        
        return wmInfo;
        
    }
    
    @Override
    protected void generateArtifacts(DeploymentContext dc) 
        throws DeploymentException {
        DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
        if (params.precompilejsp) {
            //call JSPCompiler... 
            runJSPC(dc);
        }
    }

         
    @Override
    public WebApplication load(WebContainer container, DeploymentContext dc) {
        super.load(container, dc);
        WebBundleDescriptorImpl wbd = dc.getModuleMetaData(
            WebBundleDescriptorImpl.class);
        if (wbd != null) {
            wbd.setClassLoader(dc.getClassLoader());
        }

        WebModuleConfig wmInfo = loadWebModuleConfig(dc);
        WebApplication webApp = new WebApplication(container, wmInfo,
                new ApplicationConfigInfo(dc.getAppProps()));
        return webApp;
    }

    
    public void unload(WebApplication webApplication, DeploymentContext dc) {

    }
        
    /**
     * This method setups the in/outDir and classpath and invoke
     * JSPCompiler.
     * @param dc - DeploymentContext to get command parameters and
     *             source directory and compile jsp directory.
     * @throws DeploymentException if JSPCompiler is unsuccessful.
     */
    void runJSPC(final DeploymentContext dc) throws DeploymentException {
        final WebBundleDescriptorImpl wbd = dc.getModuleMetaData(
            WebBundleDescriptorImpl.class);
        try {
            final File outDir = dc.getScratchDir(env.kCompileJspDirName);
            final File inDir  = dc.getSourceDir();

            StringBuilder classpath = new StringBuilder(
                super.getCommonClassPath());
            classpath.append(File.pathSeparatorChar);
            classpath.append(ASClassLoaderUtil.getModuleClassPath(
                    sc.getDefaultServices(),
                    wbd.getApplication().getName(), 
                    dc.getCommandParameters(
                        DeployCommandParameters.class).libraries)); 
            JSPCompiler.compile(inDir, outDir, wbd, classpath.toString(), sc);
        } catch (DeploymentException de) {
            String msg = rb.getString(LogFacade.JSPC_FAILED);
            msg = MessageFormat.format(msg, wbd.getApplication().getName());
            logger.log(Level.SEVERE, msg, de);
            throw de;
        }
    }
}
