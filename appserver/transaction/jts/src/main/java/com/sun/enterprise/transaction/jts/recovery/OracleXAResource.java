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

//Source File Name:   OracleXAResource.java

package com.sun.enterprise.transaction.jts.recovery;

import java.sql.*;
import javax.transaction.xa.*;

import com.sun.enterprise.transaction.api.XAResourceWrapper;
import com.sun.enterprise.util.i18n.StringManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;


/**
 * This implements workaround for Oracle XAResource. Oracle's 8.1.7 XAResource implementation
 * doesn't work fine while recovery. This class fires sql statements to achieve same.
 *
 * @author <a href="mailto:bala.dutt@sun.com">Bala Dutt</a>
 * @version 1.0
 */
public class OracleXAResource extends XAResourceWrapper
{

    // Use superclass for Sting Manager 
    private static final StringManager sm = StringManager.getManager(XAResourceWrapper.class);

    // Use JTA_LOGGER for backward compatibility, so use a class from 
    // 'jta' bundle to load it.
    private static final Logger _logger = LogDomains.getLogger(
            com.sun.enterprise.transaction.JavaEETransactionManagerSimplified.class, 
            LogDomains.JTA_LOGGER);

    public XAResourceWrapper getInstance() {
        return new OracleXAResource();
    }
	
  /**
   * Recovers list of xids in transaction table. Recover on oracle ignores flags sent to it, this method
   * takes care of flags in addition to calling recoverList for xid list.
   *
   * @param flag an <code>int</code> value
   * @return a <code>Xid[]</code> value
   * @exception XAException if an error occurs
   */
    public Xid[] recover(int flag) throws XAException {
        if(flag==XAResource.TMNOFLAGS)
            return null;
	return recoverList(flag);
    }
  /**
   * Fires a select statement so that transaction xids are updated and retrieve the xid list. Oracle
   * doesn't update the xid's for sometime. After this update, recover of real oracle xa resource is
   * is used get xid list.
   *
   * @return a <code>Xid[]</code> value
   * @exception XAException if an error occurs
   */
  private Xid [] recoverList(int flag) throws XAException{
        Statement stmt = null;
        ResultSet resultset = null;
        Connection con = null;
        try{
            con=(Connection)m_xacon.getConnection(subject,null);
            if(null == con)
                // throw new XAException("Oracle XA Resource wrapper : connection could not be got");
                throw new XAException(sm.getString("transaction.oracle_xa_wrapper_connection_failed"));
            stmt = con.createStatement();
            resultset = stmt.executeQuery(
                "select pending.local_tran_id from SYS.PENDING_TRANS$ pending, SYS.DBA_2PC_NEIGHBORS");
            resultset.close();
            resultset = null;
            stmt.close();
            stmt=null;
            return m_xacon.getXAResource().recover(flag);
        }
        catch(SQLException sqlexception){
            //Trace.info("Failed to recover xid list");
            // throw new XAException("oracle XA Resource wrapper : "+sqlexception);
            throw new XAException(sm.getString("transaction.oracle_sqlexception_occurred",sqlexception));
        }
        catch(XAException e){
            throw e;
        }
        catch(Exception e){
            throw new XAException(sm.getString("transaction.oracle_unknownexception_occurred",e));
            // throw new XAException("oracle XA Resource wrapper : "+e);
        }
        finally{
            if(null != resultset)
                try{
                    resultset.close();
                }
                catch(SQLException sqlexception1) { }
            if(null != stmt)
                try{
                    stmt.close();
                }
                catch(SQLException sqlexception2) { }
        }
    }
    public void commit(Xid xid, boolean flag) throws XAException{
        doRecovery(xid, true);
    }
    public void rollback(Xid xid) throws XAException{
        doRecovery(xid, false);
    }
  /**
   * Does actual recovery depending on boolean argument - true for commmit.
   *
   * @param xid a <code>Xid</code> value
   * @param isCommit a <code>boolean</code> value
   * @exception XAException if an error occurs
   */
  private void doRecovery(Xid xid, boolean isCommit) throws XAException{

        try {
            if (isCommit)
                m_xacon.getXAResource().commit(xid,true);
            else
                m_xacon.getXAResource().rollback(xid);
        } catch (XAException ex) {
            _logger.log(Level.FINEST," An XAException occurred while calling XAResource method " , ex);
        } catch (Exception ex) {
            _logger.log(Level.FINEST," An Exception occurred while calling XAResource method " , ex);
        }

        Statement stmt = null;
        ResultSet resultset = null;
        Connection con = null;
        try{
            con=(Connection)m_xacon.getConnection(subject,null);
            if(null == con)
                throw new XAException(sm.getString("transaction.oracle_xa_wrapper_connection_failed"));
                // throw new XAException("Oracle XA Resource wrapper : connection could not be got");
            stmt = con.createStatement();
            resultset = stmt.executeQuery(
                "select pending.local_tran_id from SYS.PENDING_TRANS$ pending, SYS.DBA_2PC_NEIGHBORS dba where pending.global_foreign_id = '"
                + toHexString(xid.getGlobalTransactionId()) +
                "' and pending.local_tran_id = dba.local_tran_id and dba.branch = '"
                + toHexString(xid.getBranchQualifier()) +
                "' and pending.state = 'prepared'");
            if(resultset.next()){
                String s = resultset.getString(1);
                resultset.close();
                resultset = null;
                stmt.executeUpdate((isCommit ? "commit force '" : "rollback force '") + s + "'");
                stmt.close();
                stmt=null;
            }
        }
        catch(SQLException sqlexception){
            //Trace.info("Failed to recover " + xid+" "+ sqlexception);
            _logger.log(Level.FINE," An SQLException during recovery " , sqlexception);
            throw new XAException(sm.getString("transaction.oracle_sqlexception_occurred",sqlexception));
            // throw new XAException("oracle XA Resource wrapper : "+sqlexception);
        }
        catch(Exception e){
            //Trace.info("Exception while RecoveryTestRI recover "+e);
            _logger.log(Level.FINE," An Exception during recovery " , e);
            throw new XAException(sm.getString("transaction.oracle_unknownexception_occurred",e));
            // throw new XAException("oracle XA Resource wrapper : "+e);
        }
        finally{
            if(null != resultset)
                try{
                    resultset.close();
                }
                catch(SQLException sqlexception1) { }
            if(null != stmt)
                try{
                    stmt.close();
                }
                catch(SQLException sqlexception2) { }
        }
    }
    private static final char HEX_DIGITS[] = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F'
    };
  /**
   * Converts Xids into string that can be used in sql statements for oracle.
   *
   * @param abyte0[] a <code>byte</code> value
   * @return a <code>String</code> value
   */
  private static String toHexString(byte abyte0[]) {
        StringBuffer stringbuffer = new StringBuffer();
        if(null != abyte0 && 0 < abyte0.length) {
            for(int i = 0; i < abyte0.length; i++) {
                stringbuffer.append(HEX_DIGITS[(abyte0[i] & 0xf0) >> 4]);
                stringbuffer.append(HEX_DIGITS[abyte0[i] & 0xf]);
            }
            return stringbuffer.toString();
         } else {
            return "";
         }
    }
}
