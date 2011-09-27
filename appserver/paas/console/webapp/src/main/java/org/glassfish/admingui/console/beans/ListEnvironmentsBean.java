/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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