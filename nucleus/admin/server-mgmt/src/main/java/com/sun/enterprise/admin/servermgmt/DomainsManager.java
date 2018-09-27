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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt;

import java.util.BitSet;

public interface DomainsManager
{       
    /**
     * In SE/EE we need an admin user/password that the DAS can use to authenticate to 
     * Node Agents and servers in the domain. This is not the case in PE; hence
     * this flag -- DomainConfig.K_FLAG_START_DOMAIN_NEEDS_ADMIN_USER
     * 
     * In SE/EE we need an extra non secure http port to host the Lockhart components 
     * which is controlled by -- DomainConfig.K_FLAG_CREATE_DOMAIN_NEEDS_ALTERNATE_ADMIN_PORT
     * @return flags toggling SE/EE specific behavior.
     */    
    public BitSet getDomainFlags();
    
    /**
     * SE/EE supports NSS as its native SSL database.NSS is capable of supporting multiple
     * slots (e.g.for different SSL hardware devices, smartcards, etc).Each device 
     * needs a specific password which the CLI must prompt for.
     * @param config the config to get the options from
     * @return the extra options
     * @throws DomainException if an error occurred
     */
     public String[] getExtraPasswordOptions(DomainConfig config)
        throws DomainException;
           
    
    /**
     * Deletes a domain identified by the given name.
     * (Should we stop the DAS and instances administered by this domain before
     * deleting the domain?)     
     * @param domainConfig
     * @throws DomainException  This exception is thrown if 
     * <ul>
     * - the domain does not exist.
     * - an exception occurred while deleting the domain.
     * </ul>
     */
    public void deleteDomain(DomainConfig domainConfig) 
        throws DomainException;
    
    /**
     * Lists all the domains.
     * @param domainConfig
     * @return an array of all the domain names
     * @throws DomainException if an exception occured
     */
    public String[] listDomains(DomainConfig domainConfig)
        throws DomainException;
    
    /**
     * Changes the master password for the domain
     * @param domainConfig the config for the domain to change
     * @throws DomainException
     */    
    public void changeMasterPassword(DomainConfig domainConfig) 
        throws DomainException;
    
    public void validateDomain(DomainConfig domainConfig, boolean domainExists)
        throws DomainException;
    
    /**
     * Checks that the master password in the config is valid for the keystore
     * @param config the config to get the password from
     * @throws DomainException if the password fails to open the keystore
     */
    public void validateMasterPassword(DomainConfig config) throws DomainException;
        
}   
