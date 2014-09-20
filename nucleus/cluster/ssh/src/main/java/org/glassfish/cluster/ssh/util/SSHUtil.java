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
 */

package org.glassfish.cluster.ssh.util;

import com.trilead.ssh2.Connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.io.File;
import java.io.IOException;

import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.admin.CommandException;
/**
 * @author Rajiv Mordani
 */
public class SSHUtil {
    private static final List<Connection> activeConnections = 
                            new ArrayList<Connection>();
    private static final String NL = System.getProperty("line.separator");

   /**
       * Registers a connection for cleanup when the plugin is stopped.
       *
       * @param connection The connection.
       */
      public static synchronized void register(Connection connection) {
          if (!activeConnections.contains(connection)) {
              activeConnections.add(connection);
          }
      }

   /**
       * Unregisters a connection for cleanup when the plugin is stopped.
       *
       * @param connection The connection.
       */
      public static synchronized void unregister(Connection connection) {
          connection.close();
          activeConnections.remove(connection);
      }

   /**
       * Convert empty string to null.
       */
      public static String checkString(String s) {
          if(s==null || s.length()==0)    return null;
          return s;
      }

      public static String getExistingKeyFile() {
        String key = null;
        for (String keyName : Arrays.asList("id_rsa","id_dsa",
                                                "identity"))
        {
            String h = System.getProperty("user.home") + File.separator;
            File f = new File(h+".ssh"+File.separator+keyName);
            if (f.exists()) {
                key =  h  + ".ssh"+File.separator + keyName;
                break;
            }
        }
        return key;
      }

      public static String getDefaultKeyFile() {
          String k = System.getProperty("user.home") + File.separator
          //String k = System.getenv("HOME") + File.separator
                          + ".ssh" + File.separator + "id_rsa";
          return k;
      }

    /**
     * Simple method to validate an encrypted key file
     * @return true|false
     * @throws CommandException
     */
    public static boolean isEncryptedKey(String keyFile) throws CommandException {
        boolean res = false;
        try {
            String f = FileUtils.readSmallFile(keyFile);
            if (f.startsWith("-----BEGIN ") && f.contains("ENCRYPTED")
                    && f.endsWith(" PRIVATE KEY-----" + NL)) {
                res=true;
            }
        }
        catch (IOException ioe) {
            throw new CommandException(Strings.get("error.parsing.key", keyFile, ioe.getMessage()));
        }
        return res;
    }
    
        
    /**
     * This method validates either private or public key file. In case of private
     * key, it parses the key file contents to verify if it indeed contains a key
     * @param  file the key file
     * @return success if file exists, false otherwise
     */
    public static boolean validateKeyFile(String file) throws CommandException {
        boolean ret = false;
        //if key exists, set prompt flag
        File f = new File(file);
        if (f.exists()) {
            if (!f.getName().endsWith(".pub")) {
                String key = null;
                try {
                    key = FileUtils.readSmallFile(file);
                }
                catch (IOException ioe) {
                    throw new CommandException(Strings.get("unable.to.read.key", file, ioe.getMessage()));
                }
                if (!key.startsWith("-----BEGIN ") && !key.endsWith(" PRIVATE KEY-----" + NL)) {
                    throw new CommandException(Strings.get("invalid.key.file", file));
                }
            }
            ret = true;
        }
        else {
            throw new CommandException(Strings.get("key.does.not.exist", file));
        }
        return ret;
    }
}
