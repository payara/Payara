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

package org.glassfish.web;

import com.sun.enterprise.util.MapBuilder;
import org.apache.catalina.startup.Constants;
import org.glassfish.internal.api.ServerContext;
import org.jvnet.hk2.annotations.ContractsProvided;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * {@link EntityResolver} that recognizes known public IDs of JavaEE DTDs/schemas
 * and return a local copy.
 *
 * <p>
 * This implementation assumes that those files are available in
 * <tt>$INSTALL_ROOT/lib/schemas</tt> and <tt>$INSTALL_ROOT/lib/dtds</tt>,
 * but in different environment, different implementation can be plugged in
 * to perform entirely different resolution.
 *
 * @author Kohsuke Kawaguchi
 */
@Service(name="web")
@ContractsProvided({WebEntityResolver.class, EntityResolver.class})
public class WebEntityResolver implements EntityResolver, PostConstruct {
    @Inject
    ServerContext serverContext;

    private File dtdDir;

    /**
     * Known DTDs.
     *
     * Expose the map so that interested party can introspect the table value and modify them. 
     */
    public final Map<String/*public id*/,String/*bare file name*/> knownDTDs = new MapBuilder<String,String>()
            .put(Constants.TldDtdPublicId_11,"web-jsptaglibrary_1_1.dtd")
            .put(Constants.TldDtdPublicId_12,"web-jsptaglibrary_1_2.dtd")
            .put(Constants.WebDtdPublicId_22,"web-app_2_2.dtd")
            .put(Constants.WebDtdPublicId_23,"web-app_2_3.dtd")
            .build();

    public void postConstruct() {
        File root = serverContext.getInstallRoot();
        File libRoot = new File(root, "lib");
        dtdDir = new File(libRoot, "dtds");
    }

    /**
     * If the parser hits one of the well-known DTDs, parse local copies instead of hitting
     * the remote server.
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        String fileName = knownDTDs.get(publicId);
        if(fileName!=null) {
            File dtd = new File(dtdDir,fileName);
            if(dtd.exists())
                return new InputSource(dtd.toURI().toString());
        }

        return null;
    }
}
