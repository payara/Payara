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
 * <BR> <I>$Source: /cvs/glassfish/appserv-core/src/java/com/sun/ejb/containers/util/pool/Pool.java,v $</I>
 * @author     $Author: tcfujii $
 * @version    $Revision: 1.3 $ $Date: 2005/12/25 04:13:35 $
 */
 
package com.sun.ejb.containers.util.pool;

/**
 * Pool defines the methods that can be used by the application to access 
 * pooled objects. The basic assumption is that all objects in the pool are 
 * identical (homogeneous). This interface defines methods for a) getting an
 * object from the pool, b) returning an object back to the pool 
 * and, c) destroying (instead of reusing) an object. In addition to these 
 * methods, the Pool has methods for adding and removing PoolEventListeners.
 * There are six overloaded methods for getting objects from a pool.
 *	
 */
public interface Pool {
    
    /**
       @deprecated  
    */
    public Object getObject(boolean canWait, Object param)
        throws PoolException;
    
    /**
       @deprecated  
    */
    public Object getObject(long maxWaitTime, Object param)
        throws PoolException;
    
    /**
     * Get an object from the pool within the specified time.
     * @param The amount of time the calling thread agrees to wait.
     * @param Some value that might be used while creating the object
     * @return an Object or null if an object could not be returned in 
     *   'waitForMillis' millisecond.
     * @exception Throws PoolException if an object cannot be created
     */
    public Object getObject(Object param)
        throws PoolException;

    /**
     * Return an object back to the pool. An object that is obtained through
     *	getObject() must always be returned back to the pool using either 
     *	returnObject(obj) or through destroyObject(obj).
     */
    public void returnObject(Object obj);
    			
    /**
     * Destroys an Object. Note that applications should not ignore the 
     * reference to the object that they got from getObject(). An object
     * that is obtained through getObject() must always be returned back to
     * the pool using either returnObject(obj) or through destroyObject(obj).
     * This method tells that the object should be destroyed and cannot be
     * reused.
     *	
     */
    public void destroyObject(Object obj);
    	
}
