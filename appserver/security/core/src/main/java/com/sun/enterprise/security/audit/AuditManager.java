/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * AuditManager.java
 *
 * Created on July 28, 2003, 1:56 PM
 */

package com.sun.enterprise.security.audit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import com.sun.appserv.security.AuditModule;
import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.SecurityService;
/*V3:Commented
import com.sun.enterprise.config.serverbeans.ServerBeansFactory;
import com.sun.enterprise.config.serverbeans.ElementProperty;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.server.ApplicationServer;
 */
import org.glassfish.internal.api.ServerContext;
import com.sun.logging.LogDomains;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Singleton;

/**
 *
 * @author  Harpreet Singh
 * @author  Shing Wai Chan
 */
@Service
@Scoped(Singleton.class)
public final class AuditManager {
    static final String NAME = "name";
    static final String CLASSNAME = "classname";

    private static final String AUDIT_MGR_WS_INVOCATION_KEY = 
        "auditmgr.webServiceInvocation";
    private static final String AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY = 
        "auditmgr.ejbAsWebServiceInvocation";
    private static final String AUDIT_MGR_SERVER_STARTUP_KEY = 
        "auditmgr.serverStartup";
    private static final String AUDIT_MGR_SERVER_SHUTDOWN_KEY = 
        "auditmgr.serverShutdown";

    private static final Logger _logger = 
             LogDomains.getLogger(AuditManager.class, LogDomains.SECURITY_LOGGER);

    private static final LocalStringManagerImpl _localStrings =
	new LocalStringManagerImpl(AuditManager.class);

    private List instances = Collections.synchronizedList(new ArrayList());
    // just a copy of names of the audit classes - helpful for log messages
    // since we will not have a lot of audit classes, keeping a duplicate copy
    // seems reasonable.
    private Map moduleToNameMap = new HashMap();
    private Map nameToModuleMap = new HashMap();
    // make this accessible to the containers so that the cost of non-audit case, 
    // is just a comparision.
    private boolean auditOn = false;
   
    @Inject
    private ServerContext serverContext;
     
    /** Creates a new instance of AuditManager */
    public AuditManager() {
    }
    
    /**
     * This method initializes AuditManager which load audit modules and
     * audit enabled flag
     */
    public void loadAuditModules() {
        try {
            SecurityService securityBean = serverContext.getDefaultHabitat().getComponent(SecurityService.class);
            /*V3:Commented
            ConfigContext configContext =
                ApplicationServer.getServerContext().getConfigContext();
            assert(configContext != null);

            Server configBean = ServerBeansFactory.getServerBean(configContext);
            assert(configBean != null);

            SecurityService securityBean =
                ServerBeansFactory.getSecurityServiceBean(configContext);*/
            assert(securityBean != null);
            // @todo will be removed to incorporate the new structure.
            //v3:Commented boolean auditFlag = securityBean.isAuditEnabled();
            boolean auditFlag = Boolean.parseBoolean(securityBean.getAuditEnabled());

            setAuditOn(auditFlag);
            /*V3:Commented
            com.sun.enterprise.config.serverbeans.AuditModule[] am =
                    securityBean.getAuditModule();*/
            List<com.sun.enterprise.config.serverbeans.AuditModule> am = securityBean.getAuditModule();
            for (com.sun.enterprise.config.serverbeans.AuditModule it: am) {
            //V3:Commented for (int i = 0; i < am.length; i++){
                try {
                    //V3:Commented String name = am[i].getName();
                    //V3:Commented String classname = am[i].getClassname();
                    String name = it.getName();
                    String classname = it.getClassname();
                    Properties p = new Properties();
                    //XXX should we remove this two extra properties
                    p.setProperty(NAME, name);
                    p.setProperty(CLASSNAME, classname);
                    List<Property> ep = it.getProperty();
                    /*V3:Commented
                    ElementProperty[] ep = am[i].getElementProperty();
                    int epsize = am[i].sizeElementProperty();
                    for (int j = 0; j < epsize; j++){
                        String nme = ep[j].getName();
                        String val = ep[j].getValue();
                        p.setProperty(nme, val);
                    }*/
                    for (Property prop: ep) {
                        p.setProperty(prop.getName(), prop.getValue());
                    }
                    AuditModule auditModule = loadAuditModule(classname, p);
                    instances.add(auditModule);
                    moduleToNameMap.put(auditModule, name);
                    nameToModuleMap.put(name, auditModule);
                } catch(Exception ex){
                     String msg = _localStrings.getLocalString(
                         "auditmgr.loaderror", 
                         "Audit: Cannot load AuditModule = {0}",
                         //V3:Commented new Object[]{ am[i].getName() });
                         new Object[]{ it.getName() });
                     _logger.log(Level.WARNING, msg, ex);                    
                }
            }
        } catch (Exception e) {
            String msg = _localStrings.getLocalString("auditmgr.badinit", 
                   "Audit: Cannot load Audit Module Initialization information. AuditModules will not be loaded.");
            _logger.log(Level.WARNING, msg, e);
        }
    }

    /**
     * Add the given audit module to the list of loaded audit module.
     * Adding the same name twice will override previous one.
     * @param name of auditModule
     * @param am an instance of a class extending AuditModule that has been 
     * successfully loaded into the system.
     * @exception 
     */
    public void addAuditModule(String name, String classname, Properties props)
            throws Exception {
        // make sure only a name corresponding to only one auditModule
        removeAuditModule(name);
        AuditModule am = loadAuditModule(classname, props);

        moduleToNameMap.put(am, name);
        nameToModuleMap.put(name, am);
        // clone list to resolve multi-thread issues in looping instances
        List list = new ArrayList();
        Collections.copy(instances, list);
        list.add(am);        
        instances = Collections.synchronizedList(list);
    }

    /**
     * Remove the audit module of given name from the loaded list.
     * @param name of auditModule
     */
    public void removeAuditModule(String name) {
        Object am = nameToModuleMap.get(name);
        if (am != null) {
            nameToModuleMap.remove(name);
            moduleToNameMap.remove(am);
            // clone list to resolve multi-thread issues in looping instances
            List list = new ArrayList();
            Collections.copy(instances, list);
            list.remove(am);        
            instances = Collections.synchronizedList(list);
        }
    }

    /**
     * Get the audit module of given name from the loaded list.
     * @param name of auditModule
     */
    AuditModule getAuditModule(String name) {
        return (AuditModule)nameToModuleMap.get(name);
    }


    /**
     * This method return auditModule with given classname and properties.
     * @param classname
     * @param props
     * @exception
     */
    private AuditModule loadAuditModule(String classname,
            Properties props) throws Exception {
        AuditModule auditModule = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class am = Class.forName(classname, true, loader);
        Object obj =  am.newInstance();
        auditModule = (AuditModule) obj;
        auditModule.init(props);
        return auditModule;
    }
    
    /**
     * logs the authentication call for all the loaded modules.
     * @see com.sun.appserv.security.AuditModule.authentication
     */
    public void authentication(String user, String realm, boolean success){
        if(auditOn){
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = null;
                try{
                    am = (AuditModule)list.get(i);
                    am.authentication(user, realm, success);
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString("auditmgr.authentication",
                    " Audit Module {0} threw the followin exception during authentication:", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }
    /**
     * logs the web authorization call for all loaded modules
     * @see com.sun.appserv.security.AuditModule.webInvocation
     */
    public void webInvocation(String user, HttpServletRequest req, 
        String type, boolean success){
        if(auditOn){
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.webInvocation(user, req, type, success);
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString("auditmgr.webinvocation",
                    " Audit Module {0} threw the followin exception during web invocation :", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }
    /**
     * logs the ejb authorization call for all ejb modules
     * @see com.sun.appserv.security.AuditModule.ejbInvocation
     */
    public void ejbInvocation(String user, String ejb, String method, 
            boolean success){
        if(auditOn){
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.ejbInvocation(user, ejb, method, success);
                } catch (Exception e){
                        String name = (String)moduleToNameMap.get(am);
                        String msg = 
                        _localStrings.getLocalString("auditmgr.ejbinvocation",
                        " Audit Module {0} threw the followin exception during ejb invocation :", 
                            new Object[] {name});
                        _logger.log(Level.INFO, msg, e);
                }

            }
        }
    }    
    
    /**
     * This method is called for the web service calls with MLS set 
     * and the endpoints deployed as servlets  
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void webServiceInvocation(String uri, String endpoint, 
                                     boolean validRequest){
        if(auditOn){
            // This surely is not the most optimal way of iterating through
            // the list of audit modules since I think the list is static
            // For now just do as its done for ejb/web audits - TODO later
            // Another thing to do would be make the list of audit modules
            // generic, preventing type casting at runtime 
            // like: List<AuditModule> list
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.webServiceInvocation(uri, endpoint,  validRequest);
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString(AUDIT_MGR_WS_INVOCATION_KEY,
                    " Audit Module {0} threw the following exception during "+
                    "web service invocation :", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }


    /**
     * This method is called for the web service calls with MLS set 
     * and the endpoints deployed as servlets  
     * @see com.sun.appserv.security.AuditModule.webServiceInvocation
     */
    public void ejbAsWebServiceInvocation(String endpoint, boolean validRequest){
        if(auditOn){

            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.ejbAsWebServiceInvocation(endpoint, validRequest);
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString(AUDIT_MGR_EJB_AS_WS_INVOCATION_KEY,
                    " Audit Module {0} threw the following exception during "+
                    "ejb as web service invocation :", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }

    public void serverStarted(){
        if(auditOn){
            // This surely is not the most optimal way of iterating through
            // the list of audit modules since I think the list is static
            // For now just do as its done for ejb/web audits - TODO later
            // Another thing to do would be make the list of audit modules
            // generic, preventing type casting at runtime 
            // like: List<AuditModule> list
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.serverStarted();
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString(AUDIT_MGR_SERVER_STARTUP_KEY,
                    " Audit Module {0} threw the following exception during "+
                    "server startup :", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }

    public void serverShutdown(){
        if(auditOn){
            // This surely is not the most optimal way of iterating through
            // the list of audit modules since I think the list is static
            // For now just do as its done for ejb/web audits - TODO later
            // Another thing to do would be make the list of audit modules
            // generic, preventing type casting at runtime 
            // like: List<AuditModule> list
            List list = instances;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                AuditModule am = (AuditModule)list.get(i);
                try{
                    am.serverShutdown();
                } catch (Exception e){
                    String name = (String)moduleToNameMap.get(am);
                    String msg = 
                    _localStrings.getLocalString(AUDIT_MGR_SERVER_SHUTDOWN_KEY,
                    " Audit Module {0} threw the following exception during "+
                    "server shutdown :", 
                        new Object[] {name});
                    _logger.log(Level.INFO, msg, e);
                }
            }
        }
    }

    public void setAuditOn(boolean auditOn) {
        this.auditOn = auditOn;
    }
    
    public boolean isAuditOn() {
        return auditOn;
    }
    
}
