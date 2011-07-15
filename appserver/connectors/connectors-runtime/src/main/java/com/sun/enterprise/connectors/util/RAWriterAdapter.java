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

package com.sun.enterprise.connectors.util;

import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writer that will be given to / used by MCF, MCs of resource adapter<br>
 * PrintWriter will be set during MCF initialization<br>
 */
public class RAWriterAdapter extends Writer {
    private Logger logger;
    //by default, autoFlush will be ON.
    private boolean autoFlush = true;
    //buffer used when autoFlush is OFF
    private StringBuffer log;

    public RAWriterAdapter(Logger logger) {
        this.logger = logger;
        initializeAutoFlush();
    }

    private void initializeAutoFlush() {
        String autoFlushValue = System.getProperty("com.sun.enterprise.connectors.LogWriterAutoFlush", "true");
        autoFlush = Boolean.valueOf(autoFlushValue);
    }

    public void write(char cbuf[], int off, int len) {
        if (autoFlush) {
            logger.log(Level.INFO, new String(cbuf, off, len));
        } else {
            String s = new String(cbuf, off, len);
            if (log == null) {
                log = new StringBuffer(s);
            } else {
                log = log.append(s);
            }
        }
    }

    public void flush() {
        if (!autoFlush) {
            logger.log(Level.INFO, log.toString());
            log = null;
        }
    }

    public void close() {
        //no-op
    }
}
