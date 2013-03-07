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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.loadbalancer.config.LoadBalancer;

import java.net.URI;

import org.glassfish.api.admin.AdminCommandContext;

import org.glassfish.loadbalancer.admin.cli.reader.api.LoadbalancerReader;

import java.util.HashSet;
import java.util.List;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.ServerEnvironment;

import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.loadbalancer.admin.cli.reader.impl.LoadbalancerReaderImpl;
import org.glassfish.loadbalancer.admin.cli.helper.LbConfigHelper;
import org.glassfish.loadbalancer.config.LbConfig;

import javax.inject.Inject;

/**
 * Export load-balancer xml
 *
 * @author Kshitiz Saxena
 */
@Service(name = "export-http-lb-config")
@PerLookup
@I18n("export.http.lb.config")
@RestEndpoints({
    @RestEndpoint(configBean=LbConfig.class,
        opType=RestEndpoint.OpType.POST, // TODO: Should probable be GET 
        path="export-http-lb-config", 
        description="export-http-lb-config"),
    @RestEndpoint(configBean=LoadBalancer.class,
        opType=RestEndpoint.OpType.POST, // TODO: Should probable be GET 
        path="export-http-lb-config", 
        description="export-http-lb-config",
        params={
            @RestParam(name="lbname", value="$parent")
        })
})
public class ExportHttpLbConfig implements AdminCommand {

    @Param(name = "lbtargets", separator = ',', optional = true)
    List<String> target;
    @Param(name = "config", optional = true)
    String lbConfigName;
    @Param(name = "lbname", optional = true)
    String lbName;
    @Param(name = "retrievefile", optional = true, defaultValue = "false")
    boolean retrieveFile;
    @Param(name = "file_name", optional = true, primary = true)
    String fileName;
    @Param(name = "property", optional = true, separator = ':')
    Properties properties;
    @Inject
    Domain domain;
    @Inject
    ApplicationRegistry appRegistry;
    @Inject
    ServerEnvironment env;

    private static final String DEFAULT_LB_XML_FILE_NAME =
            "loadbalancer.xml";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        try {
            String msg = process(context);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage(msg);
        } catch (Throwable t) {
            String msg = LbLogUtil.getStringManager().getString("ExportHttpLbConfigFailed", t.getMessage());
            LbLogUtil.getLogger().log(Level.WARNING, msg);
            if (LbLogUtil.getLogger().isLoggable(Level.FINE)) {
                LbLogUtil.getLogger().log(Level.FINE, "Exception when exporting http lb config", t);
            }
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(t.getMessage());
            report.setFailureCause(t);
        }
    }

    public String process(AdminCommandContext context) throws Exception {

        LoadbalancerReader lbr = null;
        if (lbName != null && lbConfigName == null && target == null) {
            LoadBalancer lb = LbConfigHelper.getLoadBalancer(domain, lbName);
            lbr = LbConfigHelper.getLbReader(domain, appRegistry, lb.getLbConfigName());
        } else if (lbConfigName != null && lbName == null && target == null) {
            lbr = LbConfigHelper.getLbReader(domain, appRegistry, lbConfigName);
        } else if (target != null && lbName == null && lbConfigName == null) {
            Set<String> clusters = new HashSet<String>();
            clusters.addAll(target);
            lbr = new LoadbalancerReaderImpl(domain, appRegistry, clusters, properties);
        } else {
            String msg = LbLogUtil.getStringManager().getString("ExportHttpLbConfigInvalidArgs");
            throw new Exception(msg);
        }

        if (fileName == null) {
            String configName = lbr.getName();
            if (configName != null) {
                fileName = DEFAULT_LB_XML_FILE_NAME + "." + configName;
            } else {
                fileName = DEFAULT_LB_XML_FILE_NAME;
            }
        }

        File lbConfigFile = new File(fileName);
        if (!lbConfigFile.isAbsolute() && !retrieveFile) {
            File loadbalancerDir = new File(env.getInstanceRoot(),
                    "load-balancer");
            if (!loadbalancerDir.exists()) {
                boolean isMkdirSuccess = loadbalancerDir.mkdir();
                if(!isMkdirSuccess){
                	String msg = LbLogUtil.getStringManager().getString(
                            "directoryCreationFailed");
                    throw new Exception(msg);
                }
            }
            lbConfigFile = new File(loadbalancerDir, fileName);
        }

            File tmpLbXmlFile = null;
            if (retrieveFile) {
                tmpLbXmlFile = File.createTempFile("load-balancer", ".xml");
                tmpLbXmlFile.deleteOnExit();
            } else {
                if (lbConfigFile.exists()) {
                    String msg = LbLogUtil.getStringManager().getString(
                            "FileExists", lbConfigFile.getPath());
                    throw new Exception(msg);
                }

                if (!(lbConfigFile.getParentFile().exists())) {
                    String msg = LbLogUtil.getStringManager().getString(
                            "ParentFileMissing", lbConfigFile.getParent());
                    throw new Exception(msg);
                }
                tmpLbXmlFile = lbConfigFile;
            }

            FileOutputStream fo = null;

            try {
                fo = new FileOutputStream(tmpLbXmlFile);
                LbConfigHelper.exportXml(lbr, fo);
                if (retrieveFile) {
                    retrieveLbConfig(context, lbConfigFile, tmpLbXmlFile);
                }
                LbConfig lbConfig = lbr.getLbConfig();
                //Check for the case when lbtargets are provided
                //In such a case, lbconfig will be null
                if(lbConfig != null){
                    lbConfig.setLastExported();
                }
                String msg = LbLogUtil.getStringManager().getString(
                        "GeneratedFileLocation", lbConfigFile.toString());
                return msg;
            } finally {
                if (fo != null) {
                    fo.close();
                    fo = null;
                }
            }
        }
    

    private void retrieveLbConfig(AdminCommandContext context, File lbConfigFile,
                                  File tmpLbXmlFile) throws Exception {
        File localFile = lbConfigFile;
        Properties props = new Properties();
        File parent = localFile.getParentFile();
        if (parent == null) {
            parent = localFile;
        }
        props.setProperty("file-xfer-root", parent.getPath().replace('\\', '/'));
        URI parentURI = parent.toURI();
        try {
            context.getOutboundPayload().attachFile(
                    "text/xml",
                    parentURI.relativize(localFile.toURI()),
                    "sync-load-balancer-xml",
                    props,
                    tmpLbXmlFile);
        } catch (IOException ex) {
            String msg = LbLogUtil.getStringManager().getString(
                    "RetrieveFailed", lbConfigFile.getAbsolutePath());
            throw new Exception(msg, ex);
        }
    }
}
