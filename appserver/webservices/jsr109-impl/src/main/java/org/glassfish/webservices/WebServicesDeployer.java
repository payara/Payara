/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;


import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.archivist.Archivist;
import org.glassfish.api.deployment.archive.ArchiveType;
import com.sun.enterprise.deployment.web.AppListenerDescriptor;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.tools.ws.util.xml.XmlUtil;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.loader.util.ASClassLoaderUtil;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.api.container.RequestDispatcher;
import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.web.deployment.descriptor.AppListenerDescriptorImpl;
import org.glassfish.web.deployment.util.WebServerInfo;
import org.glassfish.webservices.deployment.WebServicesDeploymentMBean;
import org.glassfish.javaee.core.deployment.JavaEEDeployer;
import org.glassfish.internal.api.JAXRPCCodeGenFacade;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.servlet.SingleThreadModel;
import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Webservices module deployer. This is loaded from WebservicesContainer
 *
 * @author Bhakti Mehta
 * @author Rama Pulavarthi
 */
@Service
public class WebServicesDeployer extends JavaEEDeployer<WebServicesContainer,WebServicesApplication> {
    public static final WebServiceDeploymentNotifier deploymentNotifier =
            new WebServiceDeploymentNotifierImpl();

    public static WebServiceDeploymentNotifier getDeploymentNotifier() {
        return deploymentNotifier;
    }

    private static final Logger logger = LogUtils.getLogger();

    private ResourceBundle rb = logger.getResourceBundle();

    @Inject
    private RequestDispatcher dispatcher;

    @Inject
    private ArchiveFactory archiveFactory;

    /**
     * Constructor
     */
    public WebServicesDeployer() {
    }


    protected void cleanArtifacts(DeploymentContext deploymentContext) throws DeploymentException {

    }

   

    /**
     * Prepares the application bits for running in the application server.
     * For certain cases, this is exploding the jar file to a format the
     * ContractProvider instance is expecting, generating non portable
     * artifacts and other application specific tasks.
     * Failure to prepare should throw an exception which will cause the overall
     * deployment to fail.
     *
     * @param dc deployment context
     * @return true if the prepare phase was successful
     *
     */
    @Override
    public boolean prepare(DeploymentContext dc) {
        try {

            Application app = dc.getModuleMetaData(Application.class);
            if (app==null) {
                // hopefully the DOL gave a good message of the failure...
                logger.log(Level.SEVERE, LogUtils.FAILED_LOADING_DD);
                return false;
            }
            BundleDescriptor bundle = DOLUtils.getCurrentBundleForContext(dc);

            String moduleCP = getModuleClassPath(dc);
            final List<URL> moduleCPUrls = ASClassLoaderUtil.getURLsFromClasspath(moduleCP, File.pathSeparator, null);
            final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            URLClassLoader newCl = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
                @Override
                public URLClassLoader run() {
                    return new URLClassLoader(ASClassLoaderUtil.convertURLListToArray(moduleCPUrls), oldCl);
                }
            });


            Thread.currentThread().setContextClassLoader(newCl);
            WebServicesDescriptor wsDesc = bundle.getWebServices();
            for (WebService ws : wsDesc.getWebServices()) {
                if ((new WsUtil()).isJAXWSbasedService(ws)){
                    setupJaxWSServiceForDeployment(dc, ws);
                } else {
                    JAXRPCCodeGenFacade facade= habitat.getService(JAXRPCCodeGenFacade.class);
                    if (facade != null) {
                        facade.run(habitat, dc, moduleCP, false);
                    }  else {
                        throw new DeploymentException(rb.getString(LogUtils.JAXRPC_CODEGEN_FAIL)) ;
                    }
                }
            }
            doWebServicesDeployment(app,dc);
            Thread.currentThread().setContextClassLoader(oldCl);
            WebServicesContainer container = habitat.getService(WebServicesContainer.class);
            WebServicesDeploymentMBean bean = container.getDeploymentBean();
            WebServiceDeploymentNotifier notifier = getDeploymentNotifier();
            bean.deploy(wsDesc,notifier);
            return true;
        } catch (Exception ex) {
            RuntimeException re = new RuntimeException(ex.getMessage());
            re.initCause(ex);
            throw re;
        }
    }


    protected void setupJaxWSServiceForDeployment(DeploymentContext dc, WebService ws) throws DeploymentException {

        BundleDescriptor bundle = dc.getModuleMetaData(BundleDescriptor.class);

        // for modules this is domains/<domain-name>/j2ee-modules/<module-name>
        // for apps this is domains/<domain-name>/j2ee-apps/<app-name>/<foo_war> (in case of embedded wars)
        //  or domains/<domain-name>/j2ee-apps/<app-name>/<foo_jar> (in case of embedded jars)
        File moduleDir = dc.getSourceDir();

        //For modules this is domains/<domain-name>/generated/xml
        //Check with Hong about j2ee-modules
        File wsdlDir = dc.getScratchDir("xml");
        mkDirs(wsdlDir);


        //For modules this is domains/<domain-name>/generated/xml
        //Check with Hong about j2ee-modules
        File stubsDir = dc.getScratchDir("ejb");
        mkDirs(stubsDir);


        if (!DOLUtils.warType().equals(bundle.getModuleType()) &&
                !DOLUtils.ejbType().equals(bundle.getModuleType())) {
            // unknown module type with @WebService, just ignore...
            return;
        }


        wsdlDir = new File(wsdlDir, bundle.getWsdlDir().replaceAll("/", "\\"+File.separator));

        // Check if catalog file is present, if so get mapped WSDLs
        String wsdlFileUri;
        File wsdlFile;
        try {
            checkCatalog(bundle, ws, moduleDir);
        } catch (DeploymentException e) {
            logger.log(Level.SEVERE, LogUtils.ERROR_RESOLVING_CATALOG);
        }
        if (ws.hasWsdlFile()) {
            // If wsdl file is an http URL, download that WSDL and all embedded relative wsdls, schemas
            if (ws.getWsdlFileUri().startsWith("http")) {
                try {
                    wsdlFileUri = downloadWsdlsAndSchemas( new URL(ws.getWsdlFileUri()), wsdlDir);
                } catch(Exception e) {
                    throw new DeploymentException(e.toString(), e);
                }
                wsdlFile = new File(wsdlDir, wsdlFileUri);
            } else {
                wsdlFileUri = ws.getWsdlFileUri();
                File wsdlFileAbs = new File(wsdlFileUri);
                wsdlFile = wsdlFileAbs.isAbsolute()? wsdlFileAbs : new File(moduleDir, wsdlFileUri);
            }

            if (!wsdlFile.exists()) {
                String errorMessage = format(logger.getResourceBundle().getString(LogUtils.WSDL_NOT_FOUND),
                        ws.getWsdlFileUri(), bundle.getModuleDescriptor().getArchiveUri());
                logger.log(Level.SEVERE, errorMessage);
                throw new DeploymentException(errorMessage);
            }
        }
    }

    /**
     * Loads the meta date associated with the application.
     *
     * @parameters type type of metadata that this deployer has declared providing.
     */
    @Override
    public Object loadMetaData(Class type, DeploymentContext dc) {
         //Moved the doWebServicesDeployment back to prepare after discussing with
         //Jerome
         //see this bug  https://glassfish.dev.java.net/issues/show_bug.cgi?id=8080
         return true;
    }

    /**
     * Returns the meta data assocated with this Deployer
     *
     * @return the meta data for this Deployer
     */
    @Override
    public MetaData getMetaData() {
        return new MetaData(false, null, new Class[] {Application.class});
    }

    /**
     *  This method downloads the main wsdl/schema and its imports in to the directory specified and returns the name of downloaded root
     * document.
     * @param httpUrl
     * @param wsdlDir
     * @return Returns the name of the root file downloaded with the invocation.
     * @throws Exception
     */
    private String downloadWsdlsAndSchemas( URL httpUrl, File wsdlDir) throws Exception {
        // First make required directories and download this wsdl file
        mkDirs(wsdlDir);
        String fileName = httpUrl.toString().substring(httpUrl.toString().lastIndexOf("/")+1);
        File toFile = new File(wsdlDir.getAbsolutePath()+File.separator+fileName);
        downloadFile(httpUrl, toFile);

        // Get a list of wsdl and schema relative imports in this wsdl
        HashSet<Import> wsdlRelativeImports = new HashSet<Import>();
        HashSet<Import> schemaRelativeImports = new HashSet<Import>();
        HashSet<Import> wsdlIncludes = new HashSet<Import>();
        HashSet<Import> schemaIncludes = new HashSet<Import>();
        parseRelativeImports(httpUrl, wsdlRelativeImports, wsdlIncludes,
                schemaRelativeImports, schemaIncludes);
        wsdlRelativeImports.addAll(wsdlIncludes);
        schemaRelativeImports.addAll(schemaIncludes);

        // Download all schema relative imports
        String urlWithoutFileName = httpUrl.toString().substring(0, httpUrl.toString().lastIndexOf("/"));
        for(Import next : schemaRelativeImports) {
            String location = next.getLocation();
            location = location.replaceAll("/", "\\"+File.separator);
            if(location.lastIndexOf(File.separator) != -1) {
                File newDir = new File(wsdlDir.getAbsolutePath()+File.separator+
                location.substring(0, location.lastIndexOf(File.separator)));
                mkDirs(newDir);
            }
            downloadFile(new URL(urlWithoutFileName+"/"+next.getLocation()),
                        new File(wsdlDir.getAbsolutePath()+File.separator+location));
        }

        // Download all wsdl relative imports
        for(Import next : wsdlRelativeImports) {
            String newWsdlLocation = next.getLocation();
            newWsdlLocation = newWsdlLocation.replaceAll("/",  "\\"+File.separator);
            File newWsdlDir;
            if(newWsdlLocation.lastIndexOf(File.separator) != -1) {
                newWsdlDir = new File(wsdlDir.getAbsolutePath() + File.separator +
                newWsdlLocation.substring(0, newWsdlLocation.lastIndexOf(File.separator)));
            } else {
                newWsdlDir = wsdlDir;
            }
            downloadWsdlsAndSchemas( new URL(urlWithoutFileName+"/"+next.getLocation()), newWsdlDir);
        }
        return fileName;
    }
    // If catalog file is present, get the mapped WSDL for given WSDL and replace the value in
    // the given WebService object
    private void checkCatalog(BundleDescriptor bundle, WebService ws, File moduleDir)
                            throws DeploymentException {
        // If no catalog file is present, return
        File catalogFile = new File(moduleDir,
                bundle.getDeploymentDescriptorDir() +
                File.separator + "jax-ws-catalog.xml");
        if(!catalogFile.exists()) {
            return;
        }
        resolveCatalog(catalogFile, ws.getWsdlFileUri(), ws);
    }

    public URL resolveCatalog(File catalogFile, String wsdlFile, WebService ws) throws DeploymentException {

        try {


           URL retVal = null;
            // Get an entity resolver
            org.xml.sax.EntityResolver resolver =
                    XmlUtil.createEntityResolver(catalogFile.toURL());
            org.xml.sax.InputSource source = resolver.resolveEntity(null, wsdlFile);
            if(source != null) {
                String mappedEntry = source.getSystemId();
                // For entries with relative paths, Entity resolver always
                // return file://<absolute path
                if(mappedEntry.startsWith("file:")) {
                    File f = new File(mappedEntry.substring(mappedEntry.indexOf(":")+1));
                    if(!f.exists()) {
                        throw new DeploymentException(format(rb.getString(LogUtils.CATALOG_RESOLVER_ERROR), mappedEntry));
                    }
                    retVal = f.toURI().toURL();
                    if(ws != null) {
                        ws.setWsdlFileUri(f.getAbsolutePath());
                        ws.setWsdlFileUrl(retVal);
                    }
                } else if(mappedEntry.startsWith("http")) {
                    retVal = new URL(mappedEntry);
                    if(ws != null) {
                        ws.setWsdlFileUrl(retVal);
                    }
                }
            }
            return retVal;

        } catch (Throwable t) {
            throw new DeploymentException(format(rb.getString(LogUtils.CATALOG_ERROR),
                     catalogFile.getAbsolutePath(), t.getMessage()));
        }
      
    }

    /**
     * Copies file from source to destination
     *
     * @param src
     * @param dest
     * @throws IOException
     */
    private static void copyFile(File src, File dest) throws IOException {
        if (!dest.exists()) {
            mkDirs(dest.getParentFile());
            mkFile(dest);
        }

        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        try {
            srcChannel = new FileInputStream(src).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
        finally {
            if (srcChannel != null) {
                srcChannel.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
        }
    }

    public void downloadFile(URL httpUrl, File toFile) throws Exception {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            if(!toFile.createNewFile()) {
                throw new Exception(format(rb.getString(LogUtils.FILECREATION_ERROR), toFile.getAbsolutePath()));
            }
            is = httpUrl.openStream();

            os = new FileOutputStream(toFile, true);
            int readCount;
            byte[] buffer = new byte[10240]; // Read 10KB at a time
            while(true) {
                readCount = is.read(buffer, 0, 10240);
                if(readCount != -1) {
                    os.write(buffer, 0, readCount);
                } else {
                    break;
                }
            }
            os.flush();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }

    /**
     * Collect all relative imports from a web service's main wsdl document.
     *
     * @param wsdlFileUrl
     * @param wsdlRelativeImports output param in which wsdl relative imports
     * will be added
     *
     * @param schemaRelativeImports output param in which schema relative
     * imports will be added
     * @param schemaIncludes output param in which schema includes will be added
     */
    private void parseRelativeImports(URL wsdlFileUrl,
                                      Collection wsdlRelativeImports,
                                      Collection wsdlIncludes,
                                      Collection schemaRelativeImports,
                                       Collection schemaIncludes)
        throws Exception {

        // We will use our little parser rather than using JAXRPC's heavy weight WSDL parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        //Validation is not needed as we want to be too strict in processing wsdls that are generated by buggy tools.
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        InputStream is = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            is = wsdlFileUrl.openStream();
            Document document = builder.parse(is);
            procesSchemaImports(document, schemaRelativeImports);
            procesWsdlImports(document, wsdlRelativeImports);
            procesSchemaIncludes(document, schemaIncludes);
            procesWsdlIncludes(document, wsdlIncludes);
        } catch (SAXParseException spe) {
            // Error generated by the parser
            logger.log(Level.SEVERE, LogUtils.PARSING_ERROR,
                    new Object[] {spe.getLineNumber() ,spe.getSystemId()});
            // Use the contained exception, if any
            Exception x = spe;
            if (spe.getException() != null) {
                x = spe.getException();
            }
            logger.log(Level.SEVERE, LogUtils.ERROR_OCCURED, x);
        } catch (Exception sxe) {
            logger.log(Level.SEVERE, LogUtils.WSDL_PARSING_ERROR, sxe.getMessage());
        } finally {
            try {
                if(is != null) {
                    is.close();
                }
            } catch (IOException io) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, io.getMessage());
                }
            }
        }
    }

    private void procesSchemaImports(Document document, Collection schemaImportCollection) throws SAXException,
            ParserConfigurationException, IOException {
        NodeList schemaImports =
                document.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "import");
        addImportsAndIncludes(schemaImports, schemaImportCollection, "namespace", "schemaLocation");
    }

    private void procesWsdlImports(Document document, Collection wsdlImportCollection) throws SAXException,
            ParserConfigurationException, IOException {
        NodeList wsdlImports =
                document.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "import");
        addImportsAndIncludes(wsdlImports, wsdlImportCollection, "namespace", "location");
    }

    private void procesSchemaIncludes(Document document, Collection schemaIncludeCollection) throws SAXException,
            ParserConfigurationException, IOException {
        NodeList schemaIncludes =
                document.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "include");
        addImportsAndIncludes(schemaIncludes, schemaIncludeCollection, null, "schemaLocation");
    }

    private void procesWsdlIncludes(Document document, Collection wsdlIncludesCollection) throws SAXException,
            ParserConfigurationException, IOException {
        NodeList wsdlIncludes =
                document.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "include");
        addImportsAndIncludes(wsdlIncludes, wsdlIncludesCollection, null, "location");
    }

    private void addImportsAndIncludes(NodeList list, Collection result,
                        String namespace, String location) throws SAXException,
                        ParserConfigurationException, IOException {
        for(int i=0; i<list.getLength(); i++) {
            String givenLocation = null;
            Node element = list.item(i);
            NamedNodeMap attrs = element.getAttributes();
            Node n= attrs.getNamedItem(location);
            if(n != null) {
                givenLocation = n.getNodeValue();
            }
            if(givenLocation == null || givenLocation.startsWith("http")) {
                continue;
            }
            Import imp = new Import();
            imp.setLocation(givenLocation);
            if(namespace != null) {
                n = attrs.getNamedItem(namespace);
                if(n != null) {
                    imp.setNamespace(n.getNodeValue());
                }
            }
            result.add(imp);
        }

    }

    /**
     * Processes all the web services in the module and prepares for deployment.
     * The tasks include composing the endpoint publish url and generating WSDL in to the application repository
     * directory.
     *
     * In JAX-WS, WSDL files are generated dynamically, hence skips the wsdl generation step unless explicitly requested
     * for WSDL file publishing via DD.
     *
     * @param app
     * @param dc
     * @throws Exception
     */
    private void doWebServicesDeployment(Application app, DeploymentContext dc)
        throws Exception{

        Collection<WebService> webServices = new HashSet<WebService>();

        // when there are multiple sub modules in ear, we only want to handle the ones local to this deployment context

        //First get the web services associated with module bundle descriptor.
        WebServicesDescriptor wsDesc = dc.getModuleMetaData(WebServicesDescriptor.class);
        if (wsDesc != null && wsDesc.getWebServices().size() > 0) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogUtils.WS_LOCAL,
                        new Object[] {wsDesc.getWebServices().size(), getWebServiceDescriptors(app).size()});
            }
            webServices.addAll(wsDesc.getWebServices());
        }
        //Now get the web services associated with extension descriptors,ex: EJB WS in war.
        WebBundleDescriptor webBundleDesc = dc.getModuleMetaData(WebBundleDescriptor.class);
        if (webBundleDesc != null) {
            Collection<EjbBundleDescriptor> ejbBundleDescriptors = webBundleDesc.getExtensionsDescriptors(EjbBundleDescriptor.class);
            for (EjbBundleDescriptor ejbBundleDescriptor : ejbBundleDescriptors) {
                Collection wsInExtnDesc = ejbBundleDescriptor.getWebServices().getWebServices();
                webServices.addAll(wsInExtnDesc);
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogUtils.WS_VIA_EXT, wsInExtnDesc);
                }
            }
        }

        // swap the deployment descriptors context-root with the one
        // provided in the deployment request.
        //do not do for ejb in war case
        if (webBundleDesc!= null && webBundleDesc.getExtensionsDescriptors(EjbBundleDescriptor.class).size()==0) {
            if (dc.getAppProps().get("context-root") != null &&
                    app.isVirtual() ) {

                String contextRoot = ((String)dc.getAppProps().get("context-root"));
                webBundleDesc.setContextRoot(contextRoot);
            }
        }

        // Generate final wsdls for all web services and store them in
        // the application repository directory.
        for(WebService next : webServices) {
            WsUtil wsUtil = new WsUtil();

            // For JAXWS services, we rely on JAXWS RI to do WSL gen and publishing
            // For JAXRPC we do it here in 109
            //however it is needed for file publishing for jaxws
            if(wsUtil.isJAXWSbasedService(next) && (!next.hasFilePublishing())) {
                for(WebServiceEndpoint wsep : next.getEndpoints()) {
                    URL finalWsdlURL = wsep.composeFinalWsdlUrl(wsUtil.getWebServerInfoForDAS().getWebServerRootURL(wsep.isSecure()));
                    Set<ServiceReferenceDescriptor> serviceRefs = new HashSet<ServiceReferenceDescriptor>();
                    if (webBundleDesc != null) {
                        serviceRefs = webBundleDesc.getServiceReferenceDescriptors();
                    } else {
                        EjbBundleDescriptor ejbBundleDesc = dc.getModuleMetaData(EjbBundleDescriptor.class);
                        if (ejbBundleDesc != null) {
                            serviceRefs = ejbBundleDesc.getEjbServiceReferenceDescriptors();
                        } else {
                            logger.log(Level.SEVERE, LogUtils.UNSUPPORTED_MODULE_TYPE,
                                    DOLUtils.getCurrentBundleForContext(dc).getModuleType());
                        }
                    }
                    //if there's service-ref to this service update its
                    // wsdl-file value to point to the just exposed URL
                    for (ServiceReferenceDescriptor srd : serviceRefs) {
                        if (srd.getServiceName().equals(wsep.getServiceName())
                                && srd.getServiceNamespaceUri().equals(wsep.getWsdlService().getNamespaceURI())) {
                            srd.setWsdlFileUri(finalWsdlURL.toExternalForm() + "?WSDL");
                        }
                    }
                }
            } else {

                // Even if deployer specified a wsdl file
                // publish location, server can't assume it can access that
                // file system.  Plus, it's cleaner to depend on a file stored
                // within the application repository rather than one directly
                // exposed to the deployer. Name of final wsdl is derived based
                // on the location of its associated module.  This prevents us
                // from having write the module to disk in order to store the
                // modified runtime info.
                URL url = next.getWsdlFileUrl();
                if (url == null ) {
                    File f = new File(dc.getSourceDir(),next.getWsdlFileUri()) ;
                    url = f.toURL();
                }

                // Create the generated WSDL in the generated directory; for that create the directories first
                File genXmlDir =  dc.getScratchDir("xml");

                String wsdlFileDir = next.getWsdlFileUri().substring(0, next.getWsdlFileUri().lastIndexOf('/'));
                mkDirs(new File(genXmlDir, wsdlFileDir));
                File genWsdlFile = new File(genXmlDir, next.getWsdlFileUri());
                wsUtil.generateFinalWsdl(url, next, wsUtil.getWebServerInfoForDAS(), genWsdlFile);
            }
        }
        //Swap the servlet class name with a real servlet processing the SOAP requests.
        if (webBundleDesc != null) {
            doWebServiceDeployment(webBundleDesc);
        }
    }

    /**
     * Prepares the servlet based web services specified in web.xml for deployment.
     *
     * Swap the application written servlet implementation class for
     * one provided by the container.  The original class is stored
     * as runtime information since it will be used as the servant at
     * dispatch time.
     */
    private void doWebServiceDeployment(WebBundleDescriptor webBunDesc)
        throws DeploymentException, MalformedURLException {

        /**
         * Combining code from <code>com.sun.enterprise.deployment.backend.WebServiceDeployer</code>
         * in v2
         */

        Collection<WebServiceEndpoint> endpoints =
            webBunDesc.getWebServices().getEndpoints();
        ClassLoader cl = webBunDesc.getClassLoader();
        WsUtil wsutil = new WsUtil();

        for(WebServiceEndpoint nextEndpoint : endpoints) {
            WebComponentDescriptor webComp = nextEndpoint.getWebComponentImpl();

            if( !nextEndpoint.hasServletImplClass() ) {
                throw new DeploymentException( format(rb.getString(
                        LogUtils.DEPLOYMENT_BACKEND_CANNOT_FIND_SERVLET),
                        nextEndpoint.getEndpointName()));
            }

            if (nextEndpoint.hasEndpointAddressUri()) {
                webComp.getUrlPatternsSet().clear();
                webComp.addUrlPattern(nextEndpoint.getEndpointAddressUri());
            }

            if( !nextEndpoint.getWebService().hasFilePublishing() ) {
                String publishingUri = nextEndpoint.getPublishingUri();
                String publishingUrlPattern =
                        (publishingUri.charAt(0) == '/') ?publishingUri : "/" + publishingUri + "/*";
                webComp.addUrlPattern(publishingUrlPattern);

            }

            String servletImplClass = nextEndpoint.getServletImplClass();
            try {
                Class servletImplClazz  = cl.loadClass(servletImplClass);
                String containerServlet;
                if(wsutil.isJAXWSbasedService(nextEndpoint.getWebService())) {
                    containerServlet = "org.glassfish.webservices.JAXWSServlet";
                    addWSServletContextListener(webBunDesc);
                } else {
                    containerServlet =
                    SingleThreadModel.class.isAssignableFrom(servletImplClazz) ?
                    "org.glassfish.webservices.SingleThreadJAXRPCServlet" :
                        "org.glassfish.webservices.JAXRPCServlet";
                }
                webComp.setWebComponentImplementation(containerServlet);

            } catch(ClassNotFoundException cex) {
                throw new DeploymentException( format(rb.getString(
                        LogUtils.DEPLOYMENT_BACKEND_CANNOT_FIND_SERVLET),
                        nextEndpoint.getEndpointName()));
            }

            /**
             * Now trying to figure the address from <code>com.sun.enterprise.webservice.WsUtil.java</code>
             */
            // Get a URL for the root of the webserver, where the host portion
            // is a canonical host name.  Since this will be used to compose the
            // endpoint address that is written into WSDL, it's better to use
            // hostname as opposed to IP address.
            // The protocol and port will be based on whether the endpoint
            // has a transport guarantee of INTEGRAL or CONFIDENTIAL.
            // If yes, https will be used.  Otherwise, http will be used.
            WebServerInfo wsi = new WsUtil().getWebServerInfoForDAS();
            URL rootURL = wsi.getWebServerRootURL(nextEndpoint.isSecure());
            String contextRoot = webBunDesc.getContextRoot();
            URL actualAddress = nextEndpoint.composeEndpointAddress(rootURL, contextRoot);
            if (wsi.getHttpVS() != null && wsi.getHttpVS().getPort()!=0) {
               logger.log(Level.INFO, LogUtils.ENDPOINT_REGISTRATION,
                       new Object[] {nextEndpoint.getEndpointName(), actualAddress});
            }
        }
    }

    private void addWSServletContextListener(WebBundleDescriptor webBunDesc) {
        for(AppListenerDescriptor appListner: webBunDesc.getAppListenerDescriptors()) {
            if(appListner.getListener().equals(WSServletContextListener.class.getName())) {
                //already registered
                return;
            }
        }
        webBunDesc.addAppListenerDescriptor(new AppListenerDescriptorImpl(WSServletContextListener.class.getName()));
    }
    
    private String format(String key, String ... values){
        return MessageFormat.format(key, (Object [])values);
    }

    public static void moveFile(String sourceFile, String destFile)
    throws IOException {
        FileUtils.copy(sourceFile, destFile);
        FileUtils.deleteFile(new File(sourceFile));
    }

    @Override
    public void unload(WebServicesApplication container, DeploymentContext context) {
        final WebServiceDeploymentNotifier notifier = getDeploymentNotifier();
        deletePublishedFiles(container.getPublishedFiles());
        Application app = container.getApplication();
        if ( app == null ) {
            // load uses context.getModuleMetaData(Application.class) to get the Application.  If there's a deployment
            // failure then "container" may not have initialized the application and container.getApplication() returns
            // null and we get NPE.  So use context.getModuleMetaData(Application.class) always.
            app = context.getModuleMetaData(Application.class);
        }
        if ( app != null ) {
            for(WebService svc : getWebServiceDescriptors(app)) {
                for(WebServiceEndpoint endpoint : svc.getEndpoints()) {
                    if (notifier != null) {
                        notifier.notifyUndeployed(endpoint);
                    }
                }
            }
        }
    }

    @Override
    public void clean(DeploymentContext dc) {
        super.clean(dc);

        WebServicesContainer container = habitat.getService(WebServicesContainer.class);
        WebServicesDeploymentMBean bean = container.getDeploymentBean();
        UndeployCommandParameters params = dc.getCommandParameters(UndeployCommandParameters.class);
        if (params != null)  {
            bean.undeploy(params.name);
        }
    }

    @Override
    public WebServicesApplication load(WebServicesContainer container, DeploymentContext context) {
        Set<String> publishedFiles = null;
        Application app = context.getModuleMetaData(Application.class);
        try {
            publishedFiles = populateWsdlFilesForPublish(context, getWebServiceDescriptors(app));
        } catch(Exception e) {
            throw new RuntimeException(e);
        }


        return new WebServicesApplication(context,  dispatcher, publishedFiles);
    }

    /**
     * Populate the wsdl files entries to download (if any) (Only for webservices which
     * use file publishing).
     *
     * TODO File publishing currently works only for wsdls packaged in the application for jax-ws.
     * Need to publish the dynamically generated wsdls as well. Lazy creation of WSEndpoint objects prohibits it now.
     */
    private Set<String> populateWsdlFilesForPublish(
            DeploymentContext dc, Set<WebService> webservices) throws IOException {

        Set<String> publishedFiles = new HashSet<String>();
        for (WebService webService : webservices) {
            if (!webService.hasFilePublishing()) {
                continue;
            }

            copyExtraFilesToGeneratedFolder(dc);
            BundleDescriptor bundle = webService.getBundleDescriptor();

            ArchiveType moduleType = bundle.getModuleType();
            //only EAR, WAR and EJB archives could contain wsdl files for publish
            if (moduleType==null || !(moduleType.equals(DOLUtils.earType()) ||
                    moduleType.equals(DOLUtils.warType()) ||
                    moduleType.equals(DOLUtils.ejbType()))) {
                return publishedFiles;
            }


            File sourceDir = dc.getScratchDir("xml");

            File parent;
            try {
                URI clientPublishURI = webService.getClientPublishUrl().toURI();
                if(!clientPublishURI.isOpaque()) {
                    parent = new File(clientPublishURI);
                } else {
                    parent =  new File(webService.getClientPublishUrl().getPath());
                }
            } catch (URISyntaxException e) {
                logger.log(Level.WARNING, LogUtils.EXCEPTION_THROWN, e);
                parent = new File(webService.getClientPublishUrl().getPath());
            }

            // Collect the names of all entries in or below the
            // dedicated wsdl directory.
            FileArchive archive = new FileArchive();
            archive.open(sourceDir.toURI());


            Enumeration entries = archive.entries(bundle.getWsdlDir());


            while (entries.hasMoreElements()) {
                String name = (String) entries.nextElement();
                String wsdlName = stripWsdlDir(name, bundle);
                File clientwsdl = new File(parent, wsdlName);
                File fulluriFile = new File(sourceDir, name);
                if (!fulluriFile.isDirectory()) {
                    publishFile(fulluriFile, clientwsdl);
                    publishedFiles.add(clientwsdl.getAbsolutePath());
                }
            }

        }
        return publishedFiles;
    }

    private void publishFile(File file, File publishLocation) throws IOException {
        copyFile(file, publishLocation);
    }

    private void deletePublishedFiles(Set<String> publishedFiles) {
        if (publishedFiles != null) {
            for (String path: publishedFiles) {
                File f = new File(path);
                if (f.exists()) {
                    FileUtils.deleteFile(f);
                }
            }
        }
    }
    /**
     * This is to be used for file publishing only. In case of wsdlImports and wsdlIncludes
     * we need to copy the nested wsdls from applications folder to the generated/xml folder
     *
     */
    private void copyExtraFilesToGeneratedFolder( DeploymentContext context) throws IOException{
        Archivist archivist = habitat.getService(Archivist.class);

        ReadableArchive archive = archiveFactory.openArchive(
                context.getSourceDir());

        WritableArchive archive2 = archiveFactory.createArchive(
                context.getScratchDir("xml"));

        // copy the additional webservice elements etc
        archivist.copyExtraElements(archive, archive2);
 
    }

    /**
     * Return the entry name without "WEB-INF/wsdl" or "META-INF/wsdl".
     */
    private String stripWsdlDir(String entry, BundleDescriptor bundle) {
        String wsdlDir = bundle.getWsdlDir();
        return entry.substring(wsdlDir.length()+1);
    }

    /**
     * Return a set of all com.sun.enterprise.deployment.WebService
     * descriptors in the application.
     */
    private Set<WebService> getWebServiceDescriptors(Application app) {
        Set<WebService> webServiceDescriptors = new HashSet<WebService>();
        for (BundleDescriptor next : app.getBundleDescriptors()) {
            WebServicesDescriptor webServicesDesc =
                    next.getWebServices();
            webServiceDescriptors.addAll(webServicesDesc.getWebServices());
        }
        return webServiceDescriptors;
    }

    private static void mkDirs(File f) {
        if (!f.mkdirs() && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.DIR_EXISTS, f);
        }
    }

    private static void mkFile(File f) throws IOException {
        if (!f.createNewFile() && logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, LogUtils.FILE_EXISTS, f);
        }
    }
}



