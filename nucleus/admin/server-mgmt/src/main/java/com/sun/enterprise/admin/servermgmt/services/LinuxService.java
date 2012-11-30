/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.servermgmt.services;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ObjectAnalyzer;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.io.ServerDirs;
import java.io.File;
import java.lang.String;
import java.util.*;
import java.util.ArrayList;
import java.util.LinkedList;
import static com.sun.enterprise.admin.servermgmt.services.Constants.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Byron Nevins
 */
public class LinuxService extends NonSMFServiceAdapter {
    static boolean apropos() {
        if (LINUX_HACK)
            return true;

        return OS.isLinux();
    }

    LinuxService(ServerDirs dirs, AppserverServiceType type) {
        super(dirs, type);
        if (!apropos()) {
            // programmer error
            throw new IllegalArgumentException(Strings.get("internal.error",
                    "Constructor called but Linux Services are not available."));
        }
        setRcDirs();
    }

    @Override
    public void initializeInternal() {
        try {
            getTokenMap().put(SERVICEUSER_START_TN, getServiceUserStart());
            getTokenMap().put(SERVICEUSER_STOP_TN, getServiceUserStop());
            setTemplateFile(TEMPLATE_FILE_NAME);
            checkFileSystem();
            setTarget();
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void createServiceInternal() {
        try {
            handlePreExisting(info.force);
            ServicesUtils.tokenReplaceTemplateAtDestination(getTokenMap(), getTemplateFile().getPath(), target.getPath());
            trace("Target file written: " + target);
            trace("**********   Object Dump  **********\n" + this.toString());

            if (deleteLinks() == 0 && !info.dryRun)
                System.out.println(Strings.get("linux.services.uninstall.good"));
            else
                trace("No preexisting Service with that name was found");

            install();
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final void deleteServiceInternal() {
        try {
            uninstall();
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public final String getSuccessMessage() {
        if (info.dryRun)
            return Strings.get("dryrun");

        return Strings.get("LinuxServiceCreated",
                info.serviceName,
                info.type.toString(),
                target,
                getFinalUser(),
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
        }
        else {
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

    private void setRcDirs() {
        // Yes -- they differ on different platforms!
        // I Know what Sol10, Ubuntu, Debian, SuSE, RH and OEL look like
        // on SuSE the rc?.d dirs are in /init.d/
        // On RH, OEL they are linked dirs to the real dirs under /etc/rc.d/
        // On Ubuntu they are real dirs in /etc

        // try to make this as forgiving as possible.
        File[] rcDirs = new File[8];    // 0, 1, 2...6, S
        if (!setRcDirs(new File(Constants.ETC), rcDirs))
            if (!setRcDirs(new File(Constants.INITD), rcDirs))
                throw new RuntimeException(Strings.get("no_rc2"));

        // now we have an array of at least some rc directories.
        addKills(rcDirs);
        addStarts(rcDirs);
    }

    private boolean setRcDirs(File dir, File[] rcDirs) {

        if(LINUX_HACK) {
            return true;
        }
        // some have 4 missing, some have S missing etc.  All seem to have 5
        if (!new File(dir, "rc5.d").isDirectory())
            return false;

        for (int i = 0; i < 7; i++) {
            rcDirs[i] = new File(dir, "rc" + i + ".d");
        }

        rcDirs[7] = new File(dir, "rcS.d");

        for (int i = 0; i < 8; i++) {
            rcDirs[i] = validate(rcDirs[i]);
        }

        return true;
    }

    private void addKills(File[] rcDirs) {
        if (rcDirs[0] != null)
            killDirs.add(rcDirs[0]);
        if (rcDirs[1] != null)
            killDirs.add(rcDirs[1]);
        if (rcDirs[6] != null)
            killDirs.add(rcDirs[6]);
        if (rcDirs[7] != null)
            killDirs.add(rcDirs[7]);
    }

    private void addStarts(File[] rcDirs) {
        if (rcDirs[2] != null)
            startDirs.add(rcDirs[2]);
        if (rcDirs[3] != null)
            startDirs.add(rcDirs[3]);
        if (rcDirs[4] != null)
            startDirs.add(rcDirs[4]);
        if (rcDirs[5] != null)
            startDirs.add(rcDirs[5]);
    }

    private File validate(File rcdir) {
        if (rcdir == null)
            return null;

        // On OEL for instance the files are links to the real dir like this:
        //  /etc/rc0.d --> /etc/rc.d/rc0.d
        // let's use the REAL dirs just to be safe...
        rcdir = FileUtils.safeGetCanonicalFile(rcdir);

        if (!rcdir.isDirectory())
            return null;

        return rcdir;
    }

    private void checkFileSystem() {
        File initd = new File(INITD);
        checkDir(initd, "no_initd");
    }

    /**
     * Make sure that the dir exists and that we can write into it
     */
    private void checkDir(File dir, String notDirMsg) {
        if (!dir.isDirectory())
            throw new RuntimeException(Strings.get(notDirMsg, dir));

        if (!dir.canWrite())
            throw new RuntimeException(Strings.get("no_write_dir", dir));
    }

    private void handlePreExisting(boolean force) {
        if (isPreExisting()) {
            if (force) {
                boolean result = target.delete();
                if (!result || isPreExisting()) {
                    throw new RuntimeException(Strings.get("services.alreadyCreated", target, "rm"));
                }
            }
        }
    }

    private boolean isPreExisting() {
        return target.isFile();
    }

    private void install() throws ProcessManagerException {
        createLinks();
    }

    // meant to be overridden bu subclasses
    int uninstall() {
        if (target.delete())
            trace("Deleted " + target);

        return deleteLinks();
    }

    private int deleteLinks() {
        trace("Deleting link files...");
        List<File> deathRow = new LinkedList<File>();

        if (!StringUtils.ok(targetName)) // invariant
            throw new RuntimeException("Programmer Internal Error");

        String regexp = REGEXP_PATTERN_BEGIN + targetName;

        List<File> allDirs = new ArrayList<File>(killDirs);
        allDirs.addAll(startDirs);

        for (File dir : allDirs) {
            File[] matches = FileUtils.findFilesInDir(dir, regexp);

            if (matches.length < 1)
                continue; // perfectly normal
            else if (matches.length == 1)
                deathRow.add(matches[0]);
            else {
                tooManyLinks(matches);  // error!!
            }
        }

        for (File f : deathRow) {
            if (info.dryRun) {
                dryRun("Would have deleted: " + f);
            }
            else {
                if (!f.delete())
                    throw new RuntimeException(Strings.get("cant_delete", f));
                else
                    trace("Deleted " + f);
            }
        }
        return deathRow.size();
    }

    private void createLinks() {
        String[] cmds = new String[4];
        cmds[0] = "ln";
        cmds[1] = "-s";
        cmds[2] = target.getAbsolutePath();

        createLinks(cmds, kFile, killDirs);
        createLinks(cmds, sFile, startDirs);
    }

    // This is what happens when you hate copy&paste code duplication.  Lots of methods!!
    private void createLinks(String[] cmds, String linkname, List<File> dirs) {
        for (File dir : dirs) {
            File link = new File(dir, linkname);
            cmds[3] = link.getAbsolutePath();
            String cmd = toString(cmds);

            if (LINUX_HACK)
                trace(cmd);
            else if (info.dryRun)
                dryRun(cmd);
            else
                createLink(link, cmds);
        }
    }

    private void createLink(File link, String[] cmds) {
        try {
            ProcessManager mgr = new ProcessManager(cmds);
            mgr.setEcho(false);
            mgr.execute();
            trace("Create Link Output: " + mgr.getStdout() + mgr.getStderr());
            link.setExecutable(true, false);
            trace("Created link file: " + link);
        }
        catch (ProcessManagerException e) {
            throw new RuntimeException(Strings.get("ln_error", toString(cmds), e));
        }
    }

    private void tooManyLinks(File[] matches) {
        // this is complicated enough to turn it into a method
        StringBuffer theMatches = new StringBuffer();
        boolean first = true;
        for (File f : matches) {
            if (first)
                first = false;
            else
                theMatches.append("\n");

            theMatches.append(f.getAbsolutePath());
        }
        throw new RuntimeException(Strings.get("too_many_links", theMatches));
    }

    private void setTarget() {
        targetName = "GlassFish_" + info.serverDirs.getServerName();
        target = new File(INITD + "/" + targetName);
        kFile = "K" + info.kPriority + targetName;
        sFile = "S" + info.sPriority + targetName;
    }

    private String getServiceUserStart() {
        // if the user is root (e.g. called with sudo and no serviceuser arg given)
        // then do NOT specify a user.
        // on the other hand -- if they specified one or they are logged in as a'privileged'
        // user then use that account.
        String u = getFinalUserButNotRoot();
        hasStartStopTokens = (u != null);

        if (hasStartStopTokens)
            return "su --login " + u + " --command \"";
        return "";
    }

    private String getServiceUserStop() {
        if(hasStartStopTokens)
            return "\"";
        return "";
    }

    private String getFinalUser() {
        if (StringUtils.ok(info.serviceUser))
            return info.serviceUser;
        else
            return info.osUser;
    }

    private String getFinalUserButNotRoot() {
        String u = getFinalUser();

        if ("root".equals(u))
            return null;

        return u;
    }

    private String toString(String[] arr) {
        // for creating messages/error reports
        StringBuilder sb = new StringBuilder();

        for (String s : arr)
            sb.append(s).append(" ");

        return sb.toString();
    }
    private String targetName;
    File target;
    private static final String TEMPLATE_FILE_NAME = "linux-service.template";
    private List<File> killDirs = new ArrayList<File>();
    private List<File> startDirs = new ArrayList<File>();
    private String sFile;
    private String kFile;
    private boolean hasStartStopTokens = false;
}
