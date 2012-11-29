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

package com.sun.enterprise.v3.admin;

import java.io.*;
import java.security.SecureRandom;
import java.util.logging.*;
import javax.inject.Inject;
import org.glassfish.api.admin.ServerEnvironment;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.*;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 * Manage a local password, which is a cryptographically secure random number
 * stored in a file with permissions that only allow the owner to read it.
 * A new local password is generated each time the server starts.  The
 * asadmin client can use it to authenticate when executing local commands,
 * such as stop-domain, without the user needing to supply a password.
 *
 * @author Bill Shannon
 */
@Service
@RunLevel(InitRunLevel.VAL)
public class LocalPasswordImpl implements PostConstruct, LocalPassword {

    @Inject
    ServerEnvironment env;

    private String password;

    private static final String LOCAL_PASSWORD_FILE = "local-password";
    private static final int PASSWORD_BYTES = 20;
    private static final char[] hex = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static final Logger logger = KernelLoggerInfo.getLogger();

    /**
     * Generate a local password and save it in the local-password file.
     */
    public void postConstruct() {
        logger.fine("Generating local password");
        SecureRandom random = new SecureRandom();
        byte[] pwd = new byte[PASSWORD_BYTES];
        random.nextBytes(pwd);
        password = toHex(pwd);
        File localPasswordFile =
            new File(env.getConfigDirPath(), LOCAL_PASSWORD_FILE);
        PrintWriter w = null;
        try {
            if (localPasswordFile.exists()) {
                if (!localPasswordFile.delete()) {
                    logger.log(Level.WARNING, KernelLoggerInfo.cantDeletePasswordFile,
                                    localPasswordFile.toString());
                    // if we can't make sure it's our file, don't write it
                    return;
                }
            }
            if (!localPasswordFile.createNewFile()) {
                logger.log(Level.WARNING, KernelLoggerInfo.cantCreatePasswordFile,
                                localPasswordFile.toString());
                // if we can't make sure it's our file, don't write it
                return;
            }

            /*
             * XXX - There's a security hole here.
             * Between the time the file is created and the permissions
             * are changed to prevent others from opening it, someone
             * else could open it and wait for the data to be written.
             * Java needs the ability to create a file that's readable
             * only by the owner; coming in JDK 7.
             *
             * The setReadable(false, false) call will fail on Windows.
             * we ignore the failures on all platforms - this is a best
             * effort.  The above calls ensured that the file is our
             * file, so the following is the best we can do on all
             * operating systems.
             */
            localPasswordFile.setWritable(false, false); // take from all
            localPasswordFile.setWritable(true, true);   // owner only
            localPasswordFile.setReadable(false, false); // take from all
            localPasswordFile.setReadable(true, true);   // owner only

            w = new PrintWriter(localPasswordFile);
            w.println(password);
        } catch (IOException ex) {
            // ignore errors
            logger.log(Level.FINE, "Exception writing local password file", ex);
        } finally {
            if (w != null)
                w.close();
        }
    }

    /**
     * Is the given password the local password?
     */
    public boolean isLocalPassword(String p) {
        return password != null && password.equals(p);
    }

    /**
     * Get the local password.
     */
    public String getLocalPassword() {
        return password;
    }

    /**
     * Convert the byte array to a hex string.
     */
    private static String toHex(byte[] b) {
        char[] bc = new char[b.length * 2];
        for (int i = 0, j = 0; i < b.length; i++) {
            byte bb = b[i];
            bc[j++] = hex[(bb >> 4) & 0xF];
            bc[j++] = hex[bb & 0xF];
        }
        return new String(bc);
    }
}
