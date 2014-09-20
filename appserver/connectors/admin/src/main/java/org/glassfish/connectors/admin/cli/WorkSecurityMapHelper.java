/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import org.glassfish.connectors.config.WorkSecurityMap;

import java.util.List;

public class WorkSecurityMapHelper {
    static boolean doesResourceAdapterNameExist(String raName, Resources resources) {
        //check if the resource adapter exists.If it does not then throw an exception.
        boolean doesRAExist = false;
        for (Resource resource : resources.getResources()) {
            if (resource instanceof WorkSecurityMap) {
                if (((WorkSecurityMap) resource).getResourceAdapterName().equals(raName)) {
                    doesRAExist = true;
                    break;
                }
            }
        }
        return doesRAExist;
    }

    static boolean doesMapNameExist(String raName, String mapname, Resources resources) {
        //check if the mapname exists for the given resource adapter name..
        List<WorkSecurityMap> maps = ConnectorsUtil.getWorkSecurityMaps(raName, resources);

        boolean doesMapNameExist = false;
        if (maps != null) {
            for (WorkSecurityMap wsm : maps) {
                String name = wsm.getName();
                if (name.equals(mapname)) {
                    doesMapNameExist = true;
                    break;
                }
            }
        }
        return doesMapNameExist;
    }

    static WorkSecurityMap getSecurityMap(String mapName, String raName, Resources resources) {
        List<WorkSecurityMap> maps = ConnectorsUtil.getWorkSecurityMaps(raName, resources);
        WorkSecurityMap map = null;
        if (maps != null) {
            for (WorkSecurityMap wsm : maps) {
                if (wsm.getName().equals(mapName)) {
                    map = wsm;
                    break;
                }
            }
        }
        return map;
    }

}
