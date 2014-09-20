/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core.jws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

/**
 * Abstracts the OSGi configuration information so it can be served easily
 * via JNLP to Java Web Start.
 * 
 * @author tjquinn
 */
class LoaderConfigContent {

    private static final String OSGI_CONFIG_FILE_PATH = "config/osgi.properties";
    private final String content;
    
    LoaderConfigContent(final File installDir) throws FileNotFoundException, IOException {
        content = loadContent(configFileURI(installDir));
    }

    String content() {
        return content;
    }
    
    private String loadContent(final URI configFileURI) throws FileNotFoundException, IOException {
        final File configFile = new File(configFileURI);
        final FileReader fr = new FileReader(configFile);
        final StringBuilder sb = new StringBuilder();
        int charsRead;

        final char[] buffer = new char[1024];
        try {
            while ( (charsRead = fr.read(buffer)) != -1) {
                final String input = new String(buffer, 0, charsRead);
                sb.append(Util.toXMLEscapedInclAmp(input));
            }
            return sb.toString();
        } finally {
            fr.close();
        }
    }
    
    private URI configFileURI(final File installDir) {
        return installDir.toURI().resolve(OSGI_CONFIG_FILE_PATH);
    }
}
