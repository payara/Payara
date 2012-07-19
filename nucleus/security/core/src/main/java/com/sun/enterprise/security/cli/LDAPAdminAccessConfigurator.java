/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.cli;

//import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.ldap.LDAPRealm;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.internal.api.Target;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import javax.naming.AuthenticationNotSupportedException;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import org.glassfish.hk2.api.PerLookup;

/**  A convenience command to configure LDAP for administration. There are several properties and attributes that
 *   user needs to remember and that's rather user unfriendly. That's why this command is being developed.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @since GlassFish V3
 */
@Service(name="configure-ldap-for-admin")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="configure-ldap-for-admin", 
        description="configure-ldap-for-admin")
})
public class LDAPAdminAccessConfigurator implements AdminCommand {

    @Param (name="basedn", shortName="b", optional=false)
    public volatile String basedn;

    @Param(name="url", optional=true)
    public volatile String url = "ldap://localhost:389"; // the default port for LDAP on localhost


    @Param(name="ldap-group", shortName="g", optional=true)
    public volatile String ldapGroupName;
    
    @Inject
    Target targetService;
    
    @Inject
    private ConfigBeansUtilities configBeansUtilities;

    //TODO: not sure what to do with --target here
    @Param(name = "target", optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    private final static String ADMIN_SERVER = "server"; //this needs to be at central place, oh well
    private static final StringManager lsm = StringManager.getManager(LDAPAdminAccessConfigurator.class);
    private static final String DIR_P    = "directory";
    private static final String BASEDN_P = "base-dn";
    private static final String JAAS_P   = "jaas-context";
    private static final String JAAS_V   = "ldapRealm";
    public static final String LDAP_SOCKET_FACTORY = "java.naming.ldap.factory.socket";
    public static final String DEFAULT_SSL_LDAP_SOCKET_FACTORY = "com.sun.enterprise.security.auth.realm.ldap.CustomSocketFactory";
    public static final String LDAPS_URL = "ldaps://";

    private static final Logger logger = LogDomains.getLogger(LDAPAdminAccessConfigurator.class, LogDomains.SECURITY_LOGGER);


    /** Field denoting the name of the realm used for administration. This is fixed in entire of v3. Note that
     *  the same name is used in admin GUI's web.xml and sun-web.xml. The name of the realm is the key, the
     *  underlying backend (LDAP, File, Database) can change.
     */
    public static final String FIXED_ADMIN_REALM_NAME = "admin-realm";
    public static final String ORIG_ADMIN_REALM_NAME  = "admin-realm-original";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport rep = context.getActionReport();
        StringBuilder sb = new StringBuilder();
        if(url != null) {
            if (!url.startsWith("ldap://") && !url.startsWith("ldaps://")) {
                url = "ldap://" + url;        //it's ok to accept just host:port
            }
        }
        if (!pingLDAP(sb)) {
            rep.setMessage(sb.toString());
            rep.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            configure(sb);
            //Realm.getInstance(FIXED_ADMIN_REALM_NAME).refresh();
            rep.setMessage(sb.toString());
            rep.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch(TransactionFailure tf) {
            rep.setMessage(tf.getMessage());
            rep.setActionExitCode(ActionReport.ExitCode.FAILURE);
        } catch (PropertyVetoException e) {
            rep.setMessage(e.getMessage());
            rep.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
/*
        catch (NoSuchRealmException e) {
            ActionReport ar = rep.addSubActionsReport();
            ar.setMessage(lsm.getString("realm.not.refreshed"));
            ar.setActionExitCode(ActionReport.ExitCode.WARNING);
        } catch (BadRealmException e) {
            ActionReport ar = rep.addSubActionsReport();
            ar.setMessage(lsm.getString("realm.not.refreshed"));
            ar.setActionExitCode(ActionReport.ExitCode.WARNING);
        }
*/
    }

    private void configure(StringBuilder sb) throws TransactionFailure, PropertyVetoException {
        Server s = configBeansUtilities.getServerNamed(ADMIN_SERVER);
        String ac = s.getConfigRef();
        Config asc = targetService.getConfig(ac);

        //following things should happen transactionally - TODO replace SingleConfigCode by ConfigCode ...
        //createBackupRealm(sb, getAdminRealm(asc.getSecurityService()), getNewRealmName(asc.getSecurityService()));
        deleteRealm(asc.getSecurityService(), sb);
        createRealm(asc.getSecurityService(), sb);
        configureAdminService(asc.getAdminService());
        //configure(asc.getSecurityService(), asc.getAdminService(), sb);
    }

/*    private String getNewRealmName(SecurityService ss) {
        List<AuthRealm> realms = ss.getAuthRealm();
        String pref = ORIG_ADMIN_REALM_NAME + "-";
        int index = 0;  //last one
        for (AuthRealm realm : realms) {
            if (realm.getName().indexOf(pref) >= 0) {
                index = Integer.parseInt(realm.getName().substring(pref.length()));
            }
        }
        return pref + (index+1);
    }*/

    private AuthRealm getAdminRealm(SecurityService ss) {
        List<AuthRealm> realms = ss.getAuthRealm();
        for (AuthRealm realm : realms) {
            if (FIXED_ADMIN_REALM_NAME.equals(realm.getName()))
                return realm;
        }
        return null;  //unlikely - represents an assertion
    }

    private void configureAdminService(AdminService as) throws PropertyVetoException, TransactionFailure {
        SingleConfigCode<AdminService> scc = new SingleConfigCode<AdminService>() {
            @Override
            public Object run(AdminService as) {
                try {
                    as.setAuthRealmName(FIXED_ADMIN_REALM_NAME);  //just in case ...
                    return as;
                } catch(Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };
        ConfigSupport.apply(scc, as);
    }

    private void createRealm(SecurityService ss, final StringBuilder sb) throws TransactionFailure {
        SingleConfigCode<SecurityService> scc = new SingleConfigCode<SecurityService>() {
            @Override
            public Object run(SecurityService ss) throws PropertyVetoException, TransactionFailure {
                AuthRealm ldapr = createLDAPRealm(ss);
                ss.getAuthRealm().add(ldapr);
                appendNL(sb,lsm.getString("ldap.realm.setup", FIXED_ADMIN_REALM_NAME));
                return true;
            }
        };
        ConfigSupport.apply(scc, ss);
    }

    // delete and create a new realm to replace it in a single transaction
    private void deleteRealm(SecurityService ss, final StringBuilder sb) throws TransactionFailure {
        SingleConfigCode<SecurityService> scc = new SingleConfigCode<SecurityService>() {
            @Override
            public Object run(SecurityService ss) throws PropertyVetoException, TransactionFailure {
                AuthRealm oldAdminRealm = getAdminRealm(ss);
                ss.getAuthRealm().remove(oldAdminRealm);
                appendNL(sb,"...");
                //AuthRealm ldapr = createLDAPRealm(ss);
                //ss.getAuthRealm().add(ldapr);
                //appendNL(sb,lsm.getString("ldap.realm.setup", FIXED_ADMIN_REALM_NAME));
                return true;
            }
        };
        ConfigSupport.apply(scc, ss);
    }

    // this had been called renameRealm, but in the SecurityConfigListener, the method authRealmUpdated actually does a create...
/*    private void createBackupRealm(final StringBuilder sb, AuthRealm realm, final String to) throws PropertyVetoException, TransactionFailure {
        SingleConfigCode<AuthRealm> scc = new SingleConfigCode<AuthRealm>() {
            @Override
            public Object run(AuthRealm realm) throws PropertyVetoException, TransactionFailure {
                appendNL(sb, lsm.getString("config.to.ldap", FIXED_ADMIN_REALM_NAME, to));
                realm.setName(to);
                return realm;
            }
        };
        ConfigSupport.apply(scc, realm);
    }*/

    private AuthRealm createLDAPRealm(SecurityService ss) throws TransactionFailure, PropertyVetoException {
        AuthRealm ar = ss.createChild(AuthRealm.class);
        ar.setClassname(LDAPRealm.class.getName());
        ar.setName(FIXED_ADMIN_REALM_NAME);
        List<Property> props = ar.getProperty();

        Property p = ar.createChild(Property.class);
        p.setName(DIR_P);
        p.setValue(url);
        props.add(p);

        p = ar.createChild(Property.class);
        p.setName(BASEDN_P);
        p.setValue(basedn);
        props.add(p);

        p = ar.createChild(Property.class);
        p.setName(JAAS_P);
        p.setValue(JAAS_V);
        props.add(p);

        if (ldapGroupName!= null) {
            p = ar.createChild(Property.class);
            p.setName(Realm.PARAM_GROUP_MAPPING);
            p.setValue(ldapGroupName +"->asadmin"); //appears as gfdomain1->asadmin in domain.xml
            props.add(p);
        }
        
        return ar;
    }

    private boolean pingLDAP(StringBuilder sb) {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        
        if (url != null && url.startsWith(LDAPS_URL)) {
            env.put(LDAP_SOCKET_FACTORY,
                    DEFAULT_SSL_LDAP_SOCKET_FACTORY);
        }
        try {
            new InitialContext(env);
            appendNL(sb,lsm.getString("ldap.ok", url));
            return true;
        } catch (AuthenticationNotSupportedException anse) {
            //CR 6944776
            //If the server throws this error, it is up
            //and is configured with Anonymous bind disabled.
            //Ignore this error while configuring ldap for admin
            appendNL(sb,lsm.getString("ldap.ok", url));
            return true;
        } catch(Exception e) {
            appendNL(sb,lsm.getString("ldap.na", url, e.getClass().getName(), e.getMessage()));
            logger.info(StringUtils.getStackTrace(e));
            return false;
        }
    }

    private static void appendNL(StringBuilder sb, String s) {
        sb.append(s).append("%%%EOL%%%");
    }
}
