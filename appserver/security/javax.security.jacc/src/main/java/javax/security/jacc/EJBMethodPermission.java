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

package javax.security.jacc;

import java.security.*;
import java.lang.reflect.*;

import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectStreamField;


/**
 * Class for EJB method permissions.
 * <P>
 * The name of an EJBMethodPermission contains the value of the
 * ejb-name element in the application's deployment descriptor
 * that identifies the target EJB.
 * <P>
 * The actions of an EJBMethodPermission identifies the methods of
 * the EJB to which the permission applies.
 * <P> 
 * Implementations of this class MAY implement newPermissionCollection or
 * inherit its implementation from the super class.
 *
 * @see java.security.Permission
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 *
 */

public final class EJBMethodPermission extends Permission
implements java.io.Serializable
{

    private static final String interfaceKeys[] =
         { "Local", "LocalHome", "Remote", "Home", "ServiceEndpoint" };

    private static HashMap interfaceHash = new HashMap();
    static {
	for (int i=0; i<interfaceKeys.length; i++) 
	    interfaceHash.put(interfaceKeys[i], new Integer(i));
    };

    private transient int methodInterface;

    private transient String otherMethodInterface = null;

    private transient String methodName;

    private transient String methodParams;

    private transient String actions;

    private transient int hashCodeValue = 0;

    private static final long serialVersionUID = 1L;

   /**
    * The serialized fields of this permission are defined below. Whether
    * or not the serialized fields correspond to actual (private) fields 
    * is an implementation decision.
    * @serialField actions String
    * the canonicalized actions string (as returned by getActions).
    */
    private static final ObjectStreamField[] serialPersistentFields = { 
        new ObjectStreamField("actions", java.lang.String.class)
    };

   /**
    * Creates a new EJBMethodPermission with the specified name and actions.
    * <P>
    * The name contains the value of the ejb-name element corresponding
    * to an EJB in the application's deployment descriptor.
    * <P>
    * The actions contains a methodSpec. The syntax of the actions parameter
    * is defined as follows:
    * <P><Pre>
    *      methodNameSpec ::= methodName | emptyString
    *
    *      methodInterfaceName ::= String
    *
    *      methodInterfaceSpec ::= methodInterfaceName | emptyString
    *
    *      typeName ::= typeName | typeName []
    *
    *      methodParams ::= typeName | methodParams comma typeName
    *
    *      methodParamsSpec ::= emptyString | methodParams
    *
    *      methodSpec ::= null |
    *           methodNameSpec |
    *           methodNameSpec comma methodInterfaceName |
    *           methodNameSpec comma methodInterfaceSpec comma methodParamsSpec
    * </Pre><P>
    * A MethodInterfaceName is a non-empty String and should contain a 
    * method-intf value as defined for use in EJB deployment descriptors.
    * An implementation must be flexible such that it supports additional
    * interface names especially if they are standardized by the EJB 
    * Specification. The EJB Specification currently defines the following
    * method-intf values:
    * <P><Pre>
    *       { "Home", "LocalHome", "Remote", "Local", "ServiceEndpoint" }
    * </Pre><P>
    * A null or empty string methodSpec indicates that the permission applies
    * to all methods of the EJB. A methodSpec with a methodNameSpec of the
    * empty string matches all methods of the EJB that match the
    * methodInterface and methodParams elements of the methodSpec.
    * <P>
    * A methodSpec with a methodInterfaceSpec of the
    * empty string matches all methods of the EJB that match the 
    * methodNameSpec and methodParamsSpec elements of the methodSpec.
    * <P>
    * A methodSpec without a methodParamsSpec matches all methods
    * of the EJB that match the methodNameSpec and methodInterface elements
    * of the methodSpec.
    * <P>
    * The order of the typeNames in methodParams array must match
    * the order of occurence of the corresponding parameters in the method 
    * signature of the target method(s). Each typeName in the methodParams 
    * must contain the canonical form of the corresponding parameter's typeName 
    * as defined by the getActions method. A methodSpec with
    * an empty methodParamsSpec matches all 0 argument methods of the
    * EJB that match the methodNameSpec and methodInterfaceSpec elements of
    * the methodSpec.
    * <P>
    * @param name of the EJB to which the permission pertains.
    * <P>
    * @param actions identifies the methods of the EJB to which the
    * permission pertains.
    */
    
    public EJBMethodPermission(String name, String actions)
    {
	super(name);
	setMethodSpec(actions);
    }

   /**
    * Creates a new EJBMethodPermission with name corresponding to
    * the EJBName and actions composed from methodName, methodInterface,
    * and methodParams.
    * <P>
    * @param EJBName The string representation of the name of the EJB as it
    * appears in the corresponding ejb-name element in the deployment
    * descriptor.
    * <P>
    * @param methodName A string that may be used to indicate the method of
    * the EJB to which the permission pertains. A value of null or ""
    * indicates that the permission pertains to all methods that
    * match the other parameters of the permission specification
    * without consideration of method name.
    * <P>
    * @param methodInterface A string that may be used to specify the EJB
    * interface to which the permission pertains. A value of null or "",
    * indicates that the permission pertains to all methods that match the
    * other parameters of the permission specification without consideration
    * of the interface they occur on.
    * <P>
    * @param methodParams An array of strings that may be used to specify
    * (by typeNames) the parameter signature of the target methods. The
    * order of the typeNames in methodParams array must match
    * the order of occurence of the corresponding parameters
    * in the method signature of the target method(s). Each typeName in
    * the methodParams array must contain the canonical form of the 
    * corresponding parameter's typeName as defined by the getActions method. 
    * An empty methodParams array is used to represent
    * a method signature with no arguments. A value of null indicates that
    * the permission pertains to all methods that match the other
    * parameters of the permission specification without consideration
    * of method signature.
    */

    public EJBMethodPermission(String EJBName, String methodName,
	String methodInterface, String[] methodParams)
    {
	super(EJBName);
	setMethodSpec(methodName,methodInterface,methodParams);
    }

   /**
    * Creates a new EJBMethodPermission with name corresponding to the
    * EJBName and actions composed from methodInterface, and the
    * Method object.
    * <P>
    * A container uses this constructor prior to checking if a caller
    * has permission to call the method of an EJB.
    * <P>
    * @param EJBName The string representation of the name of the EJB as it
    * appears in the corresponding ejb-name element in the deployment
    * descriptor.
    * <P>
    * @param methodInterface A string that may be used to specify the EJB
    * interface to which the permission pertains.
    * A value of null or "", indicates that the permission pertains
    * to all methods that match the other parameters of the
    * permission specification without consideration of the
    * interface they occur on.
    * <P>
    * @param method an instance of the Java.lang.reflect.Method class
    * corresponding to the method that the container is trying to determine
    * whether the caller has permission to access. This value must not be
    * null.
    */

    public EJBMethodPermission(String EJBName, String methodInterface,
        Method method)
    {
	super(EJBName);
	setMethodSpec(methodInterface,method);
    }

   /**
    * Checks two EJBMethodPermission objects for equality.
    * EJBMethodPermission objects are equivalent if they have case sensitive
    * equivalent name and actions values.
    * <P>
    * Two Permission objects, P1 and P2, are equivalent if and only if
    * P1.implies(P2) && P2.implies(P1).
    * <P>
    * @param o the EJBMethodPermission object being tested for equality
    * with this EJBMethodPermission
    * <P>
    * @return true if the argument EJBMethodPermission object is equivalent
    * to this EJBMethodPermission.
    */

    public boolean equals(Object o)
    {
	if (o == null || ! (o instanceof EJBMethodPermission)) return false;

	EJBMethodPermission that = (EJBMethodPermission) o;

	if (!this.getName().equals(that.getName())) return false;

	if (this.methodName != null) {
	    if (that.methodName == null || 
		!this.methodName.equals(that.methodName)) return false;
	}
	else if (that.methodName != null) return false;

	if (this.methodInterface != that.methodInterface) return false;

	if (this.methodInterface == -2 &&
	    !this.otherMethodInterface.equals(that.otherMethodInterface))
	    return false;

	if (this.methodParams != null) {
	    if (that.methodParams == null || 
		!this.methodParams.equals(that.methodParams)) return false;
	}
	else if (that.methodParams != null) return false;

	return true;
    }

   /**
    * Returns a String containing a canonical representation of the actions
    * of this EJBMethodPermission. The Canonical form of the actions
    * of an EJBMethodPermission is described by the following syntax
    * description.
    * <P><Pre>
    *      methodNameSpec ::= methodName | emptyString
    *
    *      methodInterfaceName ::= String
    *
    *      methodInterfaceSpec ::= methodInterfaceName | emptyString
    *
    *      typeName ::= typeName | typeName []
    *
    *      methodParams ::= typeName | methodParams comma typeName
    *
    *      methodParamsSpec ::= emptyString | methodParams
    *
    *      methodSpec ::= null |
    *           methodName |
    *           methodNameSpec comma methodInterfaceName |
    *           methodNameSpec comma methodInterfaceSpec comma methodParamsSpec
    * </Pre><P>
    * The canonical form of each typeName must begin with the fully qualified Java 
    * name of the corresponding parameter's type. The canonical form of a typeName 
    * for an array parameter is the fully qualified Java name of the array's component
    * type followed by as many instances of the string "[]" as there are dimensions 
    * to the array. No additional characters (e.g. blanks) may occur in the 
    * canonical form.
    * <P>
    * A MethodInterfaceName is a non-empty String and should contain a 
    * method-intf value as defined for use in EJB deployment descriptors.
    * An implementation must be flexible such that it supports additional
    * interface names especially if they are standardized by the EJB 
    * Specification. The EJB Specification currently defines the following
    * method-intf values:
    * <P><Pre>
    *       { "Home", "LocalHome", "Remote", "Local", "ServiceEndpoint" }
    * </Pre><P>
    * @return a String containing the canonicalized actions of this
    * EJBMethodPermission.
    */

    public String getActions()
    {
	if (this.actions == null) {

	    String iSpec = (this.methodInterface == -1 ? null :
			    (this.methodInterface < 0 ? 
			     this.otherMethodInterface :
			     interfaceKeys[this.methodInterface]));
	    
	    if (this.methodName == null) {
		if (iSpec == null) {
		    if (this.methodParams != null)
			this.actions = "," + this.methodParams;
		}
		else if (this.methodParams == null)
		    this.actions = "," + iSpec; 
		else this.actions = "," + iSpec + this.methodParams;
	    }
	    else if (iSpec == null) { 
		if (this.methodParams == null) this.actions = this.methodName;
		else this.actions = this.methodName + "," + this.methodParams;
	    }
	    else if (this.methodParams == null) {
		this.actions = this.methodName + "," + iSpec; 
	    }
	    else this.actions = this.methodName + "," + iSpec + 
		     this.methodParams;
	}

	return this.actions;
    }

   /**
    * Returns the hash code value for this EJBMethodPermission. The properties
    * of the returned hash code must be as follows: <p>
    * <ul>
    * <li> During the lifetime of a Java application, the hashCode method
    *      must return the same integer value every time it is called on a
    *      EJBMethodPermission object. The value returned by hashCode for a
    *      particular EJBMethodPermission need not remain consistent from
    *      one execution of an application to another.
    * <li> If two EJBMethodPermission objects are equal according to the
    *      equals method, then calling the hashCode method on each of the two
    *      Permission objects must produce the same integer result (within an
    *      application).
    * </ul>
    * <P>
    * @return the integer hash code value for this object.
    */

    public int hashCode()
    {
	if (hashCodeValue == 0) {

	    String hashInput;
	    String actions = this.getActions();

	    if (actions == null) hashInput = this.getName();
	    else hashInput = this.getName() + " " + actions;

	    hashCodeValue = hashInput.hashCode();
	}
	return this.hashCodeValue;
    }

    /**
     * Determines if the argument Permission is "implied by" this
     * EJBMethodPermission. For this to be the case, <p>
     * <ul>
     * <li> The argument must be an instanceof EJBMethodPermission
     * <li> with name equivalent to that of this EJBMethodPermission, and
     * <li> the methods to which the argument permission applies (as defined
     *      in its actions) must be a subset of the methods to which this
     *      EJBMethodPermission applies (as defined in its actions).
     * </ul>
     * <P>
     * The argument permission applies to a subset of the methods to which
     * this permission applies if all of the following conditions are met.
     * <ul>
     * <li> the method name component of the methodNameSpec of this 
     *      permission is null, the empty string, or 
     *      equivalent to the method name of the argument permission, and
     * <li> the method interface component of the methodNameSpec of this 
     *      permission is null, the empty string, or equivalent to the 
     *      method interface of the argument permission, and
     * <li> the method parameter list component of the methodNameSpec
     *      of this permission is null, the empty string, or equivalent
     *      to the method parameter list of the argument permission. 
     * </ul>
     * <P>
     * The name and actions comparisons described above are case sensitive.
     * <P>
     * @param permission "this" EJBMethodPermission is checked to see if
     * it implies the argument permission.
     * <P>
     * @return true if the specified permission is implied by this object,
     * false if not.
     */
    public boolean implies(Permission permission)
    {
	if (permission == null ||
	    ! (permission instanceof EJBMethodPermission)) return false;

	EJBMethodPermission that = (EJBMethodPermission) permission;

	if (!this.getName().equals(that.getName())) return false;
	    
	if (this.methodName != null && 
	    (that.methodName == null || 
	     !this.methodName.equals(that.methodName))) return false;

	if (this.methodInterface != -1 &&
	    (that.methodInterface == -1 ||
	    this.methodInterface != that.methodInterface)) return false;

	if (this.methodInterface == -2 &&
	    !this.otherMethodInterface.equals(that.otherMethodInterface))
	    return false;

	if (this.methodParams != null &&
	    (that.methodParams == null ||
	    !this.methodParams.equals(that.methodParams))) return false;

	return true;
    }

    // ----------------- Private Methods ---------------------

   /**
     * readObject reads the serialized fields from the
     * input stream and uses them to restore the permission. 
     * This method need not be implemented if establishing the 
     * values of the serialized fields (as is done by defaultReadObject) 
     * is sufficient to initialize the permission.
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
         throws IOException,ClassNotFoundException
    {
	setMethodSpec((String) s.readFields().get("actions",null));
    }

    /**
     * writeObject is used to establish the values of the serialized fields
     * before they are written to the output stream and need not be
     * implemented if the values of the serialized fields are always 
     * available and up to date. The serialized fields are written to
     * the output stream in the same form as they would be written
     * by defaultWriteObject.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
         throws IOException
    {
	s.putFields().put("actions",this.getActions());
	s.writeFields();
    }

    private void setMethodSpec (String actions)
    {
	
        String mInterface = null;

	this.methodName = null;
	this.methodParams = null;

	if (actions != null) {

	    if (actions.length() > 0) {

		int i = actions.indexOf(',');
		if (i < 0) this.methodName = actions;
		else if (i >= 0) {

		    if (i != 0) this.methodName = actions.substring(0,i);

		    if (actions.length() == i+1)
			throw new 
			    IllegalArgumentException("illegal actions spec"); 

		    int j = actions.substring(i+1).indexOf(',');
		    if (j < 0) mInterface = actions.substring(i+1);
		    
		    else {
			if (j > 0) mInterface = actions.substring(i+1,i+j+1);
			this.methodParams = actions.substring(i+j+1);

			if (this.methodParams.length() > 1 && 
			    this.methodParams.endsWith(",")) 
			    throw new 
				IllegalArgumentException("illegal methodParam"); 
		    }
		}
	    } else {
		//canonical form of emptystring actions is null
		actions = null;
	    }
	}

	this.methodInterface = validateInterface(mInterface); 

	if (this.methodInterface < -1) 
	    this.otherMethodInterface = mInterface;

	this.actions = actions;
    }

    private void setMethodSpec(String methodName,String mInterface,
			       String[] methodParams)
    {
	if (methodName != null && methodName.indexOf(',') >= 0)
	    throw new IllegalArgumentException("illegal methodName");
	
	this.methodInterface = validateInterface(mInterface);

	if (this.methodInterface < -1) 
	    this.otherMethodInterface = mInterface;

	if (methodParams != null) {

	    StringBuffer mParams = new StringBuffer(",");

	    for (int i=0; i<methodParams.length; i++) {
		if (methodParams[i] == null || 
		    methodParams[i].indexOf(',') >= 0)
		    throw new IllegalArgumentException("illegal methodParam");
		if (i == 0) mParams.append(methodParams[i]);
		else mParams.append("," + methodParams[i]);
	    }
	    this.methodParams = mParams.toString();
	}
	else this.methodParams = null;

	this.methodName = methodName;
    }

    private void setMethodSpec(String mInterface, Method method)
    {
	this.methodInterface = validateInterface(mInterface);

	if (this.methodInterface < -1) 
	    this.otherMethodInterface = mInterface;

	this.methodName = method.getName();

	Class[] params = method.getParameterTypes();
 
        StringBuffer mParams = new StringBuffer(",");

	for (int i=0; i<params.length; i++) {

	    String pname = params[i].getName();
	    Class compType = params[i].getComponentType();

	    // Canonicalize parameter if it is an Array.
	    if (compType != null) {
		String brackets = "[]";
		while (compType.getComponentType() != null) {
		    compType = compType.getComponentType();
		    brackets = brackets + "[]";
		}
		pname = compType.getName() + brackets;
	    }

 	    if (i == 0) mParams.append(pname);
	    else mParams.append("," + pname);
	}

        this.methodParams = mParams.toString();
    }

    private static int validateInterface (String methodInterface)
    {
	int result = -1;
	if (methodInterface != null && methodInterface.length() > 0) {
	    Integer i = (Integer) interfaceHash.get(methodInterface);
	    if (i != null) result = i.intValue();
	    else result = -2;
	}
	return result;
    }

}
