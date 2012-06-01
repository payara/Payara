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

package com.sun.enterprise.tools.verifier.tests.ejb;

import org.glassfish.ejb.deployment.descriptor.FieldDescriptor;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.Vector;

/**
 * Exceptions checked for CreateException, FinderException, RemoteException
 * compliance test.
 * 
 */
public class EjbUtils { 



    /** 
     * The names of the fields in the primary key class must be a subset of the
     * names of the container-managed fields.
     * Verify the following:
     *
     *   The primary key class field must be a subset of the names of the 
     *   container-managed fields.  
     *
     * @param field the field to be checked for containment within the names of the 
     *        container-managed fields
     * @param CMPFields the Set of contianer managed fields 
     *
     * @return <code>boolean</code> true if field is subset of CMP fields, false otherwise
     */
    public static boolean isFieldSubsetOfCMP(Field field, Set CMPFields) {
	if (CMPFields.contains(new FieldDescriptor(field))) {
	    return true;
	} else {
	    return false;
	}
    }


    /**
     * The names of the fields in the primary key class must correspond to the
     * field names of the entity bean class that comprise the key.
     * Verify the following:
     *
     *   The primary key class field must correspond to the field names of the 
     *   entity bean class that comprise the key.
     *
     * @param field the field to be checked for matching bean field 
     * @param beanFields the Set of contianer managed fields 
     *
     * @return <code>boolean</code> true if field is subset of bean class fields, false otherwise
     */
    public static boolean isPKFieldMatchingBeanFields(Field field, Vector beanFields) {

      for (int i = 0; i < beanFields.size(); i++) {
          if (((FieldDescriptor)beanFields.elementAt(i)).getName().equals(field.getName())) {
	      return true;
	  } else {
              continue;
	  }
      }
      // if you made it here, then field[] didn't contain field
      return false;
    }


    /**
     * Method exception javax.ejb.CreateException checked for compliance 
     * test.
     *
     * Verify the following:
     *
     *   The home/remote interface methods exception types must be legal types for
     *   CreateException.  
     *   This means that their exception must throw javax.ejb.CreateException.
     *
     * @param methodExceptions the exceptions to be checked for throws
     *        javax.ejb.CreateException 
     *
     * @return <code>boolean</code> true if exceptions throw javax.ejb.CreateException, false otherwise
     */
    public static boolean isValidCreateException(Class [] methodExceptions) {
	// methods must throw javax.ejb.CreateException
	boolean throwsCreateException = false;
	for (int kk = 0; kk < methodExceptions.length; ++kk) {
	    if ((methodExceptions[kk].getName().equals("javax.ejb.CreateException")) ||
		(methodExceptions[kk].getName().equals("CreateException"))) {
		throwsCreateException = true;
		break;
	    }
	}
	return throwsCreateException;

    }



    /**
     * Method exception java.rmi.RemoteException checked for compliance 
     * test.
     *
     * Verify the following:
     *
     *   The home/remote interface methods exception types must be legal types for
     *   RemoteException.  
     *   This means that their exception must throw java.rmi.RemoteException
     *
     * @param methodExceptions the exceptions to be checked for throws
     *        java.rmi.RemoteException 
     *
     * @return <code>boolean</code> true if exceptions throw java.rmi.RemoteException, false otherwise
     */
    public static boolean isValidRemoteException(Class [] methodExceptions) {
	// methods must throw java.rmi.RemoteException
	boolean throwsRemoteException = false;
	for (int kk = 0; kk < methodExceptions.length; ++kk) {
	    if ((methodExceptions[kk].getName().equals("java.rmi.RemoteException")) ||
		(methodExceptions[kk].getName().equals("RemoteException"))) {
		throwsRemoteException = true;
		break;
	    }
	}
	return throwsRemoteException;

    }



    /**
     * Method exception javax.ejb.ObjectNotFoundException checked for compliance 
     * test.
     *
     * Verify the following:
     *
     *     The ObjectNotFoundException is a subclass of FinderException. It is
     *     thrown by the ejbFind<METHOD>(...) methods to indicate that the
     *     requested entity object does not exist.
     *
     *     Only single-object finders (see Subsection 9.1.8) may throw this
     *     exception.   Multi-object finders must not throw this exception.
     *
     * @param methodExceptions the exceptions to be checked for throws
     *        javax.ejb.ObjectNotFoundException 
     *
     * @return <code>boolean</code> true if exceptions throw javax.ejb.ObjectNotFoundException, false otherwise
     */
    public static boolean isValidObjectNotFoundExceptionException(Class [] methodExceptions) {
	// methods must throw javax.ejb.ObjectNotFoundException
	boolean throwsObjectNotFoundException = false;
	for (int kk = 0; kk < methodExceptions.length; ++kk) {
	    if ((methodExceptions[kk].getName().equals("javax.ejb.ObjectNotFoundException")) ||
		(methodExceptions[kk].getName().equals("ObjectNotFoundException"))) {
		throwsObjectNotFoundException = true;
		break;
	    }
	}
	return throwsObjectNotFoundException;
    }




    /**
     * Method exception javax.ejb.FinderException checked for compliance 
     * test.
     *
     * Verify the following:
     *
     *   The home/remote interface methods exception types must be legal types for
     *   FinderException  
     *   This means that their exception must throw javax.ejb.FinderException
     *
     * @param methodExceptions the exceptions to be checked for throws
     *        javax.ejb.FinderException 
     *
     * @return <code>boolean</code> true if exceptions throw javax.ejb.FinderException, false otherwise
     */
    public static boolean isValidFinderException(Class [] methodExceptions) {
	// methods must throw javax.ejb.FinderException
	boolean throwsFinderException = false;
	for (int kk = 0; kk < methodExceptions.length; ++kk) {
	    if ((methodExceptions[kk].getName().equals("javax.ejb.FinderException")) ||
		(methodExceptions[kk].getName().equals("FinderException"))) {
		throwsFinderException = true;
		break;
	    }
	}
	return throwsFinderException;

    }


    /** Class checked for implementing java.io.Serializable interface test.
     * Verify the following:
     *
     *   The class must implement the java.io.Serializable interface, either
     *   directly or indirectly.
     *
     * @param serClass the class to be checked for Rmi-IIOP value type
     *        compliance
     *
     * @return <code>boolean</code> true if class implements java.io.Serializable, false otherwise
     */
    public static boolean isValidSerializableType(Class serClass) {

        if (java.io.Serializable.class.isAssignableFrom(serClass))
           return true;
        else
           return false;

        /**
        // This complex logic is replaced by the above simple logic
	Class c = serClass;
	boolean validInterface = false;
	boolean badOne = false;
	// The class must implement the java.io.Serializable interface, either
	// directly or indirectly, 
	// walk up the class tree

	if (c.getName().equals("java.io.Serializable")) {
	    validInterface = true;
	    return validInterface;
	}
	do {	    
	    Class[] interfaces = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		if (interfaces[i].getName().equals("java.io.Serializable")) {
		    validInterface = true;
		    break;
		} else {
		    // walk up the class tree of the interface and see if it
		    // implements java.io.Serializable
		    Class superClass = interfaces[i];
		    do {
			if (superClass.getName().equals("java.io.Serializable")) {
			    validInterface = true;
			    break;
			}
		    } while ((((superClass=superClass.getSuperclass()) != null) && (!validInterface)));
		}
	    }
	} while ((((c=c.getSuperclass()) != null) && (!validInterface)));


	if (validInterface) {
	    return true;
	} else {
	    return false;
	}
       **/
    }



    /** 
     * Method application exception checked for compliance test.
     *
     * Verify the following:
     *
     *   An application exception is an exception defined in the throws clause of 
     *   a method in the Bean's home interface, other than java.rmi.RemoteException.
     *   An application exception must not be defined as a subclass of the
     *   java.lang.RuntimeException, or of the java.rmi.RemoteException. These are
     *   reserved for system exceptions.
     *   The javax.ejb.CreateException, javax.ejb.RemoveException,
     *   javax.ejb.FinderException, and subclasses thereof, are considered to be
     *   application exceptions.
     *
     * @param methodExceptions the exceptions to be checked for throws
     *        application exception
     *
     * @return <code>boolean</code> true if exceptions are valid application exception, false otherwise
     */
    public static boolean isValidApplicationException(Class [] methodExceptions) {
	for (int kk = 0; kk < methodExceptions.length; ++kk) {
	 Class ex=methodExceptions[kk];
	 //check 0: app exception set is all exceptions minus java.rmi.RemoteException.
         if (java.rmi.RemoteException.class != ex) {
	 	//check 1: app exception must subclass java.lang.Exception
	 	if (!java.lang.Exception.class.isAssignableFrom(ex)) return false;
	 	//check 2: app exception must not subclass java.lang.RuntimeException or java.rmi.RemoteException
            	if(java.rmi.RemoteException.class.isAssignableFrom(ex) || java.lang.RuntimeException.class.isAssignableFrom(ex)) {
                	return false;
            	}
         }
	}
	return true;
    }
}
