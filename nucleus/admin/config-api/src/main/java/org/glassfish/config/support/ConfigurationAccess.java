/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

/**
 * Service to lock the configuration elements for a particular domain
 * configuration. All changes to the domain configuration changes
 * being the domain.xml or the security artifacts must go through
 * this service to ensure proper synchronization.
 *
 * The access gate must be implemented using a read-write locking
 * where multiple users can access in read mode the configuration
 * while a write access requires a exclusive lock.
 *
 * A try {...} finally {...} block should be used to ensure the
 * Lock returned for access is released when the access to the
 * configuration is not needed any longer.
 *
 * @author Jerome Dochez
 */
@Contract
public interface ConfigurationAccess {

    /**
     * Wait and return an read access {@link Lock} to the configuration
     * elements. Once the lock is returned, other threads can access
     * the configuration is read mode, but no thread can access it in
     * write mode.
     *
     * The lock instance must be released in the same thread that
     * obtained it.
     *
     * @return the read access lock to be released once the
     * configuration access is not needed any longer.
     * @throws IOException if the configuration cannot be accessed
     * due to a file access error.
     * @throws TimeoutException if the lock cannot be obtained
     * before the system defined time out runs out.
     */
    public Lock accessRead() throws IOException, TimeoutException;

    /**
     * Wait and return an exclusive write access {@link Lock} to the configuration
     * elements. Once the lock is returned, no other thread can
     * access the configuration is read or write mode.
     *
     * The lock instance must be released in the same thread that
     * obtained it.
     *
     * @return the read access lock to be released once the
     * configuration access is not needed any longer.
     * @throws IOException if the configuration cannot be accessed
     * due to a file access error.
     * @throws TimeoutException if the lock cannot be obtained
     * before the system defined time out runs out.
     */

    public Lock accessWrite() throws IOException, TimeoutException;
}
