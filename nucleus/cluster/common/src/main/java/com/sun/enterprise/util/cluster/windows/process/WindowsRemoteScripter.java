/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.cluster.windows.process;

import com.sun.enterprise.util.cluster.windows.SharedStrings;
import java.util.Collection;
import java.util.logging.Level;
import org.jinterop.dcom.common.JISystem;
import org.jinterop.dcom.core.*;
import org.jinterop.dcom.impls.JIObjectFactory;
import org.jinterop.dcom.impls.automation.IJIDispatch;

/**
 * Call a script on a remote Windows system
 * @author bnevins
 */
public class WindowsRemoteScripter {
    private final WindowsCredentials bonafides;
    private String outputStream = "StdOut";

    public WindowsRemoteScripter(WindowsCredentials bonafides) {
        this.bonafides = bonafides;
    }

    /*
     * You get your choice of stderr OR stdout.  Getting both is hairy and too
     * difficult.
     * The default is stdout
     */
    public final void wantStdErr() {
        outputStream = "StdErr";
    }

    /**
     * Run a remote script command.
     * Convenience method which creates one big String from the substrings
     * @param cmd e.g. "C:/glassfish4/bin/asadmin" "start-local-instance" "i1"
     * @return The stdout of the command
     */
    public String run(Collection<String> cmdArgs) throws WindowsException {
        if (cmdArgs == null || cmdArgs.isEmpty())
            throw new IllegalArgumentException("Internal Error: No args to run");

        StringBuilder sb = new StringBuilder();

        for (String s: cmdArgs) {
            sb.append(s).append(' ');
        }

        return run(sb.toString());
    }

    /**
     * Run a remote script command
     * @param cmd e.g. "C:/glassfish4/bin/asadmin start-local-instance i1"
     * @return The stdout of the command
     */
    public String run(String cmd) throws WindowsException {
         if (cmd == null || cmd.isEmpty())
            throw new IllegalArgumentException("Internal Error: No args to run");

        try {
            // JISystem is **extremely** verbose!
            JISystem.getLogger().setLevel(Level.SEVERE);

            // Create a session
            JISession session = JISession.createSession(bonafides.getDomain(),
                    bonafides.getUser(), bonafides.getPassword());
            session.useSessionSecurity(true);

            // Execute command
            JIComServer comStub = new JIComServer(JIProgId.valueOf("WScript.Shell"),
                    bonafides.getHost(), session);
            IJIComObject unknown = comStub.createInstance();
            IJIComObject comobject = unknown.queryInterface(IJIDispatch.IID);
            IJIDispatch shell = (IJIDispatch) JIObjectFactory.narrowObject(comobject);

            Object[] scriptArgs = new Object[]{
                new JIString("%comspec% /c " + cmd)
            };

            // ref: http://stackoverflow.com/questions/6781340/how-to-call-a-remote-bat-file-using-jinterop

            JIVariant results[] = shell.callMethodA("Exec", scriptArgs);
            final IJIDispatch wbemObjectSet_dispatch =
                    (IJIDispatch) JIObjectFactory.narrowObject((results[0]).getObjectAsComObject());

            JIVariant stdOutJIVariant = wbemObjectSet_dispatch.get(outputStream);

            IJIDispatch stdOut =
                    (IJIDispatch) JIObjectFactory.narrowObject(stdOutJIVariant.getObjectAsComObject());

            // Read all from stdOut
            StringBuilder sb = new StringBuilder();

            while (!((JIVariant) stdOut.get("AtEndOfStream")).getObjectAsBoolean()) {
                sb.append(stdOut.callMethodA("ReadAll").getObjectAsString().getString());
            }

            return sb.toString();
        }
        catch (NoClassDefFoundError err) {
            throw new WindowsException(SharedStrings.get("missing_jinterop"));
        }
        catch (Exception e) {
            // do NOT allow j-interop exceptions to leak
            // out of the module.
            throw new WindowsException(e.toString());
        }
    }
}
