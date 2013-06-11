/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.EmbeddedSecurity;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.server.pluggable.SecuritySupport;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.embedded.EmbeddedFileSystem;
import org.glassfish.internal.embedded.EmbeddedLifecycle;
import org.glassfish.internal.embedded.Server;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Nithya Subramanian
 */

@Service
public class EmbeddedSecurityLifeCycle
        implements EmbeddedLifecycle{

    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    @Inject
    private EmbeddedSecurity embeddedSecurity;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private SecurityService securityService;

    @Override
    public void creation(Server server) {

        //If the instanceRoot is not set to a non-embedded GF install,
        //copy the security config files from the security.jar to the instanceRoot/config dir

        EmbeddedFileSystem fileSystem = server.getFileSystem();
        File instanceRoot = fileSystem.instanceRoot;
        if (instanceRoot == null) {
            return;
        }

        try {
            //Get the keyfile names from the security service
            List<String> keyFileNames = embeddedSecurity.getKeyFileNames(securityService);
            for(String keyFileName:keyFileNames) {
                //Copy the keyfiles in instanceRoot/config. If file is already present, then exit (handled by getManagedFile)
                FileUtils.getManagedFile("config" + File.separator + embeddedSecurity.parseFileName(keyFileName), instanceRoot);
            }
            //Copy the other security files to instanceRoot/config
            //Assuming that these files are present as config/filename in the embedded jar file and are to be extracted that way/
            FileUtils.getManagedFile("config" + File.separator + "login.conf", instanceRoot);
            FileUtils.getManagedFile("config" + File.separator + "server.policy", instanceRoot);
            FileUtils.getManagedFile("config" + File.separator + "cacerts.jks", instanceRoot);
            FileUtils.getManagedFile("config" + File.separator + "keystore.jks", instanceRoot);
            String keystoreFile = null;
            String truststoreFile = null;
            try {
                keystoreFile = Util.writeConfigFileToTempDir("keystore.jks").getAbsolutePath();
                truststoreFile = Util.writeConfigFileToTempDir("cacerts.jks").getAbsolutePath();
            } catch (IOException ex) {
                _logger.log(Level.SEVERE, SecurityLoggerInfo.obtainingKeyAndTrustStoresError, ex);
            }
            System.setProperty(SecuritySupport.keyStoreProp, keystoreFile);
            System.setProperty(SecuritySupport.trustStoreProp, truststoreFile);
        }catch(IOException ioEx) {
           _logger.log(Level.WARNING, SecurityLoggerInfo.copyingSecurityConfigFilesIOError, ioEx);
        }
    }

    @Override
    public void destruction(Server server) {

    }

}
