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

import com.sun.enterprise.config.serverbeans.customvalidators.*;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import static org.glassfish.config.support.Constants.NAME_REGEX;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.beans.PropertyVetoException;
import java.util.List;

@Configured
@FileRealmPropertyCheck
@LDAPRealmPropertyCheck
@JDBCRealmPropertyCheck
@SolarisRealmPropertyCheck
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-auth-realm"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-auth-realm")
})
/**
 * The auth-realm element defines and configures one authentication realm. 
 * There must be at least one realm available for a server instance; 
 * any number can be configured, as desired.               
 * Authentication realms need provider-specific parameters which vary depending 
 * on what a particular implementation needs; these are defined as properties 
 * since they vary by provider and cannot be predicted for any custom or add-on 
 * providers.
 * For the default file provider, the param used is: file                     
 */
public interface AuthRealm extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the name property.
     * Defines the name of this realm
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    @Pattern(regexp=NAME_REGEX)
    String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the classname property.
     * Defines the java class which implements this realm
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    @JavaClassName
    String getClassname();

    /**
     * Sets the value of the classname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setClassname(String value) throws PropertyVetoException;

    @DuckTyped
    String getGroupMapping();

    class Duck {
        public static String getGroupMapping(AuthRealm me) {
            Property prop = me.getProperty("group-mapping"); //have to hard-code this, unfortunately :(
            if (prop != null)
                return prop.getValue();
            return null;
        }
    }
    /**
        Properties.
     */
@PropertiesDesc(
    props={
        @PropertyDesc(name="jaas-context",
            description="jaas-contextfile,jdbcSpecifies the JAAS (Java Authentication and Authorization Service) context"),
        @PropertyDesc(name="file", defaultValue="${com.sun.aas.instanceRoot}/config/keyfile",
            description="file realm. Specifies the file that stores user names, passwords, and group names."),
        @PropertyDesc(name="assign-groups",
            description="file, jdbc realms. Comma-separated list of group names."),
        @PropertyDesc(name="datasource-jndi",
            description="Specifies name of the jdbc-resource for the database"),
        @PropertyDesc(name="user-table",
            description="Specifies the name of the user table in the database"),
        @PropertyDesc(name="user-name-column",
            description="Specifies the name of the user name column in the database user table"),
        @PropertyDesc(name="password-column",
            description="Specifies the name of the password column in the database user table"),
        @PropertyDesc(name="group-table",
            description="Specifies the name of the group table in the database"),
        @PropertyDesc(name="group-name-column",
            description="Specifies the name of the group name column in the database user table"),
        @PropertyDesc(name="db-user",
            description="The database user name in the realm instead of that in the jdbc-connection-pool. " +
                "Prevents other applications from looking up the database, getting a connection, and browsing the user table"),
        @PropertyDesc(name="db-password",
            description="The database password in the realm instead of that in the jdbc-connection-pool. " +
                "Prevents other applications from looking up the database, getting a connection, and browsing the user table"),
        @PropertyDesc(name="digest-algorithm", defaultValue="MD5", values={"MD5", "none", "SHA"},
            description="Any algorithm supported in the JDK"),
        @PropertyDesc(name="encoding", values={"Hex", "Base64"},
            description="Specifies the encoding. If digest-algorithm is specified, the default is Hex, otherwise no encoding is specified"),
        @PropertyDesc(name="charset",
            description="Specifies the charset for the digest algorithm")
    }
    )
    @Element
    List<Property> getProperty();
}












