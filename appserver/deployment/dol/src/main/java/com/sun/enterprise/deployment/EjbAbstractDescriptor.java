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

import org.glassfish.deployment.common.Descriptor;

import java.util.HashSet;
import java.util.Set;

/**
 * I am an abstract super class of all ejb descriptors.
 *
 * @author Danny Coward
 */

public abstract class EjbAbstractDescriptor extends Descriptor implements NamedDescriptor {
    private String homeClassName;
    private String remoteClassName;
    private String localHomeClassName;
    private String localClassName;

    private Set<String> remoteBusinessClassNames = new HashSet<String>();
    private Set<String> localBusinessClassNames = new HashSet<String>();
    private Set<String> noInterfaceLocalBeanClassNames = new HashSet<String>();
    

    // This is the value of the EJB 2.1 deployment descriptor entry
    // for service endpoint interface.
    private String webServiceEndpointInterfaceName;

    private String jndiName = "";
    private String mappedName = "";
    
    // Is set to true if this bean exposes a no-interface view 
    private boolean localBean = false;
       
	/** 
	* Default constructor. 
	*/
    public EjbAbstractDescriptor() {
    }
    
    /**
    * Copy constructor.
    */
    protected EjbAbstractDescriptor(EjbAbstractDescriptor other) {
	super(other);
	this.homeClassName = other.homeClassName;
	this.remoteClassName = other.remoteClassName;
        this.remoteBusinessClassNames = 
            new HashSet<String>(other.remoteBusinessClassNames);
        this.localHomeClassName = other.localHomeClassName;
        this.localClassName = other.localClassName;
        this.localBusinessClassNames = 
            new HashSet<String>(other.localBusinessClassNames);
        this.webServiceEndpointInterfaceName = 
            other.webServiceEndpointInterfaceName;
        this.localBean = other.localBean;
	this.jndiName = other.jndiName;
    }
    
 
    public abstract String getType();
    public abstract void setType(String type);
    
	/**
	* Returns the classname of the Home interface of this ejb. 
	*/
    public String getHomeClassName() {
	return this.homeClassName;
    }
	
	/** 
	* Sets the classname of the Home interface of this ejb. 
	*/
    public void setHomeClassName(String homeClassName) {
	this.homeClassName = homeClassName;
    }
    
	/**
	* Sets the classname of the Remote interface of this ejb. 
	*/
    public void setRemoteClassName(String remoteClassName) {
	this.remoteClassName = remoteClassName;
    }
    

	/**
	* Returns the classname of the Remote interface of this ejb.
	*/
    public String getRemoteClassName() {
	return this.remoteClassName;
    }	
    
    /**
     * Sets the classname for the local home interface of this ejb
     * 
     * @param localHomeClassName fully qualified class name for the interface
     */
    public void setLocalHomeClassName(String localHomeClassName) {
        this.localHomeClassName = localHomeClassName;
    }
    
    /**
     * @return the fully qualified class name for the local home interface of this ejb
     */
    public String getLocalHomeClassName() {
        return localHomeClassName;
    }
    
    /**
     * Sets the classname for the local interface of this ejb
     * 
     * @param localClassName fully qualified class name for the interface
     */
    public void setLocalClassName(String localClassName) {
        this.localClassName = localClassName;
    }
    
    /**
     * @return the fully qualified class name for the local interface of this ejb
     */
    public String getLocalClassName() {
        return localClassName;
    }
    
    /**
     * Add a classname for a no-interface view of the local ejb
     * 
     * @param localClassName fully qualified class name for the interface
     */
    public void addNoInterfaceLocalBeanClass(String className) {
        this.noInterfaceLocalBeanClassNames.add(className);
    }
    
    /**
     * @return all the public classes of this no-interface local ejb
     */
    public Set<String> getNoInterfaceLocalBeanClasses() {
        return this.noInterfaceLocalBeanClassNames;
    }
    
    
    public void addRemoteBusinessClassName(String className) {
        remoteBusinessClassNames.add(className);
    }

    public void addLocalBusinessClassName(String className) {
        localBusinessClassNames.add(className);
    }

    /**
     * Returns the set of remote business interface names for this ejb.
     * If the bean does not expose a remote business view, return a set
     * of size 0.
     */
    public Set<String> getRemoteBusinessClassNames() {
        return new HashSet<String>( remoteBusinessClassNames );
    }

    /**
     * Returns the set of local business interface names for this ejb.
     * If the bean does not expose a local business view, return a set
     * of size 0.
     */
    public Set<String> getLocalBusinessClassNames() {
        return new HashSet<String>( localBusinessClassNames );
    }

    public void setWebServiceEndpointInterfaceName(String name) {
        this.webServiceEndpointInterfaceName = name;
    }

    public String getWebServiceEndpointInterfaceName() {
        return webServiceEndpointInterfaceName;
    }

	/**
	* Return the JNDI name which will be assigned to the ejb home object ar runtime.
	*/
    public String getJndiName() {
	if (this.jndiName == null) {
	    this.jndiName = "";
	}
	return (jndiName != null && jndiName.length() > 0)?
                jndiName : getMappedName();
    }
   
	/**
	* Sets the JNDI name which will be assigned to the ejb home object ar runtime.
	*/
    public void setJndiName(String jndiName) {
	this.jndiName = jndiName;
	if (this.getName().equals("")) {
	    super.setName(jndiName);
	}

    }    

    public String getMappedName() {
        return (mappedName != null)? mappedName : "";
    }

    public void setMappedName(String mappedName) {
        this.mappedName = mappedName;

    }

    /**
     * Marks this ejb as a LocalBean.
     */
    public void setLocalBean(boolean localBean) {
        this.localBean = localBean;
    }

	/**
	* Returns a formatted String of the attributes of this object.
	*/
    public void print(StringBuffer toStringBuffer) {
        super.print(toStringBuffer);
	toStringBuffer.append("\n homeClassName ").append(homeClassName);
	toStringBuffer.append("\n remoteClassName ").append(remoteClassName);
	toStringBuffer.append("\n remoteBusinessIntfs ").append(remoteBusinessClassNames).append("\n");
	toStringBuffer.append("\n localhomeClassName ").append(localHomeClassName);
	toStringBuffer.append("\n localClassName ").append(localClassName);
	toStringBuffer.append("\n localBusinessIntfs ").append(localBusinessClassNames);
        toStringBuffer.append("\n isLocalBean ").append(isLocalBean()).append("\n");
	toStringBuffer.append("\n jndiName ").append(jndiName).append("\n");
    }
    
    /**
     * @return true if the EJB described has a LocalHome/Local interface
     */
    public boolean isLocalInterfacesSupported() {
        return (getLocalHomeClassName() != null);
    }

    /**
     * @return true if the EJB has 1 or more local business interfaces
     */
    public boolean isLocalBusinessInterfacesSupported() {
        return (localBusinessClassNames.size() > 0);
    }
    
    /**
     * @return true if the EJB has a RemoteHome/Remote interface
     */
    public boolean isRemoteInterfacesSupported() {
        return (getHomeClassName() != null);        
    }

    /**
     * @return true if the EJB has 1 or more remote business interfaces
     */
    public boolean isRemoteBusinessInterfacesSupported() {
        return (remoteBusinessClassNames.size() > 0);
    }


    /**
     * @return true if this is an EJB that implements a web service endpoint.
     */
    public boolean hasWebServiceEndpointInterface() {
        return (getWebServiceEndpointInterfaceName() != null);        
    }

    /**
     * @return true if this is an EJB provides a no interface Local view.
     */
    public boolean isLocalBean() {
        return localBean;     
    }

}
    
