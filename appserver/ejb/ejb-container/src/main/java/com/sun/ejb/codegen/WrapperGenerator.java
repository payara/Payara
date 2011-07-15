/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

import org.glassfish.api.deployment.DeploymentContext;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This class is used to generate the type specific EJBObject implementation
 */

public class WrapperGenerator extends Generator {

    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(WrapperGenerator.class);


    public static final String REMOTE_SUFFIX = "_EJBObjectImpl";
    public static final String LOCAL_SUFFIX = "_EJBLocalObjectImpl";

    private Class bean;
    private Class componentInterface; // remote or local interface
    private Method[] bizMethods;
    private String wrapperBase;
    private String wrapperImpl;
    private EjbDescriptor dd;
    private boolean isLocal;

    /**
     * Get the fully qualified name of the generated class.
     * Note: the remote/local implementation class is in the same package 
     * as the bean class, NOT the remote/local interface.
     * @return the name of the generated class.
     */
    public String getGeneratedClass() {
	String pname = getPackageName(bean.getName());
	if(pname != null)
	    return pname+"."+wrapperImpl;
	else 
	    return wrapperImpl;
    }
    
    public static String getDefaultEJBObjectImplClassName(EjbDescriptor desc) {
	//IASRI 4725194 return desc.getEjbClassName() + REMOTE_SUFFIX;
	return desc.getEjbImplClassName() + REMOTE_SUFFIX;
    }
    
    /**
     * Construct the Wrapper generator with the specified deployment
     * descriptor and class loader.
     * @param the Deployment Descriptor
     * @param the class loader.
     * @exception GeneratorException.
     */
    public WrapperGenerator(DeploymentContext context, EjbDescriptor dd, 
			    boolean isLocal, Vector existingClassNames)
	throws GeneratorException 
    {
	super();

	this.dd = dd;
	this.isLocal = isLocal;
        
        ClassLoader cl = context.getClassLoader();
	try {
	    //IASRI 4725194 this.bean = cl.loadClass(dd.getEjbClassName());
	    this.bean = cl.loadClass(dd.getEjbImplClassName());
	} catch (ClassNotFoundException ex) {
	    throw new InvalidBean(
		localStrings.getLocalString(
		"generator.bean_class_not_found",
		"Bean class not found "));
	}

	String compIntfName;
	if ( isLocal ) {
	    compIntfName = dd.getLocalClassName();
	    ejbClassSymbol = MethodDescriptor.EJB_LOCAL;
	}
	else {
	    compIntfName = dd.getRemoteClassName();
	    ejbClassSymbol = MethodDescriptor.EJB_REMOTE;
	}
	try {
	    this.componentInterface = cl.loadClass(compIntfName);
	} catch (ClassNotFoundException ex) {
	    throw new InvalidBean(
		localStrings.getLocalString(
		"generator.remote_interface_not_found",
		"Remote interface not found "));
	}
	
	String suffix;
	if ( isLocal ) {
	    wrapperBase = "com.sun.ejb.containers.EJBLocalObjectImpl";
	    suffix = LOCAL_SUFFIX;
	}
	else {
	    wrapperBase = "com.sun.ejb.containers.EJBObjectImpl";
	    suffix = REMOTE_SUFFIX;
	}

	// find a unique classname for this ejbObject/LocalObject impl
	String wrapperClassName = getUniqueClassName(context, bean.getName(), 
						  suffix, existingClassNames);
	wrapperImpl = getBaseName(wrapperClassName);

	bizMethods = removeDups(componentInterface.getMethods());
	bizMethods = removeEJBObjectMethods(bizMethods);
    }

    private Method[] removeEJBObjectMethods(Method[] methods)
    {
        // each remote method
        ArrayList newArray = new ArrayList();
        for(int i = 0; i < methods.length; i++) {
	    if( !isEJBObjectMethod(methods[i]) )
                newArray.add(methods[i]);
        }
        Method[] newMethods = new Method[newArray.size()];
        return (Method[])newArray.toArray(newMethods);
    }


    /**
     * Return true is method is on javax.ejb.EJBObject/EJBLocalObject
     */
    private boolean isEJBObjectMethod(Method methodToCheck) {
        Class ejbObjectClz = isLocal ? 
            javax.ejb.EJBLocalObject.class : javax.ejb.EJBObject.class;

        return isEJBIntfMethod(ejbObjectClz, methodToCheck);
    }
            
    /**
     * Generate the code to the specified output stream.
     * @param the output stream
     * @exception GeneratorException on a generation error
     * @exception IOException on an IO error
     */
    public void generate(OutputStream out)
	throws GeneratorException, IOException 
    {
	IndentingWriter p = new IndentingWriter(new OutputStreamWriter(out));

	String packageName = getPackageName(bean.getName());
	if (packageName != null)
	    p.pln("package " + packageName + ";");

	p.plnI("public final class " + wrapperImpl + " extends " + 
		wrapperBase + " implements " + componentInterface.getName() + 
		" {");

	// print static variables for Method objects and static initializer
	String[] methodVariables = printStaticMethodInit(p, componentInterface, 
							 bizMethods);

	// this is the constructor
	p.plnI("public " + wrapperImpl + "() "
		+ (isLocal ? "" : "throws java.rmi.RemoteException ") + "{");
	p.pOln("}");

	// each remote method
	for(int i = 0; i < bizMethods.length; i++) {
	    printMethodImpl(p, bizMethods[i], methodVariables[i]);
	}

	p.pOln("}");
	p.close();
    }


    /**
     * Generate the code for a single method.
     * @param the writer.
     * @param the method to generate code for.
     * @exception IOException.
     */
    private void printMethodImpl(IndentingWriter p, Method m, String methodVar)
	throws IOException
    {
	p.pln("");

	// print method signature and exceptions
	p.p("public " + printType(m.getReturnType()) + " "
		+ m.getName() + "(");
	Class[] params = m.getParameterTypes();
	for(int i = 0; i < params.length; i++) {
	    if (i != 0)
		p.p(", ");
	    p.p(printType(params[i]) + " param" + i);
	}
	p.p(") ");
	Class[] exceptions = m.getExceptionTypes();
	for(int i = 0; i < exceptions.length; i++) {
	    if (i == 0)
		p.p("throws ");
	    else
		p.p(", ");
	    p.p(exceptions[i].getName());
	}
	p.plnI("{");

	p.pln("com.sun.ejb.Invocation i = new com.sun.ejb.Invocation();");
	if ( isLocal ) {
	    p.pln("i.isLocal = true;");
	    // Set tx/security attrs only for local invocations.
	    // For remote invocations the generator does not
	    // run unless the interface itself has been modified,
	    // so the attr set in generated code could be wrong for
	    // remote intfs if only deployment desc was modified.
	    p.pln("i.transactionAttribute = com.sun.ejb.Container." +
						getTxAttribute(dd, m) + ";");
	    p.pln("i.securityPermissions = com.sun.ejb.Container." +
					 getSecurityAttribute(dd, m) + ";");
	}
	p.pln("i.ejbObject = this;");

	// print code for setting Method object in Invocation
	p.pln("i.method = " + methodVar + ";");

	if(!m.getReturnType().isPrimitive())
	    p.pln(printType(m.getReturnType()) + " $retVal = null;");
	else if (m.getReturnType() != void.class) {
	    if(m.getReturnType() == boolean.class)
	        p.pln(printType(m.getReturnType()) + " $retVal = false;");
	    else
	        p.pln(printType(m.getReturnType()) + " $retVal = 0;");
	}

	// print code for calling biz method on bean class
	p.pln("try {");
	p.pln("\tObject[] objarr = new Object["+params.length+"];");
	p.p("\n");
	for(int i = 0; i < params.length; i++) {
	    p.p("objarr["+i+"] = ");
	    Class clazz = params[i];
	    p.p(marshallPrimitiveToObject(clazz, i));
	}
	p.pln("\t\ti.methodParams = objarr;\n");
	p.pln("\tthis.getContainer().preInvoke(i);");
	p.pln("\tClass ejbClass = i.ejb.getClass();");
	p.pln("\tjava.lang.reflect.Method beanMethod = ejbClass.getMethod(i.method.getName(), i.method.getParameterTypes());");
	
	p.pln("\tObject obj = com.sun.enterprise.security.SecurityUtil.runMethod(beanMethod, i, i.ejb , objarr, this.getContainer());");
	Class clazz = m.getReturnType();
	p.p(marshallObjectToPrimitive(clazz, "obj", "$retVal"));
	p.p("\n");
	
	p.pln("} catch(Throwable c) {");
	p.pln("\ti.exception = c;");
	p.pln("} finally {");
	p.pln("\tthis.getContainer().postInvoke(i);");
	p.pln("}");

	// exception handling code
	p.plnI("if (i.exception != null) {");
	p.pln("if(i.exception instanceof java.lang.RuntimeException) {");
	p.pln("\tthrow (java.lang.RuntimeException)i.exception; ");
	p.p("} ");
	if ( !isLocal ) {
	    p.pln("else if(i.exception instanceof java.rmi.RemoteException) {");
	    p.pln("\tthrow (java.rmi.RemoteException)i.exception; ");
	    p.p("} ");
	}
	for(int i = 0; i < exceptions.length; i++) {
	    if(!exceptions[i].getName().equals("java.rmi.RemoteException")) {
	        p.pln("else if(i.exception instanceof " + 
			exceptions[i].getName() + ") {" );
	        p.pln("\tthrow (" + exceptions[i].getName() + ")i.exception;");
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
	    

	// print return code
	if (m.getReturnType() != void.class)
	    p.pln("return $retVal;");
	p.pOln("}");
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
	    }else if(clazz ==  void.class){
		return "\n";
	    }
	} 
	return retVal +"= ("+printType(clazz)+") "+obj+";\n";
    }
}
