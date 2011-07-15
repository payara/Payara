/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.optional;

import com.sun.enterprise.config.serverbeans.AuditModule;
import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import java.util.ArrayList;
import java.util.WeakHashMap;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Dom;

// config imports
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpListener;
import com.sun.enterprise.config.serverbeans.IiopListener;
import com.sun.enterprise.config.serverbeans.JmsHost;
import com.sun.enterprise.config.serverbeans.JmxConnector;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.ConnectorConnectionPool;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.IiopService;
import com.sun.enterprise.config.serverbeans.JmsService;
import com.sun.enterprise.config.serverbeans.SecurityMap;
import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.Result;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Does basic level verification of domain.xml. This is helpful as in v3 there
 * is no DTD to validate the domain's config i.e. domain.xml
 * 
 * @author Nandini Ektare
 */
public class DomainXmlVerifier {

    private Domain domain;
    public boolean error;
    PrintStream _out;
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(DomainXmlVerifier.class);

    public DomainXmlVerifier(Domain domain) throws Exception {
        this(domain, System.out);
    }
    
    public DomainXmlVerifier(Domain domain, PrintStream out) throws Exception {
        this.domain = domain;
        _out = out;
        error = false;
    }

    /*
     * Returns true if there is an error in the domain.xml or some other problem.
     */
    public boolean invokeConfigValidator() {
        boolean failed = false;
        try {
            failed =  validate();
        } catch(Exception e) {
            failed = true;
            e.printStackTrace();
        }
        return failed;
    }

    public boolean validate() {
        try {
            checkUnique(domain);
            if (!error)
               _out.println(strings.get("VerifySuccess"));
        } catch(Exception e) {
            error = true;
            e.printStackTrace();
        }
        return error;
    }
    
    public void checkUnique(Domain domain) {

        try {
            // Resources
            Resources resources = domain.getResources();
            Collection<ConnectorConnectionPool> connectionPoolList =
                resources.getResources(ConnectorConnectionPool.class);
            checkDuplicate(connectionPoolList);

            for (ConnectorConnectionPool ccp : connectionPoolList) {
                List<SecurityMap> secMapList = ccp.getSecurityMap();
                checkDuplicate(secMapList);
            }

            // Servers -- commented out as of now because Server 
            // element does not have a key. See bugs 6039 and 6040
            /*
            Servers servers = domain.getServers();
            List<Server> serversList = servers.getServer();
            checkDuplicate(serversList);
            */
            
            // Configs
            Configs configs = domain.getConfigs();
            if (configs != null) {
                List<Config> configList = configs.getConfig();
                checkDuplicate(configList);
                if (configList != null) {
                    // Children of each Config viz: HttpService, IiopService
                    for (Config cfg : configList) {
                        // config-->http-service
                        HttpService httpSvc = cfg.getHttpService();
                        if (httpSvc != null) {
                            List<HttpListener> httpListeners = httpSvc.getHttpListener();
                            checkDuplicate(httpListeners);
                            List<VirtualServer> virtualServers = httpSvc.getVirtualServer();
                            checkDuplicate(virtualServers);
                        }

                        // config-->iiop-service
                        IiopService iiopSvc = cfg.getIiopService();
                        if (iiopSvc != null) {
                            List<IiopListener> iiopListeners = iiopSvc.getIiopListener();
                            checkDuplicate(iiopListeners);
                        }

                        // config-->admin-service-->jmx-connector
                        AdminService adminsvc = cfg.getAdminService();
                        if (adminsvc != null) {
                            List<JmxConnector> jmxConnectors = adminsvc.getJmxConnector();
                            checkDuplicate(jmxConnectors);
                        }

                        // config-->jms-service-->jms-host
                        JmsService jmsService = cfg.getJmsService();
                        if (jmsService != null) {
                            List<JmsHost> jmsHosts = jmsService.getJmsHost();
                            checkDuplicate(jmsHosts);
                        }

                        // Now sub elements of Security Service
                        SecurityService securitysvc = cfg.getSecurityService();

                        if (securitysvc != null) {
                            // config-->security-service-->audit-module
                            List<AuditModule> auditModules = securitysvc.getAuditModule();
                            checkDuplicate(auditModules);

                            // config-->security-service-->audit-module
                            List<AuthRealm> authrealms = securitysvc.getAuthRealm();
                            checkDuplicate(authrealms);

                            // config-->security-service-->audit-module
                            List<JaccProvider> providers = securitysvc.getJaccProvider();
                            checkDuplicate(providers);
                        }
                    }
                }
            }
        } catch(Exception e) {
            error = true;
            e.printStackTrace();
        }
    }
    
    public void output(Result result) {
        _out.println(strings.get("VerifyError", result.result()));
    }

    public void checkDuplicate(Collection <? extends ConfigBeanProxy> beans) {
        if (beans == null) {
            return;
        }
        WeakHashMap keyBeanMap = new WeakHashMap();
        ArrayList<String> keys = new ArrayList<String>(beans.size());
        for (ConfigBeanProxy cbp : beans) {
            String key = Dom.unwrap(cbp).getKey();
            keyBeanMap.put(key,Dom.unwrap(cbp));
            keys.add(key);
        }

        WeakHashMap<String, Class<ConfigBeanProxy>> errorKeyBeanMap = 
                new WeakHashMap<String, Class<ConfigBeanProxy>>();
        String[] strKeys = keys.toArray(new String[beans.size()]);
        for (int i = 0; i < strKeys.length; i++) {
            boolean foundDuplicate = false;
            for (int j = 0; j < strKeys.length; j++) {
                // If the keys are same and if the indexes don't match
                // we have a duplicate. So output that error
                if ( (strKeys[i].equals(strKeys[j])) && (i!=j) ) {
                    foundDuplicate = true;
                    errorKeyBeanMap.put(strKeys[i],
                        ((Dom)keyBeanMap.get(strKeys[i])).getProxyType());
                    error = true;
                    break;
                }
            }
        }

        for (Map.Entry e : errorKeyBeanMap.entrySet()) {
            Result result = new Result(strings.get("VerifyDupKey", e.getKey(), e.getValue()));
            output(result);
        }
    }    
}
