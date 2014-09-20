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

import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;


/**
 * The base class for all code generators.
 */

public abstract class Generator {


    protected static final Logger _logger = LogDomains.getLogger(Generator.class, LogDomains.EJB_LOGGER);

    protected String ejbClassSymbol;

    public abstract String getGeneratedClass();


    /**
     * Get the package name from the class name.
     * @param className class name.
     * @return the package name.
     */
    protected String getPackageName(String className) {
        int dot = className.lastIndexOf('.');
        if (dot == -1)
            return null;
        return className.substring(0, dot);
    }

    protected String getBaseName(String className) {
        int dot = className.lastIndexOf('.');
        if (dot == -1)
            return className;
        return className.substring(dot+1);
    }

    protected String printType(Class cls) {
        if (cls.isArray()) {
            return printType(cls.getComponentType()) + "[]";
        } else {
            return cls.getName();
        }
    }

    protected Method[] removeDups(Method[] orig)
    {
        // Remove duplicates from method array.
        // Duplicates will arise if a class/intf and super-class/intf
        // define methods with the same signature. Potentially the
        // throws clauses of the methods may be different (note Java
        // requires that the superclass/intf method have a superset of the
        // exceptions in the derived method).
        Vector nodups = new Vector();
        for ( int i=0; i<orig.length; i++ ) {
            Method m1 = orig[i];
            boolean dup = false;
            for ( Enumeration e=nodups.elements(); e.hasMoreElements(); ) {
                Method m2 = (Method)e.nextElement();

                // m1 and m2 are duplicates if they have the same signature
                // (name and same parameters). 
                if ( !m1.getName().equals(m2.getName()) )
                    continue;

                Class[] m1parms = m1.getParameterTypes();
                Class[] m2parms = m2.getParameterTypes();
                if ( m1parms.length != m2parms.length )
                    continue;

                boolean parmsDup = true;
                for ( int j=0; j<m2parms.length; j++ ) {
                    if ( m1parms[j] != m2parms[j] ) {
                        parmsDup = false;
                        break;
                    }
                }
                if ( parmsDup ) {
                    dup = true;
                    // Select which of the duplicate methods to generate 
                    // code for: choose the one that is lower in the
                    // inheritance hierarchy: this ensures that the generated
                    // method will compile.
                    if ( m2.getDeclaringClass().isAssignableFrom(
                                                    m1.getDeclaringClass()) ) {
                        // m2 is a superclass/intf of m1, so replace m2 with m1
                        nodups.remove(m2);
                        nodups.add(m1);
                    }
                    break;
                }
            }

            if ( !dup )
                nodups.add(m1);
        }
        return (Method[])nodups.toArray(new Method[nodups.size()]);
    }

    /**
     * Return true if method is on a javax.ejb.EJBObject/EJBLocalObject/
     * javax.ejb.EJBHome,javax.ejb.EJBLocalHome interface.
     */
    protected boolean isEJBIntfMethod(Class ejbIntfClz, Method methodToCheck) {
        boolean isEJBIntfMethod = false;

        Method[] ejbIntfMethods = ejbIntfClz.getMethods();
        for(int i = 0; i < ejbIntfMethods.length; i++) {
            Method next = ejbIntfMethods[i];
            if(methodCompare(methodToCheck, next)) {
                isEJBIntfMethod = true;

                String ejbIntfClzName  = ejbIntfClz.getName();
                Class methodToCheckClz = methodToCheck.getDeclaringClass();
                if( !methodToCheckClz.getName().equals(ejbIntfClzName) ) {
                    String[] logParams = { next.toString(), 
                                           methodToCheck.toString() };
                    _logger.log(Level.WARNING, 
                                "ejb.illegal_ejb_interface_override",
                                logParams);
                }

                break;
            }
        }

	return isEJBIntfMethod;
    }


    private boolean methodCompare(Method factoryMethod, Method homeMethod) {

	if(! factoryMethod.getName().equals(homeMethod.getName())) {
	    return false;
        }

	Class[] factoryParamTypes = factoryMethod.getParameterTypes();
	Class[] beanParamTypes = homeMethod.getParameterTypes();
	if (factoryParamTypes.length != beanParamTypes.length) {
	    return false;
        }
	for(int i = 0; i < factoryParamTypes.length; i++) {
	    if (factoryParamTypes[i] != beanParamTypes[i]) {
		return false;
            }
        }

	// NOTE : Exceptions and return types are not part of equality check

	return true;

    }

    protected String getUniqueClassName(DeploymentContext context, String origName,
                                        String origSuffix,
					Vector existingClassNames)
    {
	String newClassName = null;
	boolean foundUniqueName = false;
	int count = 0;
	while ( !foundUniqueName ) {
	    String suffix = origSuffix;
	    if ( count > 0 ) {
		suffix = origSuffix + count;
            }
	    newClassName = origName + suffix;
	    if ( !existingClassNames.contains(newClassName) ) {
		foundUniqueName = true;
                existingClassNames.add(newClassName);
            }
	    else {
		count++;
            }
	}
	return newClassName;
    }


    protected String getTxAttribute(EjbDescriptor dd, Method method)
    {
	// The TX_* strings returned MUST match the TX_* constants in 
	// com.sun.ejb.Container.
        if ( dd instanceof EjbSessionDescriptor
	     && ((EjbSessionDescriptor)dd).getTransactionType().equals("Bean") )
	    return "TX_BEAN_MANAGED";

        String txAttr = null;
	MethodDescriptor mdesc = new MethodDescriptor(method, ejbClassSymbol);
        ContainerTransaction ct = dd.getContainerTransactionFor(mdesc);
        if ( ct != null ) {
            String attr = ct.getTransactionAttribute();
            if ( attr.equals(ContainerTransaction.NOT_SUPPORTED) )
                txAttr = "TX_NOT_SUPPORTED";
            else if ( attr.equals(ContainerTransaction.SUPPORTS) )
                txAttr = "TX_SUPPORTS";
            else if ( attr.equals(ContainerTransaction.REQUIRED) )
                txAttr = "TX_REQUIRED";
            else if ( attr.equals(ContainerTransaction.REQUIRES_NEW) )
                txAttr = "TX_REQUIRES_NEW";
            else if ( attr.equals(ContainerTransaction.MANDATORY) )
                txAttr = "TX_MANDATORY";
            else if ( attr.equals(ContainerTransaction.NEVER) )
                txAttr = "TX_NEVER";
        }

        if ( txAttr == null ) {
            throw new RuntimeException("Transaction Attribute not found for method "+method);
        }
	return txAttr;
    } 

    protected String getSecurityAttribute(EjbDescriptor dd, Method m)
    {
	// The SEC_* strings returned MUST match the SEC_* constants in 
	// com.sun.ejb.Container.

	MethodDescriptor thisMethodDesc = new MethodDescriptor(m, ejbClassSymbol);

	Set unchecked = dd.getUncheckedMethodDescriptors();
	if ( unchecked != null ) {
	    Iterator i = unchecked.iterator();
	    while ( i.hasNext() ) {
		MethodDescriptor md = (MethodDescriptor)i.next();
		if ( thisMethodDesc.equals(md) )
		    return "SEC_UNCHECKED";
	    }
	}

	Set excluded = dd.getExcludedMethodDescriptors();
	if ( excluded != null ) {
	    Iterator i = excluded.iterator();
	    while ( i.hasNext() ) {
		MethodDescriptor md = (MethodDescriptor)i.next();
		if ( thisMethodDesc.equals(md) )
		    return "SEC_EXCLUDED";
	    }
	}

	return "SEC_CHECKED";
    } 

}
