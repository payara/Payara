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

package org.glassfish.ejb.deployment.descriptor;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.deployment.common.Descriptor;

/**
 * This descriptor represents a specification of a transactional behavior.
 * 
 * @author Danny Coward
 */

public final class ContainerTransaction extends Descriptor {
    private String transactionAttribute;
    /** Transactions are not supported. */
    public static final String NOT_SUPPORTED = "NotSupported";
    /** Transactions need support. */
    public static final String SUPPORTS = "Supports";
    /** A transaction is required. */
    public static final String REQUIRED = "Required";
    /** A new transaction must be created. */
    public static final String REQUIRES_NEW = "RequiresNew";
    /** Transaction is mandatory.*/
    public static final String MANDATORY = "Mandatory";
    /** Never supply a transaction. */
    public static final String NEVER = "Never";
    private static final LocalStringManagerImpl localStrings =
	    new LocalStringManagerImpl(ContainerTransaction.class);
    
    /**
     * Copy constructor.
     */
    public ContainerTransaction(ContainerTransaction other) {
	if (other != null) {
	    this.transactionAttribute = other.transactionAttribute;
	    this.setDescription(other.getDescription());
	}
    }
    
    /**
     * Create a new transaction descriptor with the given attribute. Throws 
     * an IllegalArgumentException if the attribute is not an allowed type. 
     * The allowed types are enumeration ny this class.
     * @param transactionAttribute .
     * @param description .
     */
    public ContainerTransaction(String transactionAttribute, 
				String description) {
	super("a Container Transaction", description);
	boolean isValidAttribute = (NOT_SUPPORTED.equals(transactionAttribute)
	    || SUPPORTS.equals(transactionAttribute)
		|| REQUIRED.equals(transactionAttribute)
		    || REQUIRED.equals(transactionAttribute)
			|| REQUIRES_NEW.equals(transactionAttribute)
			    || MANDATORY.equals(transactionAttribute)
				|| NEVER.equals(transactionAttribute) );
	if (!isValidAttribute && this.isBoundsChecking()) {
	    throw new IllegalArgumentException(localStrings.getLocalString(
			"enterprise.deployment.exceptionunknowncontainertxtype",
			"Unknown ContainerTransaction type: {0}", 
			new Object[] {transactionAttribute}));
	} else {
	    this.transactionAttribute = transactionAttribute;
	}
    }
    
    /**
     * The transaction attribute that I specify.
     * @return the transaction attribute.
     */
    public String getTransactionAttribute() {
	return this.transactionAttribute;
    }
    
    /** 
     * Equality iff the other object is another container transaction with the 
     * same transaction attribute.
     * @return true if the objects are equal, false otherwise.
     */
    public boolean equals(Object other) {
	if (other != null && other instanceof ContainerTransaction) {
	    ContainerTransaction otherContainerTransaction = 
			    (ContainerTransaction) other;
	    if (otherContainerTransaction.getTransactionAttribute().equals(
					this.getTransactionAttribute())) {
		return true;
	    }
	}
	return false;
    }

    public int hashCode() {
        int result = 17;
        result = 37*result + getTransactionAttribute().hashCode();
        return result;
    }

    
    /**
     * Returns a formatted String representing my state.
     */
    public void print(StringBuffer toStringBuffer) {
	toStringBuffer.append("Container Transaction: ").append(this.getTransactionAttribute()).append("@").append(this.getDescription());
    }
}

