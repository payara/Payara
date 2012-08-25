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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.process.JavaClassRunner;
import com.sun.enterprise.universal.process.ProcessUtils;
import com.sun.enterprise.util.StringUtils;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.internal.api.Globals;
import org.glassfish.embeddable.GlassFish;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * For non-verbose mode:
 * Stop this server, spawn a new JVM that will wait for this JVM to die.  The new JVM then starts the server again.
 *
 * For verbose mode:
 * We want the asadmin console itself to do the respawning -- so just return a special int from
 * System.exit().  This tells asadmin to restart.
 *
 * @author Byron Nevins
 */
public class RestartServer {
    @Inject
    private Provider<GlassFish> glassfishProvider;
    
    protected final void setDebug(Boolean b) {
        debug = b;
    }

    protected final void setRegistry(final ModulesRegistry registryIn) {
        registry = registryIn;
    }

    protected final void setServerName(String serverNameIn) {
        serverName = serverNameIn;
    }

    /**
     * Restart of the application server :
     *
     * All running services are stopped.
     * LookupManager is flushed.
     *
     * Client code that started us should notice the special return value and restart us.
     */
    protected final void doExecute(AdminCommandContext context) {
        try {
            // unfortunately we can't rely on constructors with HK2...
            if (registry == null)
                throw new NullPointerException(new LocalStringsImpl(getClass()).get("restart.server.internalError", "registry was not set"));

            init(context);
            
            // get the GlassFish object - we have to wait in case startup is still in progress
            // This is a temporary work-around until HK2 supports waiting for the service to 
            // show up in the ServiceLocator. 
            GlassFish gfKernel = glassfishProvider.get();
            while (gfKernel == null) {
                Thread.sleep(1000);
                gfKernel = glassfishProvider.get();
            }
            
            if (!verbose) {
                // do it now while we still have the Logging service running...
                reincarnate();
            }
            // else we just return a special int from System.exit()
            gfKernel.stop();
        }
        catch (Exception e) {
            context.getLogger().severe(strings.get("restart.server.failure", e));
        }

        int ret = RESTART_NORMAL;

        if (debug != null)
            ret = debug ? RESTART_DEBUG_ON : RESTART_DEBUG_OFF;

        System.exit(ret);
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /////////               ALL PRIVATE BELOW               ////////////////////
    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private void init(AdminCommandContext context) throws IOException {
        logger = context.getLogger();
        props = Globals.get(StartupContext.class).getArguments();
        verbose = Boolean.parseBoolean(props.getProperty("-verbose", "false"));
        logger.info(strings.get("restart.server.init"));
    }

    private void reincarnate() {
        try {
            if (setupReincarnationWithAsadmin() || setupReincarnationWithOther())
                doReincarnation();
            else
                logger.severe(strings.get("restart.server.noStartupInfo",
                        strings.get("restart.server.asadminError"),
                        strings.get("restart.server.nonAsadminError")));
        }
        catch (RDCException rdce) {
            // already logged...
        }
        catch (Exception e) {
            logger.severe(strings.get("restart.server.internalError", e));
        }

    }

    private void doReincarnation() throws RDCException {
        try {
            // TODO JavaClassRunner is very simple and primitive.
            // Feel free to beef it up...

            String[] props = normalProps;

            if (Boolean.parseBoolean(System.getenv("AS_SUPER_DEBUG")))
                props = debuggerProps;  // very very difficult to debug this stuff otherwise!

            new JavaClassRunner(classpath, props, classname, args);
        }
        catch (Exception e) {
            logger.severe(strings.get("restart.server.jvmError", e));
            throw new RDCException();
        }
    }

    private boolean setupReincarnationWithAsadmin() throws RDCException {
        classpath = props.getProperty("-asadmin-classpath");
        classname = props.getProperty("-asadmin-classname");
        argsString = props.getProperty("-asadmin-args");

        return verify("restart.server.asadminError");
    }

    private boolean setupReincarnationWithOther() throws RDCException {

        classpath = props.getProperty("-startup-classpath");
        classname = props.getProperty("-startup-classname");
        argsString = props.getProperty("-startup-args");

        return verify("restart.server.nonAsadminError");
    }

    private boolean verify(String errorStringKey) throws RDCException {
        // Either asadmin or non-asadmin startup params have been set -- check them!
        // THREE possible returns:
        // 1) true
        // 2) false
        // 3) RDCException
        if (classpath == null && classname == null && argsString == null) {
            return false;
        }

        // now that at least one is set -- demand that ALL OF THEM be set...
        if (!ok(classpath) || !ok(classname) || argsString == null) {
            logger.severe(strings.get(errorStringKey));
            throw new RDCException();
        }

        args = argsString.split(",,,");
        handleDebug();
        return true;
    }

    private void handleDebug() {
        if (debug == null) // nothing to do!
            return;

        stripDebugFromArgs();
        stripOperandFromArgs();
        int oldlen = args.length;
        int newlen = oldlen + 2;
        String debugArg = "--debug=" + debug.toString();
        String[] newArgs = new String[newlen];

        // copy all but the last arg (domain-name)
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[newlen - 2] = debugArg;
        newArgs[newlen - 1] = serverName;
        args = newArgs;
    }

    private void stripDebugFromArgs() {
        // this is surprisingly complex!
        // "--debug domain1" is one
        // "--debug=true" is one
        // "--debug false" is two
        boolean twoArgs = false;
        int indexOfDebug = -1;

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--debug=")) {
                indexOfDebug = i;
                break;
            }
            if (args[i].startsWith("--debug")) {
                indexOfDebug = i;

                // who knows what happens in CLI when the domain's name is "true" ?!?
                // we could potentially be fooled by that one very unlikely scenario
                if (args.length > i + 1) {// broken into two if's for readability...
                    if (args[i + 1].equals("true") || args[i + 1].equals("false")) {
                        twoArgs = true;
                    }
                }
                break;
            }
        }

        if (indexOfDebug < 0)
            return;

        int oldlen = args.length;
        int newlen = oldlen - 1;

        if (twoArgs)
            --newlen;

        String[] newArgs = new String[newlen];
        int ctr = 0;

        for (int i = 0; i < oldlen; i++) {
            if (i == indexOfDebug)
                continue;
            if (twoArgs && i == (indexOfDebug + 1))
                continue;

            newArgs[ctr++] = args[i];
        }

        args = newArgs;
    }

    private void stripOperandFromArgs() {
        // remove the domain-name operand
        // it may not be here!
        if (args.length < 2 || !StringUtils.ok(serverName))
            return;

        int newlen = args.length - 1;

        if (serverName.equals(args[newlen])) {
            String[] newargs = new String[newlen];
            System.arraycopy(args, 0, newargs, 0, newlen);
            args = newargs;
        }
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0;
    }

    // We use this simply to tell the difference between fatal errors and other
    // non-fatal conditions.
    private static class RDCException extends Exception {
    }
    ModulesRegistry registry;
    private Boolean debug = null;
    private Properties props;
    private Logger logger;
    private boolean verbose;
    private String classpath;
    private String classname;
    private String argsString;
    private String[] args;
    private String serverName = "";
    private static final LocalStringsImpl strings = new LocalStringsImpl(RestartServer.class);
    /////////////             static variables               ///////////////////
    private static final String magicProperty = "-DAS_RESTART=" + ProcessUtils.getPid();
    private static final String[] normalProps = {magicProperty};
    private static final int RESTART_NORMAL = 10;
    private static final int RESTART_DEBUG_ON = 11;
    private static final int RESTART_DEBUG_OFF = 12;
    private static final String[] debuggerProps = {
        magicProperty,
        "-Xdebug",
        "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1323"};
}
