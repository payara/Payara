/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.universal.process.ProcessManager;
import com.sun.enterprise.universal.process.ProcessManagerException;
import java.io.*;
import java.io.File;
import java.util.*;
import org.glassfish.api.Param;
import static com.sun.enterprise.universal.process.ProcessUtils.getExe;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.glassfish.api.admin.*;
import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.util.OS;

/*
 * @author Byron Nevins
 */
// I give up!  I spent 2 hours trying to get at the properties file.  No way!
// classloader has no clue where it is!!!
// It isn't worth the effort - so I embedded the strings in here.
// http://java.net/jira/browse/GLASSFISH-18338
@Service(name = "setup-local-dcom")
@Scoped(PerLookup.class)
public final class SetupLocalDcom extends CLICommand {
    @Param(name = "verbose", shortName = "v", primary = false, optional = true)
    boolean verbose;
    @Param(name = "force", shortName = "f", primary = false, optional = true)
    boolean force;
    private final static String[] DEPENDENCIES = new String[]{
        "advapi32.dll",
        "kernel32.dll", //"test32.dll",
    };
    private static final String CPP_APP_FILENAME = "DcomConfigurator.exe";
    private static final File TMPDIR = new File(System.getProperty("java.io.tmpdir"));
    private static final File CPP_APP = new File(TMPDIR, CPP_APP_FILENAME);
    private final Console console = System.console();

    @Override
    protected void validate() throws CommandException {
        super.validate();

        if (!OS.isWindowsForSure())
            throw new CommandException("This command is exclusively for Windows computers.");

        if (console == null)
            throw new CommandException("This command can only be run from a "
                    + "console.  Please try again with a console attached.");

        if (!force)
            areYouSure();

        checkPath();
        prepareCppApp();
    }

    @Override
    protected int executeCommand() throws CommandException, CommandValidationException {
        try {
            List<String> cmds = new ArrayList<String>();
            cmds.add(CPP_APP.getAbsolutePath());

            if (verbose)
                cmds.add("--verbose");

            ProcessManager pm = new ProcessManager(cmds);
            pm.execute();

            int ret = pm.getExitValue();

            if (verbose || ret != 0)
                logger.info(pm.getStdout() + pm.getStderr());

            return ret;
        }
        catch (ProcessManagerException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * make sure that the right DLLs for running the C++ program are in the Path
     * @throws CommandException if the right DLLs are not in the path.
     */
    private void checkPath() throws CommandException {
        List<String> notFound = new ArrayList<String>();

        for (String dll : DEPENDENCIES) {
            File f = getExe(dll);

            if (f == null)
                notFound.add(dll);
            else
                logger.finer("Required DLL Located: " + f);
        }

        if (!notFound.isEmpty()) {
            throw prepareMissingDllMessage(notFound);
        }
    }

    private static CommandException prepareMissingDllMessage(List<String> notFound) {
        StringBuilder sb = new StringBuilder();

        for (String dll : notFound) {
            sb.append('\t').append(dll).append('\n');
        }
        String msg = "This command runs a native Windows program that requires "
                + "other files to run.  \nHere are the missing file(s):\n"
                + sb.toString();

        return new CommandException(msg);
    }

    private void prepareCppApp() throws CommandException {
        if (!TMPDIR.isDirectory())
            throw new CommandException("Internal Error: The Java-provided temp "
                    + "dir (java.io.tmpdir), " + TMPDIR
                    + ", is not a directory.");

        if (CPP_APP.exists()) {
            CPP_APP.delete();

            if (CPP_APP.exists())
                throw new CommandException("The DCOM tool already exists ("
                        + CPP_APP + ") and can't be deleted.\n"
                        + "Please delete it manually and re-run this command.");

            logger.finer("This is unusual.  The app, " + CPP_APP + ", already existed.  "
                    + "It was deleted with no problem.");
        }

        CPP_APP.deleteOnExit();

        // copy it from inside this jar to the file system
        InputStream in = getClass().getResourceAsStream("/lib/" + CPP_APP_FILENAME);
        FileOutputStream out;

        try {
            out = new FileOutputStream(CPP_APP);
            copyStream(in, out);
        }
        catch (IOException ex) {
            throw new CommandException("Error while attempting to extract DCOM Configuration tool "
                    + CPP_APP + "\n" + ex);
        }

        if (!CPP_APP.canExecute())
            throw new CommandException("Error while attempting to extract DCOM "
                    + "Configuration tool " + CPP_APP);
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null)
            throw new NullPointerException("internal error: null arguments");

        byte[] buf = new byte[16384];
        int len;

        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }

        in.close();
        out.close();
    }
    /*
     * note how this method will likely be inlined by the compiler since it is tiny
     * and private...
     */

    private void areYouSure() throws CommandException {
        if (!programOpts.isInteractive())
            throw new CommandException(
                    "This command can only run in interactive mode.  \n"
                    + "Please make sure you have the interactive flag set to true.  "
                    + "See 'asadmin --help' for details\n"
                    + "The other option is to use the --force option");

        String msg =
                "Caution: This command might modify the permissions of some keys in the Windows registry.\n"
                + "Before running this command, back up the Windows registry.\n"
                + "The modification allows the Windows user full control over these keys.\n"
                + "\nAre you sure that you want to edit the Windows registry? If so, type yes in full";

        String answer = console.readLine("%s:  ", msg);

        if (!"yes".equalsIgnoreCase(answer))
            throw new CommandException("You chose to not run the command.");
    }
}
