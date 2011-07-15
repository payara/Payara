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

package com.sun.enterprise.deployment.types;

import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.InjectionCapable;

/** 
 * Protocol associated with defining an EJB Interface
 *
 * @author Jerome Dochez
 */

public interface EjbReference  extends NamedInformation, InjectionCapable {
    
    /** 
     * Get the type of the EJB (Session, Entity or Message-Driven). 
     * @return the type of the EJB.
     */
    public String getType();

    /**
     * Set the type of the EJB. Allowed values are Session, Entity or
     * Message-driven.
     * @param the type of the EJB.
     */
    public void setType(String type);

    /** 
     * Gets the home classname of the referee EJB. 
     * @return the class name of the EJB home.
     */
    public String getEjbHomeInterface();

    /** 
     * Sets the local or remote home classname of the referee EJB. 
     * @param the class name of the EJB home.
     */
    public void setEjbHomeInterface(String ejbHomeInterface);

    /** 
     * Gets the local or remote interface classname of the referee EJB. 
     * @return the classname of the EJB remote object.
     */
    public String getEjbInterface();

    /** 
     * Sets the local or remote bean interface classname of the referee EJB. 
     * @param the classname of the EJB remote object.
     */
    public void setEjbInterface(String ejbInterface);
    
    /** 
     * Gets the link name of the reference. For use when linking to an EJB 
     * within a J2EE application. 
     * @return the link name.
     */
    public String getLinkName();

    /** 
     * Sets the link name of the reference. For use when linking to an EJB 
     * within a J2EE application. 
     * @param the link name.
     */
    public void setLinkName(String linkName);
    
    /**
     * Tests if the reference to the referree EJB is through local or 
     * remote interface
     * @return true if using the local interfaces
     */
    public boolean isLocal();
    
    /**
     * Sets whether the reference uses the local or remote interfaces of 
     * the referring EJB
     * @param true if using the local interface
     */
    public void setLocal(boolean isLocal);
 
    /**
     * Set the referring bundle, i.e. the bundle within which this
     * EJB reference is declared. 
     */
    public void setReferringBundleDescriptor(BundleDescriptor referringBundle);

    /**
     * Get the referring bundle, i.e. the bundle within which this
     * EJB reference is declared.  
     */
    public BundleDescriptor getReferringBundleDescriptor();
    
    /**
     * Set the jndi name for this ejb reference
     */
    public void setJndiName(String jndiName);
    
    /**
     * @return the jndi name for this ejb reference
     */
    public String getJndiName();

    public boolean hasJndiName();

    public boolean hasLookupName();
    public String getLookupName();
    
    public EjbDescriptor getEjbDescriptor();
    public void setEjbDescriptor(EjbDescriptor descriptor);

    /**
     * @return true if the EJB reference is a 30 client view
     */
    public boolean isEJB30ClientView();

}

