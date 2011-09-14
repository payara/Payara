/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.lbplugin;

import java.io.BufferedOutputStream;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.virtualization.spi.VirtualMachine.PropertyName;

/**
 * @author kshitiz
 */
@Service(name="Native-LB")
public class ApacheNativeTemplateCustomizer implements TemplateCustomizer {

    @Inject
    private ServerContext serverContext;

    private static final String LB_INSTALL_LOC = "/u01/glassfish/lb";

    private static final String LB_ZIP_FILE_LOC = "config" + File.separator + "lb.zip";

    public void customize(VirtualCluster cluster, VirtualMachine virtualMachine) throws VirtException {

        File lbZipFile = new File(serverContext.getInstallRoot() + File.separator + LB_ZIP_FILE_LOC);
        if(!lbZipFile.exists()){
            String msg = "File " + lbZipFile.getAbsolutePath() + "does not exist. Cannot create native load-balancer";
            LBPluginLogger.getLogger().log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }

        File lbInstallDir = new File(LB_INSTALL_LOC);

        if(lbInstallDir.exists()){
            String msg = "Directory " + lbInstallDir + " already exists. Cannot create native load-balancer";
            LBPluginLogger.getLogger().log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }else{
            lbInstallDir = lbInstallDir.getParentFile();
            lbInstallDir.mkdirs();
        }

        try {
            ZipFile zipFile = new ZipFile(lbZipFile);

            Enumeration entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();

                if (entry.isDirectory()) {
                    // Assume directories are stored parents first then children.
                    (new File(lbInstallDir, entry.getName())).mkdir();
                    continue;
                }

                File outputFile = new File(lbInstallDir, entry.getName());

                copyInputStream(zipFile.getInputStream(entry),
                        new BufferedOutputStream(new FileOutputStream(outputFile)));
                if(outputFile.getParent().endsWith("/scripts")
                        || outputFile.getParent().endsWith("/bin")){
                    outputFile.setExecutable(true);
                }
            }

            zipFile.close();
        } catch (IOException ioe) {
            String msg = "Unable to unzip file " + lbZipFile.getAbsolutePath()
                    + ".  Creation of native load-balancer failed";
            LBPluginLogger.getLogger().log(Level.SEVERE, msg);
            clean(virtualMachine);
            throw new RuntimeException(msg, ioe);
        }

    }

    private void copyInputStream(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[1024];
        int len;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }

    public void start(VirtualMachine virtualMachine) {
    }

    public void stop(VirtualMachine virtualMachine) {
    }

    public void clean(VirtualMachine virtualMachine) {
        File lbInstallDir = new File(LB_INSTALL_LOC);

        if(lbInstallDir.exists()){
            File[] files = lbInstallDir.listFiles();
            for(int i=0; i < files.length; i++){
                files[i].delete();
            }
            lbInstallDir.delete();
        }

    }
}
