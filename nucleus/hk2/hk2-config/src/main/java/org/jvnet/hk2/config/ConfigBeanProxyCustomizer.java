/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Oracle and/or its affiliates. All rights reserved.
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

import org.jvnet.hk2.annotations.Contract;

/**
 * @author jwells
 *
 */
@Contract
public interface ConfigBeanProxyCustomizer {
    public static final String DEFAULT_IMPLEMENTATION = "system default";
    
    /**                      
     * Returns the parent element of this configuration element.
     *
     * It is possible to return a not null parent while the parent knows nothing of this
     * child element. This could happen when the child element was removed
     * from the configuration tree, yet it's parent would not have been reset.
     *
     * @return the parent configuration node.
     */
    public ConfigBeanProxy getParent(ConfigBeanProxy me);

    /**
     * Returns the typed parent element of this configuration element.
     *
     * It is possible to return a not null parent while the parent knows nothing of this
     * child element. This could happen when the child element was removed
     * from the configuration tree, yet it's parent would not have been reset.
     *
     * @param type parent's type
     * @return the parent configuration node.
     */
    public ConfigBeanProxy getParent(ConfigBeanProxy me, Class<?> type);

    /**
     * Creates a child element of this configuration element
     *
     * @param type the child element type
     * @return the newly created child instance
     * @throws TransactionFailure when called outside the boundaries of a transaction 
     */
    public ConfigBeanProxy createChild(ConfigBeanProxy me, Class<?> type);


    /**
     * Performs a deep copy of this configuration element and returns it.
     * The parent of this configuration must be locked in a transaction and the newly created
     * child will be automatically enrolled in the parent's transaction.
     *
     * @param parent the writable copy of the parent
     * @return a deep copy of itself.
     * @throws TransactionFailure if the transaction cannot be completed.
     */
    public ConfigBeanProxy deepCopy(ConfigBeanProxy me, ConfigBeanProxy parent);

}
