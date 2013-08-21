/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.codegen;

import com.sun.appserv.ClassLoaderUtil;
import com.sun.enterprise.deployment.util.DOLUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URI;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.util.LocalStringManagerImpl;


// DOL imports
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.loader.util.ASClassLoaderUtil;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.io.JaxrpcMappingDeploymentDescriptorFile;
import com.sun.enterprise.deployment.JaxrpcMappingDescriptor.Mapping;
import com.sun.enterprise.deployment.util.ApplicationVisitor;
import com.sun.enterprise.deployment.util.AppClientVisitor;
import com.sun.enterprise.deployment.util.EjbBundleVisitor;
import com.sun.enterprise.deployment.util.ModuleContentLinker;
import com.sun.enterprise.deployment.*;
import org.glassfish.deployment.common.InstalledLibrariesResolver;
import org.glassfish.deployment.common.ClientArtifactsManager;
import org.glassfish.hk2.api.ServiceLocator;

import com.sun.enterprise.loader.ASURLClassLoader;

// TODO : Neds quivalent
//import com.sun.enterprise.deployment.backend.Deployer;

import javax.xml.rpc.Stub;

// web service impl imports
import org.glassfish.webservices.WsUtil;
import org.glassfish.webservices.WsCompile;

import org.glassfish.web.deployment.util.WebBundleVisitor;
import org.glassfish.web.deployment.util.WebServerInfo;

//JAX-RPC SPI
import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.tools.CompileTool;
import com.sun.xml.rpc.spi.tools.GeneratedFileInfo;
import com.sun.xml.rpc.spi.tools.GeneratorConstants;
import com.sun.xml.rpc.spi.tools.J2EEModelInfo;
import com.sun.xml.rpc.spi.tools.ModelFileModelInfo;
import com.sun.xml.rpc.spi.tools.ModelInfo;
import com.sun.xml.rpc.spi.tools.NamespaceMappingInfo;
import com.sun.xml.rpc.spi.tools.NamespaceMappingRegistryInfo;
import com.sun.xml.rpc.spi.tools.NoMetadataModelInfo;
import org.glassfish.webservices.LogUtils;

/**
 * This class is responsible for generating all non portable 
 * jax-rpc artifacts for a single .ear or standalone module.
 *
 * @author  Jerome Dochez
 */
public class JaxRpcRICodegen extends ModuleContentLinker
        implements JaxRpcCodegenAdapter, ApplicationVisitor, EjbBundleVisitor, WebBundleVisitor, AppClientVisitor  
{
    protected DeploymentContext context = null;
    protected ServiceLocator habitat = null;
    protected String moduleClassPath = null;

    // list of generated files
    ArrayList<String> files = new ArrayList<String>();

    private JaxRpcObjectFactory rpcFactory;

    private static final Logger logger = WsUtil.getDefaultLogger();

    // total number of times wscompile is invoked for the .ear or the
    // standalone module.
    private int wscompileInvocationCount = 0;

    // resources...
    private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(JaxRpcRICodegen.class);

    private CompileTool wscompileForAccept = null;
    private CompileTool wscompileForWebServices = null;

    //is this invocation for processing service references or services?
    private boolean processServiceReferences = false;

    private boolean hasWebServiceClients = false;

    /** Creates a new instance of JaxRpcRICodegen */
    public JaxRpcRICodegen(boolean processServiceReferences) {
        rpcFactory = JaxRpcObjectFactory.newInstance();
        this.processServiceReferences = processServiceReferences;
    }

    @Override
    public void run(ServiceLocator habitat, DeploymentContext context, String cp) throws Exception {
        rootLocation_ = new FileArchive();
        BundleDescriptor bundle = DOLUtils.getCurrentBundleForContext(context);
        if (bundle.hasWebServiceClients() && (bundle instanceof ApplicationClientDescriptor)) {
            hasWebServiceClients = true;
        }
        if(bundle.isStandalone()) {
            rootLocation_.open(context.getSourceDir().toURI());
        } else {
            rootLocation_.open(context.getSource().getParentArchive().getURI());
        }
        this.context = context;
        this.habitat = habitat;
        this.moduleClassPath = cp;
        Application application = context.getModuleMetaData(Application.class);
        application.visit((ApplicationVisitor) this);
    }

    /**
     * Visits a webs service reference
     */
    @Override
    public void accept(ServiceReferenceDescriptor serviceRef)  {

        if(!processServiceReferences) {
           return;
        }
        boolean codegenRequired = false;

        URL wsdlOverride = null;
        boolean wsdlOverriden = false;
        boolean jaxwsClient = false;
        super.accept(serviceRef);
        try {
            ClassLoader clr = serviceRef.getBundleDescriptor().getClassLoader();

            if ( serviceRef.getServiceInterface() != null ) {
                Class serviceInterface = clr.loadClass(serviceRef.getServiceInterface());

                if (javax.xml.ws.Service.class.isAssignableFrom(serviceInterface)) {
                    jaxwsClient = true;
                }
            }

            // Resolve port component links to target endpoint address.
            // We can't assume web service client is running in same VM
            // as endpoint in the intra-app case because of app clients.
            //
            // Also set port-qname based on linked port's qname if not
            // already set.
            for(Iterator ports = serviceRef.getPortsInfo().iterator(); ports.hasNext();) {
                ServiceRefPortInfo portInfo = (ServiceRefPortInfo) ports.next();

                if( portInfo.isLinkedToPortComponent() ) {
                    WebServiceEndpoint linkedPortComponent = portInfo.getPortComponentLink();

                    if (linkedPortComponent==null) {
                        throw new Exception(localStrings.getLocalString(
                                "enterprise.webservice.componentlinkunresolved",
                                "The port-component-link {0} cannot be resolved",
                                new Object[] {portInfo.getPortComponentLinkName()}));
                    }
                    WsUtil wsUtil = new WsUtil();
                    WebServerInfo wsi = wsUtil.getWebServerInfoForDAS();
                    URL rootURL = wsi.getWebServerRootURL(linkedPortComponent.isSecure());
                    URL actualAddress = linkedPortComponent.composeEndpointAddress(rootURL);
                    if(jaxwsClient) {
                        portInfo.addStubProperty(javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                actualAddress.toExternalForm());
                    } else {
                        portInfo.addStubProperty(Stub.ENDPOINT_ADDRESS_PROPERTY, actualAddress.toExternalForm());
                    }
                    if (serviceRef.getBundleDescriptor().getModuleType().equals(DOLUtils.carType())) {
                        wsdlOverride = serviceRef.getWsdlOverride();
                        if (wsdlOverride!=null) {
                            wsdlOverriden = true;
                            serviceRef.setWsdlOverride(linkedPortComponent.getWebService().getWsdlFileUrl());
                        }
                    }
                }
            }

            // If this is a post JAXRPC-1.1 based web service, then no need for code gen etc etc
            if(jaxwsClient) {
                return;
            }

            if( serviceRef.hasGeneratedServiceInterface() ) {

                if( serviceRef.hasWsdlFile() && serviceRef.hasMappingFile() ) {
                    codegenRequired = true;
                } else {
                    throw new Exception
                            ("Deployment error for service-ref " + serviceRef.getName()
                                    + ".\nService references with generated service " +
                                    "interface must include WSDL and mapping information.");
                }

            } else {

                if( serviceRef.hasWsdlFile() ) {
                    if( serviceRef.hasMappingFile() ) {
                        codegenRequired = true;
                    } else {
                        throw new Exception
                                ("Deployment error for service-ref " + serviceRef.getName()
                                        + ".\nService references with wsdl must also have " +
                                        "mapping information.");
                    }
                }
            }

            if( codegenRequired ) {
                ModelInfo modelInfo = createModelInfo(serviceRef);

                /*
                 * If clients exist, force regeneration so that the
                 * ClientArtifactsManager is populated. Issue 10612.
                 */
                String args[] = createJaxrpcCompileArgs(
                    false, hasWebServiceClients);

                CompileTool wscompile =
                        rpcFactory.createCompileTool(System.out, "wscompile");
                wscompileForAccept = wscompile;
                WsCompile delegate =
                        new WsCompile(wscompile, serviceRef);
                delegate.setModelInfo(modelInfo);
                wscompile.setDelegate(delegate);

                jaxrpc(args, delegate, serviceRef, files);
                if (hasWebServiceClients)   {
                    addArtifactsForAppClient();
                }
            }
            if (wsdlOverriden) {
                serviceRef.setWsdlOverride(wsdlOverride);
            }
        } catch(IllegalStateException e) {
            //do nothing
           logger.info("Attempting to add artifacts for appClient after artifacts were generated" +
                   " "+e.getMessage());

        }  catch(Exception e) {
            RuntimeException re = new RuntimeException(e.getMessage());
            re.initCause(e);
            throw re;
        }
    }

    private void addArtifactsForAppClient(){
        ClientArtifactsManager cArtifactsManager = ClientArtifactsManager.get(context);
        for (int i = 0; i < files.size(); i ++) {
            URI baseURI = context.getScratchDir("ejb").toURI();
            File file = new File(files.get(i));
            URI artifact = baseURI.relativize(file.toURI());
            //Fix for issue 9734
            if (!cArtifactsManager.contains(baseURI,artifact) ){
               cArtifactsManager.add(baseURI, artifact);
            }
        }
    }

    /**
     * visits a web service definition
     * @param webService
     */
    @Override
    public void accept(WebService webService) {
        if(processServiceReferences) {
           return;
        }

        if (!webServiceInContext(webService)) {
            return;
        }
        super.accept(webService);
        try {
            if((new WsUtil()).isJAXWSbasedService(webService)) {
                WsUtil wsUtil = new WsUtil();
                Collection<WebServiceEndpoint> endpoints = webService.getEndpoints();
                for(WebServiceEndpoint ep : endpoints) {
                    if( ep.implementedByWebComponent() ) {
                        wsUtil.updateServletEndpointRuntime(ep);
                    } else {
                        wsUtil.validateEjbEndpoint(ep);
                    }
                }
                //wsImport(webService,  files);
            } else {
                jaxrpcWebService(webService, files);
            }
        } catch(Exception e) {
            RuntimeException ge =new RuntimeException(e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    @Override
    public Iterator getListOfBinaryFiles() {
        return files.iterator();
    }

    @Override
    public Iterator getListOfSourceFiles() {
        // for now I do not maintain those
        return null;
    }

    /**
     *Releases resources used during the code gen and compilation.
     */
    @Override
    public void done() {
//        done(CompileTool) is now invoked after each compilation is complete
//        from inside the jaxrpc method.  Otherwise, multiple uses of jaxrpc could
//        cause continued file locking on Windows since only the last one was 
//        recorded in the wscompileForxxx variables.
//        
//        done(wscompileForAccept);
//        done(wscompileForWebServices);
    }

    /**
     *Navigates to the URLClassLoader used by the jaxrpc compilation and 
     *releases it.
     *@param wscompile the CompileTool whose loader is to be released
     */
    private void done(CompileTool wscompile) {
        /*
         *Follow the object graph to the loader: 
         *basically CompileTool -> ProcessorEnvironment -> the URLClassLoader.
         */
        if (wscompile != null && wscompile instanceof com.sun.xml.rpc.tools.wscompile.CompileTool) {
            com.sun.xml.rpc.tools.wscompile.CompileTool compileTool = (com.sun.xml.rpc.tools.wscompile.CompileTool) wscompile;
            com.sun.xml.rpc.spi.tools.ProcessorEnvironment env = compileTool.getEnvironment();
            if (env != null && env instanceof com.sun.xml.rpc.processor.util.ProcessorEnvironment) {
                com.sun.xml.rpc.processor.util.ProcessorEnvironment typedEnv = (com.sun.xml.rpc.processor.util.ProcessorEnvironment) env;
                java.net.URLClassLoader urlCL = typedEnv.getClassLoader();
                ClassLoaderUtil.releaseLoader(urlCL);
            }
        }
    }

    private JaxrpcMappingDescriptor getJaxrpcMappingInfo(URL mappingFileUrl,
                                                         Descriptor desc)
            throws Exception {
        JaxrpcMappingDescriptor mappingDesc = null;

        InputStream is = null;
        try {
            is = mappingFileUrl.openStream();
            JaxrpcMappingDeploymentDescriptorFile jaxrpcDD =
                    new JaxrpcMappingDeploymentDescriptorFile();

            // useful for validation errors...
            if (desc instanceof ServiceReferenceDescriptor) {
                ServiceReferenceDescriptor srd = (ServiceReferenceDescriptor) desc;
                jaxrpcDD.setDeploymentDescriptorPath(srd.getMappingFileUri());
                jaxrpcDD.setErrorReportingString(srd.getBundleDescriptor().getModuleDescriptor().getArchiveUri());
            }
            if (desc instanceof WebService) {
                WebService ws = (WebService) desc;
                jaxrpcDD.setDeploymentDescriptorPath(ws.getMappingFileUri());
                jaxrpcDD.setErrorReportingString(ws.getBundleDescriptor().getModuleDescriptor().getArchiveUri());
            }
            //jaxrpcDD.setXMLValidationLevel(Deployer.getValidationLevel());
            jaxrpcDD.setXMLValidationLevel("none");
            mappingDesc = JaxrpcMappingDescriptor.class.cast(jaxrpcDD.read(is));
        } finally {
            if( is != null ) {
                is.close();
            }
        }

        return mappingDesc;
    }

    private boolean isJaxrpcRIModelFile(URL mappingFileUrl) {
        boolean isModel = false;
        InputStream is  = null;
        try {
            is = mappingFileUrl.openStream();
            isModel = rpcFactory.createXMLModelFileFilter().isModelFile(is);
        } catch(Throwable t) {
        } finally {
            if( is != null ) {
                try {
                    is.close();
                } catch(Exception e) {}
            }
        }
        return isModel;
    }

    private ModelInfo createModelInfo(WebService webService)
            throws Exception {

        ModelInfo modelInfo = null;
        URL mappingFileUrl = webService.getMappingFile().toURL();
        modelInfo = createModelFileModelInfo(mappingFileUrl);
        if( isJaxrpcRIModelFile(mappingFileUrl) ) {
            debug("000. JaxrpcRIModelFile.");
            modelInfo = createModelFileModelInfo(mappingFileUrl);
        } else {
            JaxrpcMappingDescriptor mappingDesc =
                    getJaxrpcMappingInfo(mappingFileUrl, webService);
            if( mappingDesc.isSimpleMapping() ) {
                debug("111. SimpleMapping.");
                modelInfo = createNoMetadataModelInfo(webService, mappingDesc);
            } else {
                debug("222. FullMapping .");
                modelInfo = createFullMappingModelInfo(webService);
            }
        }

        return modelInfo;
    }

    private ModelInfo createModelInfo(ServiceReferenceDescriptor serviceRef)
            throws Exception {

        ModelInfo modelInfo = null;
        URL mappingFileUrl = serviceRef.getMappingFile().toURL();
        if( isJaxrpcRIModelFile(mappingFileUrl) ) {
            modelInfo = createModelFileModelInfo(mappingFileUrl);
        } else {
            JaxrpcMappingDescriptor mappingDesc =
                    getJaxrpcMappingInfo(mappingFileUrl, serviceRef);
            if( mappingDesc.isSimpleMapping() &&
                    serviceRef.hasGeneratedServiceInterface() ) {
                // model info for this modeler requires generated service 
                // interface name.
                modelInfo = createNoMetadataModelInfo(serviceRef, mappingDesc);
            } else {
                modelInfo = createFullMappingModelInfo(serviceRef);
            }
        }

        return modelInfo;
    }

    private ModelFileModelInfo createModelFileModelInfo(URL modelFileUrl)
            throws Exception {

        ModelFileModelInfo modelInfo = rpcFactory.createModelFileModelInfo();
        modelInfo.setLocation(modelFileUrl.toExternalForm());

        return modelInfo;
    }

    private J2EEModelInfo createFullMappingModelInfo(WebService webService)
            throws Exception {

        URL mappingFileUrl = webService.getMappingFile().toURL();
        URL wsdlFileUrl = webService.getWsdlFileUrl();

        return createFullMappingModelInfo(mappingFileUrl, wsdlFileUrl);
    }

    private J2EEModelInfo createFullMappingModelInfo
            (ServiceReferenceDescriptor serviceRef) throws Exception {

        URL mappingFileUrl = serviceRef.getMappingFile().toURL();
        URL wsdlFileUrl = serviceRef.hasWsdlOverride() ?
                serviceRef.getWsdlOverride() : serviceRef.getWsdlFileUrl();
        return createFullMappingModelInfo(mappingFileUrl, wsdlFileUrl);
    }

    private J2EEModelInfo createFullMappingModelInfo
            (URL mappingFile, URL wsdlFile) throws Exception {

        J2EEModelInfo modelInfo = rpcFactory.createJ2EEModelInfo(mappingFile);
        modelInfo.setLocation(wsdlFile.toExternalForm());
        // java package name not used
        modelInfo.setJavaPackageName("package_ignored");
        return modelInfo;

    }

    private NoMetadataModelInfo createNoMetadataModelInfo
            (WebService webService, JaxrpcMappingDescriptor mappingDesc)
            throws Exception {

        NoMetadataModelInfo modelInfo = rpcFactory.createNoMetadataModelInfo();
        URL wsdlFileUrl = webService.getWsdlFileUrl();

        Collection endpoints = webService.getEndpoints();
        if( endpoints.size() != 1 ) {
            throw new Exception
                    ("Deployment code generation error for webservice " +
                            webService.getName() + ". " +
                            " jaxrpc-mapping-file is required if web service has " +
                            "multiple endpoints");
        }

        WebServiceEndpoint endpoint = (WebServiceEndpoint)
                endpoints.iterator().next();

        modelInfo.setLocation(wsdlFileUrl.toExternalForm());
        modelInfo.setInterfaceName(endpoint.getServiceEndpointInterface());
        modelInfo.setPortName(endpoint.getWsdlPort());

        addNamespaceMappingRegistry(modelInfo, mappingDesc);

        return modelInfo;
    }

    private void addNamespaceMappingRegistry
            (NoMetadataModelInfo modelInfo, JaxrpcMappingDescriptor mappingDesc) {

        NamespaceMappingRegistryInfo namespaceRegistry =
                rpcFactory.createNamespaceMappingRegistryInfo();

        modelInfo.setNamespaceMappingRegistry(namespaceRegistry);

        Collection mappings = mappingDesc.getMappings();
        for(Iterator iter = mappings.iterator(); iter.hasNext();) {
            Mapping next = (Mapping) iter.next();
            NamespaceMappingInfo namespaceInfo =
                    rpcFactory.createNamespaceMappingInfo(next.getNamespaceUri(),
                            next.getPackage());
            namespaceRegistry.addMapping(namespaceInfo);
        }
    }

    private NoMetadataModelInfo createNoMetadataModelInfo
            (ServiceReferenceDescriptor serviceRef,
             JaxrpcMappingDescriptor mappingDesc) throws Exception {

        NoMetadataModelInfo modelInfo = rpcFactory.createNoMetadataModelInfo();
        URL wsdlFile = serviceRef.hasWsdlOverride() ?
                serviceRef.getWsdlOverride() : serviceRef.getWsdlFileUrl();
        modelInfo.setLocation(wsdlFile.toExternalForm());

        // Service endpoint interface is required.  Parse generated
        // service interface for it since we can't count on SEI
        // having been listed in standard deployment information.
        WsUtil wsUtil = new WsUtil();
        String serviceInterfaceName = serviceRef.getServiceInterface();

        ClassLoader cl = context.getModuleMetaData(Application.class).getClassLoader();
        if (cl instanceof ASURLClassLoader) {
            String modClassPath = ASClassLoaderUtil.getModuleClassPath(habitat, context);
            List<URL> moduleList = ASClassLoaderUtil.getURLsFromClasspath(modClassPath, File.pathSeparator, null);
            for (Iterator<URL> itr=moduleList.iterator();itr.hasNext();) {
                ((ASURLClassLoader) cl).appendURL(itr.next());
            }
        }

        Class serviceInterface = cl.loadClass(serviceInterfaceName);
        Collection seis = wsUtil.getSEIsFromGeneratedService(serviceInterface);

        if( seis.size() == 0 ) {
            throw new Exception("Invalid Generated Service Interface "
                    + serviceInterfaceName + " . ");
        } else if( seis.size() > 1 ) {
            throw new Exception("Deployment error : If no " +
                    "jaxrpc-mapping file is provided, " +
                    "Generated Service Interface must have"
                    +" only 1 Service Endpoint Interface");
        }

        String serviceEndpointInterface = (String) seis.iterator().next();
        modelInfo.setInterfaceName(serviceEndpointInterface);

        addNamespaceMappingRegistry(modelInfo, mappingDesc);

        return modelInfo;
    }

    private boolean keepJaxrpcGeneratedFile(String fileType, Descriptor desc) {
        boolean keep = true;
        if( (fileType.equals(GeneratorConstants.FILE_TYPE_WSDL) ||
                fileType.equals(GeneratorConstants.FILE_TYPE_REMOTE_INTERFACE)) ) {
            keep = false;
        } else if( fileType.equals(GeneratorConstants.FILE_TYPE_SERVICE ) ) {
            // Only keep the service interface if this is a service reference
            // with generic service interface.  In this case, the interface
            // is generated during deployment instead of being packaged in
            // the module.
            keep = (desc instanceof ServiceReferenceDescriptor) &&
                    ((ServiceReferenceDescriptor)desc).hasGenericServiceInterface();
        }

        return keep;
    }

    // dummy file for jax-rpc wscompile bug
    File dummyConfigFile=null;

    private String[] createJaxrpcCompileArgs(boolean generateTies,
        boolean forceRegen) throws IOException {
        
        int numJaxrpcArgs = 11;
        if (logger.isLoggable(Level.FINE) ) {
            numJaxrpcArgs = 16;
        }
        if (forceRegen) {
            numJaxrpcArgs--;
        }

        // If we need to run wscompile more than once per .ear or
        // standalone module, use the -infix option to reduce the
        // chances that generated non-portable jaxrpc artifacts will clash
        // with generated artifacts from other service-refs and endpoints
        // loaded by the same classloader at runtime.   
        wscompileInvocationCount++;
        String infix = null;

        if( wscompileInvocationCount > 1 ) {
            numJaxrpcArgs++;
            infix = wscompileInvocationCount + "";
        }

        String[] jaxrpcArgs = new String[numJaxrpcArgs];
        int jaxrpcCnt = 0;

        if( dummyConfigFile == null ) {
            dummyConfigFile = File.createTempFile("dummy_wscompile_config",
                    "config");
            dummyConfigFile.deleteOnExit();
        }

        // wscompile doesn't support the -extdirs option, so the best we
        // can do is prepend the ext dir jar files to the classpath.
        String optionalDependencyClassPath =
                InstalledLibrariesResolver.getExtDirFilesAsClasspath();
        if(optionalDependencyClassPath.length() > 0) {
            moduleClassPath = optionalDependencyClassPath +
                    File.pathSeparatorChar + moduleClassPath;
        }
        
        // Could also check if getSourceDir() ends in 'war,' but
        // this is a little more general.
        String moduleBasePath = context.getSourceDir().getAbsolutePath();
        String moduleWebInfPath = moduleBasePath +
            File.separatorChar + "WEB-INF" +
            File.separatorChar + "classes";
        File moduleWebInfFile = new File(moduleWebInfPath);
        if (moduleWebInfFile.exists()) {
            moduleClassPath = moduleWebInfPath +
                File.pathSeparatorChar + moduleClassPath;
        } else {
            moduleClassPath = moduleBasePath +
                File.pathSeparatorChar + moduleClassPath;
        }
        
        if (!context.getScratchDir("ejb").mkdirs() && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.DIR_EXISTS, context.getScratchDir("ejb"));
        }

        jaxrpcArgs[jaxrpcCnt++] = generateTies ? "-gen:server" : "-gen:client";

        // Prevent wscompile from regenerating portable classes that are
        // already packaged within the deployed application.
        if (!forceRegen) {
            jaxrpcArgs[jaxrpcCnt++] = "-f:donotoverride";
        }

        if( infix != null ) {
            jaxrpcArgs[jaxrpcCnt++] = "-f:infix:" + infix;
        }

        jaxrpcArgs[jaxrpcCnt++] = "-classpath";
        jaxrpcArgs[jaxrpcCnt++] = moduleClassPath;

        if (logger.isLoggable(Level.FINE)) {
            long timeStamp = System.currentTimeMillis();
            jaxrpcArgs[jaxrpcCnt++] = "-Xdebugmodel:" +
                    context.getScratchDir("ejb") + File.separator + "debugModel.txt." +
                    timeStamp;
            jaxrpcArgs[jaxrpcCnt++] = "-Xprintstacktrace";
            jaxrpcArgs[jaxrpcCnt++] = "-model";
            jaxrpcArgs[jaxrpcCnt++] =
                    context.getScratchDir("ejb") + File.separator + "debugModel.model" +
                            timeStamp;
            jaxrpcArgs[jaxrpcCnt++] = "-verbose";
        }

        jaxrpcArgs[jaxrpcCnt++] = "-s";
        jaxrpcArgs[jaxrpcCnt++] = context.getScratchDir("ejb").getAbsolutePath();
        jaxrpcArgs[jaxrpcCnt++] = "-d";
        jaxrpcArgs[jaxrpcCnt++] = context.getScratchDir("ejb").getAbsolutePath();
        jaxrpcArgs[jaxrpcCnt++] = "-keep";
        jaxrpcArgs[jaxrpcCnt++] = "-g";

        // config file is not used, but it must be an existing file or it
        // will not pass CompileTool argument validation.
        jaxrpcArgs[jaxrpcCnt++] = dummyConfigFile.getPath();

        if ( logger.isLoggable(Level.FINE)) {
            for ( int i = 0; i < jaxrpcArgs.length; i++ ) {
                logger.fine(jaxrpcArgs[i]);
            }
        }

        return jaxrpcArgs;
    }

    private void jaxrpc(String[] args, WsCompile wsCompile, Descriptor desc,
                        ArrayList<String> files)
            throws Exception {

        try {
            if (logger.isLoggable(Level.FINE)) {
                debug("---> ARGS = ");
                for (int i = 0; i < args.length; i++) {
                    logger.fine(args[i] + "; ");
                }
            }
            boolean compiled = wsCompile.getCompileTool().run(args);
            done(wsCompile.getCompileTool());
            if( compiled ) {
                Iterator generatedFiles =
                        wsCompile.getGeneratedFiles().iterator();

                while(generatedFiles.hasNext()) {
                    GeneratedFileInfo next = (GeneratedFileInfo)
                            generatedFiles.next();
                    String fileType = next.getType();
                    File file = next.getFile();
                    String origPath = file.getPath();
                    if( origPath.endsWith(".java") ) {
                        int javaIndex = origPath.lastIndexOf(".java");
                        String newPath = origPath.substring(0, javaIndex) +
                                ".class";
                        if( keepJaxrpcGeneratedFile(fileType, desc) ) {
                            files.add(newPath);
                        }
                    }
                }
            } else {
                throw new Exception("jaxrpc compilation exception");
            }
        } catch (Throwable t) {
            Exception ge =
                    new Exception(t.getMessage());
            ge.initCause(t);
            throw ge;
        }
    }

    private void jaxrpcWebService(WebService webService, ArrayList<String> files)
            throws Exception {

        if((webService.getWsdlFileUrl() == null) ||
                (webService.getMappingFileUri() == null)) {
            throw new Exception(localStrings.getLocalString(
                    "enterprise.webservice.jaxrpcFilesNotFound",
                    "Service {0} seems to be a JAXRPC based web service but without "+
                            "the mandatory WSDL and Mapping file. Deployment cannot proceed",
                    new Object[] {webService.getName()}));
        }
        ModelInfo modelInfo = createModelInfo(webService);
        String args[] = createJaxrpcCompileArgs(true, false);

        CompileTool wscompile =
                rpcFactory.createCompileTool(System.out, "wscompile");
        wscompileForWebServices = wscompile;
        WsCompile delegate = new WsCompile(wscompile, webService);
        delegate.setModelInfo(modelInfo);
        wscompile.setDelegate(delegate);

        jaxrpc(args, delegate, webService, files);
    }

    private void debug(String msg) {
        if (logger.isLoggable(Level.FINE) ) {
            logger.fine("[JaxRpcRICodegen] --> " + msg);
        }
    }

    /*
     * The run() method calls visit() on the entire application.
     * If this is being called in the context of a submodule within
     * the application, then accept(WebService) will be called
     * for every web service in the app, not just in the current
     * module. We want to ignore the unrelated web services --
     * they will be handled when their module is loaded.
     */
    private boolean webServiceInContext(WebService webService) {
        BundleDescriptor contextBundleDescriptor =
            context.getModuleMetaData(BundleDescriptor.class);
        String moduleId = contextBundleDescriptor.getModuleID();
        return moduleId.equals(webService.getBundleDescriptor().getModuleID());
    }

    public void accept (BundleDescriptor descriptor) {
        if (descriptor instanceof Application) {
            Application application = (Application)descriptor;
            for (BundleDescriptor ebd : application.getBundleDescriptorsOfType(DOLUtils.ejbType())) {
                ebd.visit(getSubDescriptorVisitor(ebd));
            }

            for (BundleDescriptor wbd : application.getBundleDescriptorsOfType(DOLUtils.warType())) {
                if (wbd != null) {
                    wbd.visit(getSubDescriptorVisitor(wbd));
                }
            }

            for (BundleDescriptor acd : application.getBundleDescriptorsOfType(DOLUtils.carType())) {
                acd.visit(getSubDescriptorVisitor(acd));
            }
        } else {
            super.accept(descriptor);
        }
    }


    /**
     * visit an application object
     * @param the application descriptor
     */
    public void accept(Application application) {
    }

    /**
     * visits an ejb bundle descriptor
     * @param an ejb bundle descriptor
     */
    public void accept(EjbBundleDescriptor bundleDescriptor) {
    }


    /**
     * visits a appclient descriptor
     * @param appclientdescriptor
     * get the visitor for its sub descriptor
     * @param sub descriptor to return visitor for
     */
    public void accept(ApplicationClientDescriptor appclientdescriptor) {
    }

    /**
     * visit a web bundle descriptor
     *
     * @param the web bundle descriptor
     */
    public void accept(WebBundleDescriptor descriptor) {
    }
}
