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

package org.glassfish.virtualization.util;

import org.glassfish.internal.api.ServerContext;
import org.glassfish.virtualization.ShellExecutor;
import org.glassfish.virtualization.os.Disk;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.IOException;

/**
 * Abstraction for a virtual disk, based on OS scripts for implementation
 */
@Service
@Scoped(PerLookup.class)
public class ScriptBasedDisk implements Disk {

    @Inject
    ShellExecutor shell;

    @Inject
    ServerContext env;

    int loopCounter = 0;
    File mountPoint=null;

    public File getScriptPath() {

        File file = new File(env.getInstallRoot(), "config");
        File parentDir = new File(file, "libvirt"); // needs to be changed.
        // we try to locate the right directory using this runtime operating system.
        // if not found, we user the generic linux scripts.
        File osDir = new File(parentDir, RuntimeContext.getEncodedOS());
        if (!osDir.exists()) {
            osDir = new File(parentDir,  "linux");
        }
        return osDir;
    }

    @Override
    public int create(File path, int size, File mountPoint) throws IOException {
        Process p;
        try{
            p = shell.execute(new File("/usr/bin"),
                    "sudo " + getScriptPath() + "/create-disk.sh " + path.getAbsolutePath() + " " +
                    size + " " + mountPoint.getAbsolutePath());
            p.waitFor();
            RuntimeContext.logger.info(shell.output(p));
            RuntimeContext.logger.info(shell.error(p));
            this.mountPoint = mountPoint;
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
        loopCounter=p.exitValue();
        return p.exitValue();

    }

    @Override
    public int mount(File path, File mountPoint) throws IOException {
        Process p;
        try{
            p = shell.execute(new File("/usr/bin"),
                    "sudo " + getScriptPath() + "/mount-disk.sh " + path.getAbsolutePath() + " " +
                    mountPoint.getAbsolutePath());
            p.waitFor();
            RuntimeContext.logger.info(shell.output(p));
            RuntimeContext.logger.info(shell.error(p));
            this.mountPoint = mountPoint;
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
        loopCounter=p.exitValue();
        return p.exitValue();

    }

    @Override
    public int umount() throws IOException {
        if (mountPoint==null) return -1;
        try{
            Process p = shell.execute(new File("/usr/bin"),
                    "sudo " + getScriptPath() + "/unmount-disk.sh " +
                            mountPoint.getAbsolutePath() + " " + loopCounter);
            RuntimeContext.logger.info(shell.output(p));
            RuntimeContext.logger.info(shell.error(p));
            p.waitFor();
            return p.exitValue();
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int createISOFromDirectory(File sourceDirectory, File outputISOFile) throws IOException {
        try{
            File scriptLoc = new File(getScriptPath(),"mkisofs.sh");
            scriptLoc.setExecutable(true);
            Process p = shell.execute(getScriptPath(),
                      "mkisofs.sh " +
                            sourceDirectory.getAbsolutePath() + " " + outputISOFile.getAbsolutePath());
            RuntimeContext.logger.info(shell.output(p));
            RuntimeContext.logger.info(shell.error(p));
            p.waitFor();
            return p.exitValue();
        } catch(InterruptedException e) {
            throw new IOException(e);
        }
    }
}
