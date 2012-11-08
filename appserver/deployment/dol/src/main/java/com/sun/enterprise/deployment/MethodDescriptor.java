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

package com.sun.enterprise.deployment;

import com.sun.enterprise.deployment.util.TypeUtil;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

    /** I am a deployment object representing a single method or a collection
    * of methods on Enterprise Bean classes.
    * @author Danny Coward
    */

public final class MethodDescriptor extends Descriptor {
    /** Represents the bean home interface ejbClassSymbol.*/
    public static final String EJB_HOME = "Home";
    /** Represents the bean local home interface ejbClassSymbol.*/
    public static final String EJB_LOCALHOME = "LocalHome";
    /** Represents the bean remote interface ejbClassSymbol.*/
    public static final String EJB_REMOTE = "Remote";
    /** Represents the local interface and no-interface view ejbClassSymbol.*/
    public static final String EJB_LOCAL = "Local";
    /** Represents the web service interface ejbClassSymbol.*/
    public static final String EJB_WEB_SERVICE = "ServiceEndpoint";
    /** Represents the bean class ejbClassSymbol.*/
    public static final String EJB_BEAN = "Bean";
    /** Unused.*/
    public static final String ALL_OF_NAME = "AllOfName";
    /** The method descriptor name representing all methods.*/
    public static final String ALL_METHODS = "*";

    /** Represents the bean timeout methods ejbClassSymbol. */
    public static final String TIMER_METHOD = "Timer";
    /** Represents the bean lifecycle methods ejbClassSymbol. */
    public static final String LIFECYCLE_CALLBACK = "LifecycleCallback";

    /** Represents the bean MessageEndpoint methods ejbClassSymbol. */
    public static final String MESSAGE_ENDPOINT_METHOD = "MessageEndpoint";
    
    private String[] parameterClassNames = null;
    private String[] javaParameterClassNames = null;
    private String className = ""; // cache this
    private String ejbClassSymbol;
    private String ejbName;
    private static LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(MethodDescriptor.class);    
	    
     final static Logger _logger = DOLUtils.getDefaultLogger();
		
    private final int JAVA_FORMAT = 1;
    private final int XML_FORMAT = -1;
    private final int XML_JAVA_FORMAT = 0;
    private boolean isExact = false;
    
    /** 
    * Constructs a method descriptor corresponding to methods on the ejb class defined by the ejbClassSymbol (or home
    * and remote if null) with the same name (or all if ALL_METHODS) and paramater list (or just all by name of this is null).
    * (Styles 1 2 and 3 in the ejb specification)
    */
    public MethodDescriptor(String name, String description, String[] parameterClassNames, String ejbClassSymbol) {
	super(name, description);
	if (name == null) {
	    super.setName("");
	}
        if (parameterClassNames != null)
            convertToAppropriateFormat (parameterClassNames);
	this.setEjbClassSymbol(ejbClassSymbol);
    }
           
    // converts an XML style parameter class name to java style and vice versa
    private void convertToAppropriateFormat (String[] parameterClassNames){
	int format = isJavaFormat (parameterClassNames);
	// not java format so fix the java string
	if (format == JAVA_FORMAT) {
	    this.javaParameterClassNames = parameterClassNames;
	    this.parameterClassNames =
		fixParamClassNames (parameterClassNames);
	} else if (format == XML_FORMAT) { // fix the non java string
	    this.javaParameterClassNames = 
		xmlFormat2JavaClassNames (parameterClassNames);
	    this.parameterClassNames = parameterClassNames;
	} else if (format == XML_JAVA_FORMAT){
	    // let them be as it is makes no difference
	    this.javaParameterClassNames = parameterClassNames;
	    this.parameterClassNames = parameterClassNames;
	}
    }
    /** Constructor for styles 2 and 1. 
    ** Style 1 iff ALL_METHODS is used
    */
    
    public MethodDescriptor(String name, String description, String ejbClassSymbol) {
	super(name, description);
	this.parameterClassNames = null;
	this.setEjbClassSymbol(ejbClassSymbol);
    }
    
    /** Construct an exact method descriptor from the given method object, classloader and ejb descriptor.
    */
    public MethodDescriptor(Method method, String methodIntf) {
        this(method);
        isExact=true;
	this.setEjbClassSymbol(methodIntf);
    }

    /** Construct an method descriptor from the given method object.
     */
    public MethodDescriptor(Method method) {
        super(method.getName(), "");
        Class[] paramTypes = method.getParameterTypes();
        this.parameterClassNames = getParameterClassNamesFor(method, paramTypes);
        this.javaParameterClassNames = getJavaFormatClassNamesFor(paramTypes);
        this.className = method.getDeclaringClass().getName();
    }
    
    public MethodDescriptor() {
    }
    
    public void setEmptyParameterClassNames() {
        parameterClassNames = new String[0];
    }

    // XXX JD fix this
    public void addParameterClass(String parameter) {
        if (parameterClassNames==null) {
            parameterClassNames = new String[1];
        } else {
            String [] newParameterClassNames = new String[parameterClassNames.length + 1];
            for (int i=0;i<parameterClassNames.length;i++) {
                newParameterClassNames[i] = parameterClassNames[i];
            }
            parameterClassNames = newParameterClassNames;
        }
        parameterClassNames[parameterClassNames.length-1]=parameter;            
	javaParameterClassNames = xmlFormat2JavaClassNames (parameterClassNames);        
    }
    
    public void setEjbName(String ejbName) {
        this.ejbName = ejbName;
    } 
    
    public String getEjbName() {
        return ejbName;
    }
    
    
    /** Returns true if I have enough information to specifiy a unique method
    * on an ejb's home or remote interface unambiguously.
    */
    public boolean isExact() {
        if (isExact) {
            return true;
        }
	boolean isExactName = !this.getName().equals(ALL_METHODS);
	boolean hasMethodIntf = getEjbClassSymbol()!=null;
	boolean hasParamsListed = (this.getParameterClassNames() != null);
	return isExactName && hasMethodIntf && hasParamsListed;
    }	
    
    /**
     * <p>
     * @return the style level of this method descriptors. According to the J2EE spec, methods
     * can be described byt using style 1, style 2 or style 3 xml tags. 
     * </p>
     */
    public int getStyle() {
        if ( (getName().equals(ALL_METHODS)) ) {
            return 1;
        }
        if (getParameterClassNames()==null)
            return 2;
        return 3;
    }

    public Method getMethod(EjbDescriptor ejbDescriptor) {
        Method method = null;
        try {
            ClassLoader classloader = ejbDescriptor.getEjbBundleDescriptor().getClassLoader();
            String[] javaParamClassNames = getJavaParameterClassNames();

	    if ( ejbClassSymbol == null || ejbClassSymbol.equals("") 
		 || ejbClassSymbol.equals(EJB_BEAN) || ejbClassSymbol.equals(TIMER_METHOD)
                 || ejbClassSymbol.equals(LIFECYCLE_CALLBACK) || ejbClassSymbol.equals(MESSAGE_ENDPOINT_METHOD)) {
		try {
                    if ( !(className.equals("")) ) {
                        // If declaring class is known, use it. Since method
                        // can have any access type and there is no need
                        // to search super-classes, use 
                        // Class.getDeclaredMethod() lookup behavior.
                        Class declaringClass =classloader.loadClass(className);
                        return TypeUtil.getDeclaredMethod
                            (declaringClass, classloader, getName(),
                             javaParamClassNames);
                    } else {
                        // Method is public but can be anywhere in class
                        // hierarchy.
                        Class ejbClass = classloader.loadClass
                            (ejbDescriptor.getEjbClassName());
                        return TypeUtil.getMethod(ejbClass, classloader,
					      getName(), javaParamClassNames);
                    }
		} catch(NoSuchMethodException nsme) {}
		try {
                    if( ejbDescriptor.isRemoteInterfacesSupported() ) {
                        Class homeClass = classloader.loadClass
                            (ejbDescriptor.getHomeClassName());
                        return TypeUtil.getMethod(homeClass, classloader,
                                                  getName(), javaParamClassNames);
                    }
		} catch(NoSuchMethodException nsme) {}
		try {
                    if( ejbDescriptor.isLocalInterfacesSupported() ) {
                        Class cl = classloader.loadClass
                            (ejbDescriptor.getLocalHomeClassName());
                        return TypeUtil.getMethod(cl, classloader,
						getName(), javaParamClassNames);
                    }
		} catch(NoSuchMethodException nsme) {}
                try {
                    if( ejbDescriptor.hasWebServiceEndpointInterface() ) {
                        Class cl = classloader.loadClass
                           (ejbDescriptor.getWebServiceEndpointInterfaceName());
                        return TypeUtil.getMethod(cl, classloader,
						getName(), javaParamClassNames);
                    }
		} catch(NoSuchMethodException nsme) {}
	    }
	    else if ( ejbClassSymbol.equals(EJB_HOME) ) {
		try {
		    Class homeClass =
			classloader.loadClass(ejbDescriptor.getHomeClassName());
		    method = TypeUtil.getMethod(homeClass, classloader,
						getName(), javaParamClassNames);
		} catch(NoSuchMethodException nsme) {}
            }
	    else if ( ejbClassSymbol.equals(EJB_LOCALHOME) ) {
		try {
		    Class cl = classloader.loadClass(
					ejbDescriptor.getLocalHomeClassName());
		    method = TypeUtil.getMethod(cl, classloader,
						getName(), javaParamClassNames);
		} catch(NoSuchMethodException nsme) {}
            }
	    else if ( ejbClassSymbol.equals(EJB_REMOTE) ) {
                if( ejbDescriptor.isRemoteInterfacesSupported() ) {
                    try {
                        Class cl = classloader.loadClass(
                                       ejbDescriptor.getRemoteClassName());
                        method = TypeUtil.getMethod(cl, classloader,
                                       getName(), javaParamClassNames);
                    } catch(NoSuchMethodException nsme) {}
                }
                if( (method == null) && 
                    ejbDescriptor.isRemoteBusinessInterfacesSupported() ) {

                    for(String intf : 
                            ejbDescriptor.getRemoteBusinessClassNames() ) {
                        try {
                            Class cl = classloader.loadClass(intf);
                            method = TypeUtil.getMethod(cl, classloader,
                                         getName(), javaParamClassNames);
                        } catch(NoSuchMethodException nsme) {}
                        
                        if( method != null ) {
                            break;
                        }
                    }
                }
            }
        else if ( ejbClassSymbol.equals(EJB_LOCAL) ) {
                if (ejbDescriptor.isLocalBean()) {
                    try {
                        Class cl = classloader.loadClass(
                            ejbDescriptor.getEjbClassName());
                        method = TypeUtil.getMethod(cl, classloader,
                            getName(), javaParamClassNames);
                    } catch (NoSuchMethodException nsme) {}
                }

                if( (method == null) && ejbDescriptor.isLocalInterfacesSupported() ) {
                    try {
                        Class cl = classloader.loadClass(
				      ejbDescriptor.getLocalClassName());
                        method = TypeUtil.getMethod(cl, classloader,
				      getName(), javaParamClassNames);
                    } catch(NoSuchMethodException nsme) {}
                }
                if( (method == null) &&
                    ejbDescriptor.isLocalBusinessInterfacesSupported() ) {

                    for(String intf : 
                            ejbDescriptor.getLocalBusinessClassNames() ) {
                        try {
                            Class cl = classloader.loadClass(intf);
                            method = TypeUtil.getMethod(cl, classloader,
                                         getName(), javaParamClassNames);
                        } catch(NoSuchMethodException nsme) {}
                        
                        if( method != null ) {
                            break;
                        }
                    }  
                }
            }
            else if ( ejbClassSymbol.equals(EJB_WEB_SERVICE) ) {
		try {
		    Class cl = classloader.loadClass
                        (ejbDescriptor.getWebServiceEndpointInterfaceName());
		    method = TypeUtil.getMethod(cl, classloader,
						getName(), javaParamClassNames);
		} catch(NoSuchMethodException nsme) {}
            }
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object[]{e.getMessage(), ejbDescriptor});
        }
        return method;
    }


    public Method getMethod(Class declaringClass)
    {
    try {
        return TypeUtil.getMethod(declaringClass, 
                      declaringClass.getClassLoader(),
                                      getName(), getJavaParameterClassNames());
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object[]{declaringClass});
        return null;
        }
    }

    public Method getDeclaredMethod(Class declaringClass)
    {
    try {
        return TypeUtil.getDeclaredMethod(declaringClass, 
                      declaringClass.getClassLoader(),
                                      getName(), getJavaParameterClassNames());
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object[]{declaringClass});
        return null;
        }
    }
    
    public Method getDeclaredMethod(EjbDescriptor ejbDescriptor)
    {
        ClassLoader classloader = ejbDescriptor.getEjbBundleDescriptor().getClassLoader();
        try {
            Class[] parameterTypes = TypeUtil.paramClassNamesToTypes(
                    getJavaParameterClassNames(), classloader);

            return getDeclaredMethod(ejbDescriptor, parameterTypes);
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object[]{e, ejbDescriptor});
        }

        return null;
    }
    
    public Method getDeclaredMethod(EjbDescriptor ejbDescriptor, Class[] javaParamClassNames)
    {
        try {
            ClassLoader classloader = ejbDescriptor.getEjbBundleDescriptor().getClassLoader();
            Class nextClass = classloader.loadClass(ejbDescriptor.getEjbClassName());
            String mname = getName();

            while((nextClass != Object.class) && (nextClass != null)) {
                // Do not use TypeUtil not to spend time converting parameter 
                // types for each call.
                try {
                    return nextClass.getDeclaredMethod(mname, javaParamClassNames);

                } catch (NoSuchMethodException nsme) {
                    nextClass = nextClass.getSuperclass();
                }
            }

        } catch(Exception e) {
            _logger.log(Level.SEVERE,"enterprise.deployment.backend.methodClassLoadFailure",new Object[]{ejbDescriptor});
        }

        return null;
    }
    
	/**
	* Performs a conversion from the style1 style2 and style3 (no interface symbol) to
	* method descriptors of style3 with an interface symbol.
	*/
    public Vector doStyleConversion(EjbDescriptor ejbDescriptor, Collection allMethods) { // must be exact methods
	Vector v = new Vector();
	if (getStyle() == 1) { // STYLE 1
	    for (Iterator itr = allMethods.iterator(); itr.hasNext();) {
		MethodDescriptor next = (MethodDescriptor) itr.next();
                // when ejb-name is present
                // since it is an optional element in some case
                if (this.getEjbName() != null
                    && this.getEjbName().length() > 0) {
                    next.setEjbName(ejbDescriptor.getName());
                }
                /*
		if (!next.isExact()) {                    
		    //throw new RuntimeException("Conversion failed: " + next);
		}
                */
                if (this.getDescription() != null
                    && this.getDescription().length() > 0) {
		    next.setDescription(this.getDescription());
                }
		if (getEjbClassSymbol()==null) {
		    v.addElement(next);
		} else if (this.getEjbClassSymbol().equals(next.getEjbClassSymbol())) {
		    v.addElement(next);
		}
		
	    }
	} else if (this.getParameterClassNames() == null) { // STYLE 2
	    v.addAll(this.getMethodDescriptorsOfName(this.getName(), allMethods));
	} else { // STYLE 3, but maybe not exact
	    if (getEjbClassSymbol()==null) {
		v.addAll(this.getMethodDescriptorsOfNameAndParameters(this.getName(), this.getParameterClassNames(), allMethods));
	    } else {
		v.addElement(this); // this must be exact
	    }
	}
	return v;
    }
    
    private Set getMethodDescriptorsOfNameAndParameters(String name, String[] parameterArray, Collection methodDescriptors) {
	Set methods = new HashSet();
	for (Iterator itr = getMethodDescriptorsOfName(name, methodDescriptors).iterator(); itr.hasNext();) {
	    MethodDescriptor next = (MethodDescriptor) itr.next();
            next.setEjbName(getEjbName());
	    if (stringArrayEquals(parameterArray, next.getParameterClassNames())) {
		methods.add(next);
	    }
	}
	return methods;
    }
    
    private Set getMethodDescriptorsOfName(String name, Collection methodDescriptors) {
	Set set = new HashSet();
	for (Iterator itr = methodDescriptors.iterator(); itr.hasNext();) {
	    MethodDescriptor next = (MethodDescriptor) itr.next();
            next.setEjbName(getEjbName());
	    if (name.equals(next.getName())) {
		if (getEjbClassSymbol()==null) {
		    set.add(next);
		} else if (getEjbClassSymbol().equals(next.getEjbClassSymbol())) {
		    set.add(next);
		}
	    }
	}
	return set;
    }
    
    /** Returns the ejb class sybol for this method descriptor. */
    public String getEjbClassSymbol() {
	return this.ejbClassSymbol;
    }
     /** Sets the ejb class sybol for this method descriptor. */
    public void setEjbClassSymbol(String ejbClassSymbol) {
	this.ejbClassSymbol = ejbClassSymbol;
    }
    
    public String getFormattedString() {
	return this.getName() + this.getPrettyParameterString();
    }
    
    public String getPrettyParameterString() {
	StringBuilder prettyParameterString = new StringBuilder("(");
	if (this.parameterClassNames != null) {
	    for (int i = 0; i < this.parameterClassNames.length; i++) {
		int j = i + 1;
		if (i > 0) {
		    prettyParameterString.append(", ").append(this.parameterClassNames[i]).
                            append(" p").append(j);
		} else {
		    prettyParameterString.append(this.parameterClassNames[i]).
                            append(" p").append(j);
		}
	    }
	}
	prettyParameterString.append(")");
	return prettyParameterString.toString();
    }
    
    public String[] getParameterClassNames() {
	return parameterClassNames;
    }
    public String[] getJavaParameterClassNames (){
	return javaParameterClassNames;
    }

    private boolean stringArrayEquals(String[] s1, String[] s2) {
	if (s1 == null && s2 == null) {
	    return true;
	}
	if (s1 == null || s2 == null) {
	    return false;
	}
	if (s1.length == s2.length) {
	    for (int i = 0; i < s1.length; i++) {
		if (!s1[i].equals(s2[i])) {
		    return false;
		}
	    }
	    return true;
	} else {
	    return false;
	}
    }
    
    /** Equlity iff the parameter names match and the name matches.*/
    public boolean equals(Object other) {
	if (other instanceof MethodDescriptor) {
	    MethodDescriptor otherMethodDescriptor = (MethodDescriptor) other;
	    if (otherMethodDescriptor.getName().equals(getName())
		&& stringArrayEquals(otherMethodDescriptor.getParameterClassNames(), getParameterClassNames())) {
                    // If method names and params match, it still can be a wild-card method-name...
                    // And wild-card can be present with farious interfaces or no method-intf at all...
                    if (getEjbClassSymbol()!=null && otherMethodDescriptor.getEjbClassSymbol()!=null) {                        
                        // Method descriptors are equal if method-intf value is the same, even if they have
                        // wild-card method names
                        return getEjbClassSymbol().equals(otherMethodDescriptor.getEjbClassSymbol());
                    } else if (getName().equals(ALL_METHODS)) {
                        // For wild-card method names, method descriptors are equal if method-intf value is not set in both
                        return (getEjbClassSymbol() == null && otherMethodDescriptor.getEjbClassSymbol() == null);
                    }
                    // If method name is provided, and parameters match, we consider the method described being the same
                    // if the ejb class symbol (method-intf) is not defined in one of the descriptors
                    return true;
	    }
	}
	return false;
    }

    /** Indicates if a method descriptor implies the other one*/
    public boolean implies(Object other) {
        if (other != null && other instanceof MethodDescriptor) {
            MethodDescriptor otherMethodDescriptor = (MethodDescriptor) other;
            if (getName().equals(ALL_METHODS) || 
                getName().equals(otherMethodDescriptor.getName())) {
                if (getParameterClassNames() == null || 
                    stringArrayEquals(getParameterClassNames(), 
                        otherMethodDescriptor.getParameterClassNames())) { 
                    return true;
                }
            }
        }
        return false;
    }

    
    public int hashCode() {
	return this.getPrettyParameterString().hashCode() + this.getName().hashCode();
    }

    /** My pretty format. */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("Method Descriptor").append((ejbName==null?"":" for ejb " + ejbName)).append(
                " name: ").append(this.getName()).append(" params: ").append(this.getPrettyParameterString()).append( 
                " intf: ").append(this.ejbClassSymbol);
    }
    
    public String prettyPrint() {
        return "Name : " + this.getName() + " Params: " +  this.getPrettyParameterString() + " Intf: " +  this.ejbClassSymbol;
    }
    
    
    public String[] getParameterClassNamesFor(Method method, Class[] paramTypes) {
	String[] classNames = new String[paramTypes.length];
	for (int i = 0; i < paramTypes.length; i++) {
	    Class compType = paramTypes[i].getComponentType();
	    if ( compType == null ) { // not an array
		classNames[i] = paramTypes[i].getName();
	    }
	    else {
		// name of array types should be like int[][][]
		// Class.getName() returns something like [[[I
                int dimensions = 1;
                while(compType.getComponentType()!=null) {
                    dimensions++;
                    compType=compType.getComponentType();
                }
               
		classNames[i] = compType.getName();
		// add "[]" depending on array dimension
                for (int j=0;j<dimensions;j++) {
                    classNames[i] += "[]";
                }
	    }
	}
	return classNames;
    }

    private int isJavaFormat (String[] params){
	int ret = XML_JAVA_FORMAT; 
	for (int i=0; i<params.length; i++){
	    int index = params[i].indexOf ('[');
	    if (index == -1) {
		//not an array thus cannot determine format
		ret = XML_JAVA_FORMAT;
		continue;
	    } else if (index == 0) {// begins with [ thus java format
		return JAVA_FORMAT;
	    } else { // not java format thus of form int[][]
		return XML_FORMAT; 
	    }
	}
	return ret;	
    }

    private String[] getJavaFormatClassNamesFor(Class[] paramTypes){
	String[] classNames = new String[paramTypes.length];
	for (int i = 0; i < paramTypes.length; i++) {
	    classNames[i] = paramTypes[i].getName();
	}
	return classNames;
    }
    
    private String[] fixParamClassNames(String[] paramClassNames)
    {
	if(paramClassNames == null) {
	    return null;
	}

	String[] newParams = new String[paramClassNames.length];

	// This is done for backward compatibility with J2EE 1.2.1
	// in which param classnames were wrongly generated in [[[I form.
	for ( int i=0; i<paramClassNames.length; i++ ) {
	    newParams[i] = fixParamClassName(paramClassNames[i]);

	}

	return newParams;
    }
    private String[] xmlFormat2JavaClassNames (String[] from) {
	String[] to = new String[from.length];
	for (int i=0; i<from.length; i++) {
	    to[i] = xmlFormat2JavaClassNames (from[i]);
	}
	return to;
    }

    // Convert arrays from form  int[][][] to [[[L form.
    public static String xmlFormat2JavaClassNames (String param){
	int indexOfArray = param.indexOf ('[');
	if (indexOfArray == -1) {// not an array
	    return param;
	}
	String buf = param.substring (0, indexOfArray);
	int lastIndexOf = param.lastIndexOf (']');
	int dimension = lastIndexOf - indexOfArray + 1;
	dimension = dimension / 2;
	StringBuffer fs = new StringBuffer ();
	for (int i=0; i<dimension; i++) {
	    fs.append ("[");
	}
        String javaPrimitiveType = (String) getJavaPrimitiveTypes().get(buf);
        if (javaPrimitiveType!=null) {
            fs.append(javaPrimitiveType);
	} else { //default it is a class or a interface
	    fs.append ("L");
	    fs.append (buf);
	    fs.append (";");
	} 
	return fs.toString ();
    }
    
    /**
     * Computes the mapping between java primitive type and class loaders identifier
     * for such types.
     * 
     * @return the mapping with the java primitive type identifier as keys
     */
    public synchronized static Map getJavaPrimitiveTypes() {
        if (javaPrimitivesTypes==null) {
            javaPrimitivesTypes = new Hashtable();
            javaPrimitivesTypes.put("char", "C");
            javaPrimitivesTypes.put("byte", "B");
            javaPrimitivesTypes.put("double", "D");
            javaPrimitivesTypes.put("float", "F");
            javaPrimitivesTypes.put("int", "I");
            javaPrimitivesTypes.put("long", "J");
            javaPrimitivesTypes.put("short", "S");
            javaPrimitivesTypes.put("boolean", "Z");
        }
        return javaPrimitivesTypes;
    }
    
    private static Map javaPrimitivesTypes;

    // Convert arrays from [[[I form to int[][][] form.
    public static String fixParamClassName(String param)
    {
	if ( param.charAt(0) == '[' ) { // an array
	    int dimensions = param.lastIndexOf('[') + 1;	
	    char code = param.charAt(dimensions);
	    String newparam=null;
	    switch (code) {
		case 'B':
		    newparam = "byte";
                    break;
		case 'C':
		    newparam = "char";
                    break;
		case 'D':
		    newparam = "double";
                    break;
		case 'F':
		    newparam = "float";
                    break;
		case 'I':
		    newparam = "int";
                    break;
		case 'J':
		    newparam = "long";
                    break;
		case 'S':
		    newparam = "short";
                    break;
		case 'Z':
		    newparam = "boolean";
                    break;
		case 'L':
		    newparam = param.substring(dimensions+1);
                    break;
                default:
                  newparam = null;
	    }
            StringBuffer buf = new StringBuffer();
            buf.append(newparam);
	    for ( int j=0; j<dimensions; j++ )
                buf.append("[]");
            newparam = buf.toString();
	    return newparam;
	}
	else {
	    return param;
	}
    }

}
