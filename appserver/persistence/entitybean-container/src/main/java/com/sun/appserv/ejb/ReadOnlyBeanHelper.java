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

package com.sun.appserv.ejb;

import java.rmi.RemoteException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.glassfish.persistence.ejb.entitybean.container.spi.ReadOnlyEJBLocalHome;
import org.glassfish.persistence.ejb.entitybean.container.spi.ReadOnlyEJBHome;

import org.glassfish.persistence.ejb.entitybean.container.ReadOnlyBeanNotifierImpl;

import java.util.logging.*;

import com.sun.logging.*;

/**
 * Class that is used to obtain ReadOnlyBeanNotifier
 *  and ReadOnlyBeanLocalNotifier.
 *
 * @author Mahesh Kannan
 */
public class ReadOnlyBeanHelper {
    protected static final Logger _logger =
        LogDomains.getLogger(ReadOnlyBeanHelper.class, LogDomains.EJB_LOGGER);

    public static ReadOnlyBeanNotifier getReadOnlyBeanNotifier(String ejbName) {
        try {
            Context ctx = new InitialContext();
            Object obj = ctx.lookup(ejbName);
            ReadOnlyEJBHome home = (ReadOnlyEJBHome) 
                    PortableRemoteObject.narrow(obj, ReadOnlyEJBHome.class);
            ReadOnlyBeanNotifier roNotifier = new ReadOnlyBeanNotifierImpl();
            roNotifier.setHome(home);
            return roNotifier;
        } catch (Exception ex) {
            if(_logger.isLoggable(Level.SEVERE)) {
                _logger.log(Level.SEVERE, "entitybean.container.remote_exception", ex);
            }
        }
        return null;
    }

    public static ReadOnlyBeanLocalNotifier getReadOnlyBeanLocalNotifier(
            String ejbName)
    {
        try {
            Context ctx = new InitialContext();
            ReadOnlyEJBLocalHome home = 
                (ReadOnlyEJBLocalHome) ctx.lookup(ejbName);
            return home.getReadOnlyBeanLocalNotifier();
        } catch (Exception ex) {
            if(_logger.isLoggable(Level.SEVERE)) {
                _logger.log(Level.SEVERE, "entitybean.container.remote_exception",ex);
            }
        }
        return null;
    }

}

