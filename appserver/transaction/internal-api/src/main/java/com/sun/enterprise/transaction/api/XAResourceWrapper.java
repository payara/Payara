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

package com.sun.enterprise.transaction.api;

import javax.sql.*;
import javax.transaction.xa.*;
import javax.security.auth.Subject;
import javax.resource.spi.ManagedConnection;

import com.sun.enterprise.util.i18n.StringManager;

/**
 * Wrappers over XAResources extend from this class. This class simply implements the
 * the standard XAResource interface. In addition it holds the XAConnection which is
 * set by XARecoveryManager and is used by deriving classes to implement workarounds.
 * An example of class extending from this is OracleXARescource.
 *
 * @author <a href="mailto:bala.dutt@sun.com">Bala Dutt</a>
 * @version 1.0
 */
public abstract class XAResourceWrapper implements XAResource
{

    /// Sting Manager for Localization
    private static StringManager sm = StringManager.getManager(XAResourceWrapper.class);

    protected ManagedConnection m_xacon;
    protected Subject subject;

    public void init(ManagedConnection xacon,Subject subject){
        m_xacon=xacon;
        this.subject = subject;
    }

    public void end(Xid xid, int i) throws XAException{
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public void forget(Xid xid) throws XAException{
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public int getTransactionTimeout() throws XAException{
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public boolean isSameRM(XAResource xaresource) throws XAException
    {
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public int prepare(Xid xid) throws XAException{
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public boolean setTransactionTimeout(int i) throws XAException {
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public void start(Xid xid, int i) throws XAException{
        throw new XAException(sm.getString("transaction.for_recovery_only"));
    }

    public abstract Xid[] recover(int flag) throws XAException;

    public abstract void commit(Xid xid, boolean flag) throws XAException;

    public abstract void rollback(Xid xid) throws XAException;

    /**
    public Xid[] recover(int flag) throws XAException {
        throw new XAException("This is to be implemented by sub classes");
    }
    public void commit(Xid xid, boolean flag) throws XAException{
        throw new XAException("This is to be implemented by sub classes");
    }
    public void rollback(Xid xid) throws XAException{
        throw new XAException("This is to be implemented by sub classes");
    }
    */

    public abstract XAResourceWrapper getInstance();
}
