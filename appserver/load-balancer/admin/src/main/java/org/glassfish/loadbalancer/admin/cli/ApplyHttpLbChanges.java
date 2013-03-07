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

package org.glassfish.loadbalancer.admin.cli;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;

import org.glassfish.loadbalancer.admin.cli.connection.ConnectionManager;
import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;

import org.netbeans.modules.schema2beans.Schema2BeansException;

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.loadbalancer.config.LoadBalancer;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.helper.LbConfigHelper;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;

/**
 * Class to publish the loadbalancer.xml to the physical loadbalancer.
 * 
 * @author Kshitiz Saxena
 */
@Service(name = "apply-http-lb-changes")
@PerLookup
@I18n("apply.http.lb.changes")
@RestEndpoints({
    @RestEndpoint(configBean=LoadBalancer.class,
        opType=RestEndpoint.OpType.POST, 
        path="apply-http-lb-changes", 
        description="apply-http-lb-changes",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class ApplyHttpLbChanges implements AdminCommand {

    @Inject
    Domain domain;
    @Inject
    ApplicationRegistry appRegistry;
    @Param(name = "ping", optional = true)
    String ping;
    @Param(name = "lb-name", primary = true)
    String lbName;

    /** Creates a new instance of LbConfigPublisher */
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        try {
            process();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Throwable t) {
            String msg = LbLogUtil.getStringManager().getString("ApplyHttpLbChangesFailed", t.getMessage());
            LbLogUtil.getLogger().log(Level.WARNING, msg);
            if (LbLogUtil.getLogger().isLoggable(Level.FINE)) {
                LbLogUtil.getLogger().log(Level.FINE, "Exception when applying http lb changes", t);
            }
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(t.getMessage());
            report.setFailureCause(t);
        }
    }

    public void process() throws Exception {

        ConnectionManager _connectionManager = null;
        LoadBalancer lb = LbConfigHelper.getLoadBalancer(domain, lbName);
        String lbHost = lb.getDeviceHost();
        String lbPort = lb.getDevicePort();
        String lbProxyHost = null;
        String lbProxyPort = null;
        Property lbProxyHostProperty = lb.getProperty(SSL_PROXY_HOST_PROPERTY);
        if (lbProxyHostProperty != null) {
            lbProxyHost = lbProxyHostProperty.getValue();
            Property lbProxyPortProperty = lb.getProperty(SSL_PROXY_PORT_PROPERTY);
            if (lbProxyPortProperty != null) {
                lbProxyPort = lbProxyPortProperty.getValue();
            }
        }

        boolean isSec = true;
        Property isSecProperty = lb.getProperty(IS_SECURE_PROPERTY);
        if (isSecProperty != null) {
            isSec = Boolean.getBoolean(isSecProperty.getValue());
        }

        _connectionManager = new ConnectionManager(lbHost, lbPort, lbProxyHost, lbProxyPort, lbName, isSec);
        if (ping != null && Boolean.valueOf(ping).booleanValue()) {
            ping(_connectionManager);
        } else {
            publish(_connectionManager, domain, lb.getLbConfigName());
        }
    }

    /**
     * publishes the loadbalancer.xml to the physical loadbalancer.
     * @throws java.io.IOException 
     * @throws com.sun.enterprise.config.ConfigException 
     * @throws org.netbeans.modules.schema2beans.Schema2BeansException 
     */
    public void publish(ConnectionManager _connectionManager, Domain domain, String lbConfigName) throws IOException,
            Exception {


        // check if the lb exists
        LoadbalancerReader lbr = LbConfigHelper.getLbReader(domain, appRegistry, lbConfigName);

        HttpURLConnection conn =
                _connectionManager.getConnection(LB_UPDATE_CONTEXT_ROOT);
        OutputStream out = null;
        try {
            conn.setDoOutput(true);
            conn.setRequestMethod(POST);
            conn.connect();
            out = conn.getOutputStream();
            LbConfigHelper.exportXml(lbr, out);
            out.flush();
            lbr.getLbConfig().setLastApplied();
        } catch (UnknownHostException uhe) {
            throw new IOException(LbLogUtil.getStringManager().getString("CannotConnectToLBHost", uhe.getMessage()), uhe);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (out != null && conn != null) {
                int code = conn.getResponseCode();
                String response = conn.getResponseMessage();
                out.close();
                conn.disconnect();
                out = null;
                if (code != HttpURLConnection.HTTP_OK) {
                    String url = conn.getURL().toString();
                    conn = null;
                    throw new IOException(LbLogUtil.getStringManager().getString("HttpError", Integer.valueOf(code), response, url));
                }
                conn = null;
            }
        }
    }

    public boolean ping(ConnectionManager _connectionManager) throws IOException {
        HttpURLConnection conn = _connectionManager.getConnection(LB_UPDATE_CONTEXT_ROOT);
        conn.setRequestMethod(GET);
        conn.connect();
        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            return false;
        }
        return true;
    }
    private static final String LB_UPDATE_CONTEXT_ROOT = "/lbconfigupdate";
    private static final String IS_SECURE_PROPERTY = "is-device-ssl-enabled";
    private static final String SSL_PROXY_HOST_PROPERTY = "ssl-proxy-host";
    private static final String SSL_PROXY_PORT_PROPERTY = "ssl-proxy-port";
    private static final String GET = "GET";
    private static final String POST = "POST";
}
