/*
 * 
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) 2016 Payara Foundation and/or its affiliates.
 *  All rights reserved.
 * 
 *  The contents of this file are subject to the terms of the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 * 
 */
package fish.payara.nucleus.phonehome;

import com.sun.appserv.server.util.Version;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.bootstrap.StartupContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.glassfish.api.admin.ServerEnvironment;

/**
 *
 * @author David Weaver
 */
public class PhoneHomeTask implements Runnable {
    
    private final String PHONE_HOME_ID;
    private static final String PHONE_HOME_URL = "http://www.payara.fish/phonehome";
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int CONN_TIMEOUT_MS = 5000;    // 5 seconds
    private static final int READ_TIMEOUT_MS = 5000;    // 5 seconds
    
    private static final Logger LOGGER = Logger.getLogger(PhoneHomeTask.class.getCanonicalName());
    
    ServerEnvironment env;
    Domain domain;
    
    PhoneHomeTask(String phoneHomeId, Domain domain, ServerEnvironment env) {
        PHONE_HOME_ID = phoneHomeId;
        this.env = env;
        this.domain = domain;
    }

    @Override
    public void run() {
        
        Map<String,String> params = new HashMap<>();
        params.put("id", PHONE_HOME_ID);
        params.put("ver", getVersion());
        params.put("jvm", getJavaVersion());
        params.put("uptime", getUptime());
        params.put("nodes", getNodeCount());
        params.put("servers", getServerCount());
        
        String targetURL = PHONE_HOME_URL + encodeParams(params);
        send(targetURL);
    }
    
    private String getVersion() {     
        String ver = Version.getFullVersion();
        if (ver == null) {
            ver = "unknown";
        }
        return ver;
    }
    
    private String getJavaVersion() {
        
        String ver = System.getProperty("java.version");
        if (ver == null) {
            ver = "unknown";
        }
        return ver;
    }
    
    private String getUptime() {
        RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
        long totalTime_ms = -1;
        
        if (mxbean != null)
            totalTime_ms = mxbean.getUptime();

        if (totalTime_ms <= 0 && env != null) {
            StartupContext ctx = env.getStartupContext();
            if (ctx != null) {               
                long start = ctx.getCreationTime();
                totalTime_ms = System.currentTimeMillis() - start;
            }
        }
        return Long.toString(totalTime_ms);
    }
    
    private String getNodeCount(){      
        String result = "unknown";
        if (domain != null) {
            Nodes nodes = domain.getNodes();
            if (nodes != null) {
                List<Node> nodelist = nodes.getNode();
                if (nodelist != null) {
                    result = Integer.toString(nodelist.size());
                }
            }
        }
        return result;
    }
    
    private String getServerCount(){
        String result = "unknown";      
        if (domain != null) {
            List<Server> serverlist = domain.getServers().getServer();
            if (serverlist != null) {
                result = Integer.toString(serverlist.size());
            }
        }
        return result;
    }
    
    private String encodeParams(Map<String,String> params) {
        StringBuilder sb = new StringBuilder();
        char seperator;
        seperator = '?';
        for (Map.Entry<String,String> param : params.entrySet()) {       
            try {
                sb.append(String.format("%c%s=%s", seperator,
                    URLEncoder.encode(param.getKey(), "UTF-8"),
                    URLEncoder.encode(param.getValue(), "UTF-8")
                ));
                seperator='&';
            } catch (UnsupportedEncodingException uee) {/*Ignore*/}                     
        }
        return sb.toString();
    }
    
    private void send(String target) {

        HttpURLConnection conn = null;
        try {
            URL url = new URL(target);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(CONN_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.getResponseCode();
        } catch (IOException ioe) {
            /*Ignore*/
        } finally {
            if (conn != null) {
                    conn.disconnect();
            }
        }
    }   
}
