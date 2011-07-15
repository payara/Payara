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

/**
 * The methods of this interface are used by containers to
 * create policy statements in a Policy provider.
 * An object that implements the PolicyConfiguration interface provides the
 * policy statement configuration interface for a corresponding policy context
 * within the corresponding Policy provider.
 * <P>
 * The life cycle of a policy context
 * is defined by three states; "open", "inService", and "deleted". A policy
 * context is in one of these three states.
 * <P>
 * A policy context in the "open" state is in the process of being 
 * configured, and may be operated on by any of the methods of the 
 * PolicyConfiguration interface. A policy context in the "open" state
 * must not be assimilated at <code>Policy.refresh</code> into the policy
 * statements used by the Policy provider in performing its access decisions.
 * In order for the policy statements of a policy context to be assimilated
 * by the associated provider, the policy context must be in the
 * "inService" state. A policy context in the "open" state is transitioned to
 * the "inService" state by calling the commit method.
 * <P>
 * A policy context in the "inService" state is available for assimilation
 * into the policy statements being used to perform access decisions by the
 * associated Policy provider. Providers assimilate policy contexts containing
 * policy statements when the refresh method of the provider is called. When
 * a provider's refresh method is called, it must assimilate only those policy
 * contexts whose state is "inService" and it must ensure that the policy
 * statements put into service for each policy context are only those defined
 * in the context at the time of the call to refresh. A policy context in the
 * "inService" state is not available for additional configuration and may be
 * returned to the "open" state by calling the getPolicyConfiguration method
 * of the PolicyConfigurationFactory.
 * <P>
 * A policy context in the "deleted" state is neither available for
 * configuration, nor is it available for assimilation into the Provider. A
 * policy context whose state is "deleted" may be reclaimed for subsequent
 * processing by calling the getPolicyConfiguration method of the associated 
 * PolicyConfigurationFactory. A "deleted" policy context
 * is transitioned to the "open" state when it it returned as a result of
 * a call to getPolicyConfiguration.
 * <P>
 * The following table captures the correspondence between the policy context
 * life cycle and the methods of the PolicyConfiguration interface.
 * The rightmost 3 columns of the table correspond to the 
 * PolicyConfiguration state identified at the head of the column.
 * The values in the cells of these columns indicate
 * the next state resulting from a call to the method 
 * identifed in the leftmost column of the corresponding row, or that
 * calling the method is unsupported in the state 
 * represented by the column (in which case the state will remain unchanged).
 *
 * <br><br>
 * <table border="1" width="90%" nosave="" align="center"> 
 * <caption>PolicyConfiguration State Table</caption>
 *     <tr>
 *       <th valign="middle" rowspan="2" colspan="1" align="center">
 *         <font size="-2">Method</font></th>
 *       <th valign="top" rowspan="1" colspan="3" align="center">
 *         <font size="-2">Current State to Next State</font></th>
 *     </tr>
 *     <tr>
 *       <th width="25%" align="center"><font size="-2">deleted</font></th>
 *       <th width="12%" align="center"><font size="-2">open</font></th>
 *       <th width="25%" align="center"><font size="-2">inService</font></th>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">addToExcludedPolicy</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">addToRole</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">addToUncheckedPolicy</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *        <td width="28%"><font size="-2">commit</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">Unsupported Operation</font></td>
 *        <td width="12%" align="center">
 *          <font size="-2">inService</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">inService</font></td>
 *     </tr>
 *     <tr>
 *        <td width="28%"><font size="-2">delete</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">deleted</font></td>
 *        <td width="12%" align="center">
 *          <font size="-2">deleted</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">deleted</font></td>
 *     </tr>
 *     <tr>
 *        <td width="28%"><font size="-2">getContextID</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">deleted</font></td>
 *        <td width="12%" align="center">
 *          <font size="-2">open</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">inService</font></td>
 *     </tr>
 *     <tr>
 *        <td width="28%"><font size="-2">inService</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">deleted</font></td>
 *        <td width="12%" align="center">
 *          <font size="-2">open</font></td>
 *        <td width="25%" align="center">
 *          <font size="-2">inService</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">linkConfiguration</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">removeExcludedPolicy</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center"><font size="-2">
 *         open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">removeRole</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 *     <tr>
 *       <td width="28%"><font size="-2">removeUncheckedPolicy</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *       <td width="12%" align="center">
 *         <font size="-2">open</font></td>
 *       <td width="25%" align="center">
 *         <font size="-2">Unsupported Operation</font></td>
 *     </tr>
 * </table>
 * <br><P>
 * For a provider implementation to be compatible with multi-threaded
 * environments, it may be necessary to synchronize the refresh method of
 * the provider with the methods of its PolicyConfiguration interface and
 * with the getPolicyConfiguration and inService methods of its
 * PolicyConfigurationFactory.
 *
 * @see java.security.Permission
 * @see java.security.PermissionCollection
 * @see javax.security.jacc.PolicyContextException
 * @see javax.security.jacc.PolicyConfigurationFactory
 *
 * @author Ron Monzillo
 * @author Gary Ellison
 */

public interface PolicyConfiguration {
	
   /**
    * This method returns this object's policy context identifier.
    * @return this object's policy context identifier.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the getContextID method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */

    public String getContextID()
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add permissions to a named role in this PolicyConfiguration.
    * If the named role does not exist in the PolicyConfiguration, it is
    * created as a result of the call to this function.
    * <P>
    * It is the job of the Policy provider to ensure that all the permissions
    * added to a role are granted to principals "mapped to the role".
    * <P>
    * @param roleName the name of the Role to which the permissions are
    * to be added.
    * <P>
    * @param permissions the collection of permissions to be added
    * to the role. The collection may be either a homogenous or
    * heterogenous collection.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToRole method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToRole(String roleName, PermissionCollection permissions)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add a single permission to a named role in this
    * PolicyConfiguration.
    * If the named role does not exist in the PolicyConfiguration, it is
    * created as a result of the call to this function.
    * <P>
    * It is the job of the Policy provider to ensure that all the permissions
    * added to a role are granted to principals "mapped to the role".
    * <P>
    * @param roleName the name of the Role to which the permission is
    * to be added.
    * <P>
    * @param permission the permission to be added
    * to the role.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToRole method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToRole(String roleName, Permission permission)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add unchecked policy statements to this PolicyConfiguration.
    * <P>
    * @param permissions the collection of permissions to be added
    * as unchecked policy statements. The collection may be either
    * a homogenous or heterogenous collection.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToUncheckedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToUncheckedPolicy(PermissionCollection permissions)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add a single unchecked policy statement to this
    * PolicyConfiguration.
    * <P>
    * @param permission the permission to be added
    * to the unchecked policy statements.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToUncheckedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToUncheckedPolicy(Permission permission)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add excluded policy statements to this PolicyConfiguration.
    * <P>
    * @param permissions the collection of permissions to be added
    * to the excluded policy statements. The collection may be either
    * a homogenous or heterogenous collection.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToExcludedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToExcludedPolicy(PermissionCollection permissions)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to add a single excluded policy statement to this
    * PolicyConfiguration.
    * <P>
    * @param permission the permission to be added
    * to the excluded policy statements. 
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the addToExcludedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void addToExcludedPolicy(Permission permission)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to remove a role and all its permissions from this
    * PolicyConfiguration. This method has no effect on the links
    * between this PolicyConfiguration and others.
    * <P>
    * @param roleName the name of the role to remove from this 
    * PolicyConfiguration. If the value of the roleName parameter is "*"
    * and no role with name "*" exists in this PolicyConfiguration,
    * then all roles must be removed from this PolicyConfiguration.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the removeRole method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void removeRole(String roleName)
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to remove any unchecked policy statements from this 
    * PolicyConfiguration. This method has no effect on the links
    * between this PolicyConfiguration and others.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the removeUncheckedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void removeUncheckedPolicy()
        throws javax.security.jacc.PolicyContextException;

   /**
    * Used to remove any excluded policy statements from this
    * PolicyConfiguration. This method has no effect on the links
    * between this PolicyConfiguration and others.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the removeExcludedPolicy method signature.
    * The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void removeExcludedPolicy()
        throws javax.security.jacc.PolicyContextException;

   /**
    * Creates a relationship between this configuration and another
    * such that they share the same principal-to-role mappings.
    * PolicyConfigurations are linked to apply a common principal-to-role
    * mapping to multiple seperately manageable PolicyConfigurations,
    * as is required when an application is composed of multiple
    * modules.
    * <P>
    * Note that the policy statements which comprise a role, or comprise
    * the excluded or unchecked policy collections in a PolicyConfiguration
    * are unaffected by the configuration being linked to another.
    * <P>
    * @param link a reference to a different PolicyConfiguration than this
    * PolicyConfiguration.
    * <P>
    * The relationship formed by this method is symetric, transitive
    * and idempotent. If the argument PolicyConfiguration does not have a
    * different Policy context identifier than this PolicyConfiguration
    * no relationship is formed, and an exception, as described below, is
    * thrown.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" or "inService" when this
    * method is called.
    *
    * @throws java.lang.IllegalArgumentException
    * if called with an argument PolicyConfiguration whose Policy context
    * is equivalent to that of this PolicyConfiguration.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the linkConfiguration method signature. The exception
    * thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void linkConfiguration(PolicyConfiguration link)
    	throws javax.security.jacc.PolicyContextException;

   /**
    * Causes all policy statements to be deleted from this PolicyConfiguration
    * and sets its internal state such that calling any method, other than
    * delete, getContextID, or inService on the PolicyConfiguration will 
    * be rejected and cause an UnsupportedOperationException to be thrown.
    * <P>
    * This operation has no affect on any linked PolicyConfigurations
    * other than removing any links involving the deleted PolicyConfiguration.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the delete method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void delete()
        throws javax.security.jacc.PolicyContextException;

   /**
    * This method is used to set to "inService" the state of the policy context
    * whose interface is this PolicyConfiguration Object. Only those policy
    * contexts whose state is "inService" will be included in the policy
    * contexts processed by the Policy.refresh method. A policy context whose
    * state is "inService" may be returned to the "open" state by calling the 
    * getPolicyConfiguration method of the PolicyConfiguration factory
    * with the policy context identifier of the policy context.
    * <P>
    * When the state of a policy context is "inService", calling any method
    * other than commit, delete, getContextID, or inService on its
    * PolicyConfiguration Object will cause an UnsupportedOperationException
    * to be thrown.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws java.lang.UnsupportedOperationException
    * if the state of the policy context whose interface is this
    * PolicyConfiguration Object is "deleted" when this
    * method is called.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the commit method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public void commit()
        throws javax.security.jacc.PolicyContextException;

   /**
    * This method is used to determine if the policy context whose interface is
    * this PolicyConfiguration Object is in the "inService" state.
    *
    * @return true if the state of the associated policy context is
    * "inService"; false otherwise.
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.
    *
    * @throws javax.security.jacc.PolicyContextException
    * if the implementation throws a checked exception that has not been
    * accounted for by the inService method signature. The exception thrown
    * by the implementation class will be encapsulated (during construction)
    * in the thrown PolicyContextException.
    */
    public boolean inService()
    	throws javax.security.jacc.PolicyContextException;
}

