/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.javaee.core.deployment;

import org.glassfish.api.deployment.*;
import org.glassfish.api.container.Container;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.api.JAXRPCCodeGenFacade;
import org.glassfish.loader.util.ASClassLoaderUtil;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.ApplicationVisitor;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.deployment.common.DeploymentProperties;
import com.sun.enterprise.config.serverbeans.ServerTags;
import org.jvnet.hk2.annotations.Optional;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.List;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 * Convenient superclass for JavaEE Deployer implementations.
 *
 */
public abstract class   JavaEEDeployer<T extends Container, U extends ApplicationContainer>
        implements Deployer<T, U> {

    @Inject
    protected ServerEnvironment env;

    @Inject
    protected ApplicationRegistry appRegistry;

    @Inject
    protected ServiceLocator habitat;

    @Inject @Named("application_undeploy") @Optional
    protected ApplicationVisitor undeploymentVisitor=null;

    @Inject Provider<JAXRPCCodeGenFacade> jaxrpcCodeGenFacadeProvider;

    private static final String APPLICATION_TYPE = "Application-Type";

    /**
     * Returns the meta data assocated with this Deployer
     *
     * @return the meta data for this Deployer
     */
    public MetaData getMetaData() {
        return new MetaData(false, null, null);
    }

     
    /**
     * Returns the classpath associated with this module
     * Can be used to compile generated cmp classes,
     * rmi stubs etc.
     *
     * @return the classpath for this module
     */
    protected String getModuleClassPath(DeploymentContext ctx) {
        // get the base module classpath
        // this includes the system classpath and deploy time lib libraries
        StringBuilder classpath = new StringBuilder
            (ASClassLoaderUtil.getModuleClassPath(habitat, ctx));

        try {
            // add the module dir
            classpath.append(ctx.getSourceDir().toURI().getPath());
            classpath.append(File.pathSeparator);

            // add the stubs dir
            classpath.append(ctx.getScratchDir("ejb").toURI().getPath());
            classpath.append(File.pathSeparator);

            // add the ear lib libraries if it's ear
            Application app = ctx.getModuleMetaData(Application.class); 
            if (!app.isVirtual()) {
                ReadableArchive parentArchive = 
                    ctx.getSource().getParentArchive();

                String compatProp = ctx.getAppProps().getProperty(
                    DeploymentProperties.COMPATIBILITY);

                List<URL> earLibURLs = 
                    ASClassLoaderUtil.getAppLibDirLibrariesAsList(new File(
                        parentArchive.getURI()), app.getLibraryDirectory(), 
                        compatProp);  

                for (URL url : earLibURLs) { 
                    classpath.append(url.toURI().getPath());
                    classpath.append(File.pathSeparator);
                }
            }
        } catch (Exception e) {
            // log a warning
        }

        return classpath.toString();
    }

    /*
     * Gets the common instance classpath, which is composed of the
     * pathnames of domain_root/lib/classes and
     * domain_root/lib/[*.jar|*.zip] (in this
     * order), separated by the path-separator character.
     * @return The instance classpath
     */
    protected String getCommonClassPath() {
        StringBuffer sb = new StringBuffer();

        File libDir = env.getLibPath();
        String libDirPath = libDir.getAbsolutePath();

        // Append domain_root/lib/classes
        sb.append(libDirPath + File.separator + "classes");
        sb.append(File.pathSeparator);

        // Append domain_root/lib/[*.jar|*.zip]
        String[] files = libDir.list();
        if (files != null) {
            for (int i=0; i<files.length; i++) {
                if (files[i].endsWith(".jar") || files[i].endsWith(".zip")) {
                    sb.append(libDirPath + File.separator + files[i]);
                    sb.append(File.pathSeparator);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Loads the meta date associated with the application.
     *
     * @param type type of metadata that this deployer has declared providing.
     * @param dc deployment context
     */
    public <V> V loadMetaData(Class<V> type, DeploymentContext dc) {
        return null;
    }

    /**
     * Prepares the application bits for running in the application server.
     * For certain cases, this is generating non portable
     * artifacts and other application specific tasks.
     * Failure to prepare should throw an exception which will cause the overall
     * deployment to fail.
     *
     * @param dc deployment context
     * @return true if the prepare phase was successful
     *
     */
    public boolean prepare(DeploymentContext dc) {
        try {
            ((ExtendedDeploymentContext)dc).prepareScratchDirs();


             //In jaxrpc it was required to run
             //Wscompile to generate the artifacts for clients too.
             //service-ref element can be in client in web.xml,  application-client.xml, sun-ejb-jar.xml
             //Fix for issue 16015 
            BundleDescriptor bundleDesc = dc.getModuleMetaData(BundleDescriptor.class);
            if (bundleDesc.hasWebServiceClients())     {
                JAXRPCCodeGenFacade jaxrpcCodeGenFacade = jaxrpcCodeGenFacadeProvider.get();
                if (jaxrpcCodeGenFacade != null) {
                    jaxrpcCodeGenFacade.run(habitat,dc,getModuleClassPath(dc), true) ;
                }
            }
            if (! dc.getCommandParameters(OpsParams.class).origin.isArtifactsPresent()) {
                         // only generate artifacts when there is no artifacts present

                generateArtifacts(dc);
            }
            return true;
        } catch (Exception ex) {
            // re-throw all the exceptions as runtime exceptions
            throw new RuntimeException(ex.getMessage(),ex);
        }
    }

   /**
     * Loads a previously prepared application in its execution environment and
     * return a ContractProvider instance that will identify this environment in
     * future communications with the application's container runtime.
     * @param container in which the application will reside
     * @param context of the deployment
     * @return an ApplicationContainer instance identifying the running application
     */
    public U load(T container, DeploymentContext context) {
        // reset classloader on DOL object before loading so we have a
        // valid classloader set on DOL
        Application app = context.getModuleMetaData(Application.class);
        if (app != null) {
            app.setClassLoader(context.getClassLoader());
        }

        return null;
    }

    protected void generateArtifacts(DeploymentContext dc)
        throws DeploymentException {
    }

    /**
     * Clean any files and artifacts that were created during the execution
     * of the prepare method.
     *
     * @param context deployment context
     */
    public void clean(DeploymentContext context) {
        if (undeploymentVisitor!=null) {

            String appName = context.getCommandParameters(OpsParams.class).name();
            Application app = getApplicationFromApplicationInfo(appName);
            if (app != null) {
                context.addModuleMetaData(app);
                   undeploymentVisitor.accept(app);
            }
        }
    }

    // get the object type from the application manifest file if
    // it is present. Application can be user application or system
    // appliction.
    protected String getObjectType(DeploymentContext context) {
        try{
            Manifest manifest = context.getSource().getManifest();
            if(manifest==null)  return null;
            Attributes attrs = manifest.getMainAttributes();
            return attrs.getValue(APPLICATION_TYPE);
        }catch(IOException e){
            // by default resource-type will be assigned "user".
            return null;
        }
    }

    protected Application getApplicationFromApplicationInfo(
        String appName) {
        ApplicationInfo appInfo = appRegistry.get(appName);
        if (appInfo == null) {
            return null;
        }
        return appInfo.getMetaData(Application.class);
    }

}
