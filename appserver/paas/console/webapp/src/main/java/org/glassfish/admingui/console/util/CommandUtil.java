/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.util;

import java.util.*;

import org.glassfish.admingui.console.rest.RestUtil;

/**
 *
 * @author anilam
 */
public class CommandUtil {
    public static String SERVICE_TYPE_RDMBS = "Database";
    public static final String SERVICE_TYPE_JAVAEE = "JavaEE";
    public static final String SERVICE_TYPE_LB = "LB";

    static final String REST_URL="http://localhost:4848/management/domain";

    /**
     *	<p> This method returns the list of Services. </p>
     *
     *	@param	appName	    Name of Application. This is optional parameter, can be set to NULL.
     *	@param	type        Service type. Possible value is "Cluster", "ClusterInstance", "database", "load_balancer", "JavaEE"
     *                      This is optional parameter, can be set to Null.
     *  @param  scope       Scope of services.  Possible value is "external", "shared", "application".
     *                      This is optional parameter, can be set to NULL.
     *
     *	@return	<code>List<Map></code>  Each map represents one Service.
     */
    public static List<Map> listServices(String appName, String type, String scope){

        List<Map>services = null;
        //String endpoint = GuiUtil.getSessionValue("REST_URL")+"/list-services";
        String endpoint = REST_URL+"/list-services";

        Map attrs = new HashMap();
        putOptionalAttrs(attrs, "appname", appName);
        putOptionalAttrs(attrs, "type", type);
        putOptionalAttrs(attrs, "scope", scope);


        services = RestUtil.getListFromREST(endpoint, attrs, "list");
        if (services == null){
            services = new ArrayList();
        }else {
            for(Map oneS : services){
                oneS.put("serviceName" , oneS.get("SERVICE-NAME"));
                oneS.put("serverType", oneS.get("SERVER-TYPE"));
                if ("Running".equals(oneS.get("STATE"))){
                    oneS.put("stateImage", "/images/running_small.gif");
                }
            }
        }
        //System.out.println("======== CommandUtil.listServices():  services = " + services);
        return services;
    }


    /**
     *	<p> This method returns the list of Names of the existing Templates. </p>
     *
     *	@param	serviceType Acceptable value is "JavaEE", "Database" "LoadBalancer".
     *                      If set to NULL, all service type will be returned.
     *	@return	<code>List<String></code>  Returns the list of names of the template.
     */
    public static List<String> getTemplateList(String type){

    //For now, since backend only supports one Virtualization setup, we will just return the list if anyone exist.
    //Later, probably need to pass in the virtualiztion type to this method.
        List<String> tList = new ArrayList();
        try{
            List<String> virts = RestUtil.getChildNameList(REST_URL+"/virtualizations");
            for(String virtType : virts){
                List<String> virtInstances = RestUtil.getChildNameList(REST_URL+"/virtualizations/" + virtType);
                if ( (virtInstances != null ) && (virtInstances.size() > 0)){
                    //get the templates for this V that is the same service type
                    String templateEndpoint = REST_URL+"/virtualizations/" + virtType + "/" + virtInstances.get(0) + "/template";
                    if (RestUtil.doesProxyExist(templateEndpoint )){
                        Map<String, String> templateEndpoints = RestUtil.getChildMap(templateEndpoint);
                        for(String oneT : templateEndpoints.keySet()){
                            Map<String, String> tempIndexes = RestUtil.getChildMap(templateEndpoints.get(oneT) + "/template-index");
                            for(String oneI : tempIndexes.keySet()){
                                Map attrs = RestUtil.getAttributesMap(tempIndexes.get(oneI));
                                if ("ServiceType".equals (attrs.get("type"))  &&  type.equals(attrs.get("value"))){
                                    //finally found it
                                    tList.add(oneT);
                                }
                            }
                        }
                    }
                }
            }
        }catch(Exception ex){

        }
        return tList;
    }

    /**
     *	<p> This method returns the list of of Services that is pre-selected by Orchestrator.  It is indexed by the serviceType.
     *  If a particular serviceType doesn't exist, it means the application doesn't require such service.  Service Configuration
     *  page in deployment wizard will not show that section.
     *
     *	@param	filePath  This is the absolute file path of the uploaded application.
     * 
     *	@return	raw meta data as returned by web service, e.g.
     *	<pre>
     *	{
     *    characteristics = {service-type = LB},
     *    init-type = lazy,
     *    name = basic_db_paas_sample-lb,
     *    service-type = LB,
     *  },
     *  {
     *    characteristics = {
     *      os-name = Linux,
     *      service-type = JavaEE,
     *    },
     *    configurations = {
     *      max.clustersize = 4,
     *      min.clustersize = 2
     *    },
     *    init-type = lazy,
     *    name = basic-db,
     *    service-type = JavaEE,
     *  },
     *  {
     *    characteristics = {
     *      os-name = 	Windows XP,
     *      service-type = Database,
     *    },
     *    init-type = lazy,
     *    name = default-derby-db-service,
     *    service-type = Database,
     *  }
     *	</pre>
    */
    public static List<Map<String, Object>> getPreSelectedServices(String filePath) {
        Map attrs = new HashMap();
        attrs.put("archive", filePath);
        try{
            Map appData = (Map) RestUtil.restRequest(REST_URL + "/applications/_get-service-metadata", attrs, "GET", null, null, false, true).get("data");
            List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) appData.get("extraProperties")).get("list");
            System.out.println("========== _get-service-metadata : " );
            System.out.println(list);
            return list;
        }catch(Exception ex){
            System.out.println("Exception occurs:");
            System.out.println(REST_URL + "/applications/_get-service-metadata");
            System.out.println("attrs = " + attrs);
            return new ArrayList();
        }
        
    }

    private static void putOptionalAttrs(Map attrs, String key, String value){
        if (!GuiUtil.isEmpty(value)){
            attrs.put(key, value);
        }
    }

}
