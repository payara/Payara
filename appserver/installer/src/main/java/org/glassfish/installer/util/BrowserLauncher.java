/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.installer.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openinstaller.util.ClassUtils;
import org.openinstaller.util.EnhancedException;
import org.openinstaller.util.ExecuteCommand;

/**
 * BrowserLauncher is a utility class that can be used to open any file or URL. It makes use of the
 * default application associated with the file type and opens the file using that application.
 * @author Sathyan Catari(This class is a copied/customized version of OpenInstaller's BrowserLauncher utility).
 * @version %Revision%
 */
public class BrowserLauncher {

    /**
     * The enum used to hold the various desktop environments.
     */
    private enum DesktopType {

        /** The GNOME Desktop Environment. */
        GNOME,
        /** The KDE Desktop Environment. */
        KDE,
        /** The Common Desktop Environment. */
        CDE,
    };

    /**
     * Creates a new instance of BrowserLauncher.
     */
    public BrowserLauncher() {
        // Empty default constructor.
    }

    /* LOGGING */
    private static final Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(ClassUtils.getClassName());
    }

    /**
     * This method is used to open a URL using the detected web browser.
     * @param aURL The URL to open
     * @throws EnhancedException thrown when unable to open the URL
     */
    public void openURL(final URL aURL) throws EnhancedException {
        final String theURL = aURL.toString();
        String[] theCommand;

        Platform curPlatform = new Platform();
        switch (curPlatform.getOSType()) {
            case UNIX:
                // Use sdtwebclient if OS is Solaris
                if (curPlatform.getOSNameForString(System.getProperty("os.name"))
                        == Platform.OSName.SOLARIS) {
                    theCommand = new String[]{"/usr/dt/bin/sdtwebclient",
                                theURL,};
                    /* don't wait for sdtwebclient, since it sleeps for 60 seconds */
                    if (executeCommand(theCommand, false) == 0) {
                        return;
                    } else {
                        LOGGER.log(Level.FINE, "sdtwebclient could not find any web browser in PATH");
                    }
                }
                // Detect the current Desktop Environment
                final DesktopType theDesktopEnvironment = detectDesktopEnvironment();

                switch (theDesktopEnvironment) {
                    case GNOME:
                        theCommand = new String[]{"gnome-open",
                                    theURL,};
                        if (executeCommand(theCommand, true) != 0) {
                            throw new EnhancedException("BROWSER_NOT_SUPPORTED");
                        }
                        break;
                    case KDE:
                        theCommand = new String[]{"kfmclient exec",
                                    theURL,};
                        if (executeCommand(theCommand, true) != 0) {
                            throw new EnhancedException("BROWSER_NOT_SUPPORTED");
                        }
                        break;
                    case CDE:
                        try {
                            final File theTmp = File.createTempFile("browsercommand", null);
                            final FileWriter theFW = new FileWriter(theTmp);
                            theFW.write(theURL + "\n");
                            theFW.close();
                            theCommand = new String[]{"/usr/dt/bin/dtaction",
                                        "Browse",
                                        theTmp.getAbsolutePath(),};
                            if (executeCommand(theCommand, true) != 0) {
                                throw new EnhancedException("BROWSER_NOT_SUPPORTED");
                            }
                        } catch (final IOException theEx) {
                            LOGGER.log(Level.FINE, "Error writing to temporary file.");
                            throw new EnhancedException("BROWSER_NOT_SUPPORTED", theEx);
                        }
                        break;
                }
                break;
            case WINDOWS:
                theCommand = new String[]{"cmd.exe",
                            "/c",
                            "start",
                            "\"\"",
                            theURL,};
                if (executeCommand(theCommand, true) != 0) {
                    theCommand = new String[]{"command.exe",
                                "/c",
                                "start",
                                "\"\"",
                                theURL,};
                    if (executeCommand(theCommand, true) != 0) {
                        throw new EnhancedException("BROWSER_NOT_SUPPORTED");
                    }
                }
                break;
            case MAC:
                theCommand = new String[]{"open",
                            theURL,};
                if (executeCommand(theCommand, true) != 0) {
                    throw new EnhancedException("BROWSER_NOT_SUPPORTED");
                }
                break;
            default:
                throw new EnhancedException("UNSUPPORTED_PLATFORM",
                        "platform=" + System.getProperty("os.name"));
        }
    }

    /**
     * Used to detect the current Desktop Environment.
     *
     * @return the String corresponding to the detected Desktop Environment
     *
     * @throws EnhancedException thrown when the detected Desktop Environment is none of the three -
     *                           GNOME, KDE or CDE
     */
    private DesktopType detectDesktopEnvironment() throws EnhancedException {
        if ("Default".equalsIgnoreCase(System.getenv("GNOME_DESKTOP_SESSION_ID"))) {
            /**
             * GNOME is running if the value of the GNOME_DESKTOP_SESSION_ID
             * environment variable is 'Default'
             */
            return DesktopType.GNOME;
        } else if (System.getenv("DESKTOP_SESSION").contains("gnome")) {
            /**
            * GNOME is running if the value of the DESKTOP_SESSION
            * environment variable contains 'gnome'
            */
            return DesktopType.GNOME;
        } else if ("true".equalsIgnoreCase(System.getenv("KDE_FULL_SESSION"))) {
            /**
             * KDE is running if the value of the KDE_FULL_SESSION environment
             * variable is 'true'
             */
            return DesktopType.KDE;
        } else if (System.getenv("DTUSERSESSION") != null && System.getenv("DTUSERSESSION").length() != 0) {
            /**
             * CDE is running if the value of the DTUSERSESSION environment
             * variable is set.
             * DTUSERSESSION is also being set in GNOME, but since GNOME is being
             * checked for earlier in this if-else ladder, this condition would be
             * satisfied only when the running instance is CDE.
             */
            return DesktopType.CDE;
        }
        /* cannot detect the type of environment we're in */
        throw new EnhancedException("BROWSER_NOT_SUPPORTED");
    }

    /**
     * This method executes a given command and returns its result.
     *
     * @param aCommand the command to be executed, as an array of spaceless Strings.
     * @param aWaitFor whether to wait for this command.  If this argument is false, then
     * this method will invoke the command but not wait for it to complete, and will always
     * return 0 (indicating success).
     * @return the result of executing the command, or -1, when command execution throws an
     *         EnhancedException.
     */
    private int executeCommand(final String[] aCommand, final boolean aWaitFor) {
        try {
            final ExecuteCommand theExecCmd;
            theExecCmd = new ExecuteCommand(aCommand);

            theExecCmd.setWaitFor(aWaitFor);
            theExecCmd.execute();
            return theExecCmd.getResult();
        } catch (Exception theEx) {
            LOGGER.log(Level.FINE, "Error executing command. Command=" + Arrays.toString(aCommand));
            return -1;
        }
    }
}
