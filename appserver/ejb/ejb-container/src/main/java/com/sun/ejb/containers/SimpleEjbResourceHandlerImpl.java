/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.ejb.containers;

import org.glassfish.api.invocation.ResourceHandler;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/*
//This class was originally an inner class of AbstractSingletonContainer. I have moved this to a top level
//  class as it is now used by EjbInvocation.clone() method also.
*/
public class SimpleEjbResourceHandlerImpl
        implements ResourceHandler, Synchronization {

    private static Map<Transaction, SimpleEjbResourceHandlerImpl> _resourceHandlers =
            new ConcurrentHashMap<Transaction, SimpleEjbResourceHandlerImpl>();

    private List l = null;
    private Transaction tx = null;
    private TransactionManager tm = null;

    private SimpleEjbResourceHandlerImpl(TransactionManager tm) {
        this.tm = tm;
        checkTransaction();
    }

    public static ResourceHandler createResourceHandler(TransactionManager tm) {
        return new SimpleEjbResourceHandlerImpl(tm);
    }

    public static ResourceHandler getResourceHandler(TransactionManager tm) {
        SimpleEjbResourceHandlerImpl rh = null;
        try {
            Transaction tx = tm.getTransaction();
            if (tx != null) {
                rh = _resourceHandlers.get(tx);
            }
        } catch (Exception e) {
            BaseContainer._logger.log(Level.WARNING, "Exception during Singleton ResourceHandler processing", e);
        }

        if (rh == null) {
            rh = new SimpleEjbResourceHandlerImpl(tm);
        }

        return rh;
    }

    public List getResourceList() {
        if (tx == null) {
            checkTransaction();
        }

        if( l == null ) {
            l = new ArrayList();
        }
        return l;
    }

    public void beforeCompletion() {
        // do nothing
    }

    public void afterCompletion(int status) {
        if (tx != null) {
            _resourceHandlers.remove(tx);
            tx = null;
        }
    }

    private void checkTransaction() {
        try {
            tx = tm.getTransaction();
            if (tx != null) {
                tx.registerSynchronization(this);
                _resourceHandlers.put(tx, this);
            }
        } catch (Exception e) {
            tx = null;
            BaseContainer._logger.log(Level.WARNING, "Exception during Singleton ResourceHandler processing", e);
        }
    }

}
