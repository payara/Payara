/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deploy.shared;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.deployment.common.DeploymentUtils;
import java.io.IOException;
import java.util.jar.Manifest;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLResolver;

import org.glassfish.internal.deployment.GenericHandler;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Common methods for ArchiveHandler implementations
 *
 * @author Jerome Dochez
 */
public abstract class AbstractArchiveHandler extends GenericHandler {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Exception while getting manifest classpath: ", level="WARNING")
    private static final String MANIFEST_CLASSPATH_ERROR = "NCLS-DEPLOYMENT-00024";
    private static XMLInputFactory xmlInputFactory;

    static {
        xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        // set an zero-byte XMLResolver as IBM JDK does not take SUPPORT_DTD=false
        // unless there is a jvm option com.ibm.xml.xlxp.support.dtd.compat.mode=false
        xmlInputFactory.setXMLResolver(new XMLResolver() {
                @Override
                public Object resolveEntity(String publicID,
                        String systemID, String baseURI, String namespace)
                        throws XMLStreamException {

                    return new ByteArrayInputStream(new byte[0]);
                }
            });
    }
    
    public List<URL> getManifestLibraries(DeploymentContext context) {
        try {
            Manifest manifest = getManifest(context.getSource());
            return DeploymentUtils.getManifestLibraries(context, manifest);
        }catch (IOException ioe) {
            deplLogger.log(Level.WARNING,
                           MANIFEST_CLASSPATH_ERROR,
                           ioe);
            return new ArrayList<URL>();
        }
    }

    protected static XMLInputFactory getXMLInputFactory() {
        return xmlInputFactory;
    }
}
