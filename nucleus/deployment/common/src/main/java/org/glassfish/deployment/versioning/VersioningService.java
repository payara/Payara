/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.versioning;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Application;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.deployment.common.DeploymentUtils;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * This service provides methods to handle application names
 * in the versioning context
 *
 * @author Romain GRECOURT - SERLI (romain.grecourt@serli.com)
 */
@I18n("versioning.service")
@Service
@PerLookup
public class VersioningService {

    @Inject
    private CommandRunner commandRunner;
    @Inject
    private Domain domain;

    /**
     * Extract the set of version(s) of the given application represented as
     * an untagged version name
     *
     * @param untaggedName the application name as an untagged version : an
     * application name without version identifier
     * @param target the target where we want to get all the versions
     * @return all the version(s) of the given application
     */
    private final List<String> getAllversions(String untaggedName, String target) {
        List<Application> allApplications = null;
        if (target != null) {
            allApplications = domain.getApplicationsInTarget(target);
        } else {
            allApplications = domain.getApplications().getApplications();
        }
        return VersioningUtils.getVersions(untaggedName, allApplications);
    }

   /**
     * Search the enabled versions on the referenced targets of each version
     * matched by the expression.
     * This method is designed to be used with domain target. As different
     * versions maybe enabled on different targets, the return type used is a map.
     *
     * @param versionExpression a version expression (that contains wildcard character)
     * @return a map matching the enabled versions with their target(s)
     * @throws VersioningSyntaxException if getEnabledVersion throws an exception
     */
    public Map<String, Set<String>> getEnabledVersionInReferencedTargetsForExpression(String versionExpression)
            throws VersioningSyntaxException {

        Map<String,Set<String>> enabledVersionsInTargets = Collections.EMPTY_MAP;
        List<String> matchedVersions = getMatchedVersions(versionExpression, "domain");

        // foreach matched version
        Iterator it = matchedVersions.iterator();
        while(it.hasNext()){

            String matchedVersion = (String) it.next();
            // retrieved all the enabled version on the referenced target on each matched version
            Map<String,Set<String>> tempMap =
                    getEnabledVersionsInReferencedTargets(matchedVersion);

            if(enabledVersionsInTargets != Collections.EMPTY_MAP){

                // foreach enabled version we combine the target list into the map
                for (Map.Entry<String, Set<String>> entry : tempMap.entrySet()) {
                    String tempKey = entry.getKey();
                    Set<String> tempList = entry.getValue();

                    if(enabledVersionsInTargets.containsKey(tempKey)){
                        enabledVersionsInTargets.get(tempKey).addAll(tempList);
                    } else {
                        enabledVersionsInTargets.put(tempKey, tempList);
                    }
                }
            } else {
                enabledVersionsInTargets = tempMap;
            }
        }
        return enabledVersionsInTargets;
    }
    
    /**
     * Search the enabled versions on the referenced targets of the given version.
     * This method is designed to be used with domain target. As different
     * versions maybe enabled on different targets, the return type used is a map.
     *
     * @param versionIdentifier a version expression (that contains wildcard character)
     * @return a map matching the enabled versions with their target(s)
     * @throws VersioningSyntaxException if getEnabledVersion throws an exception
     */
    public Map<String,Set<String>> getEnabledVersionsInReferencedTargets(String versionIdentifier)
            throws VersioningSyntaxException {
        
        Map<String,Set<String>> enabledVersionsInTargets =
                new HashMap<String, Set<String>>();

        List<String> allTargets =
                domain.getAllReferencedTargetsForApplication(versionIdentifier);

        Iterator it = allTargets.iterator();
        while(it.hasNext()){
            String target = (String)it.next();
            String enabledVersion = getEnabledVersion(versionIdentifier, target);
            if(enabledVersion != null){
                // the key already exists, we just add the new target into the list
                if(enabledVersionsInTargets.containsKey(enabledVersion)){
                    enabledVersionsInTargets.get(enabledVersion).add(target);
                } else {
                    // we have to create the list associated with the key
                    Set<String> setTargets = new HashSet<String>();
                    setTargets.add(target);
                    enabledVersionsInTargets.put(enabledVersion, setTargets);
                }
            }
        }
        return enabledVersionsInTargets;
    }

    /**
     * Search for the enabled version of the given application.
     *
     * @param name the application name
     * @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     * @return the enabled version of the application, if exists
     * @throws VersioningSyntaxException if getUntaggedName throws an exception
     */
    public final String getEnabledVersion(String name, String target)
            throws VersioningSyntaxException {

        String untaggedName = VersioningUtils.getUntaggedName(name);
        List<String> allVersions = getAllversions(untaggedName, target);

        if (allVersions != null) {
            Iterator it = allVersions.iterator();

            while (it.hasNext()) {
                String app = (String) it.next();

                // if a version of the app is enabled
                if (domain.isAppEnabledInTarget(app, target)) {
                    return app;
                }
            }
        }
        // no enabled version found
        return null;
    }
    
    /**
     * Process the expression matching operation of the given application name.
     *
     * @param name the application name containing the version expression
     * @param target the target we are looking for the verisons 
     * @return a List of all expression matched versions, return empty list
     * if no version is registered on this target
     * or if getUntaggedName throws an exception
     */
    public final List<String> getMatchedVersions(String name, String target)
            throws VersioningSyntaxException, VersioningException {

        String untagged = VersioningUtils.getUntaggedName(name);
        List<String> allVersions = getAllversions(untagged, target);

        if (allVersions.size() == 0) {
            // if versionned
            if(!name.equals(untagged)){
                throw new VersioningException(
                        VersioningUtils.LOCALSTRINGS.getLocalString("versioning.deployment.application.noversion",
                        "Application {0} has no version registered",
                        untagged));  
            }
            return Collections.EMPTY_LIST;
        }

        return VersioningUtils.matchExpression(allVersions, name);
    }

    /**
     *  Disable the enabled version of the application if it exists. This method
     *  is used in versioning context.
     *
     *  @param appName application's name
     *  @param target an option supply from admin command, it's retained for
     * compatibility with other releases
     *  @param report ActionReport, report object to send back to client.
     *  @param subject the Subject on whose behalf to run
     */
    public void handleDisable(final String appName, final String target,
            final ActionReport report, final Subject subject) throws VersioningSyntaxException {

        Set<String> versionsToDisable = Collections.EMPTY_SET;

        if (DeploymentUtils.isDomainTarget(target)) {
            // retrieve the enabled versions on each target in the domain 
            Map<String,Set<String>> enabledVersions =
                    getEnabledVersionsInReferencedTargets(appName);

            if (!enabledVersions.isEmpty()) {
                versionsToDisable = enabledVersions.keySet();
            }
        } else {
            // retrieve the currently enabled version of the application
            String enabledVersion = getEnabledVersion(appName, target);

            if (enabledVersion != null
                    && !enabledVersion.equals(appName)) {
                versionsToDisable = new HashSet<String>();
                versionsToDisable.add(enabledVersion);
            }
        }

        Iterator<String> it = versionsToDisable.iterator();
        while (it.hasNext()) {
            String currentVersion = it.next();
            // invoke disable if the currently enabled version is not itself
            if (currentVersion != null
                    && !currentVersion.equals(appName)) {
                final ParameterMap parameters = new ParameterMap();
                parameters.add("DEFAULT", currentVersion);
                parameters.add("target", target);

                ActionReport subReport = report.addSubActionsReport();

                CommandRunner.CommandInvocation inv =
                        commandRunner.getCommandInvocation("disable", subReport, subject);
                inv.parameters(parameters).execute();
            }
        }
    }

    /**
     * Get the version directory-deployed from the given directory
     *
     * @param directory
     * @return the name of the version currently using the directory, else null
     * @throws VersioningSyntaxException     *
    */
    public String getVersionFromSameDir(File dir)
            throws VersioningSyntaxException{

        try {
            Iterator it = domain.getApplications().getApplications().iterator();
            Application app = null;

            // check if directory deployment exist
            while ( it.hasNext() ) {
                app = (Application) it.next();
                /*
                 * A lifecycle module appears as an application but has a null location.
                 */
                if (dir.toURI().toString().equals(app.getLocation())) {
                    if(!VersioningUtils.getUntaggedName(app.getName()).equals(app.getName())){
                        return app.getName();
                    }
                }
            }
        } catch (VersioningSyntaxException ex) {
            // return null if an exception is thrown
        }
        return null;
    }
}
