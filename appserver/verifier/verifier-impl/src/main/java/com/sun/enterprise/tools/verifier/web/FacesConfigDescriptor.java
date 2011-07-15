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

package com.sun.enterprise.tools.verifier.web;

import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.FileArchive;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

/**
 *
 * Class which represents WEB-INF/faces-config.xml
 *
 * @author bshankar@sun.com
 *
 */
public class FacesConfigDescriptor {
    
    private final String MANAGED_BEAN_CLASS = "managed-bean-class"; // NOI18N
    private final String facesConfigFileName = "WEB-INF/faces-config.xml"; // NOI18N
    
    private VerifierTestContext context;
    private Document facesConfigDocument;
    
    public FacesConfigDescriptor(VerifierTestContext context, WebBundleDescriptor descriptor) {
        try {
            this.context = context;
            readFacesConfigDocument(descriptor);
        } catch(Exception ex) {
            facesConfigDocument = null;
        }
    }
    
    private void readFacesConfigDocument(WebBundleDescriptor webd) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        ModuleDescriptor moduleDesc = webd.getModuleDescriptor();
        String archBase = context.getAbstractArchive().getURI().toString();
        String uri = null;
        if(moduleDesc.isStandalone()){
            uri = archBase;
        } else {
            uri = archBase + File.separator +
                    FileUtils.makeFriendlyFilename(moduleDesc.getArchiveUri());
        }
        FileArchive arch = new FileArchive();
        arch.open(URI.create(uri));
        InputStream is = arch.getEntry(facesConfigFileName);
        InputSource source = new InputSource(is);
        try {
            facesConfigDocument = builder.parse(source);
        } finally {
            try{
                if(is != null)
                    is.close();
            } catch(Exception e) {}
        }
    }
    
    public List<String> getManagedBeanClasses() {
        if (facesConfigDocument == null) {
            return new ArrayList<String>();
        }
        NodeList nl = facesConfigDocument.getElementsByTagName(MANAGED_BEAN_CLASS);
        List<String> classes = new ArrayList<String>();
        if (nl != null) {
            int size = nl.getLength();
            for (int i = 0; i < size; i++) {
                classes.add(nl.item(i).getFirstChild().getNodeValue().trim());
            }
        }
        return classes;
    }
    
}
