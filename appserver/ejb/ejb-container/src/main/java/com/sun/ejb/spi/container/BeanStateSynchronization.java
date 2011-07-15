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

package com.sun.ejb.spi.container;

import javax.ejb.DuplicateKeyException;

/**
 * There are cases where the container would need to interact with the 
 * persistence manager. Some known cases are listed below
 * 1. provide the user with a mechanism to flush changes to the database 
 *    at the end of a method with out waiting until the end of the transaction. 
 * 2. for read only beans provide a mechanism to have the master copy of the bean  
 *    sync up with the database record.
 *
 * Currently the bean concrete implementation that is created as part of the codegen
 * would implement this interface. 
 *
 * @author Pramod Gopinath
 */


public interface BeanStateSynchronization {
    /**
     * Provides a mechanism to flush changes to the database w/o waiting for
     * the end of the transaction, based on some descriptor values set by the user. 
     * The container would call this method in the postInvoke(), only if the flush
     * is enabled for the current method and there were no other exceptions set 
     * into inv.exception.
     */
    public void ejb__flush() 
        throws DuplicateKeyException;

    /**
     * On receiving this message the PM would update its master copy
     * by fetching the latest information for the primary key from the database
     */
    public void ejb__refresh(Object primaryKey);


    /**
     * On receiving this message the PM would delete from its master copy
     * the details related to the primaryKey
     */
    public void ejb__remove(Object primaryKey);
}
