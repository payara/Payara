/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.cluster.SyncRequest;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.inject.Inject;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Usage: <br>
 * export-sync-bundle --target cluster_std-alone-instance [--retrieve <false>] [file_name]
 * <p>
 * --target      Cluster or stand alone server instance (required) <br>
 * --retrieve    When true, the zip file is downloaded under the specified file_name in local machine
 *	         When false, the zip file is exported under the the specified file_name on DAS
 *	         Default value is false. (optional)</br>
 * </p>
 * file_name    Specifies the file name and location of the synchronized content.<br>
 * If file_name is not specified and --retrieve=false, the default value is
 * install-root/domains/domain_name/sync/&lt;target&gt;-sync-bundle.zip.
 * If file_name is not specified and --retrieve=true, the default value is
 * &lt;target&gt;-sync-bundle.zip.  (optional)
 *
 * @author Byron Nevins
 * @author Jennifer Chou
 */
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@Service(name = "export-sync-bundle")
@PerLookup
//@TargetType(value={CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE})
@I18n("export-sync-bundle")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="export-sync-bundle", 
        description="export-sync-bundle")
})
public class ExportSyncBundle implements AdminCommand {

    @Param(name="target", optional = false)
    private String clusterInstance;
    @Param(name="retrieve", optional = true, defaultValue="false")
    private boolean isRetrieve;
    @Param(optional = true, primary = true)
    String file_name;
    
    @Inject @Optional
    private Servers servers;
    @Inject @Optional
    private Clusters clusters;
    @Inject
    private ServerSynchronizer serverSynchronizer;
    @Inject
    
    private ServerEnvironment env;
    private ActionReport report = null;
    private File syncBundleExport;
    private Logger logger = null;
    private Payload.Outbound payload = null;
    private Server instance;
    private Cluster cluster;
    private SyncRequest syncRequest = new SyncRequest();
    private static final String[] ALL_DIRS = new String[]{
        "config", "applications", "lib", "docroot", "config-specific"
    };
    
    private static final String SYNC_FAIL = "export.sync.bundle.fail";

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        report.setActionExitCode(ExitCode.SUCCESS);
        logger = context.getLogger();

        // we use our own private payload.  Don't use the one in the context!
        payload = PayloadImpl.Outbound.newInstance();


        try {
            if (!isValid())
                return;

            if (!setSyncBundleExportFile())
                return;

            syncRequest = new SyncRequest();
            syncRequest.instance = clusterInstance;

            if (!sync())
                return;

            // write to the das or temp file
            write();

            //all OK...download local file
            if (isRetrieve)
                pumpItOut(context);
        }
        catch (Exception e) {
            setError(Strings.get(SYNC_FAIL, e.toString()));
            logger.log(Level.SEVERE, Strings.get(SYNC_FAIL, e.toString()), e);
        }
    }

    private void pumpItOut(AdminCommandContext context) {
        String fileName = file_name != null && !file_name.isEmpty() ? file_name : getDefaultBundleName();
        File localFile = new File(fileName.replace('\\', '/'));
        Properties props = new Properties();
        File parent = localFile.getParentFile();
        if (parent == null) {
            parent = localFile;
        }
        props.setProperty("file-xfer-root", parent.getPath().replace('\\', '/'));
        URI parentURI = parent.toURI();
        try {
            context.getOutboundPayload().attachFile(
                    "application/octet-stream",
                    parentURI.relativize(localFile.toURI()),
                    "sync-bundle",
                    props,
                    syncBundleExport);
        } catch (IOException ex) {
            setError(Strings.get("export.sync.bundle.retrieveFailed", ex.getLocalizedMessage()));
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "fileURI: {0}", parentURI.relativize(localFile.toURI()));
                logger.log(Level.FINER, "file-xfer-root: {0}", parent.getPath().replace('\\', '/'));
                logger.log(Level.FINER, "file: {0}", syncBundleExport.getAbsolutePath());
            }
        }
    }

    private boolean sync() {
        for (String dir : ALL_DIRS) {
            syncRequest.dir = dir;

            if (!syncOne())
                return false;
        }

        return !hasError();
    }

    private void write() {
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(syncBundleExport));
            payload.writeTo(out);
        } catch (IOException ex) {
            setError(Strings.get("export.sync.bundle.exportFailed",
                    syncBundleExport.getAbsolutePath(), ex.getLocalizedMessage()));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.warning(Strings.get("export.sync.bundle.closeStreamFailed",
                            syncBundleExport.getAbsolutePath(), ex.getLocalizedMessage()));
                }
            }
        }
        if (!isRetrieve) {
            if (syncBundleExport.isFile()) {
                report.setMessage(Strings.get("export.sync.bundle.success", syncBundleExport.getAbsolutePath()));
            } else {
                setError(Strings.get("export.sync.bundle.fail", syncBundleExport.getAbsolutePath()));
            }
        }
    }

    private boolean syncOne() {
        if (instance != null) {
            serverSynchronizer.synchronize(instance, syncRequest, payload, report, logger);
        }
        if (cluster != null) { // Use one of the clustered instances
            List<Server> slist = cluster.getInstances();
            if (slist != null && !slist.isEmpty()) {
                Server s = slist.get(0);
                serverSynchronizer.synchronize(s, syncRequest, payload, report, logger);
            }
        }

        // synchronize() will be set to FAILURE if there were problems
        return !hasError();
    }

    private File getDefaultBundle() {
        return new File(new File(env.getInstanceRoot(), "sync"), getDefaultBundleName());
    }

    private String getDefaultBundleName() {
        return clusterInstance + "-sync-bundle.zip";
    }

    private boolean isValid() {
        // verify the cluster or stand-alone server name corresponds to reality!
        if (servers != null)
            instance = servers.getServer(clusterInstance);
        if (clusters != null) {
            cluster = clusters.getCluster(clusterInstance);
            if (cluster != null) {
                List<Server> list = cluster.getInstances();
                if (list == null || list.isEmpty()) {
                    setError(Strings.get("sync.empty_cluster", clusterInstance));
                    return false;
                }
            }
        }
        if (instance == null && cluster == null) {
            setError(Strings.get("sync.unknown.instanceOrCluster", clusterInstance));
            return false;
        }

        return true;
    }

    private boolean setSyncBundleExportFile() {
        if (isRetrieve) {
            try {
                syncBundleExport = File.createTempFile("GlassFishSyncBundle", ".zip");
                syncBundleExport.deleteOnExit();
            } catch (Exception ex) {
                syncBundleExport = null;
                setError(Strings.get("sync.bad_temp_file", ex.getLocalizedMessage()));
                return false;
            }
        } else {
            File f = null;
            if (file_name != null && !file_name.isEmpty()) {
                f = new File(file_name);
                if (f.isDirectory()) {
                    //Existing directory specified, <target>-sync-bundle.zip is created in specified directory.
                    f = new File(f, getDefaultBundleName());
                }
            } else {
                //No operand specified, <target>-sync-bundle.zip is created in install-root/domains/domain_name/sync
                f = getDefaultBundle();
            }
            
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                if (!f.getParentFile().mkdirs()) {
                    setError(Strings.get("export.sync.bundle.createDirFailed", f.getParentFile().getPath()));
                    return false;
                }
            }
            syncBundleExport = SmartFile.sanitize(f);
        }
        return true;
    }

    private void setError(String msg) {
        report.setActionExitCode(ExitCode.FAILURE);
        report.setMessage(msg);
    }

    private boolean hasError() {
        return report.getActionExitCode() != ExitCode.SUCCESS;
    }
    
}
