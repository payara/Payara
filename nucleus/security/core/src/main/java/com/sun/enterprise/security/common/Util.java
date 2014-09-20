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

package com.sun.enterprise.security.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.security.auth.callback.CallbackHandler;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ProcessEnvironment.ProcessType;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.embedded.Server;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

/**
 *
 * @author venu
 * TODO: need to change this class, it needs to be similar to SecurityServicesUtil
 */
@Service
@Singleton
public class Util {
    private static ServiceLocator habitat = Globals.getDefaultHabitat();
    
    @Inject 
    private ProcessEnvironment penv;
    
   
    //stuff required for AppClient
    private CallbackHandler callbackHandler;
    private Object appClientMsgSecConfigs;
    
    //Note: Will return Non-Null only after Util has been 
    //Injected in some Service.
    public static ServiceLocator getDefaultHabitat() {
        return habitat;
    }
    
    public static Util getInstance() {
        // return my singleton service
        return habitat.getService(Util.class);
    }
    
    public boolean isACC() {
        return penv.getProcessType().equals(ProcessType.ACC);
    }
    public boolean isServer() {
        return penv.getProcessType().isServer();
    }
    public boolean isNotServerOrACC() {
        return penv.getProcessType().equals(ProcessType.Other);
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public Object getAppClientMsgSecConfigs() {
        return appClientMsgSecConfigs;
    }

    public void setAppClientMsgSecConfigs(Object appClientMsgSecConfigs) {
        this.appClientMsgSecConfigs = appClientMsgSecConfigs;
    }
    
    public static boolean isEmbeddedServer() {
        List<String> servers = Server.getServerNames();
        if (!servers.isEmpty()) {
            return true;
        }
        return false;
    }
    
    public static File writeConfigFileToTempDir(String fileName) throws IOException {
        File filePath = new File(fileName);

        if (filePath.exists()) {
            //the string provided is a filepath, so return
            return filePath;
        }
        File localFile = null;
        //Parent directories until the fileName exist, so create the file that has been provided
        if (filePath.getParentFile() != null && filePath.getParentFile().exists()) {
            localFile = filePath;
            if(!localFile.createNewFile()) {
                throw new IOException();
            }

        } else {
            /*
             * File parent directory does not exist - so create parent directory as user.home/.glassfish-{embedded}/config
             * */
            String userHome = System.getProperty("user.home");

            String embeddedServerName = getCurrentEmbeddedServerName();
            File tempDir = new File(userHome + File.separator + ".glassfish4-"+embeddedServerName+File.separator + "config");
            boolean mkDirSuccess = true;
            if (!tempDir.exists()) {
                mkDirSuccess = tempDir.mkdirs();
            }

            localFile = new File(tempDir.getAbsolutePath()+File.separator + fileName);


            if (mkDirSuccess && !localFile.exists()) {
                localFile.createNewFile();
            }
        }
        FileOutputStream oStream = null;
        InputStream iStream = null;
        try {
            oStream = new FileOutputStream(localFile);
            iStream = Util.class.getResourceAsStream("/config/" + fileName);

            while (iStream != null && iStream.available() > 0) {
                oStream.write(iStream.read());
            }
        } finally {
	    if (oStream != null) {
                oStream.close();
	    }
            if  (iStream != null) {
                iStream.close();
            }

        }

        return localFile;

    }

    public static String getCurrentEmbeddedServerName() {
        List<String> embeddedServerNames = Server.getServerNames();
        String embeddedServerName = (embeddedServerNames.get(0) == null) ? "embedded" : embeddedServerNames.get(0);
        return embeddedServerName;

    }
    
}
