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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]

package fish.payara.logging.jul.handler;


import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

/**
 * Send a message via syslog.
 */
// This code is taken from spy.jar and enhanced
class Syslog {

    private static final int SYSLOG_FACILITY = 24;
    private static final int SYSLOG_PORT = 514;

    private final InetAddress addr;

    /**
     * Log to a particular log host.
     *
     * @param loghost
     * @throws UnknownHostException if no IP address for the host could be found, or if a scope_id
     *             was specified for a global IPv6 address.
     */
    Syslog(final String loghost) throws UnknownHostException {
        addr = InetAddress.getByName(loghost);
    }


    /**
     * Send a log message.
     *
     * @param level
     * @param msg
     */
    void log(final SyslogLevel level, final String msg) {
        final int fl = SYSLOG_FACILITY | level.code();
        final String what = "<" + fl + ">" + msg;
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            final byte[] buf = what.getBytes();
            final int len = buf.length;
            final DatagramPacket dp = new DatagramPacket(buf, len, addr, SYSLOG_PORT);
            datagramSocket.send(dp);
        } catch (final IOException e) {
            PayaraLoggingTracer.error(getClass(), "Failed to send the log message: " + msg, e);
        }
    }


    enum SyslogLevel {

        CRIT(2),
        WARNING(4),
        INFO(6),
        DEBUG(7),
        ;

        private int code;

        SyslogLevel(final int code) {
            this.code = code;
        }


        public int code() {
            return this.code;
        }

        /**
         * @param level
         * @return never null, default is {@link #INFO}
         */
        public static SyslogLevel of(final Level level) {
            if (Level.SEVERE.equals(level)) {
                return CRIT;
            } else if (Level.WARNING.equals(level)) {
                return WARNING;
            } else if (level.intValue() <= Level.FINE.intValue()) {
                return DEBUG;
            } else {
                return INFO;
            }
        }
    }
}
