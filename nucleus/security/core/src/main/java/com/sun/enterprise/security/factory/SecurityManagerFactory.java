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

package com.sun.enterprise.security.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author monzillo
 */
public class SecurityManagerFactory {

    protected SecurityManagerFactory() {
      //do not allow creation of this base class directly
    }
    /**
     * find SecurityManager by policy context identifier and name (as 
     * secondary key). There is a separate SecurityManager for each EJB of
     * a module; they all share the same policy context identifier, and are 
     * differentiated by (ejb) name. There is one SecurityManager per web 
     * module/policy context identifier.
     * 
     * @param iD2sMmap maps policy context id (as key) to subordinate map 
     * (as value) of (module) name (as key) to SecurityManagers (as value).
     * @param ctxId the policy context identifier (i.e., the primary key of the
     * lookup
     * @param name the name of the component (e.g., the ejb name when looking up
     * an EJBSecurityManager. for WebSecurityManagers this value should be null 
     * @param remove boolean indicating whether the corresponding SecurityManager
     * is to be deleted from the map. 
     * @return the selected SecurityManager or null.
     */
    public<T> T getManager(Map<String, Map<String, T>> iD2sMmap,
            String ctxId, String name, boolean remove) {

        T manager = null;

        synchronized (iD2sMmap) {
            Map<String, T> managerMap = iD2sMmap.get(ctxId);
            if (managerMap != null) {
                manager = managerMap.get(name);
                if (remove) {
                    managerMap.remove(name);
                    if (managerMap.isEmpty()) {
                        iD2sMmap.remove(ctxId);
                    }
                }
            }

        }

        return manager;
    }

    /**
     * Get all SecurityManagers associated with a policy context identifier. 
     * EJBs from the same jar, all share the same policy context identifier, but
     * each have their own SecurityManager. WebSecurityManager(s)
     * map 1-to-1 to policy context identifier.
     * 
     * @param iD2sMmap maps policy context id (as key) to subordinate map 
     * (as value) of (module) name (as key) to SecurityManagers (as value).
     * @param ctxId the policy context identifier (i.e., the lookup key).
     * @param remove boolean indicating whether the corresponding
     * SecurityManager(s) are to be deleted from the map. 
     * @return a non-empty ArrayList containing the selected managers, or null. 
     */
    public <T> ArrayList<T> getManagers(Map<String, Map<String, T>> iD2sMmap,
            String ctxId, boolean remove) {

        ArrayList<T> managers = null;

        synchronized (iD2sMmap) {
            Map<String, T> managerMap = iD2sMmap.get(ctxId);

            if (managerMap != null && !managerMap.isEmpty()) {
                managers = new ArrayList(managerMap.values());
            }
            if (remove) {
                iD2sMmap.remove(ctxId);
            }
        }
        return managers;
    }

    /**
     * Get (Web or EJB) SecurityManagers associated with an application.
     * Note that the WebSecurityManager and EJBSecurityManager classes manage
     * separate maps for their respectibe security manager types.
     * 
     * @param iD2sMmap maps policy context id (as key) to subordinate map 
     * (as value) of (module) name (as key) to SecurityManagers (as value).
     * @param app2iDmap maps appName (as key) to list of policy context
     * identifiers (as value). 
     * @param appName the application name, (i.e., the lookup key)
     * @param remove boolean indicating whether the corresponding mappings
     * are to be removed from the app2iDmap and aiD2sMmap.
     * @return a non-empty ArrayList containing the selected managers, or null. 
     */
    public <T> ArrayList<T> getManagersForApp(Map<String, Map<String, T>> iD2sMmap,
            Map<String, ArrayList<String>> app2iDmap, String appName, boolean remove) {

        ArrayList<T> managerList = null;
        String[] ctxIds = getContextsForApp(app2iDmap, appName, remove);
        if (ctxIds != null) {
            ArrayList<T> managers = null;
            synchronized (iD2sMmap) {
                for (String id : ctxIds) {
                    managers = getManagers(iD2sMmap, id, remove);
                    if (managers != null) {
                        if (managerList == null) {
                            managerList = new ArrayList<T>();
                        }
                        managerList.addAll(managers);
                    }
                }
            }
        }

        return managerList;
    }

    /**
     * Get (EJB or Web) Policy context identifiers for app.
     * 
     * @param app2iDmap maps appName (as key) to list of policy context 
     * identifiers (as value). 
     * @param appName the application name, (i.e., the lookup key).
     * @param remove boolean indicating whether the corresponding mappings
     * are to be removed from the app2iDmap.
     * @return a non-zero length array containing the selected
     * policy context identifiers, or null.
     */
    public <T> String[] getContextsForApp(Map<String, ArrayList<String>> app2iDmap,
            String appName, boolean remove) {

        String[] ctxIds = null;

        synchronized (app2iDmap) {

            ArrayList<String> ctxList = app2iDmap.get(appName);
            if (ctxList != null && !ctxList.isEmpty()) {
                ctxIds = ctxList.toArray(new String[ctxList.size()]);
            }
            if (remove) {
                app2iDmap.remove(appName);
            }
        }

        return ctxIds;
    }

    /**
     * In iD2sMmap, maps manager to  ctxId and name, and in app2iDmap, 
     * includes ctxID in values mapped to appName.   
     * 
     * @param iD2sMmap maps policy context id (as key) to subordinate map 
     * (as value) of (module) name (as key) to SecurityManagers (as value).
     * @param app2iDmap maps appName (as key) to list of policy context
     * identifiers (as value). 
     * @param ctxId the policy context identifier
     * @param name the component name (the EJB name or null for web modules)
     * @param appName the application name
     * @param manager the SecurityManager
     */
    public <T> void addManagerToApp(Map<String, Map<String, T>> iD2sMmap,
            Map<String, ArrayList<String>> app2iDmap,
            String ctxId, String name, String appName, T manager) {

        synchronized (iD2sMmap) {

            Map<String, T> managerMap = iD2sMmap.get(ctxId);

            if (managerMap == null) {
                managerMap = new HashMap<String, T>();
                iD2sMmap.put(ctxId, managerMap);
            }

            managerMap.put(name, manager);
        }
        synchronized (app2iDmap) {
            ArrayList<String> ctxList = app2iDmap.get(appName);

            if (ctxList == null) {
                ctxList = new ArrayList<String>();
                app2iDmap.put(appName, ctxList);
            }

            if (!ctxList.contains(ctxId)) {
                ctxList.add(ctxId);
            }
        }
    }
}   
