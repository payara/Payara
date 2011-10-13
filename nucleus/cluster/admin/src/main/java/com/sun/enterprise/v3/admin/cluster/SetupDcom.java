/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
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
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.universal.process.WindowsException;
import com.sun.enterprise.universal.process.WindowsRemotePinger;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.universal.process.WindowsCredentials;
import com.sun.enterprise.universal.process.WindowsRemoteScripter;
import com.sun.enterprise.util.io.WindowsRemoteFileSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.admin.CommandValidationException;
import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.util.io.WindowsRemoteFile;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.cluster.ssh.util.DcomUtils;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * The name of this class matches "setup-ssh".  It does not setup but rather tests
 * to see if a remote Windows machine can be contacted from the server via DCOM.
 * @author Byron Nevins
 */
@Service(name = "setup-dcom")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
public class SetupDcom implements AdminCommand {
    @Param(name = "dcomuser", optional = true, defaultValue = NodeUtils.NODE_DEFAULT_REMOTE_USER)
    private String user;
    @Param(name = "dcompassword", optional = false, password = true)
    private String password;
    @Param(name = "host", optional = false, primary = true)
    private String host;
    @Param(name = "windowsdomain", optional = true)
    private String windowsdomain;
    @Param(name = "remotetestdir", optional = true, defaultValue = "C:\\")
    private String testdir;
    private TokenResolver resolver = new TokenResolver();
    private ActionReport report;
    private Logger logger;
    private WindowsRemoteFileSystem wrfs;
    private WindowsCredentials creds;
    private StringBuilder out = new StringBuilder();
    private WindowsRemoteFile wrf;
    private WindowsRemoteFile script;
    private static final String SCRIPT_NAME = "delete_me.bat";
    private String scriptFullPath;

    @Override
    public final void execute(AdminCommandContext context) {
        init(context);

        if (!testDcomFileAccess())
            return;

        if (!testDcomFileWrite())
            return;

        if (!testRemoteScript())
            return;

        report.setMessage(out.toString());
    }

    private void init(AdminCommandContext context) {
        report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        logger = context.getLogger();
        user = resolver.resolve(user);
        password = DcomUtils.resolvePassword(resolver.resolve(password));
        // backslash does not actually matter but it's neater
        testdir = resolver.resolve(testdir).replace('/', '\\');
        if (testdir.endsWith("\\"))
            testdir = testdir.substring(0, testdir.length() - 1);

        if (!ok(windowsdomain))
            windowsdomain = host;

        creds = new WindowsCredentials(host, windowsdomain, user, password);
        wrfs = new WindowsRemoteFileSystem(creds);
        scriptFullPath = testdir + "\\" + SCRIPT_NAME;
    }

    /**
     * return true if A-OK.  Otherwise set up the reporter and return false
     * @throws CommandValidationException
     */
    private boolean testDcomFileAccess() {
        try {
            wrf = new WindowsRemoteFile(wrfs, testdir);
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.remote.access", testdir, host));
            return false;
        }
        try {
            // also looking for side-effect of Exception getting thrown...
            if (!wrf.exists()) {
                setError(Strings.get("dcom.no.remote.file", testdir, host));
                return false;
            }
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.remote.file", testdir, host));
            return false;
        }
        out.append(Strings.get("dcom.access.ok", testdir, host)).append('\n');
        return true;
    }

    private boolean testDcomFileWrite() {
        try {
            script = new WindowsRemoteFile(wrf, SCRIPT_NAME);
            script.copyFrom("dir " + testdir + "\\\n");
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.write", SCRIPT_NAME, testdir, host));
            return false;
        }
        out.append(Strings.get("dcom.write.ok", SCRIPT_NAME, testdir, host)).append('\n');
        return true;
    }

    private boolean testRemoteScript() {
        String scriptOut = null;
        try {
            WindowsRemoteScripter scripter = new WindowsRemoteScripter(creds);
            scriptOut = scripter.run(scriptFullPath);
            script.delete();
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.run", host));
            return false;
        }
        out.append(Strings.get("dcom.run.ok", host, crunch(12, scriptOut))).append('\n');
        return true;
    }

    // TODO report a bug with JIRA  setFailureCause causes explosions!
    private void setError(Exception e, String msg) {
        //report.setFailureCause(e);
        setError(msg + " : " + e.getMessage());
    }

    private void setError(String msg) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        report.setMessage(msg);
    }

    private String crunch(int numlines, String big) {
        if(!ok(big))
            return big;
        StringBuilder sb = new StringBuilder();
        String[] ss = big.split("\n");

        // numlines or fewer lines
        for(int i = 0; i < numlines && i < ss.length; i++) {
            sb.append(ss[i]).append('\n');
        }

        return sb.toString();
    }
}
