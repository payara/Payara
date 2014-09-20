/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deployment.xml.ConnectorTagNames;

import javax.resource.spi.AuthenticationMechanism;
import javax.resource.spi.security.GenericCredential;
import javax.resource.spi.security.PasswordCredential;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.deployment.common.Descriptor;
import org.ietf.jgss.GSSCredential;

/**
 * Deployment Information for connector outbound-resourceadapter
 *
 * @author	Qingqing Ouyang
 * @author Sheetal Vartak
 */
public class OutboundResourceAdapter extends Descriptor
{

    private int     transactionSupport = PoolManagerConstants.LOCAL_TRANSACTION;
    private Set     authMechanisms;
    private boolean reauthenticationSupport = false;
    private Set     connectionDefs;
    
    /*Set variables indicates that a particular attribute is set by DD processing so that
      annotation processing need not (must not) set the values from annotation */
    private boolean reauthenticationSupportSet = false;
    private boolean transactionSupportSet = false;

    public OutboundResourceAdapter () 
    {
    	this.authMechanisms      = new OrderedSet();
	this.connectionDefs = new OrderedSet();
    }

    /** 
     * Gets the value of supportsReauthentication
     */
    public boolean 
    supportsReauthentication() 
    {
        return reauthenticationSupport;
    }

    public String getReauthenticationSupport() {
        return String.valueOf(reauthenticationSupport);
    } 
 
    /** 
     * Sets the value of supportsReauthentication
     */
    public void 
    setReauthenticationSupport(boolean reauthenticationSupport) 
    {
        this.reauthenticationSupportSet = true;
        this.reauthenticationSupport = reauthenticationSupport;
    }

    /** sets the value of supportsReauthentication
     * DOL rearchitecture
    */
    public void setReauthenticationSupport(String reauthSupport) {
        this.reauthenticationSupport = 
	    (Boolean.valueOf(reauthSupport)).booleanValue();
        this.reauthenticationSupportSet = true;
    }


    /** 
     * Returns NO_TRANSACTION, LOCAL_TRANSACTION, XA_TRANSACTION
     * as defined in PoolManagerConstants interface
     */
    public String 
    getTransSupport() 
    {
        if (transactionSupport == PoolManagerConstants.NO_TRANSACTION)
	    return ConnectorTagNames.DD_NO_TRANSACTION;
	else if (transactionSupport == PoolManagerConstants.LOCAL_TRANSACTION)
            return ConnectorTagNames.DD_LOCAL_TRANSACTION;
	else 
            return ConnectorTagNames.DD_XA_TRANSACTION;
    }

    public int 
    getTransactionSupport() 
    {
        return transactionSupport;
    }
 
    /**
     * Set value of transactionSupport to NO_TRANSACTION, 
     * LOCAL_TRANSACTION, XA_TRANSACTION as defined in 
     * PoolManagerConstants interface 
     */
    public void 
    setTransactionSupport(int transactionSupport) 
    {
        this.transactionSupport = transactionSupport;
        this.transactionSupportSet = true;
    }

    /**
     * Set value of transactionSupport to NO_TRANSACTION, 
     * LOCAL_TRANSACTION, XA_TRANSACTION as defined in 
     * PoolManagerConstants interface 
     */
    public void 
    setTransactionSupport(String support) 
    {
        //TODO V3 : should throw exception when the "support" is none of XA/NO/Local ?
        try{
        if (ConnectorTagNames.DD_XA_TRANSACTION.equals(support))
            this.transactionSupport = PoolManagerConstants.XA_TRANSACTION;
        else if (ConnectorTagNames.DD_LOCAL_TRANSACTION.equals(support))
            this.transactionSupport = PoolManagerConstants.LOCAL_TRANSACTION;
        else
            this.transactionSupport = PoolManagerConstants.NO_TRANSACTION;

        this.transactionSupportSet = true;
        }catch(NumberFormatException nfe){
            Logger.getAnonymousLogger().log(Level.WARNING, "Error occurred", nfe); 
        }
    }
      
   /** 
    * Set of AuthMechanism objects 
    */
    public Set 
    getAuthMechanisms() 
    {
        if (authMechanisms == null) {
            authMechanisms = new OrderedSet();
        }
        return authMechanisms;
    }
    
    /** 
     * Add a AuthMechanism object to the set return value : 
     *                 false = found 
     *                 true = not found 
     */
    public boolean 
    addAuthMechanism(AuthMechanism mech) 
    {
        boolean flag=false;
        for (Iterator itr = authMechanisms.iterator(); itr.hasNext();){
	    AuthMechanism next = (AuthMechanism) itr.next();
	    if (next.getAuthMechVal()==mech.getAuthMechVal()) {  
	        return(flag);
	    }
	}	    
	flag=this.authMechanisms.add(mech);
	return(flag);
    }
    
    /** 
     * Remove a AuthMechanism object to the set
     * return value : false = found
     *                true = not found 
     */
    public boolean 
    removeAuthMechanism(AuthMechanism mech) 
    {
        boolean flag=false;
        for (Iterator itr = authMechanisms.iterator(); itr.hasNext();){
	    AuthMechanism next = (AuthMechanism) itr.next();
	    if (next.equals(mech)) {
	        flag=this.authMechanisms.remove(mech);
		return(flag);
	    }
	}
	return(flag);
    }


   /** 
    * Add a AuthMechanism object with given auth mech value to the set
    * return value : false = found
    *                true = not found 
    */
    public boolean 
    addAuthMechanism(int mech) 
    {
        boolean flag = false;
        for (Iterator itr = authMechanisms.iterator(); itr.hasNext();){
	    AuthMechanism next = (AuthMechanism) itr.next();
	    if (next.getAuthMechVal() == mech)
	       return(flag);
	}	    
        String credInf = null;
        if (mech == PoolManagerConstants.BASIC_PASSWORD) {
            credInf = PoolManagerConstants.PASSWORD_CREDENTIAL;
        } else {
            credInf = PoolManagerConstants.GENERIC_CREDENTIAL;
        }
	AuthMechanism auth = new AuthMechanism("",mech, credInf);
        flag = this.authMechanisms.add(auth);
	return(flag);
    }
    

    /** 
     * Remove a AuthMechanism object with given auth mech value from the set
     *       return value : false = found
     *                      true = not found 
     */
    public boolean 
    removeAuthMechanism(int mech) 
    {
        boolean flag = false;
        for (Iterator itr = authMechanisms.iterator(); itr.hasNext();){
	    AuthMechanism next = (AuthMechanism) itr.next();	   
	    if (next.getAuthMechVal()==mech)
	    {
	        flag = this.authMechanisms.remove(next);
                return(flag);
	    }
	}
	return(flag);
    }

    /**
     * adds an entry to the set of connection definitions
     */
    public void addConnectionDefDescriptor(ConnectionDefDescriptor conDefDesc) {
	this.connectionDefs.add(conDefDesc);
    }

    public boolean hasConnectionDefDescriptor(String connectionFactoryIntf){
        for(Object o  : connectionDefs){
            ConnectionDefDescriptor cdd = (ConnectionDefDescriptor)o;
            if(cdd.getConnectionFactoryIntf().equals(connectionFactoryIntf)){
                return true;
            }
        }
        return false;
    }

    /**
     * removes an entry from the set of connection definitions
     */
    public void removeConnectionDefDescriptor(ConnectionDefDescriptor conDefDesc) {
	this.connectionDefs.remove(conDefDesc);
    }

    /**
     * returns the set of connection definitions
     */
    public Set getConnectionDefs() {
	return connectionDefs;
    }
    
    ///////////////////////////////////////////////////////////////////////
    /**
     * For 1.0 DTD, the OutboundRA descriptor needs to take care of adding
     * connection factories and connection interfaces/impls
     * The following methods create a ConnectionFactoryDescriptor/ConnectionDescriptor instance 
     * using the info available in the 1.0 DTD.
     * BACKWARD COMPATIBILITY REQUIREMENT
     */

	/*    public void createNewConnectionDescriptor() {
	 ConnectionDescriptor conDesc = new ConnectionDescriptor();
	 this.addConnection(conDesc);
    }

    public void createNewConnectionFactoryDescriptor() {
	ConnectionFactoryDescriptor conDesc = new ConnectionFactoryDescriptor();
	 this.addConnectionFactory(conDesc);
    }

    public String getConnectionInterface() 
    {
	Iterator cons = getConnections().iterator(); 
	if (cons.hasNext()) {
	    return ((ConnectionDescriptor)cons.next()).getConnectionInterface();
	}
        else return null;
    }
    
    public String getConnectionClass()
    {
	Iterator cons = getConnections().iterator(); 
	if (cons.hasNext()) {
	    return ((ConnectionDescriptor)cons.next()).getConnectionClass();
	}
        else return null;
    }

    public void setConnectionInterface(String intf) 
    {
	Iterator cons = getConnections().iterator(); 
	if (cons.hasNext()) {
	    ((ConnectionDescriptor)cons.next()).setConnectionInterface(intf);
	}
	else throw new RuntimeException("There is no connection-interface specified for this 1.0 DTD");
    }

    public void setConnectionClass(String cl)
    {
	Iterator cons = getConnections().iterator(); 
	if (cons.hasNext()) {
	    ((ConnectionDescriptor)cons.next()).setConnectionClass(cl);
	}
	else throw new RuntimeException("There is no connection-class specified for this 1.0 DTD");
    }

    public String getConnectionFactoryInterface() 
    {
	Iterator cons = getConnectionFactories().iterator(); 
	if (cons.hasNext()) {
	    return ((ConnectionFactoryDescriptor)cons.next()).getConnectionFactoryInterface();
	}
        else return null;
    }
    
    public String getConnectionFactoryClass()
    {
	Iterator cons = getConnectionFactories().iterator(); 
	if (cons.hasNext()) {
	    return ((ConnectionFactoryDescriptor)cons.next()).getConnectionFactoryClass();
	}
        else return null;
    }

    public void setConnectionFactoryInterface(String intf) 
    {
	Iterator cons = getConnectionFactories().iterator(); 
	if (cons.hasNext()) {
	    ((ConnectionFactoryDescriptor)cons.next()).setConnectionFactoryInterface(intf);
	}
	else throw new RuntimeException("There is no connectionfactory-interface specified for this 1.0 DTD");
    }

    public void setConnectionFactoryClass(String cl)
    {
	Iterator cons = getConnectionFactories().iterator(); 
	if (cons.hasNext()) {
	    ((ConnectionFactoryDescriptor)cons.next()).setConnectionFactoryClass(cl);
	}
	else throw new RuntimeException("There is no connectionfactory-class specified for this 1.0 DTD");
	}*/
	
	///////////////////////////

	/**
	 * For being able to read 1.0 and write 1.5
	 */

	public void setConnectionDef(ConnectionDefDescriptor conDef) {
	    this.connectionDefs.add(conDef);
	}
    
    public ConnectionDefDescriptor getConnectionDef() {
	Iterator iter = connectionDefs.iterator(); 
	ConnectionDefDescriptor conDef = (ConnectionDefDescriptor)iter.next();
	return conDef;
    }
    
    /** 
     * Gets the value of ManagedconnectionFactoryImpl
     */
    public String getManagedConnectionFactoryImpl() 
    {
        return getConnectionDef().getManagedConnectionFactoryImpl();
    }
    
    /** 
     * Sets the value of ManagedconnectionFactoryImpl
     */
    public void 
    setManagedConnectionFactoryImpl(String managedConnectionFactoryImpl) 
    {
        getConnectionDef().setManagedConnectionFactoryImpl(managedConnectionFactoryImpl);
    }

    /** 
     * Set of EnvironmentProperty 
     */
    public Set getConfigProperties() 
    {
        return getConnectionDef().getConfigProperties();
    }
      
    /** 
     * Add a configProperty to the set
     */
    public void addConfigProperty(EnvironmentProperty configProperty) 
    {
	getConnectionDef().getConfigProperties().add(configProperty);
    }

    /** 
     * Add a configProperty to the set
     */ 
    public void removeConfigProperty(EnvironmentProperty configProperty) 
    {
	getConnectionDef().getConfigProperties().remove(configProperty);
    }
    
    /**
     * Get connection factory impl
     */
    public String getConnectionFactoryImpl()
    {
        return getConnectionDef().getConnectionFactoryImpl();
    }

    /** 
     * set connection factory impl 
     */
    public void setConnectionFactoryImpl(String cf) 
    {
	getConnectionDef().setConnectionFactoryImpl(cf);
    }

    /**
     * Get connection factory intf
     */
    public String getConnectionFactoryIntf()
    {
        return getConnectionDef().getConnectionFactoryIntf();
    }

    /** 
     * set connection factory intf
     */
    public void setConnectionFactoryIntf(String cf) 
    {
	getConnectionDef().setConnectionFactoryIntf(cf);
    }

    /**
     * Get connection intf
     */
    public String getConnectionIntf()
    {
        return getConnectionDef().getConnectionIntf();
    }

    /** 
     * set connection intf
     */
    public void setConnectionIntf(String con) 
    {
	getConnectionDef().setConnectionIntf(con);
    }

    /**
     * Get connection impl
     */
    public String getConnectionImpl()
    {
        return getConnectionDef().getConnectionImpl();
    }

    /** 
     * set connection intf
     */
    public void setConnectionImpl(String con) 
    {
	getConnectionDef().setConnectionImpl(con);
    }

    public boolean isReauthenticationSupportSet() {
        return reauthenticationSupportSet;
    }

    public boolean isTransactionSupportSet() {
        return transactionSupportSet;
    }

    public static String getCredentialInterfaceName(AuthenticationMechanism.CredentialInterface ci) {
        if (ci.equals(AuthenticationMechanism.CredentialInterface.GenericCredential)) {
            return GenericCredential.class.getName();
        } else if (ci.equals(AuthenticationMechanism.CredentialInterface.GSSCredential)) {
            return GSSCredential.class.getName(); 
        } else if (ci.equals(AuthenticationMechanism.CredentialInterface.PasswordCredential)) {
            return PasswordCredential.class.getName();
        }
        throw new RuntimeException("Invalid credential interface :  " + ci);
    }

}
