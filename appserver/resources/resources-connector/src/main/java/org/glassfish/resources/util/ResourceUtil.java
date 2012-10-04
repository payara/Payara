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

package org.glassfish.resources.util;

import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.api.ServiceLocator;

import java.io.IOException;
import java.util.Enumeration;

/**
 * @author Jagadish Ramu
 */
public class ResourceUtil {

    private static final String RESOURCES_XML_META_INF = "META-INF/glassfish-resources.xml";
    private static final String RESOURCES_XML_WEB_INF = "WEB-INF/glassfish-resources.xml";


    public static boolean hasResourcesXML(ReadableArchive archive, ServiceLocator locator){
        boolean hasResourcesXML = false;
        try{
            if(DeploymentUtils.isArchiveOfType(archive, DOLUtils.earType(), locator)){
                //handle top-level META-INF/glassfish-resources.xml
                if(archive.exists(RESOURCES_XML_META_INF)){
                    return true;
                }

                //check sub-module level META-INF/glassfish-resources.xml and WEB-INF/glassfish-resources.xml
                Enumeration<String> entries = archive.entries();
                while(entries.hasMoreElements()){
                    String element = entries.nextElement();
                    if(element.endsWith(".jar") || element.endsWith(".war") || element.endsWith(".rar") ||
                            element.endsWith("_jar") || element.endsWith("_war") || element.endsWith("_rar")){
                        ReadableArchive subArchive = archive.getSubArchive(element);
                        boolean answer = (subArchive != null && hasResourcesXML(subArchive, locator));
                        if (subArchive != null) {
                            subArchive.close();
                        }
                        if (answer) {
                            return true;
                        }
                   }
                }
            }else{
                if(DeploymentUtils.isArchiveOfType(archive, DOLUtils.warType(), locator)){
                    return archive.exists(RESOURCES_XML_WEB_INF);
                }else {
                    return archive.exists(RESOURCES_XML_META_INF);
                }
            }
        }catch(IOException ioe){
            //ignore
        }
        return hasResourcesXML;
    }
}
