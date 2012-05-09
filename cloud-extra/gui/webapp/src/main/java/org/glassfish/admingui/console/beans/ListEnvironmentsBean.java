/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.console.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import org.glassfish.admingui.console.rest.RestUtil;
import org.glassfish.admingui.console.util.DeployUtil;

/**
 *
 * @author anilam
 */
@ManagedBean (name="listEnvironmentsBean")
@ViewScoped
public class ListEnvironmentsBean implements Serializable {

    private List<Map> envList = new ArrayList();
    private boolean modelUpdated = false;

    private void ensureModel() {
        if (!modelUpdated) {
            updateModel();
            modelUpdated = true;
        }
    }

    public List<Map> getEnvsAndApps() {
        ensureModel();
        return envList;
    }


    private void updateModel() {
        String prefix = REST_URL+"/clusters/cluster/" ;
        try{
            List<String> clusterList = new ArrayList(RestUtil.getChildMap(prefix).keySet());
//            System.out.println("====== getEnvsAndApps --- clusterList: " + clusterList);
            if ( (clusterList != null) && (! clusterList.isEmpty())){
                for(String oneCluster : clusterList){
                    if (DeployUtil.isClusterAnEnvironment(oneCluster)){
                        List<String> apps = RestUtil.getChildNameList(prefix+oneCluster+"/application-ref");
//                        System.out.println("======== getEnvsAndApps : apps = " + apps );
                        Map env = new HashMap();
                        env.put("clusterName", oneCluster);
                        env.put("appList", apps);
                        envList.add(env);
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public static final String REST_URL = "http://localhost:4848/management/domain";
}