/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.stats;

import java.util.List;
import java.util.Map;
import org.glassfish.j2ee.statistics.JTAStats;

/** Defines additional Sun ONE Application Server specific statistic to transaction service.
 * @author  <a href="mailto:Kedar.Mhaswade@sun.com">Kedar Mhaswade</a>
 * @since S1AS8.0
 * @version $Revision: 1.4 $
 */
public interface GFJTAStats extends JTAStats {

	/** Returns the IDs of the transactions that are currently active, as a StrignStatistic. 
	 * An active transaction is same as an in-flight transaction. Every such transaction can be rolled back after
	 * freezing the transaction service.
	 * @see org.glassfish.j2ee.statistics.JTAStats#getActiveCount
	 * @return		a comma separated String of IDs
	 */
	public StringStatistic getActiveIds();
	
	/** Returns the current state of the transaction service as a StringStatistic.
	 *
	 * @return		String representing the current state of the transaction service.
	 */
	public StringStatistic getState();
	
	/** Freezes the transaction subsystem. This operation is advised before
	 * an attempt is made to rollback any transactions to avoid the possibility of 
         * transaction completion before the rollback request is issued. The transaction subsystem
	 * is expected be active before it can be frozen. Calling this methd on 
	 * an already frozen transaction subsystem has no effect.
	 */
	public void freeze();
	
	/** Unfreezes the transaction subsystem. It is required to unfreeze the 
	 * transaction subsystem after it is frozen earlier.
	 * Calling this method when system is not active, has no effect.
	 */
	public void unfreeze();
	
	/** 
	 * Rolls back a given transaction. It is advisable to call this method  
         * when the transaction subsystem is in a frozen state so that transactions
         * won't be completed before this request. It is left to implementation how
	 * the transactions are rolled back.
	 * @param	String representing the unique id of the transaction that
	 * needs to be rolled-back. Every transaction that can be rolled back 
	 * has to be an in flight transaction.
         * @return String contains the status of the rollback operation for the given txnId.
         * status contains "Rollback successful", 
         * "Rollback unsuccessful. Current Thread is not associated with the transaction",
         * "Rollback unsuccessful. Thread is not allowed to rollback the transaction",
         * "Rollback unsuccessful. Unexpected error condition encountered by Transaction Manager".
	 */	
	public String rollback(String txnId);
	
	
	/** 
	 * Rolls back the given transactions. It is advisable to call this method  
         * when the transaction subsystem is in a frozen state so that transactions
         * won't be completed before this request. It is left to implementation how
	 * the transactions are rolled back.
	 * @param	String array representing the unique ids of the transactions that
	 * need to be frozen. Every transaction that can be rolled back has to be an
	 * in flight transaction.
	 * @return	String[] containing the status for the given transaction ids.
         * status contains "Successful","Invalid transaction Id or Transaction is over",
         * "Rollback unsuccessful. Current Thread is not associated with the transaction",
         * "Rollback unsuccessful. Unexpected error condition encountered by Transaction Manager".
	
	public String[] rollback(String[] txnIds);
	 */

        // To be used for GUI ... 
        public List<Map<String, String>> listActiveTransactions();

	/**
	 * Utility method to find out if in place recovery is required.
	 * @return true if the recovery is required, else false
	 */
	public Boolean isRecoveryRequired();
}

