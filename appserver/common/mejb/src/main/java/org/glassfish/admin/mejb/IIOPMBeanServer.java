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

package org.glassfish.admin.mejb;

import java.util.Set;

import javax.management.*;

import java.rmi.RemoteException;

public interface IIOPMBeanServer extends java.rmi.Remote {

    
    Set queryNames(ObjectName name, QueryExp query) throws RemoteException;

    /**
     * Checks whether an managed object, identified by its object name, is already registered
     * with the MEJB.
     *
     * @param name The object name of the managed object to be checked.
     *
     * @return  True if the managed object is already registered in the MEJB, false otherwise.
     *
     * @exception RuntimeOperationsException Wraps a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in parameter is null.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     *
     */
    boolean isRegistered(ObjectName name) throws RemoteException;

    /**
     * Returns the number of managed objects registered in the MEJB.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     */
    Integer getMBeanCount() throws RemoteException;

    /**
     * This method discovers the attributes and operations that an managed object exposes
     * for management.
     *
     * @param name The name of the managed object to analyze
     *
     * @return  An instance of <CODE>MBeanInfo</CODE> allowing the retrieval of all attributes and operations
     * of this managed object.
     *
     * @exception IntrospectionException An exception occurs during introspection.
     * @exception InstanceNotFoundException The managed object specified is not found.
     */
    MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, RemoteException;

    /**
     * Gets the value of a specific attribute of a named managed object. The managed object
     * is identified by its object name.
     *
     * @param name The object name of the managed object from which the attribute is to be retrieved.
     * @param attribute A String specifying the name of the attribute to be
     * retrieved.
     *
     * @return  The value of the retrieved attribute.
     *
     * @exception AttributeNotFoundException The attribute specified is not accessible in the managed object.
     * @exception MBeanException  Wraps an exception thrown by the managed object's getter.
     * @exception InstanceNotFoundException The managed object specified is not registered in the MEJB.
     * @exception ReflectionException  Wraps a <CODE>java.lang.Exception</CODE> thrown when trying to invoke the setter. 
     * @exception RuntimeOperationsException Wraps a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in parameter is null or 
     * the attribute in parameter is null.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     */
    Object getAttribute(ObjectName name, String attribute) throws
            AttributeNotFoundException, MBeanException, InstanceNotFoundException, ReflectionException, RemoteException;

    /**
     * Enables the values of several attributes of a named managed object. The managed object
     * is identified by its object name.
     *
     * @param name The object name of the managed object from which the attributes are
     * retrieved.
     * @param attributes A list of the attributes to be retrieved.
     *
     * @return The list of the retrieved attributes.
     *
     * @exception InstanceNotFoundException The managed object specified is not registered in the MEJB.
     * @exception ReflectionException An exception occurred when trying to invoke the getAttributes method of a Dynamic managed object.
     * @exception RuntimeOperationsException Wrap a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in parameter is null or 
     * attributes in parameter is null.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     *
     */
    AttributeList getAttributes(ObjectName name, String[] attributes)
            throws InstanceNotFoundException, ReflectionException, RemoteException;

    /**
     * Sets the value of a specific attribute of a named managed object. The managed object
     * is identified by its object name.
     *
     * @param name The name of the managed object within which the attribute is to be set.
     * @param attribute The identification of the attribute to be set and the value it is to be set to.
     *
     * @return  The value of the attribute that has been set.
     *
     * @exception InstanceNotFoundException The managed object specified is not registered in the MEJB.
     * @exception AttributeNotFoundException The attribute specified is not accessible in the managed object.
     * @exception InvalidAttributeValueException The value specified for the attribute is not valid.
     * @exception MBeanException Wraps an exception thrown by the managed object's setter.
     * @exception ReflectionException  Wraps a <CODE>java.lang.Exception</CODE> thrown when trying to invoke the setter. 
     * @exception RuntimeOperationsException Wraps a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in parameter is null or 
     * the attribute in parameter is null.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     */
    void setAttribute(ObjectName name, Attribute attribute) throws
            InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException, RemoteException;

    /**
     * Sets the values of several attributes of a named managed object. The managed object is
     * identified by its object name.
     *
     * @param name The object name of the managed object within which the attributes are to
     * be set.
     * @param attributes A list of attributes: The identification of the
     * attributes to be set and  the values they are to be set to.
     *
     * @return  The list of attributes that were set, with their new values.
     *
     * @exception InstanceNotFoundException The managed object specified is not registered in the MEJB.
     * @exception ReflectionException An exception occurred when trying to invoke the getAttributes method of a Dynamic managed object.
     * @exception RuntimeOperationsException Wraps a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in parameter is null or 
     * attributes in parameter is null.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     *
     */
    AttributeList setAttributes(ObjectName name, AttributeList attributes)
            throws InstanceNotFoundException, ReflectionException, RemoteException;

    /**
     * Invokes an operation on an managed object.
     *
     * @param name The object name of the managed object on which the method is to be invoked.
     * @param operationName The name of the operation to be invoked.
     * @param params An array containing the parameters to be set when the operation is
     * invoked
     * @param signature An array containing the signature of the operation. The class objects will
     * be loaded using the same class loader as the one used for loading the managed object on which the operation was invoked.
     *
     * @return  The object returned by the operation, which represents the result ofinvoking the operation on the 
     * managed object specified.
     *
     * @exception InstanceNotFoundException The managed object specified is not registered in the MEJB.
     * @exception MBeanException  Wraps an exception thrown by the managed object's invoked method.
     * @exception ReflectionException  Wraps a <CODE>java.lang.Exception</CODE> thrown while trying to invoke the method.
     * @BM_EXPOSED
     * @BUSINESSMETHOD
     *
     */
    Object invoke(ObjectName name, String operationName, Object[] params,
            String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, RemoteException;

    /**
     * Returns the domain name of this MEJB.
     */
    String getDefaultDomain() throws RemoteException;

    /**
     * De-registers an MBean from the MBean server. The MBean is identified by
     * its object name. Once the method has been invoked, the MBean may no longer be accessed by its object name.
     * @param name The object name of the MBean to be de-registered.
     * @exception InstanceNotFoundException The MBean specified is not registered in the MBean server.
     * @exception MBeanRegistrationException The preDeregister ((<CODE>MBeanRegistration</CODE>  interface) method of the MBean
     * has thrown an exception.
     * @exception RuntimeOperationsException Wraps a <CODE>java.lang.IllegalArgumentException</CODE>: The object name in
     * parameter is null or the MBean you are when trying to de-register is the {@link javax.management.MBeanServerDelegate
     * MBeanServerDelegate} MBean.
     */
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException,
            MBeanRegistrationException, RemoteException;
}
