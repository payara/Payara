/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.io.ServerDirs;
import java.io.*;
import java.util.*;
import static com.sun.enterprise.admin.servermgmt.services.Constants.*;

/**
 *
 * @author bnevins
 */
public abstract class ServiceAdapter implements Service {
    ServiceAdapter(ServerDirs serverDirs, AppserverServiceType type) {
        info = new PlatformServicesInfo(serverDirs, type);
    }

    @Override
    public final void deleteService() {
        info.validate();
        initialize();
        initializeInternal();
        deleteServiceInternal();
    }

    @Override
    public PlatformServicesInfo getInfo() {
        return info;
    }

    @Override
    public final boolean isDomain() {
        return info.type == AppserverServiceType.Domain;
    }

    @Override
    public final boolean isInstance() {
        return info.type == AppserverServiceType.Instance;
    }

    @Override
    public final ServerDirs getServerDirs() {
        return info.serverDirs;
    }

    @Override
    public final void createService() {
        info.validate();
        initialize();
        initializeInternal();
        createServiceInternal();
    }

    @Override
    public String getLocationArgsRestart() {
        return getLocationArgsStart();
    }

    //////////////////////////////////////////////////////////////////////////
    ////////////////   pkg-private     ///////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////
    void initialize() {
        final String parentPath = info.serverDirs.getServerParentDir().getPath();
        final String serverName = info.serverDirs.getServerName();
        setAsadminCredentials();

        getTokenMap().put(CFG_LOCATION_TN, parentPath);
        getTokenMap().put(ENTITY_NAME_TN, serverName);
        getTokenMap().put(LOCATION_ARGS_START_TN, getLocationArgsStart());
        getTokenMap().put(LOCATION_ARGS_RESTART_TN, getLocationArgsRestart());
        getTokenMap().put(LOCATION_ARGS_STOP_TN, getLocationArgsStop());
        getTokenMap().put(START_COMMAND_TN, info.type.startCommand());
        getTokenMap().put(RESTART_COMMAND_TN, info.type.restartCommand());
        getTokenMap().put(STOP_COMMAND_TN, info.type.stopCommand());
        getTokenMap().put(FQSN_TN, info.fqsn);
        getTokenMap().put(OS_USER_TN, info.osUser);

        if (OS.isWindowsForSure() && !LINUX_HACK) {
            // Windows doesn't respond well to slashes in the name!!
            getTokenMap().put(SERVICE_NAME_TN, info.serviceName);
            getTokenMap().put(ENTITY_NAME_TN, serverName);
        }
        else
            getTokenMap().put(SERVICE_NAME_TN, info.smfFullServiceName);

        getTokenMap().put(AS_ADMIN_PATH_TN, info.asadminScript.getPath().replace('\\', '/'));
        getTokenMap().put(DATE_CREATED_TN, info.date.toString());
        getTokenMap().put(SERVICE_TYPE_TN, info.type.toString());
        getTokenMap().put(CREDENTIALS_TN, getCredentials());

    }

    final String getCredentials() {
        // 1 -- no auth of any kind needed -- by definition when there is no
        // password file
        // note: you do NOT want to give a "--user" arg -- it can only appear
        // if there is a password file too
        if (info.passwordFile == null)
            return " ";

        // 2. --
        String user = info.appserverUser; // might be null

        StringBuilder sb = new StringBuilder();

        if (StringUtils.ok(user))
            sb.append(" --user ").append(user);

        sb.append(" --passwordfile ").append(info.passwordFile.getPath()).append(" ");

        return sb.toString();
    }

    void trace(String s) {
        if (info.trace)
            System.out.println(TRACE_PREPEND + s);
    }

    void dryRun(String s) {
        if (info.dryRun)
            System.out.println(DRYRUN_PREPEND + s);
    }

    final Map<String, String> getTokenMap() {
        return tokenMap;
    }

    /**
     * If the user has specified a password file than get the info
     * and convert into a String[] that CLI can use.
     * e.g. { "--user", "harry", "--passwordfile", "/xyz" }
     * authentication artifacts. Parameter may not be null.
     */
    private void setAsadminCredentials() {

        // it is allowed to have no passwordfile specified in V3
        if (info.passwordFile == null)
            return;

        // But if they DID specify it -- it must be kosher...

        if (!info.passwordFile.isFile())
            throw new IllegalArgumentException(Strings.get("windows.services.passwordFileNotA", info.passwordFile));

        if (!info.passwordFile.canRead())
            throw new IllegalArgumentException(Strings.get("windows.services.passwordFileNotReadable", info.passwordFile));

        Properties p = getProperties(info.passwordFile);

        // IT 10255
        // the password file may just have master password or just user or just user password
        //

        String userFromPasswordFile = p.getProperty("AS_ADMIN_USER");

        // Byron Nevins sez:
        // unfiled bug -- this was the ONLY check for username.  I changed it
        // in November 2012 -- now the user has been already set to whatever CLICommand's
        // ProgramOptions.getUser() returned.
        // the username in the passwordfile takes precedence if it is in there.
        // In summary - before this change if --user was specified then that username was
        // completely ignored.  Now it is used.
        //
        if(StringUtils.ok(userFromPasswordFile))
            info.setAppServerUser(p.getProperty("AS_ADMIN_USER"));
    }

    private Properties getProperties(File f) {
        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(f));
            final Properties p = new Properties();
            p.load(bis);
            return p;
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (Exception ee) {
                    // ignore
                }
            }
        }
    }
    private final Map<String, String> tokenMap = new HashMap<String, String>();
    final PlatformServicesInfo info;
}
