/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.List;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.Create;
import org.glassfish.config.support.Delete;
import org.glassfish.config.support.Listing;
import org.glassfish.config.support.TypeAndNameResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Transaction;



@Configured
/**
 * Represents the admin security settings for the domain.
 *
 */
public interface SecureAdmin extends ConfigBeanProxy {

    @Element
    @Create(value="enable-secure-admin-principal", 
            decorator=SecureAdminPrincipal.CrDecorator.class, 
            i18n=@I18n("enable.secure.admin.principal.command"),
            cluster=@org.glassfish.api.admin.ExecuteOn(
                value = {RuntimeType.DAS,RuntimeType.INSTANCE}))
    @Delete(value="disable-secure-admin-principal", 
            resolver=SecureAdminPrincipal.Resolver.class, 
            i18n=@I18n("disable.secure.admin.principal.command"),
            cluster=@org.glassfish.api.admin.ExecuteOn(
                value = {RuntimeType.DAS,RuntimeType.INSTANCE}))
    @Listing(value="list-secure-admin-principals", i18n=@I18n("list.secure.admin.principals.command"))
    public List<SecureAdminPrincipal> getSecureAdminPrincipal();
    
    @Element
    @Create(value="enable-secure-admin-internal-user", 
            decorator=SecureAdminInternalUser.CrDecorator.class, 
            i18n=@I18n("enable.secure.admin.internal.user.command"),
            cluster=@org.glassfish.api.admin.ExecuteOn(
                value = {RuntimeType.DAS,RuntimeType.INSTANCE}))
    @Delete(value="disable-secure-admin-internal-user", 
            resolver=TypeAndNameResolver.class, 
            i18n=@I18n("disable.secure.admin.internal.user.command"),
            cluster=@org.glassfish.api.admin.ExecuteOn(
                value = {RuntimeType.DAS,RuntimeType.INSTANCE}))
    @Listing(value="list-secure-admin-internal-users", i18n=@I18n("list.secure.admin.internal.user.command"))
    public List<SecureAdminInternalUser> getSecureAdminInternalUser();

    /**
     * Gets whether admin security is turned on.
     *
     * @return {@link String } containing the type
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets whether admin security is turned on.
     *
     * @param value whether admin security should be on or off ("true" or "false")
     */
    void setEnabled(String value);

    @Attribute (defaultValue=Util.ADMIN_INDICATOR_DEFAULT_VALUE)
    String getSpecialAdminIndicator();

    void setSpecialAdminIndicator(String value);

    @Attribute(defaultValue=Duck.DEFAULT_ADMIN_ALIAS)
    String dasAlias();

    void setDasAlias(String alias);

    @Attribute(defaultValue=Duck.DEFAULT_INSTANCE_ALIAS)
    String instanceAlias();

    void setInstanceAlias(String alias);

    /**
     * Returns the SecureAdminPrincipal corresponding to the Principal the
     * instances use to authenticate themselves using SSL/TLS
     * @return the SecureAdminPrincipal for the instances
     */
    @DuckTyped
    String getInstanceAlias();

    @DuckTyped
    String getDasAlias();

    @DuckTyped
    boolean isEnabled();

    class Duck {

        public final static String DEFAULT_INSTANCE_ALIAS = "glassfish-instance";
        public final static String DEFAULT_ADMIN_ALIAS = "s1as";

        public static String getInstanceAlias(final SecureAdmin secureAdmin) {
            return secureAdmin.instanceAlias();
        }

        public static String getDasAlias(final SecureAdmin secureAdmin) {
            return secureAdmin.dasAlias();
        }
    }
    
    public static class Util {
        
        public static final String ADMIN_INDICATOR_HEADER_NAME = "X-GlassFish-admin";
        public   static final String ADMIN_INDICATOR_DEFAULT_VALUE = "true";
        public static final String ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME = "X-GlassFish-authToken";
        
        private static volatile SecureAdminHelper _secureAdminHelper = null;

        
        /**
         * Reports whether secure admin is enabled.
         * @param secureAdmin the SecureAdmin, typically returned from domain.getSecureAdmin()
         * @return true if secure admin is enabled; false otherwise
         */
        public static boolean isEnabled(final SecureAdmin secureAdmin) {
            return (secureAdmin != null && Boolean.parseBoolean(secureAdmin.getEnabled()));
        }

        /**
         * Returns the configured (which could be the default) value for the
         * special admin indicator.
         * @param secureAdmin the SecureAdmin, typically returned from domain.getSecureAdmin()
         * @return the current value for the admin indicator
         */
        public static String configuredAdminIndicator(final SecureAdmin secureAdmin) {
            return (secureAdmin == null ? ADMIN_INDICATOR_DEFAULT_VALUE : secureAdmin.getSpecialAdminIndicator());
            }

        public static String DASAlias(final SecureAdmin secureAdmin) {
            return (secureAdmin == null) ? Duck.DEFAULT_ADMIN_ALIAS :
                secureAdmin.getDasAlias();
        }

        public static String instanceAlias(final SecureAdmin secureAdmin) {
            return (secureAdmin == null) ? Duck.DEFAULT_INSTANCE_ALIAS :
                secureAdmin.getInstanceAlias();
        }
        
        
                
        
        public static List<SecureAdminInternalUser> secureAdminInternalUsers(final SecureAdmin secureAdmin) {
            return (secureAdmin == null) ? Collections.EMPTY_LIST : secureAdmin.getSecureAdminInternalUser();
        }
        
        public static SecureAdminInternalUser secureAdminInternalUser(final SecureAdmin secureAdmin) {
            final List<SecureAdminInternalUser> secureAdminUsers = secureAdminInternalUsers(secureAdmin);
            return (secureAdminUsers.isEmpty() ? null : secureAdminUsers.get(0));
        }
        
        public static boolean isUsingUsernamePasswordAuth(final SecureAdmin secureAdmin) {
            return ! secureAdminInternalUsers(secureAdmin).isEmpty();
        }
        
        public static List<SecureAdminPrincipal> secureAdminPrincipals(
                final SecureAdmin secureAdmin,
                final ServiceLocator habitat) {
            List<SecureAdminPrincipal> result = Collections.EMPTY_LIST;
            if (secureAdmin != null) {
                result = secureAdmin.getSecureAdminPrincipal();
                if (result.isEmpty()) {
                    try{
                        final Transaction t = new Transaction();
                        final SecureAdmin secureAdmin_w = t.enroll(secureAdmin);
                        result = secureAdmin_w.getSecureAdminPrincipal();
                        final SecureAdminPrincipal dasPrincipal = 
                            secureAdmin_w.createChild(SecureAdminPrincipal.class);
                        dasPrincipal.setDn(secureAdminHelper(habitat).getDN(secureAdmin.dasAlias(), true));
                        result.add(dasPrincipal);

                        final SecureAdminPrincipal instancePrincipal =
                                secureAdmin_w.createChild(SecureAdminPrincipal.class);
                        instancePrincipal.setDn(secureAdminHelper(habitat).getDN(secureAdmin.instanceAlias(), true));
                        result.add(instancePrincipal);
                        t.commit();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return result;
        }
        
        private static synchronized SecureAdminHelper secureAdminHelper(final ServiceLocator habitat) {
            if (_secureAdminHelper == null) {
                _secureAdminHelper = habitat.getService(SecureAdminHelper.class);
            }
            return _secureAdminHelper;
        }
    }
}


