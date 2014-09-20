/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.perms;

import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.AccessController;
import java.security.PrivilegedAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.sun.logging.LogDomains;

import sun.security.provider.PolicyFile;

/**
 * 
 * Utility class to load the EE permissions, EE restrictions, 
 * and check restrictions for a given permission set
 *
 */
public class SMGlobalPolicyUtil {
    
    static Logger logger = Logger.getLogger(LogDomains.SECURITY_LOGGER);


    /**
     * 
     * Java EE Component type supporting the use of declared permissions
     *
     */
    public enum CommponentType {
        ear,
        ejb,
        war,
        rar,
        car
    }
    
    
    private enum PolicyType {
        /**
         * Configured EE permissions in the domain
         */
        EEGranted,
        
        /**
         * Configured EE restriction list in the domain
         */
        EERestricted,
        
        /**
         * Configured domain allowed list
         */
        ServerAllowed
    }
        
    /**
     * This is the file storing the default permissions granted to each component type
     */
    public static final String EE_GRANT_FILE = "javaee.server.policy";
    
    /**
     * This is the file storing the restricted permissions for each component type;
     * Any permissions declared in this list can not be used by the application 
     */
    public static final String EE_RESTRICTED_FILE = "restrict.server.policy";
    
    /**
     * This is the file storing the allowed permissions for each component type
     * A permission listed in this file may not be used but the application,
     * but any application declared permission must exist in this list;
     */
    public static final String SERVER_ALLOWED_FILE = "restrict.server.policy";
    
    protected static final String SYS_PROP_JAVA_SEC_POLICY = "java.security.policy";

    
    /**
     * Code source URL representing Ejb type
     */
    public static final String EJB_TYPE_CODESOURCE = "file:/module/Ejb";
    /**
     * Code source URL representing Web type
     */
    public static final String WEB_TYPE_CODESOURCE = "file:/module/Web";
    /**
     * Code source URL representing Rar type
     */
    public static final String RAR_TYPE_CODESOURCE = "file:/module/Rar";
    /**
     * Code source URL representing App client type
     */
    public static final String CLIENT_TYPE_CODESOURCE = "file:/module/Car";

    /**
     * Code source URL representing Ear type
     */
    public static final String EAR_TYPE_CODESOURCE = "file:/module/Ear";
    

    public static final String EAR_CLASS_LOADER = "org.glassfish.javaee.full.deployment.EarClassLoader";
    
    //map recording the 'Java EE component type' to its code source URL
    private static final Map<CommponentType, String> CompTypeToCodeBaseMap = new HashMap<CommponentType, String>();

    static {
        CompTypeToCodeBaseMap.put(CommponentType.ejb, EJB_TYPE_CODESOURCE);
        CompTypeToCodeBaseMap.put(CommponentType.war, WEB_TYPE_CODESOURCE);
        CompTypeToCodeBaseMap.put(CommponentType.rar, RAR_TYPE_CODESOURCE);
        CompTypeToCodeBaseMap.put(CommponentType.car, CLIENT_TYPE_CODESOURCE);
        CompTypeToCodeBaseMap.put(CommponentType.ear, EAR_TYPE_CODESOURCE);
    }

    
    //map recording the 'Java EE component type' to its EE granted permissions
    private static final Map<CommponentType, PermissionCollection> compTypeToEEGarntsMap 
            = new HashMap<CommponentType, PermissionCollection>();

    //map recording the 'Java EE component type' to its EE restricted permissions
    private static final Map<CommponentType, PermissionCollection> compTypeToEERestrictedMap 
            = new HashMap<CommponentType, PermissionCollection>();
    
    //map recording the 'Java EE component type' to its allowed permissions
    private static final Map<CommponentType, PermissionCollection> compTypeToServAllowedMap 
            = new HashMap<CommponentType, PermissionCollection>();
    
    
    private static boolean eeGrantedPolicyInitDone = false;

    
    protected static final String domainCfgFolder = getJavaPolicyFolder() + File.separator;
    
    private static final AllPermission ALL_PERM = new AllPermission();

    

    //convert a string type to the CommponentType
    public static CommponentType convertComponentType(String type) {        
        
        return Enum.valueOf(CommponentType.class, type);
    }
    
    
    /**
     * Get the default granted permissions of a specified component type 
     * @param type  Java EE component type
     * @return the permission set granted to the specified component
     */
    public static PermissionCollection getEECompGrantededPerms(CommponentType type) {
        initDefPolicy();
        return compTypeToEEGarntsMap.get(type);
    }
    
    /**
     * Get the default granted permissions of a specified component type
     * @param type Java EE component type such as ejb, war, rar, car, ear
     * @return
     */
    public static PermissionCollection getEECompGrantededPerms(String type) {
        CommponentType compType = convertComponentType(type);
        return getEECompGrantededPerms(compType);
    }
    

    /**
     * Get the restricted permission set of a specified component type on the server
     * @param type  Java EE component type
     * @return the restricted permission set of the specified component type on the server
     */
    public static PermissionCollection getCompRestrictedPerms(CommponentType type) {
        initDefPolicy();
        return compTypeToEERestrictedMap.get(type);
    }
    
    
    public static PermissionCollection getCompRestrictedPerms(String type) {
        CommponentType compType = convertComponentType(type);
        return getCompRestrictedPerms(compType);
    }
    
    

    private synchronized static void initDefPolicy() {
        
        try {
            
            if (logger.isLoggable(Level.FINE)){
                logger.fine("defGrantedPolicyInitDone= " + eeGrantedPolicyInitDone);
            }
            
            if (eeGrantedPolicyInitDone)
                return;
            
            eeGrantedPolicyInitDone = true;

            loadServerPolicy(PolicyType.EEGranted);
            
            loadServerPolicy(PolicyType.EERestricted);
            
            loadServerPolicy(PolicyType.ServerAllowed);
            
            checkDomainRestrictionsForDefaultPermissions();
            
        } catch (FileNotFoundException e) {
            //ignore: the permissions files not exist
        } catch (IOException e) {
            logger.warning(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String getJavaPolicyFolder() {
        
        String policyPath = System.getProperty(SYS_PROP_JAVA_SEC_POLICY);
        
        if (policyPath == null)
            return null;
        
        File pf = new File(policyPath);
        
        return pf.getParent();
    }
    

    private static void loadServerPolicy(PolicyType policyType) throws IOException {
                
        if (policyType == null)
            return;

        if (logger.isLoggable(Level.FINE)){
            logger.fine("PolicyType= " + policyType);
        }

        String policyFilename = null;
        Map<CommponentType, PermissionCollection> policyMap = null;
        
        switch (policyType) {
            case EEGranted:
                policyFilename = domainCfgFolder + EE_GRANT_FILE;
                policyMap = compTypeToEEGarntsMap;
                break;
            case EERestricted:
                policyFilename = domainCfgFolder + EE_RESTRICTED_FILE;
                policyMap = compTypeToEERestrictedMap;
                break;
            case ServerAllowed:
                policyFilename = domainCfgFolder + SERVER_ALLOWED_FILE;
                policyMap = compTypeToServAllowedMap;
                break;
        }
        
        if (policyFilename == null  || policyMap == null)
            throw new IllegalArgumentException("Unrecognized policy type: " + policyType);

        if (logger.isLoggable(Level.FINE)){
            logger.fine("policyFilename= " + policyFilename);
        }

        File f = new File(policyFilename);
        if (!f.exists())
            return;
        
        URL furl = new URL("file:" + policyFilename);
            
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loading policy from " + furl);
        }
        PolicyFile pf = new PolicyFile(furl);

        CodeSource cs = new CodeSource(new URL(EJB_TYPE_CODESOURCE), (Certificate[])null ); 
        PermissionCollection pc = pf.getPermissions(cs);
        policyMap.put(CommponentType.ejb, pc);
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loaded EJB policy = " + pc);
        }

        cs = new CodeSource(new URL(WEB_TYPE_CODESOURCE), (Certificate[])null );
        pc = pf.getPermissions(cs);
        policyMap.put(CommponentType.war, pc);
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loaded WEB policy =" + pc);
        }
        
        cs = new CodeSource(new URL(RAR_TYPE_CODESOURCE), (Certificate[])null );
        pc = pf.getPermissions(cs);
        policyMap.put(CommponentType.rar, pc);
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loaded rar policy =" + pc);
        }
        
        cs = new CodeSource(new URL(CLIENT_TYPE_CODESOURCE), (Certificate[])null );
        pc = pf.getPermissions(cs);
        policyMap.put(CommponentType.car, pc);
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loaded car policy =" + pc);
        }

        cs = new CodeSource(new URL(EAR_TYPE_CODESOURCE), (Certificate[])null );
        pc = pf.getPermissions(cs);
        policyMap.put(CommponentType.ear, pc);
        if (logger.isLoggable(Level.FINE)){
            logger.fine("Loaded ear policy =" + pc);
        }


    }
    
    //this checks default permissions against restrictions    
    private static void checkDomainRestrictionsForDefaultPermissions() throws SecurityException {
        
        checkEETypePermsAgainstServerRestiction(CommponentType.ejb);
        checkEETypePermsAgainstServerRestiction(CommponentType.war);
        checkEETypePermsAgainstServerRestiction(CommponentType.rar);
        checkEETypePermsAgainstServerRestiction(CommponentType.car);       
        checkEETypePermsAgainstServerRestiction(CommponentType.ear);       
    }

    private static void checkEETypePermsAgainstServerRestiction(CommponentType type) throws SecurityException  {

        checkRestriction(compTypeToEEGarntsMap.get(type), 
                compTypeToEERestrictedMap.get(type));
    }
    

    
    public static void checkRestriction (CommponentType type, 
            PermissionCollection declaredPC ) throws SecurityException  { 
            
        checkRestriction(declaredPC, getCompRestrictedPerms(type));
    }
    
    
    /**
     * Checks a permissions set against a restriction set
     * @param declaredPC
     * @param restrictedPC
     * @return true for passed 
     * @throws SecurityException is thrown if violation detected 
     */
    public static void checkRestriction (PermissionCollection declaredPC, 
            PermissionCollection restrictedPC) throws SecurityException  {
        
        if (restrictedPC == null  || declaredPC == null)
            return;

        //check declared does not contain restricted
        checkContains(declaredPC, restrictedPC);
        
        //check restricted does not contain declared
        checkContains(restrictedPC, declaredPC);

    }
 
    
    //check if permissionCollection toBeCheckedPC is contained/implied by  containPC
    private static void checkContains(PermissionCollection containPC, 
            PermissionCollection toBeCheckedPC) throws SecurityException {
    
        if (containPC == null  || toBeCheckedPC == null)
            return;

        Enumeration<Permission> checkEnum = toBeCheckedPC.elements();
        while (checkEnum.hasMoreElements()) {
            Permission p = checkEnum.nextElement();
            if (containPC.implies(p)) {
                throw new SecurityException("Restricted permission " + p 
                        + " is declared or implied in the " + containPC);
            }
        }
        
        return;        
    }
    
    /**
     * Check a permission set against a restriction of a component type
     * @param declaredPC
     * @param type
     * @return
     * @throws SecurityException
     */
    public static void checkRestrictionOfComponentType (PermissionCollection declaredPC, CommponentType type) throws SecurityException  {
        
        if (CommponentType.ear == type)
            checkRestrictionOfEar(declaredPC);
        
        PermissionCollection restrictedPC =
            compTypeToEERestrictedMap.get(type);
        
        checkRestriction(declaredPC, restrictedPC);                
    }
    

    //for ear type, check evrything
    public static void checkRestrictionOfEar (PermissionCollection declaredPC) throws SecurityException  {        

        PermissionCollection pc =            
            compTypeToEERestrictedMap.get(CommponentType.ejb);
        if (pc != null)
            SMGlobalPolicyUtil.checkRestriction(declaredPC, pc);
        
        pc = compTypeToEERestrictedMap.get(CommponentType.war);
        if (pc != null)
            SMGlobalPolicyUtil.checkRestriction(declaredPC, pc);

        pc = compTypeToEERestrictedMap.get(CommponentType.rar);
        if (pc != null)
            SMGlobalPolicyUtil.checkRestriction(declaredPC, pc);
        
        pc = compTypeToEERestrictedMap.get(CommponentType.car);
        if (pc != null)
            SMGlobalPolicyUtil.checkRestriction(declaredPC, pc);
        
    }
    
}

