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

package com.sun.enterprise.registration.impl;

import com.sun.enterprise.registration.RegistrationService;
import com.sun.enterprise.registration.RegistrationException;
import com.sun.enterprise.registration.RegistrationAccount;
import com.sun.enterprise.registration.RegistrationDescriptor;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.File;
import java.util.logging.Logger;

public class SysnetRegistrationService implements RegistrationService {
        
    /* Creates a new service which persists the registration data in the specified File.
     * Main entry point for application server code doing registry 
     * @param localRepositoryFile a File object for the local repository file
     */
    public SysnetRegistrationService(File localRepositoryFile) {
        this.localRepositoryFile = localRepositoryFile;
    }
    
    public boolean isRegistrationEnabled() {
        // hack to disable registration on AIX. The sysnet registration APIs do not work on AIX.
         if (AIX.equalsIgnoreCase(System.getProperty("os.name")))
             return false;
        return localRepositoryFile.canWrite();
    }
    
    /*  Registers the generated ServiceTags to SunConnection backend */
    public void register(RegistrationAccount account) 
        throws RegistrationException, ConnectException, UnknownHostException {
        throw new RuntimeException("Not supported");
    }
    
    /* Creates a Sun Online Account 
     * @param soa SunOnlineAccount object
     * throws RegistrationException if the online account could not be created.
     */
    public void createRegistrationAccount(RegistrationAccount soa) 
            throws RegistrationException {
        throw new RuntimeException("Not supported");
    }
    
    public List getRegistrationDescriptors() throws RegistrationException {
        RepositoryManager rm = getRepositoryManager();
        // make sure runtime values are generated in RepositoryManager
        rm.updateRuntimeValues();
        return rm.getServiceTags();
    }

    public List getRegistrationDescriptors(String productURN) throws RegistrationException {
        List<ServiceTag> st1 = getRegistrationDescriptors();
        List<ServiceTag> st2 = new ArrayList();
        for (int i = 0; i < st1.size(); i++) {
            ServiceTag st = st1.get(i);
            if (st.getProductURN().equals(productURN))
                st2.add(st);
        }
        return st2;
    }
    
    public List getRegistrationDescriptors(RegistrationDescriptor.RegistrationStatus status) throws RegistrationException {
        List<ServiceTag> st1 = getRegistrationDescriptors();
        List<ServiceTag> st2 = new ArrayList();
        RepositoryManager rm = getRepositoryManager();
        for (int i = 0; i < st1.size(); i++) {
            ServiceTag st = st1.get(i);
            if (rm.getRegistrationStatus(st).equals(status))
                st2.add(st);
        }
        return st2;
    }

    /* read the registration reminder from local persistent store */
    public RegistrationReminder getRegistrationReminder() throws RegistrationException {
        return getRepositoryManager().getRegistrationReminder();
    }
    
    /* set the registration reminder to local persistent store */
    public void setRegistrationReminder(RegistrationReminder reminder) throws RegistrationException {        
        getRepositoryManager().setRegistrationReminder(reminder);
    }

    /* read the registration status from local persistent store */
    public RegistrationStatus getRegistrationStatus() throws RegistrationException {
        return getRepositoryManager().getRegistrationStatus();
    }

    /* set the registration status to local persistent store */
    public void setRegistrationStatus(RegistrationStatus status) throws RegistrationException {        
        getRepositoryManager().setRegistrationStatus(status);
    }
   

    /**
     * Transfers locally-registered service tags that have not yet been transferred
     * to the SysNet repository to the SysNet repository using the stclient 
     * utility and also updates the service tag's status in the private
     * repository to record the transfer.
     * 
     * @throws RegistrationException for errors reading the local repository or
     * invoking stclient to transfer them
     */
    public void transferEligibleServiceTagsToSysNet() throws RegistrationException {
        
        SysnetTransferManager transferManager = 
                new SysnetTransferManager(localRepositoryFile);
        
        transferManager.transferServiceTags();
    }

    private RepositoryManager getRepositoryManager() throws RegistrationException {
        return new RepositoryManager(localRepositoryFile);
    }
    
    private final File localRepositoryFile;
    private static final String AIX = "AIX";
    private static final Logger logger = RegistrationLogger.getLogger();
}
