/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.server.logging;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
* Send a message via syslog.
*/
/**
 * this code is taken from spy.jar and enhanced
 * User: cmott
 */
public class Syslog {
    public static final int EMERG=0;
    public static final int ALERT=1;
    public static final int CRIT=2;
    public static final int ERR=3;
    public static final int WARNING=4;
    public static final int NOTICE=5;
    public static final int INFO=6;
    public static final int DEBUG=7;

    public static final int KERN = 0;
    public static final int USER = 8;
    public static final int MAIL = 16;
    public static final int DAEMON = 24;
    public static final int AUTH = 32;
    public static final int SYSLOG = 40;
    public static final int LPR = 48;
    public static final int NEWS = 56;
    public static final int UUCP = 64;
    public static final int CRON = 72;
    public static final int AUTHPRIV = 80;
    public static final int FTP = 88;
    public static final int LOCAL0 = 128;
    public static final int LOCAL1 = 136;
    public static final int LOCAL2 = 144;
    public static final int LOCAL3 = 152;
    public static final int LOCAL4 = 160;
    public static final int LOCAL5 = 168;
    public static final int LOCAL6 = 176;
    public static final int LOCAL7 = 184;

    private static final int SYSLOG_PORT=514;

    private final InetAddress addr;

    /**
     * Log to a particular log host.
     */
    public Syslog(String loghost) throws UnknownHostException {
      addr=InetAddress.getByName(loghost);
    }

    /**
     * Send a log message.
     */
    public void log(int facility, int level, String msg) {
      int fl=facility | level;

      String what="<" + fl + ">" + msg;
      // System.out.println("Writing to syslog:" + what);

      try {
        byte[] buf = what.getBytes();
        int len = buf.length;
        DatagramPacket dp = new DatagramPacket(buf,len,addr,SYSLOG_PORT);
        DatagramSocket s = new DatagramSocket();
        s.send(dp);
        if(!s.isClosed()) {
            s.close();
        }
      } catch(IOException e) {
        LogFacade.LOGGING_LOGGER.log(Level.SEVERE, LogFacade.ERROR_SENDING_SYSLOG_MSG, e);
      }
    }

    
}
