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

package com.sun.enterprise.transaction.jts.api;
/**
 *
 * @author mvatkina
 * Date: Sep 10, 2010
 */

/**
 * Interface to implement by delegated transaction recovery lock
 */

public interface DelegatedTransactionRecoveryFence {
    /**
     * Returns true if the specified instance on the specified path is being recovered
     * after the specified timestamp
     */
    public boolean isFenceRaised(String path, String instanceName, long timestamp);

    /**
     * Raise the fence for the specified instance on the specified path so that no other instance can
     * start delegated recovery at the same time.
     */
    public void raiseFence(String path, String instanceName);

    /**
     * Lower the fence for the specified instance on the specified path
     */
    public void lowerFence(String path, String instanceName);
        
    /**
     * Returns instance for which delegated recovery was done before the timestamp specified 
     * on the specified path or null if such instance does not exist
     */
    public String getInstanceRecoveredFor(String path, long timestamp);
        
    /**
     * If an instance was doing delegated recovery on the specified path, assign
     * specified instance instead.
     */
    public void transferRecoveryTo(String path, String instanceName);
        
}
