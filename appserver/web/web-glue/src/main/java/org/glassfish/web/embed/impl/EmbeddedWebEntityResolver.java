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

package org.glassfish.web.embed.impl;

import org.apache.catalina.startup.Constants;
import org.glassfish.internal.api.ServerContext;
import javax.inject.Inject;
import org.glassfish.hk2.api.PostConstruct;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * For Embedded GlassFish, override loading of known DTDs via
 * getClass().getResource() whenever there is no installRoot/lib/dtds
 * to avoid fetching the DTDs via HttpUrlConnection.
 *
 * @author bhavanishankar@dev.java.net
 * @see org.glassfish.web.WebEntityResolver#resolveEntity(String, String)
 */
//@Service(name="web")
//@ContractsProvided({EmbeddedWebEntityResolver.class, EntityResolver.class})
public class EmbeddedWebEntityResolver implements EntityResolver, PostConstruct {

    @Inject
    ServerContext serverContext;

    private File dtdDir;

    public static final Map<String/*public id*/, String/*bare file name*/> knownDTDs =
            new HashMap<String, String>();

    static {
        knownDTDs.put(Constants.TldDtdPublicId_11, "web-jsptaglibrary_1_1.dtd");
        knownDTDs.put(Constants.TldDtdPublicId_12, "web-jsptaglibrary_1_2.dtd");
        knownDTDs.put(Constants.WebDtdPublicId_22, "web-app_2_2.dtd");
        knownDTDs.put(Constants.WebDtdPublicId_23, "web-app_2_3.dtd");
    }

    public void postConstruct() {
        if (serverContext != null) {
            File root = serverContext.getInstallRoot();
            File libRoot = new File(root, "lib");
            dtdDir = new File(libRoot, "dtds");
        }
    }

    /**
     * Fetch the DTD via getClass().getResource() if the DTD is not
     *
     * @param publicId
     * @param systemId
     * @return
     * @throws SAXException
     * @throws IOException
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        InputSource resolvedEntity = __resolveEntity(publicId, systemId);
        if (resolvedEntity == null) {
            String fileName = knownDTDs.get(publicId);
            URL url = this.getClass().getResource("/dtds/" + fileName);
            InputStream stream = url != null ? url.openStream() : null;
            if (stream != null) {
                resolvedEntity = new InputSource(stream);
                resolvedEntity.setSystemId(url.toString());
            }
        }
        return resolvedEntity;
    }

    /**
     * Try to fetch DTD from installRoot. Copied from org.glassfish.web.WebEntityResolver
     */
    public InputSource __resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String fileName = knownDTDs.get(publicId);
        if (fileName != null && dtdDir != null) {
            File dtd = new File(dtdDir, fileName);
            if (dtd.exists()) {
                return new InputSource(dtd.toURI().toString());
            }
        }
        return null;
    }

}
