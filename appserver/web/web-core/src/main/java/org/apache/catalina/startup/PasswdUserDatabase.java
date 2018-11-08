/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.startup;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Concrete implementation of the <strong>UserDatabase</code> interface
 * that processes the <code>/etc/passwd</code> file on a Unix system.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:28:09 $
 */

public final class PasswdUserDatabase
    implements UserDatabase {

    private static final Logger LOGGER = Logger.getLogger(PasswdUserDatabase.class.getName());

    // --------------------------------------------------------- Constructors


    /**
     * Initialize a new instance of this user database component.
     */
    public PasswdUserDatabase() {

        super();

    }


    // --------------------------------------------------- Instance Variables


    /**
     * The pathname of the Unix password file.
     */
    private static final String PASSWORD_FILE = "/etc/passwd";


    /**
     * The set of home directories for all defined users, keyed by username.
     */
    private Hashtable<String, String> homes = new Hashtable<String, String>();


    /**
     * The UserConfig listener with which we are associated.
     */
    private UserConfig userConfig = null;


    // ----------------------------------------------------------- Properties


    /**
     * Return the UserConfig listener with which we are associated.
     */
    public UserConfig getUserConfig() {

        return (this.userConfig);

    }


    /**
     * Set the UserConfig listener with which we are associated.
     *
     * @param userConfig The new UserConfig listener
     */
    public void setUserConfig(UserConfig userConfig) {

        this.userConfig = userConfig;
        init();

    }


    // ------------------------------------------------------- Public Methods


    /**
     * Return an absolute pathname to the home directory for the specified user.
     *
     * @param user User for which a home directory should be retrieved
     */
    public String getHome(String user) {

        return homes.get(user);

    }


    /**
     * Return an enumeration of the usernames defined on this server.
     */
    public Enumeration<String> getUsers() {

        return (homes.keys());

    }


    // ------------------------------------------------------ Private Methods


    /**
     * Initialize our set of users and home directories.
     */
    private void init() {

        try (BufferedReader reader = new BufferedReader(new FileReader(PASSWORD_FILE))) {

            while (true) {

                // Accumulate the next line
                StringBuilder buffer = new StringBuilder();
                while (true) {
                    int ch = reader.read();
                    if ((ch < 0) || (ch == '\n'))
                        break;
                    buffer.append((char) ch);
                }
                String line = buffer.toString();
                if (line.length() < 1)
                    break;

                // Parse the line into constituent elements
                int n = 0;
                String tokens[] = new String[7];
                for (int i = 0; i < tokens.length; i++)
                    tokens[i] = null;
                while (n < tokens.length) {
                    String token;
                    int colon = line.indexOf(':');
                    if (colon >= 0) {
                        token = line.substring(0, colon);
                        line = line.substring(colon + 1);
                    } else {
                        token = line;
                        line = "";
                    }
                    tokens[n++] = token;
                }

                // Add this user and corresponding directory
                if ((tokens[0] != null) && (tokens[5] != null))
                    homes.put(tokens[0], tokens[5]);

            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Initialization of set of users and home directories failed.", e);
        }

    }


}
