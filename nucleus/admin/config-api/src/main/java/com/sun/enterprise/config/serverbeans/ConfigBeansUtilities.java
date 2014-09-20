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

package com.sun.enterprise.config.serverbeans;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.glassfish.api.admin.config.ApplicationName;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Bunch of utility methods for the new serverbeans config api based on jaxb
 */
@Service @Singleton
public final class ConfigBeansUtilities {

    private final Applications apps;

    private final Domain domain;
    
    // dochez : this class needs to be killed but I have no time to do it now
    // I am making it a singleton, will force its initialization early enough so
    // users can continue using the static method. Eventually all these methods will
    // need to be moved to @DuckTyped methods on the interfaces directory.
    @Inject
    public ConfigBeansUtilities(Applications paramApps, Domain paramDomain) {
        apps = paramApps;
        domain = paramDomain;
    }

    /**
     * Get the default value of Format from dtd
     */
    public static String getDefaultFormat() {
        return "%client.name% %auth-user-name% %datetime% %request% %status% %response.length%";
    }

    /**
     * Get the default value of RotationPolicy from dtd
     */
    public static String getDefaultRotationPolicy() {
        return "time";
    }

    /**
     * Get the default value of RotationEnabled from dtd
     */
    public static String getDefaultRotationEnabled() {
        return "true";
    }

    /**
     * Get the default value of RotationIntervalInMinutes from dtd
     */
    public static String getDefaultRotationIntervalInMinutes() {
        return "1440";
    }

    /**
     * Get the default value of QueueSizeInBytes from dtd
     */
    public static String getDefaultQueueSizeInBytes() {
        return "4096";
    }

    /**
     * This method is used to convert a string value to boolean.
     *
     * @return true if the value is one of true, on, yes, 1. Note
     *         that the values are case sensitive. If it is not one of these
     *         values, then, it returns false.
     */
    public static boolean toBoolean(final String value) {
        if (value != null) {
            final String v = value.trim();
            return "true".equals(v)
                || "yes".equals(v)
                || "on".equals(v)
                || "1".equals(v);
        } else {
            return false;
        }
    }

    /** Returns the list of system-applications that are referenced from the given server.
     *  A server references an application, if the server has an element named
     *  &lt;application-ref> in it that points to given application. The given server
     *  is a &lt;server> element inside domain.
     *  
     * @param sn the string denoting name of the server
     * @return List of system-applications for that server, an empty list in case there is none
     */
    public List<Application> getSystemApplicationsReferencedFrom(String sn) {
        if (domain == null || sn == null)
            throw new IllegalArgumentException("Null argument");
        List<Application> allApps = getAllDefinedSystemApplications();
        if (allApps.isEmpty()) {
            return allApps; //if there are no sys-apps, none can reference one :)
        }
        //allApps now contains ALL the system applications
        Server s = getServerNamed(sn);
        List<Application> referencedApps = new ArrayList<Application>();
        List<ApplicationRef> appsReferenced = s.getApplicationRef();
        for (ApplicationRef ref : appsReferenced) {
            for (Application app : allApps) {
                if (ref.getRef().equals(app.getName())) {
                    referencedApps.add(app);
                }
            }
        }
        return referencedApps;
    }
    
    public Application getSystemApplicationReferencedFrom(String sn, String appName) {
        //returns null in case there is none
        List<Application> allApps = getSystemApplicationsReferencedFrom(sn);
        for (Application app : allApps) {
            if (app.getName().equals(appName)) {
                return app;
            }
        }
        return null;
    }
    public boolean isNamedSystemApplicationReferencedFrom(String appName, String serverName) {
        List <Application> referencedApps = getSystemApplicationsReferencedFrom(serverName);
        for (Application app : referencedApps) {
            if (app.getName().equals(appName))
                return true;
        }
        return false;
    }
    
    public List<Server> getServers() {
        if (domain == null || domain.getServers() == null )
            throw new IllegalArgumentException ("Either domain is null or no <servers> element");
        return domain.getServers().getServer();
    }

    public Server getServerNamed(String name) {
        if (domain == null || domain.getServers() == null || name == null)
            throw new IllegalArgumentException ("Either domain is null or no <servers> element");
        List<Server> servers = domain.getServers().getServer();
        for (Server s : servers) {
            if (name.equals(s.getName().trim())) {
                return s;
            }
        }
        return null;
    }
    
    public List<Application> getAllDefinedSystemApplications() {
        List<Application> allSysApps = new ArrayList<Application>();
        SystemApplications sa = domain.getSystemApplications();
        if (sa != null) {
            for (ApplicationName m : sa.getModules()) {
                if (m instanceof Application)
                    allSysApps.add((Application)m);
            }
        }
        return allSysApps;
    }

   /**
    * Lists the app refs for non-system apps assigned to the specified server
    * @param sn server name
    * @return List of ApplicationRef for non-system apps assigned to the specified server
    */
   public List<ApplicationRef> getApplicationRefsInServer(String sn) {
       return getApplicationRefsInServer(sn, true);
   }

   /**
    * Lists the app refs for apps assigned to the specified server, excluding
    * system apps from the result if requested.
    * @param sn server name to check
    * @param excludeSystemApps whether system apps should be excluded
    * @return List of ApplicationRef for apps assigned to the specified server
    */
   public List<ApplicationRef> getApplicationRefsInServer(String sn, boolean excludeSystemApps) {

        Servers ss = domain.getServers();
        List<Server> list = ss.getServer();
        Server theServer = null;
        for (Server s : list) {
            if (s.getName().equals(sn)) {
                theServer = s;
                break;
            }
        }
        if (theServer != null) {
            List<ApplicationName> modulesToExclude = excludeSystemApps ?
                domain.getSystemApplications().getModules() : Collections.<ApplicationName>emptyList();
            List<ApplicationRef> result = new ArrayList<ApplicationRef>();
              for (ApplicationRef candidateRef : theServer.getApplicationRef()) {
                String appRefModuleName = candidateRef.getRef();
                boolean isSystem = false;
                for (ApplicationName sysModule : modulesToExclude) {
                    if (sysModule.getName().equals(appRefModuleName)) {
                        isSystem = true;
                        break;
                    }
                }
                if (!isSystem) {
                    result.add(candidateRef);
                }
            }
            return result;
        } else {
            return Collections.<ApplicationRef>emptyList();
        }
    }


    public ApplicationRef getApplicationRefInServer(String sn, String name) {
        Servers ss = domain.getServers();
        List<Server> list = ss.getServer();
        Server theServer = null;
        for (Server s : list) {
            if (s.getName().equals(sn)) {
                theServer = s;
                break;
            }
        }
        ApplicationRef aref = null;
        if (theServer != null) {
            List <ApplicationRef> arefs = theServer.getApplicationRef();
            for (ApplicationRef ar : arefs) {
                if (ar.getRef().equals(name)) {
                    aref = ar;
                    break;
                }
            }
        }
        return aref;
    }

    public ApplicationName getModule(String moduleID) {
        for (ApplicationName module : apps.getModules()) {
            if (module.getName().equals(moduleID)) {
                return module;
            }
        }
        return null;
    }

    public String getEnabled(String sn, String moduleID) {
        ApplicationRef appRef = getApplicationRefInServer(sn, moduleID);
        if (appRef != null) { 
            return appRef.getEnabled();
        } else {
            return null;
        }
    }

    public String getVirtualServers(String sn, String moduleID) {
        ApplicationRef appRef = getApplicationRefInServer(sn, moduleID);
        if (appRef != null) { 
            return appRef.getVirtualServers();
        } else {
            return null;
        }
    }

   public String getContextRoot(String moduleID) {
        ApplicationName module = getModule(moduleID);
        if (module == null) {
            return null;
        }

        if (module instanceof Application) {
            return ((Application)module).getContextRoot();
        } else {
            return null;
        }
    }

    public String getLibraries(String moduleID) {
        ApplicationName module = getModule(moduleID);
        if (module == null) {
            return null;
        }

        if (module instanceof Application) {
            return ((Application)module).getLibraries();
        } else {
            return null;
        }
    }

    public String getLocation(String moduleID) {
        ApplicationName module = getModule(moduleID);
        if (module == null) {
            return null;
        } 

        String location = null; 
        if (module instanceof Application) {
            location =  ((Application)module).getLocation();
        }

        try { 
            if (location != null) {
                return new URI(location).getPath();
            } else {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }            
    }

    public String getDirectoryDeployed(String moduleID) {
        ApplicationName module = getModule(moduleID);
        if (module == null) {
            return null;
        } 

        if (module instanceof Application) {
            return ((Application)module).getDirectoryDeployed();
        } else {
            return null;
        }
    }

    public static String join(Iterable<String> list, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (String string : list) {
            if (builder.length() != 0) {
                builder.append(delimiter);
            }
            builder.append(string);
        }
        return builder.toString();
    }

    public static String toString(Throwable t) {
        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

   public Domain getDomain() {
        return domain;
   }
}
