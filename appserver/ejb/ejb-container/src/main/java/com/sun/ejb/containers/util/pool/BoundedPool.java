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

/**
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/BoundedPool.java,v $</I>
 * @author     $Author: mk111283 $
 * @version    $Revision: 1.4 $ $Date: 2006/12/23 13:48:57 $
 */
 

package com.sun.ejb.containers.util.pool;

import java.util.Collection;
import java.util.ArrayList;
import java.util.logging.*;

import com.sun.logging.*;

/**
 * <p>Abstract pool provides the basic implementation of an object pool. 
 * The implementation uses a linked list to maintain a list of (available) 
 * objects. If the pool is empty it simply creates one using the 
 * ObjectFactory instance. Subclasses can change this behaviour by overriding 
 * getObject(...) and returnObject(....) methods. This class provides basic 
 * support for synchronization, event notification, pool shutdown and pool 
 * object recycling. It also does some very basic bookkeeping like the
 * number of objects created, number of threads waiting for object.
 * <p> Subclasses can make use of these book-keeping data to provide complex 
 * pooling mechanism like LRU / MRU / Random. Also, note that AbstractPool 
 * does not have a notion of  pool limit. It is upto to the derived classes 
 * to implement these features.
 *	
 */
public class BoundedPool
    extends AbstractPool
{
    
    protected int previousSize = 0;
    
    public BoundedPool(ObjectFactory factory, long beanId, int steadyPoolSize,
        int resizeQuantity, int maxPoolsize, long maxWaitTimeInMillis, 
        int idleTimeoutInSeconds, ClassLoader loader)
    {
    	super(factory, beanId, steadyPoolSize, resizeQuantity, maxPoolsize,
              maxWaitTimeInMillis, idleTimeoutInSeconds, loader);
        super.poolName="BoundedPool";
    }
    
    protected void removeIdleObjects() {
        int curSize = 0;
        int count = 0;
        synchronized (list) {
            curSize = list.size();
        }

        if(curSize <= steadyPoolSize)
            return; // no need to trim the pool beyond steadyPoolSize
        count = (curSize > (steadyPoolSize + resizeQuantity) ) ? 
            resizeQuantity: (curSize - steadyPoolSize);
        previousSize = curSize;
        
        if (count > 0) {
            _logger.log(Level.FINE,
                        "BoundedPool removing " + count + " objects");
            super.remove(count);
        }
    }

}
