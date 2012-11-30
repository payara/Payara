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

package com.sun.enterprise.v3.server;

import com.sun.appserv.server.LifecycleEventContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.kernel.KernelLoggerInfo;

public class LifecycleEventContextImpl implements LifecycleEventContext {
    
    private ServerContext ctx;

    private static final Logger logger = KernelLoggerInfo.getLogger();
    
    /**
     * public constructor
     */
    public LifecycleEventContextImpl(ServerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Get the server command-line arguments
     */
    public String[] getCmdLineArgs() {
        return ctx.getCmdLineArgs();
    }
    
    /**
     * Get server installation root
     */
    public String getInstallRoot() {
        return ctx.getInstallRoot().getPath();
    }
    
    /**
     * Get the server instance name
     */
    public String getInstanceName() {
        return ctx.getInstanceName();
    }
    
    /** 
     * Get the initial naming context.
     */
    public InitialContext getInitialContext() {
        return ctx.getInitialContext();
    }

    /**
     * Writes the specified message to a server log file.
     *
     * @param msg 	a <code>String</code> specifying the 
     *			message to be written to the log file
     */
    public void log(String message) {
        logger.info(message);
    }
    
    /**
     * Writes an explanatory message and a stack trace
     * for a given <code>Throwable</code> exception
     * to the server log file.
     *
     * @param message 		a <code>String</code> that 
     *				describes the error or exception
     *
     * @param throwable 	the <code>Throwable</code> error 
     *				or exception
     */
    public void log(String message, Throwable throwable) {
        logger.log(Level.INFO, message, throwable);
    }
}
