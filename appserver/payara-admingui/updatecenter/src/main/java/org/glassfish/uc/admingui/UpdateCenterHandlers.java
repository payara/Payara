/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.uc.admingui;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.Properties;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import com.sun.pkg.client.Image;
import com.sun.pkg.client.Fmri;
import com.sun.pkg.client.LicenseAction;
import com.sun.pkg.client.Manifest;
import com.sun.pkg.client.SystemInfo;
import com.sun.pkg.client.SystemInfo.UpdateCheckFrequency;
import com.sun.pkg.client.Version;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.http.HttpSession;


import org.glassfish.admingui.common.util.GuiUtil;

/**
 *
 * @author anilam
 */
public class UpdateCenterHandlers {

   @Handler(id="checkConnectionInfo",
        output={
        @HandlerOutput(name="connectionInfo", type=String.class),
        @HandlerOutput(name="noConnection", type=String.class)})
    public static void checkConnectionInfo(HandlerContext handlerCtx) {
        if (Boolean.TRUE.equals(GuiUtil.getSessionValue("_noNetwork"))){
            handlerCtx.setOutputValue("connectionInfo",  GuiUtil.getMessage(BUNDLE, "noNetworkDetected"));
            handlerCtx.setOutputValue("noConnection", "true");
        }
        /*
        else if ("true".equals(GuiUtil.getSessionValue("_doNotPing"))){
            handlerCtx.setOutputValue("connectionInfo",  GuiUtil.getMessage(BUNDLE, "noCheckPerformed"));
            handlerCtx.setOutputValue("noConnection", "true");
        }
         */
        else{
            handlerCtx.setOutputValue("noConnection", "false");
        }
    }


    @Handler(id="getInstalledPath",
        output={
        @HandlerOutput(name="result", type=String.class)})
    public static void getInstalledPath(HandlerContext handlerCtx) {
        Image image = getUpdateCenterImage();
        handlerCtx.setOutputValue("result",  (image == null) ? 
            GuiUtil.getMessage(BUNDLE, "updateCenter.NoImageDirectory") : image.getRootDirectory());
    }
    
    
    @Handler(id="getAuthority",
        output={
        @HandlerOutput(name="result", type=String.class)})
    public static void getAuthority(HandlerContext handlerCtx) {
        Image image = getUpdateCenterImage();
        handlerCtx.setOutputValue("result",  (image == null) ? "" : image.getPreferredAuthorityName());
    }
    
    
    @Handler(id="getPkgDetailsInfo",
    	input={
        @HandlerInput(name="fmriStr", type=String.class, required=true ),
        @HandlerInput(name="auth", type=String.class, required=true )},
        output={
        @HandlerOutput(name="details", type=java.util.Map.class)})
    public static void getPkgDetailsInfo(HandlerContext handlerCtx) {
        String fmriStr = (String)handlerCtx.getInputValue("fmriStr");
        //Called by the intiPage and don't need to process.  When we can use beforeCreate to do this, we can remove this check.
        if (fmriStr == null){
            handlerCtx.setOutputValue("details", new HashMap());
            return;
        }
        Fmri fmri = new Fmri(fmriStr);
        Map details = new HashMap();  
        Image img = getUpdateCenterImage();
        try{
            details.put("pkgName", fmri.getName());
            details.put("uid", fmri.toString());
            details.put("version", getPkgVersion(fmri.getVersion()));
            details.put("date", fmri.getVersion().getPublishDate());
            details.put("auth", (String) handlerCtx.getInputValue("auth"));
            details.put("url", fmri.getURLPath());
            if (img != null){
                Manifest manifest = img.getManifest(fmri);
                details.put("category", getCategory(manifest));
                details.put("bytes", "" + manifest.getPackageSize() );
                details.put("pkgSize", getPkgSize(manifest));
                // look for description in the following order:
                // pkg.description, description_long, pkg.summary, description
                // since description_long and description has been deprecated.
                String desc = manifest.getAttribute(PKG_DESC);
                if (GuiUtil.isEmpty(desc)){
                    desc = manifest.getAttribute(DESC_LONG);
                    if (GuiUtil.isEmpty(desc)){
                        desc = manifest.getAttribute(PKG_SUMMARY);
                        if (GuiUtil.isEmpty(desc))
                            desc = manifest.getAttribute(DESC);
                    }
                }
                details.put("desc", desc);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        handlerCtx.setOutputValue("details", details);
        
    }

    private static String  getCategory(Manifest manifest){
        String attr = manifest.getAttribute(CATEGORY);
        //attr is of the form  scheme:catgory   refer to issue# 8494.
        int index = attr.indexOf(":");
        return (index==-1) ? attr : attr.substring(index+1);
    }
    
    @Handler(id="getUcList",
    	input={
        @HandlerInput(name="state", type=String.class, required=true )},
        output={
        @HandlerOutput(name="result", type=java.util.List.class)})
    public static void getUcList(HandlerContext handlerCtx) {

        List result = new ArrayList();
        if (Boolean.TRUE.equals(GuiUtil.getSessionValue("_noNetwork"))){
            handlerCtx.setOutputValue("result", result);
            return;
        }
        try {
            Image img = getUpdateCenterImage();
            if (img == null){
                handlerCtx.setOutputValue("result", result);
                return;
            }
            String state= (String)handlerCtx.getInputValue("state");
            if (state.equals("update")){
                handlerCtx.setOutputValue("result", getUpdateDisplayList(img, false));
                return;
            }
            
            List<Fmri> displayList = null;
            if (state.equals("installed"))
                displayList = getInstalledList(img);
            else
            if (state.equals("addOn"))
                displayList = getAddOnList(img);

          if (displayList != null){
            for (Fmri fmri : displayList){
                Map oneRow = new HashMap();
                try{
                    Manifest manifest = img.getManifest(fmri);
                    oneRow.put("selected", false);
                    oneRow.put("fmri", fmri);
                    oneRow.put("fmriStr", fmri.toString());
                    putInfo(oneRow, "pkgName", fmri.getName());
                    putInfo(oneRow, "version", getPkgVersion(fmri.getVersion()));
                    putInfo(oneRow, "newVersion", "");
                    putInfo(oneRow, "category", getCategory(manifest));
                    putInfo(oneRow, "pkgSize", getPkgSize(manifest));
                    oneRow.put( "size", Integer.valueOf(manifest.getPackageSize()));
                    putInfo(oneRow, "auth", fmri.getAuthority());
                    String tooltip = manifest.getAttribute(PKG_SUMMARY);
                    if (GuiUtil.isEmpty(tooltip))
                        tooltip = manifest.getAttribute(DESC);
                    putInfo(oneRow, "tooltip", tooltip);
                    result.add(oneRow);
                }catch(Exception ex){
                    GuiUtil.getLogger().info("getUcList(): " +  ex.getLocalizedMessage());
                    if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                        ex.printStackTrace();
                    }
                }
            }
          }
        }catch(Exception ex1){
            GuiUtil.getLogger().info("getUcList(): " +  ex1.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex1.printStackTrace();
            }
        }
        handlerCtx.setOutputValue("result", result);
    }
    
    
    @Handler(id="getAuthList",
        output={
        @HandlerOutput(name="result", type=java.util.List.class)})
    public static void getAuthList(HandlerContext handlerCtx) {
        
        List result = new ArrayList();
        try {
            Image image = getUpdateCenterImage();
            if (image == null){
                handlerCtx.setOutputValue("result", result);
                return;
            }
            String[] auths = image.getAuthorityNames();
            for(int i=0; i< auths.length; i++){
                Map oneRow = new HashMap();
                    oneRow.put("authName", auths[i]);
                    result.add(oneRow);
            }
        }catch(Exception ex1){
            ex1.printStackTrace();
        }
        handlerCtx.setOutputValue("result", result);
    }
    
    
    @Handler(id="getProxyInfo",
        output={
        @HandlerOutput(name="connection", type=String.class),
        @HandlerOutput(name="host", type=String.class),
        @HandlerOutput(name="port", type=String.class)}
        )
    public static void getProxyInfo(HandlerContext handlerCtx) {
        
        Proxy proxy = SystemInfo.getProxy();
        if (proxy != null){
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            if (address != null){
                handlerCtx.setOutputValue("connection", "useProxy");
                handlerCtx.setOutputValue("host", address.getHostName());
                handlerCtx.setOutputValue("port", address.getPort());
                return;
            }
        }
        handlerCtx.setOutputValue("connection", "direct");
        handlerCtx.setOutputValue("host", "");
        handlerCtx.setOutputValue("port", "");
    }
    
    @Handler(id="setProxyInfo",
        input={
        @HandlerInput(name="connection", type=String.class),
        @HandlerInput(name="host", type=String.class),
        @HandlerInput(name="port", type=String.class)}
        )
    public static void setProxyInfo(HandlerContext handlerCtx) {
        String connection = (String)handlerCtx.getInputValue("connection");
        String host = (String)handlerCtx.getInputValue("host");
        String port = (String)handlerCtx.getInputValue("port");
        try{
            Image image = getUpdateCenterImage();
            if (image == null){
                String ucDir = (String)GuiUtil.getSessionValue("topDir");
                GuiUtil.handleError(handlerCtx,  GuiUtil.getMessage(BUNDLE, "NoImage", new String[]{ucDir}));
                return;
            }
            if (connection.equals("useProxy")){
                int portNo = Integer.parseInt(port);
                SocketAddress address = new InetSocketAddress(host, portNo);
                image.setProxy(new Proxy(Proxy.Type.HTTP, address));
                String url="http://"+host+":"+portNo;
                Properties prop = new Properties();
                prop.setProperty("proxy.URL", url);
                SystemInfo.initUpdateToolProps(prop);
            }else{
                image.setProxy(null);
                Properties prop = new Properties();
                prop.setProperty("proxy.URL", "");
                SystemInfo.initUpdateToolProps(prop);
            }
        }catch (Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
        }
    }
                    
    
    private static void putInfo( Map oneRow, String key, String value){
        oneRow.put( key, GuiUtil.isEmpty(value) ? "" : value);
    }

    private static List<Fmri> getInstalledList(Image image){
        List<Image.FmriState> fList = image.getInventory(null, false);
        ArrayList<Fmri> result = new ArrayList();
        for(Image.FmriState fs: fList){
            result.add(fs.fmri);
        }
        return result;

    }

    /**
     * Returns true if f1 supersedes f2
     */
    private static boolean supersedes(Fmri f1, Fmri f2, String pAuth) {
        boolean f1Preferred = f1.getAuthority().equals(pAuth);
        boolean f2Preferred = f2.getAuthority().equals(pAuth);

        // If f1 is from the preferred authority and f2 is not, then
        // f1 supersedes.
        if (f1Preferred && ! f2Preferred) {
            return true;
        }

        // If f2 is from the preferred authority and f1 is not, then
        // f1 does not supersede
        if (f2Preferred && ! f1Preferred) {
            return false;
        }

        // Otherwise compare versions. f1 supersedes if it is a successor
        return f1.isSuccessor(f2);
    }

    
    private static List<Fmri> getAddOnList(Image image){
        List<String> installed = new ArrayList<String>();
        for (Image.FmriState each : image.getInventory(null, false)) {
            installed.add(each.fmri.getName());
        }
        String pAuth = image.getPreferredAuthorityName();
        SortedMap<String, Fmri> addOnMap = new TreeMap();
        for (Image.FmriState each : image.getInventory(null, true)) {
            Fmri fmri = each.fmri;
            // If this exact package is installed, or another version
            // of this package is installed, then skip it.
            if (each.installed || installed.contains(fmri.getName())) {
                continue;
            }

            if (addOnMap.containsKey(fmri.getName())) {
               // We have seen this package name already. See if this
               // version should replace the saved version.
               Fmri saved = addOnMap.get(fmri.getName());
               if (supersedes(fmri, saved, pAuth)) {
                   addOnMap.put(fmri.getName(), fmri);
               }
            } else {
               // We haven't seen this package name yet. Save fmri
               addOnMap.put(fmri.getName(), fmri);
            }

        }

        List<Fmri> result = new ArrayList();
        for(Fmri f: addOnMap.values()){
            result.add(f);
        }
        return result;
    }

    //If countOnly is set to true,  return a List that contains only one Integer that specifies the # of updates.
    //This is for displaying in the masthead
    //If countOnly is set to false, it will go through each package that has update available and return a list
    //suitable for displaying as a table row.
    private static List getUpdateDisplayList(Image image, boolean countOnly){
        List<Image.FmriState> installed = image.getInventory(null, false);
        Map<String, Fmri> updateListMap = new HashMap();
        List<String> nameList = new ArrayList();
        for(Image.FmriState fs: installed){
            if (fs.upgradable){
                Fmri fmri = fs.fmri;
                updateListMap.put(fmri.getName(),fmri);
                nameList.add(fmri.getName());
            }
        }
        List result = new ArrayList();
        String[] pkgsName = nameList.toArray(new String[nameList.size()]);
        try{
            Image.ImagePlan ip = image.makeInstallPlan(pkgsName, "list");
            Fmri[] proposed = ip.getProposedFmris();
            if (countOnly){
                result.add(Integer.valueOf(proposed.length));
                return result;
            }
            for( Fmri newPkg : proposed){
                Map oneRow = new HashMap();
                try{
                    String name = newPkg.getName();
                    Fmri oldPkg = updateListMap.get(name);
                    Manifest manifest = image.getManifest(newPkg);
                    int changedSize = manifest.getPackageSize() - image.getManifest(oldPkg).getPackageSize();
                    oneRow.put("selected", false);
                    oneRow.put("fmri", newPkg);
                    oneRow.put("fmriStr", newPkg.toString());
                    putInfo(oneRow, "pkgName", name);
                    putInfo(oneRow, "newVersion", getPkgVersion(newPkg.getVersion()));
                    putInfo(oneRow, "version", getPkgVersion(oldPkg.getVersion()));
                    putInfo(oneRow, "category", getCategory(manifest));
                    putInfo(oneRow, "pkgSize", convertSizeForDispay(changedSize));
                    oneRow.put( "size", Integer.valueOf(changedSize));
                    putInfo(oneRow, "auth", newPkg.getAuthority());
                    String tooltip = manifest.getAttribute(PKG_SUMMARY);
                    if (GuiUtil.isEmpty(tooltip))
                        tooltip = manifest.getAttribute(DESC);
                    putInfo(oneRow, "tooltip", tooltip);
                    result.add(oneRow);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
            if (countOnly){
                List tmpL = new ArrayList();
                tmpL.add(Integer.valueOf(-1));
                return tmpL;
            }
        }
       GuiUtil.setSessionValue("_updateCountMsg", GuiUtil.getMessage(UpdateCenterHandlers.BUNDLE, "msg.updatesAvailable", new String[]{""+result.size()}));
       return result;
    }
    
    
    
    @Handler(id="updateCenterProcess",
    	input={
        @HandlerInput(name="action", type=String.class, required=true ),
        @HandlerInput(name="selectedRows", type=java.util.List.class, required=true )})
    public static void updateCenterProcess(HandlerContext handlerCtx) {
        Image image = getUpdateCenterImage();
        boolean install = false;
        String action= (String)handlerCtx.getInputValue("action");
        if (action.equals("install")) {
            install=true;
        }
        List obj = (List) handlerCtx.getInputValue("selectedRows");
        if (obj == null){
            //no row selected
            return;
        }
        List<Map> selectedRows = (List) obj;
        //do not use Fmri list to pass to installPackages, use String array to avoid UPDATECENTER2-2187
        String[] fmris = new String[selectedRows.size()];
        int i=0;
        try {
            for (Map oneRow : selectedRows) {
                fmris[i++]=((Fmri)oneRow.get("fmri")).toString();
            }
            if (install){
                image.installPackages(fmris);
                //updateCountInSession(image);   No need to update the update count since the count will not change.  Only installing new component is allowed.
            }else{
                image.uninstallPackages(fmris);
            }
            GuiUtil.setSessionValue("restartRequired", Boolean.TRUE);
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
            ex.printStackTrace();
        }
    }
    
   // getLicenseText(selectedRows="#{selectedRows}" license=>$page{license});
     @Handler(id="getLicenseText",
    	input={
        @HandlerInput(name="selectedRows", type=java.util.List.class, required=true)},
        output={
        @HandlerOutput(name="license", type=String.class),
        @HandlerOutput(name="hasLicense", type=Boolean.class)})
    public static void getLicenseText(HandlerContext handlerCtx) {
         
        List obj = (List) handlerCtx.getInputValue("selectedRows");
        Image image = getUpdateCenterImage();
        List<Map> selectedRows = (List) obj;
        try {
            StringBuffer allLicense = new StringBuffer();
            for (Map oneRow : selectedRows) {
                Fmri fmri = (Fmri)oneRow.get("fmri");
                allLicense.append(getLicense(image, fmri));
            }
            handlerCtx.setOutputValue("license", ""+allLicense);
            handlerCtx.setOutputValue("hasLicense", (allLicense.length() > 0));
        }catch(Exception ex){
            GuiUtil.handleException(handlerCtx, ex);
            //ex.printStackTrace();
        }
     }
        
     
    @Handler(id = "getUpdateComponentCount")
    public static void getUpdateComponentCount(HandlerContext handlerCtx) {

        boolean donotPing = false;
        //If user set NO_NETWORK system properties, don't try to do anything.
        boolean noNetwork = (Boolean) GuiUtil.getSessionValue("_noNetwork");
        if (noNetwork){
            GuiUtil.getLogger().info(GuiUtil.getMessage(BUNDLE,"noNetworkDetected"));
            donotPing = true;
        }else{
            UpdateCheckFrequency userPreference = SystemInfo.getUpdateCheckFrequency();
            if (userPreference == UpdateCheckFrequency.NEVER){
                GuiUtil.getLogger().info(GuiUtil.getMessage(BUNDLE,"noCheckPerformed"));
                GuiUtil.setSessionValue("_doNotPing", "true");
                donotPing = true;
            }
        }
        if (donotPing){
            GuiUtil.setSessionValue("_hideUpdateMsg", Boolean.TRUE);
            return;
        }
        GuiUtil.setSessionValue("_hideUpdateMsg", Boolean.FALSE);
        GuiUtil.setSessionValue("_updateCountMsg", ""); 
        UcThread thread = new UcThread((HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(false));
        thread.start();
    }


     public static Integer updateCountInSession(Image image){
	 Integer countInt = Integer.valueOf(-1);
	 if (image != null){
	    List list = getUpdateDisplayList(image, true);
	    countInt = (Integer) list.get(0);
	 }else{
	    GuiUtil.getLogger().warning(GuiUtil.getMessage(BUNDLE, "cannotGetImage"));
	 }
         return countInt;
     }
     
    private static String getLicense(Image img, Fmri fmri){
        StringBuffer licenseText = new StringBuffer();
        try{
            Manifest manifest = img.getManifest(fmri);
            List<LicenseAction> lla = manifest.getActionsByType(LicenseAction.class);
            for (LicenseAction la : lla) {
                licenseText.append("============= ").append(la.getName()).append(" ================\n");
                licenseText.append(fmri.toString());
                licenseText.append("\n\n");
                licenseText.append(la.getText());
                licenseText.append("\n\n");
            }
            return "" + licenseText;
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return null;
    }
    
    private static String getPkgVersion(Version version){
        //The version format is release[,build_release]-branch:datetime, which is decomposed into three DotSequences and the datetime. 
        //eg. 2.4.4,0-8.724:20080612T135341Z
        
        String dotSequence = version.getRelease().toString();
        String branch = version.getBranch().toString();
        return GuiUtil.isEmpty(branch) ? dotSequence : dotSequence+"-"+branch; 
    }
    
    private static String getPkgSize(Manifest manifest){
        int size = manifest.getPackageSize();
        return convertSizeForDispay(size);
    }
    
    private static String convertSizeForDispay(int size){
        String sizep = (size <= MB) ?
            size/1024 + GuiUtil.getMessage(BUNDLE, "sizeKB") :
            size/MB + GuiUtil.getMessage(BUNDLE, "sizeMB")  ;
        return sizep;
    }

    /* comment out un-used method.
    private static String getPkgDate(Version version){
        //TODO localize the date format
        int begin = version.toString().indexOf(":");
        int end = version.toString().indexOf("T");
        String dateStr = version.toString().substring(begin+1, end);
        String result = dateStr.substring(0,4) + "/" + dateStr.substring(4,6) + "/" + dateStr.substring(6,8);
        return result;
    }
     *
     */

    
    public static Image getUpdateCenterImage(){
        if (Boolean.TRUE.equals(GuiUtil.getSessionValue("_noNetwork"))){
            return null;
        }else{
         return getUpdateCenterImage((String)GuiUtil.getSessionValue("topDir"), false);
        }
    }

    public static Image getUpdateCenterImage(String ucDir, boolean force){
        Image image = null;
        try{
            image = new Image (new File (ucDir));
            if (force || (GuiUtil.getSessionValue(CATALOG_REFRESHED) == null)){
		image.refreshCatalogs();
		GuiUtil.setSessionValue(CATALOG_REFRESHED, "TRUE");
            }
        }catch(Exception ex){
            if(force && (image == null)){
                GuiUtil.getLogger().warning(GuiUtil.getMessage(BUNDLE, "NoImage", new String[]{ucDir}));
            }
        }
        return image;
    }

    
    final private static String CATEGORY = "info.classification";
    final private static String DESC_LONG = "description_long";
    final private static String PKG_DESC = "pkg.description";
    final private static String PKG_SUMMARY = "pkg.summary";
    final private static String DESC = "description";
    final private static String CATALOG_REFRESHED = "__gui_uc_catalog_refreshed";
    final public static String BUNDLE = "org.glassfish.updatecenter.admingui.Strings";
    final private static int MB = 1024*1024;

}
