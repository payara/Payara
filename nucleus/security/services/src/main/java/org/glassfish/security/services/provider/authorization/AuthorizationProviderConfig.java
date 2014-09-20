/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.provider.authorization;

import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.glassfish.security.services.config.SecurityProviderConfig;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import com.sun.enterprise.config.serverbeans.customvalidators.JavaClassName;


@Configured
public interface AuthorizationProviderConfig extends SecurityProviderConfig, PropertyBag {

    
    /**
     * Gets the class name of the authorization provider.
     */
    @Attribute(required=false)
    @NotNull
    @JavaClassName
    public String getProviderClass();
    public void setProviderClass(String value) throws PropertyVetoException;
    
    /**
     * Configuration parameter indicating if the provider support policy deploy or not
     * @return true support policy deploy
     */
    @Attribute(defaultValue = "true")
    boolean getSupportPolicyDeploy();
    void setSupportPolicyDeploy(boolean value) throws PropertyVetoException;

    /**
     * configuration parameter to indicate the version of the provider
     * @return version of the provider
     */
    @Attribute(required=false)
    String getVersion();
    void setVersion(String value) throws PropertyVetoException;
    
    
    /**
     * Gets the properties of the LoginModule.
     */
    @Element
    List<Property> getProperty();
    
    /**
     * Gets the options of the LoginModule for use with JAAS Configuration.
     */
    @DuckTyped
    Map<String,?> getProviderOptions();

    class Duck {
        /**
         * Gets the options of the LoginModule for use with JAAS Configuration.
         */
        public static Map<String,?> getProviderOptions(AuthorizationProviderConfig config) {
            Map<String,String> providerOptions = new HashMap<String,String>();
            for (Property prop : config.getProperty()) {
                providerOptions.put(prop.getName(), prop.getValue());
            }
            return providerOptions;
        }
    }


}
