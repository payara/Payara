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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017-2024] [Payara Foundation and/or its affiliates]

package org.glassfish.weld.connector;

import java.beans.PropertyVetoException;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

/**
 * @author <a href="mailto:j.j.snyder@oracle.com">JJ Snyder</a>
 * @author Gaurav Gupta
 */
@Configured
public interface CDIService extends ConfigExtension {
    /**
     * Gets the value of the enableImplicitCdi property.
     *
     * @return The value of the enableImplicitCdi property.
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getEnableImplicitCdi();

    /**
     * Sets the value of the enableImplicitCdi property.
     *
     * @param value allowed object is {@link String }
     */
    void setEnableImplicitCdi(String value);
    
    /**
     * Gets the value of the enableConcurrentDeployment property.
     *
     * @return The value of the enableImplicitCdi property.
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getEnableConcurrentDeployment();

    /**
     * Sets the value of the enableConcurrentDeployment property.
     *
     * @param value allowed object is {@link String }
     */
    void setEnableConcurrentDeployment(String value);

    /**
     * Gets the value of the preloader threads property.
     *
     * @return The value of the enableImplicitCdi property.
     */
    @Attribute (defaultValue="0", dataType=Integer.class)
    String getPreLoaderThreadPoolSize();

    /**
     * Sets the value of the enableConcurrentDeployment property.
     *
     * @param value allowed object is {@link String }
     */
    void setPreLoaderThreadPoolSize(String value);


}
