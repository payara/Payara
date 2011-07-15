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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.core;

import org.apache.catalina.*;
import org.apache.catalina.realm.JAASRealm;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.modeler.modules.MbeansSource;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Standard implementation of the <b>Engine</b> interface.  Each
 * child container must be a Host implementation to process the specific
 * fully qualified host name of that virtual host.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.5 $ $Date: 2006/03/12 01:27:01 $
 */

public class StandardEngine
    extends ContainerBase
    implements Engine {

    private static final Logger log = Logger.getLogger(
        StandardEngine.class.getName());

    // ----------------------------------------------------------- Constructors


    /**
     * Create a new StandardEngine component with the default basic Valve.
     */
    public StandardEngine() {

        super();
        pipeline.setBasic(new StandardEngineValve());
        /* Set the jmvRoute using the system property jvmRoute */
        try {
            setJvmRoute(System.getProperty("jvmRoute"));
        } catch(Exception ex) {
        }
        // By default, the engine will hold the reloading thread
        backgroundProcessorDelay = 10;

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * Host name to use when no server host, or an unknown host,
     * is specified in the request.
     */
    private String defaultHost = null;


    /**
     * The descriptive information string for this implementation.
     */
    private static final String info =
        "org.apache.catalina.core.StandardEngine/1.0";


    /**
     * The <code>Service</code> that owns this Engine, if any.
     */
    private Service service = null;

    /** Allow the base dir to be specified explicitly for
     * each engine. In time we should stop using catalina.base property -
     * otherwise we loose some flexibility.
     */
    private String baseDir = null;

    /** Optional mbeans config file. This will replace the "hacks" in
     * jk and ServerListener. The mbeans file will support (transparent) 
     * persistence - soon. It'll probably replace jk2.properties and could
     * replace server.xml. Of course - the same beans could be loaded and 
     * managed by an external entity - like the embedding app - which
     *  can use a different persistence mechanism.
     */ 
    private String mbeansFile = null;
    
    /** Mbeans loaded by the engine.  
     */ 
    private List<ObjectName> mbeans;
    
    /**
     * DefaultContext config
     */
    private DefaultContext defaultContext;


    /**
     * The JVM Route ID for this Tomcat instance. All Route ID's must be unique
     * across the cluster.
     */
    private String jvmRouteId;


    // ------------------------------------------------------------- Properties

    /** Provide a default in case no explicit configuration is set
     *
     * @return configured realm, or a JAAS realm by default
     */
    public Realm getRealm() {
        Realm configured=super.getRealm();
        // If no set realm has been called - default to JAAS
        // This can be overridden at engine, context and host level  
        if( configured==null ) {
            configured=new JAASRealm();
            this.setRealm( configured );
        }
        return configured;
    }


    /**
     * Return the default host.
     */
    public String getDefaultHost() {

        return (defaultHost);

    }


    /**
     * Set the default host.
     *
     * @param host The new default host
     */
    public void setDefaultHost(String host) {

        String oldDefaultHost = this.defaultHost;
        if (host == null) {
            this.defaultHost = null;
        } else {
            // START OF PE 4989789
            //this.defaultHost = host.toLowerCase();
            this.defaultHost = host;
            // END OF PE 4989789
        }
        support.firePropertyChange("defaultHost", oldDefaultHost,
                                   this.defaultHost);

    }
    
    public void setName(String name ) {
        if( domain != null ) {
            // keep name==domain, ignore override
            // we are already registered
            super.setName( domain );
            return;
        }
        // The engine name is used as domain
        domain=name; // XXX should we set it in init() ? It shouldn't matter
        super.setName( name );
    }


    /**
     * Set the cluster-wide unique identifier for this Engine.
     * This value is only useful in a load-balancing scenario.
     * <p>
     * This property should not be changed once it is set.
     */
    public void setJvmRoute(String routeId) {
        jvmRouteId = routeId;
    }


    /**
     * Retrieve the cluster-wide unique identifier for this Engine.
     * This value is only useful in a load-balancing scenario.
     */
    public String getJvmRoute() {
        return jvmRouteId;
    }


    /**
     * Set the DefaultContext
     * for new web applications.
     *
     * @param defaultContext The new DefaultContext
     */
    public void addDefaultContext(DefaultContext defaultContext) {

        DefaultContext oldDefaultContext = this.defaultContext;
        this.defaultContext = defaultContext;
        support.firePropertyChange("defaultContext",
                                   oldDefaultContext, this.defaultContext);

    }


    /**
     * Retrieve the DefaultContext for new web applications.
     */
    public DefaultContext getDefaultContext() {
        return (this.defaultContext);
    }


    /**
     * Return the <code>Service</code> with which we are associated (if any).
     */
    public Service getService() {

        return (this.service);

    }


    /**
     * Set the <code>Service</code> with which we are associated (if any).
     *
     * @param service The service that owns this Engine
     */
    public void setService(Service service) {
        this.service = service;
    }

    public String getMbeansFile() {
        return mbeansFile;
    }

    public void setMbeansFile(String mbeansFile) {
        this.mbeansFile = mbeansFile;
    }

    public String getBaseDir() {
        if( baseDir==null ) {
            baseDir=System.getProperty("catalina.base");
        }
        if( baseDir==null ) {
            baseDir=System.getProperty("catalina.home");
        }
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Install the StandardContext portion of the DefaultContext
     * configuration into current Context.
     *
     * @param context current web application context
     */
    public void installDefaultContext(Context context) {

        if (defaultContext != null &&
            defaultContext instanceof StandardDefaultContext) {

            ((StandardDefaultContext)defaultContext).installDefaultContext(context);
        }
    }


    /**
     * Import the DefaultContext config into a web application context.
     *
     * @param context web application context to import default context
     */
    public void importDefaultContext(Context context) {

        if ( this.defaultContext != null )
            this.defaultContext.importDefaultContext(context);

    }


    /**
     * Add a child Container, only if the proposed child is an implementation
     * of Host.
     *
     * @param child Child container to be added
     */
    public void addChild(Container child) {

        if (!(child instanceof Host))
            throw new IllegalArgumentException
                (sm.getString("standardEngine.notHost"));
        super.addChild(child);

    }


    /**
     * Return descriptive information about this Container implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (info);

    }

    /**
     * Disallow any attempt to set a parent for this Container, since an
     * Engine is supposed to be at the top of the Container hierarchy.
     *
     * @param container Proposed parent Container
     */
    public void setParent(Container container) {

        throw new IllegalArgumentException
            (sm.getString("standardEngine.notParent"));

    }


    /* CR 6368085
    private boolean initialized=false;
    */
    
    public void init() {
        if( initialized ) return;
        /* CR 6368085
        initialized=true;
        */

        if( oname==null ) {
            // not registered in JMX yet - standalone mode
            try {
                if (domain==null) {
                    domain=getName();
                }
                if (log.isLoggable(Level.FINE)) {
                    log.fine( "Register " + domain );
                }
                oname=new ObjectName(domain + ":type=Engine");
                controller=oname;
                Registry.getRegistry(null, null).registerComponent(this, oname, null);
            } catch (Throwable t) {
                log.log(Level.INFO, "Error registering ", t);
            }
        }

        if( mbeansFile == null ) {
            String defaultMBeansFile=getBaseDir() + "/conf/tomcat5-mbeans.xml";
            File f=new File( defaultMBeansFile );
            if( f.exists() ) mbeansFile=f.getAbsolutePath();
        }
        if( mbeansFile != null ) {
            readEngineMbeans();
        }
        if( mbeans != null ) {
            try {
                Registry.getRegistry(null, null).invoke(mbeans, "init", false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error in init() for " + mbeansFile, e);
            }
        }
        
        if( service==null ) {
            // for consistency...: we are probably in embedded mode
            try {
                service=new StandardService();
                service.setContainer( this );
                service.initialize();
                // Use same name for Service
                service.setName(getName());
            } catch( Throwable t ) {
                log.log(Level.SEVERE, t.toString());
            }
        }
        // START CR 6368085
        initialized = true;
        // END CR 6368085
        
    }

    /* CR 6368085    
    public void destroy() throws LifecycleException {
    */
    // START CR 6368085
    public void destroy() throws Exception {
    // END CR 6368085
        if( ! initialized ) return;
        /* CR 6368085
        initialized=false;
        */
        // START CR 6368085
        super.destroy();
        // END CR 6368085

        // if we created it, make sure it's also destroyed
        ((StandardService)service).destroy();

        if( mbeans != null ) {
            try {
                Registry.getRegistry(null, null).invoke(mbeans, "destroy", false);
            } catch (Exception e) {
                log.log(Level.SEVERE,
                        sm.getString(
                            "standardEngine.unregister.mbeans.failed",
                            mbeansFile),
                        e);
            }
        }
        // 
        if( mbeans != null ) {
            try {
                for( int i=0; i<mbeans.size() ; i++ ) {
                    Registry.getRegistry(null, null).unregisterComponent(mbeans.get(i));
                }
            } catch (Exception e) {
                log.log(Level.SEVERE,
                        sm.getString(
                            "standardEngine.unregister.mbeans.failed",
                            mbeansFile),
                        e);
            }
        }
        
        // force all metadata to be reloaded.
        // That doesn't affect existing beans. We should make it per
        // registry - and stop using the static.
        Registry.getRegistry(null, null).resetMetadata();
        
                
    }
    
    /**
     * Start this Engine component.
     *
     * @exception LifecycleException if a startup error occurs
     */
    public void start() throws LifecycleException {
        if( started ) {
            return;
        }
        if( !initialized ) {
            init();
        }

        /* PWC 6296256
        // Log our server identification information
        log.info( "Starting Servlet Engine: " + ServerInfo.getServerInfo());
        */
        // START PWC 6296256
        if (log.isLoggable(Level.FINE)) {
            log.fine("Starting Servlet Engine");
        }
        // END PWC 6296256

        if( mbeans != null ) {
            try {
                Registry.getRegistry(null, null).invoke(mbeans, "start", false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error in start() for " + mbeansFile, e);
            }
        }

        // Standard container startup
        super.start();

    }
    
    public void stop() throws LifecycleException {
        super.stop();
        if( mbeans != null ) {
            try {
                Registry.getRegistry(null, null).invoke(mbeans, "stop", false);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error in stop() for " + mbeansFile, e);
            }
        }
    }


    /**
     * Return a String representation of this component.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("StandardEngine[");
        sb.append(getName());
        sb.append("]");
        return (sb.toString());

    }


    // ------------------------------------------------------ Protected Methods


    // -------------------- JMX registration  --------------------

    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception
    {
        super.preRegister(server,name);

        this.setName( name.getDomain());

        return name;
    }

    // FIXME Remove -- not used 
    public ObjectName getParentName() throws MalformedObjectNameException {
        if (getService()==null) {
            return null;
        }
        String name = getService().getName();
        ObjectName serviceName=new ObjectName(domain +
                        ":type=Service,serviceName="+name);
        return serviceName;                
    }
    
    public ObjectName createObjectName(String domain, ObjectName parent)
        throws Exception
    {
        if (log.isLoggable(Level.FINE))
            log.fine("Create ObjectName " + domain + " " + parent );
        return new ObjectName( domain + ":type=Engine");
    }

    
    private void readEngineMbeans() {
        try {
            MbeansSource mbeansMB=new MbeansSource();
            File mbeansF=new File( mbeansFile );
            mbeansMB.setSource(mbeansF);
            
            Registry.getRegistry(null, null).registerComponent(mbeansMB, 
                    domain + ":type=MbeansFile", null);
            mbeansMB.load();
            mbeansMB.init();
            mbeansMB.setRegistry(Registry.getRegistry(null, null));
            mbeans=mbeansMB.getMBeans();
            
        } catch( Throwable t ) {
            log.log(Level.SEVERE, "Error loading " + mbeansFile, t);
        }
        
    }
    
    public String getDomain() {
        if (domain!=null) {
            return domain;
        } else { 
            return getName();
        }
    }
    
    public void setDomain(String domain) {
        this.domain = domain;
    }
    
}
