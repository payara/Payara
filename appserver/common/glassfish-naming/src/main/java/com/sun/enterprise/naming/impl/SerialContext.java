/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2014 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.naming.impl;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.api.naming.NamingClusterInfo;
import org.glassfish.api.naming.NamingObjectProxy;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ORBLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

import static com.sun.enterprise.naming.util.LogFacade.logger;
import org.glassfish.config.support.TranslatedConfigView;

/**
 * This context provides access to the app server naming service. This
 * is the default Context for GlassFish. Lookups of unqualified names (i.e.
 * names not starting with "java:", "corbaname:" etc) are serviced by
 * SerialContext. The namespace is implemented in the
 * SerialContextProviderImpl object,  which is accessed directly in the
 * case that the client is collocated with the naming service impl or
 * remotely via RMI-IIOP if not collocated.
 * <p/>
 * <b>NOT THREAD SAFE: mutable instance variables</b>
 */
public class SerialContext implements Context {
    @LogMessageInfo(message = "Exception during name lookup : {0}",
    cause = "App Server may not be running at port intended, or possible Network Error.",
    action = "Check to see if the AppServer is up and running on the port intended. The problem could be because of incorrect port. Check to see if you can access the host on which the AppServer running.")
    public static final String EXCEPTION_DURING_LOOKUP = "AS-NAMING-00002";

    // Maximum number of recursive calls to lookup on comm error
    /** Maximum number of recursive calls to lookup on common error */
    private static final int MAX_LEVEL = 5 ;

    private static final String JAVA_URL = "java:";

    private static final String JAVA_GLOBAL_URL = "java:global";

    // Sets unmanaged SerialContext in test mode to prevent attempts to contact server. 
    static final String INITIAL_CONTEXT_TEST_MODE = "com.sun.enterprise.naming.TestMode";

    private static final NameParser myParser = new SerialNameParser();

    private static final Map<ProviderCacheKey, SerialContextProvider> providerCache =
            new HashMap<ProviderCacheKey, SerialContextProvider>();

    private static final ThreadLocal<ThreadLocalIC> stickyContext =
        new ThreadLocal<ThreadLocalIC>() {
            @Override
            protected ThreadLocalIC initialValue() {
                return new ThreadLocalIC() ;
            }
        } ;

    private Hashtable myEnv = null; // THREAD UNSAFE

    private SerialContextProvider provider;

    private final String myName;

    private final JavaURLContext javaUrlContext;

    private ServiceLocator services;

    private boolean testMode = false;

    private ProcessType processType = ProcessType.Server;

    private ORB orbFromEnv;

    private String targetHost;
    private String targetPort;

    private ORB orb = null ;
    
    /** True if we're running in the server and no orb,host, or port
    * properties have been explicitly set in the properties
    * Allows special optimized intra-server naming service access
     */
    private boolean intraServerLookups;

    /** Common Class Loader. It is used as a fallback classloader to locate
    * GlassFish object factories.
    */ 
    private ClassLoader commonCL;

    /** Methods for preserving stickiness. This is a
     * solution to store the sticky IC as a thread local variable. This sticky
     * IC will be used by all classes that require a context object to do lookup
     * (if LB is enabled) SerialContext.lookup() sets a value for the thread
     * local variable (stickyContext) before performing th lookup in case LB is
     * enabled. If not, the thread local variable is null. At the end of the
     * SerialContext.lookup() method, the thread local variable gets set to
     * null. So actually speaking, more than being a global variable for the
     * entire thread, its global only during the execution of the
     * SerialContext.lookup() method. bug 5050591
     *
     */
    static Context getStickyContext() {
        return stickyContext.get().getContext() ;
    }

    private void grabSticky() {
        stickyContext.get().grab( this ) ;
    }

    private void releaseSticky() {
        stickyContext.get().release() ;
    }

    static void clearSticky() {
        stickyContext.get().clear() ;
    }

    /** Store the sticky context as a threadlocal variable (bug 5050591).
    * Count is needed to know how many times the lookup method is being called
    * from within the user code's ic.lookup().
    * e.g. JMS resource lookups (via ConnectorObjectFactory)
    */
    private static class ThreadLocalIC {
        private Context ctx = null ;
        private int count = 0;

        Context getContext() {
            return ctx ;
        }

        void grab( Context context ) {
            if (ctx == null) {
                ctx = context ;
            }

            count++ ;
        }

        void release() {
            if (count > 0) {
                count-- ;
                if (count == 0) {
                    ctx = null ;
                }
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "SerialContext: attempt to release StickyContext without grab");
                }
                ctx = null;
            }
        }

        void clear() {
            ctx = null ;
            count = 0 ;
        }
    }


    /**
     * Constructor for the context. Initializes the object reference to the
     * remote provider object.
     */
    public SerialContext(String name, Hashtable environment, ServiceLocator h)
            throws NamingException {

        services = h;
        myEnv = (Hashtable) environment.clone();

        // TODO REMOVE when property stuff is figured out
        myEnv.put("java.naming.factory.url.pkgs",
            "com.sun.enterprise.naming");
        myEnv.put("java.naming.factory.state",
            "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl" );

        this.myName = name;
        if( myEnv.get(INITIAL_CONTEXT_TEST_MODE) != null ) {
            testMode = true;
            System.out.println("SerialContext in test mode");
        }

        if( (services == null) && !testMode ) {
            synchronized(SerialContext.class) {
                if( SerialInitContextFactory.getDefaultServices() == null ) {

                    // Bootstrap a hk2 environment.
                    // TODO This will need to be moved somewhere else.  Potentially any
                    // piece of glassfish code that can be an initial entry point from a
                    // Java SE client will need to make this happen.

                    services = Globals.getStaticHabitat();

                    SerialInitContextFactory.setDefaultServices(services);
                } else {
                    services = SerialInitContextFactory.getDefaultServices();
                }
            }
        }
        
        if( testMode ) {
            processType = ProcessType.Server;
        } else {
            ProcessEnvironment processEnv = services.getService(ProcessEnvironment.class);
            if (processEnv == null) {
                processEnv = services.create(ProcessEnvironment.class);
                services.inject(processEnv);
                services.postConstruct(processEnv);
            }
            
            processType = processEnv.getProcessType();
        }

        // using these two temp variables allows instance variables
        // to be 'final'.
        JavaURLContext urlContextTemp = null;

        if (myEnv.get(NamingClusterInfo.IIOP_URL_PROPERTY) != null) {
            urlContextTemp = new JavaURLContext(myEnv, this);
        } else {
            urlContextTemp = new JavaURLContext(myEnv, null);
        }

        javaUrlContext = urlContextTemp;

        orbFromEnv  = (ORB) myEnv.get(ORBLocator.JNDI_CORBA_ORB_PROPERTY);
        String targetHostFromEnv = (String)myEnv.get(
            ORBLocator.OMG_ORB_INIT_HOST_PROPERTY);
        String targetPortFromEnv = (String)myEnv.get(
            ORBLocator.OMG_ORB_INIT_PORT_PROPERTY);

        intraServerLookups = (processType.isServer()) && (orbFromEnv == null) &&
            (targetHostFromEnv == null) && (targetPortFromEnv == null);

        // Set target host / port from env.  If only one of the two is set, fill in the
        // other with the default.  
        if( targetHostFromEnv != null ) {
            targetHost = targetHostFromEnv;
            if( targetPortFromEnv == null ) {
                targetPort = ORBLocator.DEFAULT_ORB_INIT_PORT;
            }
        }

        if( targetPortFromEnv != null ) {
            targetPort = targetPortFromEnv;
            if( targetHostFromEnv == null ) {
                targetHost = ORBLocator.DEFAULT_ORB_INIT_HOST;
            }
        }

        orb = orbFromEnv;
        if (services != null) { // can happen in test mode
            ServerContext sc = services.getService(ServerContext.class);
            if (sc != null) {
                commonCL = sc.getCommonClassLoader();
            }
        }
    }

    /**
     * This constructor takes the component id as an argument. All name
     * arguments to operations are prepended by the component id.
     */
    public SerialContext(Hashtable env, ServiceLocator services) throws NamingException {
        this("", env, services);
    }

    private SerialContextProvider getProvider() throws NamingException {
        SerialContextProvider returnValue = provider;

        if (provider == null) {
            try {
                if (intraServerLookups) {
                    returnValue =
                        ProviderManager.getProviderManager().getLocalProvider();
                }  else {
                    returnValue = getRemoteProvider();
                }
            } catch(Exception e) {
                clearSticky() ;
                e.printStackTrace();
                NamingException ne = new NamingException(
                    "Unable to acquire SerialContextProvider for " + this);
                ne.initCause(e);
                throw ne;
            }
        }

        return returnValue;
    }

    private ORB getORB() {
        ORBLocator orbHelper = services.getService(ORBLocator.class);
        if (orb == null) {
            orb = orbHelper.getORB() ;
        }

        return orb ;
    }

    private ProviderCacheKey getProviderCacheKey() {
        final ORB myORB = getORB();
        ProviderCacheKey key;

        String eplist = null ;
        if (myEnv != null) {
            eplist = (String)myEnv.get(
                    NamingClusterInfo.IIOP_URL_PROPERTY) ;
        }

        if (eplist != null) {
            key = new ProviderCacheKey(eplist) ;
        } else if (targetHost == null) {
            key = new ProviderCacheKey(myORB) ;
        }  else {
            key = new ProviderCacheKey(targetHost, targetPort);
        }

        return key ;
    }

    private void clearProvider() {
        ProviderCacheKey key = getProviderCacheKey() ;
        synchronized(SerialContext.class) {
            providerCache.remove( key ) ;
        }
        provider = null ;
    }

    private SerialContextProvider getRemoteProvider() throws Exception {
        if (provider == null) {
            ProviderCacheKey key = getProviderCacheKey() ;

            SerialContextProvider cachedProvider;
            synchronized(SerialContext.class) {
                cachedProvider = providerCache.get(key);
            }

            if (cachedProvider == null) {
                // Don't hold lock during this call: remote invocation
                SerialContextProvider newProvider = key.getNameService() ;

                synchronized(SerialContext.class) {
                    cachedProvider = providerCache.get(key);
                    if (cachedProvider == null)  {
                        providerCache.put(key, newProvider);
                        provider = newProvider;
                    } else {
                        provider = cachedProvider;
                    }
                }
            } else {
                provider = cachedProvider;
            }
                       
        }

        return provider;
    }

    /**
     * The getNameInNamespace API is not supported in this context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        return myName;
    }

    /**
     * method to check if the name to look up starts with "java:"
     */
    private boolean isjavaURL(String name) {
        return name.startsWith(JAVA_URL) && !name.startsWith(JAVA_GLOBAL_URL) ;
    }

    /**
     * Lookup the specified name in the context. Returns the resolved object.
     *
     * @return the resolved object.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookup(String name) throws NamingException {
        return lookup( name, 0 ) ;
    }

    private Object lookup(String name, int level ) throws NamingException {
        // Before any lookup bind any NamedNamingObjectProxy
        // Skip if in plain Java SE client
        // TODO this should really be moved somewhere else
        NamedNamingObjectManager.checkAndLoadProxies(services);

        /**
         * In case a user is creating an IC with env passed in constructor; env
         * specifies endpoints in some form in that case, the sticky IC should
         * be stored as a thread local variable.
         *
         */
        final boolean useSticky = myEnv.get(NamingClusterInfo.IIOP_URL_PROPERTY) != null ;

        if (useSticky) {
            grabSticky() ;
        }

        try {
            if (name.isEmpty()) {
                // Asking to look up this context itself. Create and return
                // a new instance with its own independent environment.
                return (new SerialContext(myName, myEnv, services));
            }

            name = TranslatedConfigView.expandValue(name);
            name = getRelativeName(name);

            if (isjavaURL(name)) {
                //it is possible that the object bound in a java url ("java:") is
                //reference object.
                Object o = javaUrlContext.lookup(name);
                if(o instanceof Reference){
                    o = getObjectInstance(name, o);
                }
                return o;
            } else {
                SerialContextProvider prvdr = getProvider() ;
                Object obj = prvdr.lookup(name);
                if (obj instanceof NamingObjectProxy) {
                    return ((NamingObjectProxy) obj).create(this);
                }

                if (obj instanceof Context) {
                    return new SerialContext(name, myEnv, services);
                }

                Object retObj = getObjectInstance(name, obj);
                return retObj;
            }
        } catch (NamingException nnfe) {
            NamingException ne = new NamingException
                    ("Lookup failed for '" + name + "' in " + this);
            ne.initCause(nnfe);
            throw ne;
        } catch (Exception ex) {
            // Issue 14732: make this FINE, as a cluster configuration change
            // can send us here in a normal retry scenario.
            logger.log(Level.FINE, EXCEPTION_DURING_LOOKUP, name);
            logger.log(Level.FINE, "", ex);

            final int nextLevel = level + 1 ;

            // temp fix for 6320008
            // this should be removed once we change the transient NS
            // implementation to persistent
            if (ex instanceof java.rmi.MarshalException
                    && ex.getCause() instanceof org.omg.CORBA.COMM_FAILURE
                    && nextLevel < MAX_LEVEL) {
                clearProvider();
                logger.fine("Resetting provider to NULL. Will get new obj ref for provider since previous obj ref was stale...");
                return lookup(name, nextLevel );
            } else {
                CommunicationException ce = new CommunicationException(
                    "Communication exception for " + this);
                ce.initCause(ex);
                throw ce;
            }
        } finally {
            if (useSticky) {
                releaseSticky();
            }
        }
    }

    private Object getObjectInstance(String name, Object obj) throws Exception
    {
        Object retObj = javax.naming.spi.NamingManager.getObjectInstance(
            obj, new CompositeName(name), null, myEnv);

        if (retObj == obj) {
            // NamingManager.getObjectInstance() returns the same object
            // when it can't find the factory class. Since NamingManager
            // uses Thread's context class loader to locate factory classes,
            // it may not be able to locate the various GlassFish factories
            // when lookup is performed outside of a Java EE context like
            // inside an OSGi bundle's activator.
            // So, let's see if using CommonClassLoader helps or not.
            // We will only try with CommonClassLoader when the passed object
            // reference has a factory class name set.

            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            if (tccl != commonCL) {
                Reference ref = getReference(obj);
                if (ref != null) {
                    logger.logp(Level.FINE, "SerialContext", "getObjectInstance",
                            "Trying with CommonClassLoader for name {0} ", name);
                    ObjectFactory factory = getObjectFactory(ref, commonCL);
                    if (factory != null) {
                        retObj = factory.getObjectInstance(
                                ref, new CompositeName(name), null, myEnv);
                    }
                    if (retObj != obj) {
                        logger.logp(Level.FINE, "SerialContext", "getObjectInstance",
                                "Found with CommonClassLoader");
                    }
                }
            }
        }
        return retObj;
    }

    /**
     * This method tries to check if the passed object is a Reference or
     * Refenciable. If it is a Reference, it just casts it to a Reference and
     * returns, else if it is a Referenceable, it tries to get a Reference from
     * the Referenceable and returns that, otherwise, it returns null.
     *
     * @param obj
     * @return
     * @throws NamingException
     */
    private Reference getReference(Object obj) throws NamingException
    {
        Reference ref = null;
        if (obj instanceof Reference) {
            ref = (Reference) obj;
        } else if (obj instanceof Referenceable) {
            ref = ((Referenceable)(obj)).getReference();
        }

        return ref;
    }

    /**
     * It tries to load the factory class for the given reference using the
     * given class loader and return an instance of the same. Returns null
     * if it can't load the class.
     *
     * @param ref
     * @param cl
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private ObjectFactory getObjectFactory(Reference ref, ClassLoader cl)
            throws IllegalAccessException, InstantiationException
    {
        String factoryName = ref.getFactoryClassName();
        if (factoryName != null) {
            try
            {
                Class c = Class.forName(factoryName, false, cl);
                return (ObjectFactory)c.newInstance();
            }
            catch (ClassNotFoundException e)
            {
                // ignore only CNFE, all other exceptions are considered errors
            }
        }
        return null;
    }

    /**
     * Lookup the specifed name in the context. Returns the resolved object.
     *
     * @return the resolved object.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return lookup(name.toString());
    }

    /**
     * Bind the object to the specified name.
     *
     * @param name name that the object is being bound to.
     * @param obj object that is being bound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {

        name = getRelativeName(name);
        if (isjavaURL(name)) {
            javaUrlContext.bind(name, obj);
        } else {
            try {
                getProvider().bind(name, obj);
            } catch (RemoteException ex) {
                throw new CommunicationException(ex.toString());
            }
        }
    }

    /**
     * Bind the object to the specified name.
     *
     * @param name name that the object is being bound to.
     * @param obj  object that is being bound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        // Flat namespace; no federation; just call string version
        bind(name.toString(), obj);
    }

    /**
     * Rebind the object to the specified name.
     *
     * @param name name that the object is being bound to.
     * @param obj  object that is being bound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rebind(String name, Object obj) throws NamingException {

        name = getRelativeName(name);
        if (isjavaURL(name)) {
            javaUrlContext.rebind(name, obj);
        } else {
            try {
                getProvider().rebind(name, obj);
            } catch (RemoteException ex) {
                throw new CommunicationException(ex.toString());
            }
        }
    }

    /**
     * Rebind the object to the specified name.
     *
     * @param name name that the object is being bound to.
     * @param obj  object that is being bound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        // Flat namespace; no federation; just call string version
        rebind(name.toString(), obj);
    }

    /**
     * Unbind the object with the specified name.
     *
     * @param name that is being unbound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void unbind(String name) throws NamingException {
        name = getRelativeName(name);
        if (isjavaURL(name)) {
            javaUrlContext.unbind(name);
        } else {
            try {
                getProvider().unbind(name);
            } catch (RemoteException ex) {
                throw new CommunicationException(ex.toString());
            }
        }
    }

    /**
     * Unbind the object with the specified name.
     *
     * @param name name that is being unbound.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void unbind(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        unbind(name.toString());
    }

    /**
     * Rename the bound object.
     *
     * @param oldname old name that the object is bound as.
     * @param newname new name that the object will be bound as.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rename(String oldname, String newname) throws NamingException {
        oldname = getRelativeName(oldname);
        newname = getRelativeName(newname);
        if (isjavaURL(oldname)) {
            javaUrlContext.rename(oldname, newname);
        } else {
            try {
                getProvider().rename(oldname, newname);
            } catch (RemoteException ex) {
                throw new CommunicationException(ex.toString());
            }
        }
    }

    /**
     * Rename the bound object.
     *
     * @param oldname old name that the object is bound as.
     * @param newname new name that the object will be bound as.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void rename(Name oldname, Name newname) throws NamingException {
        // Flat namespace; no federation; just call string version
        rename(oldname.toString(), newname.toString());
    }

    /**
     * List the contents of the specified context.
     *
     * @param name context name.
     * @return an enumeration of the contents.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        if (name.isEmpty()) {
            // listing this context
            try {
                Hashtable bindings = getProvider().list(myName);
                return new RepNames<NameClassPair>(bindings);
            } catch (RemoteException ex) {
                throw new CommunicationException(ex.toString());
            }
        }

        name = getRelativeName(name);
        if (isjavaURL(name)) {
            return javaUrlContext.list(name);
        } else {
            // Perhaps 'name' names a context
            Object target = lookup(name);
            if (target instanceof Context) {
                return ((Context) target).list("");
            }
            throw new NotContextException(name + " cannot be listed");
        }
    }

    /**
     * List the contents of the specified context.
     *
     * @param name context name.
     * @return an enumeration of the contents.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return list(name.toString());
    }

    /**
     * List the bindings in the specified context.
     *
     * @param name context name.
     * @return an enumeration of the bindings.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        if (name.isEmpty()) {
            // listing this context
            try {
                Hashtable bindings = getProvider().list(myName);
                return new RepBindings<Binding>(bindings);
            } catch (RemoteException ex) {
                CommunicationException ce = new CommunicationException(ex
                        .toString());
                ce.initCause(ex);
                throw ce;
            }
        }

        name = getRelativeName(name);
        if (isjavaURL(name)) {
            return javaUrlContext.listBindings(name);
        } else {
            // Perhaps 'name' names a context
            Object target = lookup(name);
            if (target instanceof Context) {
                return ((Context) target).listBindings("");
            }
            throw new NotContextException(name + " cannot be listed");
        }
    }

    /**
     * List the bindings in the specified context.
     *
     * @param name context name.
     * @return an enumeration of the bindings.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return listBindings(name.toString());
    }

    /**
     * Destroy the specified subcontext.
     *
     * @param name name of the subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        name = getRelativeName(name);
        if (isjavaURL(name)) {
            javaUrlContext.destroySubcontext(name);
        } else {
            try {
                getProvider().destroySubcontext(name);
            } catch (RemoteException e) {
                CommunicationException ce = new CommunicationException(e
                        .toString());
                ce.initCause(e);
                throw ce;
            }
        }
    }

    /**
     * Destroy the specified subcontext.
     *
     * @param name name of the subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        destroySubcontext(name.toString());
    }

    /**
     * Create the specified subcontext.
     *
     * @param name name of the subcontext.
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        Context c = null;
        name = getRelativeName(name);
        if (isjavaURL(name)) {
            return javaUrlContext.createSubcontext(name);
        } else {
            try {
                getProvider().createSubcontext(name);
                /*
                 * this simulates the transient context structure on the client
                 * side. Have to do this - as reference to Transient Context is
                 * not resolved properly due to rmi
                 */
                c = new SerialContext(name, myEnv, services);
            } catch (RemoteException e) {
                CommunicationException ce = new CommunicationException(e
                        .toString());
                ce.initCause(e);
                throw ce;
            }
            return c;
        }
    }

    /**
     * Create the specified subcontext.
     *
     * @param name name of the subcontext.
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return createSubcontext(name.toString());
    }

    /**
     * Links are not treated specially.
     *
     * @param name name of the link.
     * @return the resolved object.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        name = getRelativeName(name);
        if (isjavaURL(name)) {
            return javaUrlContext.lookupLink(name);
        } else {
            // This flat context does not treat links specially
            return lookup(name);
        }
    }

    /**
     * Links are not treated specially.
     *
     * @param name name of the link.
     * @return the resolved object.
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return lookupLink(name.toString());
    }

    /**
     * Allow access to the name parser object.
     *
     * @param name JNDI name, is ignored since there is only one Name Parser
     *             object.
     * @return NameParser object
     * @throws NamingException
     */
    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return myParser;
    }

    /**
     * Allow access to the name parser object.
     *
     * @param name JNDI name, is ignored since there is only one Name Parser
     *             object.
     * @return NameParser object
     * @throws NamingException
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        // Flat namespace; no federation; just call string version
        return getNameParser(name.toString());
    }

    @Override
    public String composeName(String name, String prefix)
            throws NamingException {
        Name result = composeName(new CompositeName(name), new CompositeName(
                prefix));
        return result.toString();
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        Name result = (Name) (prefix.clone());
        result.addAll(name);
        return result;
    }

    /**
     * Add to the environment for the current context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object addToEnvironment(String propName, Object propVal)
            throws NamingException {
        if (myEnv == null) {
            myEnv = new Hashtable(5, 0.75f);
        }
        return myEnv.put(propName, propVal);
    }

    /**
     * Remove from the environment for the current context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        if (myEnv == null) {
            return null;
        }
        return myEnv.remove(propName);
    }

    /**
     * Return the environment for the current context.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public Hashtable getEnvironment() throws NamingException {
        if (myEnv == null) {
            // Must return non-null
            myEnv = new Hashtable(3, 0.75f);
        }
        return myEnv;
    }

    /**
     * Set the environment for the current context to null when close is called.
     *
     * @throws NamingException if there is a naming exception.
     */
    @Override
    public void close() throws NamingException {
        myEnv = null;
    }

    private String getRelativeName(String name) {
        if (!myName.equals("")) {
            name = myName + "/" + name;
        }
        return name;
    }

    // Class for enumerating name/class pairs
    static class RepNames<T> implements NamingEnumeration<T> {
        Hashtable bindings;

        Enumeration names;

        RepNames(Hashtable bindings) {
            this.bindings = bindings;
            this.names = bindings.keys();
        }

        @Override
        public boolean hasMoreElements() {
            return names.hasMoreElements();
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public T nextElement() {
            if (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                String className = bindings.get(name).getClass().getName();
                return (T) (new NameClassPair(name, className));
            } else {
                return null;
            }
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }

        @Override
        public void close() {
            //no-op since no steps needed to free up resources
        }
    }

    // Class for enumerating bindings
    static class RepBindings<T> implements NamingEnumeration<T> {
        Enumeration names;

        Hashtable bindings;

        RepBindings(Hashtable bindings) {
            this.bindings = bindings;
            this.names = bindings.keys();
        }

        @Override
        public boolean hasMoreElements() {
            return names.hasMoreElements();
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public T nextElement() {
            if (hasMoreElements()) {
                String name = (String) names.nextElement();
                return (T) (new Binding(name, bindings.get(name)));
            } else {
                return null;
            }
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }

        @Override
        public void close() {
            //no-op since no steps needed to free up resources
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SerialContext[");
        if(testMode) {
            sb.append("( IN TEST MODE ) ");
        }

        sb.append( "myEnv=" ) ;
        sb.append( myEnv ) ;

        return sb.toString();

    }

    private class ProviderCacheKey {
        // Key is either orb OR host/port combo.
        private ORB localORB = null ;
        private String endpoints = null ;

        ProviderCacheKey(ORB orb) {
            this.localORB  = orb;
        }

        // Host and Port must both be non-null
        ProviderCacheKey(String host, String port) {
            endpoints = "corbaloc:iiop:1.2@" + host + ":" + port
                + "/NameService" ;
        }

        ProviderCacheKey( String endpoints ) {
            this.endpoints = endpoints ;
        }

        @Override
        public String toString() {
            if (localORB == null) {
                return "ProviderCacheKey[" + endpoints + "]" ;
            } else {
                return "ProviderCacheKey[" + localORB + "]" ;
            }
        }

        public SerialContextProvider getNameService() throws InvalidName,
            NotFound, CannotProceed,
            org.omg.CosNaming.NamingContextPackage.InvalidName {

            org.omg.CORBA.Object objref = null;
            if (endpoints == null) {
                objref = orb.resolve_initial_references( "NameService");
            } else {
                objref = orb.string_to_object(endpoints) ;
            }

            final NamingContext nctx = NamingContextHelper.narrow(objref);
            final NameComponent[] path =
                { new NameComponent("SerialContextProvider", "") };
            final org.omg.CORBA.Object obj = nctx.resolve(path) ;

            SerialContextProvider result =
                (SerialContextProvider)PortableRemoteObject.narrow( obj,
                    SerialContextProvider.class );

            return result ;
        }

        @Override
        public int hashCode() {
            return (orb != null) ? orb.hashCode() : endpoints.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false ;
            }

            if (!(other instanceof ProviderCacheKey)) {
                return false ;
            }

            ProviderCacheKey otherKey = (ProviderCacheKey) other;

            if (localORB != null) {
                return localORB == otherKey.localORB ;
            } else {
                if (endpoints == null) {
                    return otherKey.endpoints == null ;
                }

                return endpoints.equals(otherKey.endpoints) ;
            }
        }
    }
}
