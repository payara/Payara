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

package com.sun.enterprise.web.connector.extension;

import com.sun.enterprise.admin.monitor.registry.MonitoredObjectType;
import com.sun.enterprise.admin.monitor.registry.MonitoringLevel;
import com.sun.enterprise.admin.monitor.registry.MonitoringLevelListener;
import com.sun.enterprise.admin.monitor.registry.MonitoringRegistry;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.ModuleMonitoringLevels;
import com.sun.enterprise.web.WebContainer;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.j2ee.statistics.Stats;

import javax.management.ObjectName;
import java.lang.String;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class track monitoring or Grizzly, using JMX to invoke Grizzly main
 * classes.
 *
 * @author Jeanfrancois Arcand 
 */ 
public class GrizzlyConfig implements MonitoringLevelListener{

    private static final Logger logger = com.sun.enterprise.web.WebContainer.logger;

    private static final ResourceBundle rb = logger.getResourceBundle();

    @LogMessageInfo(
            message = "Exception when initializing monitoring for network-listener [{0}]",
            level = "WARNING")
    public static final String INIT_MONITORING_EXCEPTION = "AS-WEB-GLUE-00081";

    @LogMessageInfo(
            message = "InvokeGrizzly method={0} objectName={1}",
            level = "FINE")
    public static final String INVOKE_GRIZZLY = "AS-WEB-GLUE-00082";

    @LogMessageInfo(
            message = "Exception while invoking mebean server operation [{0}]",
            level = "WARNING")
    public static final String INVOKE_MBEAN_EXCEPTION = "AS-WEB-GLUE-00083";

    /**
     * Is monitoring already started.
     */
    private boolean isMonitoringEnabled = false;
    
    /**
     * The JMX domain
     */
    private String domain;
    

    /**
     * The port used to lookup Grizzly's Selector
     */
    private int port;


    /**
     * The list of instance created. This list is not thread-safe.
     */
    private static ArrayList<GrizzlyConfig>
            grizzlyConfigList = new ArrayList<GrizzlyConfig>();
    
  
    /**
     * This server context's default services.
     */
    private ServiceLocator services = null;
    

    // --------------------------------------------------------------- //
   
    
    /**
     * Creates the monitoring helper.
     */
    public GrizzlyConfig(WebContainer webContainer, String domain, int port) {

        this.domain = domain;
        this.port = port;

        this.services = webContainer.getServerContext().getDefaultServices();

        grizzlyConfigList.add(this);
    }

    public void destroy() {
        unregisterMonitoringLevelEvents();
        grizzlyConfigList.remove(this);
    }
    
    public void initConfig(){
        initMonitoringLevel();
    }
    
    
    private void initMonitoringLevel() {
        try{
            Config cfg = services.getService(Config.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            
            MonitoringLevel monitoringLevel = MonitoringLevel.OFF; // default per DTD

            if (cfg.getMonitoringService() != null) {
                ModuleMonitoringLevels levels =
                    cfg.getMonitoringService().getModuleMonitoringLevels();
                if (levels != null) {
                    monitoringLevel = MonitoringLevel.instance(
                                                    levels.getHttpService());
                }
            }
        
            if(MonitoringLevel.OFF.equals(monitoringLevel)) {
                isMonitoringEnabled = false;
            } else {
                isMonitoringEnabled = true;
            } 
            
            String methodToInvoke = isMonitoringEnabled ? "enableMonitoring" :
                "disableMonitoring";
            invokeGrizzly(methodToInvoke);
        } catch (Exception ex) {
            String msg = rb.getString(INIT_MONITORING_EXCEPTION);
            msg = MessageFormat.format(msg, Integer.valueOf(port));
            logger.log(Level.WARNING, msg, ex);
        }
    } 
    
    
    public void registerMonitoringLevelEvents() {
        MonitoringRegistry monitoringRegistry = services.getService(MonitoringRegistry.class);
        if (monitoringRegistry!=null) {
            monitoringRegistry.registerMonitoringLevelListener(
                this, MonitoredObjectType.HTTP_LISTENER);
        }
    }

    
    private void unregisterMonitoringLevelEvents() {
        MonitoringRegistry monitoringRegistry = services.getService(MonitoringRegistry.class);
        if (monitoringRegistry!=null) {
            monitoringRegistry.unregisterMonitoringLevelListener(this);
        }
    }

    
    public void setLevel(MonitoringLevel level) {
        // deprecated, ignore
    }
    
    
    public void changeLevel(MonitoringLevel from, MonitoringLevel to,
                            MonitoredObjectType type) {
        if (MonitoredObjectType.HTTP_LISTENER.equals(type)) {
            if(MonitoringLevel.OFF.equals(to)) {
                isMonitoringEnabled = false;
            } else {
                isMonitoringEnabled = true;
            }
        }            
        String methodToInvoke = isMonitoringEnabled ? "enableMonitoring" :
            "disabledMonitoring";
        invokeGrizzly(methodToInvoke);        
    }
    
    
    public void changeLevel(MonitoringLevel from, MonitoringLevel to, 
			    Stats handback) {
        // deprecated, ignore
    }

    
    protected final void invokeGrizzly(String methodToInvoke) {  
        invokeGrizzly(methodToInvoke,null,null);
    }   
     
    
    protected final void invokeGrizzly(String methodToInvoke, 
                                       Object[] objects, String[] signature) {  
        try{
            String onStr = domain + ":type=Selector,name=http" + port;
            ObjectName objectName = new ObjectName(onStr);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, INVOKE_GRIZZLY,
                        new Object[] {methodToInvoke, objectName});
            }
            
        } catch ( Exception ex ){
            String msg = rb.getString(INVOKE_MBEAN_EXCEPTION);
            msg = MessageFormat.format(msg, methodToInvoke);
            logger.log(Level.SEVERE, msg, ex);
            //throw new RuntimeException(ex);
        }
    }

    
    /**
     * Enable CallFlow gathering mechanism.
     */
    public final void setEnableCallFlow(boolean enableCallFlow){
        String methodToInvoke = enableCallFlow ? "enableMonitoring" :
            "disabledMonitoring";
        invokeGrizzly(methodToInvoke);        
    }

    
    /**
     * Return the list of all instance of this class.
     */
    public static ArrayList<GrizzlyConfig> getGrizzlyConfigInstances(){
        return grizzlyConfigList;
    }
    
    
    /**
     * Return the port this configuration belongs.
     */
    public int getPort(){
        return port;
    }   
}
