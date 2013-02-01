/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.ejb.entitybean.container;

import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.security.SecurityManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/*
* This class implements the Commit-Option C as described in
* the EJB Specification.
*
* The CommitOptionC Container extends Entity Container and
* hence all the life cycle management is still in Entitycontainer
*
* @author Mahesh Kannan
*/

public class CommitCEntityContainer
    extends EntityContainer
{
    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     * @exception Exception on error
     */
    protected CommitCEntityContainer(EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
        throws Exception
    {
        super(desc, loader, sm);
    }
    
    protected EntityContextImpl getReadyEJB(EjbInvocation inv) {
        Object primaryKey = getInvocationKey(inv);
        return activateEJBFromPool(primaryKey, inv);
    }
    
    protected void createReadyStore(int cacheSize, int numberOfVictimsToSelect,
            float loadFactor, long idleTimeout)
    {
        readyStore = null;
    }
    
    protected void createEJBObjectStores(int cacheSize,
            int numberOfVictimsToSelect, long idleTimeout) throws Exception
    {
        super.defaultCacheEJBO = false;
        super.createEJBObjectStores(cacheSize, numberOfVictimsToSelect, idleTimeout);
    }
    
    // called from releaseContext, afterCompletion
    protected void addReadyEJB(EntityContextImpl context) {
        passivateAndPoolEJB(context);
    }
    
    protected void destroyReadyStoreOnUndeploy() {
        readyStore = null;
    }
    
    protected void removeContextFromReadyStore(Object primaryKey,
            EntityContextImpl context)
    {
        // There is nothing to remove as we don't have a readyStore
    }
    
}

