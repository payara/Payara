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

package org.glassfish.loadbalancer.admin.cli.helper;

import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import org.glassfish.loadbalancer.admin.cli.reader.api.ClusterReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.InstanceReader;
import org.glassfish.loadbalancer.admin.cli.reader.api.WebModuleReader;
import org.glassfish.loadbalancer.config.LbConfig;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.loadbalancer.config.LoadBalancer;
import org.glassfish.loadbalancer.config.LbConfigs;
import org.glassfish.loadbalancer.config.LoadBalancers;

import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;
import org.glassfish.loadbalancer.admin.cli.reader.impl.LoadbalancerReaderImpl;
import org.glassfish.loadbalancer.admin.cli.transform.LoadbalancerVisitor;

import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.LbLogUtil;
import org.glassfish.loadbalancer.admin.cli.beans.Loadbalancer;


/**
 * Export support class
 *
 * @author Kshitiz Saxena
 */
public class LbConfigHelper {

    /**
     * exports the loadbalancer.xml from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config
     * @param out OutputStream into which the loadbalancer.xml is written
     */
    public static LoadbalancerReader getLbReader(Domain domain, ApplicationRegistry appRegistry,
            String lbConfigName) throws Exception {
        // reads the load balancer related data
        LbConfigs lbConfigs = domain.getExtensionByType(LbConfigs.class);
        if (lbConfigs == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLbConfig", lbConfigName));
        }
        LbConfig lbConfig = lbConfigs.getLbConfig(lbConfigName);
        if (lbConfig == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLbConfig", lbConfigName));
        }
        return new LoadbalancerReaderImpl(domain, appRegistry, lbConfig);
    }

    public static LoadBalancer getLoadBalancer(Domain domain, String lbName) throws Exception {
        LoadBalancers loadBalancers = domain.getExtensionByType(LoadBalancers.class);
        if (loadBalancers == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLoadbalancer", lbName));
        }
        LoadBalancer loadBalancer = loadBalancers.getLoadBalancer(lbName);
        if (loadBalancer == null) {
            throw new Exception(LbLogUtil.getStringManager().getString("UnableToGetLoadbalancer", lbName));
        }
        return loadBalancer;
    }

    /**
     * exports the loadbalancer.xml from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config
     * @param out OutputStream into which the loadbalancer.xml is written
     */
    public static void exportXml(LoadbalancerReader lbRdr, OutputStream out)
            throws Exception {

        // tranform the data using visitor pattern
        Loadbalancer _lb = new Loadbalancer();

        LoadbalancerVisitor lbVstr = new LoadbalancerVisitor(_lb);
        lbRdr.accept(lbVstr);

        try {
            String footer = LbLogUtil.getStringManager().getString("GeneratedFileFooter",
                    new Date().toString());
            // write the header
            _lb.graphManager().setDoctype(PUBLICID, SYSTEMID);
            _lb.write(out);
            out.write(footer.getBytes());
        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    /**
     * exports the workser.properties from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config
     * @param out OutputStream into which the loadbalancer.xml is written
     */
    public static void exportWorkerProperties(LoadbalancerReader lbRdr, OutputStream out)
            throws Exception {

        // tranform the data using visitor pattern
        Loadbalancer _lb = new Loadbalancer();

        Properties props = new Properties();

        String WORKER = "worker";
        String SEPARATOR = ".";
        String HOST = "host";
        String PORT = "port";
        String LIST = "list";
        String TYPE = "type";
        String TYPE_VALUE = "ajp13";
        String LBFACTOR = "lbfactor";
        String LBFACTOR_VALUE = "1";
        String SOCKET_KEEPALIVE = "socket_keepalive";
        String SOCKET_TIMEOUT = "socket_timeout";
        String SOCKET_KEEPALIVE_VALUE = "1";
        String SOCKET_TIMEOUT_VALUE = "300";
        String LOADBALANCER = "-lb";
        String BALANCER_WORKERS = "balance_workers";
        String LB = "lb";
        String CONTEXT_ROOT_MAPPING="CONTEXT_ROOT_MAPPING";
        String APP="APP";
        StringBuffer buffer = new StringBuffer();

        String workerList = "";

        LoadbalancerVisitor lbVstr = new LoadbalancerVisitor(_lb);
        lbRdr.accept(lbVstr);

        ClusterReader clusterReaders[] = lbRdr.getClusters();

        int c;
        buffer.append("worker.properties");

        for(int i=0;i<clusterReaders.length;i++) {
            String clusterWorkerList = "";
            ClusterReader clusterReader = clusterReaders[i];
            String clusterName = clusterReader.getName();
            WebModuleReader webmoduleReaders[] = clusterReader.getWebModules();
            InstanceReader instanceReaders[] = clusterReader.getInstances();

            for(int j =0; j<instanceReaders.length;j++) {
                InstanceReader instanceReader = instanceReaders[j];
                String listenerHost = "";
                String listenerPort = "";
                StringTokenizer st = new StringTokenizer(instanceReader.getListeners(), " ");
                while (st.hasMoreElements()) {
                    String listener = st.nextToken();
                    if (listener.contains("ajp://")) {
                        listenerHost = listener.substring(listener.lastIndexOf("/") + 1, listener.lastIndexOf(":"));
                        listenerPort = listener.substring(listener.lastIndexOf(":") + 1, listener.length());
                        break;
                    }
                }
                String listenterName = instanceReader.getName();

                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + HOST, listenerHost);
                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + PORT, listenerPort);
                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + TYPE, TYPE_VALUE);
                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + LBFACTOR, LBFACTOR_VALUE);
                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + SOCKET_KEEPALIVE, SOCKET_KEEPALIVE_VALUE);
                props.setProperty(WORKER + SEPARATOR + listenterName + SEPARATOR + SOCKET_TIMEOUT, SOCKET_TIMEOUT_VALUE);
                workerList = workerList + listenterName + ",";
                clusterWorkerList = clusterWorkerList + listenterName + ",";
            }

            workerList = workerList + clusterName + LOADBALANCER + "," ;
            props.setProperty(WORKER+SEPARATOR+LIST,workerList.substring(0,workerList.length()-1));
            props.setProperty(WORKER+SEPARATOR+clusterName+LOADBALANCER + SEPARATOR+TYPE,LB);
            props.setProperty(WORKER+SEPARATOR+clusterName+LOADBALANCER + SEPARATOR+BALANCER_WORKERS,clusterWorkerList.substring(0,clusterWorkerList.length()-1));

            for (int m=0; m<webmoduleReaders.length;m++) {
               buffer.append("\n" + CONTEXT_ROOT_MAPPING+SEPARATOR+webmoduleReaders[m].getContextRoot()
                       +"="+clusterName+LOADBALANCER);
            }

        }

        try {
            
        props.store(out,buffer.toString());

        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }


   /**
     * exports the otd.properties from the config to the outputstream provided
     * @param ctx ConfigContext
     * @param lbConfigName name of lb-config
     */
    public static void exportOtdProperties(LoadbalancerReader lbRdr, OutputStream out)
            throws Exception {

        // tranform the data using visitor pattern
        Loadbalancer _lb = new Loadbalancer();

        Properties props = new Properties();

        String CLUSTER = "cluster";
        String LISTENER = "listeners";
        String WEB = "web-modules";
        String SEPARATOR = ".";
        StringBuffer buffer = new StringBuffer();


        LoadbalancerVisitor lbVstr = new LoadbalancerVisitor(_lb);
        lbRdr.accept(lbVstr);

        ClusterReader clusterReaders[] = lbRdr.getClusters();


        buffer.append("otd.properties");

        for(int i=0;i<clusterReaders.length;i++) {
            StringBuffer clusterHostList = new StringBuffer();
            String clusterWebList = "";
            ClusterReader clusterReader = clusterReaders[i];
            String clusterName = clusterReader.getName();
            WebModuleReader webmoduleReaders[] = clusterReader.getWebModules();
            InstanceReader instanceReaders[] = clusterReader.getInstances();

            for(int j =0; j<instanceReaders.length;j++) {
                InstanceReader instanceReader = instanceReaders[j];
                String listenerHost = "";
                String listenerPort = "";
                StringTokenizer st = new StringTokenizer(instanceReader.getListeners(), " ");
                while (st.hasMoreElements()) {
                    String listener = st.nextToken();
                    if (listener.contains("http://")) {
                        listenerHost = listener.substring(listener.lastIndexOf("/") + 1, listener.lastIndexOf(":"));
                        listenerPort = listener.substring(listener.lastIndexOf(":") + 1, listener.length());
                        break;
                    }
                }
                clusterHostList = clusterHostList.append(j > 0 ? "," : "").append(listenerHost).append(":").append(listenerPort);
            }

            props.setProperty(CLUSTER+SEPARATOR+clusterName+SEPARATOR+LISTENER,clusterHostList.toString());


            for (int m=0; m<webmoduleReaders.length;m++) {
               clusterWebList = clusterWebList + (m > 0 ? "," : "") + webmoduleReaders[m].getContextRoot();
            }

            props.setProperty(CLUSTER+SEPARATOR+clusterName+SEPARATOR+WEB,clusterWebList);
        }

        try {

        props.store(out,buffer.toString());

        } finally {
            if (out != null) {
                out.close();
                out = null;
            }
        }
    }

    private static final String PUBLICID =
            "-//Sun Microsystems Inc.//DTD Sun Java System Application Server 9.1//EN";
    private static final String SYSTEMID = "glassfish-loadbalancer_1_3.dtd";
}


