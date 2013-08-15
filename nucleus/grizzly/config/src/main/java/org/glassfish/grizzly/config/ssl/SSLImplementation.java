/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config.ssl;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import org.glassfish.grizzly.config.GrizzlyConfig;

import org.glassfish.grizzly.ssl.SSLSupport;

/**
 * SSLImplementation:
 *
 * Abstract factory and base class for all SSL implementations.
 *
 * @author EKR
 */
public abstract class SSLImplementation {
    /**
     * Default Logger.
     */
    private final static Logger logger = GrizzlyConfig.logger();
    // The default implementations in our search path
    private static final String JSSEImplementationClass = JSSEImplementation.class.getName();
    private static final String[] implementations = {JSSEImplementationClass};

    public static SSLImplementation getInstance() throws ClassNotFoundException {
        for (String implementation : implementations) {
            try {
                return getInstance(implementation);
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Error creating " + implementation, e);
                }
            }
        }
        // If we can't instantiate any of these
        throw new ClassNotFoundException("Can't find any SSL implementation");
    }

    public static SSLImplementation getInstance(String className) throws ClassNotFoundException {
        if (className == null) {
            return getInstance();
        }
        try {
            return (SSLImplementation) ((Class) Class.forName(className)).newInstance();
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Error loading SSL Implementation " + className, e);
            }
            throw new ClassNotFoundException("Error loading SSL Implementation " + className + " :" + e.toString());
        }
    }

    public abstract String getImplementationName();

    public abstract ServerSocketFactory getServerSocketFactory();

    public abstract SSLSupport getSSLSupport(Socket sock);

    public abstract SSLSupport getSSLSupport(SSLEngine sslEngine);
}    
