/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.internal.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.hk2.bootstrap.DescriptorFileFinder;

/**
 * This DescriptorFileFinder is used to find all of the META-INF/hk2-locator/application files
 * in the application
 * 
 * @author jwells
 *
 */
public class ApplicationDescriptorFileFinder implements DescriptorFileFinder {
    
    private final String resourceName;
    private final ClassLoader loaderToUse;
    
    /* package */
    ApplicationDescriptorFileFinder(ClassLoader loaderToUse, String resourceName) {
        this.resourceName = resourceName;
        this.loaderToUse = loaderToUse;
    }

    @Override
    public List<InputStream> findDescriptorFiles() throws IOException {
        Enumeration<URL> urls = loaderToUse.getResources(resourceName);

        LinkedList<InputStream> retVal = new LinkedList<InputStream>();
        
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            
            retVal.add(url.openStream());
        }
                
        return retVal;
    }

}
