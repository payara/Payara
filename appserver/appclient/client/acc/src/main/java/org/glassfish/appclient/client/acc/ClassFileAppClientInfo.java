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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.glassfish.apf.AnnotationProcessorException;
import org.glassfish.api.deployment.archive.ReadableArchive;

/**
 * Represents an app client specified by a .class file on the command line.
 * @author tjquinn
 */
public class ClassFileAppClientInfo extends AppClientInfo {
    
    /** the class file name specified on the command line */
    private String classFileFromCommandLine;
    
    /**
     *Creates a new instance of the class file app client info.
     *@param isJWS whether Java Web Start was used to launch the app client
     *@param logger the Logger available for writing log messages
     *@param archive the archive containing the app client (and perhaps other files as well)
     *@param archivist the archivist appropriate to the type of archive being processed
     *@param mainClassFromCommandLine the main class command-line argument value
     *@param classFileFromCommandLine the class file name from the command line arguments
     */
    protected ClassFileAppClientInfo(
            boolean isJWS, Logger logger, String mainClassFromCommandLine, 
            String classFileFromCommandLine) {
        super(isJWS, logger, mainClassFromCommandLine);
        this.classFileFromCommandLine = classFileFromCommandLine;
    }

    @Override
    protected String getMainClassNameToRun(ApplicationClientDescriptor acDescr) {
        return classFileFromCommandLine;
    }

    @Override
    protected void massageDescriptor()
            throws IOException, AnnotationProcessorException {
        ApplicationClientDescriptor appClient = getDescriptor();
        appClient.setMainClassName(classFileFromCommandLine);
        appClient.getModuleDescriptor().setStandalone(true);
        FileArchive fa = new FileArchive();
        fa.open(new File(classFileFromCommandLine).toURI());
        new AppClientArchivist().processAnnotations(appClient, fa);
    }

//    @Override
//    protected ReadableArchive expand(File file)
//        throws IOException, Exception {
//        return archiveFactory.openArchive(file);
//    }

//    protected boolean deleteAppClientDir() {
//        return false;
//    }
}
