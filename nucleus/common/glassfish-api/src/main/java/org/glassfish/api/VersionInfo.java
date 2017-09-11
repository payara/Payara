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
package org.glassfish.api;

import org.jvnet.hk2.annotations.Contract;

/**
 * Service interface to define the version info for an installed product.
 *
 */
@Contract
public interface VersionInfo {

    /**
     * Returns the short name of the product. E.g. Payara.
     * @return The product name.
     */
    public String getAbbreviatedProductName();
    
    /**
     * Returns the any prefixes to the products version. E.g. Alpha.
     * @return The product version prefix.
     */
    public String getVersionPrefix();
    
    /**
     * Returns the major version of the product. E.g. for Payara 5 the major version is 5.
     * @return The major version of the product.
     */
    public String getMajorVersion();
    
    /**
     * Returns the minor version of the product. E.g. for Payara 5.1 the minor version is 1.
     * @return The minor version of the product.
     */
    public String getMinorVersion();
    
    /**
     * Returns the Payara version. E.g. for Payara 5.1.181 the Payara version is 181.
     * @return The Payara version.
     */
    public String getPayaraVersion();

}
