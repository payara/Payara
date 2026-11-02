/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Any object that want to be part of a configuration transaction 
 * should implement this interface.
 *
 * @author Jerome Dochez
 */
public interface Transactor {

	/**
	 * Enter a new Transaction, this method should return false if this object
	 * is already enlisted in another transaction, or cannot be enlisted with
	 * the passed transaction. If the object returns true, the object
	 * is enlisted in the passed transaction and cannot be enlisted in another 
	 * transaction until either commit or abort has been issued.
	 * 
	 * @param t the transaction to enlist with
	 * @return true if the enlisting with the passed transaction was accepted, 
	 * false otherwise
	 */
    public boolean join(Transaction t);
	
	/**
	 * Returns true of this Transaction can be committed on this object
	 *
	 * @param t is the transaction to commit, should be the same as the
	 * one passed during the join(Transaction t) call.
	 *
	 * @return true if the transaction committing would be successful
     * @throws TransactionFailure if the changes cannot be validated
	 */
    public boolean canCommit(Transaction t) throws TransactionFailure;

	/**
	 * Commit this Transaction.
	 *
	 * @param t the transaction commiting.
     * @return list of applied property changes
	 * @throws TransactionFailure if the transaction commit failed
	 */
    public List<PropertyChangeEvent> commit(Transaction t) throws TransactionFailure;

	/**
	 * Aborts this Transaction, reverting the state
	 
	 * @param t the aborting transaction
	 */
    public void abort(Transaction t);
}
