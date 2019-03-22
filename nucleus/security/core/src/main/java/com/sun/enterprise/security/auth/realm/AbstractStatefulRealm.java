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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates
package com.sun.enterprise.security.auth.realm;

import static com.sun.enterprise.security.BaseRealm.JAAS_CONTEXT_PARAM;
import static com.sun.enterprise.security.auth.realm.RealmsManagerStore._getRealmsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class is the base for all Payara Realm classes, and contains common
 * object state such as the realm name, general properties and the assign groups.
 * 
 * @author Arjan Tijms
 *
 */
public abstract class AbstractStatefulRealm extends AbstractRealm implements Comparable<Realm> {
    
    public static final String PARAM_GROUP_MAPPING = "group-mapping";
    
    // For assign-groups
    
    private static final String PARAM_GROUPS = "assign-groups";
    private static final String GROUPS_SEP = ",";
    private static final String DEFAULT_DEF_DIG_ALGO_VAL = "SHA-256";
    
    // All realms have a set of properties from config file, consolidate.
    
    private String realmName;
    private Properties contextProperties;
    private List<String> assignGroups;
    private String defaultDigestAlgorithm;
    
    protected GroupMapper groupMapper;
    
    // ---[ Instance methods ]------------------------------------------------
    
    /**
     * The default the constructor creates a realm which will later be initialized, 
     * either from properties or by deserializing.
     */
    protected AbstractStatefulRealm() {
        contextProperties = new Properties();
    }

    /**
     * Initialize a realm with some properties. This can be used when instantiating realms from their
     * descriptions. This method may only be called a single time.
     *
     * @param properties initialization parameters used by this realm.
     * @exception BadRealmException if the configuration parameters identify a corrupt realm
     * @exception NoSuchRealmException if the configuration parameters specify a realm which doesn't
     * exist
     */
    protected void init(Properties properties) throws BadRealmException, NoSuchRealmException {
        String groupList = properties.getProperty(PARAM_GROUPS);
        
        if (groupList != null && groupList.length() > 0) {
            setProperty(PARAM_GROUPS, groupList);
            
            assignGroups = new ArrayList<String>();
            for (String group :  groupList.split(GROUPS_SEP)) {
                if (!assignGroups.contains(group)) {
                    assignGroups.add(group);
                }
            }
        }
        
        String groupMapping = properties.getProperty(PARAM_GROUP_MAPPING);
        if (groupMapping != null) {
            groupMapper = new GroupMapper();
            groupMapper.parse(groupMapping);
        }
        
        String defaultDigestAlgo = null;
        if (_getRealmsManager() != null) {
            defaultDigestAlgo = _getRealmsManager().getDefaultDigestAlgorithm();
        }
        
        defaultDigestAlgorithm = defaultDigestAlgo == null ? DEFAULT_DEF_DIG_ALGO_VAL : defaultDigestAlgo;
    }
    
    /**
     * Add assign groups to given array of groups. To be used by getGroupNames.
     *
     * @param groups
     * @return
     */
    protected String[] addAssignGroups(String[] groups) {
        String[] resultGroups = groups;
        
        if (assignGroups != null && assignGroups.size() > 0) {
            List<String> groupList = new ArrayList<>();
            if (groups != null && groups.length > 0) {
                for (String group : groups) {
                    groupList.add(group);
                }
            }

            for (String assignGroup : assignGroups) {
                if (!groupList.contains(assignGroup)) {
                    groupList.add(assignGroup);
                }
            }
            resultGroups = groupList.toArray(new String[groupList.size()]);
        }
        
        return resultGroups;
    }

    protected ArrayList<String> getMappedGroupNames(String group) {
        if (groupMapper == null) {
            return null;
        }
        
        ArrayList<String> result = new ArrayList<String>();
        groupMapper.getMappedGroups(group, result);
        
        return result;
    }
    
    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * @param configName
     * @exception BadRealmException if realm data structures are bad
     */
    public void refresh(String configName) throws BadRealmException {
        // do nothing
    }
    
    /**
     * Returns the name of this realm.
     *
     * @return realm name.
     */
    public final String getName() {
        return realmName;
    }
    
    /**
     * Assigns the name of this realm, and stores it in the cache of realms. Used when initializing a
     * newly created in-memory realm object; if the realm already has a name, there is no effect.
     *
     * @param name name to be assigned to this realm.
     */
    protected final void setName(String name) {
        if (realmName != null) {
            return;
        }
        
        realmName = name;
    }

    protected String getDefaultDigestAlgorithm() {
        return defaultDigestAlgorithm;
    }
    
    /**
     * Get a realm property.
     *
     * @param name property name.
     * @return
     * @returns value.
     *
     */
    public synchronized String getProperty(String name) {
        return contextProperties.getProperty(name);
    }

    /**
     * Set a realm property.
     *
     * @param name property name.
     * @param value property value.
     *
     */
    public synchronized void setProperty(String name, String value) {
        contextProperties.setProperty(name, value);
    }

    /**
     * Return properties of the realm.
     *
     * @return
     */
    protected synchronized Properties getProperties() {
        return contextProperties;
    }

    /**
     * Returns name of JAAS context used by this realm.
     *
     * <P>
     * The JAAS context is defined in server.xml auth-realm element associated with this realm.
     *
     * @return String containing JAAS context name.
     *
     */
    public synchronized String getJAASContext() {
        return contextProperties.getProperty(JAAS_CONTEXT_PARAM);
    }
    
    /**
     * Returns the name of this realm.
     *
     * @return name of realm.
     */
    @Override
    public String toString() {
        return realmName;
    }

    /**
     * Compares a realm to another. 
     * 
     * <p>
     * The comparison first considers the authentication type, so that realms supporting the same 
     * kind of user authentication are grouped together. 
     * Then it compares realm realm names. Realms compare "before" other kinds of objects 
     * (i.e. there's only a partial order defined, in the case that those other objects compare themselves 
     * "before" a realm object).
     */
    @Override
    public int compareTo(Realm otherRealm) {
        String str = otherRealm.getAuthType();
        int temp;

        if ((temp = getAuthType().compareTo(str)) != 0) {
            return temp;
        }

        return getName().compareTo(otherRealm.getName());
    }

}
