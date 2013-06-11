/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.realm;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.util.IASSecurityException;
import java.io.*;
import java.util.*;
import com.sun.enterprise.util.*;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Contract;

/**
 * javadoc
 *
 * @see java.security.Principal
 *
 * @author Harish Prabandham
 * @author Harpreet Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 *
 */
@Contract
public abstract class Realm implements Comparable {

    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(Realm.class);

//    private static Hashtable loadedRealms = new Hashtable();
    private String myName;

    // Keep track of name of default realm. This is updated during startup
    // using value from server.xml
//    private static String defaultRealmName="default";
    
    // Keep a mapping from "default" to default realm (if no such named
    // realm is present) for the sake of all the hardcoded accesses to it.
    // This needs to be removed as part of RI security service cleanup.
    final static String RI_DEFAULT="default";

    // All realms have a set of properties from config file, consolidate.
    private Properties ctxProps;

    // for assign-groups
    private static final String PARAM_GROUPS = "assign-groups";
    private static final String GROUPS_SEP = ",";
    private List<String> assignGroups = null;
    public static final String PARAM_GROUP_MAPPING="group-mapping";
    protected GroupMapper groupMapper = null;
    private static RealmStatsProvider realmStatsProvier = null;
    private static final String DEFAULT_DIGEST_ALGORITHM = "default-digest-algorithm";
    private static final String DEFAULT_DEF_DIG_ALGO_VAL = "SHA-256";

    private static WeakReference<RealmsManager> realmsManager = new WeakReference<RealmsManager>(null);
    private String defaultDigestAlgorithm = null;

    protected static final Logger _logger = SecurityLoggerInfo.getLogger();

    
    /**
     * Returns the name of this realm.
     *
     * @return realm name.
     */
    public final String	getName() { 
	return myName; 
    }

    protected String getDefaultDigestAlgorithm() {
        return defaultDigestAlgorithm;
    }

    
    /**
     * Assigns the name of this realm, and stores it in the cache
     * of realms.  Used when initializing a newly created in-memory
     * realm object; if the realm already has a name, there is no
     * effect.
     *
     * @param name name to be assigned to this realm.
     */
    protected final void setName(String name) {
	if (myName != null) {
	    return;
	}
	myName = name;
    }

    
    /**
     * Returns the name of this realm.
     *
     * @return name of realm.
     */
    @Override
    public String  toString() { 
	return myName; 
    }

    
    /**
     * Compares a realm to another.  The comparison first considers the
     * authentication type, so that realms supporting the same kind of
     * user authentication are grouped together.  Then it compares realm
     * realm names.  Realms compare "before" other kinds of objects (i.e.
     * there's only a partial order defined, in the case that those other
     * objects compare themselves "before" a realm object).
     */
    public int compareTo (Object realm) {
	if (!(realm instanceof Realm)) {
	    return 1;
	}
	
	Realm 	r = (Realm) realm;
	String	str = r.getAuthType ();
	int	temp;
	
	if ((temp = getAuthType ().compareTo (str)) != 0) {
	    return temp;
	}

	str = r.getName ();
	return getName ().compareTo (str);
    }


    /**
     * Instantiate a Realm with the given name and properties using the
     * Class name given. This method is used by iAS and not RI.
     *
     * @param name Name of the new realm.
     * @param className Java Class name of the realm to create.
     * @param props Properties containing values of the Property element
     *     from server.xml
     * @returns Reference to the new Realm. The Realm class keeps an internal
     *     list of all instantiated realms.
     * @throws BadRealmException If the requested realm cannot be instantiated.
     *
     */
    public static synchronized Realm instantiate(String name, String className,
                                    Properties props)
        throws BadRealmException
    {
        //Register the realm provider
        registerRealmStatsProvier();
        
        Realm realmClass = _getInstance(name);
        if(realmClass == null) {
            realmClass = doInstantiate(name, className, props);
            RealmsManager mgr = getRealmsManager();
            mgr.putIntoLoadedRealms(name, realmClass);
        }
        return realmClass;
    }

    /**
     * Instantiate a Realm with the given name and properties using the
     * Class name given. This method is used by iAS and not RI.
     *
     * @param name Name of the new realm.
     * @param className Java Class name of the realm to create.
     * @param props Properties containing values of the Property element
     *     from server.xml
     * @param configName the config to which this realm belongs
     * @returns Reference to the new Realm. The Realm class keeps an internal
     *     list of all instantiated realms.
     * @throws BadRealmException If the requested realm cannot be instantiated.
     *
     */
    public static synchronized Realm instantiate(String name, String className,
                                    Properties props, String configName)
        throws BadRealmException
    {
        //Register the realm provider
        registerRealmStatsProvier();

        Realm realmClass = _getInstance(configName, name);
        if(realmClass == null) {
            realmClass = doInstantiate(name, className, props);
            RealmsManager mgr = getRealmsManager();
            mgr.putIntoLoadedRealms(configName, name, realmClass);
        }
        return realmClass;

    }

    private static void registerRealmStatsProvier() {
        if (realmStatsProvier == null) {
            getRealmStatsProvier();
            StatsProviderManager.register("security", PluginPoint.SERVER, "security/realm", realmStatsProvier);
        }
    }

    public static synchronized void getRealmStatsProvier(){
        if(realmStatsProvier == null){
            realmStatsProvier = new RealmStatsProvider();
        }
    }

    /**
     * Instantiate a Realm with the given name, loading properties from
     * the given file. This method is only used by RI and is not called
     * anywhere in iAS.
     * Note : this method stands unused in V3.1 but keeping it since it is a 
     * public method.
     * @deprecated
     * @param realmName Name of the new realm.
     * @param f File containing Properties for the new realm.
     */
    public static synchronized Realm instantiate(String realmName, File f)
    throws NoSuchRealmException, BadRealmException, FileNotFoundException
    {


        if (!f.exists() || !f.isFile()) {
            throw new FileNotFoundException ();
        }
        
        if(_getInstance(realmName) != null) {
            throw new BadRealmException(
                localStrings.getLocalString("realm.already_exists", 
                                            "This Realm already exists."));
        } 
        
        //
        // First load the description from properties.
        //
        InputStream in = null;
        Properties  props = new Properties();
        
        try{    
            in = new FileInputStream(f);
            props.load(in);
            //
            // Then instantiate and initialize, using the single mandatory
            // property ("classname").
            //
            String classname = props.getProperty("classname");
            assert (classname != null);

            return doInstantiate(realmName, classname, props);
        } catch (IOException e) {
            throw new BadRealmException(e.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch(Exception ex) {
                }
            }
        }
    }

    
    /**
     * Instantiates a Realm class of the given type and invokes its init()
     *
     */
    private static synchronized Realm doInstantiate(String name, String className,
                                       Properties props)
        throws BadRealmException
    {
        
        ServiceLocator habitat = Globals.getDefaultHabitat();
        RealmsManager mgr = null;
        try {
            mgr = getRealmsManager();
            Class realmClass = null;
            //try a HK2 route first
            Realm r = habitat.getService(Realm.class, name);
            if (r == null) {
                try {
                    //TODO: workaround here. Once fixed in V3 we should be able to use
                    //Context ClassLoader instead.
                    ClassLoaderHierarchy hierarchy =
                            habitat.getService(ClassLoaderHierarchy.class);
                    realmClass = hierarchy.getCommonClassLoader().loadClass(className);
                    Object obj = realmClass.newInstance();
                    r = (Realm) obj;
                } catch (ClassNotFoundException ex) {
                    realmClass = Class.forName(className);
                    Object obj = realmClass.newInstance();
                    r = (Realm) obj;
                }
            }

            r.setName(name);
            r.init(props);
            if (mgr == null) {
                throw new BadRealmException("Unable to locate RealmsManager Service");
            }
            _logger.log(Level.INFO, SecurityLoggerInfo.realmCreated ,new Object[]{name, className});
            return r;


        } catch (NoSuchRealmException ex) {
            throw new BadRealmException(ex);
        } catch (InstantiationException ex) {
            throw new BadRealmException(ex);
        } catch (IllegalAccessException ex) {
            throw new BadRealmException(ex);
        } catch (ClassNotFoundException ex) {
            throw new BadRealmException(ex);
        }
    }


    /**
     * Replace a Realm instance. Can be used by a Realm subclass to
     * replace a previously initialized instance of itself. Future
     * getInstance requests will then obtain the new instance.
     *
     * <P>Minimal error checking is done. The realm being replaced must
     * already exist (instantiate() was previously called), the new
     * instance must be fully initialized properly and it must of course
     * be of the same class as the previous instance.
     *
     * @param realm The new realm instance.
     * @param name The (previously instantiated) name for this realm.
     *
     */
    protected static synchronized void updateInstance(Realm realm, String name)
    {
        RealmsManager mgr = getRealmsManager();
        if (mgr == null) {
             throw new RuntimeException("Unable to locate RealmsManager Service");
        }
        
        Realm oldRealm = mgr.getFromLoadedRealms(name);
        if (!oldRealm.getClass().equals(realm.getClass())) {
            // would never happen unless bug in realm subclass
            throw new Error("Incompatible class "+realm.getClass()+
                            " in replacement realm "+name);
        }
        realm.setName(oldRealm.getName());
        mgr.putIntoLoadedRealms(name, realm);
        _logger.log(Level.INFO, SecurityLoggerInfo.realmUpdated, new Object[]{realm.getName()});
    }

    /**
     * Replace a Realm instance. Can be used by a Realm subclass to
     * replace a previously initialized instance of itself. Future
     * getInstance requests will then obtain the new instance.
     *
     * <P>Minimal error checking is done. The realm being replaced must
     * already exist (instantiate() was previously called), the new
     * instance must be fully initialized properly and it must of course
     * be of the same class as the previous instance.
     *
     * @param realm The new realm instance.
     * @param name The (previously instantiated) name for this realm.
     *
     */
    protected static synchronized void updateInstance(String configName, Realm realm, String name) {
        RealmsManager mgr = getRealmsManager();
        if (mgr == null) {
            throw new RuntimeException("Unable to locate RealmsManager Service");
        }

        Realm oldRealm = mgr.getFromLoadedRealms(configName, name);
        if (!oldRealm.getClass().equals(realm.getClass())) {
            // would never happen unless bug in realm subclass
            throw new Error("Incompatible class " + realm.getClass()
                    + " in replacement realm " + name);
        }
        realm.setName(oldRealm.getName());
        mgr.putIntoLoadedRealms(configName, name, realm);
        _logger.log(Level.INFO, SecurityLoggerInfo.realmUpdated, new Object[]{realm.getName()});
    }

    
    /**
     * Convenience method which returns the Realm object representing
     * the current default realm. Equivalent to
     * getInstance(getDefaultRealm()).
     * 
     * @return Realm representing default realm.
     * @exception NoSuchRealmException if default realm does not exist
     */
    public static synchronized Realm getDefaultInstance() throws NoSuchRealmException
    {
        return getInstance(getDefaultRealm());
    }

    
    /**
     * Returns the name of the default realm.
     *
     * @return Default realm name.
     *
     */
    public static synchronized String getDefaultRealm() {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr.getDefaultRealmName();
        } else {
            throw new RuntimeException("Unable to locate RealmsManager Service");
        }
    }

    
    /**
     * Sets the name of the default realm.
     *
     * @param realmName Name of realm to set as default.
     *
     */
    public static synchronized void setDefaultRealm(String realmName) {
        //defaultRealmName = realmName;
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
             mgr.setDefaultRealmName(realmName);
        } else  {
           throw new RuntimeException("Unable to locate RealmsManager Service");
        }
    }

    /**
     * Remove realm with given name from cache.
     * @param realmName
     * @exception NoSuchRealmException
     */
    public static synchronized void unloadInstance(String realmName) throws NoSuchRealmException {
        //make sure instance exist
        getInstance(realmName);
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
             mgr.removeFromLoadedRealms(realmName);
        } else {
           throw new RuntimeException("Unable to locate RealmsManager Service");
        }
        _logger.log(Level.INFO, SecurityLoggerInfo.realmDeleted, realmName);
    }

     /**
     * Remove realm with given name from cache.
     * @param realmName
     * @exception NoSuchRealmException
     */
    public static synchronized void unloadInstance(String configName, String realmName) throws NoSuchRealmException {
        //make sure instance exist
        //getInstance(configName, realmName);
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
             mgr.removeFromLoadedRealms(configName, realmName);
        } else {
           throw new RuntimeException("Unable to locate RealmsManager Service");
        }
        _logger.log(Level.INFO, SecurityLoggerInfo.realmDeleted, realmName);
    }



    /**
     * Set a realm property.
     *
     * @param name property name.
     * @param value property value.
     *
     */
    public synchronized void setProperty(String name, String value)
    {
        ctxProps.setProperty(name, value);
    }


    /**
     * Get a realm property.
     *
     * @param name property name.
     * @returns value.
     *
     */
    public synchronized String getProperty(String name)
    {
        return ctxProps.getProperty(name);
    }

    /**
     * Return properties of the realm.
     */
    protected synchronized Properties getProperties() {
        return ctxProps;
    }


    /**
     * Returns name of JAAS context used by this realm.
     *
     * <P>The JAAS context is defined in server.xml auth-realm element
     * associated with this realm. 
     *
     * @return String containing JAAS context name.
     *
     */
    public synchronized String getJAASContext()
    {
        return ctxProps.getProperty(IASRealm.JAAS_CONTEXT_PARAM);
    }
    
    
    /**
     * Returns the realm identified by the name which is passed
     * as a parameter.  This function knows about all the realms
     * which exist; it is not possible to store (or create) one
     * which is not accessible through this routine.
     *
     * @param name identifies the realm
     * @return the requested realm
     * @exception NoSuchRealmException if the realm is invalid
     * @exception BadRealmException if realm data structures are bad
     */
    public static synchronized Realm	getInstance(String name) throws NoSuchRealmException
    {
	Realm retval = _getInstance(name);

        if (retval == null) {
            throw new NoSuchRealmException(
                localStrings.getLocalString("realm.no_such_realm", 
                name + " realm does not exist.",
                new Object[] { name }));
        }
     
	return retval;
    }

    /**
     * Returns the realm identified by the name which is passed
     * as a parameter.  This function knows about all the realms
     * which exist; it is not possible to store (or create) one
     * which is not accessible through this routine.
     *
     * @param name identifies the realm
     * @return the requested realm
     * @exception NoSuchRealmException if the realm is invalid
     * @exception BadRealmException if realm data structures are bad
     */
    public static synchronized Realm getInstance(String configName, String name) throws NoSuchRealmException
    {
	Realm retval = _getInstance(configName, name);

        if (retval == null) {
            throw new NoSuchRealmException(
                localStrings.getLocalString("realm.no_such_realm",
                name + " realm does not exist.",
                new Object[] { name }));
        }

	return retval;
    }
    
    /**
     * This is a private method for getting realm instance.
     * If realm does not exist, then it will not return null rather than 
     * throw exception.
     * @param name identifies the realm
     * @return the requested realm
     */
    private static synchronized Realm _getInstance(String name) {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr._getInstance(name);
        } else  {
           throw new RuntimeException("Unable to locate RealmsManager Service");
        }
    }

    /**
     * This is a private method for getting realm instance.
     * If realm does not exist, then it will not return null rather than
     * throw exception.
     * @param name identifies the realm
     * @return the requested realm
     */
    private static synchronized Realm _getInstance(String configName, String name) {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr._getInstance(configName, name);
        } else  {
           throw new RuntimeException("Unable to locate RealmsManager Service");
        }
    }
    

    /**
     * Returns the names of accessible realms.
     * @return set of realm names
     */
    public static synchronized Enumeration	getRealmNames() {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr.getRealmNames();
        }
        throw new RuntimeException("Unable to locate RealmsManager Service");
    }


    /**
     * The default constructor creates a realm which will later
     * be initialized, either from properties or by deserializing.
     */
    protected Realm() {
        ctxProps = new Properties();
    }


    /**
     * Initialize a realm with some properties.  This can be used
     * when instantiating realms from their descriptions.  This
     * method may only be called a single time.  
     *
     * @param props initialization parameters used by this realm.
     * @exception BadRealmException if the configuration parameters
     *	identify a corrupt realm
     * @exception NoSuchRealmException if the configuration parameters
     *	specify a realm which doesn't exist
     */
    protected void init(Properties props)
            throws BadRealmException, NoSuchRealmException {
        String groupList = props.getProperty(PARAM_GROUPS);
        if (groupList != null && groupList.length() > 0) {
            this.setProperty(PARAM_GROUPS, groupList);
            assignGroups = new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(groupList, GROUPS_SEP);
            while (st.hasMoreTokens()) {
                String grp = (String)st.nextToken();
                if (!assignGroups.contains(grp)) {
                    assignGroups.add(grp);
                }
            }
        }
        String groupMapping = props.getProperty(PARAM_GROUP_MAPPING);
        if (groupMapping != null) {
            groupMapper = new GroupMapper();
            groupMapper.parse(groupMapping);
        }
        String defaultDigestAlgo = null;
        if(_getRealmsManager() != null) {
            defaultDigestAlgo = _getRealmsManager().getDefaultDigestAlgorithm();
        }       
       this.defaultDigestAlgorithm = (defaultDigestAlgo == null)?DEFAULT_DEF_DIG_ALGO_VAL:defaultDigestAlgo;       
    }

    private static synchronized  RealmsManager _getRealmsManager() {
        if (realmsManager.get() == null) {
            if(Globals.getDefaultHabitat() != null) {
                realmsManager = new WeakReference<RealmsManager>(Globals.get(RealmsManager.class));
            }
            else {
                return null;
            }
        }
        return realmsManager.get();
    }

    private static RealmsManager getRealmsManager() {
        if (realmsManager.get() != null) {
            return realmsManager.get();
        }
        return _getRealmsManager();
     }
    /**
     * Checks if the given realm name is loaded/valid.
     * @param String name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public static boolean isValidRealm(String name) {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr.isValidRealm(name);
        }
        throw new RuntimeException("Unable to locate RealmsManager Service");
    }

    /**
     * Checks if the given realm name is loaded/valid.
     * @param String name of the realm to check.
     * @return true if realm present, false otherwise.
     */
    public static boolean isValidRealm(String configName, String name) {
        RealmsManager mgr = getRealmsManager();
        if (mgr != null) {
            return mgr.isValidRealm(configName, name);
        }
        throw new RuntimeException("Unable to locate RealmsManager Service");
    }

    /**
     * Add assign groups to given Vector of groups.
     * To be used by getGroupNames.
     * @param grps
     */
    protected String[] addAssignGroups(String[] grps) {
        String[] resultGroups = grps;
        if (assignGroups != null && assignGroups.size() > 0) {
            List<String> groupList = new ArrayList<String>();
            if (grps != null &&  grps.length > 0) {
                for (String grp : grps) {
                    groupList.add(grp);
                }
            }
            
            for (String agrp : assignGroups) {
                if (!groupList.contains(agrp)) {
                    groupList.add(agrp);
                }
            }
            resultGroups = groupList.toArray(new String[groupList.size()]);
        }
        return resultGroups;
    }
    
    protected ArrayList<String> getMappedGroupNames(String group) {
        if (groupMapper != null) {
            ArrayList<String> result = new ArrayList<String>();
            groupMapper.getMappedGroups(group, result);
            return result;
        }
        return null;
    }
    
    //---[ Abstract methods ]------------------------------------------------

    
    /**
     * Returns a short (preferably less than fifteen characters) description
     * of the kind of authentication which is supported by this realm.
     *
     * @return description of the kind of authentication that is directly
     *	supported by this realm.
     */
    public abstract String getAuthType ();

    /**
     * Returns an AuthenticationHandler object which can be used to 
     * authenticate within this realm.
     *
     * @return An AuthenticationHandler object for this realm.
     */
    public abstract AuthenticationHandler getAuthenticationHandler ();

    /**
     * Returns names of all the users in this particular realm.
     *
     * @return enumeration of user names (strings)
     * @exception BadRealmException if realm data structures are bad
     */
    public abstract Enumeration	getUserNames() throws BadRealmException;

    /**
     * Returns the information recorded about a particular named user.
     *
     * @param name name of the user whose information is desired
     * @return the user object
     * @exception NoSuchUserException if the user doesn't exist
     * @exception BadRealmException if realm data structures are bad
     */
    public abstract User getUser(String name)
	throws NoSuchUserException, BadRealmException;

    /**
     * Returns names of all the groups in this particular realm.
     *
     * @return enumeration of group names (strings)
     * @exception BadRealmException if realm data structures are bad
     */
    public abstract Enumeration	getGroupNames()
	throws BadRealmException;

    /**
     * Returns the name of all the groups that this user belongs to
     * @param username name of the user in this realm whose group listing
     * is needed.
     * @return enumeration of group names (strings)
     * @exception InvalidOperationException thrown if the realm does not
     * support this operation - e.g. Certificate realm does not support this
     * operation
     */
    public abstract Enumeration getGroupNames (String username)
	throws InvalidOperationException, NoSuchUserException;
    
    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * @exception BadRealmException if realm data structures are bad
     */
    public abstract void  refresh() throws BadRealmException;

    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * @exception BadRealmException if realm data structures are bad
     */
    public void refresh(String configName) throws BadRealmException {
        //do nothing
    }
    
    /**
     * Adds new user to file realm. User cannot exist already.
     *
     * @param name User name.
     * @param password Cleartext password for the user.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     *
     */
    public abstract void addUser(String name, char[] password, String[] groupList)
        throws BadRealmException, IASSecurityException;
    
    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     * @throws NoSuchUserException If user does not exist.
     *
     */
     public abstract void removeUser(String name)
        throws NoSuchUserException, BadRealmException;
     
     /**
     * Update data for an existing user. User must exist.
     *
     * @param name Current name of the user to update.
     * @param newName New name to give this user. It can be the same as
     *     the original name. Otherwise it must be a new user name which
     *     does not already exist as a user.
     * @param password Cleartext password for the user. If non-null the user
     *     password is changed to this value. If null, the original password
     *     is retained.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    public abstract void updateUser(String name, String newName, char[] password,
                           String[] groups)
        throws NoSuchUserException, BadRealmException,
                               IASSecurityException;
    /**
     * @return true if the realm implementation support User Management (add,remove,update user)
     */
    public abstract boolean supportsUserManagement();
    
   /**
    * Persist the realm data to permanent storage
    * @throws com.sun.enterprise.security.auth.realm.BadRealmException
    */
    public abstract void persist() throws BadRealmException;
}





