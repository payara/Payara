/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package com.sun.enterprise.admin.servermgmt.services;

import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ObjectAnalyzer;
import com.sun.enterprise.util.io.ServerDirs;

import java.io.File;

import static com.sun.enterprise.admin.servermgmt.services.Constants.*;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of create-service and delete-service for SystemD based Linux systems.
 *
 * @author Petr Aubrecht
 */
public class LinuxSystemDService extends NonSMFServiceAdapter {

    private String targetName;
    File target;
    private static final String TEMPLATE_FILE_NAME = "linux-systemd-service.template";

    static boolean apropos() {
        return OS.isLinuxSystemDBased();
    }

    LinuxSystemDService(ServerDirs dirs, AppserverServiceType type) {
        super(dirs, type);
        if (!apropos()) {
            // programmer error
            throw new IllegalArgumentException(Strings.get("internal.error",
                    "Constructor called but Linux SystemD Services are not available."));
        }
    }

    @Override
    public void initializeInternal() {
        try {
            setTemplateFile(TEMPLATE_FILE_NAME);
            checkFileSystem();
            setTarget();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ex) {
            throw new OSServiceAdapterException(ex);
        }
    }

    @Override
    public final void createServiceInternal() {
        try {
            getTokenMap().put(PID_FILE_TN, info.serverDirs.getPidFile().getPath());
            getTokenMap().put(SERVICE_PROPERTIES_TN, formatAsServiceEnvironmentRecords(tokensAndValues()));
            handlePreExisting(info.force);
            if (info.dryRun) {
                trace(Strings.get("dryrun"));
            } else {
                ServicesUtils.tokenReplaceTemplateAtDestination(getTokenMap(), getTemplateFile().getPath(), target.getPath());
                trace("Target file written: " + target);
                trace("**********   Object Dump  **********\n" + this.toString());
                install();
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OSServiceAdapterException(ex);
        }
    }

    @Override
    public final void deleteServiceInternal() {
        try {
            if (info.dryRun) {
                trace(Strings.get("dryrun"));
            }
            uninstall();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OSServiceAdapterException(ex);
        }
    }

    @Override
    public final String getSuccessMessage() {
        if (info.dryRun) {
            return Strings.get("dryrun");
        }

        return Strings.get("LinuxSystemDServiceCreated",
                info.serviceName,
                info.type.toString(),
                target,
                getServiceUser(),
                target.getName());
    }

    // called by outside caller (createService)
    @Override
    public final void writeReadmeFile(String msg) {
        File f = new File(getServerDirs().getServerDir(), README);

        ServicesUtils.appendTextToFile(f, msg);
    }

    @Override
    public final String toString() {
        return ObjectAnalyzer.toStringWithSuper(this);
    }

    @Override
    public final String getLocationArgsStart() {
        if (isDomain()) {
            return " --domaindir " + getServerDirs().getServerParentDir().getPath() + " ";
        } else {
            return " --nodedir " + getServerDirs().getServerGrandParentDir().getPath()
                    + " --node " + getServerDirs().getServerParentDir().getName() + " ";
        }
    }

    @Override
    public final String getLocationArgsStop() {
        // exactly the same on Linux
        return getLocationArgsStart();
    }
    ///////////////////////////////////////////////////////////////////////
    //////////////////////////   ALL PRIVATE BELOW    /////////////////////
    ///////////////////////////////////////////////////////////////////////

    private void checkFileSystem() {
        File configDir = new File(SYSTEMD_CONFIG_DIR);
        checkDir(configDir, "no_systemd");
    }

    /**
     * Make sure that the dir exists and that we can write into it
     */
    private void checkDir(File dir, String notDirMsg) {
        if (!dir.isDirectory()) {
            throw new RuntimeException(Strings.get(notDirMsg, dir));
        }

        if (!dir.canWrite()) {
            throw new RuntimeException(Strings.get("no_write_dir", dir));
        }
    }

    private void handlePreExisting(boolean force) {
        if (isPreExisting() && force) {
            boolean result = target.delete();
            if (!result || isPreExisting()) {
                throw new RuntimeException(Strings.get("services.alreadyCreated", target, "rm"));
            }
        }
    }

    private boolean isPreExisting() {
        return target.isFile();
    }

    private void install() throws ProcessManagerException {
    }

    // meant to be overridden by subclasses
    int uninstall() {
        if (info.dryRun) {
            dryRun("Would have deleted: " + target);
        } else if (target.delete()) {
            trace("Deleted " + target);
        }

        return 0;
    }

    private void setTarget() {
        targetName = "payara_" + info.serverDirs.getServerName() + ".service";
        target = new File(SYSTEMD_CONFIG_DIR, targetName);
    }

    private String formatAsServiceEnvironmentRecords(Map<String, String> tokensAndValues) {
        return tokensAndValues.entrySet().stream()
                .map(entry -> "Environment=" + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}
