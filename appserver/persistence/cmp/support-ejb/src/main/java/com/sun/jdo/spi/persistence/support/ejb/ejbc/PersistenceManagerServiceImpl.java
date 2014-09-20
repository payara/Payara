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

/*
 * PersistenceManagerServiceImpl.java
 *
 * Created on January 24, 2002
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

import com.sun.jdo.spi.persistence.utility.logging.LogHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.CMPHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.SunContainerHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ejb.LoggerFactoryiAS;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.ejb.spi.CMPService;

/**
 * This class implements CMPService contract and allows to load Sun specific implementation of the
 * ContainerHelper when loaded.
 */
@Service
public class PersistenceManagerServiceImpl implements CMPService {

    // Initialize the appserver loggers.
    static {
        LogHelper.registerLoggerFactory(new LoggerFactoryiAS());
    }

    // Reference and force the initialization of the Sun specific 
    // implementation of the TransactionHelper and the ContainerHelper.
    private static final Class helper = forceInit(SunContainerHelper.class);

    // Reference and force the initialization of the DeploymentEventListener
    // implementation.
/**
    private static final Class listener = 
        forceInit("com.sun.jdo.spi.persistence.support.ejb.ejbc.DeploymentEventListenerImpl");
**/

    public boolean isReady() {
        // Checks that SunContainerHelper regeistered with CMPHelper
        return CMPHelper.isContainerReady();
    }

    /**
     * Forces the initialization of the class pertaining to the specified
     * <tt>Class</tt> object. This method does nothing if the class is already
     * initialized prior to invocation.
     *
     * @param klass the class for which to force initialization
     * @return <tt>klass</tt>
     */
    private static <T> Class<T> forceInit(Class<T> klass) {
        try {
            Class.forName(klass.getName(), true, klass.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);  // Can't happen
        }
        return klass;
    }

}
