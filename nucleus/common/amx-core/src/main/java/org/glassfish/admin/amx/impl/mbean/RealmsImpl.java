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

package org.glassfish.admin.amx.impl.mbean;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import javax.management.ObjectName;

import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.StringUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.base.DomainRoot;

import org.glassfish.admin.amx.impl.util.ImplUtil;

import com.sun.enterprise.config.serverbeans.AuthRealm;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import org.jvnet.hk2.config.types.Property;

import org.glassfish.internal.api.Globals;
import com.sun.enterprise.security.auth.realm.RealmsManager;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.User;
import org.glassfish.admin.amx.base.Realms;
import org.glassfish.admin.amx.impl.mbean.AMXImplBase;
import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.api.admin.ServerEnvironment;

import com.sun.enterprise.security.auth.login.LoginContextDriver;
import org.glassfish.admin.amx.util.AMXLoggerInfo;


/**
    AMX Realms implementation.
    Note that realms don't load until {@link #loadRealms} is called.
 */
public final class RealmsImpl extends AMXImplBase
    // implements Realms
{
		public
	RealmsImpl( final ObjectName containerObjectName )
	{
        super( containerObjectName, Realms.class);
	}
    
    public static RealmsManager
    getRealmsManager()
    {
        final RealmsManager mgr = Globals.getDefaultHabitat().getService(RealmsManager.class);
        return mgr;
    }
    
    private volatile boolean    realmsLoaded = false;
    
    private SecurityService getSecurityService()
    {   
    	return InjectedValues.getInstance().getHabitat().getService(SecurityService.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
    }

    private List<AuthRealm>  getAuthRealms()
    {
        return getSecurityService().getAuthRealm();
    }
    
    /** realm names as found in configuration; some might be defective and unable to be loaded */
    private Set<String> getConfiguredRealmNames()
    {
    	Set<String> names = new HashSet<String>();
    	List<AuthRealm> realms = getAuthRealms();
    	for (AuthRealm realm : realms) {
    		names.add(realm.getName());
    	}
    	return names;
    }
    
        private synchronized void 
    loadRealms()
    {
        if ( realmsLoaded )
        {
            final Set<String> loaded = SetUtil.newStringSet( _getRealmNames() );
            if ( getConfiguredRealmNames().equals( loaded ) )
            {
                return;
            }
            // reload: there are new or different realms
            realmsLoaded = false;
        }
        
        _loadRealms();
    }
                                                                                                      
        private void
    _loadRealms()
    {
        if ( realmsLoaded ) throw new IllegalStateException();

        final List<AuthRealm> authRealms = getAuthRealms();
        
        final List<String> goodRealms = new ArrayList<String>();
        for( final AuthRealm authRealm : authRealms )
        {
            final List<Property> propList = authRealm.getProperty();
            final Properties props = new Properties();
            for (final Property p : propList )
            {
                props.setProperty( p.getName(), p.getValue() );
            }
            try
            {
                Realm.instantiate( authRealm.getName(), authRealm.getClassname(), props );
                goodRealms.add( authRealm.getName() );
            }
            catch( final Exception e )
            {
                AMXLoggerInfo.getLogger().log( Level.WARNING, AMXLoggerInfo.cantInstantiateRealm,
                        new Object[] {StringUtil.quote(authRealm), e.getLocalizedMessage()} );
            }
        }
        
        if ( goodRealms.size() != 0 )
        {
            final String goodRealm = goodRealms.iterator().next();
            try
            {
                final String defaultRealm = getSecurityService().getDefaultRealm();
                Realm.getInstance(defaultRealm);
                Realm.setDefaultRealm(defaultRealm);
            }
            catch (final Exception e)
            {
                AMXLoggerInfo.getLogger().log( Level.WARNING, AMXLoggerInfo.cantInstantiateRealm,
                        new Object[] {StringUtil.quote(goodRealm), e.getLocalizedMessage()} );
                Realm.setDefaultRealm(goodRealms.iterator().next());
            }
        }
        
        realmsLoaded = true;
    }
    
    
        private String[]
    _getRealmNames()
    {
        final List<String> items = ListUtil.newList( getRealmsManager().getRealmNames() );
        return CollectionUtil.toArray(items, String.class);
    }
    
    
    public String[]
    getRealmNames()
    {
        try
        {
            loadRealms();
            return _getRealmNames();
        }
        catch( final Exception e )
        {
            AMXLoggerInfo.getLogger().log( Level.WARNING, AMXLoggerInfo.cantGetRealmNames, 
                    e.getLocalizedMessage() );
            return new String[] {};
        }
    }

    
    public String[]
    getPredefinedAuthRealmClassNames()
    {
        final List<String> items = getRealmsManager().getPredefinedAuthRealmClassNames();
        return CollectionUtil.toArray(items, String.class);
    }
    
    
    public String getDefaultRealmName()
    {
        return getRealmsManager().getDefaultRealmName();
    }
    
    
    public void setDefaultRealmName(final String realmName)
    {
        getRealmsManager().setDefaultRealmName(realmName);
    }
        
        private Realm
    getRealm(final String realmName)
    {
        loadRealms();
        final Realm realm = getRealmsManager().getFromLoadedRealms(realmName);
        if ( realm == null )
        {
            throw new IllegalArgumentException( "No such realm: " + realmName );
        }
        return realm;
    }
    
    public void addUser(
        final String realmName,
        final String user,
        final String password,
        final String[] groupList )
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.addUser(user, password.toCharArray(), groupList);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public void updateUser(
        final String realmName,
        final String existingUser,
        final String newUser,
        final String password,
        final String[] groupList )
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.updateUser(existingUser, newUser, password.toCharArray(), groupList);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public void removeUser(final String realmName, final String user)
    {
        checkSupportsUserManagement(realmName);
        
        try
        {
            final Realm realm = getRealm(realmName);
            realm.removeUser(user);
            realm.persist();
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public boolean supportsUserManagement(final String realmName)
    {
        return getRealm(realmName).supportsUserManagement();
    }
    
        private void
    checkSupportsUserManagement(final String realmName)
    {
        if ( ! supportsUserManagement(realmName) )
        {
            throw new IllegalStateException( "Realm " + realmName + " does not support user management" );
        }
    }



    public String[] getUserNames(final String realmName)
    {
        try
        {
            final List<String> names = ListUtil.newList( getRealm(realmName).getUserNames() );
            return CollectionUtil.toArray(names, String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public String[] getGroupNames(final String realmName)
    {
        try
        {
            final List<String> names = ListUtil.newList( getRealm(realmName).getGroupNames() );
            return CollectionUtil.toArray(names, String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    public String[] getGroupNames(final String realmName, final String user)
    {
        try
        {
            return CollectionUtil.toArray(ListUtil.newList( getRealm(realmName).getGroupNames(user) ), String.class);
        }
        catch( final Exception e )
        {
            throw new RuntimeException(e);
        }
    }
    
    
    public Map<String,Object> getUserAttributes(final String realmName, final String username)
    {
        try
        {
            final User user = getRealm(realmName).getUser(username);
            final Map<String,Object> m = new HashMap<String,Object>();
            final List<String> attrNames = ListUtil.newList(user.getAttributeNames());
            for( final String attrName : attrNames ) 
            {
                m.put( attrName, user.getAttribute(attrName) );
            }
            return m;
        }
        catch( final Exception e )
        {
        throw new RuntimeException(e);
        }
    }
    
    private static final String ADMIN_REALM = "admin-realm";
    private static final String ANONYMOUS_USER = "anonymous";
    private static final String FILE_REALM_CLASSNAME = "com.sun.enterprise.security.auth.realm.file.FileRealm";
            

     public String getAnonymousUser() {
        final Domain domain = InjectedValues.getInstance().getHabitat().getService(Domain.class);
        final List<Config> configs = domain.getConfigs().getConfig();
        
        // find the ADMIN_REALM
        AuthRealm adminFileAuthRealm = null;
        for( final Config config : configs )
        {
            if ( config.getSecurityService() == null ) continue;
            
            for( final AuthRealm auth : config.getSecurityService().getAuthRealm() )
            {
                if ( auth.getName().equals(ADMIN_REALM) )
                {
                    adminFileAuthRealm = auth;
                    break;
                }
            } 
        }
        if (adminFileAuthRealm == null) {
            // There must always be an admin realm
            throw new IllegalStateException( "Cannot find admin realm" );
        }

        // Get FileRealm class name
        final String fileRealmClassName = adminFileAuthRealm.getClassname();
        if (fileRealmClassName != null && ! fileRealmClassName.equals(FILE_REALM_CLASSNAME)) {
            // This condition can arise if admin-realm is not a File realm. Then the API to extract
            // the anonymous user should be integrated for the logic below this line of code. for now,
            // we treat this as an error and instead of throwing exception return false;
            return null;
        }

        Property keyfileProp = adminFileAuthRealm.getProperty("file");
        if ( keyfileProp == null ) {
            throw new IllegalStateException( "Cannot find property 'file'" );
        }
        final String keyFile = keyfileProp.getValue();
        if (keyFile == null) {
            throw new IllegalStateException( "Cannot find key file" );
        }
        
        //System.out.println( "############### keyFile: " + keyFile);
        String user = null;
        final String[] usernames = getUserNames(adminFileAuthRealm.getName());
        if (usernames.length == 1)
        {
            try
            {
                InjectedValues.getInstance().getHabitat().getService(com.sun.enterprise.security.SecurityLifecycle.class);
                LoginContextDriver.login( usernames[0], new char[0], ADMIN_REALM);
                user = usernames[0];
            }
            catch( final Exception e )
            {
                //e.printStackTrace();
            }
        }
        
        return user;
    }

}























