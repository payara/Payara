/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.common;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.archivist.ExtensionsArchivist;
import java.io.IOException;
import java.util.ArrayList;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PostConstruct;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;
import org.xml.sax.SAXParseException;

/**
 * AppClientArchivist that does not warn if both the GlassFish and the
 * legacy Sun runtime descriptors are present.
 * <p>
 * The ACC uses a MultiReadableArchive to essentially merge the contents of
 * the generated app client JAR with the developer's original app client JAR.
 * The generated file contains a generated GlassFish runtime descriptor.
 * If the developer's app client contains a legacy sun-application-client.xml
 * descriptor, then the normal archivist logic would detect that both the
 * GlassFish DD and the developer's legacy sun-application-client.xml were
 * present in the merged contents and it would log a warning.
 * <p>
 * We prevent such warnings by overriding the method which reads the runtime
 * deployment descriptor.
 * 
 * @author Tim Quinn
 */
@Service
@PerLookup
public class ACCAppClientArchivist extends AppClientArchivist implements PostConstruct {

    @Inject @Optional
    IterableProvider<ExtensionsArchivist> allExtensionArchivists;

    @Override
    public void readRuntimeDeploymentDescriptor(ReadableArchive archive, ApplicationClientDescriptor descriptor) throws IOException, SAXParseException {
        super.readRuntimeDeploymentDescriptor(archive, descriptor, false);
    }

    public void postConstruct() {
        extensionsArchivists = new ArrayList<ExtensionsArchivist>();
        for (ExtensionsArchivist extensionArchivist : allExtensionArchivists) {
            if (extensionArchivist.supportsModuleType(getModuleType())) {
                extensionsArchivists.add(extensionArchivist);    
            }
        }
    }
}
