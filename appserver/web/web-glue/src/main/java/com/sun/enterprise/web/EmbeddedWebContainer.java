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

package com.sun.enterprise.web;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.web.logger.FileLoggerHandlerFactory;
import com.sun.enterprise.web.pluggable.WebContainerFeatureFactory;
import java.io.File;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import com.sun.web.server.WebContainerListener;
import org.apache.catalina.*;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Embedded;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.NamedNamingObjectProxy;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;



/**
 * Represents an embedded Catalina web container within the Application Server.
 */

@Service(name="com.sun.enterprise.web.EmbeddedWebContainer")
@Singleton
public final class EmbeddedWebContainer extends Embedded implements PostConstruct {

    public static final Logger logger = WebContainer.logger;

    @LogMessageInfo(
            message = "Unable to instantiate ContainerListener of type {0}",
            level = "SEVERE",
            cause = "An exception occurred during instantiation of ContainerListener of type {0}",
            action = "Check the Exception for error")
    public static final String UNABLE_TO_INSTANTIATE_CONTAINER_LISTENER = "AS-WEB-GLUE-00091";

    @LogMessageInfo(
            message = "Creating connector for address='{0}' port='{1}' protocol='{2}'",
            level = "FINE")
    public static final String CREATE_CONNECTOR = "AS-WEB-GLUE-00092";

    @Inject
    private ServiceLocator services;
    
    @Inject
    private ServerContext serverContext;

    private WebContainerFeatureFactory webContainerFeatureFactory;

    private WebContainer webContainer;

    private InvocationManager invocationManager;

    private InjectionManager injectionManager;

    private NamedNamingObjectProxy validationNamingProxy;

    /*
     * The value of the 'file' attribute of the log-service element
     */
    private String logServiceFile;
    
    /*
     * The log level for org.apache.catalina.level as defined in logging.properties 
     */
    private String logLevel;

    private FileLoggerHandlerFactory fileLoggerHandlerFactory;
    
    void setWebContainer(WebContainer webContainer) {
        this.webContainer = webContainer;
    }
        
    void setLogServiceFile(String logServiceFile) {
        this.logServiceFile = logServiceFile;
    }
        
    void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
            
    void setFileLoggerHandlerFactory(FileLoggerHandlerFactory fileLoggerHandlerFactory) {
        this.fileLoggerHandlerFactory = fileLoggerHandlerFactory;
    }
    
    void setWebContainerFeatureFactory(WebContainerFeatureFactory webContainerFeatureFactory) {
        this.webContainerFeatureFactory = webContainerFeatureFactory;
    }
    
    // --------------------------------------------------------- Public Methods
    
    public void postConstruct() {
        invocationManager = services.getService(InvocationManager.class);
        injectionManager = services.getService(InjectionManager.class);
        validationNamingProxy = services.getService(NamedNamingObjectProxy.class, "ValidationNamingProxy");
    }
    
    /**
     * Creates a virtual server.
     *
     * @param vsID Virtual server id
     * @param vsBean Bean corresponding to virtual-server element in domain.xml
     * @param vsDocroot Virtual server docroot
     * @param vsMimeMap Virtual server MIME mappings
     *
     * @return The generated virtual server instance
     */
    public Host createHost(
                    String vsID,
                    com.sun.enterprise.config.serverbeans.VirtualServer vsBean,
                    String vsDocroot,
                    String vsLogFile,
                    MimeMap vsMimeMap) {

        VirtualServer vs = new VirtualServer();
        vs.setFileLoggerHandlerFactory(fileLoggerHandlerFactory);

        vs.configure(vsID, vsBean, vsDocroot, vsLogFile, vsMimeMap,
                     logServiceFile, logLevel);
         
        ContainerListener listener = loadListener
            ("com.sun.enterprise.web.connector.extension.CatalinaListener");
        if ( listener != null ) {
            vs.addContainerListener(listener);     
        }

        return vs;
    }
    
    /**
     * Create a web module/application.
     *
     * @param ctxPath  Context path for the web module
     * @param location Absolute pathname to the web module directory
     * @param defaultWebXmlLocation Location of default-web.xml
     */
    public Context createContext(String id,
                                 String ctxPath,
                                 File location,
                                 String defaultContextXmlLocation,
                                 String defaultWebXmlLocation, 
                                 boolean useDOLforDeployment,
                                 WebModuleConfig wmInfo) {

        File configFile = null;
        // check contextPath.xml and /META-INF/context.xml if not found
        if (ctxPath.equals("")) {
            configFile = new File(getCatalinaHome()+"/config", "ROOT.xml");
        } else {
            configFile = new File(getCatalinaHome()+"/config", ctxPath+".xml");
        }
        if (!configFile.exists()) {
            configFile = new File(location, Constants.WEB_CONTEXT_XML);
        }
        
        WebModule context = new WebModule(services);
        context.setID(id);
        context.setWebContainer(webContainer);
        context.setDebug(debug);
        context.setPath(ctxPath);
        context.setDocBase(location.getAbsolutePath());
        context.setCrossContext(true);
        context.setUseNaming(isUseNaming());
        context.setHasWebXml(wmInfo.getDescriptor() != null);
        context.setWebBundleDescriptor(wmInfo.getDescriptor());
        context.setManagerChecksFrequency(1);
        context.setServerContext(serverContext);
        context.setWebModuleConfig(wmInfo);
        context.setDefaultWebXml(defaultWebXmlLocation);
        if (embeddedDirectoryListing) {
            context.setDirectoryListing(embeddedDirectoryListing);
        }

        if (configFile.exists()) {
            context.setConfigFile(configFile.getAbsolutePath());
        }
            
        ContextConfig config;
        if (useDOLforDeployment) {            
            config = new WebModuleContextConfig(services);
            ((WebModuleContextConfig)config).setDescriptor(
                wmInfo.getDescriptor());
        } else {
            config = new ContextConfig();
        }
        
        config.setDefaultContextXml(defaultContextXmlLocation);
        config.setDefaultWebXml(defaultWebXmlLocation);
        context.addLifecycleListener(config);

        // TODO: should any of those become WebModuleDecorator, too?
        context.addLifecycleListener(new WebModuleListener(webContainer, wmInfo.getDescriptor()));

        context.addContainerListener(
                new WebContainerListener(invocationManager, injectionManager, validationNamingProxy));

        for( WebModuleDecorator d : services.<WebModuleDecorator>getAllServices(WebModuleDecorator.class)) {
            d.decorate(context);
        }

        // TODO: monitoring should also hook in via WebModuleDecorator
        //context.addInstanceListener(
        //    "com.sun.enterprise.admin.monitor.callflow.WebContainerListener");
        
        return context;
    }

         
    /**
     * Util method to load classes that might get compiled after this class is
     * compiled.
     */
    private ContainerListener loadListener(String className){
        try{
            Class clazz = Class.forName(className);
            return (ContainerListener)clazz.newInstance();
        } catch (Throwable ex){
            String msg = logger.getResourceBundle().getString(UNABLE_TO_INSTANTIATE_CONTAINER_LISTENER);
            msg = MessageFormat.format(msg, className);
            logger.log(Level.SEVERE, msg, ex);
        }
        return null;
    }
    
   
    /**
     * Return the list of engines created (from Embedded API)
     */
    @Override
    public Engine[] getEngines() {
        return engines;
    }

    /**
     * Returns the list of Connector objects associated with this 
     * EmbeddedWebContainer.
     *
     * @return The list of Connector objects associated with this 
     * EmbeddedWebContainer
     */
    public Connector[] getConnectors() {
        return connectors;
    }


    /**
     * Create a customized version of the Tomcat's 5 Coyote Connector. This
     * connector is required in order to support PE Web Programmatic login
     * functionality.
     * @param address InetAddress to bind to, or <code>null</code> if the
     * connector is supposed to bind to all addresses on this server
     * @param port Port number to listen to
     * @param protocol the http protocol to use.
     */
    @Override
    public Connector createConnector(String address, int port,
				     String protocol) {

        if (address != null) {
            /*
             * InetAddress.toString() returns a string of the form
             * "<hostname>/<literal_IP>". Get the latter part, so that the
             * address can be parsed (back) into an InetAddress using
             * InetAddress.getByName().
             */
            int index = address.indexOf('/');
            if (index != -1) {
                address = address.substring(index + 1);
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, CREATE_CONNECTOR, new Object[]{(address == null) ? "ALL" : address, port, protocol});
        }

        WebConnector connector = new WebConnector(webContainer);

        if (address != null) {
            connector.setAddress(address);
        }

        connector.setPort(port);

        if (protocol.equals("ajp")) {
            connector.setProtocolHandlerClassName(
                 "org.apache.jk.server.JkCoyoteHandler");
        } else if (protocol.equals("memory")) {
            connector.setProtocolHandlerClassName(
                 "org.apache.coyote.memory.MemoryProtocolHandler");
        } else if (protocol.equals("https")) {
            connector.setScheme("https");
            connector.setSecure(true);
        }

        return (connector);

    }
    

    /**
     * Create, configure, and return an Engine that will process all
     * HTTP requests received from one of the associated Connectors,
     * based on the specified properties.
     *
     * Do not create the JAAS default realm since all children will
     * have their own.
     */
    @Override
    public Engine createEngine() {

        StandardEngine engine = new StandardEngine();

        engine.setDebug(debug);
        // Default host will be set to the first host added
        engine.setLogger(super.getLogger());       // Inherited by all children
        engine.setRealm(null);         // Inherited by all children
        
        //ContainerListener listener = loadListener
        //    ("com.sun.enterprise.admin.monitor.callflow.WebContainerListener");
        //if ( listener != null ) {
        //    engine.addContainerListener(listener);
        //}
        return (engine);

    }


    /*
    static class WebEngine extends StandardEngine {

        private WebContainer webContainer;

        public WebEngine(WebContainer webContainer) {
            this.webContainer = webContainer;
        }

        @Override
        public Realm getRealm(){
            return null;
        }

        /**
         * Starts the children (virtual servers) of this StandardEngine
         * concurrently.
         *
        protected void startChildren() {

            
            new File(webContainer.getAppsWorkRoot()).mkdirs();
            new File(webContainer.getModulesWorkRoot()).mkdirs();
            
            ArrayList<LifecycleStarter> starters
                = new ArrayList<LifecycleStarter>();

            Container children[] = findChildren();
            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Lifecycle) {
                    LifecycleStarter starter =
                        new LifecycleStarter(((Lifecycle) children[i]));
                    starters.add(starter);
                    starter.submit();
                }
            }

            for (LifecycleStarter starter : starters) {
                Throwable t = starter.waitDone(); 
                if (t != null) {
                    Lifecycle container = starter.getContainer();
                    String msg = rb.getString("embedded.startVirtualServerError");
                    msg = MessageFormat.format(msg, new Object[] { container });
                    _logger.log(Level.SEVERE, msg, t);
                }
            }
        }
    }*/
}
