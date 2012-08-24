/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.server.logging;

import java.util.ArrayList;
import java.util.Arrays;
import com.sun.logging.LogDomains;


/**
 *  A Simple No Brainer Utility to map the Module Name to Logger Name..
 *
 *  @author Hemanth Puttaswamy
 */ 
public class ModuleToLoggerNameMapper {
    
    /*the sequence of each module entry in this table is important. always place
      module with longer loggername in the first.
      This table makes it possible to support more than 1 loggernames for each
      module. Each module log level's change through admin will result in level
      change in all its logger objects.
     
      The log module part is consistent with what module is defined in
      LogService.java (package com.sun.enterprise.config.serverbeans)
      refer also: ModuleLogLevels.java & ServerTags.java

      In sun-domain.dtd:
      <!ATTLIST module-log-levels
         root %log-level; "INFO"
         server %log-level; "INFO"
         ejb-container %log-level; "INFO"
         cmp-container %log-level; "INFO"
         mdb-container %log-level; "INFO"
         web-container %log-level; "INFO"
         classloader %log-level; "INFO"
         configuration %log-level; "INFO"
         naming %log-level; "INFO"
         security %log-level; "INFO"
         jts %log-level; "INFO"
         jta %log-level; "INFO"
         admin %log-level; "INFO"
         deployment %log-level; "INFO"
         verifier %log-level; "INFO"
         jaxr %log-level; "INFO"
         jaxrpc %log-level; "INFO"
         saaj %log-level; "INFO"
         corba %log-level; "INFO"
         javamail %log-level; "INFO"
         jms %log-level; "INFO"
         connector %log-level; "INFO"
         jdo %log-level; "INFO"
         cmp %log-level; "INFO"
         util %log-level; "INFO"
         resource-adapter %log-level; "INFO"
         synchronization %log-level; "INFO"
         node-agent %log-level; "INFO"
         self-management %log-level; "INFO"
         group-management-service %log-level; "INFO"
         management-event %log-level; "INFO">
    */
    private static final Object[][] ModuleAndLoggerTable = {
        {"admin",          new String[] { LogDomains.ADMIN_LOGGER } },    //admin
        {"classloader",    new String[] { LogDomains.LOADER_LOGGER} },    //classloader
        {"cmp",            new String[] { LogDomains.CMP_LOGGER} },
        {"cmp-container",  new String[] { LogDomains.CMP_LOGGER} }, //todo: verify with "cmp"
        {"configuration",  new String[] { LogDomains.CONFIG_LOGGER} },
        {"connector",      new String[] { LogDomains.RSR_LOGGER} },
        {"resource-adapter", new String[] { LogDomains.RSR_LOGGER} },//todo: verify with "connector"
        {"corba",          new String[] { LogDomains.CORBA_LOGGER} },
        {"deployment",     new String[] { LogDomains.DPL_LOGGER} },
        {"ejb-container",  new String[] { LogDomains.EJB_LOGGER} },      
        {"javamail",       new String[] { LogDomains.JAVAMAIL_LOGGER} },
        {"jaxr",           new String[] { LogDomains.JAXR_LOGGER} },
        {"jaxrpc",         new String[] { LogDomains.JAXRPC_LOGGER} },
        {"jdo",            new String[] { LogDomains.JDO_LOGGER} },
        {"jms",            new String[] { LogDomains.JMS_LOGGER, "javax.resourceadapter.mqjmsra"} },
        {"jta",            new String[] { LogDomains.JTA_LOGGER} },
        {"jts",            new String[] { LogDomains.TRANSACTION_LOGGER} },
        {"mdb-container",  new String[] { LogDomains.MDB_LOGGER} },
        //{"management-event"  //todo: management-event module owner needs to impl this.     
        {"naming",         new String[] { LogDomains.JNDI_LOGGER} },
        {"saaj",           new String[] { LogDomains.SAAJ_LOGGER} },
        {"security",       new String[] { LogDomains.SECURITY_LOGGER} },
        {"self-management",new String[] { LogDomains.SELF_MANAGEMENT_LOGGER} },
        {"synchronization",new String[] { "javax.ee.enterprise.system.tools.synchronization"} },
        {"web-container",  new String[] { LogDomains.WEB_LOGGER,
                                          "org.apache.catalina",
                                          "org.apache.coyote","org.apache.jasper" 
                                        } },
        {"group-management-service", new String[] { LogDomains.GMS_LOGGER} },
        {"node-agent",     new String[] { "javax.ee.enterprise.system.nodeagent" } },
        {"util",           new String[] { LogDomains.UTIL_LOGGER } },
        {"core",           new String[] { LogDomains.CORE_LOGGER} },
        {"server",         new String[] { LogDomains.SERVER_LOGGER} },
    };
    

    /**
     * @loggername  the logname
     * @return the module name the logger is for. 
     */
    public static String getModuleName(String loggerName) {
        for (int i=0; i<ModuleAndLoggerTable.length; i++) {
            Object[] dim = ModuleAndLoggerTable[i];
            String   modName = (String)dim[0];
            String[] loggerNames = (String[]) dim[1];            
            for (int j=0; loggerNames!=null && j<loggerNames.length;j++) {
                String name=loggerNames[j];
                if (loggerName.equals(name))
                    return modName;
            }
        }
        return null;
    }


    /**
     * @moduleName   the log module name (eg. "admin");
                     if null, it means all modules.
     * @return       the logger names for this module; size of returned String[] >=1.
     */
    public static String[] getLoggerNames( String moduleName ) {
        ArrayList result = new ArrayList(); 
        for (int i=0; i<ModuleAndLoggerTable.length; i++) {
            Object[] dim = ModuleAndLoggerTable[i];
            String   modName = (String)dim[0];
            String[] loggerNames = (String[]) dim[1];
            if (loggerNames!=null) {
                if (moduleName == null) {  //we return all AS module loggers in this case
		    result.addAll(Arrays.asList(loggerNames) ); 
                } else if (moduleName.equals(modName)) {
                    result.addAll( Arrays.asList(loggerNames) );
                    break;
                }
	    }
        }
        String[] lNames = new String[ result.size()];
        return (String[])result.toArray(lNames);
    }


}
