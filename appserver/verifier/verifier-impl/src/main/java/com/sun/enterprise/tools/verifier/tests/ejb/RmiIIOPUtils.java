/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import java.lang.reflect.*;
import java.util.*;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.*;
import java.lang.ClassLoader;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;

/** 
 * Interface, parameters, return type, and exceptions checked for RMI-IIOP 
 *  compliance test.
 * 
 */
public class RmiIIOPUtils { 



    /** 
     * Interface checked for RMI-IIOP compliance test.
     * Verify the following:
     *
     *   The remote interface is allowed to have superinterfaces. Use of interface
     *   inheritance is subject to the RMI-IIOP rules for the definition of remote
     *   interfaces.
     * 
     *  Verify the following:
     *
     *  An RMI remote interface defines a Java interface that can be invoked 
     *  remotely. A Java interface is a conforming RMI/IDL remote interface if: 
     *
     *  1. The interface is or inherits from java.rmi.Remote either directly or 
     *     indirectly. 
     * 
     *  2. All methods in the interface are defined to throw 
     *     java.rmi.RemoteException or a superclass of java.rmi.RemoteException. 
     *     Throughout this section, references to methods in the interface include 
     *     methods in any inherited interfaces. 
     *
     *  3. Method arguments and results may be of any types. However at run-time, 
     *     the actual values passed as arguments or returned as results must be 
     *     conforming RMI/IDL types (see "Overview of Conforming RMI/IDL Types" on 
     *     page 28-2). In addition, for each RMI/IDL remote interface reference, 
     *     the actual value passed or returned must be either a stub object or a 
     *     remote interface implementation object (see "Stubs and remote 
     *     implementation classes" on page 28-4). 
     *
     *  4. All checked exception classes used in method declarations (other than 
     *     java.rmi.RemoteException and its subclasses) are conforming RMI/IDL 
     *     exception types (see "RMI/IDL Exception Types" on page 28-5)1 
     *
     *  5. Method names may be overloaded. However, when an interface directly 
     *     inherits from several base interfaces, it is forbidden for there to be 
     *     method name conflicts between the inherited interfaces. This outlaws the
     *     case where an interface A defines a method "foo," an interface B also 
     *     defines a method "foo," and an interface C tries to inherit from both A 
     *     and B. 
     *
     *  6. Constant definitions in the form of interface variables are permitted. 
     *     The constant value must be a compile-time constant of one of the RMI/IDL
     *     primitive types or String. 
     *
     *  7. Method and constant names must not cause name collisions when mapped to 
     *     IDL (see "Names that would cause OMG IDL name collisions" on page 28-9). 
     *
     *     The following is an example of a conforming RMI/IDL interface definition:
     *        // Java 
     *        public interface Wombat extends java.rmi.Remote { 
     *          String BLEAT_CONSTANT = "bleat"; 
     *          boolean bleat(Wombat other) throws java.rmi.RemoteException; 
     *        }
     *
     *     While the following is an example of a non-conforming RMI/IDL interface:
     *        // Java 
     *        // IllegalInterface fails to extend Remote!! 
     *        public interface IllegalInterface { 
     *          // illegalExceptions fails to throw RemoteException. 
     *          void illegalExceptions(); 
     *        }
     *
     * @param RMIIIOPinterface the Interface to be checked for Rmi-IIOP compliance
     *
     * @return <code>boolean</code> true if RMIIIOPinterface is valid RMIIIOP interface, false otherwise
     */
    public static boolean isValidRmiIIOPInterface(Class RMIIIOPinterface) {

	// 1. The interface is or inherits from java.rmi.Remote either directly or
	//     indirectly.
	boolean validInterface = false;                     
	Class c = RMIIIOPinterface;
	// walk up the class tree
	do {
	    if (RMIIIOPinterface.getName().equals("java.rmi.Remote")) {
		validInterface = true;
		break;
	    } else {
		// walk up the class tree of the interface and see if it
		// inherits from java.rmi.Remote either directly or indirectly.
		Class[] interfaces = RMIIIOPinterface.getInterfaces();
		//Class interfaces = RMIIIOPinterface.getSuperclass();
		for (int i = 0; i < interfaces.length; i++) {

		    //if (interfaces[i].getName().equals("java.rmi.Remote")) {
		    if ((interfaces[i].getName().equals("java.rmi.Remote")) ||
			//hack until i can ask hans why loop doesn't continue up past
			// javax.ejb.EJBHome
			(interfaces[i].getName().equals("javax.ejb.EJBObject")) ||
			(interfaces[i].getName().equals("javax.ejb.EJBHome"))) {
			validInterface = true;
			break;
		    }
		    else if (isValidRmiIIOPInterface(interfaces[i])) {
			return true;
		    }
		}
	    }
	} while ((((RMIIIOPinterface=RMIIIOPinterface.getSuperclass()) != null) && (!validInterface)));
     
        if (validInterface) {
            return true;
        } else {
            return false;
        }
    }





    /**
     * Interface checked for RMI-IIOP compliance test.
     *
     * @param RMIIIOPinterface the Interface to be checked for Rmi-IIOP compliance
     *
     * @return <code>boolean</code> true if RMIIIOPinterface is valid RMIIIOP interface, false otherwise
     */
    public static boolean isValidRmiIIOPInterfaceMethods(Class RMIIIOPinterface) {

        Class c = RMIIIOPinterface;
	    // continue on and check next condition

	    // All methods in the interface are defined to throw
	    // java.rmi.RemoteException or a superclass of java.rmi.RemoteException.
	    // Throughout this section, references to methods in the interface include
	    // methods in any inherited interfaces.
            try {
	        Method methods[] = c.getDeclaredMethods();
	        Class [] methodExceptionTypes;
	        for (int i=0; i< methods.length; i++) {
		    // The methods exceptions types must be legal types for
		    // RMI-IIOP.  This means that their exception values must
		    // throw java.rmi.RemoteException
		    methodExceptionTypes = methods[i].getExceptionTypes();
		    if (!EjbUtils.isValidRemoteException(methodExceptionTypes)) {
       		        return false;
		    } else { 
		        continue; 
		    }
		}
 
	    // made it throw all methods thowing java.rmi.RemoteException check,
	    // without returning false, continue on...

	    // Method arguments and results may be of any types. However at run-time,
	    // the actual values passed as arguments or returned as results must be
	    // conforming RMI/IDL types (see "Overview of Conforming RMI/IDL Types" on
	    // page 28-2). 
	    // can't check anything here, since this is a run-time check

	    // All checked exception classes used in method declarations (other than
	    // java.rmi.RemoteException and its subclasses) are conforming RMI/IDL
	    // exception types (see "RMI/IDL Exception Types" on page 28-5)1
	    for (int i=0; i < methods.length; i++) {
		    methodExceptionTypes = methods[i].getExceptionTypes();
		    if (!isValidRmiIIOPException(methodExceptionTypes)) {
			return false;
		    } else { 
			continue; 
		    }
		}
      
	    // Method names may be overloaded. However, when an interface directly
	    //inherits from several base interfaces, it is forbidden for there to be
	    //method name conflicts between the inherited interfaces. This outlaws the
	    //case where an interface A defines a method "foo," an interface B also
	    //defines a method "foo," and an interface C tries to inherit from both A
	    //and B.
	    // can't check anything here, since this is check cannot be determined
	    // thru reflection api checking

	    // Constant definitions in the form of interface variables are permitted.
	    //The constant value must be a compile-time constant of one of the RMI/IDL
	    // primitive types or String.
    
	    Field fields[] = c.getFields();
	    for (int i=0; i< fields.length; i++) {
		    // The fields types must be a compile-time constant of one of the 
		    // RMI/IDL primitive types or String
		    if (!(isValidRmiIIOPField(fields[i]))) {
			return false;
		    } else { 
			continue; 
		    }
		}
 
	    // Method and constant names must not cause name collisions when mapped to
	    // IDL (see "Names that would cause OMG IDL name collisions" on page 28-9)
	    // can't check anything here, since this is an non-exhaustive search and
	    // compare, don't know all the various combinations that would cause
	    // name collisions, 
            } catch (Throwable t) {
                Verifier.debug(t);
                return false;
            }
     return true;
    } 

    /**
     * Method parameters checked for RMI-IIOP compliance test.
     * Verify the following:
     *
     *   The home/remote interface methods arguments types must be legal types for
     *   RMI-IIOP.  This includes primitives, value types, and interfaces.  This 
     *   means that their arguments must be of valid types for RMI-IIOP.
     *
     *   28.2.4 RMI/IDL Value Types 
     *    An RMI/IDL value type represents a class whose values can be moved 
     *    between systems. So rather than transmitting a reference between systems,
     *    the actual state of the object is transmitted between systems. This 
     *    requires that the receiving system have an analogous class that can be 
     *    used to hold the received value. Value types may be passed as arguments 
     *    or results of remote methods, or as fields within other objects that are 
     *    passed remotely. A Java class is a conforming RMI/IDL value type if the 
     *    following applies: 
     *  
     *      1. The class must implement the java.io.Serializable interface, either 
     *         directly or indirectly, and must be serializable at run-time. It may
     *         serialize references to other RMI/IDL types, including value types 
     *         and remote interfaces. 
     *      2. The class may implement java.io.Externalizable. (This indicates it 
     *         overrides some of the standard serialization machinery.) 
     *      3. If the class is a non-static inner class, then its containing class 
     *         must also be a conforming RMI/IDL value type. 
     *      4. A value type must not either directly or indirectly implement the 
     *         java.rmi.Remote interface. (If this were allowed, then there would 
     *         be potential confusion between value types and remote interface 
     *         references.) 
     *      5. A value type may implement any interface except for java.rmi.Remote.
     *      6. There are no restrictions on the method signatures for a value type.
     *      7. There are no restrictions on static fields for a value type. 
     *      8. There are no restrictions on transient fields for a value type. 
     *      9. Method, constant and field names must not cause name collisions when
     *         mapped to IDL (see "Names that would cause OMG IDL name collisions" 
     *         on page 28-9). 
     *         
     *         Here is an example of a conforming RMI/IDL value type: 
     *           // Java 
     *           public class Point implements java.io.Serializable { 
     *             public final static int CONSTANT_FOO = 3+3; 
     *             private int x; 
     *             private int y; 
     *             public Point(int x, y) { ... } 
     *             public int getX() { ... } 
     *             public int getY() { ... } 
     *           } 
     *  
     *     28.2.4.1 The Java String Type 
     *     The java.lang.String class is a conforming RMI/IDL value type following 
     *     these rules. Note, however, that String is handled specially when mapping
     *     Java to OMG IDL (see "Mapping for java.lang.String" on page 28-18).
     *
     * @param RMIIIOPparams the params to be checked for Rmi-IIOP compliance
     *
     * @return <code>boolean</code> true if RMIIIOPParams are valid RMIIIOP parameters, false otherwise
     */
    public static boolean isValidRmiIIOPParameters(Class [] RMIIIOPparams) {
	if (RMIIIOPparams.length > 0) {
	    for (int ii = 0; ii < RMIIIOPparams.length; ii++) {
		Class c = RMIIIOPparams[ii];

		// if it's not a primitve, or
		// if it's not a valid rmi-idl type, or
		// if it's not java.lang.String, return false
		if (!(isValidRmiIDLPrimitiveType(c)) &&
		    !(isValidRmiIIOPValueType(c)) &&
		    !(isValidRmiIIOPInterfaceType(c)) &&
		    !(isJavaLangStringType(c)) &&
		    !(isValidRmiIIOPException(RMIIIOPparams)) &&
		    !(c.getName().equals("java.lang.Object")) &&
		    !(isValidRmiIIOPCORBAObjectType(c)) &&
		    !(isValidRmiIIOPIDLEntityType(c))) {
		    //exception,corba object,array,idl entity type
		    return false;
		}
	    }
	    // if you made it thru loop without returning false, then you 
	    // passed the tests, return true
	    return true;
	} else {
	    return true;
	}
    }


    /** 
     * Class checked for RMI-IIOP value type compliance test.
     * Verify the following:
     *
     *   This class is proper CORBA Object type.
     *
     * @param RMIIIOPvaluetype the class to be checked 
     *
     * @return <code>boolean</code> true if RMIIIOPvaluetype is valid Rmi-IIOP value type, false otherwise
     */
    public static boolean isValidRmiIIOPCORBAObjectType(Class RMIIIOPvaluetype) {

	Class c = RMIIIOPvaluetype;
	boolean validInterface = false;

	do {
	    Class[] interfaces = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		if (interfaces[i].getName().equals("org.omg.CORBA.Object")) {
		    validInterface = true;
		    break;
		} else {
		    // walk up the class tree of the interface and see if it
		    // implements org.omg.CORBA.Object
		    Class superClass = interfaces[i];
		    do {
			if (superClass.getName().equals("org.omg.CORBA.Object")) {
			    validInterface = true;
			    break;
			}
		    } while ((((superClass=superClass.getSuperclass()) != null) && (!validInterface)));
		}
	    }
	} while ((((c=c.getSuperclass()) != null) && (!validInterface)));
	if (!validInterface) {
	    return false;
	} else {
	    return true;
	}
    }

  /** 
     * Class checked for RMI-IIOP value type compliance test.
     * Verify the following:
     *
     *   This class is proper Java IDL Entity type.
     *
     * @param RMIIIOPvaluetype the class to be checked 
     *
     * @return <code>boolean</code> true if RMIIIOPvaluetype is valid Rmi-IIOP value type, false otherwise
     */
    public static boolean isValidRmiIIOPIDLEntityType(Class RMIIIOPvaluetype) {

	Class c = RMIIIOPvaluetype;
	boolean validInterface = false;

	do {
	    Class[] interfaces = c.getInterfaces();
	    for (int i = 0; i < interfaces.length; i++) {
		if (interfaces[i].getName().equals("org.omg.CORBA.portable.IDLEntity")) {
		    validInterface = true;
		    break;
		} else {
		    // walk up the class tree of the interface and see if it
		    // implements java.io.Serializable
		    Class superClass = interfaces[i];
		    do {
			if (superClass.getName().equals("org.omg.CORBA.portable.IDLEntity")) {
			    validInterface = true;
			    break;
			}
		    } while ((((superClass=superClass.getSuperclass()) != null) && (!validInterface)));
		}
	    }
	} while ((((c=c.getSuperclass()) != null) && (!validInterface)));
	if (!validInterface) {
	    return false;
	} else {
	    return true;
	}
    }

    /** 
     * Class checked for RMI-IIOP value type compliance test.
     * Verify the following:
     *
     *   This class s proper value types.
     *
     * @param RMIIIOPvaluetype the class to be checked for Rmi-IIOP value type
     *        compliance
     *
     * @return <code>boolean</code> true if RMIIIOPvaluetype is valid Rmi-IIOP value type, false otherwise
     */
    public static boolean isValidRmiIIOPValueType(Class RMIIIOPvaluetype) {

		Class c = RMIIIOPvaluetype;
		boolean validInterface = false;
		boolean badOne = false;
		// The class must implement the java.io.Serializable interface, either
		// directly or indirectly, and must be serializable at run-time. It may
		// serialize references to other RMI/IDL types, including value types
		// and remote interfaces.
		// walk up the class tree
		if (c.getName().equals("java.lang.Object")) {
			//validInterface = true;
			return true;
		}
		/* Buggy Code
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
		    } while ((((superClass=superClass.getSuperclass()) != null) && (validInterface == false)));
		}
	    }
	} while ((((c=c.getSuperclass()) != null) && (validInterface == false)));
        */
		validInterface = java.io.Serializable.class.isAssignableFrom(c);

		if (validInterface == false) {
			return false;
		} else {
			// 2. The class may implement java.io.Externalizable. (This indicates it
			// overrides some of the standard serialization machinery.)
			// nothing to check for here, since the keyword is "may implement"


			//  3. If the class is a non-static inner class, then its containing class
			//         must also be a conforming RMI/IDL value type.
			// don't know if this can be checked statically

			// reset class c since it may have gotten moved in the above do/while loop
            /* Buggy Code
	    c = RMIIIOPvaluetype;

	    do {
		Class[] interfaces = c.getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
		    if (interfaces[i].getName().equals("java.rmi.Remote")) {
			badOne = true;
			break;
		    }
		}
	    } while ((((c=c.getSuperclass()) != null) && (!badOne)));
            */

			badOne = java.rmi.Remote.class.isAssignableFrom(c);

			if (badOne) {
				return false;
			}

			// 5. A value type may implement any interface except for java.rmi.Remote.
			// already checked this in step #4 above

			// 6. There are no restrictions on the method signatures for a value type.
			// 7. There are no restrictions on static fields for a value type.
			// 8. There are no restrictions on transient fields for a value type.
			// no checking need be done for these 6, 7, & 8

			// 9. Method, constant and field names must not cause name collisions when
			// mapped to IDL (see "Names that would cause OMG IDL name collisions"
			// on page 28-9).
			// can't check anything here, since this is an non-exhaustive search and
			// compare, don't know all the various combinations that would cause
			// name collisions, ask hans to be sure
			return true;

		}
	}


    /** 
     * Constant definitions in the form of interface variables are permitted test.
     * The constant value must be a compile-time constant of one of the RMI/IDL 
     * primitive types or String.
     *
     * Verify the following:
     *
     *   The home/remote interface field types must be legal types for
     *   RMI-IIOP.  
     *   This means that the constant value must be a compile-time constant 
     *   of one of the RMI/IDL primitive types or String.
     *
     * @param RMIIIOPField the field to be checked for Rmi-IIOP compile-time 
     *        constant of one of the RMI/IDL primitive types or String
     *
     * @return <code>boolean</code> true if RMIIIOPField is valid Rmi-IIOP type, false otherwise
     */
    public static boolean isValidRmiIIOPField(Field RMIIIOPField) {
	boolean validPrimitiveType = false;
	if ((isValidRmiIDLPrimitiveType(RMIIIOPField)) ||
	    (RMIIIOPField.getType().equals(java.lang.String.class))) {
	    validPrimitiveType = true;
	}
	return validPrimitiveType;
    }



    /** 
     * Constant definitions in the form of interface variables are permitted test.
     * The constant value must be a compile-time constant of one of the RMI/IDL 
     * primitive types .
     *
     * Verify the following:
     *
     *   The home/remote interface field types must be legal types for
     *   RMI-IIOP.  
     *   This means that the constant value must be a compile-time constant 
     *   of one of the RMI/IDL primitive types .
     *
     * @param RMIIIOPField the field to be checked for Rmi-IIOP compile-time 
     *        constant of one of the RMI/IDL primitive types or String
     *
     * @return <code>boolean</code> true if RMIIIOPField is valid  compile-time constant of
     *                              one of the RMI/IDL primitive types, false otherwise
     */
    public static boolean isValidRmiIDLPrimitiveType(Field RMIIIOPField) {
	boolean validPrimitiveType = false;
	if ((RMIIIOPField.getType().getName().equals("void")) ||
	    (RMIIIOPField.getType().getName().equals("boolean")) ||
	    (RMIIIOPField.getType().getName().equals("byte")) ||
	    (RMIIIOPField.getType().getName().equals("char")) ||
	    (RMIIIOPField.getType().getName().equals("short")) ||
	    (RMIIIOPField.getType().getName().equals("int")) ||
	    (RMIIIOPField.getType().getName().equals("long")) ||
	    (RMIIIOPField.getType().getName().equals("float")) ||
	    (RMIIIOPField.getType().getName().equals("double")))  {
	    validPrimitiveType = true;
	}
	return validPrimitiveType;
    }


    /** Class is interface test.
     *  The class value must be a java interface type .
     *
     * Verify the following:
     *
     *   The home/remote interface method params types must be legal types for
     *   RMI-IIOP.  
     *  The class value must be a java interface type .
     *
     * @param interfaceClass the class to be checked for java interface class
     *
     * @return <code>boolean</code> true if interfaceClass is legal types for
     *                              RMI-IIOP.  false otherwise
     */
    private static boolean isValidRmiIIOPInterfaceType(Class interfaceClass) {
	if (interfaceClass.isInterface()) {
	    return true;
	} else {
	    return false;
	}
    }


    /** Class is primitve test.
     *  The class value must be a java primitive type .
     *
     * Verify the following:
     *
     *   The home/remote interface method params types must be legal types for
     *   RMI-IIOP.  
     *  The class value must be a java primitive type .
     *
     * @param primitiveClass the class to be checked for java primitive class
     *
     * @return <code>boolean</code> true if primitiveClass is legal Java primitive type, false otherwise
     */
    public static boolean isValidRmiIDLPrimitiveType(Class primitiveClass) {
	boolean validPrimitiveType = false;
	if ((primitiveClass.getName().equals("void")) ||
	    (primitiveClass.getName().equals("boolean")) ||
	    (primitiveClass.getName().equals("byte")) ||
	    (primitiveClass.getName().equals("char")) ||
	    (primitiveClass.getName().equals("short")) ||
	    (primitiveClass.getName().equals("int")) ||
	    (primitiveClass.getName().equals("long")) ||
	    (primitiveClass.getName().equals("float")) ||
	    (primitiveClass.getName().equals("double")))  {
	    validPrimitiveType = true;
	}
	return validPrimitiveType;
    }


    /** 
     * Class is java.lang.String test.
     * The class value may be java.lang.String type .
     *
     * Verify the following:
     *
     *   The home/remote interface method params types must be legal types for
     *   RMI-IIOP.  
     *   The class value may be java.lang.String type .
     *
     * @param jlsClass the class to be checked for java primitive class
     *
     * @return <code>boolean</code> true if jlsClass is java.lang.String type, false otherwise
     */
    public static boolean isJavaLangStringType(Class jlsClass) { 
	boolean validJlsType = false;
	if (jlsClass.getName().equals("java.lang.String")) {
	    validJlsType = true;
	}
	return validJlsType;
    }

    /** 
     * Method exception checked for RMI-IIOP compliance test.
     * Verify the following:
     *
     *   The home/remote interface methods exception types must be legal types for
     *   RMI-IIOP.  This includes primitives, value types, and interfaces.
     *   This means that their exception must throw java.rmi.RemoteException.
     *
     *   28.2.6 RMI/IDL Exception Types An RMI/IDL exception type is a checked 
     *   exception class (as defined by the Java Language Specification). Since 
     *   checked exception classes extend java.lang.Throwable which implements 
     *   java.io.Serializable, it is unnecessary for an RMI/IDL exception class to 
     *   directly implement java.io.Serializable. A type is a conforming RMI/IDL 
     *   exception if the class:
     *     - is a checked exception class. 
     *     - meets the requirements for RMI/IDL value types defined in 
     *       "RMI/IDL Value Types" on page 28-4.
     *
     * @param RMIIIOPexceptions the exceptions to be checked for Rmi-IIOP throws
     *        java.rmi.RemoteException 
     *
     * @return <code>boolean</code> true if RMIIIOPexceptions are legal type for RMI-IIOP, false otherwise
     */
    public static boolean isValidRmiIIOPException(Class [] RMIIIOPexceptions) {
	// methods must throw java.rmi.RemoteException
	boolean throwsRemoteException = false;
	for (int kk = 0; kk < RMIIIOPexceptions.length; ++kk) {
	    if ((RMIIIOPexceptions[kk].getName().equals("java.rmi.RemoteException")) ||
		(RMIIIOPexceptions[kk].getName().equals("RemoteException"))) {
		throwsRemoteException = true;
		break;
	    }
	}
	return throwsRemoteException;
    }

    /**
     * Method return type checked for RMI-IIOP compliance test.
     * Verify the following:
     *
     *   The home/remote interface methods return type must be legal types for
     *   RMI-IIOP.  This includes primitives, value types, and interfaces.
     *   This means that their return type must be of valid types for RMI-IIOP.
     *
     * @param RMIIIOPparams the return type to be checked for Rmi-IIOP compliance
     *
     * @return <code>boolean</code> true if RMIIIOPReturnType is legal type for RMI-IIOP, false otherwise
     */
    public static boolean isValidRmiIIOPReturnType(Class RMIIIOPReturnType) {
	// if it's not a primitve, or
	// if it's not a valid rmi-idl type, or
	// if it's not java.lang.String, return false
	if (!((isValidRmiIDLPrimitiveType(RMIIIOPReturnType)) ||
	      (isValidRmiIIOPValueType(RMIIIOPReturnType)) ||
	      (isValidRmiIIOPInterfaceType(RMIIIOPReturnType)) ||
	      (isJavaLangStringType(RMIIIOPReturnType)) ||
	      //(isValidRmiIIOPException(RMIIIOPparams)) ||
	      (isValidRmiIIOPCORBAObjectType(RMIIIOPReturnType)) ||
	      (RMIIIOPReturnType.getName().equals("java.lang.Object")) ||
	      (isValidRmiIIOPIDLEntityType(RMIIIOPReturnType)))) {
	    return false;
	} else {
	    return true;
	}
    }


    /** 
     * Class checked for Serializable value type compliance test.
     * Verify the following:
     *
     *   This class is a serializable class.
     *
     * @param serializableClass the class to be checked for serializable
     *        compliance
     *
     * @return <code>boolean</code> true if c is a serializable class, false otherwise
     */
    public static boolean isValidSerializableType(Class c) {

       return java.io.Serializable.class.isAssignableFrom(c);

       /* Buggy Code
	boolean validInterface = false;

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

	// The class must implement the java.io.Serializable interface, either
	// directly or indirectly, and must be serializable at run-time. 
	// walk up the class tree

	if (!validInterface) {
	    return false;
	} else {
	    return true;
	}
        */
    }


    /**
     * Container managed fields checked for one of the following: 
     * Java primitive types, Java serializable types, or references to 
     * enterprise beans' remote or home interfaces.
     *
     * Verify the following:
     *
     * Container managed fields checked for one of the following: 
     * Java primitive types, Java serializable types, or references to 
     * enterprise beans' remote or home interfaces.
     *
     * All the standard Java primitive types are supported as part of RMI/IDL. 
     *  These are:  void, boolean, byte, char, short, int, long, float, double
     *
     * @param CmpField params to be checked for CmpField valid type compliance
     * @param HomeClass home class
     * @param RemoteClass remote class
     *
     * @return <code>boolean</code> true if CmpField is Java primitive type,
     *                              Java serializable types, or references to
     *                              enterprise beans' remote or home interfaces.,
     *                              false otherwise
     */
    public static boolean isPersistentFieldTypeValid(Class CmpField, String HomeClass, String RemoteClass) {
	// if Java primitive types, Java serializable types, or references to 
	// enterprise beans' remote or home interfaces.
	if (!((isValidRmiIDLPrimitiveType(CmpField)) ||
	      (isValidSerializableType(CmpField)) ||
	      (CmpField.getName().equals(HomeClass)) ||
	      (CmpField.getName().equals(RemoteClass)))) {
	    return false;
	} else {
	    return true;
	}

    }


    /**
     * The ejbFind<METHOD> exceptions of Bean clas smust be a subset of the names
     * of the exceptions defined in the throws clause of the matching find method
     * of the home interface.  
     *
     * Verify the following:
     *
     * All the exceptions defined in the throws clause of the
     * matching ejbFind method of the
     * enterprise Bean class must be included in the throws
     * clause of the matching find method of the home interface
     * this home interface find method must define a superset of all the
     * exceptions thrown in the ejbFind method of the bean class                    
     * so there may not be a 1-1 mapping of exceptions
     * also, for all ejbFind/find combo's any unchecked
     * exceptions thrown by the ejbFind<METHOD> in the bean
     * class doesn't need to be thrown in the corresponding
     * find<METHOD> of the home interface , these unchecked
     * exceptions "subclass of RuntimeException" i.e
     * out of memory exception are handled by the container,
     * who throws a Runtime exception to the appropriate
     * instance/object
     *
     * @param ejbFindExceptions the ejbFind<METHOD> exceptions to be checked for 
     *        containment within the names of the exceptions defined in the throws 
     *        clause of the matching find method of the home interface (i.e subset)
     * @param findExceptions the find<METHOD> exceptions to be checked for 
     *        containing the names of the exceptions defined in the throws 
     *        clause of the matching ejbFind<METHOD> method of the bean class
     *        (i.e. superset)
     *
     * @return <code>boolean</code> true if ejbFindExceptions is subset of findExceptions,
     *                              false otherwise
     */
    public static boolean isEjbFindMethodExceptionsSubsetOfFindMethodExceptions
	(Class [] ejbFindExceptions, Class [] findExceptions) {
	boolean oneFailed = false;
	if (Arrays.equals(ejbFindExceptions,findExceptions)) {
	    return true;
	} else {
	    // manipulate as list, and use list.contains() method
	    List ejbFindList = Arrays.asList(ejbFindExceptions);
	    List findList = Arrays.asList(findExceptions);
	    if (!ejbFindList.isEmpty()) {
		for (Iterator itr = ejbFindList.iterator(); itr.hasNext();) {
		    Class nextEjbFindMethodException = (Class) itr.next();
		    if (findList.contains(nextEjbFindMethodException)) {
			continue;
		    } else {
			// also, for all ejbFind/find combo's any unchecked
			// exceptions thrown by the ejbFind<METHOD> in the bean
			// class doesn't need to be thrown in the corresponding
			// find<METHOD> of the home interface , these unchecked
			// exceptions "subclass of RuntimeException" i.e
			// out of memory exception are handled by the container,
			// who throws a Runtime exception to the appropriate
			// instance/object
			if (isSuperClassofClass("java.lang.RuntimeException",
						nextEjbFindMethodException)) {
			    // ignore this particular unchecked exception, since it stems
			    // from RuntimeException, we can ignore it
			    continue;
			} else {
			    // okay, that's it, now we know ejbFind<METHOD> exception
			    // is not defined in the find<METHOD> in the home interface
			    // test must fail
			    oneFailed = true;
			    // break out after first failure, however, no precise feedback
			    // to caller as to exactly which exception is causing problem
			    // which we happen to know at this point in time
			    break;
			}
		    }
		}
		
		// if we get thru all of 'em, and oneFailed is set, then we flunked test
		if (oneFailed) {
		    return false;
		} else {
		    // we know we never set oneFailed in the above loop, so either all
		    // ejbFind<METHOD> exceptions are defined in the superset find<METHOD>
		    // exceptions, or they are unchecked exceptions, which we can ignore
		    // so set test to passed
		    return true;
		}
	    } else {
		// ejbFind<METHOD> exceptions list is empty, pass test
		return true;
	    }
	}
    }


    /**
     * Find superClass of specified class.
     *
     * @param superClass the specifed super Class to be checked against  
     *                   for containment of
     * @param fromClass the class which you are trying to find the parent super 
     *                  class of
     *
     * @return <code>boolean</code> true if superClass is parent class of fromClass, false otherwise
     */
    private static boolean isSuperClassofClass(String superClass, Class fromClass) {
	boolean validSuperClass = false;
	Class c = fromClass;
	// walk up the class tree
	do {
	    if (c.getName().equals(superClass)) {
		validSuperClass = true;
		break;
	    }
	} while ((((c=c.getSuperclass()) != null) && (!validSuperClass)));

	if (!validSuperClass){
	    return false;
	} else {
	    return true;
	}



    }


} 
