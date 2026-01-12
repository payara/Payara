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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2016-2026 Payara Foundation and/or its affiliates

package com.sun.ejb;

import com.sun.ejb.codegen.AsmSerializableBeanGenerator;
import com.sun.ejb.codegen.EjbClassGeneratorFactory;
import com.sun.ejb.codegen.Generator;
import com.sun.ejb.codegen.GeneratorException;
import com.sun.ejb.codegen.Remote30WrapperGenerator;
import com.sun.ejb.codegen.RemoteGenerator;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.GenericEJBLocalHome;
import com.sun.ejb.containers.RemoteBusinessWrapperBase;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.loader.CurrentBeforeParentClassLoader;
import com.sun.logging.LogDomains;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;


/**
 * A handy class with static utility methods.
 * <p>
 * Note that much of this code has to execute in the client so
 * it needs to be careful about which server-only resources it
 * uses and in which code paths.
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
            throws IOException {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().serializeObject(obj, replaceObject);
    }

    public static final byte[] serializeObject(Object obj)
            throws IOException {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().serializeObject(obj, true);
    }

    /**
     * Utility method for deserializing EJBs, primary keys and
     * container-managed fields, all of which may include Remote
     * EJB references,
     * Local refs, JNDI Contexts etc which are not Serializable.
     *
     * @param data
     * @param loader
     * @param resolveObject
     * @param appUniqueId
     * @return object
     * @throws java.lang.Exception
     */
    public static final Object deserializeObject(byte[] data,
                                                 ClassLoader loader, boolean resolveObject, long appUniqueId)
            throws Exception {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().deserializeObject(data, resolveObject, loader, appUniqueId);
    }

    public static final Object deserializeObject(byte[] data, ClassLoader loader, long appUniqueId)
            throws Exception {
        return EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().deserializeObject(data, true, loader, appUniqueId);
    }

    public static boolean useStaticCodegen() {
        synchronized (EJBUtils.class) {
            if (ejbUseStaticCodegen_ == null) {
                String ejbStaticCodegenProp;
                ejbStaticCodegenProp = System.getProperty(EJB_USE_STATIC_CODEGEN_PROP);
                boolean useStaticCodegen =
                        ((ejbStaticCodegenProp != null) &&
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
    
    public static String getGeneratedOptionalInterfaceName(String ejbClassName) {
        String packageName = Generator.getPackageName(ejbClassName);
        String simpleName = Generator.getBaseName(ejbClassName);
        String optionalIntfName = "__EJB31_Generated__" + simpleName + "__Intf__";
        return (packageName != null) ? packageName + "." + optionalIntfName : optionalIntfName;
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
     * <p>
     * Note that it's better to delay this final jndi name translation as
     * much as possible and do it right before the NamingManager lookup,
     * as opposed to changing the jndi-name within the descriptor objects
     * themselves.  This way, the extra indirection will not be exposed
     * if the descriptors are written out and they won't complicate any
     * jndi-name equality logic.
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

        if (businessView) {
            if (jndiName.startsWith(CORBA_INS_PREFIX)) {


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
                int indexOfCorbaNameSep = jndiName.indexOf('#');
                String jndiNameMinusCorbaNamePortion = jndiName.substring(indexOfCorbaNameSep + 1);

                // Make sure any of the resulting jndi names still have corbaname: prefix intact
                String newJndiName = jndiName;

                if (jndiNameMinusCorbaNamePortion.startsWith(JAVA_GLOBAL_PREFIX)) {

                    newJndiName = stripFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);

                } else if (jndiNameMinusCorbaNamePortion.endsWith(glassfishFullyQualifiedPortion)) {

                    newJndiName = stripFullyQualifiedJndiName(jndiName, glassfishFullyQualifiedPortion);

                }

                returnValue = getRemote30HomeJndiName(newJndiName);

            } else {
                // Convert to fully-qualified names
                if (jndiName.startsWith(JAVA_GLOBAL_PREFIX)) {
                    returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
                } else {
                    returnValue = checkFullyQualifiedJndiName(jndiName, glassfishFullyQualifiedPortion);
                }
            }
        } else {

            // EJB 2.x Remote  Home

            // Only in the portable global case, convert to a fully-qualified name
            if (jndiName.startsWith(JAVA_GLOBAL_PREFIX)) {
                returnValue = checkFullyQualifiedJndiName(jndiName, portableFullyQualifiedPortion);
            }
        }

        return returnValue;
    }

    private static String checkFullyQualifiedJndiName(String origJndiName, String fullyQualifiedPortion) {
        String returnValue = origJndiName;
        if (!origJndiName.endsWith(fullyQualifiedPortion)) {
            returnValue = origJndiName + fullyQualifiedPortion;
        }
        return returnValue;
    }

    private static String stripFullyQualifiedJndiName(String origJndiName, String fullyQualifiedPortion) {
        String returnValue = origJndiName;
        if (origJndiName.endsWith(fullyQualifiedPortion)) {
            int portionLength = fullyQualifiedPortion.length();
            returnValue = origJndiName.substring(0, origJndiName.length() - portionLength);
        }
        return returnValue;
    }


    public static Object resolveEjbRefObject(EjbReferenceDescriptor refDesc, Object jndiObj) throws NamingException {
        if (refDesc.isLocal()) {
            EjbDescriptor target = refDesc.getEjbDescriptor();
            BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(target.getUniqueId());
            if (refDesc.isEJB30ClientView()) {
                GenericEJBLocalHome genericLocalHome = container.getEJBLocalBusinessHome(refDesc.getEjbInterface());
                return genericLocalHome.create(refDesc.getEjbInterface());
            }
           return container.getEJBLocalHome();
        }

        if (refDesc.isEJB30ClientView() && !(jndiObj instanceof RemoteBusinessWrapperBase)) {
            return EJBUtils.lookupRemote30BusinessObject(jndiObj, refDesc.getEjbInterface());
        }

        return jndiObj;
    }

    public static Object lookupRemote30BusinessObject(Object jndiObj, String businessInterface) throws NamingException {
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> genericEJBHome = loadGeneratedGenericEJBHomeClass(loader, jndiObj.getClass());
            final Object genericHomeObj = PortableRemoteObject.narrow(jndiObj, genericEJBHome);

            // The generated remote business interface and the
            // client wrapper for the business interface are produced
            // dynamically. The following call must be made before
            // any EJB 3.0 Remote business interface runtime behavior
            // is needed in a given JVM.
            loadGeneratedRemoteBusinessClasses(businessInterface);
            String generatedRemoteIntfName = RemoteGenerator.getGeneratedRemoteIntfName(businessInterface);
            Method createMethod = genericEJBHome.getMethod("create", String.class);
            java.rmi.Remote delegate = (java.rmi.Remote) createMethod.invoke(genericHomeObj, generatedRemoteIntfName);

            // TODO Bring over appclient security exception retry logic  CR 6620388
            return createRemoteBusinessObject(loader, businessInterface, delegate);
        } catch (Exception e) {
            NamingException ne = new NamingException(
                    "ejb ref resolution error for remote business interface" + businessInterface);
            ne.initCause(e instanceof InvocationTargetException ? e.getCause() : e);
            throw ne;
        }
    }

    public static Class<?> loadGeneratedSerializableClass(ClassLoader loader, final Class<?> originalClass) {
        final String generatedClassName = AsmSerializableBeanGenerator.getGeneratedSerializableClassName(originalClass.getName());
        try {
            return loader.loadClass(generatedClassName);
        } catch (ClassNotFoundException e) {
            _logger.warning("Class Not loaded yet " + generatedClassName);
        }
        AsmSerializableBeanGenerator gen = new AsmSerializableBeanGenerator(loader, originalClass, generatedClassName);
        return gen.generateSerializableSubclass();
    }

    public static void loadGeneratedRemoteBusinessClasses(String businessInterfaceName) throws Exception {
        ClassLoader appClassLoader = getBusinessIntfClassLoader(businessInterfaceName);
        loadGeneratedRemoteBusinessClasses(appClassLoader, businessInterfaceName);
    }

    public static Class<?> loadGeneratedRemoteBusinessClasses(ClassLoader appClassLoader, String businessInterfaceName)
            throws Exception {

        if (appClassLoader != null && appClassLoader.getClass().isAssignableFrom(CurrentBeforeParentClassLoader.class)) {
            appClassLoader = Thread.currentThread().getContextClassLoader();
        }
        
        try (EjbClassGeneratorFactory factory = new EjbClassGeneratorFactory(appClassLoader)) {
            return factory.ensureRemote(businessInterfaceName);
        }
    }

    /**
     * Loads the a class by name using the provided classloader.
     *
     * @param clsLoader Classloader to use for loading
     * @param clsName   Name of the class to load.
     * @return loaded class or null in case of an exception.
     */
    private static Class<?> loadClassIgnoringExceptions(ClassLoader clsLoader, String clsName) {
        try {
            return clsLoader.loadClass(clsName);
        } catch (ClassNotFoundException e) {
            _logger.log(Level.FINE, "Exception loading class: " + clsName + " using classloader: " + clsLoader, e);
            return null;
        }
    }

    public static Class<?> loadGeneratedGenericEJBHomeClass(final ClassLoader appClassLoader, Class<?> anchorClass) throws Exception {
        try (EjbClassGeneratorFactory factory = new EjbClassGeneratorFactory(appClassLoader)) {
            return factory.ensureGenericHome(anchorClass);
        }
    }


    public static Class<?> generateSEI(ClassLoader loader, final Class<?> ejbClass) throws GeneratorException {
        try (EjbClassGeneratorFactory factory = new EjbClassGeneratorFactory(loader)) {
            return factory.ensureServiceInterface(ejbClass);
        }
    }
    
    public static RemoteBusinessWrapperBase createRemoteBusinessObject(String businessInterface, java.rmi.Remote delegate)
            throws Exception {
        ClassLoader appClassLoader = getBusinessIntfClassLoader(businessInterface);
        return createRemoteBusinessObject(appClassLoader, businessInterface, delegate);
    }

    public static RemoteBusinessWrapperBase createRemoteBusinessObject(ClassLoader loader, String businessInterface, 
                                                                       java.rmi.Remote delegate) throws Exception {

        String wrapperClassName = Remote30WrapperGenerator.getGeneratedRemoteWrapperName(businessInterface);

        Class clientWrapperClass = loader.loadClass(wrapperClassName);

        Constructor ctor = null;
        for (Constructor next : clientWrapperClass.getConstructors()) {
            if (next.getParameterTypes().length > 0) {
                ctor = next;
                break;
            }
        }

        if (ctor == null) {
            throw new IllegalStateException("Missing ctor with parameters in " + clientWrapperClass);
        }

        return (RemoteBusinessWrapperBase) ctor.newInstance(delegate, businessInterface);
    }


    private static ClassLoader getBusinessIntfClassLoader(String businessInterface) throws Exception {
        final ClassLoader contextLoader;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        contextLoader = cl == null ? ClassLoader.getSystemClassLoader() : cl;

        final Class<?> businessInterfaceClass = contextLoader.loadClass(businessInterface);
        return businessInterfaceClass.getClassLoader();
    }

    public static void serializeObjectFields(Object instance, ObjectOutputStream oos, boolean usesSuperClass) throws IOException {
        Class clazz = (usesSuperClass) ? instance.getClass().getSuperclass() : instance.getClass();
        final ObjectOutputStream objOutStream = oos;

        // Write out list of fields eligible for serialization in sorted order.
        for (Field next : getSerializationFields(clazz)) {

            final Field nextField = next;
            final Object theInstance = instance;
            Object value = null;
            try {
                if (!nextField.isAccessible()) {
                    nextField.setAccessible(true);
                }
                value = nextField.get(theInstance);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "=====> Serializing field: " + nextField);
                }

                objOutStream.writeObject(value);
            } catch (Throwable t) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "=====> failed serializing field: " + nextField +
                            " =====> of class: " + clazz + " =====> using: " + oos.getClass() +
                            " =====> serializing value of type: " + ((value == null) ? null : value.getClass().getName()) +
                            " ===> Error: " + t);
                    _logger.log(Level.FINE, "", t);
                }
                throw new IOException(t instanceof InvocationTargetException ? t.getCause() : t);
            }
        }
    }

    public static void deserializeObjectFields(Object instance, ObjectInputStream ois) throws IOException {
        deserializeObjectFields(instance, ois, null, true);
    }

    public static void deserializeObjectFields(Object instance, ObjectInputStream ois, Object replaceValue, 
                                               boolean usesSuperClass) throws IOException {

        Class clazz = (usesSuperClass) ? instance.getClass().getSuperclass() : instance.getClass();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "=====> Deserializing class: " + clazz);
            if (replaceValue != null)
                _logger.log(Level.FINE, "=====> Replace requested for value: " + replaceValue.getClass());
        }

        // Use helper method to get sorted list of fields eligible
        // for deserialization.  This ensures that we correctly match
        // serialized state with its corresponding field.
        for (Field next : getSerializationFields(clazz)) {

            try {

                final Field nextField = next;
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "=====> Deserializing field: " + nextField);
                }

                // Read value from the stream even if it is to be replaced to adjust the pointers
                Object value = ois.readObject();
                if (replaceValue != null && nextField.getType().isAssignableFrom(replaceValue.getClass())) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "=====> Replacing field: " + nextField);
                    }

                    value = replaceValue;
                }
                final Object newValue = value;
                final Object theInstance = instance;

                if (!nextField.isAccessible()) {
                    nextField.setAccessible(true);
                }
                nextField.set(theInstance, newValue);
            } catch (Throwable t) {
                throw new IOException(t instanceof InvocationTargetException ? t.getCause() : t);
            }
        }
    }

    private static Collection<Field> getSerializationFields(Class clazz) {

        Field[] fields = clazz.getDeclaredFields();

        SortedMap<String, Field> sortedMap = new TreeMap<>();

        for (Field next : fields) {

            int modifiers = next.getModifiers();
            if (Modifier.isStatic(modifiers) ||
                    Modifier.isTransient(modifiers)) {
                continue;
            }

            // All fields come from a single class(not from any superclasses),
            // so sorting on field name is sufficient.  We use natural ordering
            // of field name java.lang.String object.
            sortedMap.put(next.getName(), next);

        }

        return sortedMap.values();
    }

}
