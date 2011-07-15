/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.server;


import com.sun.enterprise.module.common_impl.LogHelper;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * DTD resolver used when parsing the domain.xml and resolve to local DTD copies
 *
 * @author Jerome Dochez
 * @Deprecated
 */

@Deprecated
public class   DomainResolver implements EntityResolver {
    public InputSource resolveEntity(String publicId, String systemId) {
        
        if (systemId.startsWith("http://www.sun.com/software/appserver/")) {
            // return a special input source
            String fileName = systemId.substring("http://www.sun.com/software/appserver/".length());
            File f = new File(System.getProperty("com.sun.aas.installRoot"));
            f = new File(f, "lib");
            f = new File(f, fileName.replace('/', File.separatorChar));
            if (f.exists()) {
                try {
                    return new InputSource(new BufferedInputStream(new FileInputStream(f)));
                } catch(IOException e) {
                    LogHelper.getDefaultLogger().log(Level.SEVERE, "Exception while getting " + fileName + " : ", e);
                    return null;
                }
            } else {
                System.out.println("Cannot find " + f.getAbsolutePath());
                return null;
            }
            //MyReader reader = new MyReader();
            //return new InputSource(reader);
        } else {
            // use the default behaviour
            return null;
        }
    }
}
