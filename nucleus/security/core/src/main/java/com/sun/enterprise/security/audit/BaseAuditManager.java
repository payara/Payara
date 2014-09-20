/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * BaseAuditManager.java
 *
 * Created on July 28, 2003, 1:56 PM
 */

package com.sun.enterprise.security.audit;

import com.sun.enterprise.security.BaseAuditModule;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.config.serverbeans.AuditModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


import org.jvnet.hk2.config.types.Property;
import com.sun.enterprise.config.serverbeans.SecurityService;
/*V3:Commented
import com.sun.enterprise.config.serverbeans.ServerBeansFactory;
import com.sun.enterprise.config.serverbeans.ElementProperty;
import com.sun.enterprise.config.ConfigContext;
import com.sun.enterprise.server.ApplicationServer;
 */
import org.glassfish.internal.api.ServerContext;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.List;
import org.glassfish.api.admin.ServerEnvironment;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 * Basic implementation of audit manager.
 * <p>
 * Projects layered on top of nucleus should extend this class, adding platform-specific
 * methods for auditing platform-specific events.  See AppServerAuditManagerImpl
 * for an example.  Such implementations should be sure to invoke this class's
 * setTypeClass method.  Then this class will keep a list of AuditModules of
 * that specific type in the typedModules field which subclasses can refer
 * to directly.
 * <p>
 * (This implementation was largely refactored from the original 
 * BaseAuditManager implementation that combined nucleus and app server features.)
 * 
 * @author  Harpreet Singh
 * @author  Shing Wai Chan
 * @author tjquinn
 */
@Service
@Singleton
public class BaseAuditManager<T extends BaseAuditModule> implements AuditManager {
    static final String NAME = "name";
    static final String CLASSNAME = "classname";
    
    // For speed, maintain a separate list of audit modules of the specified
    // module subtype (if any).  This allows subclasses to have very efficient
    // access to the specified audit modules which the subclass audit manager
    // deals with.
    protected List<T> typedModules = Collections.synchronizedList(new ArrayList<T>());
    private Class<T> typedModuleClass = null; // typically set by postConstruct of a subclass invoking setTypeClass
    
    private static final Logger _logger = 
             SecurityLoggerInfo.getLogger();

    private static final LocalStringManagerImpl _localStrings =
	new LocalStringManagerImpl(BaseAuditManager.class);

    private List<BaseAuditModule> instances = Collections.synchronizedList(new ArrayList<BaseAuditModule>());
    // just a copy of names of the audit classes - helpful for log messages
    // since we will not have a lot of audit classes, keeping a duplicate copy
    // seems reasonable.
    private Map<BaseAuditModule,String> moduleToNameMap = new HashMap<BaseAuditModule,String>();
    private Map<String,BaseAuditModule> nameToModuleMap = new HashMap<String,BaseAuditModule>();
    // make this accessible to the containers so that the cost of non-audit case, 
    // is just a comparision.
    protected boolean auditOn = false;
   
    @Inject
    private ServerContext serverContext;
    
    private static final String AUDIT_MGR_SERVER_STARTUP_KEY = 
        "auditmgr.serverStartup";
    private static final String AUDIT_MGR_SERVER_SHUTDOWN_KEY = 
        "auditmgr.serverShutdown";

    /**
     * This method initializes BaseAuditManager which load audit modules and
     * audit enabled flag
     */
    @Override
    public void loadAuditModules() {
        try {
            SecurityService securityBean = serverContext.getDefaultServices().getService(SecurityService.class,
                    ServerEnvironment.DEFAULT_INSTANCE_NAME);
            
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
                    BaseAuditModule auditModule = loadAuditModule(classname, p);
                    instances.add(auditModule);
                    moduleToNameMap.put(auditModule, name);
                    nameToModuleMap.put(name, auditModule);
                    if (isAuditModuleOfParameterizedType(auditModule)) {
                        typedModules.add((T)auditModule);
                    }
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
     * @param am an instance of a class extending BaseAuditModule that has been 
     * successfully loaded into the system.
     * @exception 
     */
    public BaseAuditModule addAuditModule(String name, String classname, Properties props)
            throws Exception {
        // make sure only a name corresponding to only one auditModule
        removeAuditModule(name);
        BaseAuditModule am = loadAuditModule(classname, props);

        moduleToNameMap.put(am, name);
        nameToModuleMap.put(name, am);
        // clone list to resolve multi-thread issues in looping instances
        instances = copyAndAdd(instances, am);
        if (isAuditModuleOfParameterizedType(am)) {
            typedModules = copyAndAdd(typedModules, (T)am);
        }
        return am;
    }
    
    private boolean isAuditModuleOfParameterizedType(final BaseAuditModule am) {
        return (typedModuleClass != null && typedModuleClass.isAssignableFrom(am.getClass()));
    }
    
    private <U extends BaseAuditModule> List<U> copyAndAdd(final List<U> orig, final U am) {
        final List<U> list = new ArrayList<U>();
        Collections.copy(orig, list);
        list.add(am);
        return list;
    }
    
    private <U extends BaseAuditModule> List<U> copyAndRemove(final List<U> orig, final U am) {
        final List<U> list = new ArrayList<U>();
        Collections.copy(orig, list);
        list.remove(am);
        return list;
    }
    
    /**
     * Remove the audit module of given name from the loaded list.
     * @param name of auditModule
     */
    public BaseAuditModule removeAuditModule(String name) {
        final BaseAuditModule am = nameToModuleMap.get(name);
        if (am != null) {
            nameToModuleMap.remove(name);
            moduleToNameMap.remove(am);
            // clone list to resolve multi-thread issues in looping instances
            instances = copyAndRemove(instances, am);
            if (isAuditModuleOfParameterizedType(am)) {
                typedModules = copyAndRemove(typedModules, (T)am);
            }
        }
        return am;
    }

    /**
     * Get the audit module of given name from the loaded list.
     * @param name of auditModule
     */
    BaseAuditModule getAuditModule(String name) {
        return nameToModuleMap.get(name);
    }

    /**
     * This method return auditModule with given classname and properties.
     * @param classname
     * @param props
     * @exception
     */
    private BaseAuditModule loadAuditModule(String classname,
            Properties props) throws Exception {
        BaseAuditModule auditModule;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class am = Class.forName(classname, true, loader);
        Object obj =  am.newInstance();
        auditModule = (BaseAuditModule) obj;
        auditModule.init(props);
        return auditModule;
    }
    
    public LocalStringManagerImpl getLocalStrings() {
        return _localStrings;
    }
    
    public Logger getLogger() {
        return _logger;
    }
    
    /**
     * logs the authentication call for all the loaded modules.
     * @see com.sun.appserv.security.BaseAuditModule.authentication
     */
    public void authentication(final String user, final String realm, final boolean success){
        if (auditOn) {
            for (BaseAuditModule am : instances) {
                try {
                    am.authentication(user, realm, success);
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            "auditmgr.authentication",
                            " Audit Module {0} threw the following exception during authentication:",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }
    
    public void serverStarted(){
        if (auditOn) {
            for (BaseAuditModule am : instances) {
                try {
                    am.serverStarted();
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            AUDIT_MGR_SERVER_STARTUP_KEY,
                            " Audit Module {0} threw the following exception during server startup :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
                }
            }
        }
    }

    public void serverShutdown(){
        if (auditOn) {
            for (BaseAuditModule am : instances) {
                try {
                    am.serverShutdown();
                } catch (Exception ex) {
                    final String name = moduleName(am);
                    final String msg = 
                        _localStrings.getLocalString(
                            AUDIT_MGR_SERVER_SHUTDOWN_KEY,
                            " Audit Module {0} threw the following exception during server shutdown :",
                            name);
                    _logger.log(Level.INFO, msg, ex);
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
    
    protected String moduleName(final BaseAuditModule am) {
        return moduleToNameMap.get(am);
    }
    
    protected List<T> instances(final Class<T> c) {
        final List<T> result = new ArrayList<T>();
        for (BaseAuditModule am : instances) {
            if (c.isAssignableFrom(c)) {
                result.add((T) am);
            }
        }
        return result;
    }
}
