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

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.beans.PropertyVetoException;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.config.Named;
import org.glassfish.config.support.CreationDecorator;
import org.glassfish.config.support.CrudResolver;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;


@Configured
/**
 * Represents a security Principal, identified using an SSL cert, that is
 * authorized to perform admin operations. Used both to identify the DAS and instances
 * to each other and also for any end-user cert that should be accepted as
 * authorization for admin operations. 
 *
 */
public interface SecureAdminPrincipal extends ConfigBeanProxy {

    /**
     * Sets the DN of the SecureAdminPrincipal
     *
     * @param dn the DN
     */
    @Param(primary=true)
    public void setDn(String dn);
    
    /**
     * Gets the distinguished name for this SecureAdminPrincipal
     *
     * @return {@link String } containing the DN
     */
    @Attribute(key=true)
    String getDn();
    

    /**
     * Invoked during creation of a new SecureAdminPrincipal.
     */
    @Service
    @PerLookup
    public static class CrDecorator implements CreationDecorator<SecureAdminPrincipal> {
        
        
        @Inject
        //@Named(CREATION_DECORATOR_NAME)
        private SecureAdminHelper helper;
        
        @Param(optional=false, name="value", primary=true)
        private String value;
        
        @Param(optional=true, name="alias", defaultValue="false")
        private boolean isAlias = true;
        
        @Override
        public void decorate(AdminCommandContext context, SecureAdminPrincipal instance) throws TransactionFailure, PropertyVetoException {
            try {
                /*
                 * The user might have specified an alias, so delegate to the
                 * helper to return the DN for that alias (or the DN if that's
                 * what the user specified).
                 */
                instance.setDn(helper.getDN(value, isAlias));
            } catch (Exception ex) {
                throw new TransactionFailure("create", ex);
            }
        }
    }
    
    /**
     * Resolves using the type and any name, with no restrictions on the name and
     * with an optional mapping from a cert alias to the name.
     * <p>
     * The similar {@link TypeAndNameResolver} restricts the name to one that excludes 
     * commas, because TypeAndNameResolver uses habitat.getComponent which 
     * (ultimately) uses habitat.getInhabitantByContract which splits the name using
     * a comma to get a list of names to try to match against.
     * <p>
     * In some cases the name might actually contain a comma, so this resolver
     * supports those cases.
     * <p>
     * This resolver also allows the caller to specify an alias instead of the 
     * name (the DN) itself, in which case the resolver maps the alias to the 
     * corresponding cert's DN and uses that as the name.
     * 
     * @author Tim Quinn
     */

    @Service
    @PerLookup
    public static class Resolver implements CrudResolver {

        @Param(primary = true)
        private String value;

        @Param(optional=true, name="alias", defaultValue="false")
        private boolean isAlias = true;

        @Inject
        ServiceLocator habitat;

        @Inject
        private SecureAdminHelper helper;

        final protected static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SecureAdminPrincipal.class);

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type) {
            /*
             * First, convert the alias to the DN (if the name is an alias).
             */
            try {
                value = helper.getDN(value, isAlias);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            if ( ! SecureAdminPrincipal.class.isAssignableFrom(type)) { 
                final String msg = localStrings.getLocalString(SecureAdminPrincipal.class,
                        "SecureAdminPrincipalResolver.configTypeNotNamed",
                        "Config type {0} must extend {1} but does not", type.getSimpleName(), Named.class.getName());
                throw new IllegalArgumentException(msg);
            }

            /*
             * Look among all instances of this contract type for a match on the
             * full name.
             */
            for (T candidate : habitat.<T>getAllServices(type)) {
                if (value.equals(((SecureAdminPrincipal) candidate).getDn())) {
                    return candidate;
                }
            }
            String msg = localStrings.getLocalString(SecureAdminPrincipal.class,
                        "SecureAdminPrincipalResolver.target_object_not_found",
                        "Cannot find a {0} with a name {1}", type.getSimpleName(), value);
            throw new RuntimeException(msg);
        }
    }
}    
