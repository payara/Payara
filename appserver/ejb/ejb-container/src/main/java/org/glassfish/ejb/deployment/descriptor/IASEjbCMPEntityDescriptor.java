/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.descriptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbCMPFinder;
import org.glassfish.ejb.deployment.descriptor.runtime.PrefetchDisabledDescriptor;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

/** 
 * This class contains information about EJB1.1 and EJB2.0 CMP EntityBeans.
 */

public  class IASEjbCMPEntityDescriptor extends EjbCMPEntityDescriptor {

    private transient Class ejbClass = null;
    private String pcImplClassName = null;
    private String concreteImplClassName = null;
    private String ejbImplClassName = null;
    private String mappingProperties;
    private transient ClassLoader jcl = null;
    private String uniqueName = null;

    private String moduleDir = null;

    // for i18N
    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(IASEjbCMPEntityDescriptor.class);
    private static final Logger _logger = DOLUtils.getDefaultLogger();

    // Standard String and Character variables.
    private static final char DOT                  = '.';   // NOI18N
    private static final char LIST_START           = '(';// NOI18N   
    private static final char LIST_END             = ')';   // NOI18N
    private static final char LIST_SEPARATOR       = ',';   // NOI18N
    private static final char NAME_PART_SEPARATOR  = '_';   // NOI18N
    private static final char NAME_CONCATENATOR    = ' ';   // NOI18N
    private static final String FIND               = "find"; // NOI18N
    private static final String EJB_SELECT         = "ejbSelect"; // NOI18N
    private static final String JDOSTATE           = "_JDOState"; // NOI18N
    private static final String CONCRETE_IMPL      = "_ConcreteImpl"; // NOI18N
    private static final String MAPPINGEXT         = DOT + "mapping"; // NOI18N

    private transient Collection finders = null;
    private transient Collection selectors = null;
    private transient QueryParser queryParser = null;
    private PrefetchDisabledDescriptor prefetchDisabledDescriptor = null;
    private static final Map conversionTable = createConversionTable();
    private Map oneOneFinders = new HashMap();
    private List arrOneOneFinders = new ArrayList();
    
    private void addAllInterfaceMethodsIn(Collection methodDescriptors, Class c) {
        Method[] methods = c.getMethods();
        for (int i=0; i<methods.length; i++) {
            methodDescriptors.add(methods[i]);
        }
    }

    private void addAllUniqueInterfaceMethodsIn(Collection methodDescriptors, Class c) {
        Method[] methods = c.getMethods();
        for (int i=0; i<methods.length; i++) {
        if(findEquivalentMethod(methodDescriptors, methods[i]) == null)
            methodDescriptors.add(methods[i]);
        }
    }

    public Collection getAllUniqueMethods() {
        HashSet methods = new HashSet();

        try {
            if (isRemoteInterfacesSupported()) {
                addAllUniqueInterfaceMethodsIn(methods, jcl.loadClass(getHomeClassName()));
                addAllUniqueInterfaceMethodsIn(methods, jcl.loadClass(getRemoteClassName()));
            }
            if (isLocalInterfacesSupported()) {
                addAllUniqueInterfaceMethodsIn(methods, jcl.loadClass(getLocalHomeClassName()));
                addAllUniqueInterfaceMethodsIn(methods, jcl.loadClass(getLocalClassName()));
            }
        } catch (Throwable t) {
            _logger.log( Level.WARNING,
                "enterprise.deployment_error_loading_class_excp", t ); // NOI18N
            throw new RuntimeException(t.getMessage());
        }
        return methods;

    }

    public Collection getAllMethods() {

        HashSet methods = new HashSet();

        try {
            if (isRemoteInterfacesSupported()) {
                addAllInterfaceMethodsIn(methods, jcl.loadClass(getHomeClassName()));
                addAllInterfaceMethodsIn(methods, jcl.loadClass(getRemoteClassName()));
            }

            if (isLocalInterfacesSupported()) {
                addAllInterfaceMethodsIn(methods, jcl.loadClass(getLocalHomeClassName()));
                addAllInterfaceMethodsIn(methods, jcl.loadClass(getLocalClassName()));
            }
        } catch (Throwable t) {
            _logger.log( Level.WARNING,
                    "enterprise.deployment_error_loading_class_excp", t ); // NOI18N
            throw new RuntimeException(t.getMessage());
        }
        return methods;
    }


    private Method findEquivalentMethod(Collection methods,
                                        Method methodToMatch) {
        if(methods == null)
            return null;

        Method matchedMethod = null;
        for(Iterator iter = methods.iterator(); iter.hasNext();) {
            Method next = (Method) iter.next();
            // Compare methods, ignoring declaring class.
            if( methodsEqual(next, methodToMatch, false) ) {
                matchedMethod = next;
                break;
            }
        }
        return matchedMethod;
    }

     /**
     * Checks whether two methods that might have been loaded by
     * different class loaders are equal.
     * @param compareDeclaringClass if true, declaring class will
     * be considered as part of equality test.
     */
    private boolean methodsEqual(Method m1, Method m2,
                                 boolean compareDeclaringClass) {
        boolean equal = false;

        do {
            String m1Name = m1.getName();
            String m2Name = m2.getName();

            if( !m1Name.equals(m2Name) ) { break; }
        
            String m1DeclaringClass = m1.getDeclaringClass().getName();
            String m2DeclaringClass = m2.getDeclaringClass().getName();

            if( compareDeclaringClass ) {
                if( !m1DeclaringClass.equals(m2DeclaringClass) ) { break; }
            }

            Class[] m1ParamTypes = m1.getParameterTypes();
            Class[] m2ParamTypes = m2.getParameterTypes();

            if( m1ParamTypes.length != m2ParamTypes.length ) { break; }

            equal = true;
            for(int pIndex = 0; pIndex < m1ParamTypes.length; pIndex++) {
                String m1ParamClass = m1ParamTypes[pIndex].getName();
                String m2ParamClass = m2ParamTypes[pIndex].getName();
                if( !m1ParamClass.equals(m2ParamClass) ) {
                    equal = false;
                    break;
                }
            }

        } while(false);

        return equal;
    }

    /**
     * The method returns the class instance for the ejb class.
     * @return ejb class
     */
    private Class getEjbClass() {
        if (ejbClass == null) {
            String ejbClassName = getEjbClassName();
            if(_logger.isLoggable(Level.FINE)) 
                _logger.fine("@@@@@@ Ejb name is  "+ ejbClassName); //NOI18N
            if (jcl == null) {
                String msg = localStrings.getLocalString(
                    "enterprise.deployment.error_missing_classloader", //NOI18N
                    "IASEjbCMPEntityDescriptor.getEjbClass"); //NOI18N
                _logger.log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            }

            try {
                ejbClass=Class.forName(ejbClassName, true, jcl);

            } catch(ClassNotFoundException e) {
                String msg = localStrings.getLocalString(
                    "enterprise.deployment.error_cannot_find_ejbclass", //NOI18N
                        ejbClassName);
                _logger.log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            }
        }
        return ejbClass;
    }

    /** 
     * Returns a collection of finder method instances.
     */
    public Collection getFinders() {
        if (finders == null) {
            String ejbClassName = getEjbClassName();
            Class ejbClass = getEjbClass();
                
            if ( super.isRemoteInterfacesSupported() ) {
                Class remoteHomeIntf = null;
                if(_logger.isLoggable(Level.FINE)) 
                    _logger.fine("@@@@@@ " + ejbClassName + //NOI18N 
                         " : Remote Interface is supported "); //NOI18N 

                try {
                    remoteHomeIntf = ejbClass.getClassLoader().loadClass(
                        super.getHomeClassName());
                } catch (ClassNotFoundException ex) {
                    _logger.log( Level.WARNING,
                         "enterprise.deployment_class_not_found", ex ); //NOI18N

                    return null;
                }

                finders = getFinders(remoteHomeIntf);
                if(_logger.isLoggable(Level.FINE)) {
                    for(Iterator iter = finders.iterator(); iter.hasNext();) {
                        Method remoteHomeMethod=(Method)iter.next();
                        _logger.fine("@@@@ adding Remote interface method " + //NOI18N
                                     remoteHomeMethod.getName() );
                    }
                }
            } //end of isRemoteInterfaceSupported

            if ( super.isLocalInterfacesSupported() ) {
                Class localHomeIntf = null;

                if(_logger.isLoggable(Level.FINE)) 
                    _logger.fine("@@@@@@ " + ejbClassName + ":  Local Interface is supported "); //NOI18N

                try {
                    localHomeIntf = ejbClass.getClassLoader().loadClass(
                        super.getLocalHomeClassName());
                } catch (ClassNotFoundException ex) {
                    _logger.log( Level.WARNING,
                         "enterprise.deployment_class_not_found", ex ); //NOI18N
                    return null;
                }
        
                Collection localFinders = getFinders(localHomeIntf);
                if(finders == null) {
                    // if there were no finders specified in the remote
                    // home, the local finders are the finders  
                    finders = localFinders;

                } else if(localFinders != null) {
                    // Remove the Common Elements from the collections
                    // and keep only unique methods
                    if(_logger.isLoggable(Level.FINE)) 
                        _logger.fine("@@@@@@ Trying to remove the Common Elements from HashSet....... "); //NOI18N

                    for(Iterator iter = localFinders.iterator(); iter.hasNext();) {
                        Method localHomeMethod=(Method)iter.next();
                        if(findEquivalentMethod(finders, localHomeMethod) == null) {
                            if(_logger.isLoggable(Level.FINE)) 
                                _logger.fine("@@@@ adding local interface method " + //NOI18N
                                     localHomeMethod.getName() ); 

                            finders.add(localHomeMethod);
                        }
                    }
                }
            } //end of isLocalInterfaceSupported

            if (finders == null)
                // still not initialized => empty set
                finders = new HashSet();
        }

        return finders;
    }

    /** 
     * Returns a collection of finder methods declared by the home 
     * interface given by a class object.
     */
    public Collection getFinders(Class homeIntf) {
        Method[] methods = homeIntf.getMethods();
        Collection finders = new HashSet();
        for(int i=0; i<methods.length; i++) {
           if(methods[i].getName().startsWith(FIND)) {
               finders.add(methods[i]);
           }
        }
        
        return finders;
    }

    public void setClassLoader(ClassLoader jcl) {
        this.jcl = jcl;
    } 

    public ClassLoader getClassLoader() {
        return jcl;
    } 

    public Collection getAllPersistentFields() {
        PersistenceDescriptor pers = getPersistenceDescriptor();
        PersistentFieldInfo[] persFields = pers.getPersistentFieldInfo();
        PersistentFieldInfo[] pkeyFields = pers.getPkeyFieldInfo();
        HashMap fields = new HashMap();

        for(int i=0; i<persFields.length; i++) {
            fields.put(persFields[i].name, persFields[i]);
        }
        
        for(int i=0; i<pkeyFields.length; i++) {
            fields.put(pkeyFields[i].name, pkeyFields[i]);
        }
        
        return fields.values();        
    }
    
    public Collection getPersistentFields() {
        
        PersistenceDescriptor pers = getPersistenceDescriptor();
        PersistentFieldInfo[] persFields = pers.getPersistentFieldInfo();
        
        HashMap fields = new HashMap();

        for(int i=0; i<persFields.length; i++) {
            fields.put(persFields[i].name, persFields[i]);
        }
        
        return fields.values();        
    }

    
    public Collection getPrimaryKeyFields() {
        
        PersistenceDescriptor pers = getPersistenceDescriptor();
        PersistentFieldInfo[] pkeyFields = pers.getPkeyFieldInfo();
        
        HashMap pkey = new HashMap();
        for(int i=0; i<pkeyFields.length; i++) {
            pkey.put(pkeyFields[i].name, pkeyFields[i]);
        }
        
        return pkey.values();        
        
    }

    /**
     * Returns a collection of selector methods.
     */
    public Collection getSelectors() {
        if (selectors == null) {
            selectors = new HashSet();
            Class ejbClass = getEjbClass();
            Method[] methods = ejbClass.getMethods();
            for(int i=0; i<methods.length; i++) {
                if(methods[i].getName().startsWith(EJB_SELECT)) { //NOI18N
                    selectors.add(methods[i]);
                }
            }
        }
        
        return selectors;
    }
    
    

    public String getBaseName(String className) {
        if (className == null)
            return null;

        int dot = className.lastIndexOf(DOT);
        if (dot == -1)
            return className;
        return className.substring(dot+1);
    }
    
    public IASEjbCMPEntityDescriptor() {
    }
    
    /** 
     * The copy constructor.Hopefully we wont need it;)
     */
    public IASEjbCMPEntityDescriptor(EjbDescriptor other) {
           super(other);

           setPersistenceType(CONTAINER_PERSISTENCE);
    }
 
  
    /**
     * Sets the State class implementation classname. 
     */
     public void setPcImplClassName(String name) {
         pcImplClassName = name;
    }
  
    public String getUniqueName() {
        if(uniqueName == null) {
            BundleDescriptor bundle = getEjbBundleDescriptor();
            Application application = bundle.getApplication();

            // Add ejb name and application name.
            StringBuffer rc = new StringBuffer().
                    append(getName()).
                    append(NAME_CONCATENATOR).
                    append(application.getRegistrationName());

            // If it's not just a module, add a module name.
            if (!application.isVirtual()) {
                rc.append(NAME_CONCATENATOR).
                   append(bundle.getModuleDescriptor().getArchiveUri());
            }

            uniqueName = getBaseName(getEjbClassName()) 
                    + getUniqueNumber(rc.toString());
        }

        return uniqueName;
    }

    public String getUniqueNumber(String num) {
        //Modified to decrease the possibility of collision
        String newNum= "" + num.hashCode(); // NOI18N
        newNum = newNum.replace('-', NAME_PART_SEPARATOR); // NOI18N
        return newNum;
     }


    public String getPcImplClassName() {
       if (pcImplClassName == null) { 
           // Check for Null added 
           pcImplClassName = getUniqueName() + JDOSTATE;
           String packageName = getPackageName(getEjbClassName());
           if(packageName != null)
               pcImplClassName = packageName + DOT + pcImplClassName;

            if(_logger.isLoggable(Level.FINE)) 
                _logger.fine("##### PCImplClass Name is " + pcImplClassName); // NOI18N
        }
        return pcImplClassName;
    }


      /**
     * Sets the State class implementation classname. 
     */
    public void setConcreteImplClassName(String name) {
         concreteImplClassName = name;
    }
    
    public String getPackageName(String className) {
        int dot = className.lastIndexOf(DOT);
        if (dot == -1)
            return null;
        return className.substring(0, dot);
     }


    /** IASRI 4725194
     * Returns the Execution class, which is sam as the user-specified class
     * in case of Message, Session and Bean Managed Persistence Entity Beans
     * but is different for Container Mananged Persistence Entity Bean
     * Therefore, the implementation in the base class is to return
     * getEjbClassName() and the method is redefined in IASEjbCMPDescriptor.
     *
     */
    public String getEjbImplClassName() {
        if (ejbImplClassName == null) {
            String packageName = getPackageName(getEjbClassName());
            ejbImplClassName = getConcreteImplClassName();
            if(packageName != null)
                ejbImplClassName = packageName + DOT + ejbImplClassName;
        }
        return ejbImplClassName;
    }

    /**
     * Returns the classname of the State class impl. 
     */
 
    public String getConcreteImplClassName() {
        if (concreteImplClassName == null) {
        /**    The Ear may contain two jar files with beans with same ejb names
        */
             concreteImplClassName = getUniqueName() + CONCRETE_IMPL;
        }

        return concreteImplClassName;
    }

    public void setModuleDir(String moduleRootDir) {
        moduleDir = moduleRootDir;
    }

    /**
    * Returns the Module root of this module.
    */
    public String getModuleDir() {
        //FIXME:this needs to be changed when the API is available.
        if(moduleDir != null)
            return moduleDir;
        else
            return null;
    }

    public void setMappingProperties(String mappingProperties) {
         this.mappingProperties = mappingProperties;
    }
    
    
    /**
     * Returns the classname of the State class impl. 
     */
    public String  getMappingProperties() {
         return mappingProperties;
    }
    
    
    /**     
     * Called from EjbBundleDescriptor/EjbBundleArchivist 
     * when some classes in this bean are updated.
     */         
    public boolean classesChanged() {

        /**        No Implementation Yet
        boolean superChanged = super.classesChanged();

        boolean persChanged = pers.classesChanged();

        // Send changed event only if parent didn't already do it.
        if( !superChanged && persChanged ) {
            changed();
        }

        return (superChanged || persChanged);
        */
        
        return false;
    }

    /**
     * This method sets the parser which would be used to parse the query
     * parameter declaration given in sun-ejb-jar.xml.
     * This method is called from JDOCodenerator class 's generate() method.
     */
    public void setQueryParser(QueryParser inParser) {
        queryParser = inParser;
    }
    
    /** 
     * Returns the query parser object 
     */
    public QueryParser getQueryParser() {
        return queryParser;
    }

    /**
     * This method returns the conversion table which maps the unqualified
     * name (e.g., String) of the java.lang classes to their fully qualified
     * name (e.g., java.lang.String)
     */    
    private static Map createConversionTable () {

        HashMap conversionTable = new HashMap();
        conversionTable.put("Boolean", "java.lang.Boolean"); //NOI18N
        conversionTable.put("Byte", "java.lang.Byte"); //NOI18N
        conversionTable.put("Character", "java.lang.Character"); //NOI18N
        conversionTable.put("Double", "java.lang.Double"); //NOI18N
        conversionTable.put("Float", "java.lang.Float"); //NOI18N
        conversionTable.put("Integer", "java.lang.Integer"); //NOI18N
        conversionTable.put("Long", "java.lang.Long"); //NOI18N
        conversionTable.put("Number", "java.lang.Number"); //NOI18N
        conversionTable.put("Short", "java.lang.Short"); //NOI18N
        conversionTable.put("String", "java.lang.String"); //NOI18N
        conversionTable.put("Object", "java.lang.Object"); //NOI18N
        return conversionTable;
    }
    
    private String getFullyQualifiedType(String type) {
        String knownType=(String)conversionTable.get(type);
        return knownType == null ? type : knownType;
    }

     /**
      * Getter for prefetch-disabled
      * @return Value of prefetchDisabledDescriptor
      */
    public PrefetchDisabledDescriptor getPrefetchDisabledDescriptor() {
        return prefetchDisabledDescriptor;
    }

    /**
     * Setter for prefetch-disabled
     * @param prefetchDisabledDescriptor 
     * New value of prefetchDisabledDescriptor.
     */
    public void setPrefetchDisabledDescriptor(
        PrefetchDisabledDescriptor prefetchDisabledDescriptor) {
        this.prefetchDisabledDescriptor = prefetchDisabledDescriptor;
    }

    
    /*
     * Adds the given OneOneFinder to the HashMap
     * @Param finder represents the EJB 1.1 Finder 
     */
    public  void addOneOneFinder (IASEjbCMPFinder finder) {
        arrOneOneFinders.add(finder);
    }
    
    /**
     * Returns a Map which maps between a method signature and the 
     * corresponding IASEjbCMPFinder instance. The key is the method
     * signature as a string and consists of methodName(type1, type2.....).
     */
    public Map getOneOneFinders() {
        // update the oneOneFinders map if there are any entries pending in
        // the array arrOneOneFinders.
        if (!arrOneOneFinders.isEmpty()) {
            if (queryParser == null) {
                String msg = localStrings.getLocalString(
                    "enterprise.deployment.error_missing_queryparser", //NOI18N
                    "IASEjbCMPEntityDescriptor.getOneOneFinders"); //NOI18N
                _logger.log(Level.WARNING, msg);
                throw new RuntimeException(msg);
            }
            
            //parse the query declaration parameter and store the query object
            for ( Iterator i = arrOneOneFinders.iterator(); i.hasNext(); ) {
                IASEjbCMPFinder finder = ( IASEjbCMPFinder )i.next();
                String key = generateKey(finder, queryParser);
                oneOneFinders.put(key, finder);
            }
            arrOneOneFinders.clear();
        }
        return oneOneFinders;
    }
    
    /*
     * @returns the key used to store 1.1 Finder Object.
     * the key is methodName(param0, param1.....)
     * @param finder is the object which represents the EJB 1.1 Finder 
     */
    private String generateKey(IASEjbCMPFinder finder, QueryParser parser)     {
        
        StringBuffer key = new StringBuffer();
        key.append(finder.getMethodName()).append(LIST_START);

        String queryParams = finder.getQueryParameterDeclaration();
        Iterator iter = parser.parameterTypeIterator(queryParams);
        while ( iter.hasNext() )  {
            String type = ( String ) iter.next() ;
            key.append(getFullyQualifiedType(type)) ;
            if( iter.hasNext() ) {
                key.append(LIST_SEPARATOR); 
            }
        }
        key.append(LIST_END);

        return key.toString().intern();
    }
    
    /*
     * @returns The finder object for the particular Method object.
     * @param method object for which the Finder Object needs to be found
     */
    public IASEjbCMPFinder getIASEjbCMPFinder(Method method) {
        //Checks if the given method is present in the interfaces.
        if(findEquivalentMethod(getFinders(), method) == null ) {
            return null;
        }
        String methodName = method.getName();
        
        //key is of the form methodName(param0, param1, ....)
        StringBuffer key = new StringBuffer();
        key.append(methodName);
        key.append(LIST_START);
        Class paramList[] = method.getParameterTypes();
        for (int index = 0 ; index < paramList.length ; index++ ) {
            if(index>0) {
                key.append(LIST_SEPARATOR);
            }
            key.append(paramList[index].getName());
        }
        key.append(LIST_END);
        return (IASEjbCMPFinder)getOneOneFinders().get(key.toString());
    } 
}
