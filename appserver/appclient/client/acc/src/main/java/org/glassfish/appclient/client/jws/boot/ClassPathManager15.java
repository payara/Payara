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

package org.glassfish.appclient.client.jws.boot;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Class Path manager for Java Web Start-aware ACC running under Java runtime 1.5.x.
 *
 * @author tjquinn
 */
public class ClassPathManager15 extends ClassPathManager {
    
    /**
     *Return a new instance of the manager
     *@param loader the Java Web Start-provided class loader
     */
    protected ClassPathManager15(ClassLoader loader, boolean keepJWSClassLoader) {
        super(loader, keepJWSClassLoader);
    }
    
    public ClassLoader getParentClassLoader() {
        return (keepJWSClassLoader() ? getJnlpClassLoader() : null);
    }

    public File findContainingJar(URL resourceURL) throws IllegalArgumentException, URISyntaxException {
        File result = null;
        if (resourceURL != null) {
            URI uri = resourceURL.toURI();
            String scheme = uri.getScheme();
            String ssp = uri.getSchemeSpecificPart();
            if (scheme.equals("jar")) {
                /*
                 *The scheme-specific part will look like "file:<file-spec>!/<path-to-class>.class"
                 *so we need to isolate the scheme and the <file-spec> part.  
                 *The subscheme (the scheme within the jar) precedes the colon
                 *and the file spec appears after it and before the exclamation point.
                 */
                int colon = ssp.indexOf(':');
                int excl = ssp.indexOf('!');
                String containingJarPath = ssp.substring(colon + 1, excl);
                result = new File(containingJarPath);
            } else {
                throw new IllegalArgumentException(resourceURL.toExternalForm());
            }
        }
        return result;
    }
    

}
