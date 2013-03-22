/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.provider;

import javax.security.jacc.*;

import java.io.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.Map;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.lang.reflect.Constructor;

import java.security.*;
import javax.security.auth.Subject;

import java.util.logging.*;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.LocalStringManagerImpl;

import com.sun.enterprise.security.SecurityRoleMapperFactoryGen;
import com.sun.enterprise.security.provider.PolicyParser.GrantEntry;
import com.sun.enterprise.security.provider.PolicyParser.ParsingException;
import com.sun.enterprise.security.provider.PolicyParser.PermissionEntry;
import com.sun.enterprise.security.provider.PolicyParser.PrincipalEntry;
import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.deployment.common.SecurityRoleMapperFactory;

/** 
 * Implementation of Jacc PolicyConfiguration Interface
 * @author Harpreet Singh (harpreet.singh@sun.com)
 * @author Ron Monzillo
 */
public class PolicyConfigurationImpl implements PolicyConfiguration {

    private static Logger logger = 
	Logger.getLogger(LogDomains.SECURITY_LOGGER);

    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(PolicyConfigurationImpl.class);

    //package access
    String CONTEXT_ID = null;

    // Excluded permissions
    private Permissions excludedPermissions = null;
    // Unchecked permissions
    private Permissions uncheckedPermissions = null;
    // permissions mapped to roles.
    private HashMap rolePermissionsTable = null;

    //private /*TODO: static */ SecurityRoleMapperFactory factory = SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory();

    private static String policySuffix = ".policy";
 
    private static String PROVIDER_URL = "policy.url.";

    private static final Class[] permissionParams = { String.class, String.class};

    // These are the 3 possible states that this object can be in.
    public static final int OPEN_STATE = 0;
    public static final int INSERVICE_STATE = 2;
    public static final int DELETED_STATE = 3;

    // new instances are created in the open state.
    protected int state = OPEN_STATE;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    private Lock rLock = rwLock.readLock();
    private Lock wLock = rwLock.writeLock();

    // this bit is used to optimize commit processing
    private boolean writeOnCommit = true;

    // this bit is used to optimize refresh processing
    private boolean wasRefreshed = false;
    
    private Policy policy = null;
    private String policyUrlValue = null;

    // policy file mod times 
    private long[] lastModTimes = new long[2];
    private final  Object refreshLock = new Object();
    private  String repository = null;
    private  Permission setPolicyPermission = null;
    private  PolicyConfigurationFactoryImpl fact=null;
    
    protected PolicyConfigurationImpl(String contextId, PolicyConfigurationFactoryImpl fact){
	CONTEXT_ID = contextId;
        this.fact = fact;
        repository = fact.getRepository();
	// initialize(open,remove,!fromFile)
//        initializeRepository();
	initialize(true,true,false);
    }

    /**
     * @param applicationPolicyDirectory, need to have absolute path
     * @param open, then mark state as open
     * @param remove, then remove any existing policy statements
     */
    protected PolicyConfigurationImpl
	(File applicationPolicyDirectory, boolean open, boolean remove, PolicyConfigurationFactoryImpl fact) {

        this.fact = fact;
	CONTEXT_ID = applicationPolicyDirectory.getParentFile().getName() +
                '/' + applicationPolicyDirectory.getName();

        repository = fact.getRepository();
        //initializeRepository();
	String name = getPolicyFileName(true);
	File f = new File(name);
	if (!f.exists()) {
            String defMsg="Unable to open Policy file: "+name;
            String msg= localStrings.getLocalString("pc.file_not_found",defMsg,new Object []{ name});
	    logger.log(Level.SEVERE,msg);
	    throw new RuntimeException(defMsg);
	}

	// initialize(open,remove,fromFile)
	initialize(open,remove,true);
    }

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
    public String getContextID() throws PolicyContextException {
	checkSetPolicyPermission();
	return this.CONTEXT_ID;
    }

   /**
    * Used to add permissions to a named role in this PolicyConfiguration.
    * If the named Role does not exist in the PolicyConfiguration, it is
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
	throws PolicyContextException
    {
        assertStateIsOpen();
	
	if (roleName != null && permissions != null) {
	    checkSetPolicyPermission();
	    for(Enumeration e = permissions.elements(); e.hasMoreElements();) {
		this.getRolePermissions(roleName).add((Permission)e.nextElement());
		writeOnCommit = true;
	    }
	}
    }
    
   /**
    * Used to add a single permission to a named role in this
    * PolicyConfiguration.
    * If the named Role does not exist in the PolicyConfiguration, it is
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
	throws PolicyContextException {

        assertStateIsOpen();

	if (roleName != null && permission != null) {
	    checkSetPolicyPermission();
	    this.getRolePermissions(roleName).add(permission);
	    writeOnCommit = true;
	}
    }
	
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
	throws PolicyContextException {	

        assertStateIsOpen();

	if (permissions != null) {
	    checkSetPolicyPermission();
	    for(Enumeration e = permissions.elements(); e.hasMoreElements();){
		this.getUncheckedPermissions().add((Permission) e.nextElement());
		writeOnCommit = true;
	    }
	}
    }

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
	throws PolicyContextException{
	
        assertStateIsOpen();


	if (permission != null) {
	    checkSetPolicyPermission();
	    this.getUncheckedPermissions().add(permission);
	    writeOnCommit = true;
	}
    }

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
	throws PolicyContextException {

        assertStateIsOpen();

	if (permissions != null) {
	    checkSetPolicyPermission();
	    for(Enumeration e = permissions.elements(); e.hasMoreElements();){
		this.getExcludedPermissions().add((Permission) e.nextElement());
		writeOnCommit = true;
	    }
	}
    }

   /**
    * Used to add a single excluded policy statement to this
    * PolicyConfiguration.
    * <P>
    * @param permission the permission to be added
    * to the excluded policy statements. 
    *
    * @throws java.lang.SecurityException
    * if called by an AccessControlContext that has not been
    * granted the "setPolicy" SecurityPermission.                  fa
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
	throws PolicyContextException{

        assertStateIsOpen();


	if (permission != null) {
	    checkSetPolicyPermission();
	    this.getExcludedPermissions().add(permission);
	    writeOnCommit = true;
	}
    }

   /**
    * Used to remove a role and all its permissions from this
    * PolicyConfiguration.
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
	throws PolicyContextException{

        assertStateIsOpen();

	if(roleName != null && rolePermissionsTable != null) {
	    checkSetPolicyPermission();
	    if (rolePermissionsTable.remove(roleName) != null) {
		if (rolePermissionsTable.isEmpty()) {
		    rolePermissionsTable = null;
		}
		writeOnCommit = true;
	    } else if (roleName.equals("*")) {
		boolean wasEmpty = rolePermissionsTable.isEmpty();
		if (!wasEmpty) {
		     rolePermissionsTable.clear();
		}
		rolePermissionsTable = null;
		if (!wasEmpty) {
		    writeOnCommit = true;
		}
	    }
	}
    }

   /**
    * Used to remove any unchecked policy statements from this 
    * PolicyConfiguration.
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
	throws PolicyContextException{

        assertStateIsOpen();

	checkSetPolicyPermission();	

	if (uncheckedPermissions != null) {
	    uncheckedPermissions = null;
	    writeOnCommit = true;
	}
    }

   /**
    * Used to remove any excluded policy statements from this
    * PolicyConfiguration.
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
        throws PolicyContextException{

        assertStateIsOpen();

	checkSetPolicyPermission();

	if (excludedPermissions != null) {
	    excludedPermissions = null;
	    writeOnCommit = true;
	}
    } 

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
    public void commit() throws PolicyContextException{

	synchronized(refreshLock) {
	    if(stateIs(DELETED_STATE)){
                String defMsg="Cannot perform Operation on a deleted PolicyConfiguration";
                String msg=localStrings.getLocalString("pc.invalid_op_for_state_delete",defMsg);
		logger.log(Level.WARNING,msg);
		throw new UnsupportedOperationException(defMsg);

	    } else {
            
		try {

		    checkSetPolicyPermission();

		    if (stateIs(OPEN_STATE)) {

			generatePermissions();

			setState(INSERVICE_STATE);
		    }
		} catch(Exception e){
                    String defMsg="commit fail for contextod "+CONTEXT_ID;
                    String msg=localStrings.getLocalString("pc.commit_failure",defMsg,new Object[]{CONTEXT_ID,e});
		    logger.log(Level.SEVERE,msg);
		    throw new PolicyContextException(e);
		}
		if (logger.isLoggable(Level.FINE)){
		    logger.fine("JACC Policy Provider: PC.commit "+CONTEXT_ID);
		}
	    }
	    
	}
    }

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
    public void linkConfiguration(PolicyConfiguration link) throws PolicyContextException {

        assertStateIsOpen();

	String linkId = link.getContextID();
	if (this.CONTEXT_ID.equals(linkId)) {
            String defMsg="Operation attempted to link PolicyConfiguration to itself.";
            String msg=localStrings.getLocalString("pc.unsupported_link_operation",defMsg);
	    logger.log(Level.WARNING,msg);
	    throw new IllegalArgumentException(defMsg);
	}

	checkSetPolicyPermission();

	updateLinkTable(linkId);

    }

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
    public void delete() throws PolicyContextException
    {
	checkSetPolicyPermission();
	synchronized(refreshLock) {
	    try {
		removePolicy();
	    } finally {
		setState(DELETED_STATE);
	    }
	}
    }

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
    public boolean inService() throws PolicyContextException{
	checkSetPolicyPermission();	
	boolean rvalue = stateIs(INSERVICE_STATE);
        
        if (logger.isLoggable(Level.FINE)) {
             logger.fine("JACC Policy Provider: inService: " +
                        (rvalue ? "true " : "false ") +
                        CONTEXT_ID);
        }
        
        return rvalue;
    }

    // The following methods are implementation specific

    protected  void checkSetPolicyPermission() {
	SecurityManager sm = System.getSecurityManager();
	if (sm != null) {
	    if (setPolicyPermission == null) {
		setPolicyPermission = new java.security.SecurityPermission("setPolicy");
	    }
	    sm.checkPermission(setPolicyPermission);
	}
    }

    // get the policy object
    protected java.security.Policy getPolicy(){
	if (stateIs(INSERVICE_STATE)) {
	    return this.policy;
	} 
	if (logger.isLoggable(Level.FINEST)) {
	    logger.finest("JACC Policy Provider: getPolicy ("+CONTEXT_ID+") is NOT in service");
	}
	return null;
    }

    // get the policy object
    protected Permissions getExcludedPolicy(){
	return stateIs(INSERVICE_STATE) ? this.excludedPermissions : null;
    }

    // called by PolicyWrapper to refresh context specific policy object.
    protected void refresh(boolean force){

	synchronized(refreshLock){
	    if (stateIs(INSERVICE_STATE) && 
		(wasRefreshed == false || force || filesChanged())) {

		// find open policy.url
		int i = 0;
		String value = null;
		String urlKey = null;
		while (true) {
		    urlKey = PROVIDER_URL+(++i);
		    value = getSecurityProperty(urlKey);
		    if (value == null || value.equals("")) {
			break;
		    }
		}

		try { 
                        setSecurityProperty(urlKey, policyUrlValue);

		    if (fileChanged(false)) {
			excludedPermissions = loadExcludedPolicy();
		    }

		    // capture time before load, to ensure that we
		    // have a time that precedes load
		    captureFileTime(true);

		    if (policy == null) {
			policy = getNewPolicy();
		    } else {
			policy.refresh();
			if (logger.isLoggable(Level.FINE)){
			    logger.fine("JACC Policy Provider: Called Policy.refresh on contextId: "+CONTEXT_ID+" policyUrlValue was "+policyUrlValue);
			}
		    }
		    wasRefreshed = true;
		} finally {
		    // can't setProperty back to null, workaround is to 
		    // use empty string
		    setSecurityProperty(urlKey, "");
		}
	    }
	}
    }

    private java.security.Policy getNewPolicy() {
	Object wrapper = java.security.Policy.getPolicy();
	if (wrapper != null && wrapper instanceof BasePolicyWrapper) {
	    return ((BasePolicyWrapper) wrapper).getNewPolicy();
	} else {
	    return new sun.security.provider.PolicyFile();
	} 
    }

    private void captureFileTime(boolean granted) {
	String name = getPolicyFileName(granted);
	File f = new File(name);
	lastModTimes[(int) (granted ? 1 : 0)] = f.lastModified();
    }

    private boolean _fileChanged(boolean granted, File f) {
	return !(lastModTimes[(int) (granted ? 1 : 0)] == f.lastModified());
    }
    
    private boolean fileChanged(boolean granted) {
	String name = getPolicyFileName(granted);
	File f = new File(name);
        return _fileChanged(granted,f);
    }

    private boolean filesChanged() {
	return (fileChanged(true) || fileChanged(false));
    }
    
    /** 
     * tests if policy file has arrived (via synchronization system).
     * if File exists, also checks last modified time, in case file was
     * not deleted on transition out of inservice state. Called when context 
     * is not inService to determine if it was needs to be transitioned
     * because of file distribution.
     * @param granted selects granted or excluded policy file
     * @return true if new file has arrived.
     */
    private boolean fileArrived(boolean granted) {
	String name = getPolicyFileName(granted);
	File f = new File(name);
	boolean rvalue = ( f.exists() && _fileChanged(granted,f) );

        if (logger.isLoggable(Level.FINE)){
            logger.fine("JACC Policy Provider: file arrival check" +
                    " type: " + (granted? "granted " : "excluded ") +
                    " arrived: " + rvalue +
		    " exists: " + f.exists() +
		    " lastModified: " + f.lastModified() + 
		    " storedTime: " + lastModTimes[(int) (granted ? 1 : 0)] +
                    " state: " + (this.state == OPEN_STATE ? "open " : "deleted ") +
                    CONTEXT_ID);
        }

	return rvalue;
    }

    // initilaize the internal data structures.
    // if open, then mark state as open
    // if remove, then remove any existing policy statements
    // if fromFile (and not remove), then mark state as in service,
    //      and not requiring write on commit
    // if fromFile (and remove), then remove and mark state as open
    protected void initialize(boolean open, boolean remove, boolean fromFile) {
	synchronized(refreshLock) {
	    String name = getPolicyFileName(true);
	    if (open || remove) {
		setState(OPEN_STATE);
	    } else {
		setState(INSERVICE_STATE);
	    }
	    try {
		if (remove) {
		    removePolicy();
		}

		policyUrlValue = 
		    sun.net.www.ParseUtil.fileToEncodedURL(new File(name)).toString();
		if (fromFile && !remove) {
                    uncheckedPermissions = null;
                    rolePermissionsTable = null;
		    excludedPermissions = loadExcludedPolicy();
                    initLinkTable();
                    captureFileTime(true);
		    writeOnCommit = false;
		}
		wasRefreshed = false;
	    } catch (java.net.MalformedURLException mue) {
                String defMsg="Unable to convert Policy file Name to URL: "+name;
                String msg=localStrings.getLocalString("pc.file_to_url",defMsg, new Object[]{name,mue});
		logger.log(Level.SEVERE,msg);
		throw new RuntimeException(defMsg);
	    }
	}
    }

    private String getPolicyFileName(boolean granted) {
      return granted ?
	  getContextDirectoryName()+File.separator+"granted"+policySuffix :
	  getContextDirectoryName()+File.separator+"excluded"+policySuffix;
    }

    private String getContextDirectoryName() {
	if (repository == null) {
	    throw new RuntimeException("JACC Policy provider: repository not initialized");
	}
	return fact.getContextDirectoryName(CONTEXT_ID);
    }

    

    // remove the directory used ot hold the context's policy files
    private void removePolicyContextDirectory(){
	String directoryName = getContextDirectoryName();
	File f = new File(directoryName);
	if(f.exists()){

            // WORKAROUND: due to existence of timestamp file in given directory
            // for SE/EE synchronization
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if(!files[i].delete()) {
                        String msg = localStrings.getLocalString("pc.file_delete_error","Error while deleting policy file");
                        logger.log(Level.SEVERE,msg);
                        throw new RuntimeException(msg);
                    }
                }
             }
             //WORKAROUND: End 

	    if (!f.delete()) {
                String defMsg = "Failure removing policy context directory: "+directoryName;
                String msg=localStrings.getLocalString("pc.file_delete_error", defMsg);
		logger.log(Level.SEVERE,msg);
		throw new RuntimeException(defMsg);
	    } else if(logger.isLoggable(Level.FINE)){
		logger.fine("JACC Policy Provider: Policy context directory removed: "+directoryName);
	    }

            File appDir = f.getParentFile();
            // WORKAROUND: due to existence of timestamp file in given directory
            // for SE/EE synchronization
            File[] fs = appDir.listFiles();
            if (fs != null && fs.length > 0) {
                boolean hasDir = false;
                for (int i = 0; i < fs.length; i++) {
                    if (fs[i].isDirectory()) {
                        hasDir = true;
                        break;
                    }
                }
                if (!hasDir) {
                    for (int i = 0; i < fs.length; i++) {
                        fs[i].delete();
                    }
                }
            }
            //WORKAROUND: End 

            File[] moduleDirs = appDir.listFiles();
            if (moduleDirs == null || moduleDirs.length == 0) {
                if (!appDir.delete()) {
                    String defMsg = "Failure removing policy context directory: " + appDir;
                    String msg = localStrings.getLocalString("pc.file_delete_error", defMsg);
		    logger.log(Level.SEVERE,msg);
		    throw new RuntimeException(defMsg);
                }
            }
	}
    }

    // remove the external (file) policy statements.
    private void removePolicyFile(boolean granted){
	String fileName = getPolicyFileName(granted);
	File f = new File(fileName);
	if(f.exists()){
	    if (!f.delete()) {
                String defMsg = "Failure removing policy file: "+fileName; 
                String msg=localStrings.getLocalString("pc.file_delete_error", defMsg,new Object []{ fileName} );
		logger.log(Level.SEVERE,msg);
		throw new RuntimeException(defMsg);
	    } else if(logger.isLoggable(Level.FINE)){
		logger.fine("JACC Policy Provider: Policy file removed: "+fileName);
	    }
	}
    }

    // remove the internal and external (file) policy statements.
    private void removePolicy(){
	excludedPermissions = null;
	uncheckedPermissions = null;
	rolePermissionsTable = null;
	removePolicyFile(true);
	removePolicyFile(false);
	removePolicyContextDirectory();
	initLinkTable();
	policy = null;
	writeOnCommit = true;
    }

    private void initLinkTable() {

	synchronized(refreshLock) {
	    // get the linkSet corresponding to this context.
	    Set linkSet = (Set) fact.getLinkTable().get(CONTEXT_ID);
	    // remobe this context id from the linkSet (which may be shared
	    // with other contexts), and unmap the linkSet form this context.
	    if (linkSet != null) {
		linkSet.remove(CONTEXT_ID);
		fact.getLinkTable().remove(CONTEXT_ID);
	    }

	    // create a new linkSet with onlythis context id, and put it in the table.
	    linkSet = new HashSet();
	    linkSet.add(CONTEXT_ID);
	    fact.getLinkTable().put(CONTEXT_ID,linkSet);
	}
    }

    private void updateLinkTable(String otherId) {

	synchronized(refreshLock) {

	    // get the linkSet corresponding to this context
	    Set linkSet = (Set) fact.getLinkTable().get(CONTEXT_ID);
	    // get the linkSet corresponding to the context being linked to this
	    Set otherLinkSet = (Set) fact.getLinkTable().get(otherId);

	    if (otherLinkSet == null) {
                String defMsg="Linked policy configuration ("+otherId+") does not exist";
                //String msg = localStrings.getLocalString("pc.invalid_link_target",defMsg, new Object []{otherId});
		logger.log(Level.SEVERE,"pc.invalid_link_target",otherId);
		throw new RuntimeException(defMsg);
	    } else {
		Iterator it = otherLinkSet.iterator();
		// for each context (id) linked to the context being linked to this
		while (it.hasNext()) {
		    String id = (String) it.next();
		    //add the id to this linkSet
		    linkSet.add(id);
		    //replace the linkset mapped to all the contexts being linked
		    //to this context, with this linkset.
		    fact.getLinkTable().put(id,linkSet);
		}
	    }
	}
    }

    private void setState(int stateValue) {
	wLock.lock();
	try {
	    this.state = stateValue;
	} finally {
	    wLock.unlock();
	}
    }

    
    private boolean _stateIs(int stateValue) {
	rLock.lock();
	try {
	    return (this.state == stateValue);
	} finally {
	    rLock.unlock();
	}
    }
    
     /**
     * checks if PolicyContex is in agrument state.
     * Detects implicpit state changes resulting from
     * distribution of policy files by synchronization
     * system.
     * @param stateValue state the context is tested for
     * @return true if in state.
     */
    private boolean stateIs(int stateValue) {
	boolean inState = _stateIs(stateValue);
	if (stateValue == INSERVICE_STATE && !inState) {
	    if (fileArrived(true) || fileArrived(false)) {
                
                if (logger.isLoggable(Level.FINE)){
                    logger.fine("JACC Policy Provider: file arrived transition to inService: " +
                        " state: " + (this.state == OPEN_STATE ? "open " : "deleted ") +
                        CONTEXT_ID);
                }
                
		// initialize(!open,!remove,fromFile)               
                initialize(false,false,true);
	    }
	    inState = _stateIs(INSERVICE_STATE);
	} 
              
	return inState;
    }


    private void assertStateIsOpen() {
      
        if (!stateIs(OPEN_STATE)){
            String defMsg="Operation invoked on closed or deleted PolicyConfiguration.";
            String msg = localStrings.getLocalString("pc.op_requires_state_open",defMsg); 
	    logger.log(Level.WARNING, msg);
	    throw new UnsupportedOperationException(defMsg);
	}
    }

 

    private Permissions getUncheckedPermissions() {
	if (uncheckedPermissions == null) {
	    uncheckedPermissions = new Permissions();
	}
	return uncheckedPermissions;
    }

    private Permissions getExcludedPermissions() {
	if (excludedPermissions == null) {
	    excludedPermissions = new Permissions();
	}
	return excludedPermissions;
    }

    private Permissions getRolePermissions(String roleName) {
	if (rolePermissionsTable == null) rolePermissionsTable = new HashMap();
	Permissions rolePermissions = (Permissions) rolePermissionsTable.get(roleName);
	if (rolePermissions == null) {
	    rolePermissions = new Permissions();
	    rolePermissionsTable.put(roleName,rolePermissions);
	}
	return rolePermissions;
    }

    // This method workarounds a bug in PolicyParser.write(...).
    private String escapeName(String name) {
        return (name != null && name.indexOf('"') > 0) ?
                name.replaceAll("\"", "\\\\\"") : name;
    }
    
    private void generatePermissions() 

	throws java.io.FileNotFoundException, java.io.IOException {

	// optimization - return if the rules have not changed

	if (!writeOnCommit) return;

	// otherwise proceed to write policy file

	Map roleToSubjectMap = null;
        SecurityRoleMapperFactory factory=SecurityRoleMapperFactoryGen.getSecurityRoleMapperFactory();
	if (rolePermissionsTable != null) {
	    // Make sure a role to subject map has been defined for the Policy Context
	    if (factory != null) {
                // the rolemapper is stored against the
                // appname, for a web app get the appname for this contextid
                SecurityRoleMapper srm = factory.getRoleMapper(CONTEXT_ID);
		if (srm != null) {
		    roleToSubjectMap = srm.getRoleToSubjectMapping();
		}
		if (roleToSubjectMap != null) {
		    // make sure all liked PC's have the same roleToSubjectMap
		    Set linkSet = (Set) fact.getLinkTable().get(CONTEXT_ID);
		    if (linkSet != null) {
			Iterator it = linkSet.iterator();
			while (it.hasNext()) {
			    String contextId = (String)it.next();
			    if (!CONTEXT_ID.equals(contextId)) {
				SecurityRoleMapper otherSrm = factory.getRoleMapper(contextId);
				Map otherRoleToSubjectMap = null;

				if (otherSrm != null) {
				    otherRoleToSubjectMap = otherSrm.getRoleToSubjectMapping();
				}
				
				if (otherRoleToSubjectMap != roleToSubjectMap) {
                                    String defMsg="Linked policy contexts have different roleToSubjectMaps ("+CONTEXT_ID+")<->("+contextId+")";
                                    String msg=localStrings.getLocalString("pc.linked_with_different_role_maps",defMsg,new Object []{CONTEXT_ID,contextId});
				    logger.log(Level.SEVERE,msg); 
				    throw new RuntimeException(defMsg);
				}
			    }
			}
		    }
		}
	    }
	}

	if (roleToSubjectMap == null && rolePermissionsTable != null) {
            String defMsg="This application has no role mapper factory defined";
            String msg=localStrings.getLocalString("pc.role_map_not_defined_at_commit",defMsg,new Object []{CONTEXT_ID});
	    logger.log(Level.SEVERE,msg);
	    throw new RuntimeException
		(localStrings.getLocalString
		 ("enterprise.deployment.deployment.norolemapperfactorydefine",defMsg));
	}

	PolicyParser parser = new PolicyParser(false);

	// load unchecked grants in parser
	if (uncheckedPermissions != null) {
	    Enumeration pEnum = uncheckedPermissions.elements();
	    if (pEnum.hasMoreElements()) {
		GrantEntry grant = new GrantEntry();
		while (pEnum.hasMoreElements()) {
		    Permission p = (Permission) pEnum.nextElement();
		    PermissionEntry entry = 
			new PermissionEntry(p.getClass().getName(),
					    p.getName(),p.getActions());
		    grant.add(entry);
		}
		parser.add(grant);
	    }
	}

	// load role based grants in parser
	if (rolePermissionsTable != null) {
	    Iterator roleIt = rolePermissionsTable.keySet().iterator();
	    while (roleIt.hasNext()) {
		boolean withPrincipals = false;
		String roleName = (String) roleIt.next();
		Permissions rolePerms = getRolePermissions(roleName);
		Subject rolePrincipals = (Subject) roleToSubjectMap.get(roleName);
		if (rolePrincipals != null) {
		    Iterator pit = rolePrincipals.getPrincipals().iterator();
		    while (pit.hasNext()){
			Principal prin = (Principal) pit.next();

			if (prin != null) {
			    withPrincipals = true;
			    PrincipalEntry prinEntry = 
				new PrincipalEntry(prin.getClass().getName(),
						   escapeName(prin.getName()));
			    GrantEntry grant = new GrantEntry();
			    grant.principals.add(prinEntry);
			    Enumeration pEnum = rolePerms.elements();
			    while (pEnum.hasMoreElements()) {
				Permission perm = (Permission) pEnum.nextElement();
				PermissionEntry permEntry = 
				    new PermissionEntry(perm.getClass().getName(),
							perm.getName(),
							perm.getActions());
				grant.add(permEntry);
			    }
			    parser.add(grant);
			}
			else {
                            String msg = localStrings.getLocalString("pc.non_principal_mapped_to_role",
                                         "non principal mapped to role "+roleName,new Object[]{prin,roleName});
			    logger.log(Level.WARNING,msg);
			}
		    }
		} 
		/**
		 * JACC MR8 add grant for the any authenticated user role '**'
		 */
		if (!withPrincipals && ("**".equals(roleName))) {
			withPrincipals = true;
			PrincipalEntry prinEntry = new PrincipalEntry(
					PrincipalEntry.WILDCARD_CLASS,PrincipalEntry.WILDCARD_NAME);
			GrantEntry grant = new GrantEntry();
			grant.principals.add(prinEntry);
			Enumeration pEnum = rolePerms.elements();
			while (pEnum.hasMoreElements()) {
				Permission perm = (Permission) pEnum.nextElement();
				PermissionEntry permEntry = 
						new PermissionEntry(perm.getClass().getName(),
								perm.getName(),
								perm.getActions());
				grant.add(permEntry);
			}
			parser.add(grant);
			if(logger.isLoggable (Level.FINE)){
				logger.fine("JACC Policy Provider: added role grant for any authenticated user");
			}
		}
		if (!withPrincipals) {
                    String msg = localStrings.getLocalString("pc.no_principals_mapped_to_role",
                                  "no principals mapped to role "+roleName, new Object []{ roleName});
		    logger.log(Level.WARNING,msg);
		}
	    }
	}

	writeOnCommit = createPolicyFile(true,parser,writeOnCommit);

	// load excluded perms in excluded parser
	if (excludedPermissions != null) {

	    PolicyParser excludedParser = new PolicyParser(false);

	    Enumeration pEnum = excludedPermissions.elements();
	    if (pEnum.hasMoreElements()) {
		GrantEntry grant = new GrantEntry();
		while (pEnum.hasMoreElements()) {
		    Permission p = (Permission) pEnum.nextElement();
		    PermissionEntry entry = 
			new PermissionEntry(p.getClass().getName(),
					    p.getName(),p.getActions());
		    grant.add(entry);
		}
		excludedParser.add(grant);
	    }

	    writeOnCommit = createPolicyFile(false,excludedParser,writeOnCommit);
	} 

	if (!writeOnCommit) wasRefreshed = false;
    }

    private void createPolicyContextDirectory() {

        String contextDirectoryName = getContextDirectoryName();
        File d = new File(contextDirectoryName);

        String defMsg = "unable to create policy context directory";
        String msg = localStrings.getLocalString("pc.unable_to_create_context_directory",
                defMsg, new Object[]{contextDirectoryName});
        if (d.exists()) {
            if (!d.isDirectory()) {

                logger.log(Level.SEVERE, msg);
                throw new RuntimeException(defMsg);
            }
        } else {
            if (!d.mkdirs()) {
                logger.log(Level.SEVERE, msg);
                throw new RuntimeException(defMsg);
            }
	}
    }

    // returns false if write succeeds. otherwise returns input woc (i.e. writeOnCommit)
    private boolean createPolicyFile
	(boolean granted, PolicyParser parser, boolean woc) throws java.io.IOException {

	boolean result = woc;
	createPolicyContextDirectory();
	removePolicyFile(granted);
	String name = getPolicyFileName(granted);
	OutputStreamWriter writer = null;
	try {
	    if(logger.isLoggable (Level.FINE)){
		logger.fine("JACC Policy Provider: Writing grant statements to policy file: "+name);
	    }
	    writer = new OutputStreamWriter(new FileOutputStream(name), "UTF-8");
	    parser.write(writer);
	    result = false;
	} catch(java.io.FileNotFoundException fnfe) {
            String msg=localStrings.getLocalString("pc.file_error","file not found "+name,
                  new Object []{name, fnfe});
	    logger.log(Level.SEVERE,msg);
	    throw fnfe;
	} catch(java.io.IOException ioe){
            String msg=localStrings.getLocalString("pc.file_write_error","file IO error on file "+name,
                  new Object []{name,ioe});
	    logger.log(Level.SEVERE,msg);
	    throw ioe;
	} finally {
	    if (writer != null) {
		try {
		    writer.close();
		    captureFileTime(granted);
		} catch (Exception e) {
                    String defMsg="Unable to close Policy file: "+name;
                    String msg=localStrings.getLocalString("pc.file_close_error",defMsg,new Object []{name,e}); 
		    logger.log(Level.SEVERE,msg);
		    throw new RuntimeException(defMsg);
		}
	    }
	}
	return result;
    }

    private Permission loadPermission(String className,String name,String actions){
	Class clazz = null;
	Permission permission = null;
	try{
	    clazz = Class.forName(className);
	    Constructor c = clazz.getConstructor(permissionParams);
	    permission = (Permission) c.newInstance(new Object[] { name, actions });	
	} catch(Exception e){
            String defMsg="PolicyConfiguration error loading permission";
            String msg=localStrings.getLocalString("pc.permission_load_error",defMsg,
                       new Object []{className, e});
	    logger.log(Level.SEVERE,msg);
	    throw new RuntimeException(defMsg,e);
	}
	return permission;
    }

    private Permissions loadExcludedPolicy() {
	Permissions result = null;
	String name = getPolicyFileName(false);
	FileReader reader = null;
	PolicyParser parser = new PolicyParser(false);
	try {
	    captureFileTime(false);
	    reader = new FileReader(name);
	    parser.read(reader);
	} catch (java.io.FileNotFoundException fnf) {
	    //Just means there is no excluded Policy file, which
	    // is the typical case
	    parser = null;
	} catch (java.io.IOException ioe) {
            String defMsg="Error reading Policy file: "+name;
            String msg=localStrings.getLocalString("pc.file_read_error",defMsg,
                       new Object []{name, ioe});
	    logger.log(Level.SEVERE,msg);
	    throw new RuntimeException(defMsg);
	} catch ( ParsingException pe) {
            String defMsg="Unable to parse Policy file: "+name;
            String msg=localStrings.getLocalString("pc.policy_parsing_exception",defMsg,
                       new Object []{name,pe});
	    logger.log(Level.SEVERE,msg);
	    throw new RuntimeException(defMsg);
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (Exception e) {
                    String defMsg="Unable to close Policy file: "+name;
                    String msg=localStrings.getLocalString("pc.file_close_error",defMsg,
                                new Object []{name,e});
		    logger.log(Level.SEVERE,msg);
		    throw new RuntimeException(defMsg);
		}
	    }
	}

	if (parser != null) {
	    Enumeration grants = parser.grantElements();
	    while (grants.hasMoreElements()) {
		GrantEntry grant = (GrantEntry) grants.nextElement();
		if (grant.codeBase != null || grant.signedBy != null || 
		    grant.principals.size() != 0) {
                        String msg=localStrings.getLocalString("pc.excluded_grant_context_ignored",
                                   "ignore excluded grant context", new Object []{grant});
		    logger.log(Level.WARNING,msg);
		} else {
		    Enumeration perms = grant.permissionEntries.elements();
		    while (perms.hasMoreElements()) {
			PermissionEntry entry = (PermissionEntry) perms.nextElement();
			Permission p = 
			    loadPermission(entry.permission,entry.name,entry.action);
			if (result == null) {
			    result = new Permissions();
			}
			result.add(p);
		    }
		}
	    }
	}

	return result;
    }
    
    private void setSecurityProperty(final String key, final String value) {
        if (System.getSecurityManager() == null) {
            java.security.Security.setProperty(key, value);
        } else {
            java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction() {

                        public java.lang.Object run() {
                            java.security.Security.setProperty(key, value);
                            return null;
                        }
                    });
        }
    }

    private String getSecurityProperty(final String key) {
        if (System.getSecurityManager() == null) {
            return java.security.Security.getProperty(key);
        } else {
            return java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedAction<String>() {

                        public String run() {
                            return java.security.Security.getProperty(key);

                        }
                    });
        }
    }
}





