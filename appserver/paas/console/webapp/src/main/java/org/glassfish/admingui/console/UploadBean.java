/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.console;


import java.io.File;
import java.util.*;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import com.sun.faces.taglib.jsf_core.SelectItemTag;
import org.apache.myfaces.trinidad.model.UploadedFile;
import org.glassfish.admingui.console.event.DragDropEvent;
import org.glassfish.admingui.console.rest.JSONUtil;
import org.glassfish.admingui.console.rest.RestUtil;
import org.glassfish.admingui.console.util.CommandUtil;
import org.glassfish.admingui.console.util.FileUtil;
import org.glassfish.admingui.console.util.GuiUtil;
import org.glassfish.admingui.console.util.TargetUtil;


/**
 *
 * @author anilam
 */
@ManagedBean
@ViewScoped
public class UploadBean {
    private UploadedFile _file;
    private File tmpFile;
    private String appName;
    private String desc;
    private String contextRoot;
    private List<Map<String, Object>> metaData;

    private String database = "";
    private String eeTemplate = "";
    private String loadBalancer = "";
    private String minClusterSize = "1";
    private String maxClusterSize = "2";

    private List<String> eeTemplates = new ArrayList<String>() {{
        add("GlassFish Small");
        add("GlassFish Medium");
        add("GlassFish Large");
    }};

    private List<String> databases = new ArrayList<String>() {{
        add("Derby");
        add("MySQL");
        add("Oracle");
    }};

    private List<String> loadBalancers = new ArrayList<String>() {{
        add("foo");
        add("bar");
        add("baz");
    }};

    private List<SelectItem> databaseSelectItems;
    private List<SelectItem> eeTemplateSelectItems;
    private List<SelectItem> loadBalancerSelectItems;

    private List<Map> databasesMetaData = new ArrayList();
    private List<Map> eeTemplatesMetaData = new ArrayList();
    private List<Map> loadBalancersMetaData = new ArrayList();

    private DataModel<Map> databasesDataModel;
    private DataModel<Map> eeTemplatesDataModel;
    private DataModel<Map> loadBalancersDataModel;

    /*{
        //tmpFile = new File("D:/Projects/console.next/svn/main/appserver/tests/paas/basic-db/target/basic_db_paas_sample.war");
        //tmpFile = new File("D:/Projects/console.next/svn/main/appserver/tests/paas/service_metadata/provision_using_specified_template/target/basic_paas_sample2.war");
        tmpFile = new File("/opt/console.next/svn/main/appserver/tests/paas/basic/target/basic_paas_sample.war");
        processMetaData(tmpFile);
    }*/

    void createSelectItems(String serviceType) {
        List<SelectItem> selectItems = new ArrayList();
        List<String> templateList = CommandUtil.getTemplateList(serviceType);
        for (String template : templateList) {
            selectItems.add(new SelectItem(template));
        }
        if (CommandUtil.SERVICE_TYPE_RDMBS.equals(serviceType)) {
            databaseSelectItems =  selectItems;
            databases = templateList;
        } else if (CommandUtil.SERVICE_TYPE_JAVAEE.equals(serviceType)) {
            eeTemplateSelectItems =  selectItems;
            eeTemplates = templateList;
        } else if (CommandUtil.SERVICE_TYPE_LB.equals(serviceType)) {
            loadBalancerSelectItems =  selectItems;
            loadBalancers = templateList;
        }
    }

    void processMetaData(File file) {
        metaData = CommandUtil.getPreSelectedServices(file.getAbsolutePath());

        database = "";
        eeTemplate = "";
        loadBalancer = "";
        databasesMetaData.clear();
        eeTemplatesMetaData.clear();
        loadBalancersMetaData.clear();

        for(Map oneService : metaData){
            String serviceType = (String) oneService.get("service-type");
            String serviceName = (String) oneService.get("name");
            String templateId = (String) oneService.get("template-id");
            if (CommandUtil.SERVICE_TYPE_RDMBS.equals(serviceType)) {
                databasesMetaData.add(oneService);
                if (database.length() > 0)  {
                    database += ", ";
                }
                database += templateId != null ? templateId : serviceName;
            } else if (CommandUtil.SERVICE_TYPE_JAVAEE.equals(serviceType)) {
                eeTemplatesMetaData.add(oneService);
                if (eeTemplate.length() > 0)  {
                    eeTemplate += ", ";
                }
                eeTemplate += templateId != null ? templateId : serviceName;
            } else if (CommandUtil.SERVICE_TYPE_LB.equals(serviceType)) {
                loadBalancersMetaData.add(oneService);
                if (loadBalancer.length() > 0)  {
                    loadBalancer += ", ";
                }
                loadBalancer += templateId != null ? templateId : serviceName;
            }
        }

        createSelectItems(CommandUtil.SERVICE_TYPE_RDMBS);
        createSelectItems(CommandUtil.SERVICE_TYPE_JAVAEE);
        createSelectItems(CommandUtil.SERVICE_TYPE_LB);
    }

    public void fileUploaded(ValueChangeEvent event) {
        System.out.println("------ in filUploaded");
        UploadedFile file = (UploadedFile) event.getNewValue();
        try{
            if (file != null) {
                //FacesContext context = FacesContext.getCurrentInstance();
                //FacesMessage message = new FacesMessage( "Successfully uploaded file " + file.getFilename() + " (" + file.getLength() + " bytes)");
                //context.addMessage(event.getComponent().getClientId(context), message);
                // Here's where we could call file.getInputStream()
                System.out.println("getFilename=" + file.getFilename());
                System.out.println("getLength=" + file.getLength());
                System.out.println("getContentType=" + file.getContentType());
                tmpFile = FileUtil.inputStreamToFile(file.getInputStream(), file.getFilename());

                processMetaData(tmpFile);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }


    public UploadedFile getFile() {
        return _file;
    }

    public void setFile(UploadedFile file) {
        _file = file;
    }

    public String doDeploy(){

        // Generate deployment plan based on modified service
        Map dpAttrs = new HashMap();
        dpAttrs.put("archive" , tmpFile.getAbsolutePath());

        // ClassCastException Boolean cannot be cast to String in backend
        if (loadBalancersMetaData.get(0) != null) {
            Map config = (Map) loadBalancersMetaData.get(0).get("configurations");
            if (config != null) {
                Object sslEnabled = config.get("ssl-enabled");
                if (sslEnabled != null) {
                    config.put("ssl-enabled", sslEnabled.toString());
                }
            }
        }

        String metaDataJson = JSONUtil.javaToJSON(metaData, -1);
        dpAttrs.put("modifiedServiceDesc", metaDataJson);
        //ensure that template-id is the same as templateId, ie whatever user has changed that to.
        Map res = (Map) RestUtil.restRequest(REST_URL + "/applications/_generate-glassfish-services-deployment-plan", dpAttrs, "POST", null, null, false, true).get("data");
        Map extr = (Map) res.get("extraProperties");
        String deploymentPlanPath = (String) extr.get("deployment-plan-file-path");
        Map payload = new HashMap();
        payload.put("deploymentplan", deploymentPlanPath);
        payload.put("id", this.tmpFile.getAbsolutePath());
        payload.put("availabilityEnabled", "true");
        if (!GuiUtil.isEmpty(this.appName)){
            payload.put("name", this.appName);
        }
        if (!GuiUtil.isEmpty(this.contextRoot)){
            payload.put("contextroot", this.contextRoot);
        }
        /*
        if (!GuiUtil.isEmpty(this.desc)){
            payload.put("name", this.desc);
        }
         *
         */
        Map sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        List deployingApps = (List)sessionMap.get("_deployingApps");
        try {
            if (deployingApps == null ){
                deployingApps = new ArrayList();
                sessionMap.put("_deployingApps", deployingApps);
            }
            deployingApps.add(this.appName);
            RestUtil.restRequest(REST_URL + "/applications/application", payload, "post", null, null, false, true);
            return "/app/applications";
        } catch (Exception ex) {
            if (deployingApps != null && deployingApps.contains(this.appName)){
                deployingApps.remove(this.appName);
            }
            ex.printStackTrace();
            System.out.println("------------- do Deploy returns NULL");
            return null;
        }
    }

    public String getAppName(){
        return appName;
    }
    public void setAppName(String nm){
        this.appName = nm;
    }

    public String getDescription(){
        return desc;
    }
    public void setDescription(String description){
        this.desc = description;
    }

    public String getContextRoot(){
        return contextRoot;
    }
    public void setContextRoot(String ctxRoot){
        this.contextRoot = ctxRoot;
    }


    public List getMetaData(){
        return metaData;
    }
    public void setMetaData(List nm){
        this.metaData = nm;
    }

    public String databaseDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        if (database != null) {
            databases.add(database);
        }
        database = value;
        databases.remove(database);
        Collections.sort(databases);

        return null;
    }

    public String loadBalancerDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        if (loadBalancer != null) {
            loadBalancers.add(loadBalancer);
        }
        loadBalancer = value;
        loadBalancers.remove(loadBalancer);
        Collections.sort(loadBalancers);

        return null;
    }

    public String eeTemplateDropListener(DragDropEvent event) {
        String value = (String) event.getData();
        if (eeTemplate != null) {
            eeTemplates.add(eeTemplate);
        }
        eeTemplate = value;
        eeTemplates.remove(eeTemplate);
        Collections.sort(eeTemplates);

        return null;
    }

    public String getDatabase() {
        if (databasesDataModel != null && databasesDataModel.isRowAvailable()) {
            Map databaseMetaData = databasesDataModel.getRowData();
            return (String) databaseMetaData.get("template-id");
        } else {
            return database;
        }
    }

    public void setDatabase(String database) {
        this.database = database;
        Map databaseMetaData = databasesDataModel.getRowData();
        databaseMetaData.put("template-id", database);
    }

    public String getEeTemplate() {
        if (eeTemplatesDataModel != null && eeTemplatesDataModel.isRowAvailable()) {
            Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
            return (String) eeTemplateMetaData.get("template-id");
        }
        return eeTemplate;
    }

    public void setEeTemplate(String eeTemplate) {
        this.eeTemplate = eeTemplate;
        Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
        eeTemplateMetaData.put("template-id", eeTemplate);

    }

    public String getLoadBalancer() {
        if (loadBalancersDataModel != null && loadBalancersDataModel.isRowAvailable()) {
            Map loadBalancerMetaData = loadBalancersDataModel.getRowData();
            return (String) loadBalancerMetaData.get("template-id");
        } else {
            return loadBalancer;
        }
    }

    public void setLoadBalancer(String loadBalancer) {
        this.loadBalancer = loadBalancer;
        Map loadBalancerMetaData = loadBalancersDataModel.getRowData();
        loadBalancerMetaData.put("template-id", loadBalancer);
    }

    public List<SelectItem> getDatabaseSelectItems() {
        return databaseSelectItems;
    }

    public List<Map> getDatabasesMetaData() {
        return databasesMetaData;
    }

    public List<SelectItem> getEeTemplateSelectItems() {
        return eeTemplateSelectItems;
    }

    public List<Map> getEeTemplatesMetaData() {
        return eeTemplatesMetaData;
    }

    public String getEeTemplateMinMaxClusterSize() {
        if (eeTemplatesDataModel != null && eeTemplatesDataModel.isRowAvailable()) {
            Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
            Map config = (Map)eeTemplateMetaData.get("configurations");
            if (config != null) {
                String min = (String) config.get("min.clustersize");
                String max = (String) config.get("max.clustersize");
                return min + " - " + max;
            }
        }
        return minClusterSize + " - " + maxClusterSize;
    }

    public void setEeTemplateMinMaxClusterSize(String minMaxClusterSize) {
        if (eeTemplatesDataModel != null && eeTemplatesDataModel.isRowAvailable()) {
            Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
            String minmax[] = minMaxClusterSize.split("-");
            Map config = (Map)eeTemplateMetaData.get("configurations");
            if (config != null) {
                config.put("min.clusterSize", minmax[0].trim());
                config.put("max.clusterSize", minmax[1].trim());
            }
        }
    }

    public String getEeTemplateMinClusterSize() {
        if (eeTemplatesDataModel != null && eeTemplatesDataModel.isRowAvailable()) {
            Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
            Map config = (Map)eeTemplateMetaData.get("configurations");
            if (config != null) {
                return (String) config.get("min.clustersize");
            }
        }
        return minClusterSize;
    }

    public void setEeTemplateMinClusterSize(String minClusterSize) {
        this.minClusterSize = minClusterSize;
        Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
        Map config = (Map)eeTemplateMetaData.get("configurations");
        if (config != null) {
            config.put("min.clustersize", minClusterSize);
        }
    }

    public String getEeTemplateMaxClusterSize() {
        if (eeTemplatesDataModel != null && eeTemplatesDataModel.isRowAvailable()) {
            Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
            Map config = (Map)eeTemplateMetaData.get("configurations");
            if (config != null) {
                return (String) config.get("max.clustersize");
            }
        }
        return maxClusterSize;
    }

    public void setEeTemplateMaxClusterSize(String maxClusterSize) {
        this.maxClusterSize = maxClusterSize;
        Map eeTemplateMetaData = eeTemplatesDataModel.getRowData();
        Map config = (Map)eeTemplateMetaData.get("configurations");
        if (config != null) {
            config.put("max.clustersize", maxClusterSize);
        }
    }

    public List<SelectItem> getLoadBalancerSelectItems() {
        return loadBalancerSelectItems;
    }

    public List<Map> getLoadBalancersMetaData() {
        return loadBalancersMetaData;
    }

    public DataModel<Map> getDatabasesDataModel() {
        if (databasesDataModel == null) {
            databasesDataModel = new ListDataModel<Map>(databasesMetaData);
        }
        return databasesDataModel;
    }

    public DataModel<Map> getEeTemplatesDataModel() {
        if (eeTemplatesDataModel == null) {
            eeTemplatesDataModel = new ListDataModel<Map>(eeTemplatesMetaData);
        }
        return eeTemplatesDataModel;
    }

    public DataModel<Map> getLoadBalancersDataModel() {
        if (loadBalancersDataModel == null) {
            loadBalancersDataModel = new ListDataModel<Map>(loadBalancersMetaData);
        }
        return loadBalancersDataModel;
    }

    public List getListAsSet(Set set) {
         if (set != null) {
            return new ArrayList(set);
         } else {
             return new ArrayList();
         }
    }

    public String getCharacteristicDisplayName(String characteristic) {
        if ("os-name".equals(characteristic)) {
            return "OS Name";
        } else if ("service-type".equals(characteristic)){
            return "Service Type";
        } else {
            return characteristic;
        }
    }


    static final String REST_URL="http://localhost:4848/management/domain";
}
