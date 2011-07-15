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

package org.apache.tomcat.util.modeler;


import org.apache.tomcat.util.modeler.modules.ModelerSource;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.Globals;

import javax.management.*;
import javax.management.modelmbean.ModelMBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
   Issues:
   - exceptions - too many "throws Exception"
   - double check the interfaces 
   - start removing the use of the experimental methods in tomcat, then remove
     the methods ( before 1.1 final )
   - is the security enough to prevent Registry beeing used to avoid the permission
    checks in the mbean server ?
*/ 

/**
 * Registry for modeler MBeans. 
 *
 * This is the main entry point into modeler. It provides methods to create
 * and manipulate model mbeans and simplify their use.
 *
 * Starting with version 1.1, this is no longer a singleton and the static
 * methods are strongly deprecated. In a container environment we can expect
 * different applications to use different registries.
 * 
 * This class is itself an mbean.
 * 
 * IMPORTANT: public methods not marked with @since x.x are experimental or 
 * internal. Should not be used.  
 * 
 * @author Craig R. McClanahan
 * @author Costin Manolache
 */
public class Registry implements RegistryMBean, MBeanRegistration  {
    /** Experimental support for manifest-based discovery.
     */
    public static final String MODELER_MANIFEST="/META-INF/mbeans-descriptors.xml";

    /**
     * The Log instance to which we will write our log messages.
     */
    private static Logger log = Logger.getLogger(Registry.class.getName());
    
    // Support for the factory methods
    
    /** Will be used to isolate different apps and enhance security
     */
    private static HashMap<Object,Registry> perLoaderRegistries=null;

    /**
     * The registry instance created by our factory method the first time
     * it is called.
     */
    private static Registry registry = null;

    // Per registy fields
    
    /**
     * The <code>MBeanServer</code> instance that we will use to register
     * management beans.
     */
    private MBeanServer server = null;

    /**
     * The set of ManagedBean instances for the beans this registry
     * knows about, keyed by name.
     */
    private HashMap<String,ManagedBean> descriptors = 
            new HashMap<String,ManagedBean>();

    /** List of managed byeans, keyed by class name
     */
    private HashMap<String,ManagedBean> descriptorsByClass = 
            new HashMap<String,ManagedBean>();

    // map to avoid duplicated searching or loading descriptors 
    private HashMap<String,URL> searchedPaths=new HashMap<String,URL>();
    
    private Object key;
    private Object guard;

    // Id - small ints to use array access. No reset on stop()
    private Hashtable<String,Hashtable<String,Integer>> idDomains =
            new Hashtable<String,Hashtable<String,Integer>>();
    private Hashtable<String,int[]> ids = new Hashtable<String,int[]>();

    
    // ----------------------------------------------------------- Constructors

    /**
     */
     public Registry() {
        super();
    }

    // -------------------- Static methods  --------------------
    // Factories
    
    /**
     * Factory method to create (if necessary) and return our
     * <code>Registry</code> instance.
     *
     * Use this method to obtain a Registry - all other static methods
     * are deprecated and shouldn't be used.
     *
     * The current version uses a static - future versions could use
     * the thread class loader.
     * 
     * @param key Support for application isolation. If null, the context class
     * loader will be used ( if setUseContextClassLoader is called ) or the 
     * default registry is returned. 
     * @param guard Prevent access to the registry by untrusted components
     *
     * @since 1.1
     */
    public synchronized static Registry getRegistry(Object key, Object guard) {
        Registry localRegistry;
        if( perLoaderRegistries!=null ) {
            if( key==null ) 
                key=Thread.currentThread().getContextClassLoader();
            if( key != null ) {
                localRegistry=perLoaderRegistries.get(key);
                if( localRegistry == null ) {
                    localRegistry=new Registry();
                    localRegistry.key=key;
                    localRegistry.guard=guard;
                    perLoaderRegistries.put( key, localRegistry );
                    return localRegistry;
                }
                if( localRegistry.guard != null &&
                        localRegistry.guard != guard ) {
                    return null; // XXX Should I throw a permission ex ? 
                }
                return localRegistry;
            }
        }

        // static 
        if (registry == null) {
            registry = new Registry();
        }
        if( registry.guard != null &&
                registry.guard != guard ) {
            return null;
        }
        return (registry);
    }
    
    /** Allow containers to isolate apps. Can be called only once.
     * It  is highly recommended you call this method if using Registry in
     * a container environment. The default is false for backward compatibility
     * 
     * @param enable
     * @since 1.1
     */
    public static void setUseContextClassLoader( boolean enable ) {
        if( enable ) {
            perLoaderRegistries=new HashMap<Object,Registry>();
        }
    }
    
    // -------------------- Generic methods  --------------------

    /** Set a guard object that will prevent access to this registry 
     * by unauthorized components
     * 
     * @param guard
     * 
     * @since 1.1
     */ 
    public void setGuard( Object guard ) {
        if( this.guard!=null ) {
            return; // already set, only once
        }
        this.guard=guard;
    }

    /** Lifecycle method - clean up the registry metadata.
     * 
     * @since 1.1
     */ 
    public void stop() {
        descriptorsByClass = new HashMap<String,ManagedBean>();
        descriptors = new HashMap<String,ManagedBean>();
        searchedPaths=new HashMap<String,URL>();
    }
    
    /** 
     * Load an extended mlet file. The source can be an URL, File or
     * InputStream. 
     * 
     * All mbeans will be instantiated, registered and the attributes will be 
     * set. The result is a list of ObjectNames.
     *
     * @param source InputStream or URL of the file
     * @param cl ClassLoader to be used to load the mbeans, or null to use the
     *        default JMX mechanism ( i.e. all registered loaders )
     * @return List of ObjectName for the loaded mbeans
     * @throws Exception
     * 
     * @since 1.1
     */ 
    public List<ObjectName> loadMBeans( Object source, ClassLoader cl )
            throws Exception
    {
        return load("MbeansSource", source, null );
    }    


    /** Load descriptors. The source can be a File or URL or InputStream for the 
     * descriptors file. In the case of File and URL, if the extension is ".ser"
     * a serialized version will be loaded. 
     * 
     * Also ( experimental for now ) a ClassLoader - in which case META-INF/ will
     * be used.
     * 
     * This method should be used to explicitely load metadata - but this is not
     * required in most cases. The registerComponent() method will find metadata
     * in the same pacakge.
     * 
     * @param source
     */ 
    public void loadMetadata(Object source ) throws Exception {
        if( source instanceof ClassLoader ) {
            loadMetaInfDescriptors((ClassLoader)source);
            return;
        } else {
            registry.loadDescriptors( null, source, null );
        }
        
    }

    /** Register a bean by creating a modeler mbean and adding it to the 
     * MBeanServer.
     * 
     * If metadata is not loaded, we'll look up and read a file named
     * "mbeans-descriptors.ser" or "mbeans-descriptors.xml" in the same package
     * or parent.
     *
     * If the bean is an instance of DynamicMBean. it's metadata will be converted
     * to a model mbean and we'll wrap it - so modeler services will be supported
     *
     * If the metadata is still not found, introspection will be used to extract
     * it automatically. 
     * 
     * If an mbean is already registered under this name, it'll be first
     * unregistered.
     * 
     * If the component implements MBeanRegistration, the methods will be called.
     * If the method has a method "setRegistry" that takes a RegistryMBean as
     * parameter, it'll be called with the current registry.
     * 
     *
     * @param bean Object to be registered
     * @param oname Name used for registration
     * @param type The type of the mbean, as declared in mbeans-descriptors. If
     * null, the name of the class will be used. This can be used as a hint or
     * by subclasses.
     *
     * @since 1.1
     */ 
    public void registerComponent(Object bean, String oname, String type)
           throws Exception
    {
        registerComponent(bean, new ObjectName(oname), type);        
    }    

    /** Unregister a component. We'll first check if it is registered,
     * and mask all errors. This is mostly a helper.
     * 
     * @param oname
     * 
     * @since 1.1
     */ 
    public void unregisterComponent( String oname ) {
        try {
            unregisterComponent(new ObjectName(oname));
        } catch (MalformedObjectNameException e) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Error creating object name " + e );
            }
        }
    }    
    

    /** Invoke a operation on a list of mbeans. Can be used to implement
     * lifecycle operations.
     *
     * @param mbeans list of ObjectName on which we'll invoke the operations
     * @param operation  Name of the operation ( init, start, stop, etc)
     * @param failFirst  If false, exceptions will be ignored
     * @throws Exception
     * @since 1.1
     */
    public void invoke( List<ObjectName> mbeans, String operation, boolean failFirst )
            throws Exception
    {
        if( mbeans==null ) {
            return;
        }
        Iterator<ObjectName> itr=mbeans.iterator();
        while(itr.hasNext()) {
            ObjectName current=itr.next();
            try {
                if (current == null) {
                    continue;
                }
                if( getMethodInfo(current, operation) == null) {
                    continue;
                }
                getMBeanServer().invoke(current, operation,
                        new Object[] {}, new String[] {});

            } catch( Exception t ) {
                if( failFirst ) throw t;
                if (log.isLoggable(Level.INFO)) {
                    log.log(Level.INFO, "Error initializing " + current + " " + t.toString());
                }
            }
        }
    }

    // -------------------- ID registry --------------------

    /** Return an int ID for faster access. Will be used for notifications
     * and for other operations we want to optimize. 
     *
     * @param domain Namespace 
     * @param name  Type of the notification
     * @return  An unique id for the domain:name combination
     * @since 1.1
     */
    public synchronized int getId( String domain, String name) {
        if( domain==null) {
            domain="";
        }
        Hashtable<String,Integer> domainTable=idDomains.get( domain );
        if( domainTable == null ) {
            domainTable = new Hashtable<String,Integer>();
            idDomains.put( domain, domainTable); 
        }
        if( name==null ) {
            name="";
        }
        Integer i = domainTable.get(name);
        
        if( i!= null ) {
            return i.intValue();
        }

        int id[] = ids.get(domain);
        if( id == null ) {
            id=new int[1];
            ids.put( domain, id); 
        }
        int code=id[0]++;
        domainTable.put( name, Integer.valueOf( code ));
        return code;
    }
    
    // -------------------- Metadata   --------------------
    // methods from 1.0

    /**
     * Add a new bean metadata to the set of beans known to this registry.
     * This is used by internal components.
     *
     * @param bean The managed bean to be added
     * @since 1.0
     */
    public void addManagedBean(ManagedBean bean) {
        // XXX Use group + name
        descriptors.put(bean.getName(), bean);
        if( bean.getType() != null ) {
            descriptorsByClass.put( bean.getType(), bean );
        }
    }


    /**
     * Find and return the managed bean definition for the specified
     * bean name, if any; otherwise return <code>null</code>.
     *
     * @param name Name of the managed bean to be returned. Since 1.1, both
     *   short names or the full name of the class can be used.
     * @since 1.0
     */
    public ManagedBean findManagedBean(String name) {
        // XXX Group ?? Use Group + Type
        ManagedBean mb =  descriptors.get(name);
        if( mb==null )
            mb = descriptorsByClass.get(name);
        return mb;
    }
    
    /**
     * Return the set of bean names for all managed beans known to
     * this registry.
     *
     * @since 1.0
     */
    public String[] findManagedBeans() {
        Set<String> keySet = descriptors.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }


    /**
     * Return the set of bean names for all managed beans known to
     * this registry that belong to the specified group.
     *
     * @param group Name of the group of interest, or <code>null</code>
     *  to select beans that do <em>not</em> belong to a group
     * @since 1.0
     */
    public String[] findManagedBeans(String group) {

        ArrayList<String> results = new ArrayList<String>();
        Iterator<ManagedBean> items = descriptors.values().iterator();
        while (items.hasNext()) {
            ManagedBean item = items.next();
            if (group == null) {
                if (item.getGroup() == null) {
                    results.add(item.getName());
                }
            } else if (group.equals(item.getGroup())) {
                results.add(item.getName());
            }
        }
        String values[] = new String[results.size()];
        return results.toArray(values);

    }


    /**
     * Remove an existing bean from the set of beans known to this registry.
     *
     * @param bean The managed bean to be removed
     * @since 1.0
     */
    public void removeManagedBean(ManagedBean bean) {
       // TODO: change this to use group/name
        descriptors.remove(bean.getName());
        descriptorsByClass.remove( bean.getType());
    }

    // -------------------- Helpers  --------------------

    /** Get the type of an attribute of the object, from the metadata.
     *
     * @param oname
     * @param attName
     * @return null if metadata about the attribute is not found
     * @since 1.1
     */
    public String getType( ObjectName oname, String attName )
    {
        String type=null;
        MBeanInfo info=null;
        try {
            info=server.getMBeanInfo(oname);
        } catch (Exception e) {
            log.log(Level.INFO, "Can't find metadata for object" + oname );
            return null;
        }

        MBeanAttributeInfo attInfo[]=info.getAttributes();
        for( int i=0; i<attInfo.length; i++ ) {
            if( attName.equals(attInfo[i].getName())) {
                type=attInfo[i].getType();
                return type;
            }
        }
        return null;
    }

    /** Find the operation info for a method
     * 
     * @param oname
     * @param opName
     * @return the operation info for the specified operation
     */ 
    public MBeanOperationInfo getMethodInfo( ObjectName oname, String opName )
    {
        String type=null;
        MBeanInfo info=null;
        try {
            info=server.getMBeanInfo(oname);
        } catch (Exception e) {
            log.log(Level.INFO, "Can't find metadata " + oname );
            return null;
        }
        MBeanOperationInfo attInfo[]=info.getOperations();
        for( int i=0; i<attInfo.length; i++ ) {
            if( opName.equals(attInfo[i].getName())) {
                return attInfo[i];
            }
        }
        return null;
    }

    /** Unregister a component. This is just a helper that
     * avoids exceptions by checking if the mbean is already registered
     *
     * @param oname
     */
    public void unregisterComponent( ObjectName oname ) {
        try {
            if( getMBeanServer().isRegistered(oname)) {
                getMBeanServer().unregisterMBean(oname);
            }
        } catch( Throwable t ) {
            log.log(Level.SEVERE, "Error unregistering mbean ", t);
        }
    }

    /**
     * Factory method to create (if necessary) and return our
     * <code>MBeanServer</code> instance.
     *
     */
    public synchronized MBeanServer getMBeanServer() {
        long t1=System.currentTimeMillis();

        if (server == null) {
            // We can't use any existing MBeanServer, as it could be as well from a previous execution of GlassFish
            // which is possible as demonstrated in GLASSFISH-16501. So, create a new one.
            // To be able to create an MBeanServer, we need to ensure that we have set common-class-loader
            // as the TCL so that GlassFish MBeanServerBuilder class can be loaded.
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(
                        Globals.get(ClassLoaderHierarchy.class).getCommonClassLoader());
                server=MBeanServerFactory.createMBeanServer();

            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
            if (log.isLoggable(Level.FINE)) {
                log.fine("Creating MBeanServer"+ (System.currentTimeMillis() - t1 ));
            }
        }
        return (server);
    }

    /** Find or load metadata. 
     */ 
    public ManagedBean findManagedBean(Object bean, Class<?> beanClass, String type)
        throws Exception
    {
        if( bean!=null && beanClass==null ) {
            beanClass=bean.getClass();
        }
        
        if( type==null ) {
            type=beanClass.getName();
        }
        
        // first look for existing descriptor
        ManagedBean managed = registry.findManagedBean(type);

        // Search for a descriptor in the same package
        if( managed==null ) {
            // check package and parent packages
            if (log.isLoggable(Level.FINE)) {
                log.fine("Looking for descriptor ");
            }
            findDescriptor( beanClass, type );

            managed=findManagedBean(type);
        }
        
        if( bean instanceof DynamicMBean ) {
            if (log.isLoggable(Level.FINE)) {
                log.fine( "Dynamic mbean support ");
            }
            // Dynamic mbean
            loadDescriptors("MbeansDescriptorsDynamicMBeanSource",
                    bean, type);

            managed=findManagedBean(type);
        }

        // Still not found - use introspection
        if( managed==null ) {
            if (log.isLoggable(Level.FINE)) {
                log.fine( "Introspecting ");
            }

            // introspection
            loadDescriptors("MbeansDescriptorsIntrospectionSource",
                    beanClass, type);

            managed=findManagedBean(type);
            if( managed==null ) {
                log.warning( "No metadata found for " + type );
                return null;
            }
            managed.setName( type );
            addManagedBean(managed);
        }
        return managed;
    }
    

    /** EXPERIMENTAL Convert a string to object, based on type. Used by several
     * components. We could provide some pluggability. It is here to keep
     * things consistent and avoid duplication in other tasks 
     * 
     * @param type Fully qualified class name of the resulting value
     * @param value String value to be converted
     * @return Converted value
     */ 
    public Object convertValue(String type, String value)
    {
        Object objValue=value;
        
        if( type==null || "java.lang.String".equals( type )) {
            // string is default
            objValue=value;
        } else if( "javax.management.ObjectName".equals( type ) ||
                "ObjectName".equals( type )) {
            try {
                objValue=new ObjectName( value );
            } catch (MalformedObjectNameException e) {
                return null;
            }
        } else if( "java.lang.Integer".equals( type ) ||
                "int".equals( type )) {
            objValue=Integer.valueOf( value );
        } else if( "java.lang.Boolean".equals( type ) ||
                "boolean".equals( type )) {
            objValue=Boolean.valueOf( value );
        }
        return objValue;
    }
    
    /**
     * @param sourceType
     * @param source
     * @param param
     * @return List of descriptors
     * @throws Exception
     * @deprecated bad interface, mixing of metadata and mbeans
     */
    public List<ObjectName> load( String sourceType, Object source, String param)
        throws Exception
    {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("load " + source );
        }
        String location=null;
        String type=null;
        Object inputsource=null;

        if( source instanceof DynamicMBean ) {
            sourceType="MbeansDescriptorsDynamicMBeanSource";
            inputsource=source;
        } else if( source instanceof URL ) {
            URL url=(URL)source;
            location=url.toString();
            type=param;
            inputsource=url.openStream();
            if( sourceType == null ) {
                sourceType = sourceTypeFromExt(location);
            }
        } else if( source instanceof File ) {
            location=((File)source).getAbsolutePath();
            inputsource=new FileInputStream((File)source);            
            type=param;
            if( sourceType == null ) {
                sourceType = sourceTypeFromExt(location);
            }
        } else if( source instanceof InputStream ) {
            type=param;
            inputsource=source;
        } else if( source instanceof Class ) {
            location=((Class<?>)source).getName();
            type=param;
            inputsource=source;
            if( sourceType== null ) {
                sourceType="MbeansDescriptorsIntrospectionSource";
            }
        }
        
        if( sourceType==null ) {
            sourceType="MbeansDescriptorsDOMSource";
        }
        ModelerSource ds=getModelerSource(sourceType);
        List<ObjectName> mbeans=ds.loadDescriptors(this, location, type, inputsource);

        return mbeans;
    }

    private String sourceTypeFromExt( String s ) {
        if( s.endsWith( ".ser")) {
            return "MbeansDescriptorsSerSource";
        }
        else if( s.endsWith(".xml")) {
            return "MbeansDescriptorsDOMSource";
        }
        return null;
    }

    /** Register a component 
     * XXX make it private 
     * 
     * @param bean
     * @param oname
     * @param type
     * @throws Exception
     */ 
    public void registerComponent(Object bean, ObjectName oname, String type)
           throws Exception
    {
        if (log.isLoggable(Level.FINE)) {
            log.fine( "Managed= "+ oname);
        }

        if( bean ==null ) {
            log.log(Level.SEVERE, "Null component " + oname );
            return;
        }

        try {
            if( type==null ) {
                type=bean.getClass().getName();
            }

            ManagedBean managed = registry.findManagedBean(bean.getClass(), type);

            // The real mbean is created and registered
            ModelMBean mbean = managed.createMBean(bean);

            if(  getMBeanServer().isRegistered( oname )) {
                if (log.isLoggable(Level.FINE)) {
                    log.fine("Unregistering existing component " + oname );
                }
                getMBeanServer().unregisterMBean( oname );
            }

            getMBeanServer().registerMBean( mbean, oname);
        } catch( Exception ex) {
            log.log(Level.SEVERE, "Error registering " + oname, ex );
            throw ex;
        }
    }

    /** Lookup the component descriptor in the package and
     * in the parent packages.
     *
     * @param packageName
     */
    public void loadDescriptors( String packageName, ClassLoader classLoader  ) {
        String res=packageName.replace( '.', '/');

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Finding descriptor " + res );
        }

        if( searchedPaths.get( packageName ) != null ) {
            return;
        }
        String descriptors=res + "/mbeans-descriptors.ser";

        URL dURL=classLoader.getResource( descriptors );

        if( dURL == null ) {
            descriptors=res + "/mbeans-descriptors.xml";
            dURL=classLoader.getResource( descriptors );
        }
        if( dURL == null ) {
            return;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine( "Found " + dURL);
        }
        searchedPaths.put( packageName,  dURL );
        try {
            if( descriptors.endsWith(".xml" ))
                loadDescriptors("MbeansDescriptorsDOMSource", dURL, null);
            else
                loadDescriptors("MbeansDescriptorsSerSource", dURL, null);
            return;
        } catch(Exception ex ) {
            log.log(Level.SEVERE, "Error loading " + dURL);
        }

        return;
    }

    /** Experimental. Will become private, some code may still use it
     *
     * @param sourceType
     * @param source
     * @param param
     * @throws Exception
     * @deprecated
     */
    private void loadDescriptors( String sourceType, Object source,
            String param) throws Exception {
        load(sourceType, source, param);

    }

    /** Discover all META-INF/modeler.xml files in classpath and register
     * the components
     *
     * @since EXPERIMENTAL
     */
    private void loadMetaInfDescriptors(ClassLoader cl) {
        try {
            Enumeration en=cl.getResources(MODELER_MANIFEST);
            while( en.hasMoreElements() ) {
                URL url=(URL)en.nextElement();
                InputStream is=url.openStream();
                if (log.isLoggable(Level.FINE))
                    log.fine("Loading " + url);
                loadDescriptors("MBeansDescriptorDOMSource", is, null );
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }

    /** Lookup the component descriptor in the package and
     * in the parent packages.
     *
     * @param beanClass
     * @param type
     */
    private void findDescriptor( Class<?> beanClass, String type ) {
        if( type==null ) {
            type=beanClass.getName();
        }
        ClassLoader classLoader=null;
        if( beanClass!=null ) {
            classLoader=beanClass.getClassLoader();
        }
        if( classLoader==null ) {
            classLoader=Thread.currentThread().getContextClassLoader();
        }
        if( classLoader==null ) {
            classLoader=this.getClass().getClassLoader();
        }
        
        String className=type;
        String pkg=className;
        // pkg.indexOf(".") must be greater than 0 for a valid package name.
        while( pkg.indexOf( ".") > 0 ) {
            int lastComp=pkg.lastIndexOf( ".");
            if( lastComp <= 0 ) return;
            pkg=pkg.substring(0, lastComp);
            if( searchedPaths.get( pkg ) != null ) {
                return;
            }
            loadDescriptors(pkg, classLoader);
        }
        return;
    }

    private ModelerSource getModelerSource( String type )
            throws Exception
    {
        if( type==null ) type="MbeansDescriptorsDOMSource";
        if( type.indexOf( ".") < 0 ) {
            type="org.apache.tomcat.util.modeler.modules." + type;
        }

        Class<?> c=Class.forName( type );
        ModelerSource ds=(ModelerSource)c.newInstance();
        return ds;
    }


    // -------------------- Registration  --------------------
    
    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name) throws Exception 
    {
        this.server=server;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
    }

    public void postDeregister() {
    }

    
    
    
    // -------------------- DEPRECATED METHODS  --------------------
    // May still be used in tomcat 
    // Never part of an official release
    
    /** Called by a registry or by the container to unload a loader
     * @param loader
     */
    public void unregisterRegistry(ClassLoader loader ) {
        // XXX Cleanup ?
        perLoaderRegistries.remove(loader);
    }

    public ManagedBean findManagedBean(Class<?> beanClass, String type)
        throws Exception
    {
        return findManagedBean(null, beanClass, type);        
    }
    
    /**
     * Set the <code>MBeanServer</code> to be utilized for our
     * registered management beans.
     *
     * @param server The new <code>MBeanServer</code> instance
     */
    public void setMBeanServer( MBeanServer server ) {
        this.server=server;
    }

    public void resetMetadata() {
        stop();
    }
    /**
     * Load the registry from the XML input found in the specified input
     * stream.
     *
     * @param source Source to be used to load. Can be an InputStream or URL.
     *
     * @exception Exception if any parsing or processing error occurs
     */
    public void loadDescriptors( Object source )
            throws Exception
    {
        loadDescriptors("MbeansDescriptorsDOMSource", source, null );
    }


    // should be removed
    public void unregisterComponent( String domain, String name ) {
        try {
            ObjectName oname=new ObjectName( domain + ":" + name );

            // XXX remove from our tables.
            getMBeanServer().unregisterMBean( oname );
        } catch( Throwable t ) {
            log.log(Level.SEVERE, "Error unregistering mbean ", t );
        }
    }
    
    public List<ObjectName> loadMBeans( Object source )
            throws Exception
    {
        return loadMBeans( source, null );
    }

}
