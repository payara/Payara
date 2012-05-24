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

package com.sun.ejb.codegen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import javax.ejb.EntityBean;
import javax.ejb.SessionBean;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;


/**
 * This class generates the EJBHome implementation.
 */
public class HomeGenerator extends Generator {
    
    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(HomeGenerator.class);
    public static final String LOCAL_SUFFIX = "_LocalHomeImpl";
    public static final String REMOTE_SUFFIX = "_RemoteHomeImpl";

    private Method[] factoryMethods;
    private Method[] allbeanMethods;
    private Class homeInterface;
    private Class bean;
    private String homeImpl;
    private EjbDescriptor dd;
    private boolean isLocal=false;

    private static int CREATE = 0;
    private static int FINDER = 1;
    private static int OTHER = 2;

	//IASRI 4717059 BEGIN
	private boolean isReadOnlyBean = false;
	//IASRI 4717059 END

    private static final String READ_ONLY_EJB_HOME_IMPL
        = "com.sun.ejb.containers.ReadOnlyEJBHomeImpl";
    private static final String READ_ONLY_EJB_LOCAL_HOME_IMPL
        = "com.sun.ejb.containers.ReadOnlyEJBLocalHomeImpl";
    private static final String READ_ONLY_EJB_HOME_INTERFACE
        = "com.sun.ejb.containers.ReadOnlyEJBHome";
    private static final String READ_ONLY_EJB_LOCAL_HOME_INTERFACE
        = "com.sun.ejb.containers.ReadOnlyEJBLocalHome";

    /**
     * Get the fully qualified name of the generated class.
     * Note: the home implementation class is in the same package 
     * as the bean class, NOT the home interface.
     * @return the name of the generated class.
     */
    public String getGeneratedClass() {
	String pname = getPackageName(bean.getName());
	if(pname != null)
	    return pname+"."+homeImpl;
	else 
	    return homeImpl;
    }

    public static String getDefaultRemoteHomeImplClassName(EjbDescriptor desc) {
	//IASRI 4725194 return desc.getEjbClassName() + REMOTE_SUFFIX;
	return desc.getEjbImplClassName() + REMOTE_SUFFIX;
    }
    
    public HomeGenerator(DeploymentContext context, EjbDescriptor dd,  boolean isLocal,
			 Vector existingClassNames)
	throws GeneratorException 
    {
	super();

	this.dd = dd;
	this.isLocal = isLocal;

	String homeName;
        ClassLoader cl = context.getClassLoader();
	if ( isLocal ) {
	    homeName = dd.getLocalHomeClassName();
	    ejbClassSymbol = MethodDescriptor.EJB_LOCALHOME;
	}
	else {
	    homeName = dd.getHomeClassName();
	    ejbClassSymbol = MethodDescriptor.EJB_HOME;
	}
	try {
	    this.homeInterface = cl.loadClass(homeName);
	} catch (ClassNotFoundException ex) {
	    throw new InvalidHome(
				localStrings.getLocalString(
				"generator.invalid_home",
				"Could not find home class {0}",
				new Object[] {homeName}));
	}
	
	try {
            
	    this.bean =
		//IASRI 4725194 cl.loadClass(dd.getEjbClassName());
		cl.loadClass(dd.getEjbImplClassName());
	} catch (ClassNotFoundException ex) {
	    _logger.log(Level.FINE,"ejb.classnotfound_exception",ex);

	    InvalidBean ibe = new InvalidBean(localStrings.getLocalString
               ("generator.bean_class_not_found",
                "Bean class {0} not found ",
                new Object[] { dd.getEjbImplClassName()}));
            ibe.initCause(ex);
            throw ibe;
	}
	
	if ( !SessionBean.class.isAssignableFrom(bean)
	     && !EntityBean.class.isAssignableFrom(bean)) {
	    throw new InvalidBean(localStrings.getLocalString(
                            "generator.invalid_bean", 
                            "Bean {0} is neither {1} nor {2}.",
                            new Object[] {dd.getName(), "an EntityBean", 
                                              "a SessionBean"}));	
        }

	factoryMethods = removeDups(homeInterface.getMethods());
	factoryMethods = removeEJBHomeMethods(factoryMethods);

	allbeanMethods = removeDups(bean.getMethods());

	// find a unique classname for this home impl
	String suffix;
	if ( isLocal )
	    suffix = LOCAL_SUFFIX;
	else
	    suffix = REMOTE_SUFFIX;
	String homeClassName = getUniqueClassName(context, bean.getName(), 
						  suffix, existingClassNames);
	homeImpl = getBaseName(homeClassName);

	//IASRI 4717059 BEGIN
	if( dd instanceof EjbEntityDescriptor) {
		if (((EjbEntityDescriptor)dd).getIASEjbExtraDescriptors().isIsReadOnlyBean()) 
		{
			isReadOnlyBean = true;
		}
	}
	//IASRI 4717059 END

    }


    private Method[] removeEJBHomeMethods(Method[] methods)
    {
        // each remote method
        ArrayList newArray = new ArrayList();
        for(int i = 0; i < methods.length; i++) {
	    if( !isEJBHomeMethod(methods[i]) )
                newArray.add(methods[i]);
        }
        Method[] newMethods = new Method[newArray.size()];
        return (Method[])newArray.toArray(newMethods);
    }


    private boolean compare(Method factoryMethod, Method beanMethod) {

	if ( factoryMethod.getName().startsWith("create") 
	     && !beanMethod.getName().startsWith("ejbCreate") )
	    return false;
	    
	if ( factoryMethod.getName().startsWith("find") 
	     && !beanMethod.getName().startsWith("ejbFind") )
	    return false;
	    
	Class[] factoryParamTypes = factoryMethod.getParameterTypes();
	Class[] beanParamTypes = beanMethod.getParameterTypes();
	if (factoryParamTypes.length != beanParamTypes.length)
	    return false;
	for(int i = 0; i < factoryParamTypes.length; i++)
	    if (factoryParamTypes[i] != beanParamTypes[i])
		return false;

	return true;
    }

    private Method getBeanMethod(Method factoryMethod) {
	for(int i = 0; i < allbeanMethods.length; i++) {
	    if (isInitializeMethod(allbeanMethods[i])) {
		if (compare(factoryMethod, allbeanMethods[i]))
		    return allbeanMethods[i];
	    }
	}
	return null;
    }

    private void printFactoryMethodImpl(IndentingWriter p, Method factoryMethod,
                                        int type, String methodVar)
	throws IOException
    {
	boolean isStatelessSession = false;
	boolean isEntity = false;
        boolean isReadOnlyBean = false;
	if ( dd instanceof EjbSessionDescriptor ) {
	    if ( ((EjbSessionDescriptor)dd).isStateless() )
		isStatelessSession = true;
	} else if( dd instanceof EjbEntityDescriptor) {
	    isEntity = true;
            if (((EjbEntityDescriptor)dd).getIASEjbExtraDescriptors().isIsReadOnlyBean()) 
            {
                isReadOnlyBean = true;
            }
	}

	p.pln("");

	// print method signature and exceptions
	p.p("public ");
	p.p(printType(factoryMethod.getReturnType()) + " ");
	p.p(factoryMethod.getName() + "(");
	Class[] params = factoryMethod.getParameterTypes(); 
	for(int i = 0; i < params.length; i++) {
	    if (i != 0)
		p.p(", ");
	    p.p(printType(params[i]) + " param" + i);
	}
	p.p(")");
	Class[] exceptions = factoryMethod.getExceptionTypes();
	if (exceptions.length != 0)
	    p.p(" throws ");
	for(int i = 0; i < exceptions.length; i++) {
	    if (i != 0)
		p.p(", ");
	    p.p(exceptions[i].getName());
	}

	p.plnI(" {");

	// We dont know the ejbObject classname (local/remote impl class) 
	// because the WrapperGenerator has not been run yet.
	String ejbObjectName;
	if ( isLocal )
	    ejbObjectName = "com.sun.ejb.containers.EJBLocalObjectImpl";
	else
	    ejbObjectName = "com.sun.ejb.containers.EJBObjectImpl";

	if ( type == CREATE ) {
	    if ( !isEntity ) {
		p.pln(ejbObjectName + " ejbObject = (" + ejbObjectName
				+ ") this.createEJB"
				+ ( isLocal ? "LocalObjectImpl();" : "ObjectImpl();" ));
	    }
	} else {
	    // home methods and finders
	    if ( !isReturnTypeVoid(factoryMethod) ) {
                String retType = printType(factoryMethod.getReturnType());
                p.p(retType + " ejbObject = ");
                // Have to initialize or compiler treats it as an error
                if( isReturnTypePrimitive(factoryMethod) ) {
                    if( isReturnTypeBoolean(factoryMethod) ) {
                        p.p("false");
                    } else {
                        p.p("0");
                    }
                } else {
                    p.p("null");
                }
                p.pln(";");
            }
	}

	// print code for calling ejbCreate* / ejbFind* / ejbHome*
	if ( !isStatelessSession ) { 
            if ((isReadOnlyBean) && isCreateMethod(factoryMethod)) {
                p.pln ("throw new javax.ejb.CreateException (\"Create method is not allowed for a Read-Only Bean\");");
                p.pOln("}");
                return;
            }

            if ((isReadOnlyBean) && isCreateMethod(factoryMethod)) {
                p.pln ("throw new javax.ejb.CreateException (\"Remove method is not allowed for a Read-Only Bean\");");
                p.pOln("}");
                return;
            }       

	    p.pln("com.sun.ejb.Invocation i = new com.sun.ejb.Invocation();");
            p.pln("i.isHome = true;");
	    if ( isLocal ) {
		p.pln("i.isLocal = true;");
		p.pln("i.transactionAttribute = com.sun.ejb.Container." +
				    getTxAttribute(dd, factoryMethod) + ";");
		p.pln("i.securityPermissions = com.sun.ejb.Container." +
			    getSecurityAttribute(dd, factoryMethod) + ";");
	    }

	    if ( type == CREATE && !isEntity )
	        p.pln("i.ejbObject = ejbObject;");

	    p.pln("i.method = " + methodVar + ";");
	    p.pln("try {");
	    p.pln("\tObject[] objarr = new Object["+params.length+"];");
	    p.p("\n");
	    for(int i = 0; i < params.length; i++) {
		p.p("\t\tobjarr["+i+"] = ");
		Class clazz = params[i];
		p.p(marshallPrimitiveToObject(clazz, i));
	    }
	    p.pln("\t\ti.methodParams = objarr;");
	    p.pln("\tthis.getContainer().preInvoke(i);");
	    p.pln("\t" + bean.getName() + " ejb = (" + bean.getName() + ") i.ejb;");

	    if ( type == CREATE ) {
		String name = factoryMethod.getName().substring("create".length());
		p.pln("\t\tjava.lang.reflect.Method method = i.ejb.getClass().getMethod(\"ejbCreate"+name+"\", i.method.getParameterTypes());");
		
	        if ( isEntity ) {
		    p.p("\tjava.lang.Object primaryKey = ");
		    p.p("\tcom.sun.enterprise.security.SecurityUtil.runMethod(method, i, ejb, objarr, this.getContainer());\n");
	        } else {
		    p.p("\tcom.sun.enterprise.security.SecurityUtil.runMethod(method, i, ejb, objarr, this.getContainer());\n");
		}
	        if(isEntity) {
		    p.pln("\t\tjava.lang.reflect.Method __method__postCreate = i.ejb.getClass().getMethod(\"ejbPostCreate"+name+"\", i.method.getParameterTypes());");
		    p.pln("\tthis.getContainer().postCreate(i, primaryKey);");
		    p.p("\tcom.sun.enterprise.security.SecurityUtil.runMethod(__method__postCreate, i, ejb, objarr, this.getContainer());\n");
	        }
	    } else if (type == FINDER) { // generate code for finder method
		String name = factoryMethod.getName().substring("find".length());
		p.pln("\t\tjava.lang.reflect.Method __method__finder = i.ejb.getClass().getMethod(\"ejbFind"+ name+"\", i.method.getParameterTypes());\n");
		// For BMP, postFind needs primaryKeys returned by EJB
		p.pln("\tjava.lang.Object primaryKeys = null;");
		p.p("\tprimaryKeys = com.sun.enterprise.security.SecurityUtil.runMethod(__method__finder, i, ejb, objarr, this.getContainer());\n");
		p.pln("\tejbObject = (" + 
		    factoryMethod.getReturnType().getName() + 
		      ")this.getContainer().postFind(i, primaryKeys, null);");
		
	    } else {
		// other home method.
		String name = factoryMethod.getName();
		p.p("\t");
		// for a home method foo, call ejbHomeFoo.
		String upperCasedName = name.substring(0,1).toUpperCase()
		    + name.substring(1);
		p.p("\tjava.lang.reflect.Method __method__home = ");
		p.p("i.ejb.getClass().getMethod(\""+"ejbHome"+upperCasedName+"\", i.method.getParameterTypes());\n");
		
		if( ! isReturnTypeVoid(factoryMethod) ){ 
		    p.p("java.lang.Object obj = ");
		    p.pln("com.sun.enterprise.security.SecurityUtil.runMethod(__method__home, i, ejb, objarr, this.getContainer());\n");
		    Class clazz = factoryMethod.getReturnType();
		    p.p(marshallObjectToPrimitive(clazz, "obj", "ejbObject"));
		} else{
		    p.pln("com.sun.enterprise.security.SecurityUtil.runMethod(__method__home, i, ejb, objarr, this.getContainer());\n");
		}
	    }
	    p.pln("} catch(Throwable c) {");
	    p.pln("\ti.exception = c;");
	    p.pln("} finally {");
	    p.pln("\tthis.getContainer().postInvoke(i);");
	    p.pln("}");

	    // print exception throw code
	    p.plnI("if (i.exception != null) {");
	    p.pln("if(i.exception instanceof java.lang.RuntimeException) {");
	    p.pln("\tthrow (java.lang.RuntimeException)i.exception; ");
	    p.p("} ");
	    if ( !isLocal ) {
		p.pln("else if (i.exception instanceof java.rmi.RemoteException) {");
		p.pln("\tthrow (java.rmi.RemoteException)i.exception; ");
		p.p("} ");
	    }
	    for(int i = 0; i < exceptions.length; i++) {
		if(!exceptions[i].getName().equals("java.rmi.RemoteException")) {
		    p.pln("else if (i.exception instanceof " + 
			    exceptions[i].getName() + ") {" );
		    p.pln("\tthrow (" + exceptions[i].getName() + 
			    ")i.exception;");
		    p.p("} ");
		}
	    }
	    if ( isLocal ) {
		p.pln("else if (i.exception instanceof Exception) {");
		p.pln("\tthrow new javax.ejb.EJBException(\"Unknown exception\", (Exception)i.exception);");
		p.pln("}");
		p.pln("else {");
		p.pln("\tthrow new javax.ejb.EJBException(i.exception.getMessage());");
		p.pln("}");
	    }
	    else {
		p.pln("else {");
		p.pln("\tthrow new java.rmi.RemoteException(\"Unknown exception\", i.exception);");
		p.pln("}");
	    }
	    p.pOln("}");
	}

	// print return code
	if ( type == CREATE ) {
	    if ( isEntity ) {
		p.pln("return ("+ factoryMethod.getReturnType().getName() +")" 
		      + "((" + ejbObjectName + ")i.ejbObject)"
				+ (isLocal ? ";" : ".getStub();") );
	    } else {
		p.pln("return ("+ factoryMethod.getReturnType().getName() 
				+ ")ejbObject"
				+ (isLocal ? ";" : ".getStub();") );
	    }
	}
	else {
	    if ( !isReturnTypeVoid(factoryMethod) )
	        p.pln("return ejbObject;");
	}

	p.pOln("}");
    }

    /**
     * @exception GeneratorException on a generation error
     * @exception IOException on an IO error
     */
    public void generate(OutputStream out)
	throws GeneratorException, IOException {
	    IndentingWriter p
		= new IndentingWriter(new OutputStreamWriter(out));

	    String packageName = getPackageName(bean.getName());
	    if (packageName != null)
		p.pln("package " + packageName + ";");

		//IASRI 4717059 BEGIN
		if (isReadOnlyBean) {
            p.plnI("public final class " + homeImpl
                + " extends "
                + (isLocal ? READ_ONLY_EJB_LOCAL_HOME_IMPL : READ_ONLY_EJB_HOME_IMPL)
                + " implements " + homeInterface.getName()
                + ", " + (isLocal ? READ_ONLY_EJB_LOCAL_HOME_INTERFACE : READ_ONLY_EJB_HOME_INTERFACE)
                + " {");
		} else {
		//IASRI 4717059 END
	    	p.plnI("public final class " + homeImpl
			    + " extends com.sun.ejb.containers." 
			    + (isLocal ? "EJBLocalHomeImpl" : "EJBHomeImpl")
			    + " implements " + homeInterface.getName() + " {");
		//IASRI 4717059 BEGIN
		}
		//IASRI 4717059 END

	    // print static variables for Method objects and static initializer
	    String[] methodVariables = printStaticMethodInit(p, homeInterface, 
							     factoryMethods);

	    // constructor
	    p.plnI("public " + homeImpl + "() "
			    +(isLocal ? "" : "throws java.rmi.RemoteException ")
			    + "{");
	    p.pln("super();");
	    p.pOln("}");
	
	    for(int i = 0; i < factoryMethods.length; i++) {
		if ( isCreateMethod(factoryMethods[i]) ) { 
		    Method m = getBeanMethod(factoryMethods[i]);
		    if (m == null)
			throw new MethodNotFound("Could not find bean method for factory method: " + factoryMethods[i]);
		    printFactoryMethodImpl(p, factoryMethods[i], CREATE,
					   methodVariables[i]);
		} else if ( isFinderMethod(factoryMethods[i]) ) {
		    // Note: Container-Managed EntityBeans dont implement finder
		    printFactoryMethodImpl(p, factoryMethods[i], FINDER,
					   methodVariables[i]);
		} else {
		    // must be a home method.
		    printFactoryMethodImpl(p, factoryMethods[i], OTHER,
					   methodVariables[i]);
		}
	    }
	    p.pOln("}");
	    p.close();
    }

    /**
     * Return true if this is a "create" method.
     * @return true if it is a create method, false otherwise.
     * @param the method object.
     */
    private boolean isCreateMethod(Method m) {
	// XXX need to verify factory method returns
	//      the remote/local interface.
	if (Modifier.isPublic(m.getModifiers())
	    && m.getName().startsWith("create"))
	    return true;
	return false;
    }

    private boolean isRemoveMethod(Method m) {
	    // XXX need to verify factory method returns
	    //     the remote/local interface.
	    if (Modifier.isPublic(m.getModifiers())
	        && m.getName().startsWith("remove"))
	        return true;
	    return false;
    }    

    private boolean isFinderMethod(Method m) {
	// XXX need to verify factory method returns
	//     the remote interface.
	if (Modifier.isPublic(m.getModifiers())
	    && m.getName().startsWith("find"))
	    return true;
	return false;
    }

    private boolean isInitializeMethod(Method m) {
	if (Modifier.isPublic(m.getModifiers())
	    && (m.getName().startsWith("ejbCreate") ||
		m.getName().startsWith("ejbFind")))
	    return true;
	return false;
    }

    private String getPrimitiveWrapper(Class pclass)
    {
	if ( pclass == boolean.class )
	    return "Boolean";
	else if ( pclass == byte.class )
	    return "Byte";
	else if ( pclass == char.class )
	    return "Character";
	else if ( pclass == short.class )
	    return "Short";
	else if ( pclass == int.class )
	    return "Integer";
	else if ( pclass == long.class )
	    return "Long";
	else if ( pclass == float.class )
	    return "Float";
	else if ( pclass == double.class )
	    return "Double";
	else
	    throw new RuntimeException("Bad primitive class");
    }

    /**
     * Return true is method is on javax.ejb.EJBHome/EJBLocalHome
     */
    private boolean isEJBHomeMethod(Method methodToCheck) {

        Class ejbHomeClz = isLocal ? 
            javax.ejb.EJBLocalHome.class : javax.ejb.EJBHome.class;

	return isEJBIntfMethod(ejbHomeClz, methodToCheck);
    }

    private boolean isReturnTypeVoid(Method m) {
	String name = m.getReturnType().getName();
	if(name.equals("void")) {
	    return true;
	}
	return false;
    }

    private boolean isReturnTypeBoolean(Method m) {
	String name = m.getReturnType().getName();
        return name.equals("boolean");
    }

    private boolean isReturnTypePrimitive(Method m) {
        return m.getReturnType().isPrimitive();
    }
    private String marshallPrimitiveToObject(Class clazz , int i){
	if(clazz.isPrimitive()){
	    if(clazz ==  int.class){
		return " new java.lang.Integer(param"+i+");\n";
	    }else if(clazz ==  boolean.class){
		return " java.lang.Boolean.valueOf(param"+i+");\n";
	    }else if(clazz ==  byte.class){
		return " new java.lang.Byte(param"+i+");\n";
	    }else if(clazz ==  short.class){
		return " new java.lang.Short(param"+i+");\n";
	    }else if(clazz ==  long.class){
		return " new java.lang.Long(param"+i+");\n";
	    }else if(clazz ==  float.class){
		return " new java.lang.Float(param"+i+");\n";
	    }else if(clazz ==  double.class){
		return " new java.lang.Double(param"+i+");\n";
	    }else if(clazz ==  char.class){
		return " new java.lang.Character(param"+i+");\n";
	    }
	}
	return  "(java.lang.Object)param"+i+";\n";
    }
    private String marshallObjectToPrimitive(Class clazz , String obj, String retVal){
	if(clazz.isPrimitive()){
	    if(clazz ==  int.class){
		return retVal+ "= ((java.lang.Integer)"+ obj+").intValue();\n";
	    }else if(clazz ==  boolean.class){
		return retVal+" = ((java.lang.Boolean)"+obj+").booleanValue();\n";
	    }else if(clazz ==  byte.class){
		return retVal+" = ((java.lang.Byte)"+obj+").byteValue();\n";
	    }else if(clazz ==  short.class){
		return retVal+" = ((java.lang.Short)"+obj+").shortValue();\n";
	    }else if(clazz ==  long.class){
		return retVal+" = ((java.lang.Long)"+obj+").longValue();\n";
	    }else if(clazz ==  float.class){
		return retVal+" = ((java.lang.Float)"+obj+").floatValue();\n";
	    }else if(clazz ==  double.class){
		return retVal+" = ((java.lang.Double)"+obj+").doubleValue();\n";
	    }else if(clazz ==  char.class){
		return retVal+" = ((java.lang.Character)"+obj+").charValue();\n";
	    }
	}
	return retVal +"= ("+printType(clazz)+") "+obj+";\n";
    }
}
