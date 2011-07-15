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

package com.sun.enterprise.resource;

import javax.transaction.xa.*;
import java.util.logging.*;
import com.sun.logging.*;

/**
 * This is class is used for debugging. It prints out
 * trace information on TM calls to XAResource before
 * directing the call to the actual XAResource object
 *
 * @author Tony Ng
 *
 */
public class XAResourceWrapper implements XAResource {

    // the actual XAResource object
    private XAResource res;

    public XAResourceWrapper(XAResource res) {
        this.res = res;
    }

    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    private static Logger _logger ;
    static{
           _logger = LogDomains.getLogger(XAResourceWrapper.class, LogDomains.RSR_LOGGER);
          }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        print("XAResource.commit: " + xidToString(xid) + "," + onePhase);
        res.commit(xid, onePhase);
    }

    public void end(Xid xid, int flags) throws XAException {
        print("XAResource.end: " + xidToString(xid) + "," +
              flagToString(flags));
        res.end(xid, flags);
    }

    
    public void forget(Xid xid) throws XAException {
        print("XAResource.forget: " + xidToString(xid));
        res.forget(xid);
    }

    public int getTransactionTimeout() throws XAException {
        return res.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        if (xares instanceof XAResourceWrapper) {
            XAResourceWrapper other = (XAResourceWrapper) xares;
            boolean result = res.isSameRM(other.res);
            print("XAResource.isSameRM: " + res + "," + other.res + "," +
                  result);
            return result;
        } else {
            boolean result = res.isSameRM(xares);
            print("XAResource.isSameRM: " + res + "," + xares + "," +
                  result);
            return result;
            //throw new IllegalArgumentException("Has to use XAResourceWrapper for all XAResource objects: " + xares);
        }
    }

    public int prepare(Xid xid) throws XAException {
        print("XAResource.prepare: " + xidToString(xid));
        int result = res.prepare(xid);
        print("prepare result = " + flagToString(result));
        return result;
    }
    
    public Xid[] recover(int flag) throws XAException {
        print("XAResource.recover: " + flagToString(flag));
        return res.recover(flag);
    }

    public void rollback(Xid xid) throws XAException {
        print("XAResource.rollback: " + xidToString(xid));
        res.rollback(xid);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return res.setTransactionTimeout(seconds);
    }
            
    public void start(Xid xid, int flags) throws XAException {
        print("XAResource.start: " + xidToString(xid) + "," +
              flagToString(flags));
        res.start(xid, flags);
    }

    private void print(String s) {
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,s);
        }
    }

    private static String xidToString(Xid xid) {
        return String.valueOf((new String(xid.getGlobalTransactionId()) +
                               new String(xid.getBranchQualifier())).hashCode());
    }

    private static String flagToString(int flag) {
        switch (flag) {
        case TMFAIL:
            return "TMFAIL";
        case TMJOIN:
            return "TMJOIN";
        case TMNOFLAGS:
            return "TMNOFLAGS";
        case TMONEPHASE:
            return "TMONEPHASE";
        case TMRESUME:
            return "TMRESUME";
        case TMSTARTRSCAN:
            return "TMSTARTRSCAN";
        case TMENDRSCAN:
            return "TMENDRSCAN";
        case TMSUCCESS:
            return "TMSUCCESS";
        case TMSUSPEND:
            return "TMSUSPEND";
        case XA_RDONLY:
            return "XA_RDONLY";
        default:
            return "" + Integer.toHexString(flag);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof XAResourceWrapper) {
            XAResource other = ((XAResourceWrapper) obj).res;
            return res.equals(other);
        }
        if (obj instanceof XAResource) {
            XAResource other = (XAResource) obj;
            return res.equals(other);
        }
        return false;
    }

    public int hashCode() {
        return res.hashCode();
    }
}    
