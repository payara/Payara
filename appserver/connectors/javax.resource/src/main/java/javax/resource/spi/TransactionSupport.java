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

package javax.resource.spi;

/** 
 * This interface may be optionally implemented by a 
 * <code>ManagedConnectionFactory</code> to provide its level of transaction
 * support at runtime.
 *
 * <p>When a <code>ManagedConnectionFactory</code> implements this interface,
 * the application server uses the <code>TransactionSupportLevel</code> returned
 * by getTransactionSupport() method and not the value specified in the 
 * resource adapter deployment descriptor or deployer configuration
 *
 * @since 1.6
 * @version Java EE Connector Architecture 1.6
 */
public interface TransactionSupport extends java.io.Serializable {

    /**
     * An enumerated type that represents the levels of transaction support
     * a resource adapter may support.
     *
     * @since 1.6
     * @version Java EE Connector Architecture 1.6
     */
    public enum TransactionSupportLevel {
        /**
         * The resource adapter supports neither resource manager nor JTA 
         * transactions.
         * @since 1.6
         */
        NoTransaction, 
        /**
         * The resource adapter supports resource manager local transactions 
         * by implementing the <code>LocalTransaction</code> interface.
         * @since 1.6
         */
        LocalTransaction, 
        /**
         * The resource adapter supports both resource manager local 
         * and JTA transactions by implementing the <code>LocalTransaction</code>
         * and <code>XAResource</code> interfaces.
         * @since 1.6
         */
        XATransaction 
    };

    /**
     * Get the level of transaction support, supported by the 
     * <code>ManagedConnectionFactory</code>. A resource adapter must always
     * return a level of transaction support whose ordinal value in
     * <code>TransactionSupportLevel</code> enum is equal to or lesser than
     * the resource adapter's transaction support classification.
     *
     * @since 1.6
     */
    public TransactionSupportLevel getTransactionSupport();
}
