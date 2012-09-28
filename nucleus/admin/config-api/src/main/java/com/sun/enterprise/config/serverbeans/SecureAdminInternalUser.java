/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import java.beans.PropertyVetoException;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.config.support.CreationDecorator;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;

@Configured
/**
 * Records information about a username/password-alias pair to be
 * used for authentication internally among GlassFish processes (DAS to instance, for
 * example).
 * 
 * @author Tim Quinn
 */
public interface SecureAdminInternalUser extends ConfigBeanProxy {
    
    /**
     * Retrieves the username for this authorized internal admin user entry..
     *
     * @return {@link String } containing the username
     */
    @Param(primary=true)
    void setUsername(String value);
    
    /**
     * Sets the username for this authorized internal admin user entry.
     *
     * @param value username
     */
    @Attribute (required=true, key=true)
    String getUsername();

    /**
     * Retrieves the password alias for this authorized internal admin user entry..
     *
     * @return {@link String } containing the password alias
     */
    @Attribute (required=true)
    String getPasswordAlias();

    /**
     * Sets the password alias for this authorized internal admin user entry.
     *
     * @param value password alias
     */
    @Param(optional=false)
    void setPasswordAlias(String value);
    
    @Service
    @PerLookup
    public class CrDecorator implements CreationDecorator<SecureAdminInternalUser> {

        @Param(optional=false, primary=true)
        private String username;
        
        @Param(optional=false)
        private String passwordAlias;
        
        @Inject
        private SecureAdminHelper helper;
        
        @Override
        public void decorate(AdminCommandContext context, SecureAdminInternalUser instance) throws TransactionFailure, PropertyVetoException {
            
            try {
                helper.validateInternalUsernameAndPasswordAlias(
                        username, passwordAlias);
            } catch (Exception ex) {
                throw new TransactionFailure("create", ex);
            }
            instance.setUsername(username);
            instance.setPasswordAlias(passwordAlias);
        }
        
    }
}
