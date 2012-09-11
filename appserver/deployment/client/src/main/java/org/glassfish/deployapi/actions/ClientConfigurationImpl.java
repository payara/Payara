/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployapi.actions;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.deploy.spi.exceptions.ClientExecuteException;
import javax.enterprise.deploy.spi.status.ClientConfiguration;
import javax.enterprise.deploy.spi.TargetModuleID;

import org.glassfish.deployapi.TargetImpl;
import org.glassfish.deployapi.TargetModuleIDImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;


/**
 * This implementation of the ClientConfiguration interface allow
 * for limited support of Application Client
 *
 * @author Jerome Dochez
 */
public class ClientConfigurationImpl implements ClientConfiguration {
    
    TargetModuleIDImpl targetModuleID; // TODO neither transient or Serializable we need to choose one or the other
    String originalArchivePath;
    
    private static LocalStringManagerImpl localStrings =
	  new LocalStringManagerImpl(ClientConfigurationImpl.class);    
    
    /** Creates a new instance of ClientConfigurationImpl */
    public ClientConfigurationImpl(TargetModuleIDImpl targetModuleID) {
        this.targetModuleID = targetModuleID;
    }
                
    /** This method performs an exec and starts the
     * application client running in another process.
     *
     * @throws ClientExecuteException when the configuration
     *         is incomplete.
     */
    public void execute() throws ClientExecuteException {
        if (targetModuleID==null) {
            throw new ClientExecuteException(localStrings.getLocalString(
                "enterprise.deployapi.actions.clientconfigurationimpl.nomoduleid", 
                "No moduleID for deployed application found"));
        }
        TargetImpl target = (TargetImpl) targetModuleID.getTarget();
        String moduleID;
        if (targetModuleID.getParentTargetModuleID()!=null) {            
            moduleID = targetModuleID.getParentTargetModuleID().getModuleID();
        } else {
            moduleID = targetModuleID.getModuleID();
        }
        
        
        try {
            // retrieve the stubs from the server
            String location = target.exportClientStubs(moduleID, System.getProperty("java.io.tmpdir"));
       
            // invoke now the appclient...
            String j2eeHome = System.getProperty("com.sun.aas.installRoot");
            String appClientBinary = j2eeHome + File.separatorChar + "bin" + File.separatorChar + "appclient";
            String command = appClientBinary + " -client " + location;
            
            Runtime.getRuntime().exec(command);
            
        } catch(Exception e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "Error occurred", e); 
            throw new ClientExecuteException(localStrings.getLocalString(
                "enterprise.deployapi.actions.clientconfigurationimpl.exception", 
                "Exception while invoking application client : \n {0}", new Object[] { e.getMessage() }));
        }
    }
}
