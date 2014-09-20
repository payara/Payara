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

package com.sun.ejb;

import com.sun.ejb.codegen.ClassGeneratorFactory;
import com.sun.ejb.codegen.AsmSerializableBeanGenerator;
import com.sun.ejb.codegen.GenericHomeGenerator;
import com.sun.ejb.codegen.Remote30WrapperGenerator;
import com.sun.ejb.codegen.RemoteGenerator;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.RemoteBusinessWrapperBase;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.GenericEJBLocalHome;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbReferenceDescriptor;

import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.io.*;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper.*;
import com.sun.logging.LogDomains;

/**
 * A handy class with static utility methods.
 *
 * Note that much of this code has to execute in the client so
 * it needs to be careful about which server-only resources it
 * uses and in which code paths.  
 *
 */
public class EJBUtils {

    //
    private static final Logger _logger =
            LogDomains.getLogger(EJBUtils.class, LogDomains.EJB_LOGGER);


    // Internal property to force generated ejb container classes to
    // be created during deployment time instead of dynamically.  Note that
    // this property does *not* cover RMI-IIOP stub generation.  
    // See IASEJBC.java for more details.
    private static final String EJB_USE_STATIC_CODEGEN_PROP =
        "com.sun.ejb.UseStaticCodegen";

    private static final String REMOTE30_HOME_JNDI_SUFFIX =
        "__3_x_Internal_RemoteBusinessHome__";

    private static Boolean ejbUseStaticCodegen_ = null;

    // Initial portion of a corba interoperable naming syntax jndi name.
    private static final String CORBA_INS_PREFIX = "corbaname:";

    // Prefix of portable global JNDI namespace
    private static final String JAVA_GLOBAL_PREFIX = "java:global/";

    // Separator between simple and fully-qualified portable ejb global JNDI names
    private static final String PORTABLE_JNDI_NAME_SEP = "!";

    // Separator between simple and fully-qualified glassfish-specific JNDI names
    private static final String GLASSFISH_JNDI_NAME_SEP = "#";

    /**
     * Utility methods for serializing EJBs, primary keys and 
     * container-managed fields, all of which may include Remote EJB 
     * references,
     * Local refs, JNDI Contexts etc which are not Serializable.
     * This is not used for normal RMI-IIOP serialization.
     * It has boolean replaceObject control, whether to call replaceObject 
     * or not
     */
    public static final byte[] serializeObject(Object obj, 
                                               boolean replaceObject)
	    throws IOException
    {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().serializeObject(obj, replaceObject);
    }

    public static final byte[] serializeObject(Object obj)
        throws IOException
    {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().serializeObject(obj, true);
    }

    /**
     * Utility method for deserializing EJBs, primary keys and 
     * container-managed fields, all of which may include Remote 
     * EJB references,
     * Local refs, JNDI Contexts etc which are not Serializable.
     */
    public static final Object deserializeObject(byte[] data, 
            ClassLoader loader, boolean resolveObject)
        throws Exception
    {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().deserializeObject(data, resolveObject, loader);
    }

    public static final Object deserializeObject(byte[] data, 
                                                 ClassLoader loader)
        throws Exception
    {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().deserializeObject(data, true, loader);
    }

    public static boolean useStaticCodegen() {
        synchronized (EJBUtils.class) {
            if( ejbUseStaticCodegen_ == null ) {
                String ejbStaticCodegenProp = null;
                if(System.getSecurityManager() == null) {
                    ejbStaticCodegenProp = 
                        System.getProperty(EJB_USE_STATIC_CODEGEN_PROP);
                } else {
                    ejbStaticCodegenProp = (String)
                    java.security.AccessController.doPrivileged
                            (new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            return 
                                System.getProperty(EJB_USE_STATIC_CODEGEN_PROP);
                        }});
                }
                      
                boolean useStaticCodegen = 
                    ( (ejbStaticCodegenProp != null) &&
                      ejbStaticCodegenProp.equalsIgnoreCase("true"));
                
                ejbUseStaticCodegen_ = useStaticCodegen;

                _logger.log(Level.FINE, "EJB Static codegen is " +
                            (useStaticCodegen ? "ENABLED" : "DISABLED") +
                            " ejbUseStaticCodegenProp = " + 
                            ejbStaticCodegenProp);
            }
        }

        return ejbUseStaticCodegen_.booleanValue();
        
    }

    private static String getClassPackageName(String intf) {
        int dot = intf.lastIndexOf('.');
        return (dot == -1) ? null : intf.substring(0, dot);
    }

    private static String getClassSimpleName(String intf) {
        int dot = intf.lastIndexOf('.');
        return (dot == -1) ? intf : intf.substring(dot+1);
    }

    public static String getGeneratedOptionalInterfaceName(String ejbClassName) {
        String packageName = getClassPackageName(ejbClassName);
        String simpleName = getClassSimpleName(ejbClassName);
        String optionalIntfName = "__EJB31_Generated__" + simpleName + "__Intf__";
        return (packageName != null) ?
            packageName + "." + optionalIntfName : optionalIntfName;
    }

    public static String getGeneratedSerializableClassName(String beanClass) {
        String packageName = getClassPackageName(beanClass);
        String simpleName = getClassSimpleName(beanClass);
        String generatedSimpleName = "_" + simpleName + "_Serializable";
        return (packageName != null) ? 
            packageName + "." + generatedSimpleName : generatedSimpleName;
    }

    public static String getGeneratedRemoteIntfName(String businessIntf) {
        String packageName = getClassPackageName(businessIntf);
        String simpleName = getClassSimpleName(businessIntf);
        String generatedSimpleName = "_" + simpleName + "_Remote";
        return (packageName != null) ? 
            packageName + "." + generatedSimpleName : generatedSimpleName;
    }

    public static String getGeneratedRemoteWrapperName(String businessIntf) {
        String packageName = getClassPackageName(businessIntf);
        String simpleName = getClassSimpleName(businessIntf);
        String generatedSimpleName = "_" + simpleName + "_Wrapper";
        return (packageName != null) ? 
            packageName + "." + generatedSimpleName : generatedSimpleName;
    }

    public static String getGenericEJBHomeClassName() {
        return "com.sun.ejb.codegen.GenericEJBHome_Generated";
    }

    /**
     * Actual jndi-name under which Remote ejb factory lives depends on
     * whether it's a Remote Home view or Remote Business view.  This is
     * necessary since a single session bean can expose both views and
     * the resulting factory objects are different.  These semantics are
     * not exposed to the developer-view to keep things simpler.  The
     * developer can simply deal with a single physical jndi-name.  If the
     * target bean exposes both a Remote Home view and a Remote Business
     * view, the developer can still use the single physical jndi-name
     * to resolve remote ejb-refs, and we will handle the distinction 
     * internally.  Of course, this is based on the assumption that the
     * internal name is generated in a way that will not clash with a
     * separate top-level physical jndi-name chosen by the developer.  
     * 
     * Note that it's better to delay this final jndi name translation as
     * much as possible and do it right before the NamingManager lookup,
     * as opposed to changing the jndi-name within the descriptor objects
     * themselves.  This way, the extra indirection will not be exposed
     * if the descriptors are written out and they won't complicate any
     * jndi-name equality logic.
     * 
     */
    public static String getRemoteEjbJndiName(EjbReferenceDescriptor refDesc) {

        String intf = refDesc.isEJB30ClientView() ?
                refDesc.getEjbInterface() : refDesc.getHomeClassName();

        return getRemoteEjbJndiName(refDesc.isEJB30ClientView(),
                                    intf,
                                    refDesc.getJndiName());
    }

    public static String getRemote30HomeJndiName(String jndiName) {
        return jndiName + REMOTE30_HOME_JNDI_SUFFIX;
    }

    public static String getRemoteEjbJndiName(boolean businessView,
                                              String interfaceName,
                                              String jndiName) {

        String returnValue = jndiName;

        String portableFullyQualifiedPortion = PORTABLE_JNDI_NAME_SEP + interfaceName;
        String glassfishFullyQualifiedPortion = GLASSFISH_JNDI_NAME_SEP + interfaceName;

        if( businessView ) {
            if( jndiName.startsWith(CORBA_INS_PREFIX) ) {


                // In the case of a corba interoperable naming string, we
                // need to lookup the internal remote home.  We can't rely
                // on our SerialContext Reference object (RemoteBusinessObjectFactory)
                // to do the home lookup because we have to directly access
                // the CosNaming service.

                // First, strip off any fully-qualified portion since there's only
                // one internal generic home object in CosNaming no matter how many
                // remote business interfaces there are.

                // Separate <jndi-name> portion from "corbaname:iiop:...#<jndi-name>
                // We need to do this since we also use "#" in some glassfish-specific
                // JNDI names
                int indexOfCorbaNameSep = jndiName.indexOf("#");
                String jndiNameMinusCorbaNamePortion = jndiName.substring(indexOfCorbaNameSep + 1);

                // Make sure any of the resulting jndi names still have corbaname: prefix intact
                String newJndiName = jndiName;

                if( jndiNameMinusCorbaNamePortion.startsWith(JAVA_GLOBAL_PREFIX) ){

                    newJndiName = stripFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);

                } else if( jndiNameMinusCorbaNamePortion.endsWith(glassfishFullyQualifiedPortion ) ){

                    newJndiName = stripFullyQualifiedJndiName(jndiName, glassfishFullyQualifiedPortion);

                }

                returnValue = getRemote30HomeJndiName(newJndiName);

            } else {
                // Convert to fully-qualified names
                if( jndiName.startsWith(JAVA_GLOBAL_PREFIX)) {
                    returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
                } else {
                    returnValue = checkFullyQualifiedJndiName(jndiName, glassfishFullyQualifiedPortion);
                }
            }
        } else {

            // EJB 2.x Remote  Home

            // Only in the portable global case, convert to a fully-qualified name
            if( jndiName.startsWith(JAVA_GLOBAL_PREFIX)) {
                returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
            }                      
        }

        return returnValue;
    }

    private static String checkFullyQualifiedJndiName(String origJndiName, String fullyQualifiedPortion) {
        String returnValue = origJndiName;
        if( !origJndiName.endsWith(fullyQualifiedPortion) ) {
            returnValue = origJndiName + fullyQualifiedPortion;
        }
        return returnValue;
    }

    private static String stripFullyQualifiedJndiName(String origJndiName, String fullyQualifiedPortion) {
        String returnValue = origJndiName;
        if( origJndiName.endsWith(fullyQualifiedPortion) ) {
            int portionLength = fullyQualifiedPortion.length();
            returnValue = origJndiName.substring(0, origJndiName.length() - portionLength );
        }
        return returnValue;
    }


    public static Object resolveEjbRefObject(EjbReferenceDescriptor refDesc,
                                             Object jndiObj) 
        throws NamingException {

        Object returnObject = jndiObj;

        if( refDesc.isLocal() ) {

            EjbDescriptor target = refDesc.getEjbDescriptor();           

            BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(target.getUniqueId());

            if( refDesc.isEJB30ClientView() ) {
                GenericEJBLocalHome genericLocalHome = 
                    container.getEJBLocalBusinessHome(refDesc.getEjbInterface());
                returnObject = genericLocalHome.create(refDesc.getEjbInterface());
            } else {
                returnObject = container.getEJBLocalHome();
            }

        } else {

            // For the Remote case, the only time we have to do 
            // something extra with the given jndiObj is if the lookup 
            // is for a Remote 3.0 object and it was made through a
            // corba interoperable name.  In that case,
            // the jndiObj refers to the internal Remote 3.0 Home so we
            // still need to create a remote 30 client wrapper object.

            if ( refDesc.isEJB30ClientView() &&
                 !(jndiObj instanceof RemoteBusinessWrapperBase) ) {
                returnObject = EJBUtils.lookupRemote30BusinessObject
                    (jndiObj, refDesc.getEjbInterface());
            }

        }

        return returnObject;

    }

    public static Object lookupRemote30BusinessObject(Object jndiObj,
                                                      String businessInterface)
        throws NamingException
        
    {
        Object returnObject = null;

        try {
            
            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            Class genericEJBHome = loadGeneratedGenericEJBHomeClass
                (loader);
            
            final Object genericHomeObj =
                PortableRemoteObject.narrow(jndiObj, genericEJBHome);
            
            // The generated remote business interface and the
            // client wrapper for the business interface are produced
            // dynamically.  The following call must be made before
            // any EJB 3.0 Remote business interface runtime behavior
            // is needed in a given JVM.  
            loadGeneratedRemoteBusinessClasses(businessInterface);
            
            String generatedRemoteIntfName = EJBUtils.
                getGeneratedRemoteIntfName(businessInterface);
            
            Method createMethod = genericEJBHome.getMethod
                ("create", String.class);
            
            java.rmi.Remote delegate = (java.rmi.Remote)
                createMethod.invoke(genericHomeObj, 
                                    generatedRemoteIntfName);
                       
            
            returnObject = createRemoteBusinessObject
                (loader, businessInterface, delegate);


            // TODO Bring over appclient security exception retry logic  CR 6620388

        } catch(Exception e) {
            NamingException ne = new NamingException
                ("ejb ref resolution error for remote business interface" 
                 + businessInterface);
            
            ne.initCause(e instanceof InvocationTargetException ?
                         e.getCause() : e);
            throw ne;
        }

        return returnObject;
               
    }

    public static Class loadGeneratedSerializableClass(ClassLoader loader, String className)
        throws Exception {
        String generatedSerializableClassName = getGeneratedSerializableClassName(className);

        Class developerClass = loader.loadClass(className);
        AsmSerializableBeanGenerator gen = new AsmSerializableBeanGenerator
                (loader, developerClass, generatedSerializableClassName);
                                     
        Class serializableClass = gen.generateSerializableSubclass();

        return serializableClass;
    }

    public static void loadGeneratedRemoteBusinessClasses
        (String businessInterfaceName) throws Exception {

        ClassLoader appClassLoader = 
            getBusinessIntfClassLoader(businessInterfaceName);
        
        loadGeneratedRemoteBusinessClasses(appClassLoader, 
                                           businessInterfaceName);
    }

    public static void loadGeneratedRemoteBusinessClasses
        (ClassLoader appClassLoader, String businessInterfaceName) 
        throws Exception {

        String generatedRemoteIntfName = EJBUtils.
            getGeneratedRemoteIntfName(businessInterfaceName);
        
        String wrapperClassName = EJBUtils.
            getGeneratedRemoteWrapperName(businessInterfaceName);

        Class generatedRemoteIntf = null;
        try {
            generatedRemoteIntf = 
                appClassLoader.loadClass(generatedRemoteIntfName);
        } catch(Exception e) {
        }
        
        Class generatedRemoteWrapper = null;
        try {
            generatedRemoteWrapper = 
                appClassLoader.loadClass(wrapperClassName);
        } catch(Exception e) {
        }
        
        if( (generatedRemoteIntf != null) && 
            (generatedRemoteWrapper != null) ) {
            return;
        }

        _setClassLoader(appClassLoader);

        try {
            if( generatedRemoteIntf == null ) {
                
                RemoteGenerator gen = new RemoteGenerator(appClassLoader,
                                                          businessInterfaceName);

                Class developerClass = appClassLoader.loadClass(businessInterfaceName);
                generateAndLoad(gen, generatedRemoteIntfName, appClassLoader, developerClass);

            }

            if( generatedRemoteWrapper == null ) {
                
                Remote30WrapperGenerator gen = new Remote30WrapperGenerator
                    (appClassLoader, businessInterfaceName, 
                     generatedRemoteIntfName);
                                          
                Class developerClass = appClassLoader.loadClass(businessInterfaceName);
                generateAndLoad(gen, wrapperClassName, appClassLoader, developerClass);
            }

        } finally {
            // Fix for 7075: Make sure no classloader is bound to threadlocal:
            // avoid possible classloader leak.
            _setClassLoader(null) ;
        }
    }

    public static Class loadGeneratedGenericEJBHomeClass
        (ClassLoader appClassLoader) throws Exception {

        String className = getGenericEJBHomeClassName();

        Class generatedGenericEJBHomeClass = null;

        try {
            generatedGenericEJBHomeClass = appClassLoader.loadClass(className);
        } catch(Exception e) {
        }
        
        if( generatedGenericEJBHomeClass == null ) {
            
            GenericHomeGenerator gen =new GenericHomeGenerator(appClassLoader);
                

            generatedGenericEJBHomeClass =generateAndLoad(gen, className,
                    appClassLoader, EJBUtils.class);
        }

        return generatedGenericEJBHomeClass;
    }


    public static Class generateSEI(ClassGeneratorFactory cgf,
                                    final String seiClassName,
                                    final ClassLoader loader,
                                    final Class beanClass) {
        return generateAndLoad(cgf, seiClassName, loader, beanClass);

    }

    
    private static Class generateAndLoad(ClassGeneratorFactory cgf,
                                         final String actualClassName,
                                         final ClassLoader loader,
                                         final Class protectionDomainBase) {

        cgf.evaluate();

        final Properties props = new Properties();
        if( _logger.isLoggable(Level.FINE) ) {

            props.put(DUMP_AFTER_SETUP_VISITOR, "true");
            props.put(TRACE_BYTE_CODE_GENERATION, "true");
            props.put(USE_ASM_VERIFIER, "true");

            try {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);

                _sourceCode(ps, props);
                _logger.fine(baos.toString());

            } catch(Exception e) {
                _logger.log(Level.FINE, "exception generating src", e);
            }

        }
        
        Class result = null;
        try {
            if(System.getSecurityManager() == null) {
                result = _generate(loader, protectionDomainBase.getProtectionDomain(),
                                   props);
            } else {
                result = (Class)  java.security.AccessController.doPrivileged
                        (new java.security.PrivilegedAction() {
                    public java.lang.Object run() {
                    	return  _generate(loader, protectionDomainBase.getProtectionDomain(),
                                          props);
                    }});
            }
        } catch (RuntimeException runEx) {
            //We would have got this exception if there were two (or more)
            //  concurrent threads that attempted to define the same class
            //  Lets try to load the class and if we are able to load it
            //  then we can ignore the exception. Else throw the original exception
            try {
                result = loader.loadClass(actualClassName);
                _logger.log(Level.FINE, "[EJBUtils] Got exception ex: " + runEx
                        + " but loaded class: " + result.getName());
            } catch (ClassNotFoundException cnfEx) {
                throw runEx;
            }
        }
        
        return result;
    }


    public static RemoteBusinessWrapperBase createRemoteBusinessObject
        (String businessInterface, java.rmi.Remote delegate) 
        throws Exception {

        ClassLoader appClassLoader = 
            getBusinessIntfClassLoader(businessInterface);
        
        return createRemoteBusinessObject(appClassLoader,
                                          businessInterface, delegate);
    }


    public static RemoteBusinessWrapperBase createRemoteBusinessObject
        (ClassLoader loader, String businessInterface, 
         java.rmi.Remote delegate) 
         
        throws Exception {

        String wrapperClassName = EJBUtils.getGeneratedRemoteWrapperName
            (businessInterface);

        Class clientWrapperClass = loader.loadClass(wrapperClassName);

        Constructor ctors[] = clientWrapperClass.getConstructors();
        
        Constructor ctor = null;
        for(Constructor next : ctors) {
            if (next.getParameterTypes().length > 0 ) {
                ctor = next;
                break;
            }
        }
        
        Object obj = ctor.newInstance(new Object[] 
            { delegate, businessInterface } );

        return (RemoteBusinessWrapperBase) obj;
    }


    private static ClassLoader getBusinessIntfClassLoader
        (String businessInterface) throws Exception {
        
        ClassLoader contextLoader = null;
        if(System.getSecurityManager() == null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            contextLoader = (cl != null) ? cl :
                ClassLoader.getSystemClassLoader();
        } else {
            contextLoader = (ClassLoader)
            java.security.AccessController.doPrivileged
                    (new java.security.PrivilegedAction() {
                public java.lang.Object run() {
                    // Return context class loader.  If there is none,
                    // which could happen within Appclient container,
                    // return system class loader.
                    ClassLoader cl =
                            Thread.currentThread().getContextClassLoader();
                    return (cl != null) ? cl :
                        ClassLoader.getSystemClassLoader();
                    
                }});
        }

        final Class businessInterfaceClass = 
            contextLoader.loadClass(businessInterface);
        
        ClassLoader appClassLoader = null;
        if(System.getSecurityManager() == null) {
            appClassLoader = businessInterfaceClass.getClassLoader();
        } else {
            appClassLoader = (ClassLoader)
            java.security.AccessController.doPrivileged
                    (new java.security.PrivilegedAction() {
                public java.lang.Object run() {
                    return businessInterfaceClass.getClassLoader();
                    
                }});
        }

        return appClassLoader;
    }

    public static void serializeObjectFields(
                                             Object instance,
                                             ObjectOutputStream oos) 
        throws IOException {

        serializeObjectFields(instance, oos, true);
    }

    public static void serializeObjectFields(
                                             Object instance,
                                             ObjectOutputStream oos,
                                             boolean usesSuperClass) 
        throws IOException {

        Class clazz = (usesSuperClass)? instance.getClass().getSuperclass() : instance.getClass();
        final ObjectOutputStream objOutStream = oos;

        // Write out list of fields eligible for serialization in sorted order.
        for(Field next : getSerializationFields(clazz)) {

            final Field nextField = next;
            final Object theInstance = instance;
                Object value = null;
            try {
                if(System.getSecurityManager() == null) {
                    if( !nextField.isAccessible() ) {
                        nextField.setAccessible(true);
                    }
                    value = nextField.get(theInstance);
                } else {
                    value = java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if( !nextField.isAccessible() ) {
                                nextField.setAccessible(true);
                            }
                            return nextField.get(theInstance);
                        }
                    });
                }
                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, "=====> Serializing field: " + nextField);
                }

                objOutStream.writeObject(value);
            } catch(Throwable t) {
                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, "=====> failed serializing field: " + nextField + 
                             " =====> of class: " + clazz + " =====> using: " + oos.getClass() + 
                             " =====> serializing value of type: " + ((value == null)? null : value.getClass().getName()) + 
                             " ===> Error: " + t);
                    _logger.log(Level.FINE, "", t);
                }
                IOException ioe = new IOException();
                Throwable cause = (t instanceof InvocationTargetException) ?
                    ((InvocationTargetException)t).getCause() : t;
                ioe.initCause( cause );
                throw ioe;
            }
        }
    }

    public static void deserializeObjectFields(
                                               Object instance,
                                               ObjectInputStream ois) 
        throws IOException {

        deserializeObjectFields(instance, ois, null, true);

    }

    public static void deserializeObjectFields(
                                               Object instance,
                                               ObjectInputStream ois,
                                               Object replaceValue,
                                               boolean usesSuperClass) 
        throws IOException {

        Class clazz = (usesSuperClass)? instance.getClass().getSuperclass() : instance.getClass();
        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "=====> Deserializing class: " + clazz);
            if (replaceValue != null)
                _logger.log(Level.FINE, "=====> Replace requested for value: " + replaceValue.getClass());
        }

        // Use helper method to get sorted list of fields eligible
        // for deserialization.  This ensures that we correctly match
        // serialized state with its corresponding field.
        for(Field next : getSerializationFields(clazz)) {

            try {

                final Field nextField = next;
                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, "=====> Deserializing field: " + nextField);
                }

                // Read value from the stream even if it is to be replaced to adjust the pointers
                Object value = ois.readObject();
                if (replaceValue != null && nextField.getType().isAssignableFrom(replaceValue.getClass())) {
                    if( _logger.isLoggable(Level.FINE) ) {
                        _logger.log(Level.FINE, "=====> Replacing field: " + nextField);
                    }

                    value = replaceValue;
                }
                final Object newValue = value;
                final Object theInstance = instance;
                
                if(System.getSecurityManager() == null) {
                    if( !nextField.isAccessible() ) {
                        nextField.setAccessible(true);
                    }
                    nextField.set(theInstance, newValue);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedExceptionAction() {
                        public java.lang.Object run() throws Exception {
                            if( !nextField.isAccessible() ) {
                                nextField.setAccessible(true);
                            }
                            nextField.set(theInstance, newValue);
                            return null;
                        }
                    });
                }
            } catch(Throwable t) {
                IOException ioe = new IOException();
                Throwable cause = (t instanceof InvocationTargetException) ?
                    ((InvocationTargetException)t).getCause() : t;
                ioe.initCause( cause );
                throw ioe;
            }
        }
    }

    private static Collection<Field> getSerializationFields(Class clazz) {

        Field[] fields = clazz.getDeclaredFields();

        SortedMap<String, Field> sortedMap = new TreeMap<String, Field>();

        for(Field next : fields) {

            int modifiers = next.getModifiers();
            if( Modifier.isStatic(modifiers) || 
                Modifier.isTransient(modifiers) ) {
                continue;
            }

            // All fields come from a single class(not from any superclasses),
            // so sorting on field name is sufficient.  We use natural ordering
            // of field name java.lang.String object.
            sortedMap.put(next.getName(), next);

        }

        return (Collection<Field>) sortedMap.values();
    }

}
