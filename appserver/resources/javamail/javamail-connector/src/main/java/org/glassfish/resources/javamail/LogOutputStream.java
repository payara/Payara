/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.javamail;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Capture output lines and send them to the system error log.
 */
public class LogOutputStream extends OutputStream {
    protected Logger logger;
    protected Level level;

    private int lastb = -1;
    private byte[] buf = new byte[80];
    private int pos = 0;

    /**
     * Log to the specified facility at the default FINE level.
     */
    public LogOutputStream(String facility) {
	this(facility, Level.FINE);
    }

    /**
     * Log to the specified facility at the specified level.
     */
    public LogOutputStream(String facility, Level level) {
	logger = Logger.getLogger(facility);
	this.level = level;
    }

    public void write(int b) throws IOException {
	if (!logger.isLoggable(level))
	    return;

	if (b == '\r') {
	    logBuf();
	} else if (b == '\n') {
	    if (lastb != '\r')
		logBuf();
	} else {
	    expandCapacity(1);
	    buf[pos++] = (byte)b;
	}
	lastb = b;
    }

    public void write(byte b[]) throws IOException {
	write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
	int start = off;
	
	if (!logger.isLoggable(level))
	    return;
	len += off;
	for (int i = start; i < len ; i++) {
	    if (b[i] == '\r') {
		expandCapacity(i - start);
		System.arraycopy(b, start, buf, pos, i - start);
		pos += i - start;
		logBuf();
		start = i + 1;
	    } else if (b[i] == '\n') {
		if (lastb != '\r') {
		    expandCapacity(i - start);
		    System.arraycopy(b, start, buf, pos, i - start);
		    pos += i - start;
		    logBuf();
		}
		start = i + 1;
	    }
	    lastb = b[i];
	}
	if ((len - start) > 0) {
	    expandCapacity(len - start);
	    System.arraycopy(b, start, buf, pos, len - start);
	    pos += len - start;
	}
    }

    /**
     * Log the specified message.
     * Can be overridden by subclass to do different logging.
     */
    protected void log(String msg) {
	logger.log(level, msg);
    }

    /**
     * Convert the buffer to a string and log it.
     */
    private void logBuf() {
	String msg = new String(buf, 0, pos);
	pos = 0;
	log(msg);
    }

    /**
     * Ensure that the buffer can hold at least len bytes
     * beyond the current position.
     */
    private void expandCapacity(int len) {
	while (pos + len > buf.length) {
	    byte[] nb = new byte[buf.length * 2];
	    System.arraycopy(buf, 0, nb, 0, pos);
	    buf = nb;
	}
    }
}
