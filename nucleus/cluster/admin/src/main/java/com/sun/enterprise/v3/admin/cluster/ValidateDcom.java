/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.util.cluster.windows.process.WindowsException;
import com.sun.enterprise.universal.glassfish.TokenResolver;
import com.sun.enterprise.util.cluster.windows.process.WindowsCredentials;
import com.sun.enterprise.util.cluster.windows.process.WindowsRemoteScripter;
import com.sun.enterprise.util.cluster.windows.process.WindowsWmi;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFileSystem;
import java.io.*;
import java.io.IOException;
import java.net.*;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.RestEndpoints;
import static com.sun.enterprise.util.StringUtils.ok;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFile;
import com.sun.enterprise.util.net.NetUtils;
import java.net.InetAddress;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.cluster.ssh.util.DcomUtils;

/**
 * This command tests
 * to see if a remote Windows machine can be contacted from the server via DCOM.
 * @author Byron Nevins
 */
@Service(name = "validate-dcom")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Nodes.class,
    opType = RestEndpoint.OpType.GET,
    path = "validate-dcom",
    description = "Validate DCOM")
})
public class ValidateDcom implements AdminCommand {
    @Param(name = "windowsuser", shortName = "w", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "windowspassword", optional = true, password = true)
    private String password;
    @Param(name = "host", optional = false, primary = true)
    private String host;
    @Param(name = "windowsdomain", shortName = "d", optional = true)
    private String windowsdomain;
    @Param(name = "remotetestdir", optional = true, defaultValue = "C:\\")
    private String testdir;
    @Param(optional = true, shortName = "v", defaultValue = "false")
    private boolean verbose;
    private boolean debug;
    private final TokenResolver resolver = new TokenResolver();
    private ActionReport report;
    private WindowsRemoteFileSystem wrfs;
    private WindowsCredentials creds;
    private final StringBuilder out = new StringBuilder();
    private WindowsRemoteFile wrf;
    private WindowsRemoteFile script;
    private static final String SCRIPT_NAME = "delete_me.bat";
    private String scriptFullPath;

    @Override
    public final void execute(AdminCommandContext context) {
        debug = Boolean.parseBoolean(System.getenv("AS_DEBUG")) && verbose;
        try {
            // try/finally is least messy way of making sure partial success news
            // is delivered back to caller

            if (!init(context))
                return;

            if (!testNotLocal())
                return;

            if (!testDcomPort())
                return;

            if (!testDcomFileAccess())
                return;

            if (!testDcomFileWrite())
                return;

            if (!testWMI())
                return;

            if (!testRemoteScript())
                return;

            if (!testJdkAvailable())
                return;
        }
        finally {
            if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS || verbose)
                report.setMessage(out.toString());
        }
    }

    private boolean init(AdminCommandContext context) {
        report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        user = resolver.resolve(user);
        password = DcomUtils.resolvePassword(resolver.resolve(password));

        if (!ok(password)) {
            setError(Strings.get("dcom.nopassword"));
            return false;
        }

        // backslash does not actually matter but it's neater
        testdir = resolver.resolve(testdir).replace('/', '\\');
        if (testdir.endsWith("\\"))
            testdir = testdir.substring(0, testdir.length() - 1);

        if (!ok(windowsdomain))
            windowsdomain = host;

        creds = new WindowsCredentials(host, windowsdomain, user, password);
        try {
            wrfs = new WindowsRemoteFileSystem(creds);
        }
        catch (WindowsException ex) {
            // probably the j-interop-repackagted.jar is missing
            setError(ex.getMessage());
            return false;
        }
        scriptFullPath = testdir + "\\" + SCRIPT_NAME;
        return true;
    }

    /**
     * Make sure port 135 (the DCOM port) is alive on the remote host
     * Fast preliminary test (4 seconds worst-case)
     */
    private boolean testDcomPort() {
        try {
            // only interested in Exception side-effect...
            InetAddress ia = InetAddress.getByName(host);
            out.append(Strings.get("validate.dcom.getbyname", ia)).append('\n');
        }
        catch (UnknownHostException e) {
            setError(e, Strings.get("unknown.host", host));
            return false;
        }

        boolean b135 = testPort(135, "DCOM Port");
        boolean b139 = testPort(139, "NetBIOS Session Service");
        boolean b445 = testPort(445, "Windows Shares");

        return b135 && b139 && b445;
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
            setError(ex, Strings.get("dcom.no.remote.file.access", testdir, host));
            return false;
        }
        out.append(Strings.get("dcom.access.ok", testdir, host)).append('\n');
        return true;
    }

    private boolean testDcomFileWrite() {
        try {
            script = new WindowsRemoteFile(wrf, SCRIPT_NAME);
            script.copyFrom("dir " + testdir + "\\\n");
            out.append(Strings.get("dcom.write.ok", SCRIPT_NAME, testdir, host)).append('\n');
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.write", SCRIPT_NAME, testdir, host));
            return false;
        }
        return true;
    }

    private boolean testWMI() {
        int count = -1;
        try {
            WindowsWmi ww = new WindowsWmi(creds);
            count = ww.getCount();
            if (debug) {
                String[] info = ww.getInfo();
                out.append(Strings.get("dcom.wmi.procinfolegend"));
                for (String s : info) {
                    // e.g. '\tCommandLine = "xxxxx"'
                    String[] lines = s.split("[\t\n\r]");
                    for (String line : lines) {
                        if (line.startsWith("CommandLine")) {
                            out.append("    ").append(line).append('\n');
                            break;
                        }
                    }
                }
            }
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.wmi", host));
            return false;
        }
        out.append(Strings.get("dcom.wmi.ok", host, count)).append('\n');
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
        if (debug) {
            Throwable t = e;
            do {
                dumpStack(t);
            }
            while ((t = t.getCause()) != null);
        }
    }

    private void setError(String msg) {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        out.append(msg).append('\n');
    }

    private String crunch(int numlines, String big) {
        if (!ok(big))
            return big;
        StringBuilder sb = new StringBuilder();
        String[] ss = big.split("\n");

        // numlines or fewer lines
        for (int i = 0; i < numlines && i < ss.length; i++) {
            sb.append("    ").append(ss[i]).append('\n');
        }

        return sb.toString();
    }

    private void dumpStack(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        t.printStackTrace(pw);
        pw.close();
        out.append(sw.toString());
    }

    private boolean testPort(int port, String description) {
        try {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 4000);
            }
            out.append(Strings.get("validate.dcom.connect", description, port, host)).append('\n');
            return true;
        }
        catch (IOException e) {
            setError(e, Strings.get("validate.dcom.no.connect", description, port, host));
            return false;
        }
    }

    private boolean testJdkAvailable() {
        try {
            script = new WindowsRemoteFile(wrf, SCRIPT_NAME);
            script.copyFrom("javac -version \r\n");
            WindowsRemoteScripter scripter = new WindowsRemoteScripter(creds);

            // javac and jar write to stderr NOT stdout
            scripter.wantStdErr();

            String scriptOut = scripter.run(scriptFullPath);
            script.delete();
            out.append(Strings.get("dcom.yes.jdk", host, scriptOut));
            out.append('\n');
            return true;
        }
        catch (WindowsException ex) {
            setError(ex, Strings.get("dcom.no.jdk", host));
            return false;
        }
    }

    private boolean testNotLocal() {
        if (NetUtils.isThisHostLocal(host)) {
            setError(Strings.get("dcom.yes.local", host));
            return false;
        }
        out.append(Strings.get("dcom.no.local", host));
        out.append('\n');
        return true;
    }
}
